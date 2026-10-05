/*
 * File: MicrogridNodeRepository.cs
 * Description: Smart Solar Microgrid Trading System - MicrogridNodeRepository.cs module
 * Author: Admin
 */
using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class MicrogridNodeRepository : MongoRepository<MicrogridNode> {
    // Method: MicrogridNodeRepository - executes the relevant logic
    // Method: MicrogridNodeRepository (Constructor) - initializes the instance
    public MicrogridNodeRepository(MongoDbContext db) : base(db.MicrogridNodes) { }
}
