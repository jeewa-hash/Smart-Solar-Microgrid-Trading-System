package com.smartsolar.microgrid.db;

import android.content.*;import android.database.sqlite.*;import android.content.ContentValues;import android.database.Cursor;

public class LocalDbHelper extends SQLiteOpenHelper {
    private static final String DB="smart_solar.db"; private static final int VER=1;
    public LocalDbHelper(Context c){super(c,DB,null,VER);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE local_user(id INTEGER PRIMARY KEY AUTOINCREMENT,nic TEXT UNIQUE,full_name TEXT,email TEXT,phone TEXT,address TEXT,username TEXT,role TEXT,last_sync TEXT)");db.execSQL("CREATE TABLE reference_nodes(id TEXT PRIMARY KEY,node_code TEXT,node_name TEXT,latitude REAL,longitude REAL,capacity_kw REAL,battery_slots INTEGER,status TEXT)");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){db.execSQL("DROP TABLE IF EXISTS local_user");db.execSQL("DROP TABLE IF EXISTS reference_nodes");onCreate(db);}
    public void saveUser(String nic,String name,String email,String phone,String address,String username,String role){SQLiteDatabase db=getWritableDatabase();ContentValues v=new ContentValues();v.put("nic",nic);v.put("full_name",name);v.put("email",email);v.put("phone",phone);v.put("address",address);v.put("username",username);v.put("role",role);v.put("last_sync",String.valueOf(System.currentTimeMillis()));db.insertWithOnConflict("local_user",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public Cursor getUser(){return getReadableDatabase().rawQuery("SELECT * FROM local_user LIMIT 1",null);}
}
