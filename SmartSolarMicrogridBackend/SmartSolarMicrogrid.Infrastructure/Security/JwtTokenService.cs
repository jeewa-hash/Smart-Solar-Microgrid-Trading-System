/*
 * File: JwtTokenService.cs
 * Description: Smart Solar Microgrid Trading System - JwtTokenService.cs module
 * Author: Admin
 */
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.Security;
public class JwtTokenService {
    private readonly IConfiguration _config;
    // Method: JwtTokenService - executes the relevant logic
    // Method: JwtTokenService (Constructor) - initializes the instance
    public JwtTokenService(IConfiguration config) => _config = config;
    // Method: Create - executes the relevant logic
    public string Create(User user) {
        var key = _config["Jwt:Key"] ?? throw new InvalidOperationException("JWT key missing.");
        var claims = new[] {
            // Method: Claim - executes the relevant logic
            new Claim(JwtRegisteredClaimNames.Sub,user.Id),
            // Method: Claim - executes the relevant logic
            new Claim(ClaimTypes.Name,user.Username),
            // Method: Claim - executes the relevant logic
            new Claim(ClaimTypes.Role,user.Role.ToString())
        };
        var credentials = new SigningCredentials(new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key)),SecurityAlgorithms.HmacSha256);
        var token = new JwtSecurityToken(
            issuer:_config["Jwt:Issuer"], audience:_config["Jwt:Audience"],
            claims:claims, expires:DateTime.UtcNow.AddHours(8), signingCredentials:credentials);
        // Method: JwtSecurityTokenHandler - executes the relevant logic
        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
