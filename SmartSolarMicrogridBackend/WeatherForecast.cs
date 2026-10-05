/*
 * File: WeatherForecast.cs
 * Description: Smart Solar Microgrid Trading System - WeatherForecast.cs module
 * Author: Admin
 */
namespace SmartSolarMicrogridBackend;

public class WeatherForecast
{
    public DateOnly Date { get; set; }

    public int TemperatureC { get; set; }

    public int TemperatureF => 32 + (int)(TemperatureC / 0.5556);

    public string? Summary { get; set; }
}
