package com.audiobook;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import junit.framework.Assert;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import dataProvider.dbProvider.fileManager.FileManager;
import ru.old.AudioServer;
import ru.old.DownloadManager;
import ru.old.Errors;
import ru.old.IManagerObserver;
import ru.old.LoadingType;

import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;
import com.audiobook.R;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class PlayerActivity extends Activity implements OnCompletionListener,
		OnPreparedListener, OnErrorListener, IManagerObserver {
	
	private static ProgressDialog playerDialog;

	int offsetSecs = 0;
	XPathFactory factory = XPathFactory.newInstance();
	XPath xPath = factory.newXPath();

	String xml;
	String bookTitle;

//	enum PlayerState {
//		PL_READY, PL_ERROR, PL_NOT_READY
//	}
	
//	private static PlayerState mediaPlayerState = PlayerState.PL_NOT_READY;

	private static String playingBookId = "";
	private static String playingChapter = "";
	ArrayList<Chapter> chapters;
	String bookId;

	static final int K = 1000;
	/** Отображает длительность трека в формате чч:мм:сс */
	private TextView durationTime;
	/**
	 * Отображает прогресс проигрывания аудиочасти трека и позволяет по нему
	 * перемещаться
	 */
	private SeekBar progressbar;
	/** Отображает текущий время трека в формате чч:мм:сс */
	private TextView currentTime;

	DownloadManager downloadManager;
	// private static int bookId = 75316;
	// private static String chapterId = "01_01";

	/** Сервер к которому отправлется запрос на воспроизведения аудиокниги. */
	private static AudioServer server;

	/** Плеер который используется для проигрывания треков */
	private static MediaPlayer mediaPlayer;

	/**
	 * Поток отображающий время проигрывания, а также ведущий учёт времени для
	 * бесплатного трека.
	 */
	private Thread postTimeThread;

//	static int metaSizeReturnValue = 0;
//	static String metaSizePrevBid = "";
//	static String metaSizePrevChid = "";
//	public int metaSizeForChapter(String bid, String chid) {
//		if (!bid.equalsIgnoreCase(metaSizePrevBid)
//				|| !chid.equalsIgnoreCase(metaSizePrevChid)) { // ratake
//			// metasize from
//			// xml for new
//			// chapter
//			metaSizePrevBid = bid;
//			metaSizePrevChid = chid;
//		} else {
//			return metaSizeReturnValue;
//		}
//
//		String strMetaSize = "";
//		for (Chapter c : chapters)
//		{
//			if(c.cId.equalsIgnoreCase(chid))
//				try {
//					strMetaSize = xPath.evaluate("file/size", c.node);
//				} catch (XPathExpressionException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//		}
////		ArrayList<String> as =  gs.s()
////					.getNodeList(
////							String.format(
////									"//abook[@id='%s']/content/track[@number='%s']/file/size",
////									bid, chid), xml);
//
////		if (as.size() != 1) {
////			Log.e("**err:", String.format(
////					"**err: invalid meta size for book: %s, chpater: %s", bid,
////					chid));
////		} else
////			metaSizeReturnValue = Integer.parseInt(as.get(0));
//		metaSizeReturnValue = Integer.parseInt(strMetaSize);
//
//		return metaSizeReturnValue;
//	}

	private float calcDownProgressForBook(String bid, String chid) {
		synchronized (this) {
			int metaTrackSize = 0;
			for (Chapter c : chapters)
			{
				if(c.cId.equalsIgnoreCase(chid))
					metaTrackSize = c.cSize;
					
			}

			int trackSize = gs.s().actualSizeForChapter(bid, chid);

			float downloadProgress = ((float) trackSize / (float) metaTrackSize) * 100.0f;

			return downloadProgress;
		}
	}

	private void db_InsertMyBook(String bid) {
		String query = "INSERT OR REPLACE INTO mybooks (abook_id, last_touched) VALUES (?, CURRENT_TIMESTAMP)";
		SQLiteDatabase db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
				SQLiteDatabase.OPEN_READWRITE
						| SQLiteDatabase.NO_LOCALIZED_COLLATORS);

		db.execSQL(query, new String[] { bid });
		db.close();
	}

	private void CreateMediaPlayer() {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		Log.d("MyTrace", "PlayerService:" + MyStackTrace.func3());
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this);
	}

	private String requestBookMeta() {
		// TODO: alert user about loading chapters

		HttpResponse response = gs.s().srvResponse(
				String.format("http://%s/bookmeta.php?bid=%s&dev=%s", gs.s()
						.Host(), bookId, gs.s().deviceId()));
		String responseString = gs.s().responseString(response);
		Assert.assertEquals(0, gs.s().handleSrvError(responseString));
		Header h = response.getFirstHeader("Bought");
		String bt = h.getValue();
		String contents = String.format("<r><bt>%s</bt></r>", bt);
		String fileName = gs.s().pathForBuy(bookId);
		boolean success = gs.s().createFileAtPath(fileName, contents);
		Assert.assertTrue(success);
		success = gs.s().createFileAtPath(gs.s().pathForBookMeta(bookId),
				responseString);
		Assert.assertTrue(success);

		// TODO: checkBuyBook

		return responseString;
	}

	private ArrayAdapter<Chapter> updateMeta() {
		final String bookMeta = gs.s().fileToString(gs.s().pathForBookMeta(bookId));

		// first set title
		try {
			 bookTitle = xPath.evaluate("/abooks/abook/title",
					new InputSource(new StringReader(bookMeta)));
		} catch (XPathExpressionException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		NodeList shows = null;
		try {
			shows = (NodeList) xPath.evaluate("/abooks/abook/content/track",
					new InputSource(new StringReader(bookMeta)),
					XPathConstants.NODESET);
		} catch (XPathExpressionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ArrayList<Chapter> cl = new ArrayList<Chapter>();
		for (int i = 0; i < shows.getLength(); i++) {
			Node show = (Node) shows.item(i);
			String cId = null;
			String cName = null;
			int cSize = 0;
			int cintLength = 0;
			String cLength = null;
			try {
				cId = xPath.evaluate("@number", show);
				cName = xPath.evaluate("name", show);
				cSize = Integer.valueOf(xPath.evaluate("file/size", show));
				cintLength =  Integer.valueOf(xPath.evaluate("file/length", show));
				cLength = String.format("%d:%02d", (int)(cintLength / 60.0),
				         (int)cintLength % 60);

			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Chapter c = new Chapter();
			c.name = cName;
			c.cId = cId;
			c.node = show;
			c.cSize = cSize;
			c.cLength = cLength;
			cl.add(c);
		}

		Collections.sort(cl, new Comparator<Chapter>() {
			@Override
			public int compare(Chapter s1, Chapter s2) {
				return s1.cId.compareToIgnoreCase(s2.cId);
			}
		});

		// assigh to chapters
		chapters = cl;

		class ChaptersAdapter extends ArrayAdapter<Chapter> {
			private ArrayList<Chapter> chapters = new ArrayList<Chapter>();

			public ChaptersAdapter(ArrayList<Chapter> ca) {
				super(PlayerActivity.this, R.layout.chapter_list_item, ca);
				chapters = ca;
			}

			String xml = bookMeta;
			final LayoutInflater inflater = getLayoutInflater();
			public View getView(int position, View convertView, ViewGroup parent) {
				View tmprow = convertView;
				//
//				 if ( convertView == null )
//				 {
				if(tmprow==null)
					tmprow = inflater.inflate(R.layout.chapter_list_item, null);
				
				final View row = tmprow;
				
				final int pos = position;
				class taskCR extends AsyncTask<Void,Void,Bundle>{
					@Override
					protected Bundle doInBackground(Void... args)
					{
						Chapter ch = chapters.get(pos);
					    
					    // setup chapter's time
		//			    String path = gs.s().pathForBookMeta(bookId);
		//			    String xml = gs.s().fileToString(path);
//					    ArrayList<String> nl = null;
//						try {
//							nl = gs.s().getNodeList(String.format("//abook[@id='%s']/content/track[@number='%s']/file/length", bookId, ch.cId), xml);
//						} catch (Exception e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						Assert.assertNotNull(nl);
//					    Assert.assertEquals(1, nl.size());
//				        int fsz = Integer.parseInt(nl.get(0));
				        int chProgress = (int) calcDownProgressForBook(bookId, ch.cId);
				        Bundle b = new Bundle();
				        b.putString("timeString", ch.cLength);
				        b.putInt("chProgress", chProgress);
				        boolean inProgress = DownloadManager.s(getContext()).IsHaveTrack(bookId, chapters.get(pos).cId);
				        b.putBoolean("inProgress", inProgress);
				        b.putString("chapterName", ch.name);
				        return b;
					}
					@Override
					protected void onPostExecute(Bundle b)
					{
						// set chapter time
				        ((TextView) row.findViewById(R.id.chapter_time))
						.setText(b.getString("timeString"));
				        
						// setup download progress
					    ProgressBar progress = ((ProgressBar) row.findViewById(R.id.chapter_progress));
					    int pgs = b.getInt("chProgress");
					    progress.setProgress(pgs);
					 
					    // setup download button
					    
						final ToggleButton tbtn = (ToggleButton) row
								.findViewById(R.id.btn_download);
						if(pgs==100)
						{
							tbtn.setVisibility(View.GONE);
							progress.setVisibility(View.GONE);
						}
						else
						{
							tbtn.setVisibility(View.VISIBLE);
							progress.setVisibility(View.VISIBLE);
						}
						// set chapter time
				        ((TextView) row.findViewById(R.id.chapter_name))
						.setText(b.getString("chapterName"));

						if(b.getBoolean("inProgress"))
							tbtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
					}
				}
				AsyncTask<Void,Void,Bundle> mt = new taskCR();
				mt.execute();
//				try {
//					mt.get();
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (ExecutionException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
				// setup chapter name
				((TextView) row.findViewById(R.id.chapter_name))
						.setText(chapters.get(pos).name);

				// setup download button
				final ToggleButton tbtn = (ToggleButton) row
						.findViewById(R.id.btn_download);
				tbtn.setTag(String.valueOf(position));
				tbtn.setOnClickListener(new ToggleButton.OnClickListener() {
					@Override
					public void onClick(View v) {

						// message box
						// Toast.makeText(getApplicationContext(),
						// "Click ListItem Number " + position,
						// Toast.LENGTH_SHORT)
						// .show();

						// ToggleButton tbtn = (ToggleButton)v;
						int i = Integer.parseInt(tbtn.getTag().toString());
						Chapter c = chapters.get(i);

						if (tbtn.isChecked()) {
							if(gs.s().connected())
							{
	
								downloadManager.LoadTrack(LoadingType.Chapter, bookId, c.cId);
								// removeDownqObject(chapterIdentity);
	
								tbtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
							} 
							else
							{
								AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
								builder.setMessage("Интернет не доступен!")
								       .setCancelable(false)
								       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
								           public void onClick(DialogInterface dialog, int id) {
								                //do things
								           }
								       });
								AlertDialog alert = builder.create();
								alert.show();	
							}
							
						} else { // cancel download

							// TODO: if no internet
							// tbtn.setChecked(true);
							// return;

							downloadManager.Stop(bookId, c.cId,true,true);
							downloadManager.RemoveFromQuery(bookId, c.cId);

							tbtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.download));
						}
					}
				});

				return row;
//				 }
//				 return convertView;
			}
		}

		// This is the array adapter, it takes the context of the activity as a
		// first // parameter, the type of list view as a second parameter and
		// your array as a third parameter
		ChaptersAdapter arrayAdapter = new ChaptersAdapter(cl);

		return arrayAdapter;
	}

	private View getListItem(int position) {
		int wantedPosition = position; // Whatever position you're looking for
		ListView lv = (ListView) findViewById(R.id.chapters_list_view);
		int firstPosition = lv.getFirstVisiblePosition()
				- lv.getHeaderViewsCount(); // This is the same as child #0
		int wantedChild = wantedPosition - firstPosition;
		// Say, first visible position is 8, you want position 10, wantedChild
		// will now be 2
		// So that means your view is child #2 in the ViewGroup:
		if (wantedChild < 0 || wantedChild >= lv.getChildCount()) {
			Log.w("+++warning:",
					"Unable to get view for desired position, because it's not being displayed on screen.");
			return null;
		}
		// Could also check if wantedPosition is between
		// listView.getFirstVisiblePosition() and
		// listView.getLastVisiblePosition() instead.
		View wantedView = lv.getChildAt(wantedChild);
		return wantedView;
	}

	private void checkChapter(String chid) {
		String pf = gs.s().pathForBookFinished(bookId, chid);
		File f = new File(pf);
		boolean finished_exists = f.exists();
		int actualSize = gs.s().actualSizeForChapter(bookId, chid);
		int metaSize = gs.s().metaSizeForChapter(bookId, chid);

		if ((finished_exists && actualSize < metaSize)
				|| (!finished_exists && actualSize < 400)) {
			FileManager.delete(pf);

			String pb = gs.s().pathForBookAndChapter(bookId, chid);
			FileManager.delete(pb);
		}
	}

	private void db_SaveTrackProgress() {
		// [self runOnce];

		if (mediaPlayer == null || playingChapter.length() == 0
				|| !bookId.equalsIgnoreCase(playingBookId)) {
			return;
		}

		// [self runOnce];

		float testVal = (progressbar.getProgress() * 1.0f) - 8.0f;
		float valToSave = testVal > 0.0f ? testVal : 0.0f;
		String.format("Progress : %f", valToSave);

		String query = "INSERT OR REPLACE INTO t_tracks (abook_id, track_id, current_progress) VALUES (?, ?, ?)";
		SQLiteDatabase db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
				SQLiteDatabase.OPEN_READWRITE
						| SQLiteDatabase.NO_LOCALIZED_COLLATORS);

		db.execSQL(
				query,
				new String[] { playingBookId, playingChapter,
						String.valueOf(valToSave) });
		db.close();
	}

	private void startChapter(final String chid) {
		if (!chid.equalsIgnoreCase(playingChapter)
				|| !bookId.equalsIgnoreCase(playingBookId)) {
			checkChapter(chid);

			
			class startRoutine extends AsyncTask<Void,Void,int[]>
			{

				@Override
				protected int[] doInBackground(Void... arg0) {
					// stop current download
					downloadManager.Stop(playingBookId, playingChapter, true, true);
					
					if (mediaPlayer!=null&&mediaPlayer.isPlaying())
					{
						db_SaveTrackProgress();
						mediaPlayer.stop();
					}
		
					//progressbar.setMax(gs.s().metaSizeForChapter(bookId, chid));
		
					File f = new File(gs.s().pathForBookAndChapter(bookId, chid));
					if(!f.exists())
						NeedToStartWithFirstDownloadedBytes = true;
					
					File ff = new File(gs.s().pathForBookFinished(bookId, chid));
					if(NeedToStartWithFirstDownloadedBytes || !ff.exists())
						downloadManager.LoadTrack(LoadingType.TextAndFirstChapter,
								bookId, chid);
						
					if(!NeedToStartWithFirstDownloadedBytes)
						mediaPlayerInit(bookId, chid, 0);
		
		
					//handlePlayPause();
					int m = gs.s().metaLengthForChapter(bookId, chid);
		
					int sp = (int) calcDownProgressForBook(bookId, chid);
					sp = (progressbar.getMax() * sp) / 100;
					
					return new int[]{m,sp};
				}
				
				@Override
				public void onPostExecute(int[] args)
				{
					progressbar.setMax(args[0]);
					progressbar.setProgress(0);
					progressbar.setSecondaryProgress(args[1]);					
				}
				
			}
			new startRoutine().execute();
			
			if(!gs.s().connected())
			{
				NeedToStartWithFirstDownloadedBytes = false;
				
				AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
				builder.setMessage("Для загрузки главы нужен интернет!\nИнтернет не доступен.")
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

			
			// set for onProgressChanged to trigger
			tmpPlayingBookId = bookId;
			tmpPlayingTrackId = chid;
		}
	}

	static int rowIdx = 0;

	private class Clicker1 implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> a, View view, int position,
				long id) {

			// message box
			// Toast.makeText(getApplicationContext(),
			// "Click ListItem Number " + position, Toast.LENGTH_SHORT)
			// .show();

			//

			try {
				Chapter c = chapters.get(position); // category
				int cc = chapters.size();

				if (position >= cc) {
					--rowIdx; // leave at last position
					return;
				} else if (rowIdx < 0) {
					rowIdx = 0; // set to first chapter
					return;
				}

				// make row selected
				((TextView) findViewById(R.id.titleChapter)).setText(c.name);

				// TODO: no check for tableview pointer as it is in ios

				float progress = calcDownProgressForBook(bookId, c.cId);
				View listItem = getListItem(position);

				if (progress < 100.0) {
					ToggleButton btn = (ToggleButton) listItem
							.findViewById(R.id.btn_download);
					btn.performClick();
				}

				startChapter(c.cId);

			} catch (Exception e) {
				Toast.makeText(getBaseContext(), e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private void m(String msg)
	{
	       // update the UI
			// message box
			 Toast.makeText(getApplicationContext(),
			 msg,
			 Toast.LENGTH_SHORT)
			 .show();
	}
	
	@Override
	public void onDestroy() {
	   super.onDestroy();
	   if (mHelper != null) mHelper.dispose();
	   mHelper = null;
	}
	
	IabHelper mHelper;    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MyTrace:", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d("MyTrace:", "onActivityResult handled by IABUtil.");
        }
    }
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
				
	   
	   // compute your public key and store it in base64EncodedPublicKey
	   mHelper = new IabHelper(this, gs.pk);
	   mHelper.enableDebugLogging(true);
	   mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
		   public void onIabSetupFinished(IabResult result) {
		      if (!result.isSuccess()) {
		         // Oh noes, there was a problem.
		         Log.e("MyTrace:", "Billing: Problem setting up In-app Billing: " + result);
		         return;
		      }
		      
		         // Hooray, IAB is fully set up!  
			   List<String> additionalSkuList = new ArrayList<String>();
			   additionalSkuList.add(gs.testProduct);
			   mHelper.queryInventoryAsync(true, additionalSkuList,
					   new IabHelper.QueryInventoryFinishedListener() {
				   @Override
				   public void onQueryInventoryFinished(IabResult result, Inventory inventory)   
				   {
				      if (result.isFailure()) {
				         // handle error
				    	 Log.e("MyTrace:","** error getting in-app purchase");  
				         return;
				       }

				       String applePrice =
				          inventory.getSkuDetails(gs.testProduct).getPrice();

						// message box
						m("Price: " + applePrice);

				   }});
		   }
		});
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		final ListView searchList = (ListView) findViewById(R.id.chapters_list_view);
		searchList.setClickable(true);
		searchList.setOnItemClickListener(new Clicker1());

		Intent myLocalIntent = getIntent();
		Bundle myBundle = myLocalIntent.getExtras();
		bookId = myBundle.getString("bid");
		
		class loadTask extends AsyncTask<Void,Void,ArrayAdapter<Chapter>>
		{
			private final ProgressDialog dialog = new ProgressDialog(
					PlayerActivity.this);

			// can use UI thread here
			protected void onPreExecute() {
				this.dialog.setMessage("обновление списка...");
				this.dialog.show();
			}

			@Override
			protected ArrayAdapter<Chapter> doInBackground(Void... params) {
				db_InsertMyBook(bookId);
				// ///////////////////////////////////////////

				xml = gs.s().fileToString(
						gs.s().dirsForBook(bookId) + "/bookMeta.xml");
				
				if (server == null) {
					server = new AudioServer();
					server.init();
					server.start();
				}

				downloadManager = DownloadManager.s(getApplicationContext());
				downloadManager.BindGlobalListener(PlayerActivity.this);

				File dir = new File(gs.s().pathForBookMeta(bookId));

				if (dir.exists() == false)
				{
					runOnUiThread(new Runnable() {
						@Override
						public void run()
						{
							dialog.setMessage("загрузка каталога из интернета\nпожалуйста подождите...");
						}
					});
					
					requestBookMeta();
				}

				return updateMeta();
			}
			
			@Override
			protected void onPostExecute(final ArrayAdapter<Chapter> aac)
			{
				if (this.dialog.isShowing()) {
					this.dialog.dismiss();
				}
				
				((TextView) findViewById(R.id.title)).setText(bookTitle);				
				searchList.setAdapter(aac);				
			}
		}
		new loadTask().execute();

		// setup restore button
		Button button = (Button) findViewById(R.id.btn_nfo);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntentA1A2 = new Intent(PlayerActivity.this, InfoActivity.class);
				startActivity(myIntentA1A2);
			}
		});

		// setup play button
		((Button) findViewById(R.id.btn_play))
		.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				if(mediaPlayer==null)
					startChapter(chapters.get(0).cId);
				else
					if(mediaPlayer.isPlaying())
						mediaPlayer.pause();
					else
						mediaPlayer.start();
			}
		});
		
		// setup buy button
		((Button) findViewById(R.id.btn_buy))
		.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(!gs.s().connected())
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
					builder.setMessage("Для совершения покупки нужен интернет.\nИнтернет не доступен.")
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
					mHelper.launchPurchaseFlow(PlayerActivity.this, gs.testProduct, 10001,   
							new IabHelper.OnIabPurchaseFinishedListener() {
						@Override
						public void onIabPurchaseFinished(IabResult result, Purchase purchase) 
						{
							if (result.isFailure()) {
								Log.e("MyTrace:", "**Error purchasing: " + result);
								//mHelper.handleActivityResult(0, 0, null);
								return;
							}      
							else if (purchase.getSku().equals(gs.testProduct)) {
								// TODO: check developer payload
								String dp = purchase.getDeveloperPayload();
								// consume the gas and update the UI
								m("Поздравляем, книга куплена!");
								Log.e("MyTrace:", "++Поздравляем, книга ваша! " + result);
								
								// check is useless because at this point payment already collected
								MyShop.s().startWithBook(purchase.getSku(), false);
							}
							//					      else if (purchase.getSku().equals(SKU_PREMIUM)) {
							//					         // give user access to premium content and update the UI
							//					      }
						}
					},  bookId);
				} catch (java.lang.IllegalStateException e) {
					e.printStackTrace();
					Log.e("MyTrace:","**err: unable to start purchase flow");
				}

			}
		});

		Button button2 = (Button) findViewById(R.id.btn_downloads);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntentA1A2 = new Intent(PlayerActivity.this, DownloadsActivity.class);
				startActivity(myIntentA1A2);
			}
		});

		currentTime = (TextView) findViewById(R.id.player_current_time);
		durationTime = (TextView) findViewById(R.id.player_duration_time);
		progressbar = (SeekBar) findViewById(R.id.player_progressbar);
		// TODO: init progress controls
		progressbar.setProgress(0);
		progressbar.setMax(100);
		currentTime.setText("00:00");
		durationTime.setText("00:00");
		progressbar.setSecondaryProgress(0);

		postTimeThread = new Thread(new Runnable() {
			@Override
			public void run() {
//				if (Thread.interrupted())
//					return;
				if (mediaPlayer!=null && mediaPlayer.isPlaying()) {
					// if (pausePlayButton.isChecked())
					// pausePlayButton.toggle();

					int seconds = (int) mediaPlayer.getCurrentPosition() / K;
					seconds+=offsetSecs;
					currentTime.setText(Formatters.Time(seconds));
					durationTime.setText(Formatters.Time(progressbar.getMax()-seconds));
					progressbar.setProgress(seconds);
					// if (trackInLoading
					// && progressbar.getSecondaryProgress() > 0
					// && progressbar.getProgress() >= progressbar
					// .getSecondaryProgress() + 5)
					// {
					// player.Pause();
					// if(sync != null)
					// sync.stopSync();
					// }
				} else {
					// if(!pausePlayButton.isChecked())
					// pausePlayButton.toggle();
				}

				// if(!hasTextShowedCheckeOnTimeUpdater)
				// {
				// if(textBook.getVisibility() != View.VISIBLE)
				// {
				// if(bookManager != null && bookManager.currentTrack != null)
				// {
				// errorLook = false;
				// ShowText(bookManager.GetTrackText());
				// hasTextShowedCheckeOnTimeUpdater = true;
				// }
				// }
				// }

				currentTime.postDelayed(postTimeThread, 1000);
			}

		});
		currentTime.postDelayed(postTimeThread, 1000);
		progressbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
				offsetSecs = seekBar.getProgress();
				int osec = offsetSecs;
				int max = seekBar.getMax();
				int sz = gs.s().metaSizeForChapter(playingBookId, playingChapter);
				//SeekTo(seekBar.getProgress(), false);
				long range = (long) (((double)osec / max) * (long)sz);
				mediaPlayerInit(playingBookId,playingChapter,(int)range);
			}

			@Override public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				if(fromUser)
				{
					currentTime.setText(Formatters.Time(progress));
				}
			}
		});


	}
	
	private void SeekTo(int seconds, boolean canStartPlayer)
	{
		if(mediaPlayer != null)
		{
			//boolean needStartPlay = !mediaPlayer.isPlaying();  // && !mediaPlayer.IsPausedByCall();
			
			currentTime.removeCallbacks(postTimeThread);
			//postTimeThread.stop();
			postTimeThread.interrupt();
			
//			float po = gs.s().calcDownProgressForBook(playingBookId, playingChapter);
//			if(po < 100.0)
//			{
//				if(seconds >= progressbar.getSecondaryProgress())
//				{
//					seconds = progressbar.getSecondaryProgress();
					mediaPlayer.pause();
//					if(sync != null)
//						sync.stopSync();
//				}
//			}

			
			seconds -= 5;
			if(seconds<0) // rollback a bit
				seconds = 0;
			
			progressbar.setProgress(seconds);
			currentTime.setText(Formatters.Time(seconds));
			mediaPlayer.seekTo(seconds*K);

//			if(!MyApp.UseOpenCoreMode)
//			{
//				hasTextShowedCheckeOnTimeUpdater = false;
				currentTime.postDelayed(postTimeThread, 1500);
//			}

			if(!mediaPlayer.isPlaying())
				mediaPlayer.start();
		}			
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_player, menu);
		return true;
	}

	static String tmpPlayingBookId;
	static String tmpPlayingTrackId;
	public void mediaPlayerInit(String bid, String chid, int range) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		
		runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
				playerDialog = null;
				playerDialog = new ProgressDialog(
						PlayerActivity.this);	
				playerDialog.setMessage("Загрузка главы\nПожалуйста подождите...");
				playerDialog.show();
			}
		});
		
		if (mediaPlayer == null)
			CreateMediaPlayer();

		// create new server for new track
		if(server!=null)
			server.stop();
		server = null;
		server = new AudioServer();
		server.init();
		server.start();
		
		String playUrl = "http://127.0.0.1:" + server.getPort() + "/file://"
				+ FileManager.PathToAudioFile(bid, chid);

		// TODO: set file size from meta
		playUrl += "?size="+gs.s().metaSizeForChapter(bid, chid)+"?range="+range;

		if (mediaPlayer.isPlaying())
			mediaPlayer.stop();

		mediaPlayer.reset();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		try {
			mediaPlayer.setDataSource(playUrl);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mediaPlayer.prepareAsync();
//		mediaPlayerState = PlayerState.PL_NOT_READY;
		
		tmpPlayingBookId = bid;
		tmpPlayingTrackId = chid;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());

//		mediaPlayerState = PlayerState.PL_ERROR;
		
		runOnUiThread(new Runnable() {
			@Override
			public void run()
			{
				if(playerDialog.isShowing())
					playerDialog.dismiss();
			}
		});
		
		// message box
		 Toast.makeText(getApplicationContext(),
		 "Ошибка загрузки главы",
		 Toast.LENGTH_LONG)
		 .show();

		return false;
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());

		
			runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					if(playerDialog.isShowing())
						playerDialog.dismiss();					
				}
			});
		
		// TODO Auto-generated method stub
		// synchronized (this)
		// {
		// if (mediaPlayer != null)
		// IsPrepared = true;
		// if (playerPosition > 0 && !usingOpenCoreMediaFramework)
		// mediaPlayer.seekTo(playerPosition * K);
		// tmpPosition = playerPosition;
		// }
//		mediaPlayerState = PlayerState.PL_READY;
		mediaPlayer.start();

		playingBookId = tmpPlayingBookId;
		playingChapter = tmpPlayingTrackId;
	}

	// playing book completed
	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());

			runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					progressbar.setProgress(0);
				}
			});
	}

	@Override
	public void onError(String bookID, String trackID, Errors error) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub

	}

	@Override
	public void onStart(String bookID, String trackID) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub

	}

	static boolean NeedToStartWithFirstDownloadedBytes = false;
	@Override
	public void onProgressChanged(String bookID, String trackID, int progress) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3() + " progress: "+ progress);
		
		
		if (NeedToStartWithFirstDownloadedBytes && progress > 0)
		{
			NeedToStartWithFirstDownloadedBytes = false;
			// start player
			class playerInitializer extends AsyncTask<Void,Void,Void>
			{
				@Override
				protected Void doInBackground(Void... arg0) {
					// TODO Auto-generated method stub
					mediaPlayerInit(tmpPlayingBookId, tmpPlayingTrackId,0);
					return null;
				}
			}
			new playerInitializer().execute();

		}
		
		if(bookID.equalsIgnoreCase(playingBookId)&&trackID.equalsIgnoreCase(playingChapter))
		{
			final int fprogress = progress;
			runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					progressbar.setSecondaryProgress((progressbar.getMax() * fprogress) / 100);
				}
			});
		}

		if(!bookId.equalsIgnoreCase(playingBookId))
		{ 
			Log.i("MyTrace:","++ Отображается оглавление другой книги!");
			return;
		}
		
		// TODO: sometimes chapters is null
		if(chapters==null)
			return;
		
		for (int i = 0; i < chapters.size(); i++) {
			if( chapters.get(i).cId == trackID)
			{
				View v = getListItem(i);
				if(v != null)
				{
					final ProgressBar pb = (ProgressBar) v.findViewById(R.id.chapter_progress);
					final int val = (int)calcDownProgressForBook(bookID, trackID);
					 
					runOnUiThread(new Runnable() {
						@Override
						public void run()
						{
							pb.setProgress(val);
						}
					});
					 
				}
			}
		}

	}

	@Override
	public void onTextLoadComplete(String bookID, String trackID) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub

	}

	@Override
	public void onStop(String bookID, String trackID, boolean isComplete) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());

		if(isComplete)
		{
			File f = new File(gs.s().pathForBookFinished(bookID, trackID));
			boolean created = true;
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				created = false;
			}
			Assert.assertTrue(created);
			
			for (int i = 0; i < chapters.size(); i++) {
				if( chapters.get(i).cId == trackID)
				{
					View v = getListItem(i);
					if(v != null)
					{
						final ProgressBar pb = (ProgressBar) v.findViewById(R.id.chapter_progress);
						final ToggleButton tbtn = (ToggleButton) v
								.findViewById(R.id.btn_download);
						 
						runOnUiThread(new Runnable() {
							@Override
							public void run()
							{
								tbtn.setVisibility(View.GONE);
								pb.setVisibility(View.GONE);
							}
						});
						 
					}
				}
			}

		}

	}

	@Override
	public void onBookLoaded(String bookID, String[] trackIDs) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub

	}

	@Override
	public void onPause(String bookID, String trackID) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub

	}

}
