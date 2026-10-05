/*
 * File: ProsumerRepository.cs
 * Description: Smart Solar Microgrid Trading System - ProsumerRepository.cs module
 * Author: Admin
 */
using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class ProsumerRepository : MongoRepository<Prosumer> {
    // Method: ProsumerRepository - executes the relevant logic
    // Method: ProsumerRepository (Constructor) - initializes the instance
    public ProsumerRepository(MongoDbContext db) : base(db.Prosumers) { }
}
