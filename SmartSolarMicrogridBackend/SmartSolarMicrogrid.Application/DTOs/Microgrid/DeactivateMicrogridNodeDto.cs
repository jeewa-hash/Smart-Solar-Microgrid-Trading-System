/*
 * File: DeactivateMicrogridNodeDto.cs
 * Description: Smart Solar Microgrid Trading System - DeactivateMicrogridNodeDto.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.DTOs.Microgrid;

public record DeactivateMicrogridNodeDto(string? Reason = null, string? AdminNote = null);
