/*
 * File: ValidationTests.cs
 * Description: Smart Solar Microgrid Trading System - ValidationTests.cs module
 * Author: Admin
 */
using SmartSolarMicrogrid.Application.Validators;
using Xunit;
namespace SmartSolarMicrogrid.Tests;
public class ValidationTests {
    // Method: PositiveRejectsZero - executes the relevant logic
    [Fact] public void PositiveRejectsZero()=>Assert.Throws<ArgumentException>(()=>Validation.Positive(0,"x"));
    // Method: CoordinatesRejectInvalid - executes the relevant logic
    [Fact] public void CoordinatesRejectInvalid()=>Assert.Throws<ArgumentException>(()=>Validation.LatitudeLongitude(100,0));
    // Method: RequiredRejectsBlank - executes the relevant logic
    [Fact] public void RequiredRejectsBlank()=>Assert.Throws<ArgumentException>(()=>Validation.Required(" ","x"));
}
