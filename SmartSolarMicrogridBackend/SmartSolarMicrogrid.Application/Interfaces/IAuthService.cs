/*
 * File: IAuthService.cs
 * Description: Smart Solar Microgrid Trading System - IAuthService.cs module
 * Author: Admin
 */
using SmartSolarMicrogrid.Application.DTOs.Auth; namespace SmartSolarMicrogrid.Application.Interfaces; public interface IAuthService { Task<LoginResponseDto> LoginAsync(LoginRequestDto request); }