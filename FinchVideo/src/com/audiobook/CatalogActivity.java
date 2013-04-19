package com.audiobook;

import java.util.ArrayList;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.oreilly.demo.android.pa.finchvideo.PlayerActivity;
import com.oreilly.demo.android.pa.finchvideo.R;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class CatalogActivity extends Activity {
	private SQLiteDatabase db = null;
    private ArrayList<CatalogItem> items;
    private SimpleCursorAdapter mAdapter;
    
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
//            	      "Click ListItem Number " + position, Toast.LENGTH_LONG)
//            	      .show();
            
            // 
            
			try {
				Intent myIntentA1A2;
				if(items.get(position).type.equalsIgnoreCase("1")) // category
					myIntentA1A2 = new Intent(CatalogActivity.this, CatalogActivity.class);
				else // 2 - book
					myIntentA1A2 = new Intent(CatalogActivity.this, PlayerActivity.class);

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
						Toast.LENGTH_LONG).show();
			}
		}
    }
	
    
 	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_query_activity);
        //Resources r = getResources();
        
        final ListView searchList = (ListView) findViewById(R.id.video_list);
        searchList.setClickable(true);
        searchList.setOnItemClickListener(new Clicker1());
                
        String selection = " SELECT t_abooks.abook_id AS id, title AS name, -1 AS subgenres, 2 AS type, CASE t_abooks.bought WHEN 1 THEN '+' ELSE priceios END priceios, GROUP_CONCAT(t_authors.name, ',') authors, t_abooks.abook_id AS _id  FROM t_abooks"
                       + " LEFT JOIN"
                        +" t_abooks_authors ON t_abooks_authors.abook_id=t_abooks.abook_id"
                        +" JOIN"
                        +" t_authors ON t_abooks_authors.author_id=t_authors.author_id"
                   +" JOIN t_abooks_genres ON t_abooks.abook_id = t_abooks_genres.abook_id"
                   +" WHERE t_abooks_genres.genre_id = ? AND (t_abooks.deleted=0 OR t_abooks.bought=1) GROUP BY t_abooks.abook_id"
                   +" UNION"
                   +" SELECT t_genres.genre_id AS id, name, COUNT(t_abooks_genres.genre_id) AS subgenres, 1 AS type, 'n/a' priceos, '-' authors, t_genres.genre_id AS _id   FROM t_genres"
                   +" LEFT JOIN"
                   +" t_abooks_genres"
                   +" WHERE t_genres.genre_parent_id = ? AND t_genres.genre_id = t_abooks_genres.genre_id"
                   +" GROUP BY name"
                   +" ORDER BY  type, name DESC  LIMIT ?, ?";
        Intent myLocalIntent = getIntent();
        Bundle myBundle = myLocalIntent.getExtras();

        String parent = myBundle.getString("bid");

    	if(db==null)
	        db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
					SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		Cursor c = db.rawQuery(selection, new String[] {parent, parent, "0", "20000"});
        
		int idxname = c.getColumnIndex("name");
		int idxid = c.getColumnIndex("id");
		int idxtype = c.getColumnIndex("type");
		items = new ArrayList<CatalogItem>();
		if (c.moveToFirst()) {
			do { 
					CatalogItem ci = new CatalogItem();
					ci.name = c.getString(idxname);
					ci.ID = c.getString(idxid);
					ci.type = c.getString(idxtype);
					items.add(ci);
				}
			while (c.moveToNext());
		}
		startManagingCursor(c);
        // have to reset this on a new search

        // Maps video entries from the database to views
        mAdapter = new SimpleCursorAdapter(this,
            R.layout.video_list_item,
            c,
            new String[] {
            "id",
            "name"
        },
        new int[] { R.id.video_thumb_icon,  R.id.video_text});

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
                        ImageView iv = (ImageView)
                        view.findViewById(R.id.video_thumb_icon);
                        gs.s().displayBookImage(items.get(cursor.getPosition()).ID, iv);
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
