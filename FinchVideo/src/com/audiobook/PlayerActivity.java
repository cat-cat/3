package com.audiobook;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

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
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

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
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
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

	static String previousBookIdHolder = "";
	boolean isDebuggable;
	private Tracker mGaTracker;
	private GoogleAnalytics mGaInstance;

	
	static AlertDialog.Builder dPurchaseBuilder = null;

	
	boolean buyQueryStarted = false;

	private boolean isBought = false;
	private static ProgressDialog playerDialog;

	static int offsetSecs = 0;
	XPathFactory factory = XPathFactory.newInstance();
	XPath xPath = factory.newXPath();

	String xml;
	String bookTitle;

//	enum PlayerState {
//		PL_READY, PL_ERROR, PL_NOT_READY
//	}
	
//	private static PlayerState mediaPlayerState = PlayerState.PL_NOT_READY;
	private static String pendingPurchaseBookId = "";
	private static int playingProgressMax = 0;
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

		try {
			gs.db.execSQL(query, new String[] { bid });
		} catch (NullPointerException  e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		catch (SQLiteDiskIOException e) // TODO:
		{
			// TODO: handle exception
			e.printStackTrace();			
		}
	}

	private void CreateMediaPlayer() {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		Log.d("MyTrace", "PlayerService:" + MyStackTrace.func3());
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnErrorListener(this);
	}

	private boolean checkBuyBook()
	{
		boolean res = false;
		String pathb = gs.s().pathForBuy(bookId);
		File f = new File(pathb);

		if(f.exists())
		{
			String fc = gs.s().fileToString(pathb);
			if(fc.contains("yes"))
				res = true;
		}
		
		return res;
	}
	
	private String requestBookMeta() {
		// TODO: alert user about loading chapters

		HttpResponse response = gs.s().srvResponse(
				String.format("http://%s/bookmeta.php?bid=%s&dev=%s", gs.s()
						.Host(), bookId, gs.s().deviceId()));
		String responseString = gs.s().responseString(response);
		
		// TODO: Гаргантюа и Пантагюэль - заходим, ждем загрузки главы, выходим не включая,
		// идем до мейн активити, нажимаем кнопку плеер, падаем на нижней строке.
		Assert.assertEquals(0, gs.s().handleSrvError(responseString));
		Header h = response.getFirstHeader("Bought");
		String bt = h.getValue();
		
		if(bt.equalsIgnoreCase("yes"))
			gs.s().db_setBuyBook(bookId, 1);

		String contents = String.format("<r><bt>%s</bt></r>", bt);
		String fileName = gs.s().pathForBuy(bookId);
		boolean success = gs.s().createFileAtPath(fileName, contents);
		Assert.assertTrue(success);
		success = gs.s().createFileAtPath(gs.s().pathForBookMeta(bookId),
				responseString);
		Assert.assertTrue(success);

		return responseString;
	}

	String bookAuthors = "";
	String bookPrice = "";
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

		// authors
		try {
			 bookAuthors = xPath.evaluate("/abooks/abook/authors",
					new InputSource(new StringReader(bookMeta)));
		} catch (XPathExpressionException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		// authors
		try {
			 bookPrice = xPath.evaluate("/abooks/abook/price",
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

		chapters = cl;
		
		// display chapter if return to playing book
		if(playingChapter!=null&&bookId.equalsIgnoreCase(playingBookId)) // set chapter if not null
		{
			for (int i = 0; i < chapters.size(); i++) {
				final Chapter c = chapters.get(i);
				if( c.cId.equalsIgnoreCase(playingChapter))
					runOnUiThread(new Runnable() {
						@Override
						public void run()
						{					
							((TextView) findViewById(R.id.titleChapter)).setText(c.name);
						}
					});					
			}				
		}

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
							//tbtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
							tbtn.setChecked(true);
						else
							tbtn.setChecked(false);
					}
				}
				AsyncTask<Void,Void,Bundle> mt = new taskCR();
				
				try {
					mt.execute();
				} catch (RejectedExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					try {
						wait(250);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					mt.execute();
				}
				
				try {
					mt.get();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
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
						final Chapter c = chapters.get(i);

						if (tbtn.isChecked()) {
							if(gs.s().connected())
							{
	
								new AsyncTask<Void,Void,Void>()
								{

									@Override
									protected Void doInBackground(
											Void... params) {

										downloadManager.LoadTrack(LoadingType.Chapter, bookId, c.cId);
										return null;
									}
									
								}.execute();
								
								// removeDownqObject(chapterIdentity);
								Toast.makeText(getContext(), "добавлено в очередь загрузки",
								Toast.LENGTH_SHORT).show();
	
								//tbtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
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
							//tbtn.setChecked(false);
							// return;

							new AsyncTask<Void,Void,Void>()
							{
								@Override
								protected Void doInBackground(
										Void... params) {
									downloadManager.Stop(bookId, c.cId,true,true);
									//downloadManager.RemoveFromQuery(bookId, c.cId);
									return null;
								}
							}.execute();

							//tbtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.download));
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

	// TODO:
//	int getPossibleProgressVal()
//	{
//		int actual = gs.s().actualSizeForChapter(tmpPlayingBookId, tmpPlayingTrackId);
//		int meta = gs.s().metaSizeForChapter(tmpPlayingBookId, tmpPlayingTrackId);
//		float procSize = ((float)actual/(float)meta)*100;
//		int length = gs.s().metaLengthForChapter(tmpPlayingBookId, tmpPlayingTrackId);
//		int val =(int) (procSize/100)*length;
//		
//		return val;
//	}
	
	float db_GetTrackProgress() {
		float id = 0.0f;
	    if (mediaPlayer==null || playingChapter==null || playingChapter.length()==0 || !bookId.equalsIgnoreCase(tmpPlayingBookId)) {
	        return id;
	    }
		
		String sql = String.format("SELECT current_progress, current_progress _id from t_tracks where track_id='%s' AND abook_id='%s'"
               + " LIMIT 0,1", tmpPlayingTrackId, tmpPlayingBookId);

		Cursor c = gs.db.rawQuery(sql, null);

		int idxid = c.getColumnIndex("current_progress");
		if (c.moveToFirst()) {
			do { 
				id = c.getFloat(idxid);
			}
			while (c.moveToNext());
		}

		return id;
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

		gs.db.execSQL(
				query,
				new String[] { playingBookId, playingChapter,
						String.valueOf(valToSave) });
	}

	private void startChapter(final String chid) {
		if (!chid.equalsIgnoreCase(playingChapter)
				|| !bookId.equalsIgnoreCase(playingBookId) || mediaPlayer==null || !mediaPlayer.isPlaying()) {

			tmpPlayingBookId = bookId;
			tmpPlayingTrackId = chid;

			showPlayerDialog();
			
			class startRoutine extends AsyncTask<Void,Void,int[]>
			{

				@Override
				protected int[] doInBackground(Void... arg0) {
					
					currentTime.postDelayed(postTimeThread, 1000);
					checkChapter(chid);
					
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
					{
						NeedToStartWithFirstDownloadedBytes = true;
						offsetSecs = 0;
					}
					
					File ff = new File(gs.s().pathForBookFinished(bookId, chid));
					if((NeedToStartWithFirstDownloadedBytes || !ff.exists()) && !downloadManager.IsHaveTrack(bookId, chid))
						downloadManager.LoadTrack(LoadingType.TextAndFirstChapter,
								bookId, chid);
						
					if(!NeedToStartWithFirstDownloadedBytes)
					{
					    float stps = db_GetTrackProgress();
						offsetSecs = (int) stps;
						int osec = offsetSecs;
						int max = playingProgressMax;
						int sz = gs.s().metaSizeForChapter(tmpPlayingBookId, tmpPlayingTrackId);
						//SeekTo(seekBar.getProgress(), false);
						long range = (long) (((double)osec / max) * (long)sz);

						// TODO: calculate possible progress val
//					    if(range!=0)
//					    {
//					        int val = getPossibleProgressVal();
//					        if(range >= val)
//					        	range = val;
//					    }

						mediaPlayerInit(bookId, chid, (int)range);
					}
		
		
					//handlePlayPause();
					int m = gs.s().metaLengthForChapter(bookId, chid);
		
					int sp = (int) calcDownProgressForBook(bookId, chid);
					sp = (m * sp) / 100;
					
					return new int[]{m,sp};
				}
				
				@Override
				public void onPostExecute(int[] args)
				{
//					if(mediaPlayer!=null && mediaPlayer.isPlaying())
//						((ImageButton) findViewById(R.id.btn_play)).setImageResource(android.R.drawable.ic_media_pause);
//					else
//						((ImageButton) findViewById(R.id.btn_play)).setImageResource(android.R.drawable.ic_media_play);

					playingProgressMax = args[0];
					progressbar.setMax(playingProgressMax);
					progressbar.setProgress(0);
					progressbar.setSecondaryProgress(args[1]);					
				}
				
			}
			new startRoutine().execute();
			
			if(!gs.s().connected())
			{
				NeedToStartWithFirstDownloadedBytes = false;
				
				m("Для загрузки главы нужен интернет!\nИнтернет не доступен.");
				return;
			}
			
		}
	}

	static int rowIdx = 0;

	private class Clicker1 implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> a, View view, final int position,
				long id) {

			//

//			try {
			
				final Chapter c = chapters.get(position); // category
				// make row selected
				((TextView) findViewById(R.id.titleChapter)).setText(c.name);
				
				int cc = chapters.size();
	
				if (position >= cc) {
					--rowIdx; // leave at last position
					return;
				} else if (rowIdx < 0) {
					rowIdx = 0; // set to first chapter
					return;
				}

			    new AsyncTask<Void,Void,Float>()
			    {


					@Override
					protected Float doInBackground(Void... params) {		
						// TODO: no check for tableview pointer as it is in ios
		
						float progress = calcDownProgressForBook(bookId, c.cId);
						return progress;
					}
					
					@Override
					protected void onPostExecute(Float progress)
					{
						View listItem = getListItem(position);
						
						if (progress < 100.0) {
							ToggleButton btn = (ToggleButton) listItem
									.findViewById(R.id.btn_download);
							if(gs.s().connected())
							{
								btn.setChecked(true);
								//btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
							}
						}

					}
			    }.execute();

				startChapter(c.cId);

//			} catch (Exception e) {
//				Toast.makeText(getBaseContext(), e.getMessage(),
//						Toast.LENGTH_SHORT).show();
//			}
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
	   
	   if(bookId.equalsIgnoreCase(playingBookId))
		   playingProgressMax = progressbar.getMax();
	   
	   if(mediaPlayer!=null && mediaPlayer.isPlaying())
	   {
		   db_SaveTrackProgress();
			if(!isBought)
			{
				mediaPlayer.pause();
				pendingPurchaseBookId=playingBookId;
			}
	   }

	   
	   try {
		if (mHelper != null) mHelper.dispose();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
    
    AlertDialog.Builder getDPurchaseBuilder()
    {
		return dPurchaseBuilder;
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    	
    	if(pendingPurchaseBookId.equalsIgnoreCase(bookId))
    	{
			AlertDialog.Builder builder = getDPurchaseBuilder();
			
			try {
				AlertDialog alert = builder.create();
				alert.show();
			}catch(Exception e)
			{
				// TODO: probably this activity is invisible when come here
				e.printStackTrace();
		         Log.e("MyTrace:", "**err: Error showing buy dialog, is activity visible?");
		         //pendingPurchaseBookId=playingBookId;
			}
    	}
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
		
		
	    isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));

		
	    if(!isDebuggable)
	    {
			// Get the GoogleAnalytics singleton. Note that the SDK uses
		    // the application context to avoid leaking the current context.
		    mGaInstance = GoogleAnalytics.getInstance(this);
	
		    // Use the GoogleAnalytics singleton to get a Tracker.
		    mGaTracker = mGaInstance.getTracker("UA-39335784-1"); // Placeholder tracking ID.
		    // The rest of your onCreate() code.
	    }

//		if (android.os.Build.VERSION.SDK_INT >= 11)
//			getActionBar().hide();
		
		dPurchaseBuilder = new AlertDialog.Builder(PlayerActivity.this)
		.setMessage("Для продолжения прослушивания необходимо купить книгу.")
				.setTitle("Ограничение прослушивания")
		       .setCancelable(false)
		       .setPositiveButton("Купить", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   buyQueryStarted = false;
		        	   pendingPurchaseBookId = "";
		                //do things
	        		   ((Button) findViewById(R.id.btn_buy)).performClick();
		           }
		       	})
	           .setNegativeButton("Отменить", new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                   // User cancelled the dialog
	        		   buyQueryStarted = false;
		        	   pendingPurchaseBookId = "";
	               }
	           });
	   
	   // compute your public key and store it in base64EncodedPublicKey
	   mHelper = new IabHelper(this, gs.pk);
	   mHelper.enableDebugLogging(false);
	   mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
		   public void onIabSetupFinished(IabResult result) {
		      if (!result.isSuccess()) {
		         // Oh noes, there was a problem.
		         Log.e("MyTrace:", "Billing: Problem setting up In-app Billing: " + result);
		         return;
		      }
		      
		         // Hooray, IAB is fully set up!  
//			   List<String> additionalSkuList = new ArrayList<String>();
//			   //additionalSkuList.add(gs.testProduct);
//			   additionalSkuList.add(bookId);
//			   mHelper.queryInventoryAsync(true, additionalSkuList,
//					   new IabHelper.QueryInventoryFinishedListener() {
//				   @Override
//				   public void onQueryInventoryFinished(IabResult result, Inventory inventory)   
//				   {
//				      if (result.isFailure()) {
//				         // handle error
//				    	 Log.e("MyTrace:","** error getting in-app purchase");  
//				         return;
//				       }
//
//				       String applePrice =
//						  inventory.getSkuDetails(gs.testProduct).getPrice();
//				          inventory.getSkuDetails(bookId).getPrice();
//
//						// message box
//				       if(!isBought)
//				    	   m("Цена: " + applePrice);
//
//				   }
//				   
//			   });
		   }
		});
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		final ListView searchList = (ListView) findViewById(R.id.chapters_list_view);
		searchList.setClickable(true);
		searchList.setOnItemClickListener(new Clicker1());

		Intent myLocalIntent = getIntent();
		Bundle myBundle = myLocalIntent.getExtras();
		bookId = myBundle.getString("bid");
		

		currentTime = (TextView) findViewById(R.id.player_current_time);
		durationTime = (TextView) findViewById(R.id.player_duration_time);
		progressbar = (SeekBar) findViewById(R.id.player_progressbar);
		progressbar.setProgress(0);
		progressbar.setMax(100);
		currentTime.setText("00:00");
		durationTime.setText("00:00");
		progressbar.setSecondaryProgress(0);
		if(bookId.equalsIgnoreCase("0")) // return to playing book
		{
			progressbar.setMax(playingProgressMax);
			
			if(playingBookId.length()>0)
				bookId = playingBookId;
			else
				bookId = previousBookIdHolder;
		}
		
		previousBookIdHolder = bookId;
		
		// setup buy button
		final Button btnBuy = ((Button) findViewById(R.id.btn_buy));
		final View priceView = findViewById(R.id.price_view);
		
		btnBuy.setOnClickListener(new View.OnClickListener() {
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
					// TODO: REMOVE gs.testProduct !!!
//					mHelper.launchPurchaseFlow(PlayerActivity.this, gs.testProduct, 10001,   
					mHelper.launchPurchaseFlow(PlayerActivity.this, bookId, 10001,   
							new IabHelper.OnIabPurchaseFinishedListener() {
						@Override
						public void onIabPurchaseFinished(IabResult result, Purchase purchase) 
						{
							if (result.isFailure()) {
								Log.e("MyTrace:", "**Error purchasing: " + result);
								//mHelper.handleActivityResult(0, 0, null);
								return;
							}      
//							else if (purchase.getSku().equals(gs.testProduct)) {
							else if (purchase.getSku().equalsIgnoreCase(bookId)) {
								// TODO: check developer payload
								String dp = purchase.getDeveloperPayload();
								// consume the gas and update the UI
								m("Поздравляем, книга куплена!");
								Log.e("MyTrace:", "++Поздравляем, книга ваша! " + result);
								
								btnBuy.setVisibility(View.GONE);
								priceView.setVisibility(View.GONE);
								isBought = true;
								
								final String sku = purchase.getSku();
								new AsyncTask<Void,Void,Void>()
								{

									@Override
									protected Void doInBackground(Void... arg0) {
										// TODO Auto-generated method stub
										//
										gs.s().db_setBuyBook(sku, 1);
										
										// check is useless because at this point payment already collected
										MyShop.s().startWithBook(sku, false);
										return null;
									}
									
								}.execute();
							}
							//					      else if (purchase.getSku().equals(SKU_PREMIUM)) {
							//					         // give user access to premium content and update the UI
							//					      }
						}
					},  bookId);
				} catch (java.lang.IllegalStateException e) {
					e.printStackTrace();
					m("Ошибка при покупке. Обратитесь в поддержку.");
					Log.e("MyTrace:","**err: unable to start purchase flow");
				}

			}
		});
		
				
		class loadTask extends AsyncTask<Void,Void,ArrayAdapter<Chapter>>
		{
			private final ProgressDialog dialog = new ProgressDialog(
					PlayerActivity.this);

			// can use UI thread here
			protected void onPreExecute() {
				this.dialog.setMessage("загрузка оглавления...");
				this.dialog.show();
			}

			@Override
			protected ArrayAdapter<Chapter> doInBackground(Void... params) {
				db_InsertMyBook(bookId);
				// ///////////////////////////////////////////
								
				isBought = checkBuyBook();
				
				xml = gs.s().fileToString(
						gs.s().dirsForBook(bookId) + "/bookMeta.xml");
				
//				if (server == null) {
//					server = new AudioServer();
//					server.init();
//					server.start();
//				}

				downloadManager = DownloadManager.s(getApplicationContext());
				downloadManager.BindGlobalListener(PlayerActivity.this);

				File dir = new File(gs.s().pathForBookMeta(bookId));

				if (dir.exists() == false)
				{
//					runOnUiThread(new Runnable() {
//						@Override
//						public void run()
//						{
//							dialog.setMessage("загрузка каталога из интернета\nпожалуйста подождите...");
//						}
//					});
					
					requestBookMeta();
				}

				return updateMeta();
			}
			
			@Override
			protected void onPostExecute(final ArrayAdapter<Chapter> aac)
			{
				try {
					if (this.dialog.isShowing()) {
						this.dialog.dismiss();
					}
				} catch (IllegalArgumentException  e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				
				((TextView) findViewById(R.id.title)).setText(bookTitle);				
				((TextView) findViewById(R.id.authors_view)).setText(bookAuthors);				
				((TextView) findViewById(R.id.price_view)).setText("$"+bookPrice);
				
				searchList.setAdapter(aac);
				
				 // Send a screen view when the Activity is displayed to the user.
				if(!isDebuggable)
					mGaTracker.sendView(bookTitle);

				
				if(!isBought)
				{
					btnBuy.setVisibility(View.VISIBLE);
					priceView.setVisibility(View.VISIBLE);
				}
			}
		}
		new loadTask().execute();

		// setup restore button
		ImageButton button = (ImageButton) findViewById(R.id.btn_nfo);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntentA1A2 = new Intent(PlayerActivity.this, InfoActivity.class);
				startActivity(myIntentA1A2);
			}
		});

		// setup play button
		((ImageButton) findViewById(R.id.btn_play))
		.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				if(mediaPlayer==null || (bookId!=playingBookId))
					if(chapters==null || chapters.size()==0)
						 Toast.makeText(getApplicationContext(),
								 "Главы не загружены.\nНажмите назад и заново выберите книгу.",
								 Toast.LENGTH_LONG)
								 .show();
					else
					{
						((ImageButton) findViewById(R.id.btn_play)).setImageResource(android.R.drawable.ic_media_pause);
						startChapter(chapters.get(0).cId);
					}
				else
					if(mediaPlayer.isPlaying())
					{
						mediaPlayer.pause();
						((ImageButton)v).setImageResource(android.R.drawable.ic_media_play);
					}
					else
					{
						mediaPlayer.start();
						((ImageButton)v).setImageResource(android.R.drawable.ic_media_pause);
					}
			}
		});



		ImageButton button2 = (ImageButton) findViewById(R.id.btn_downloads);
		button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntentA1A2 = new Intent(PlayerActivity.this, DownloadsActivity.class);
				startActivity(myIntentA1A2);
			}
		});
		
		ImageButton btnBook = (ImageButton) findViewById(R.id.btn_book);
		btnBook.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent myIntentA1A2 = new Intent(PlayerActivity.this, BookActivity.class);
				Bundle b = new Bundle();
				b.putString("bid", bookId);
				myIntentA1A2.putExtras(b);
				startActivity(myIntentA1A2);
			}
		});


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
					
					// check buy book
					if(!isBought && bookId.equalsIgnoreCase(playingBookId))
					{
						float actual = seconds;
						float max = progressbar.getMax();
						float procSize = (actual/max)*100;
						
						if(procSize>70.0 && actual>350.0 && !buyQueryStarted)
						{
							mediaPlayer.pause();
							db_SaveTrackProgress();
														
							AlertDialog.Builder builder = getDPurchaseBuilder();
							
							try {
								AlertDialog alert = builder.create();
								alert.show();
								buyQueryStarted = true;
							}catch(Exception e)
							{
								// TODO: probably this activity is invisible when come here
								e.printStackTrace();
						         Log.e("MyTrace:", "**err: Error showing buy dialog, is activity visible?");
						         m("книга не куплена");
						         pendingPurchaseBookId=playingBookId;
							}

						}
					}
					
					
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

				currentTime.postDelayed(postTimeThread, K);
			}

		});
		
		if(bookId.equalsIgnoreCase(playingBookId))
			currentTime.postDelayed(postTimeThread, K);
		
		progressbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onStopTrackingTouch(final SeekBar seekBar)
			{
				
				if(!bookId.equalsIgnoreCase(playingBookId))
				{
					seekBar.setProgress(0);
					return;
				}

				new AsyncTask<Void,Void,Long>()
				{
					@Override
					protected Long doInBackground(Void... args)
					{
						if(mediaPlayer!=null&&mediaPlayer.isPlaying())
							mediaPlayer.stop();
						
						offsetSecs = seekBar.getProgress();
						int osec = offsetSecs;
						int max = seekBar.getMax();
						int sz = gs.s().metaSizeForChapter(playingBookId, playingChapter);
						//SeekTo(seekBar.getProgress(), false);
						long range = (long) (((double)osec / max) * (long)sz);
						mediaPlayerInit(playingBookId,playingChapter,(int)range);
						
						return range;
					}
										
				}.execute();
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

	private void showPlayerDialog()
	{
		if(playerDialog==null || !playerDialog.isShowing())
		{
			runOnUiThread(new Runnable() {
				@Override
				public void run()
				{
					playerDialog = null;
					playerDialog = new ProgressDialog(
							PlayerActivity.this);	
					playerDialog.setMessage("Загрузка главы\nПожалуйста подождите...");
					
					try {	
						playerDialog.show();
					} catch (BadTokenException  e) {
						// TODO: handle exception
						e.printStackTrace();
					}

				}
			});
		}
	}
	
	static String tmpPlayingBookId;
	static String tmpPlayingTrackId;
	public void mediaPlayerInit(String bid, String chid, int range) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		
		showPlayerDialog();
		
		if (mediaPlayer == null)
			CreateMediaPlayer();

		// create new server for new track
		if(server!=null)
			server.stop();
		server = null;
		server = new AudioServer();
		server.init();
		server.start();
		
//		if(server==null)
//		{
//			server = new AudioServer();
//			server.init();
//			server.start();
//		}
		
		String playUrl = "http://127.0.0.1:" + server.getPort() + "/file://"
				+ FileManager.PathToAudioFile(bid, chid);

		// TODO: set file size from meta
		playUrl += "?size="+gs.s().metaSizeForChapter(bid, chid)+"?range="+range;

		if (mediaPlayer.isPlaying())
		{
			mediaPlayer.stop();
			//((ImageButton) findViewById(R.id.btn_play)).setImageResource(android.R.drawable.ic_media_play);
		}

		boolean error = false;
		try {
			mediaPlayer.reset();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setDataSource(playUrl);
			mediaPlayer.prepareAsync();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			error = true;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			error = true;
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			error = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			error = true;			
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			error = true;			
		}
//		mediaPlayerState = PlayerState.PL_NOT_READY;
		
		if(error)
		{
			// message box
			 Toast.makeText(getApplicationContext(),
			 "Ошибка плеера\nПопроуйте ещё раз",
			 Toast.LENGTH_LONG)
			 .show();
			 
			 return;
		}
		
		tmpPlayingBookId = bid;
		tmpPlayingTrackId = chid;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());

//		mediaPlayerState = PlayerState.PL_ERROR;
		
//		runOnUiThread(new Runnable() {
//			@Override
//			public void run()
//			{
				((ImageButton) findViewById(R.id.btn_play)).setImageResource(android.R.drawable.ic_media_play);
				try {
					if(playerDialog.isShowing())
						playerDialog.dismiss();
				} catch (IllegalArgumentException  e) {
					// TODO: handle exception
					e.printStackTrace();
				}
//			}
//		});
		
		// message box
		 Toast.makeText(getApplicationContext(),
		 "Ошибка загрузки главы",
		 Toast.LENGTH_SHORT)
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
				try {
					((ImageButton) findViewById(R.id.btn_play)).setImageResource(android.R.drawable.ic_media_pause);
					if(playerDialog.isShowing())
						playerDialog.dismiss();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}					
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

	@Override
	  public void onStart() {
	    super.onStart();
	    // The rest of your onStart() code.
	    EasyTracker.getInstance().activityStart(this); // Add this method.	    
	  }
	
	@Override
		public void onPause() {
		super.onPause();
		
	   //
	   if(tmpPlayingBookId!=null)
		   gs.shouldShowPlayerButton = true;
	}

	  @Override
	  public void onStop() {
	    super.onStop();
	    // The rest of your onStop() code.
	    EasyTracker.getInstance().activityStop(this); // Add this method.
	  }
	
	static boolean NeedToStartWithFirstDownloadedBytes = false;
	@Override
	public void onProgressChanged(String bookID, String trackID, int progress) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3() + " progress: "+ progress);
		
		File f = new File(gs.s().pathForBookAndChapter(bookID, trackID));
		long fl = f.length();
		if (NeedToStartWithFirstDownloadedBytes && fl > 12000)
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
		
		if(playingBookId.length()!=0 && !bookId.equalsIgnoreCase(playingBookId))
		{ 
			Log.i("MyTrace:","++ Отображается оглавление другой книги!");
			return;
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
		
		// TODO: sometimes chapters is null
		if(chapters==null)
			return;
		
		for (int i = 0; i < chapters.size(); i++) {
			if( chapters.get(i).cId.equalsIgnoreCase(trackID))
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
			
			if(!bookId.equalsIgnoreCase(bookID))
				return;
			
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

		if(mediaPlayer!=null&&mediaPlayer.isPlaying()&&!isBought)
		{
			((ImageButton) findViewById(R.id.btn_play)).setImageResource(android.R.drawable.ic_media_play);
			mediaPlayer.pause();
			pendingPurchaseBookId=playingBookId;
		}
	}

}
