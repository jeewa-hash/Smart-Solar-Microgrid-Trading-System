package com.smartsolar.microgrid.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Response;

public final class ApiUtils {
    private ApiUtils(){}
    public static String error(Response<?> r){
        if(r==null)return "Network error";
        if(r.errorBody()!=null){try{return r.errorBody().string();}catch(Exception ignored){}}
        return "Request failed ("+r.code()+")";
    }
    public static String str(JsonObject o,String key){JsonElement e=o==null?null:o.get(key);return e==null||e.isJsonNull()?"":e.getAsString();}
    public static double num(JsonObject o,String key){try{return o.get(key).getAsDouble();}catch(Exception e){return 0;}}
}
