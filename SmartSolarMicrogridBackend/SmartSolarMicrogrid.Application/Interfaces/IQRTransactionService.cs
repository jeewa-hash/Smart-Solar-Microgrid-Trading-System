/*
 * File: IQRTransactionService.cs
 * Description: Smart Solar Microgrid Trading System - IQRTransactionService.cs module
 * Author: Admin
 */
using SmartSolarMicrogrid.Application.DTOs.QR; using SmartSolarMicrogrid.Domain.Entities; namespace SmartSolarMicrogrid.Application.Interfaces; public interface IQRTransactionService { Task<QRTransaction> GenerateAsync(string reservationId); Task<object> VerifyAsync(VerifyQRDto dto); Task<object> CompleteAsync(VerifyQRDto dto); }