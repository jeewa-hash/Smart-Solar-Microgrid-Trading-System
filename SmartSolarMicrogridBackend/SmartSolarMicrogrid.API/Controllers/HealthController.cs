/*
 * File: HealthController.cs
 * Description: Smart Solar Microgrid Trading System - HealthController.cs module
 * Author: Admin
 */
using Microsoft.AspNetCore.Mvc;
namespace SmartSolarMicrogrid.API.Controllers;
[ApiController,Route("api/health")]
public class HealthController:ControllerBase {
    // Method: Get - executes the relevant logic
    [HttpGet] public IActionResult Get()=>Ok(new{status="ok",service="SmartSolarMicrogridAPI",utc=DateTime.UtcNow});
}
