/*
 * File: UpdateEnergySlotDto.cs
 * Description: Smart Solar Microgrid Trading System - UpdateEnergySlotDto.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.DTOs.EnergySlot;
public record UpdateEnergySlotDto(DateTime SlotDate, string StartTime, string EndTime, double EnergyAmountKwh, double AvailableCapacityKwh, string Status);
