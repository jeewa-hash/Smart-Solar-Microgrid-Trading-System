/*
 * File: GridOperatorRepository.cs
 * Description: Smart Solar Microgrid Trading System - GridOperatorRepository.cs module
 * Author: Admin
 */
using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class GridOperatorRepository : MongoRepository<GridOperator> {
    // Method: GridOperatorRepository - executes the relevant logic
    // Method: GridOperatorRepository (Constructor) - initializes the instance
    public GridOperatorRepository(MongoDbContext db) : base(db.GridOperators) { }
}
