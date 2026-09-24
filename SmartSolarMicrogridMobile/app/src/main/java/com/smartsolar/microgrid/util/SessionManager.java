package com.smartsolar.microgrid.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {
    public static final String BASE_URL = "http://10.0.2.2:5000/api/";
    private static SessionManager instance; private final SharedPreferences sp;
    private SessionManager(Context c){sp=c.getSharedPreferences("session",Context.MODE_PRIVATE);}
    public static void init(Context c){if(instance==null)instance=new SessionManager(c.getApplicationContext());}
    public static SessionManager getInstance(){if(instance==null)throw new IllegalStateException("SessionManager not initialized");return instance;}
    public void save(String token,String role,String username,String nic){sp.edit().putString("token",token).putString("role",role).putString("username",username).putString("nic",nic).apply();}
    public String token(){return sp.getString("token","");} public String role(){return sp.getString("role","");} public String username(){return sp.getString("username","");} public String nic(){return sp.getString("nic","");}
    public void clear(){sp.edit().clear().apply();}
}
