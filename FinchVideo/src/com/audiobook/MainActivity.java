package com.audiobook;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.audiobook.MainActivity;
import com.audiobook.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Slightly more sophisticated FinchVideo search application that allows a user
 * to type a search query and see network results update as they are received
 * from RESTful web services like gdata.youtube.com.  The results appear one by
 * one in the graphical list display as they are parsed from network data.
 */
public class MainActivity extends Activity {
	private SQLiteDatabase db = null;
	private SimpleCursorAdapter mAdapter;

    private ArrayList<CatalogItem> items;
 
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
				if(position == 0) // search books
					myIntentA1A2 = new Intent(MainActivity.this, SearchActivity.class);
				else if(position == 1) // mybooks
					myIntentA1A2 = new Intent(MainActivity.this, MyBooksActivity.class);
				else
					myIntentA1A2 = new Intent(MainActivity.this, CatalogActivity.class);

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
	
    private void CopyDatabase() throws IOException 
    {
		File dir = new File(gs.s().dbpath());
		if(!dir.exists()){
			dir.mkdirs();
		}

		// Path to the just created empty db
		String outFileName = gs.s().dbp();

		// Open the empty db as the output stream
		OutputStream myOutput = null;
		try {
			myOutput = new FileOutputStream(outFileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;

		// Open your local db as the input stream
		InputStream myInput = getApplicationContext().getAssets().open(gs.s().dbname());
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();    	
    }
    
 	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_query_activity);

        
        final ListView searchList = (ListView) findViewById(R.id.video_list);
        searchList.setClickable(true);
        searchList.setOnItemClickListener(new Clicker1());

        // init global singleton
		gs.s().setContext(getApplicationContext()); // dont move it in asynctask, or error

        // Maps video entries from the database to views
        mAdapter = new SimpleCursorAdapter(MainActivity.this,
            R.layout.video_list_item,
            null,
            new String[] {
            "id",
            "name"
        },
        new int[] { R.id.video_thumb_icon,  R.id.video_text});
        
        class loadTask extends AsyncTask<Void,Void,Cursor>
        {
    		private final ProgressDialog dialog = new ProgressDialog(
    				MainActivity.this);

    		// can use UI thread here
    		@Override
    		protected void onPreExecute() {
    			this.dialog.setMessage("обновление списка...");
    			this.dialog.show();
    		}

			@Override
			protected Cursor doInBackground(Void... params) {

		        // check database existance
				SQLiteDatabase checkDB = null;
				try {
					String myPath = gs.s().dbp();
					checkDB = SQLiteDatabase.openDatabase(myPath, null,
							SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			
				} catch (SQLiteException e) {

					// database does't exist yet.

				}

				if (checkDB != null) {

					checkDB.close();

				}

				if (checkDB == null)
				{
					try {
						CopyDatabase();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
		        
		        String selection = " SELECT -2 id, 'Найти книгу' name, 0 subgenres, -2 type , 'n/a' priceos, '-' authors, -2 _id"
		        		+ " UNION"
		        		+" SELECT 0 id, 'Недавние' name, 0 subgenres, 0 type , 'n/a' priceos, '-' authors, 0 _id"
		        		+ " UNION"
		               + " SELECT t_abooks.abook_id AS id, title AS name, -1 AS subgenres, 2 AS type, CASE t_abooks.bought WHEN 1 THEN '+' ELSE priceios END priceios, GROUP_CONCAT(t_authors.name, ',') authors, t_abooks.abook_id AS _id FROM t_abooks"
		               + " LEFT JOIN"
		               + " t_abooks_authors ON t_abooks_authors.abook_id=t_abooks.abook_id"
		               + " JOIN"
		               + " t_authors ON t_abooks_authors.author_id=t_authors.author_id"
		               + " JOIN t_abooks_genres ON t_abooks.abook_id = t_abooks_genres.abook_id"
		               + " WHERE t_abooks_genres.genre_id = ? AND (t_abooks.deleted=0 OR t_abooks.bought=1)  GROUP BY t_abooks.abook_id"
		               + " UNION"
		               + " SELECT t_genres.genre_id AS id, name, COUNT(t_abooks_genres.genre_id) AS subgenres, 1 AS type, 'n/a' priceos, '-' authors, t_genres.genre_id AS _id FROM t_genres"
		               + " LEFT JOIN"
		               + " t_abooks_genres"
		               + " WHERE t_genres.genre_parent_id = ? AND t_genres.genre_id = t_abooks_genres.genre_id"
		               + " GROUP BY name"
		               + " ORDER BY  type, name DESC  LIMIT ?, ?";
		        
		    	if(db==null)
			        db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
							SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
				Cursor c = db.rawQuery(selection, new String[] {"-1", "-1", "0", "20000"});
		        
				int idxname = c.getColumnIndex("name");
				int idxid = c.getColumnIndex("id");
				int idxtype = c.getColumnIndex("type");
				items = new ArrayList<CatalogItem>(30);
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
		        return c;
			}
			
			@Override
			protected void onPostExecute(final Cursor c)
			{
				if (this.dialog.isShowing()) {
					this.dialog.dismiss();
				}
		        searchList.setAdapter(mAdapter);
		        mAdapter.changeCursor(c);
			}      	
        } // loadTask
        new loadTask().execute();

    }
    
    
}
