/*
 * File: UserService.cs
 * Description: Smart Solar Microgrid Trading System - UserService.cs module
 * Author: Admin
 */
using SmartSolarMicrogrid.Application.DTOs.Users;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Domain.Entities;
using SmartSolarMicrogrid.Domain.Enums;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;

namespace SmartSolarMicrogrid.Application.Services;

public class UserService : IUserService
{
    private readonly UserRepository _users;
    private readonly PasswordHasher _hasher;

    // Method: UserService - executes the relevant logic
    // Method: UserService (Constructor) - initializes the instance
    public UserService(UserRepository users, PasswordHasher hasher)
    {
        _users = users;
        _hasher = hasher;
    }

    // Method: CreateUserAsync - executes the relevant logic
    public async Task<UserResponseDto> CreateUserAsync(CreateUserDto request)
    {
        if (request.Role == UserRole.Prosumer)
            // Method: ArgumentException - executes the relevant logic
            throw new ArgumentException("Prosumer accounts cannot be created via the internal user management endpoint.");

        var existingUsers = await _users.GetAllAsync();
        if (existingUsers.Any(u => u.Username.Equals(request.Username, StringComparison.OrdinalIgnoreCase)))
            // Method: InvalidOperationException - executes the relevant logic
            throw new InvalidOperationException("Username already exists.");

        var user = new User
        {
            Id = Guid.NewGuid().ToString(),
            Username = request.Username,
            PasswordHash = _hasher.Hash(request.Password),
            Role = request.Role,
            Status = request.Status,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        await _users.InsertAsync(user);

        return new UserResponseDto
        {
            Id = user.Id,
            Username = user.Username,
            Role = user.Role,
            Status = user.Status,
            CreatedAt = user.CreatedAt
        };
    }

    // Method: GetInternalUsersAsync - executes the relevant logic
    public async Task<IEnumerable<UserResponseDto>> GetInternalUsersAsync()
    {
        var users = await _users.GetAllAsync();
        
        return users
            .Where(u => u.Role != UserRole.Prosumer)
            .Select(u => new UserResponseDto
            {
                Id = u.Id,
                Username = u.Username,
                Role = u.Role,
                Status = u.Status,
                CreatedAt = u.CreatedAt
            })
            .OrderByDescending(u => u.CreatedAt);
    }

    // Method: UpdateUserAsync - executes the relevant logic
    public async Task<UserResponseDto> UpdateUserAsync(string id, UpdateUserDto request)
    {
        var user = await _users.GetByIdAsync(id);
        if (user == null)
            // Method: KeyNotFoundException - executes the relevant logic
            throw new KeyNotFoundException("User not found.");

        if (request.Role.HasValue)
            user.Role = request.Role.Value;
            
        if (request.Status.HasValue)
            user.Status = request.Status.Value;

        if (!string.IsNullOrEmpty(request.Password))
            user.PasswordHash = _hasher.Hash(request.Password);

        user.UpdatedAt = DateTime.UtcNow;

        await _users.ReplaceAsync(id, user);

        return new UserResponseDto
        {
            Id = user.Id,
            Username = user.Username,
            Role = user.Role,
            Status = user.Status,
            CreatedAt = user.CreatedAt
        };
    }

    // Method: DeleteUserAsync - executes the relevant logic
    public async Task DeleteUserAsync(string id)
    {
        var user = await _users.GetByIdAsync(id);
        if (user == null)
            // Method: KeyNotFoundException - executes the relevant logic
            throw new KeyNotFoundException("User not found.");
            
        await _users.DeleteAsync(id);
    }
}
