/*
 * File: LoginResponseDto.cs
 * Description: Smart Solar Microgrid Trading System - LoginResponseDto.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.DTOs.Auth;
public record LoginResponseDto(string Token, string UserId, string Username, string Role, string Status, string Nic = "");
