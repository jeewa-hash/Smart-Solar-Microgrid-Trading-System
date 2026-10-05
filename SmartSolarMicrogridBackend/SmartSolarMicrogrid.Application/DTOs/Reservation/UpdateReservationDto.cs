/*
 * File: UpdateReservationDto.cs
 * Description: Smart Solar Microgrid Trading System - UpdateReservationDto.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.DTOs.Reservation;
public record UpdateReservationDto(string EnergySlotId, double EnergyAmountKwh);
