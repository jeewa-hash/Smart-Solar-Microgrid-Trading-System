/*
 * File: CreateEnergySlotDto.cs
 * Description: Smart Solar Microgrid Trading System - CreateEnergySlotDto.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.DTOs.EnergySlot;
public record CreateEnergySlotDto(string NodeId, DateTime SlotDate, string StartTime, string EndTime, double EnergyAmountKwh);
