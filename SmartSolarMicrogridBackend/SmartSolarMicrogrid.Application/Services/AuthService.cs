using SmartSolarMicrogrid.Application.DTOs.Auth;
using SmartSolarMicrogrid.Application.Interfaces;
using SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
using SmartSolarMicrogrid.Infrastructure.Security;
namespace SmartSolarMicrogrid.Application.Services;
public class AuthService : IAuthService {
    private readonly UserRepository _users; private readonly PasswordHasher _hasher; private readonly JwtTokenService _jwt;
    public AuthService(UserRepository users,PasswordHasher hasher,JwtTokenService jwt){_users=users;_hasher=hasher;_jwt=jwt;}
    public async Task<LoginResponseDto> LoginAsync(LoginRequestDto request){
        var user=(await _users.GetAllAsync()).FirstOrDefault(x=>x.Username.Equals(request.Username,StringComparison.OrdinalIgnoreCase));
        if(user is null || !_hasher.Verify(request.Password,user.PasswordHash)) throw new UnauthorizedAccessException("Invalid username or password.");
        if(user.Status != SmartSolarMicrogrid.Domain.Enums.UserStatus.Active) throw new UnauthorizedAccessException("Account is not active.");
        return new LoginResponseDto(_jwt.Create(user),user.Id,user.Username,user.Role.ToString(),user.Status.ToString());
    }
}
