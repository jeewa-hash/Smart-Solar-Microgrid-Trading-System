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

    public UserService(UserRepository users, PasswordHasher hasher)
    {
        _users = users;
        _hasher = hasher;
    }

    public async Task<UserResponseDto> CreateUserAsync(CreateUserDto request)
    {
        if (request.Role == UserRole.Prosumer)
            throw new ArgumentException("Prosumer accounts cannot be created via the internal user management endpoint.");

        var existingUsers = await _users.GetAllAsync();
        if (existingUsers.Any(u => u.Username.Equals(request.Username, StringComparison.OrdinalIgnoreCase)))
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
}
