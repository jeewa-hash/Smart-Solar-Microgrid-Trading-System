/*
 * File: ExceptionMiddleware.cs
 * Description: Smart Solar Microgrid Trading System - ExceptionMiddleware.cs module
 * Author: Admin
 */
using System.Text.Json;
namespace SmartSolarMicrogrid.API.Middleware;
public class ExceptionMiddleware {
    private readonly RequestDelegate _next;
    // Method: ExceptionMiddleware - executes the relevant logic
    // Method: ExceptionMiddleware (Constructor) - initializes the instance
    public ExceptionMiddleware(RequestDelegate next)=>_next=next;
    // Method: Invoke - executes the relevant logic
    public async Task Invoke(HttpContext context){
        try{await _next(context);}
        catch(Exception ex){
            var status=ex switch{UnauthorizedAccessException=>401,KeyNotFoundException=>404,ArgumentException=>400,InvalidOperationException=>409,_=>500};
            context.Response.StatusCode=status;context.Response.ContentType="application/json";
            await context.Response.WriteAsync(JsonSerializer.Serialize(new{error=ex.Message,status}));
        }
    }
}
