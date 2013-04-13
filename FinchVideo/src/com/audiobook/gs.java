package com.audiobook;

import android.content.Context;
import android.os.Environment;


public class gs {
	// The Android's default system path of your application database.
	private Context ctx;
	private String DB_NAME = "database.db";
		
    private static gs   _instance;
   
    public String dbp()
    {
    	return dbpath() + dbname();
    }
    
    public void setContext(Context inContext)
    {
    	ctx = inContext;
    }
    
    public String dbname()
    {
    	return DB_NAME;
    }
    
    public String dbpath()
    {
    	return Environment.getExternalStorageDirectory() +  "/Android/data/" + ctx.getPackageName() + "/databases/";
    }
    
   private gs() {}
   
   public static gs s()
    {
        if (_instance == null)
        {
            _instance = new gs();
       }
        return _instance;
    }

}
