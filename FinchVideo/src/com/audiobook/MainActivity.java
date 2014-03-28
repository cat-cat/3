package com.audiobook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import junit.framework.Assert;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.audiobook2.R;
import com.google.analytics.tracking.android.EasyTracker;

/**
 * Slightly more sophisticated FinchVideo search application that allows a user
 * to type a search query and see network results update as they are received
 * from RESTful web services like gdata.youtube.com.  The results appear one by
 * one in the graphical list display as they are parsed from network data.
 */
public class MainActivity extends SherlockActivity {
	boolean isDebuggable;
	private SimpleCursorAdapter mAdapter;

	private ArrayList<CatalogItem> items;

    public boolean onCreateOptionsMenu(Menu menu) {
        //Used to put dark icons on light action bar
        //boolean isLight = SampleList.THEME == R.style.Theme_Sherlock_Light;
        boolean isLight = true;

        menu.add(0, 0, 0, "search")
            .setIcon(isLight ? android.R.drawable.ic_menu_search : android.R.drawable.ic_menu_agenda)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, 1, 0, "recent")
        	.setIcon(isLight ? android.R.drawable.ic_menu_more : android.R.drawable.ic_menu_more)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);


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
			myIntentA1A2 = new Intent(MainActivity.this, SearchActivity.class);
			break;
		case 1:
			myIntentA1A2 = new Intent(MainActivity.this, MyBooksActivity.class);
			break;
		case 2:			
			myIntentA1A2 = new Intent(MainActivity.this, PlayerActivity.class);
			Bundle myData = new Bundle();
			myData.putString("bid", "0");
			myIntentA1A2.putExtras(myData);
			break;
		}

		startActivity(myIntentA1A2);
		return super.onOptionsItemSelected(item);
	}
    
	@Override
	  public void onStart() {
	    super.onStart();
	    
	    if(!isDebuggable)
		    // The rest of your onStart() code.
		    EasyTracker.getInstance().activityStart(this); // Add this method.
	    
	    
	  }

	  @Override
	  public void onStop() {
	    super.onStop();
	    // The rest of your onStop() code.
	    
	    if(!isDebuggable)
	    	EasyTracker.getInstance().activityStop(this); // Add this method.
	  }

	@Override
	public void onResume()
	{
		super.onResume();
		
		invalidateOptionsMenu();
		
//		if(gs.shouldShowPlayerButton)
//		{
//			Button button = (Button) findViewById(R.id.btn_go_player_main);
//			button.setVisibility(View.VISIBLE);
//			button.setOnClickListener(new View.OnClickListener() {
//				public void onClick(View v) {
//					
//					Intent myIntentA1A2 = new Intent(MainActivity.this, PlayerActivity.class);
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
    	if(gs.db!=null){
	        gs.db.close();
    	}
	}

	private class Clicker1 implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> a, View view, int position, long id) {

			// message box
			//            Toast.makeText(getApplicationContext(),
			//            	      "Click ListItem Number " + position, Toast.LENGTH_SHORT)
			//            	      .show();

			// 

			try {
				
				Intent 	myIntentA1A2 = new Intent(MainActivity.this, CatalogActivity.class);

				Bundle myData = new Bundle();
				//				TextView v = (TextView)  view.findViewById(R.id.idx_init);
				//				int pos = Integer.parseInt(v.getText().toString());
				//				myData.putInt("pos", pos);
				String name = items.get(position).name;
				Log.i("MainActivity:CategoryIDClick:", name);
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

	private void CopyDatabase() throws IOException 
	{
		File dir = new File(gs.s().dbpath());
		boolean dirsmade = false;
		if(!dir.exists()){
			dirsmade = dir.mkdirs();
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
	
	public void createShortCut(){
	    Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
	    shortcutintent.putExtra("duplicate", false);
	    shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "BookSmile");
	    Parcelable icon = Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.icon);
	    shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
	    shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(getApplicationContext(), MainActivity.class));
	    sendBroadcast(shortcutintent);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		if (android.os.Build.VERSION.SDK_INT >= 11)
//			getActionBar().hide();

		
		setContentView(R.layout.main_query_activity);
		
	    isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));



		final ListView searchList = (ListView) findViewById(R.id.video_list);
		searchList.setClickable(true);
		searchList.setOnItemClickListener(new Clicker1());

		// init global singleton
		gs.s().setContext(getApplicationContext()); // dont move it in asynctask, or error




		// Maps video entries from the database to views
		mAdapter = new SimpleCursorAdapter(MainActivity.this,
				R.layout.catalog_list_item,
				null,
				new String[] {
				"id",
				"name",
				"type"
		},
		new int[] { R.id.video_thumb_icon,  R.id.video_text, R.id.video_list_item});

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

				// SET DEVICE ID
				String imeistring=null;				
				String imsistring=null;
				String devid = null;

				TelephonyManager   telephonyManager  =  ( TelephonyManager)getSystemService( Context.TELEPHONY_SERVICE );

				/*
				 * getDeviceId() function Returns the unique device ID.
				 * for example,the IMEI for GSM and the MEID or ESN for CDMA phones.  
				 */				    
				imeistring = telephonyManager.getDeviceId();
				// IMEI No : 
				devid=imeistring+"-";


				/*
				 * getSubscriberId() function Returns the unique subscriber ID,
				 * for example, the IMSI for a GSM phone.
				 */				                                                                                      
//				imsistring = telephonyManager.getSubscriberId();        				    
//				devid+="IMSI No : "+imsistring+"\n";

				/*
				 * System Property ro.serialno returns the serial number as unique number
				 * Works for Android 2.3 and above				     
				 */

				//		 String hwID = android.os.SystemProperties.get("ro.serialno", "unknown");
				//		 devid+= "hwID : " + hwID + "\n"; 
//				String serialnum = null;      
//				try {         
//					Class<?> c = Class.forName("android.os.SystemProperties");        	           	      
//					Method get = c.getMethod("get", String.class, String.class );                 
//					serialnum = (String)(   get.invoke(c, "ro.serialno", "unknown" )  );
//					devid+= "serial : " + serialnum + "\n" ;
//				} catch (Exception ignored) {       
//				}
//				String serialnum2 = null;
//				try {
//					Class myclass = Class.forName( "android.os.SystemProperties" );
//					Method[] methods = myclass.getMethods();
//					Object[] params1 = new Object[] { new String( "ro.serialno" ) , new String(  
//							"Unknown" ) };        	
//					serialnum2 = (String)(methods[2].invoke( myclass, params1 ));        	
//					devid+= "serial2 : " + serialnum2 + "\n"; 
//				}catch (Exception ignored) 
//				{     		
//				}		
				/*
				 * Settings.Secure.ANDROID_ID returns the unique DeviceID
				 * Works for Android 2.2 and above				     
				 */
				String androidId = Settings.Secure.getString(getContentResolver(),  
						Settings.Secure.ANDROID_ID);
				// "AndroidID : "
				devid+=androidId;
				gs.s().setDeviceId(devid);




				// check database existence
				File checkDB = new File(gs.s().dbp());

				if (!checkDB.exists())
				{
					try {
						CopyDatabase();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Assert.assertTrue(false);
						e.printStackTrace();
					}
					
					createShortCut();
				}
				
				gs.s().setDatabase();

//				String selection = " SELECT -2 id, 'Найти книгу' name, 0 subgenres, -2 type , 'n/a' priceos, '-' authors, -2 _id"
//						+ " UNION"
//						+" SELECT 0 id, 'Недавние' name, 0 subgenres, 0 type , 'n/a' priceos, '-' authors, 0 _id"
//						+ " UNION"
//						+ " SELECT t_abooks.abook_id AS id, title AS name, -1 AS subgenres, 2 AS type, CASE t_abooks.bought WHEN 1 THEN '+' ELSE priceios END priceios, GROUP_CONCAT(t_authors.name, ',') authors, t_abooks.abook_id AS _id FROM t_abooks"
				String selection = " SELECT t_abooks.abook_id AS id, title AS name, -1 AS subgenres, 2 AS type, CASE t_abooks.bought WHEN 1 THEN '+' ELSE priceios END priceios, GROUP_CONCAT(t_authors.name, ',') authors, t_abooks.abook_id AS _id FROM t_abooks"
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
						+ " ORDER BY  t_genres.genre_id, name ASC  LIMIT ?, ?";

				Cursor c = gs.db.rawQuery(selection, new String[] {"-1", "-1", "0", "20000"});

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
//						((View)view.getParent()).setBackgroundColor(0xFFFFFFFF );

						break;
						case 0: // id
							// TODO:
							//setThumbResource(view, cursor);
							break;
						case 3: // type
							int typeInt = cursor.getInt(i);
							if(typeInt==-2||typeInt==0)
								view.setBackgroundColor(0xFFC0C0C0 );
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
				try {
					if (this.dialog.isShowing()) {
						this.dialog.dismiss();
					}
				} catch (IllegalArgumentException  e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				
				searchList.setAdapter(mAdapter);
				mAdapter.changeCursor(c);
				
				gs.s().initNetNotifier();
			}      	
		} // loadTask
		new loadTask().execute();

	}


}
