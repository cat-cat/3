package com.audiobook;

import java.util.ArrayList;

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
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MyBooksActivity extends Activity {
	private Cursor c = null;
	private SQLiteDatabase db = null;
    private ArrayList<CatalogItem> items;
    private SimpleCursorAdapter mAdapter;

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
    	if(db!=null)
	        db.close();
    }
    
	private class Clicker1 implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> a, View view, int position, long id) {
        	
        	// message box
//            Toast.makeText(getApplicationContext(),
//            	      "Click ListItem Number " + position, Toast.LENGTH_SHORT)
//            	      .show();
            
            // 
            
			try {
				Intent myIntentA1A2;
				myIntentA1A2 = new Intent(MyBooksActivity.this, PlayerActivity.class);

				Bundle myData = new Bundle();
//				TextView v = (TextView)  view.findViewById(R.id.idx_init);
//				int pos = Integer.parseInt(v.getText().toString());
//				myData.putInt("pos", pos);
				String name = items.get(position).name;
				Log.i("MainActivity:CategoriIDClick:", name);
				String bid = items.get(position).ID;
				myData.putInt("pos", position);
				myData.putString("name", name);
				myData.putString("bid", bid);
				myData.putDouble("myDouble1", 3.141592);
				int[] myLittleArray = { 1, 2, 3 };
				myData.putIntArray("myIntArray1", myLittleArray);

				myIntentA1A2.putExtras(myData);

				startActivity(myIntentA1A2);
			} catch (Exception e) {
				Toast.makeText(getBaseContext(), e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
		}
    }
	
    
 	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mybooks_query_activity);
        //Resources r = getResources();
        
        final ListView searchList = (ListView) findViewById(R.id.video_list);
        searchList.setClickable(true);
        searchList.setOnItemClickListener(new Clicker1());
                
        final String selection = "SELECT mb.abook_id id, title, mb.abook_id _id"
                                   +" FROM mybooks mb"
                                   +" JOIN t_abooks ab ON ab.abook_id = mb.abook_id"
                                   +" ORDER BY last_touched DESC";
        
        Intent myLocalIntent = getIntent();
        Bundle myBundle = myLocalIntent.getExtras();

        String parent = myBundle.getString("bid");

    	if(db==null)
	        db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
					SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		c = db.rawQuery(selection, null);
        
		int idxname = c.getColumnIndex("title");
		int idxid = c.getColumnIndex("id");
		items = new ArrayList<CatalogItem>();
		if (c.moveToFirst()) {
			do { 
					CatalogItem ci = new CatalogItem();
					ci.name = c.getString(idxname);
					ci.ID = c.getString(idxid);
					items.add(ci);
				}
			while (c.moveToNext());
		}
		startManagingCursor(c);
        // have to reset this on a new search

        // Maps video entries from the database to views
        mAdapter = new SimpleCursorAdapter(this,
            R.layout.mybooks_list_item,
            c,
            new String[] {
            "id",
            "title"
        },
        new int[] { R.id.btn_delete,  R.id.video_text});

        SimpleCursorAdapter.ViewBinder savb =
            new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int i) {
                switch (i) {
                    case 1: // title
                        TextView tv = (TextView)
                        view.findViewById(R.id.video_text);
                        String videoText = cursor.getString(i);
                        tv.setText(videoText);
                        


                        break;
                    case 0: // id
                        // inflate other items here : 
                        Button deleteButton = (Button) view.findViewById(R.id.btn_delete);
                         deleteButton.setTag(cursor.getPosition());

                        deleteButton.setOnClickListener(
                            new Button.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Integer index = (Integer) v.getTag();
                                    String bid = items.get(index.intValue()).ID;
                                    items.remove(index.intValue());
                                    db_MybooksRemove(bid);
                            		c = db.rawQuery(selection, null);
                            		mAdapter.changeCursor(c);
                            		startManagingCursor(c);
                                    //mAdapter.notifyDataSetChanged();
                                    FileManager.DeleteBook(bid);
                                    //FileManager.DropAllFiles();
                                }
                            });
                        break;
                }

                return true;
            }
        };
        mAdapter.setViewBinder(savb);

        searchList.setAdapter(mAdapter);
        //db.close();
        //c.close();
    }
	    
}
