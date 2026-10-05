/*
 * File: MongoRepository.cs
 * Description: Smart Solar Microgrid Trading System - MongoRepository.cs module
 * Author: Admin
 */
using MongoDB.Driver;
namespace SmartSolarMicrogrid.Infrastructure.MongoDB.Repositories;
public abstract class MongoRepository<T> where T: class {
    protected readonly IMongoCollection<T> Collection;
    // Method: MongoRepository - executes the relevant logic
    // Method: MongoRepository (Constructor) - initializes the instance
    protected MongoRepository(IMongoCollection<T> collection) => Collection = collection;
    // Method: GetAllAsync - executes the relevant logic
    public Task<List<T>> GetAllAsync() => Collection.Find(FilterDefinition<T>.Empty).ToListAsync();
    // Method: GetByIdAsync - executes the relevant logic
    public Task<T?> GetByIdAsync(string id) => Collection.Find(Builders<T>.Filter.Eq("_id", id)).FirstOrDefaultAsync();
    // Method: InsertAsync - executes the relevant logic
    public Task InsertAsync(T item) => Collection.InsertOneAsync(item);
    // Method: ReplaceAsync - executes the relevant logic
    public Task ReplaceAsync(string id,T item) => Collection.ReplaceOneAsync(Builders<T>.Filter.Eq("_id", id),item);
    // Method: DeleteAsync - executes the relevant logic
    public Task DeleteAsync(string id) => Collection.DeleteOneAsync(Builders<T>.Filter.Eq("_id", id));
}
