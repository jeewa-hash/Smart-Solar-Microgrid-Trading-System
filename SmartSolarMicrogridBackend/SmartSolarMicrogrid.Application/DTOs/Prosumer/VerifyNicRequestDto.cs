/*
 * File: VerifyNicRequestDto.cs
 * Description: Smart Solar Microgrid Trading System - VerifyNicRequestDto.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.DTOs.Prosumer;

public record VerifyNicRequestDto(string NIC, string? FullName);
