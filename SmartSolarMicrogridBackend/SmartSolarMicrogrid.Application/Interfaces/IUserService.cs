/*
 * File: IUserService.cs
 * Description: Smart Solar Microgrid Trading System - IUserService.cs module
 * Author: Admin
 */
using SmartSolarMicrogrid.Application.DTOs.Users;

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IUserService
{
    // Method: CreateUserAsync - executes the relevant logic
    Task<UserResponseDto> CreateUserAsync(CreateUserDto request);
    // Method: GetInternalUsersAsync - executes the relevant logic
    Task<IEnumerable<UserResponseDto>> GetInternalUsersAsync();
    // Method: UpdateUserAsync - executes the relevant logic
    Task<UserResponseDto> UpdateUserAsync(string id, UpdateUserDto request);
    // Method: DeleteUserAsync - executes the relevant logic
    Task DeleteUserAsync(string id);
}
