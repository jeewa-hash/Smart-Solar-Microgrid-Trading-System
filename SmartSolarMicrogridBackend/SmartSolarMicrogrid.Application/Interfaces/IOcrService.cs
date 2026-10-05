/*
 * File: IOcrService.cs
 * Description: Smart Solar Microgrid Trading System - IOcrService.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.Interfaces;
public interface IOcrService {
    // Method: ExtractNicFromImageAsync - executes the relevant logic
    Task<string?> ExtractNicFromImageAsync(string base64Image, string expectedNic);
}
