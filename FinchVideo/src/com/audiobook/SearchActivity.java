package com.audiobook;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.audiobook2.R;

public class SearchActivity  extends SherlockActivity {
    private ArrayList<CatalogItem> items;
    private SimpleCursorAdapter mAdapter;

    private EditText mSearchText;
	private class QueryTask extends AsyncTask<String, Void, Cursor> {
		private final ProgressDialog dialog = new ProgressDialog(
				SearchActivity.this);

		// can use UI thread here
		protected void onPreExecute() {
			this.dialog.setMessage("обновление списка...");
			this.dialog.show();
		}

		// automatically done on worker thread (separate from UI thread)
		@Override
		protected Cursor doInBackground(final String... args) {
	        Cursor c = db_GetBooksWithScope(args[0], args[1]); // get all books
			return c;			
		}

		// can use UI thread here
		protected void onPostExecute(final Cursor cursor) {
			try {
				if (this.dialog.isShowing()) {
					this.dialog.dismiss();
				}
			} catch (IllegalArgumentException  e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			mAdapter.changeCursor(cursor);
		}
	}
	
    public boolean onCreateOptionsMenu(Menu menu) {
        //Used to put dark icons on light action bar
        //boolean isLight = SampleList.THEME == R.style.Theme_Sherlock_Light;
        boolean isLight = true;

//        menu.add(0, 0, 0, "search")
//            .setIcon(isLight ? android.R.drawable.ic_menu_search : android.R.drawable.ic_menu_agenda)
//            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//
//        menu.add(0, 1, 0, "recent")
//        	.setIcon(isLight ? android.R.drawable.ic_menu_more : android.R.drawable.ic_menu_more)
//            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);


		if(gs.shouldShowPlayerButton)
		{
			menu.add(0, 2, 0, "player")
	            .setIcon(isLight ? android.R.drawable.ic_media_play : android.R.drawable.ic_menu_compass)
	            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		}

        return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent myIntentA1A2 = null;
		switch (item.getItemId()) {
		case 0:
			myIntentA1A2 = new Intent(SearchActivity.this, SearchActivity.class);
			break;
		case 1:
			myIntentA1A2 = new Intent(SearchActivity.this, MyBooksActivity.class);
			break;
		case 2:			
			myIntentA1A2 = new Intent(SearchActivity.this, PlayerActivity.class);
			Bundle myData = new Bundle();
			myData.putString("bid", "0");
			myIntentA1A2.putExtras(myData);
			break;
		}

		startActivity(myIntentA1A2);
		return super.onOptionsItemSelected(item);
	}
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    }
    
	private class Clicker1 implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> a, View view, int position, long id) {
        	
			File f = new File(gs.s().pathForBookMeta(items.get(position).ID));
			if(!gs.s().connected()&&!f.exists())
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);
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
						Toast.LENGTH_SHORT).show();
			}
		}
    }
	
    Cursor db_GetBooksWithScope(String scope, String sf)
    {
        //char* sqlStatement = 0;
        
        String query = " SELECT t_abooks.abook_id AS id, title, GROUP_CONCAT(t_authors.name, ',') authors, CASE t_abooks.bought WHEN 1 THEN '+' ELSE priceandroid END price,  t_abooks.abook_id AS _id  FROM t_abooks"
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
        
            String lsf = sf.toLowerCase();
            
            String[] sa = null;
            if (sf!=null && !sf.isEmpty())
    			sa = new String[] {"%"+scope+"%", "%"+lsf+"%"};
    			
    		Cursor	c = gs.db.rawQuery(query, sa);
	        
			int idxname = c.getColumnIndex("title");
			int idxid = c.getColumnIndex("id");
			int idxauthors = c.getColumnIndex("authors");
			int idxprice = c.getColumnIndex("price");
			items = new ArrayList<CatalogItem>();
			if (c.moveToFirst()) {
				do { 
						CatalogItem ci = new CatalogItem();
						ci.name = c.getString(idxname);
						ci.ID = c.getString(idxid);
						ci.price = c.getString(idxprice);
						ci.authors = c.getString(idxauthors);
						items.add(ci);
					}
				while (c.moveToNext());
			}
			//startManagingCursor(c);
			//db.close();
			return c;
    }
    	boolean activityResumed = false;
    	@Override
    	public void onResume()
    	{
    		super.onResume();
    		
			invalidateOptionsMenu();
			
    		if(activityResumed)
    			return;
    		
    		activityResumed = true;
//			if(gs.shouldShowPlayerButton)
//			{
//				Button button = (Button) findViewById(R.id.btn_go_player_search);
//				button.setVisibility(View.VISIBLE);
//				button.setOnClickListener(new View.OnClickListener() {
//					public void onClick(View v) {
//				    	
//						Intent myIntentA1A2 = new Intent(SearchActivity.this, PlayerActivity.class);
//						Bundle myData = new Bundle();
//						myData.putString("bid", "0");
//						myIntentA1A2.putExtras(myData);
//		
//						startActivity(myIntentA1A2);
//					}
//				});
//			}

    		
	        final ListView searchList = (ListView) findViewById(R.id.video_list);
	        searchList.setClickable(true);
	        searchList.setOnItemClickListener(new Clicker1());
	        
	        // Maps video entries from the database to views
	        mAdapter = new SimpleCursorAdapter(this,
	            R.layout.video_list_item,
	            null,
	            new String[] {
	            "id",
	            "title",
	            "authors",
	            "price"
	        },
	        new int[] { R.id.video_thumb_icon,  R.id.video_text, R.id.authors_text, R.id.price_text});

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
	                        
	                    case 3: // price
	                    	TextView pv = (TextView)
	                    	view.findViewById(R.id.price_text);
	                    	String priceText = cursor.getString(i);
	                    	pv.setText(priceText);
	                    	break;
	                    case 2: // authors
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

	        searchList.setAdapter(mAdapter);
	        
	        new QueryTask().execute("Новые", ""); // get all books
    	}
    
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.search_query_activity);
	        
	        mSearchText = (EditText) findViewById(R.id.video_search_box);
	        Resources r = getResources();
	        mSearchText.setText(r.getString(R.string.finch_video_search));
	        mSearchText.setOnEditorActionListener(
	            new EditText.OnEditorActionListener() {
	                @Override
	                public boolean onEditorAction(TextView textView,
	                    int actionId,
	                    KeyEvent keyEvent)
	                {
	                    // a null key event observed on some devices
//	                    if (null != keyEvent) {
//	                        int keyCode = keyEvent.getKeyCode();
//	                        if ((keyCode == KeyEvent.KEYCODE_ENTER) &&
//	                            (keyEvent.getAction() ==
//	                                KeyEvent.ACTION_DOWN))
	                        if (textView.getText().length()==0 || actionId==EditorInfo.IME_ACTION_SEARCH)
	                        {
	                            // action only causes the provider to ensure
	                            // the presence of some search results.
	                            query();

	                            return true;
	                        }
//	                    }
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

	    // sends the query to the finch video content provider
	    void query() {
	    	CharSequence actualText = mSearchText.getText();
	        if (!"".equalsIgnoreCase(actualText.toString())) {
	            Cursor c = db_GetBooksWithScope("Новые",mSearchText.getText().toString());
	            mAdapter.changeCursor(c);
	        } else {
	        	new QueryTask().execute("Новые", ""); // get all books
	        }
	   }	    
}
