/*
 * File: IDrpVerificationService.cs
 * Description: Smart Solar Microgrid Trading System - IDrpVerificationService.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Application.Interfaces;

public record DrpVerificationResult(
    bool IsVerified,
    string ReferenceNumber,
    string? CitizenFullName,
    string? Gender,
    DateTime? DateOfBirth,
    int? Age,
    string Message
);

public interface IDrpVerificationService {
    // Method: VerifyNicAsync - executes the relevant logic
    Task<DrpVerificationResult> VerifyNicAsync(string nic, string fullName);
}
