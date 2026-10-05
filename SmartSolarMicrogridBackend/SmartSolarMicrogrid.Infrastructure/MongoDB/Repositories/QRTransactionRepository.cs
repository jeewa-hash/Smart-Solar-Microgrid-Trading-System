/*
 * File: QRTransactionRepository.cs
 * Description: Smart Solar Microgrid Trading System - QRTransactionRepository.cs module
 * Author: Admin
 */
using MongoDB.Driver;
using SmartSolarMicrogrid.Domain.Entities;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public class QRTransactionRepository : MongoRepository<QRTransaction> {
    // Method: QRTransactionRepository - executes the relevant logic
    // Method: QRTransactionRepository (Constructor) - initializes the instance
    public QRTransactionRepository(MongoDbContext db) : base(db.QRTransactions) { }
}
