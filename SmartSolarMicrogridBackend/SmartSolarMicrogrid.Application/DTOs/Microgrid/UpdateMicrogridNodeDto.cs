/*
 * File: UpdateMicrogridNodeDto.cs
 * Description: Smart Solar Microgrid Trading System - UpdateMicrogridNodeDto.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.DTOs.Microgrid;
public record UpdateMicrogridNodeDto(string NodeName, double Latitude, double Longitude, double CapacityKw, int BatterySlotAvailability, string ScheduleStart, string ScheduleEnd);
