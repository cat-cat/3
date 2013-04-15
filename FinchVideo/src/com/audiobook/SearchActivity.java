package com.audiobook;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.finchframework.finch.Finch;
import com.finchframework.finch.views.MesgEditText;
import com.oreilly.demo.android.pa.finchvideo.PlayerActivity;
import com.oreilly.demo.android.pa.finchvideo.R;
import com.oreilly.demo.android.pa.finchvideo.R.id;
import com.oreilly.demo.android.pa.finchvideo.R.layout;
import com.oreilly.demo.android.pa.finchvideo.R.string;
import com.oreilly.demo.android.pa.finchvideo.provider.FinchVideo;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends Activity {
	private SQLiteDatabase db = null;
    private ArrayList<CatalogItem> items;
    private SimpleCursorAdapter mAdapter;

    private MesgEditText mSearchText;

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
				myIntentA1A2 = new Intent(SearchActivity.this, PlayerActivity.class);

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
	
    Cursor db_GetBooksWithScope(String scope, String sf)
    {
        //char* sqlStatement = 0;
        
        String query = " SELECT t_abooks.abook_id AS id, title, GROUP_CONCAT(t_authors.name, ',') authors, CASE t_abooks.bought WHEN 1 THEN '+' ELSE priceios END priceios,  t_abooks.abook_id AS _id  FROM t_abooks"
        +" LEFT JOIN"
        +" t_abooks_authors ON t_abooks_authors.abook_id=t_abooks.abook_id"
        +" JOIN"
        +" t_authors ON t_abooks_authors.author_id=t_authors.author_id"
        +" WHERE (t_abooks.deleted=0 OR t_abooks.bought=1)";
        
        if (sf!=null && !sf.isEmpty()) {
            query = query + " AND (t_authors.name_lower LIKE ? OR title_lower LIKE ?) ";
        }
        
        if (scope.equalsIgnoreCase("Новые")) // top level - genres without parents, add Search item at the top
            
            query = query +" GROUP BY t_abooks.abook_id ORDER BY  order_new DESC";    
            
        else // Все - sort by popular
            
            query = query +" GROUP BY t_abooks.abook_id ORDER BY  order_popular DESC";
        
//        else // Все
//            
//            query = [query stringByAppendingString:[NSString stringWithFormat:@" GROUP BY t_abooks.abook_id ORDER BY  title"]];
        
        	if(db==null)
		        db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
						SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            String lsf = sf.toLowerCase();
            
            String[] sa = null;
            if (sf!=null && !sf.isEmpty())
    			sa = new String[] {"%"+scope+"%", "%"+lsf+"%"};
    			
    		Cursor	c = db.rawQuery(query, sa);
	        
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
			//db.close();
			return c;
    }
    
    	@Override
    	public void onResume()
    	{
    		super.onResume();
	        final ListView searchList = (ListView) findViewById(R.id.video_list);
	        searchList.setClickable(true);
	        searchList.setOnItemClickListener(new Clicker1());
	        
	        Cursor c = db_GetBooksWithScope("Новые", ""); // get all books
	        // have to reset this on a new search

	        // Maps video entries from the database to views
	        mAdapter = new SimpleCursorAdapter(this,
	            R.layout.video_list_item,
	            c,
	            new String[] {
	            "id",
	            "title"
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
	                    	// TODO:
	                        //setThumbResource(view, cursor);
	                        break;
	                }

	                return true;
	            }
	        };

	        mAdapter.setViewBinder(savb);

	        searchList.setAdapter(mAdapter);
    	}
    
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.search_query_activity);
	        
	        mSearchText = (MesgEditText) findViewById(R.id.video_search_box);
	        Resources r = getResources();
	        mSearchText.setMesgText(r.getString(R.string.finch_video_search));
	        mSearchText.setOnEditorActionListener(
	            new EditText.OnEditorActionListener() {
	                @Override
	                public boolean onEditorAction(TextView textView,
	                    int actionId,
	                    KeyEvent keyEvent)
	                {
	                    // a null key event observed on some devices
	                    if (null != keyEvent) {
	                        int keyCode = keyEvent.getKeyCode();
	                        if ((keyCode == KeyEvent.KEYCODE_ENTER) &&
	                            (keyEvent.getAction() ==
	                                KeyEvent.ACTION_DOWN))
	                        {
	                            // action only causes the provider to ensure
	                            // the presence of some search results.
	                            query();

	                            return true;
	                        }
	                    }
	                    return false;
	                }
	            });

	        final ImageButton refreshButton = (ImageButton)
	        findViewById(R.id.video_update_button);
	        refreshButton.setOnClickListener(new View.OnClickListener() {
	            @Override public void onClick(View view) { query(); }
	        });
	        refreshButton.setFocusable(true);
	    }

	    void setThumbResource(View view, Cursor cursor) {
	        Uri thumbUri = ContentUris.
	        withAppendedId(FinchVideo.Videos.THUMB_URI,
	            cursor.getLong(FinchVideo.ID_COLUMN));
	        try {
	            InputStream thumbStream =
	                getContentResolver().openInputStream(thumbUri);
	            ImageView iv = (ImageView)
	            view.findViewById(R.id.video_thumb_icon);
	            Bitmap bm = BitmapFactory.decodeStream(thumbStream);
	            iv.setImageBitmap(bm);

	        } catch (FileNotFoundException e) {
	            Log.d(Finch.LOG_TAG, "could not open provider thumb: ", e);
	        }
	    }

	    // sends the query to the finch video content provider
	    void query() {
	        if (!mSearchText.searchEmpty()) {
	            Cursor c = db_GetBooksWithScope("Новые",mSearchText.getText().toString());
	            mAdapter.changeCursor(c);
	        }
	   }	    
}
