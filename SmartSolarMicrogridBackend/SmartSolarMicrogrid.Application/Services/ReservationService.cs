using SmartSolarMicrogrid.Application.DTOs.Reservation;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

namespace SmartSolarMicrogrid.Application.Services;

public class ReservationService : IReservationService {
    private readonly EnergyReservationRepository _res;
    private readonly ProsumerRepository _pros;
    private readonly EnergySlotRepository _slots;
    private readonly MicrogridNodeRepository _nodes;

    public ReservationService(EnergyReservationRepository res, ProsumerRepository pros, EnergySlotRepository slots, MicrogridNodeRepository nodes) {
        _res = res;
        _pros = pros;
        _slots = slots;
        _nodes = nodes;
    }

    public async Task<EnergyReservation> CreateAsync(string nic, CreateReservationDto d) {
        var p = (await _pros.GetAllAsync()).FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase)) ?? throw new KeyNotFoundException("Prosumer not found.");
        if (p.AccountStatus != UserStatus.Active) throw new UnauthorizedAccessException("Prosumer account is not active.");
        var s = await _slots.GetByIdAsync(d.EnergySlotId) ?? throw new KeyNotFoundException("Energy slot not found.");
        var node = await _nodes.GetByIdAsync(s.NodeId) ?? throw new KeyNotFoundException("Node not found.");
        if (node.Status != NodeStatus.Active || s.Status != SlotStatus.Available || s.AvailableCapacityKwh <= 0)
            throw new InvalidOperationException("Slot is unavailable.");
        if (s.SlotDate.Date < DateTime.UtcNow.Date || s.SlotDate.Date > DateTime.UtcNow.Date.AddDays(7))
            throw new ArgumentException("Reservation must be within the specified 7-day period.");
        Validation.Positive(d.EnergyAmountKwh, "EnergyAmountKwh");
        if (d.EnergyAmountKwh > s.AvailableCapacityKwh)
            throw new InvalidOperationException($"Requested energy ({d.EnergyAmountKwh} kWh) exceeds available capacity ({s.AvailableCapacityKwh} kWh).");

        var duplicate = (await _res.GetAllAsync()).Any(r => r.EnergySlotId == s.Id && r.ProsumerId == p.Id && (r.Status == ReservationStatus.Pending || r.Status == ReservationStatus.Approved));
        if (duplicate)
            throw new InvalidOperationException("You already have an active reservation for this slot.");

        var r = new EnergyReservation {
            Id = Guid.NewGuid().ToString(),
            ReservationCode = "RES-" + Random.Shared.Next(1000, 9999),
            ProsumerId = p.Id,
            NodeId = node.Id,
            NodeCode = node.NodeCode,
            NodeName = node.NodeName,
            EnergySlotId = s.Id,
            ReservationDate = s.SlotDate,
            StartTime = s.StartTime,
            EndTime = s.EndTime,
            EnergyAmountKwh = Math.Round(d.EnergyAmountKwh, 2),
            Status = ReservationStatus.Pending,
            CreatedAt = DateTime.UtcNow
        };

        // Note: Slot available capacity is deducted upon approval by backoffice/operator
        await _res.InsertAsync(r);
        return r;
    }

    public async Task<EnergyReservation?> GetAsync(string id) {
        var r = await _res.GetByIdAsync(id);
        if (r != null && (string.IsNullOrEmpty(r.NodeCode) || string.IsNullOrEmpty(r.NodeName))) {
            var n = await _nodes.GetByIdAsync(r.NodeId);
            if (n != null) {
                r.NodeCode = n.NodeCode;
                r.NodeName = n.NodeName;
            }
        }
        return r;
    }

    public async Task<IReadOnlyList<EnergyReservation>> GetMineAsync(string nic, string? search = null, string? status = null) {
        var p = (await _pros.GetAllAsync()).FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase)) ?? throw new KeyNotFoundException("Prosumer not found.");
        var x = (await _res.GetAllAsync()).Where(r => r.ProsumerId == p.Id).ToList();
        var allNodes = (await _nodes.GetAllAsync()).ToDictionary(n => n.Id, n => n);
        foreach (var r in x) {
            if (string.IsNullOrEmpty(r.NodeCode) || string.IsNullOrEmpty(r.NodeName)) {
                if (allNodes.TryGetValue(r.NodeId, out var n)) {
                    r.NodeCode = n.NodeCode;
                    r.NodeName = n.NodeName;
                }
            }
        }
        if (!string.IsNullOrWhiteSpace(search)) x = x.Where(r => r.ReservationCode.Contains(search, StringComparison.OrdinalIgnoreCase) || r.NodeId.Contains(search, StringComparison.OrdinalIgnoreCase) || r.NodeCode.Contains(search, StringComparison.OrdinalIgnoreCase) || r.NodeName.Contains(search, StringComparison.OrdinalIgnoreCase)).ToList();
        if (!string.IsNullOrWhiteSpace(status) && Enum.TryParse<ReservationStatus>(status, true, out var st)) x = x.Where(r => r.Status == st).ToList();
        return x.OrderByDescending(r => r.ReservationDate).ToList();
    }

    public async Task<IReadOnlyList<EnergyReservation>> GetByStatusAsync(string status) {
        if (!Enum.TryParse<ReservationStatus>(status, true, out var st)) throw new ArgumentException("Invalid status.");
        var list = (await _res.GetAllAsync()).Where(r => r.Status == st).OrderBy(r => r.ReservationDate).ToList();
        var allNodes = (await _nodes.GetAllAsync()).ToDictionary(n => n.Id, n => n);
        foreach (var r in list) {
            if (string.IsNullOrEmpty(r.NodeCode) || string.IsNullOrEmpty(r.NodeName)) {
                if (allNodes.TryGetValue(r.NodeId, out var n)) {
                    r.NodeCode = n.NodeCode;
                    r.NodeName = n.NodeName;
                }
            }
        }
        return list;
    }

    public async Task<EnergyReservation> UpdateAsync(string nic, string id, UpdateReservationDto d) {
        var r = await _res.GetByIdAsync(id) ?? throw new KeyNotFoundException("Reservation not found.");
        var p = (await _pros.GetAllAsync()).FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase)) ?? throw new KeyNotFoundException("Prosumer not found.");
        if (r.ProsumerId != p.Id) throw new UnauthorizedAccessException("Not your reservation.");
        if (r.Status != ReservationStatus.Pending && r.Status != ReservationStatus.Approved) throw new InvalidOperationException("Reservation cannot be modified.");
        var oldDateTime = Combine(r.ReservationDate, r.StartTime);
        if (oldDateTime <= DateTime.UtcNow.AddHours(12)) throw new InvalidOperationException("Modification requires at least 12 hours notice.");
        var s = await _slots.GetByIdAsync(d.EnergySlotId) ?? throw new KeyNotFoundException("Energy slot not found.");
        if (s.Status != SlotStatus.Available || s.AvailableCapacityKwh < d.EnergyAmountKwh) throw new InvalidOperationException("New slot is unavailable.");

        var oldSlotId = r.EnergySlotId;
        var wasApproved = r.Status == ReservationStatus.Approved;

        r.EnergySlotId = s.Id;
        r.NodeId = s.NodeId;
        r.ReservationDate = s.SlotDate;
        r.StartTime = s.StartTime;
        r.EndTime = s.EndTime;
        r.EnergyAmountKwh = Math.Round(d.EnergyAmountKwh, 2);
        r.Status = ReservationStatus.Pending;
        await _res.ReplaceAsync(id, r);

        if (wasApproved) {
            var oldSlot = await _slots.GetByIdAsync(oldSlotId);
            if (oldSlot != null) {
                var node = await _nodes.GetByIdAsync(oldSlot.NodeId);
                var allRes = await _res.GetAllAsync();
                var remainingApprovedKwh = allRes
                    .Where(x => x.EnergySlotId == oldSlot.Id && x.Id != r.Id && (x.Status == ReservationStatus.Approved || x.Status == ReservationStatus.Completed))
                    .Sum(x => x.EnergyAmountKwh);
                oldSlot.AvailableCapacityKwh = Math.Round(Math.Max(0, oldSlot.EnergyAmountKwh - remainingApprovedKwh), 2);
                if (oldSlot.AvailableCapacityKwh > 0 && (node == null || node.Status == NodeStatus.Active)) {
                    oldSlot.Status = SlotStatus.Available;
                }
                oldSlot.UpdatedAt = DateTime.UtcNow;
                await _slots.ReplaceAsync(oldSlot.Id, oldSlot);
            }
        }
        return r;
    }

    public async Task<EnergyReservation> CancelAsync(string nic, string id) {
        var r = await _res.GetByIdAsync(id) ?? throw new KeyNotFoundException("Reservation not found.");
        var p = (await _pros.GetAllAsync()).FirstOrDefault(x => x.NIC.Equals(nic, StringComparison.OrdinalIgnoreCase)) ?? throw new KeyNotFoundException("Prosumer not found.");
        if (r.ProsumerId != p.Id) throw new UnauthorizedAccessException("Not your reservation.");
        if (r.Status == ReservationStatus.Cancelled) throw new InvalidOperationException("Already cancelled.");
        if (Combine(r.ReservationDate, r.StartTime) <= DateTime.UtcNow.AddHours(12)) throw new InvalidOperationException("Cancellation requires at least 12 hours notice.");

        var wasApproved = r.Status == ReservationStatus.Approved;
        r.Status = ReservationStatus.Cancelled;
        r.CancelledAt = DateTime.UtcNow;
        await _res.ReplaceAsync(id, r);

        if (wasApproved) {
            var s = await _slots.GetByIdAsync(r.EnergySlotId);
            if (s != null) {
                var node = await _nodes.GetByIdAsync(s.NodeId);
                var allRes = await _res.GetAllAsync();
                var remainingApprovedKwh = allRes
                    .Where(x => x.EnergySlotId == s.Id && x.Id != r.Id && (x.Status == ReservationStatus.Approved || x.Status == ReservationStatus.Completed))
                    .Sum(x => x.EnergyAmountKwh);
                s.AvailableCapacityKwh = Math.Round(Math.Max(0, s.EnergyAmountKwh - remainingApprovedKwh), 2);
                if (s.AvailableCapacityKwh > 0 && (node == null || node.Status == NodeStatus.Active)) {
                    s.Status = SlotStatus.Available;
                }
                s.UpdatedAt = DateTime.UtcNow;
                await _slots.ReplaceAsync(s.Id, s);
            }
        }
        return r;
    }

    public async Task<EnergyReservation> ApproveAsync(string id) {
        var r = await _res.GetByIdAsync(id) ?? throw new KeyNotFoundException("Reservation not found.");
        if (r.Status != ReservationStatus.Pending) throw new InvalidOperationException("Only pending reservations can be approved.");

        var s = await _slots.GetByIdAsync(r.EnergySlotId) ?? throw new KeyNotFoundException("Energy slot not found.");
        var node = await _nodes.GetByIdAsync(s.NodeId);

        // Check if there is enough available capacity to approve
        var allReservations = await _res.GetAllAsync();
        var currentApprovedKwh = allReservations
            .Where(x => x.EnergySlotId == s.Id && (x.Status == ReservationStatus.Approved || x.Status == ReservationStatus.Completed))
            .Sum(x => x.EnergyAmountKwh);
        var remainingAvailable = Math.Round(Math.Max(0, s.EnergyAmountKwh - currentApprovedKwh), 2);

        if (r.EnergyAmountKwh > remainingAvailable)
            throw new InvalidOperationException($"Cannot approve reservation: requested energy ({r.EnergyAmountKwh} kWh) exceeds remaining available slot capacity ({remainingAvailable} kWh).");

        r.Status = ReservationStatus.Approved;
        r.ApprovedAt = DateTime.UtcNow;
        await _res.ReplaceAsync(id, r);

        // Deduct from slot available capacity upon approval
        var newApprovedKwh = currentApprovedKwh + r.EnergyAmountKwh;
        s.AvailableCapacityKwh = Math.Round(Math.Max(0, s.EnergyAmountKwh - newApprovedKwh), 2);
        if (s.AvailableCapacityKwh <= 0) {
            s.Status = SlotStatus.Reserved;
        } else if (node != null && node.Status == NodeStatus.Active) {
            s.Status = SlotStatus.Available;
        }
        s.UpdatedAt = DateTime.UtcNow;
        await _slots.ReplaceAsync(s.Id, s);

        return r;
    }

    private static DateTime Combine(DateTime date, string time) {
        return DateTime.TryParse($"{date:yyyy-MM-dd} {time}", out var x) ? DateTime.SpecifyKind(x, DateTimeKind.Utc) : date;
    }
}
