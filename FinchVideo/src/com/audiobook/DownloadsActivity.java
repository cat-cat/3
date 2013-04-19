package com.audiobook;

import java.util.ArrayList;

import ru.old.DownloadManager;
import ru.old.Load;

import com.audiobook.R;

import dataProvider.dbProvider.fileManager.FileManager;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadsActivity extends Activity {
	private DownloadManager downloadManager;
    private ArrayList<CatalogItem> items;

    void db_MybooksRemove(String bid)
    {
        String sql = "DELETE FROM mybooks WHERE abook_id = ?";
        SQLiteDatabase db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
				SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.NO_LOCALIZED_COLLATORS);

		db.execSQL(sql, new String[]{bid});
		db.close();
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    }
    
	
    
 	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.downloads_activity);
        //Resources r = getResources();
        
        final ListView searchList = (ListView) findViewById(R.id.downloads_list);
//        searchList.setClickable(true);
//        searchList.setOnItemClickListener(new Clicker1());
        
        downloadManager = DownloadManager.s(getApplicationContext());
//        Intent myLocalIntent = getIntent();
//        Bundle myBundle = myLocalIntent.getExtras();
//
//        String parent = myBundle.getString("bid");


        // Maps video entries from the database to views
        DownloadsArrayAdapter adapter = new DownloadsArrayAdapter(DownloadsActivity.this,
                downloadManager.CurrentDownloadBooks());

        searchList.setAdapter(adapter);
        //db.close();
        //c.close();
    }
	    
}
