/*
 * File: IMicrogridNodeService.cs
 * Description: Smart Solar Microgrid Trading System - IMicrogridNodeService.cs module
 * Author: Admin
 */
using SmartSolarMicrogrid.Application.DTOs.Microgrid;
using SmartSolarMicrogrid.Domain.Entities;

namespace SmartSolarMicrogrid.Application.Interfaces;

public interface IMicrogridNodeService
{
    // Method: CreateAsync - executes the relevant logic
    Task<MicrogridNode> CreateAsync(CreateMicrogridNodeDto dto);
    // Method: GetAllAsync - executes the relevant logic
    Task<IReadOnlyList<MicrogridNode>> GetAllAsync(bool activeOnly = false);
    // Method: GetAsync - executes the relevant logic
    Task<MicrogridNode?> GetAsync(string id);
    // Method: UpdateAsync - executes the relevant logic
    Task<MicrogridNode> UpdateAsync(string id, UpdateMicrogridNodeDto dto);
    // Method: DeactivateAsync - executes the relevant logic
    Task<MicrogridNode> DeactivateAsync(string id, string? reason = null);
    // Method: ActivateAsync - executes the relevant logic
    Task<MicrogridNode> ActivateAsync(string id);
    // Method: UpdateScheduleAsync - executes the relevant logic
    Task<MicrogridNode> UpdateScheduleAsync(string id, string start, string end);
}