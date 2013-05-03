package com.audiobook;

import java.io.File;
import java.util.ArrayList;

import junit.framework.Assert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class CatalogActivity extends Activity {
	ListView searchList;
	private ArrayList<CatalogItem> items;
	private SimpleCursorAdapter mAdapter;

	@Override
	public void onResume()
	{
		super.onResume();


		
		if(gs.shouldShowPlayerButton)
		{
			Button button = (Button) findViewById(R.id.btn_go_player);
			button.setVisibility(View.VISIBLE);
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent myIntentA1A2 = new Intent(CatalogActivity.this, PlayerActivity.class);
					Bundle myData = new Bundle();
					myData.putString("bid", "0");
					myIntentA1A2.putExtras(myData);
	
					startActivity(myIntentA1A2);
				}
			});
		}
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
	}

	private class Clicker1 implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> a, View view, int position, long id) {

			File f = new File(gs.s().pathForBookMeta(items.get(position).ID));
			if(!gs.s().connected()&&!f.exists())
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(CatalogActivity.this);
				builder.setMessage("Для загрузки книги нужен интернет!\nИнтернет не доступен.")
				       .setCancelable(false)
				       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                //do things
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
				return;
			}

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
						Toast.LENGTH_SHORT).show();
			}
		}
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.catalog_query_activity);
		//Resources r = getResources();

		searchList = (ListView) findViewById(R.id.video_list);
		searchList.setClickable(true);
		searchList.setOnItemClickListener(new Clicker1());

		// Maps video entries from the database to views
		mAdapter = new SimpleCursorAdapter(CatalogActivity.this,
				R.layout.video_list_item,
				null,
				new String[] {
				"id",
				"name",
				"authors",
				"price"
		},
		new int[] { R.id.video_thumb_icon,  R.id.video_text, R.id.authors_text, R.id.price_text });

		class loadTask extends AsyncTask<Void,Void,Cursor>
		{
			@Override
			protected Cursor doInBackground(Void... v)
			{
				String selection = " SELECT t_abooks.abook_id AS id, title AS name, -1 AS subgenres, 2 AS type, CASE t_abooks.bought WHEN 1 THEN '+' ELSE priceandroid END price, GROUP_CONCAT(t_authors.name, ',') authors, t_abooks.abook_id AS _id  FROM t_abooks"
						+ " LEFT JOIN"
						+" t_abooks_authors ON t_abooks_authors.abook_id=t_abooks.abook_id"
						+" JOIN"
						+" t_authors ON t_abooks_authors.author_id=t_authors.author_id"
						+" JOIN t_abooks_genres ON t_abooks.abook_id = t_abooks_genres.abook_id"
						+" WHERE t_abooks_genres.genre_id = ? AND (t_abooks.deleted=0 OR t_abooks.bought=1) GROUP BY t_abooks.abook_id"
						+" UNION"
						+" SELECT t_genres.genre_id AS id, name, COUNT(t_abooks_genres.genre_id) AS subgenres, 1 AS type, 'n/a' price, '-' authors, t_genres.genre_id AS _id   FROM t_genres"
						+" LEFT JOIN"
						+" t_abooks_genres"
						+" WHERE t_genres.genre_parent_id = ? AND t_genres.genre_id = t_abooks_genres.genre_id"
						+" GROUP BY name"
						+" ORDER BY  type, name DESC  LIMIT ?, ?";
				Intent myLocalIntent = getIntent();
				Bundle myBundle = myLocalIntent.getExtras();

				String parent = myBundle.getString("bid");

				Cursor c = gs.db.rawQuery(selection, new String[] {parent, parent, "0", "20000"});

				int idxname = c.getColumnIndex("name");
				int idxid = c.getColumnIndex("id");
				int idxtype = c.getColumnIndex("type");
				int idxprice = c.getColumnIndex("price");
				int idxauthors = c.getColumnIndex("authors");
				items = new ArrayList<CatalogItem>();
				if (c.moveToFirst()) {
					do { 
						CatalogItem ci = new CatalogItem();
						ci.name = c.getString(idxname);
						ci.ID = c.getString(idxid);
						ci.type = c.getString(idxtype);
						ci.price = c.getString(idxprice);
						ci.authors = c.getString(idxauthors);
						items.add(ci);
					}
					while (c.moveToNext());
				}
				//startManagingCursor(c);
				// have to reset this on a new search

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
	                        
	                    case 4: // price
	                    	TextView pv = (TextView)
	                    	view.findViewById(R.id.price_text);
	                    	String priceText = cursor.getString(i);
	                    	pv.setText(priceText);
	                    	break;
	                    	
	                    case 5: // authors
	                    	TextView av = (TextView)
	                    	view.findViewById(R.id.authors_text);
	                    	String authorsText = cursor.getString(i);
	                    	av.setText(authorsText);
	                    	break;
						}

						return true;
					}
				};

				mAdapter.setViewBinder(savb);
				return c;
			}

			@Override
			protected void onPostExecute(Cursor c)
			{
				mAdapter.changeCursor(c);
				searchList.setAdapter(mAdapter);
			}
		}
		new loadTask().execute();
	}

}
