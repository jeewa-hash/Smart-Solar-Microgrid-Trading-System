/*
 * File: MicrogridNodesController.cs
 * Description: Smart Solar Microgrid Trading System - MicrogridNodesController.cs module
 * Author: Admin
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Microgrid;
using SmartSolarMicrogrid.Application.Interfaces;

namespace SmartSolarMicrogrid.API.Controllers;

// Method: Route - executes the relevant logic
[ApiController, Route("api/microgrid-nodes")]
public class MicrogridNodesController(IMicrogridNodeService service) : ControllerBase {
    [Authorize(Roles = "Backoffice"), HttpPost]
    // Method: Create - executes the relevant logic
    public async Task<IActionResult> Create(CreateMicrogridNodeDto d) => Ok(await service.CreateAsync(d));

    [Authorize, HttpGet]
    // Method: GetAll - executes the relevant logic
    public async Task<IActionResult> GetAll([FromQuery] bool activeOnly = false) => Ok(await service.GetAllAsync(activeOnly));

    // Method: HttpGet - executes the relevant logic
    [Authorize, HttpGet("{id}")]
    // Method: Get - executes the relevant logic
    public async Task<IActionResult> Get(string id) => Ok(await service.GetAsync(id));

    [Authorize(Roles = "Backoffice"), HttpPut("{id}")]
    // Method: Update - executes the relevant logic
    public async Task<IActionResult> Update(string id, UpdateMicrogridNodeDto d) => Ok(await service.UpdateAsync(id, d));

    [Authorize(Roles = "Backoffice"), HttpPut("{id}/schedule")]
    // Method: Schedule - executes the relevant logic
    public async Task<IActionResult> Schedule(string id, [FromQuery] string start, [FromQuery] string end) => Ok(await service.UpdateScheduleAsync(id, start, end));

    [Authorize(Roles = "Backoffice"), HttpPut("{id}/deactivate")]
    // Method: Deactivate - executes the relevant logic
    public async Task<IActionResult> Deactivate(string id, [FromBody] DeactivateMicrogridNodeDto? dto = null, [FromQuery] string? reason = null)
    {
        try {
            var r = dto?.Reason ?? dto?.AdminNote ?? reason;
            // Method: Ok - executes the relevant logic
            return Ok(await service.DeactivateAsync(id, r));
        } catch (Exception e) {
            // Method: BadRequest - executes the relevant logic
            return BadRequest(new { error = e.Message });
        }
    }

    [Authorize(Roles = "Backoffice"), HttpPut("{id}/activate")]
    // Method: Activate - executes the relevant logic
    public async Task<IActionResult> Activate(string id)
    {
        try {
            // Method: Ok - executes the relevant logic
            return Ok(await service.ActivateAsync(id));
        } catch (Exception e) {
            // Method: BadRequest - executes the relevant logic
            return BadRequest(new { error = e.Message });
        }
    }
}
