/*
 * File: IDashboardService.cs
 * Description: Smart Solar Microgrid Trading System - IDashboardService.cs module
 * Author: Admin
 */
using SmartSolarMicrogrid.Application.DTOs.Dashboard; namespace SmartSolarMicrogrid.Application.Interfaces; public interface IDashboardService { Task<ProsumerDashboardDto> ProsumerAsync(string nic); Task<OperatorDashboardDto> OperatorAsync(); Task<BackofficeDashboardDto> BackofficeAsync(); }