/*
 * File: EnergySlotRepository.cs
 * Description: Smart Solar Microgrid Trading System - EnergySlotRepository.cs module
 * Author: Admin
 */
using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class EnergySlotRepository : MongoRepository<EnergySlot> {
    // Method: EnergySlotRepository - executes the relevant logic
    // Method: EnergySlotRepository (Constructor) - initializes the instance
    public EnergySlotRepository(MongoDbContext db) : base(db.EnergySlots) { }
}
