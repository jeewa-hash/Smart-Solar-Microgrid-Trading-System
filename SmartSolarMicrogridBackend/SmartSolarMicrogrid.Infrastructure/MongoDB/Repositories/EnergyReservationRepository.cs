/*
 * File: EnergyReservationRepository.cs
 * Description: Smart Solar Microgrid Trading System - EnergyReservationRepository.cs module
 * Author: Admin
 */
using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class EnergyReservationRepository : MongoRepository<EnergyReservation> {
    // Method: EnergyReservationRepository - executes the relevant logic
    // Method: EnergyReservationRepository (Constructor) - initializes the instance
    public EnergyReservationRepository(MongoDbContext db) : base(db.Reservations) { }
}
