/*
 * File: UserRepository.cs
 * Description: Smart Solar Microgrid Trading System - UserRepository.cs module
 * Author: Admin
 */
using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class UserRepository : MongoRepository<User> {
    // Method: UserRepository - executes the relevant logic
    // Method: UserRepository (Constructor) - initializes the instance
    public UserRepository(MongoDbContext db) : base(db.Users) { }
}
