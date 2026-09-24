using SmartSolarMicrogrid.Application.DTOs.Reservation;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Application.Validators;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
namespace SmartSolarMicrogrid.Application.Services;
public class ReservationService : IReservationService {
    private readonly EnergyReservationRepository _res; private readonly ProsumerRepository _pros; private readonly EnergySlotRepository _slots; private readonly MicrogridNodeRepository _nodes;
    public ReservationService(EnergyReservationRepository res,ProsumerRepository pros,EnergySlotRepository slots,MicrogridNodeRepository nodes){_res=res;_pros=pros;_slots=slots;_nodes=nodes;}
    public async Task<EnergyReservation> CreateAsync(string nic,CreateReservationDto d){
        var p=(await _pros.GetAllAsync()).FirstOrDefault(x=>x.NIC.Equals(nic,StringComparison.OrdinalIgnoreCase))??throw new KeyNotFoundException("Prosumer not found.");
        if(p.AccountStatus!=UserStatus.Active)throw new UnauthorizedAccessException("Prosumer account is not active.");
        var s=await _slots.GetByIdAsync(d.EnergySlotId)??throw new KeyNotFoundException("Energy slot not found.");
        var node=await _nodes.GetByIdAsync(s.NodeId)??throw new KeyNotFoundException("Node not found.");
        if(node.Status!=NodeStatus.Active||s.Status!=SlotStatus.Available||s.AvailableCapacityKwh<=0)throw new InvalidOperationException("Slot is unavailable.");
        if(s.SlotDate.Date<DateTime.UtcNow.Date||s.SlotDate.Date>DateTime.UtcNow.Date.AddDays(7))throw new ArgumentException("Reservation must be within the specified 7-day period.");
        Validation.Positive(d.EnergyAmountKwh,"EnergyAmountKwh");
        if(d.EnergyAmountKwh>s.AvailableCapacityKwh)throw new InvalidOperationException("Requested energy exceeds available capacity.");
        var duplicate=(await _res.GetAllAsync()).Any(r=>r.EnergySlotId==s.Id && r.Status!=ReservationStatus.Cancelled);
        if(duplicate)throw new InvalidOperationException("This slot already has an active reservation.");
        var r=new EnergyReservation{Id=Guid.NewGuid().ToString(),ReservationCode="RES-"+Random.Shared.Next(1000,9999),ProsumerId=p.Id,NodeId=node.Id,EnergySlotId=s.Id,ReservationDate=s.SlotDate,StartTime=s.StartTime,EndTime=s.EndTime,EnergyAmountKwh=d.EnergyAmountKwh};
        s.AvailableCapacityKwh-=d.EnergyAmountKwh;s.Status=SlotStatus.Reserved;s.UpdatedAt=DateTime.UtcNow;
        await _res.InsertAsync(r);await _slots.ReplaceAsync(s.Id,s);return r;
    }
    public Task<EnergyReservation?> GetAsync(string id)=>_res.GetByIdAsync(id);
    public async Task<IReadOnlyList<EnergyReservation>> GetMineAsync(string nic,string? search=null,string? status=null){
        var p=(await _pros.GetAllAsync()).FirstOrDefault(x=>x.NIC.Equals(nic,StringComparison.OrdinalIgnoreCase))??throw new KeyNotFoundException("Prosumer not found.");
        var x=(await _res.GetAllAsync()).Where(r=>r.ProsumerId==p.Id);
        if(!string.IsNullOrWhiteSpace(search))x=x.Where(r=>r.ReservationCode.Contains(search,StringComparison.OrdinalIgnoreCase)||r.NodeId.Contains(search,StringComparison.OrdinalIgnoreCase));
        if(!string.IsNullOrWhiteSpace(status)&&Enum.TryParse<ReservationStatus>(status,true,out var st))x=x.Where(r=>r.Status==st);
        return x.OrderByDescending(r=>r.ReservationDate).ToList();
    }
    public async Task<IReadOnlyList<EnergyReservation>> GetByStatusAsync(string status){if(!Enum.TryParse<ReservationStatus>(status,true,out var st))throw new ArgumentException("Invalid status.");return (await _res.GetAllAsync()).Where(r=>r.Status==st).OrderBy(r=>r.ReservationDate).ToList();}
    public async Task<EnergyReservation> UpdateAsync(string nic,string id,UpdateReservationDto d){
        var r=await _res.GetByIdAsync(id)??throw new KeyNotFoundException("Reservation not found.");
        var p=(await _pros.GetAllAsync()).FirstOrDefault(x=>x.NIC.Equals(nic,StringComparison.OrdinalIgnoreCase))??throw new KeyNotFoundException("Prosumer not found.");
        if(r.ProsumerId!=p.Id)throw new UnauthorizedAccessException("Not your reservation.");
        if(r.Status!=ReservationStatus.Pending&&r.Status!=ReservationStatus.Approved)throw new InvalidOperationException("Reservation cannot be modified.");
        var oldDateTime=Combine(r.ReservationDate,r.StartTime);
        if(oldDateTime<=DateTime.UtcNow.AddHours(12))throw new InvalidOperationException("Modification requires at least 12 hours notice.");
        var s=await _slots.GetByIdAsync(d.EnergySlotId)??throw new KeyNotFoundException("Energy slot not found.");
        if(s.Status!=SlotStatus.Available||s.AvailableCapacityKwh<d.EnergyAmountKwh)throw new InvalidOperationException("New slot is unavailable.");
        r.EnergySlotId=s.Id;r.NodeId=s.NodeId;r.ReservationDate=s.SlotDate;r.StartTime=s.StartTime;r.EndTime=s.EndTime;r.EnergyAmountKwh=d.EnergyAmountKwh;r.Status=ReservationStatus.Pending;
        await _res.ReplaceAsync(id,r);return r;
    }
    public async Task<EnergyReservation> CancelAsync(string nic,string id){
        var r=await _res.GetByIdAsync(id)??throw new KeyNotFoundException("Reservation not found.");
        var p=(await _pros.GetAllAsync()).FirstOrDefault(x=>x.NIC.Equals(nic,StringComparison.OrdinalIgnoreCase))??throw new KeyNotFoundException("Prosumer not found.");
        if(r.ProsumerId!=p.Id)throw new UnauthorizedAccessException("Not your reservation.");
        if(r.Status==ReservationStatus.Cancelled)throw new InvalidOperationException("Already cancelled.");
        if(Combine(r.ReservationDate,r.StartTime)<=DateTime.UtcNow.AddHours(12))throw new InvalidOperationException("Cancellation requires at least 12 hours notice.");
        r.Status=ReservationStatus.Cancelled;r.CancelledAt=DateTime.UtcNow;await _res.ReplaceAsync(id,r);
        var s=await _slots.GetByIdAsync(r.EnergySlotId);if(s is not null){s.Status=SlotStatus.Available;s.AvailableCapacityKwh+=r.EnergyAmountKwh;await _slots.ReplaceAsync(s.Id,s);}return r;
    }
    public async Task<EnergyReservation> ApproveAsync(string id){
        var r=await _res.GetByIdAsync(id)??throw new KeyNotFoundException("Reservation not found.");
        if(r.Status!=ReservationStatus.Pending)throw new InvalidOperationException("Only pending reservations can be approved.");
        r.Status=ReservationStatus.Approved;r.ApprovedAt=DateTime.UtcNow;await _res.ReplaceAsync(id,r);return r;
    }
    private static DateTime Combine(DateTime date,string time){return DateTime.TryParse($"{date:yyyy-MM-dd} {time}",out var x)?DateTime.SpecifyKind(x,DateTimeKind.Utc):date;}
}
