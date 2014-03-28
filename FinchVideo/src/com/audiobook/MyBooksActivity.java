package com.audiobook;

import java.io.File;
import java.util.ArrayList;

import junit.framework.Assert;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.audiobook2.R;

import dataProvider.dbProvider.fileManager.FileManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class MyBooksActivity extends SherlockActivity {
    final String selection = "SELECT mb.abook_id id, title, mb.abook_id _id"
            +" FROM mybooks mb"
            +" JOIN t_abooks ab ON ab.abook_id = mb.abook_id"
            +" ORDER BY last_touched DESC";

	private Cursor c = null;
    private ArrayList<CatalogItem> items;
    private SimpleCursorAdapter mAdapter;

    void db_MybooksRemove(String bid)
    {
        String sql = "DELETE FROM mybooks WHERE abook_id = ?";

        gs.db.execSQL(sql, new String[]{bid});
		
		// delete chapters
		sql = "DELETE FROM t_tracks WHERE abook_id = ?";

		gs.db.execSQL(sql, new String[]{bid});
		
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
			myIntentA1A2 = new Intent(MyBooksActivity.this, SearchActivity.class);
			break;
		case 1:
			myIntentA1A2 = new Intent(MyBooksActivity.this, MyBooksActivity.class);
			break;
		case 2:			
			myIntentA1A2 = new Intent(MyBooksActivity.this, PlayerActivity.class);
			Bundle myData = new Bundle();
			myData.putString("bid", "0");
			myIntentA1A2.putExtras(myData);
			break;
		}

		startActivity(myIntentA1A2);
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		invalidateOptionsMenu();

		c = gs.db.rawQuery(selection, null);

		int idxname = c.getColumnIndex("title");
		int idxid = c.getColumnIndex("id");
		items = new ArrayList<CatalogItem>();
		if (c.moveToFirst()) {
			do {
				CatalogItem ci = new CatalogItem();
				ci.name = c.getString(idxname);
				ci.ID = c.getString(idxid);
				items.add(ci);
			} while (c.moveToNext());
		}
		//startManagingCursor(c);
		mAdapter.changeCursor(c);	
		
		
		// set up player button
//		if(gs.shouldShowPlayerButton)
//		{
//			Button button = (Button) findViewById(R.id.btn_go_player_my);
//			button.setVisibility(View.VISIBLE);
//			button.setOnClickListener(new View.OnClickListener() {
//				public void onClick(View v) {
//					Intent myIntentA1A2 = new Intent(MyBooksActivity.this, PlayerActivity.class);
//					Bundle myData = new Bundle();
//					myData.putString("bid", "0");
//					myIntentA1A2.putExtras(myData);
//	
//					startActivity(myIntentA1A2);
//				}
//			});
//		}
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
				AlertDialog.Builder builder = new AlertDialog.Builder(MyBooksActivity.this);
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
                            		c = gs.db.rawQuery(selection, null);
                            		mAdapter.changeCursor(c);
                            		//startManagingCursor(c);
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
