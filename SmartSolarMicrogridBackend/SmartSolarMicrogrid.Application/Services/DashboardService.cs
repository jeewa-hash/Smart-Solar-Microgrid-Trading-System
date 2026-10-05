/*
 * File: DashboardService.cs
 * Description: Smart Solar Microgrid Trading System - DashboardService.cs module
 * Author: Admin
 */
using SmartSolarMicrogrid.Application.DTOs.Dashboard;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
namespace SmartSolarMicrogrid.Application.Services;
public class DashboardService : IDashboardService {
    private readonly ProsumerRepository _p; private readonly EnergyReservationRepository _r; private readonly MicrogridNodeRepository _n; private readonly EnergySlotRepository _s;
    // Method: DashboardService - executes the relevant logic
    // Method: DashboardService (Constructor) - initializes the instance
    public DashboardService(ProsumerRepository p,EnergyReservationRepository r,MicrogridNodeRepository n,EnergySlotRepository s){_p=p;_r=r;_n=n;_s=s;}
    // Method: ProsumerAsync - executes the relevant logic
    public async Task<ProsumerDashboardDto> ProsumerAsync(string nic){
        var p=(await _p.GetAllAsync()).FirstOrDefault(x=>x.NIC.Equals(nic,StringComparison.OrdinalIgnoreCase))??throw new KeyNotFoundException("Prosumer not found.");
        var r=(await _r.GetAllAsync()).Where(x=>x.ProsumerId==p.Id).ToList();var nodes=await _n.GetAllAsync();
        var upcoming=r.Where(x=>x.Status==ReservationStatus.Approved).OrderBy(x=>x.ReservationDate).FirstOrDefault();
        // Method: new - executes the relevant logic
        return new(r.Count(x=>x.Status==ReservationStatus.Approved),r.Count(x=>x.Status==ReservationStatus.Pending),r.Count(x=>x.Status==ReservationStatus.Completed),upcoming,nodes.Cast<object>().ToList());
    }
    // Method: OperatorAsync - executes the relevant logic
    public async Task<OperatorDashboardDto> OperatorAsync(){
        var r=await _r.GetAllAsync();var s=await _s.GetAllAsync();return new(r.Count(x=>x.Status==ReservationStatus.Pending),r.Count(x=>x.Status==ReservationStatus.Approved),r.Count(x=>x.ReservationDate.Date==DateTime.UtcNow.Date),s.Count(x=>x.Status==SlotStatus.Available),new List<object>());
    }
    // Method: BackofficeAsync - executes the relevant logic
    public async Task<BackofficeDashboardDto> BackofficeAsync(){
        var p=await _p.GetAllAsync();var n=await _n.GetAllAsync();var s=await _s.GetAllAsync();var r=await _r.GetAllAsync();
        // Method: new - executes the relevant logic
        return new(p.Count(x=>x.AccountStatus==UserStatus.Active),p.Count(x=>x.AccountStatus==UserStatus.Pending),n.Count,s.Count(x=>x.Status==SlotStatus.Available),r.Count(x=>x.Status==ReservationStatus.Pending),r.Count(x=>x.Status==ReservationStatus.Approved),r.Count(x=>x.Status==ReservationStatus.Completed));
    }
}
