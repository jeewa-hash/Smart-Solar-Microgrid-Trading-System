using SmartSolarMicrogrid.Application.DTOs.EnergySlot;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;

namespace SmartSolarMicrogrid.Application.Services;

public class EnergySlotService : IEnergySlotService {
    private readonly EnergySlotRepository _slots;
    private readonly MicrogridNodeRepository _nodes;
    private readonly EnergyReservationRepository _res;

    public EnergySlotService(EnergySlotRepository slots, MicrogridNodeRepository nodes, EnergyReservationRepository res) {
        _slots = slots;
        _nodes = nodes;
        _res = res;
    }

    public async Task<EnergySlot> CreateAsync(CreateEnergySlotDto d) {
        var node = await _nodes.GetByIdAsync(d.NodeId) ?? throw new KeyNotFoundException("Node not found.");
        if (node.Status != NodeStatus.Active) throw new InvalidOperationException("Cannot create slot for inactive node.");
        Validation.Positive(d.EnergyAmountKwh, "EnergyAmountKwh");
        if (d.EnergyAmountKwh > node.CapacityKw)
            throw new ArgumentException($"Slot energy amount ({d.EnergyAmountKwh} kWh) cannot exceed node capacity ({node.CapacityKw} kW) for {node.NodeCode} ({node.NodeName}).");
        if (d.SlotDate.Date < DateTime.UtcNow.Date || d.SlotDate.Date > DateTime.UtcNow.Date.AddDays(7))
            throw new ArgumentException("Slot must be within the allowed 7-day period.");

        var s = new EnergySlot {
            Id = Guid.NewGuid().ToString(),
            NodeId = d.NodeId,
            SlotDate = d.SlotDate.Date,
            StartTime = d.StartTime,
            EndTime = d.EndTime,
            EnergyAmountKwh = Math.Round(d.EnergyAmountKwh, 2),
            AvailableCapacityKwh = Math.Round(d.EnergyAmountKwh, 2),
            Status = SlotStatus.Available,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };
        await _slots.InsertAsync(s);
        return s;
    }

    public async Task<IReadOnlyList<EnergySlot>> GetAsync(string? nodeId = null, bool availableOnly = false) {
        var slots = await _slots.GetAllAsync();
        var allReservations = await _res.GetAllAsync();
        var approvedBySlot = allReservations
            .Where(r => r.Status == ReservationStatus.Approved || r.Status == ReservationStatus.Completed)
            .GroupBy(r => r.EnergySlotId)
            .ToDictionary(g => g.Key, g => g.Sum(r => r.EnergyAmountKwh));
        var allNodes = (await _nodes.GetAllAsync()).ToDictionary(n => n.Id, n => n);

        foreach (var s in slots) {
            approvedBySlot.TryGetValue(s.Id, out var approvedKwh);
            var expectedAvailable = Math.Round(Math.Max(0, s.EnergyAmountKwh - approvedKwh), 2);
            allNodes.TryGetValue(s.NodeId, out var node);
            var isNodeInactive = node != null && node.Status != NodeStatus.Active;

            bool changed = false;
            if (Math.Abs(s.AvailableCapacityKwh - expectedAvailable) > 0.001) {
                s.AvailableCapacityKwh = expectedAvailable;
                changed = true;
            }

            if (isNodeInactive) {
                if (s.Status != SlotStatus.Unavailable) {
                    s.Status = SlotStatus.Unavailable;
                    changed = true;
                }
            } else if (s.AvailableCapacityKwh <= 0) {
                if (s.Status != SlotStatus.Reserved && s.Status != SlotStatus.Unavailable) {
                    s.Status = SlotStatus.Reserved;
                    changed = true;
                }
            } else if (s.Status == SlotStatus.Reserved && s.AvailableCapacityKwh > 0) {
                s.Status = SlotStatus.Available;
                changed = true;
            }

            if (changed) {
                s.UpdatedAt = DateTime.UtcNow;
                await _slots.ReplaceAsync(s.Id, s);
            }
        }

        var x = slots.AsEnumerable();
        if (nodeId is not null) x = x.Where(s => s.NodeId == nodeId);
        if (availableOnly) x = x.Where(s => s.Status == SlotStatus.Available && s.AvailableCapacityKwh > 0);
        return x.OrderBy(s => s.SlotDate).ThenBy(s => s.StartTime).ToList();
    }

    public async Task<EnergySlot> UpdateAsync(string id, UpdateEnergySlotDto d) {
        var s = await _slots.GetByIdAsync(id) ?? throw new KeyNotFoundException("Slot not found.");
        var node = await _nodes.GetByIdAsync(s.NodeId);
        Validation.Positive(d.EnergyAmountKwh, "EnergyAmountKwh");
        if (node != null && d.EnergyAmountKwh > node.CapacityKw)
            throw new ArgumentException($"Slot energy amount ({d.EnergyAmountKwh} kWh) cannot exceed node capacity ({node.CapacityKw} kW) for {node.NodeCode} ({node.NodeName}).");

        var reservations = await _res.GetAllAsync();
        var approvedKwh = reservations
            .Where(r => r.EnergySlotId == id && (r.Status == ReservationStatus.Approved || r.Status == ReservationStatus.Completed))
            .Sum(r => r.EnergyAmountKwh);

        if (d.EnergyAmountKwh < approvedKwh)
            throw new ArgumentException($"Slot total energy ({d.EnergyAmountKwh} kWh) cannot be less than already approved reservations ({approvedKwh} kWh).");

        s.EnergyAmountKwh = Math.Round(d.EnergyAmountKwh, 2);
        // Calculation: Total energy minus approved reservations
        s.AvailableCapacityKwh = Math.Round(Math.Max(0, s.EnergyAmountKwh - approvedKwh), 2);

        if (Enum.TryParse<SlotStatus>(d.Status, true, out var st)) {
            if (node != null && node.Status != NodeStatus.Active && st == SlotStatus.Available)
                throw new InvalidOperationException($"Cannot set slot to Available because microgrid node '{node.NodeName}' is deactivated ({node.AdminNote ?? "Inactive"}).");
            
            if (s.AvailableCapacityKwh <= 0 && st == SlotStatus.Available) {
                s.Status = SlotStatus.Reserved;
            } else {
                s.Status = st;
            }
        } else {
            throw new ArgumentException("Invalid slot status.");
        }

        s.SlotDate = d.SlotDate.Date;
        s.StartTime = d.StartTime;
        s.EndTime = d.EndTime;
        s.UpdatedAt = DateTime.UtcNow;
        await _slots.ReplaceAsync(id, s);
        return s;
    }

    public async Task<EnergySlot> UpdateAvailabilityAsync(string id, double available) {
        var s = await _slots.GetByIdAsync(id) ?? throw new KeyNotFoundException("Slot not found.");
        var node = await _nodes.GetByIdAsync(s.NodeId);
        if (node != null && node.Status != NodeStatus.Active && available > 0)
            throw new InvalidOperationException($"Cannot make slot available because microgrid node '{node.NodeName}' is deactivated ({node.AdminNote ?? "Inactive"}).");

        var reservations = await _res.GetAllAsync();
        var approvedKwh = reservations
            .Where(r => r.EnergySlotId == id && (r.Status == ReservationStatus.Approved || r.Status == ReservationStatus.Completed))
            .Sum(r => r.EnergyAmountKwh);

        var maxAvailable = Math.Round(Math.Max(0, s.EnergyAmountKwh - approvedKwh), 2);
        if (available < 0 || available > maxAvailable)
            throw new ArgumentException($"Available capacity must be between 0 and {maxAvailable} kWh ({approvedKwh} kWh is locked by approved reservations).");
        if (node != null && available > node.CapacityKw)
            throw new ArgumentException($"Available capacity ({available} kWh) cannot exceed node capacity ({node.CapacityKw} kW) for {node.NodeCode} ({node.NodeName}).");

        s.AvailableCapacityKwh = Math.Round(available, 2);
        s.Status = s.AvailableCapacityKwh > 0 ? SlotStatus.Available : SlotStatus.Unavailable;
        s.UpdatedAt = DateTime.UtcNow;
        await _slots.ReplaceAsync(id, s);
        return s;
    }
}
