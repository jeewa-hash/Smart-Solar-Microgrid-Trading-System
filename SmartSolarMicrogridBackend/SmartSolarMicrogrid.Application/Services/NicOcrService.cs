/*
 * File: NicOcrService.cs
 * Description: Smart Solar Microgrid Trading System - NicOcrService.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.Services;
using SmartSolarMicrogrid.Application.Interfaces;

public class NicOcrService : IOcrService {
    // Method: ExtractNicFromImageAsync - executes the relevant logic
    public async Task<string?> ExtractNicFromImageAsync(string base64Image, string expectedNic) {
        if (string.IsNullOrWhiteSpace(base64Image)) return null;

        // Simulate OCR processing time
        await Task.Delay(500);

        // 90% chance it extracts correctly, 10% chance it makes a mistake (simulating real OCR quirks)
        bool mockFailure = new Random().Next(0, 10) > 8; 
        
        if (mockFailure && expectedNic.Length > 2) {
            return expectedNic.Substring(0, expectedNic.Length - 1) + (expectedNic.EndsWith("V", StringComparison.OrdinalIgnoreCase) ? "X" : "V");
        }
        
        return expectedNic;
    }
}
