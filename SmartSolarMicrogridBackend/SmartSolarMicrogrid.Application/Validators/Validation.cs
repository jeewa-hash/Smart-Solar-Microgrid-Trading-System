namespace SmartSolarMicrogrid.Application.Validators;
public static class Validation {
    public static void Required(string? value, string name) {
        if (string.IsNullOrWhiteSpace(value)) throw new ArgumentException($"{name} is required.");
    }
    public static void Positive(double value, string name) {
        if (value <= 0) throw new ArgumentException($"{name} must be greater than zero.");
    }
    public static void LatitudeLongitude(double lat, double lon) {
        if (lat < -90 || lat > 90) throw new ArgumentException("Latitude must be between -90 and 90.");
        if (lon < -180 || lon > 180) throw new ArgumentException("Longitude must be between -180 and 180.");
    }
}
