using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Microgrid;
using SmartSolarMicrogrid.Application.Interfaces;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/microgrid-nodes")]
public class MicrogridNodesController(IMicrogridNodeService service):ControllerBase {
    [Authorize(Roles="Backoffice,GridOperator"),HttpPost] public async Task<IActionResult> Create(CreateMicrogridNodeDto d)=>Ok(await service.CreateAsync(d));
    [Authorize,HttpGet] public async Task<IActionResult> GetAll([FromQuery]bool activeOnly=false)=>Ok(await service.GetAllAsync(activeOnly));
    [Authorize,HttpGet("{id}")] public async Task<IActionResult> Get(string id)=>Ok(await service.GetAsync(id));
    [Authorize(Roles="Backoffice,GridOperator"),HttpPut("{id}")] public async Task<IActionResult> Update(string id,UpdateMicrogridNodeDto d)=>Ok(await service.UpdateAsync(id,d));
    [Authorize(Roles="Backoffice,GridOperator"),HttpPut("{id}/schedule")] public async Task<IActionResult> Schedule(string id,[FromQuery]string start,[FromQuery]string end)=>Ok(await service.UpdateScheduleAsync(id,start,end));
    [Authorize(Roles="Backoffice,GridOperator"),HttpPut("{id}/deactivate")] public async Task<IActionResult> Deactivate(string id)=>Ok(await service.DeactivateAsync(id));
}
