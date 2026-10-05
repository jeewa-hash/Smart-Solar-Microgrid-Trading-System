/*
 * File: PasswordHasher.cs
 * Description: Smart Solar Microgrid Trading System - PasswordHasher.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogrid.Infrastructure.Security;
public class PasswordHasher {
    // Method: Hash - executes the relevant logic
    public string Hash(string password) => BCrypt.Net.BCrypt.HashPassword(password);
    // Method: Verify - executes the relevant logic
    public bool Verify(string password,string hash) => BCrypt.Net.BCrypt.Verify(password,hash);
}
