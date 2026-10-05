/*
 * File: UserController.cs
 * Description: Smart Solar Microgrid Trading System - UserController.cs module
 * Author: Admin
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.Application.DTOs.Users;
using SmartSolarMicrogrid.Application.Interfaces;

namespace SmartSolarMicrogrid.API.Controllers;

[ApiController]
[Route("api/users")]
[Authorize(Roles = "Backoffice")]
public class UserController : ControllerBase
{
    private readonly IUserService _userService;

    // Method: UserController - executes the relevant logic
    // Method: UserController (Constructor) - initializes the instance
    public UserController(IUserService userService)
    {
        _userService = userService;
    }

    [HttpPost]
    // Method: CreateUser - executes the relevant logic
    public async Task<IActionResult> CreateUser([FromBody] CreateUserDto request)
    {
        if (!ModelState.IsValid)
            // Method: BadRequest - executes the relevant logic
            return BadRequest(ModelState);

        try
        {
            var user = await _userService.CreateUserAsync(request);
            // Method: Ok - executes the relevant logic
            return Ok(user);
        }
        catch (InvalidOperationException ex)
        {
            // Method: BadRequest - executes the relevant logic
            return BadRequest(new { error = ex.Message });
        }
        catch (ArgumentException ex)
        {
            // Method: BadRequest - executes the relevant logic
            return BadRequest(new { error = ex.Message });
        }
    }

    [HttpGet("internal")]
    // Method: GetInternalUsers - executes the relevant logic
    public async Task<IActionResult> GetInternalUsers()
    {
        var users = await _userService.GetInternalUsersAsync();
        // Method: Ok - executes the relevant logic
        return Ok(users);
    }

    [HttpPut("{id}")]
    // Method: UpdateUser - executes the relevant logic
    public async Task<IActionResult> UpdateUser(string id, [FromBody] UpdateUserDto request)
    {
        if (!ModelState.IsValid)
            // Method: BadRequest - executes the relevant logic
            return BadRequest(ModelState);

        try
        {
            var user = await _userService.UpdateUserAsync(id, request);
            // Method: Ok - executes the relevant logic
            return Ok(user);
        }
        catch (KeyNotFoundException ex)
        {
            // Method: NotFound - executes the relevant logic
            return NotFound(new { error = ex.Message });
        }
    }

    [HttpDelete("{id}")]
    // Method: DeleteUser - executes the relevant logic
    public async Task<IActionResult> DeleteUser(string id)
    {
        try
        {
            await _userService.DeleteUserAsync(id);
            // Method: NoContent - executes the relevant logic
            return NoContent();
        }
        catch (KeyNotFoundException ex)
        {
            // Method: NotFound - executes the relevant logic
            return NotFound(new { error = ex.Message });
        }
    }
}
