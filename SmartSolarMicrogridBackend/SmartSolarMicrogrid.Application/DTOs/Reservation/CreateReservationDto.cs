/*
 * File: CreateReservationDto.cs
 * Description: Smart Solar Microgrid Trading System - CreateReservationDto.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.DTOs.Reservation;
public record CreateReservationDto(string EnergySlotId, double EnergyAmountKwh);
