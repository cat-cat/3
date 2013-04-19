package com.oreilly.demo.android.pa.finchvideo;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import junit.framework.Assert;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import dataProvider.dbProvider.fileManager.FileManager;
import ru.audiobook.MyStackTrace;
import ru.librofon.Errors;
import ru.librofon.audioserver.AudioServer;
import ru.librofon.download.IManagerObserver;
import ru.librofon.download.LoadingType;
import ru.librofon.download.DownloadManager;

import com.audiobook.CatalogActivity;
import com.audiobook.DownloadsActivity;
import com.audiobook.Formatters;
import com.audiobook.gs;
import com.oreilly.demo.android.pa.finchvideo.R;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.app.Activity;
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

	enum PlayerState {
		PL_READY, PL_ERROR, PL_NOT_READY
	}
	
	private static PlayerState mediaPlayerState = PlayerState.PL_NOT_READY;

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

	private boolean updateMeta() {
		String bookMeta = gs.s().fileToString(gs.s().pathForBookMeta(bookId));
		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();

		// first set title
		try {
			String bookTitle = xPath.evaluate("/abooks/abook/title",
					new InputSource(new StringReader(bookMeta)));
			((TextView) findViewById(R.id.title)).setText(bookTitle);
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
			Element show = (Element) shows.item(i);
			String cId = null;
			String cName = null;
			try {
				cId = xPath.evaluate("@number", show);
				cName = xPath.evaluate("name", show);
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Chapter c = new Chapter();
			c.name = cName;
			c.cId = cId;
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
			private ArrayList<Chapter> chapters;

			public ChaptersAdapter(ArrayList<Chapter> ca) {
				super(PlayerActivity.this, R.layout.chapter_list_item, ca);
				chapters = ca;
			}

			public View getView(int position, View convertView, ViewGroup parent) {
				// View row = convertView;
				//
				// if ( row == null )
				// {
				final LayoutInflater inflater = getLayoutInflater();
				View row = inflater.inflate(R.layout.chapter_list_item, null);
				Chapter ch = chapters.get(position);
			    
			    // setup chapter's time
			    String path = gs.s().pathForBookMeta(bookId);
			    String xml = gs.s().fileToString(path);
			    ArrayList<String> nl = null;
				try {
					nl = gs.s().getNodeList(String.format("//abook[@id='%s']/content/track[@number='%s']/file/length", bookId, ch.cId), xml);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Assert.assertNotNull(nl);
			    Assert.assertEquals(1, nl.size());
		        int fsz = Integer.parseInt(nl.get(0));
		        String timeString = String.format("%d:%02d", (int)(fsz / 60.0),
		         (int)fsz % 60);
		        ((TextView) row.findViewById(R.id.chapter_time))
				.setText(timeString);
		        
				// setup download progress
			    ProgressBar progress = ((ProgressBar) row.findViewById(R.id.chapter_progress));
			    progress.setProgress((int) gs.s(). calcDownProgressForBook(bookId, ch.cId));
			    
				
				// setup chapter name and button
				((TextView) row.findViewById(R.id.chapter_name))
						.setText(chapters.get(position).name);

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
						// Toast.LENGTH_LONG)
						// .show();

						// ToggleButton tbtn = (ToggleButton)v;
						int i = Integer.parseInt(tbtn.getTag().toString());
						Chapter c = chapters.get(i);
						if (tbtn.isChecked()) {

							// TODO:
							// removeDownqObject(chapterIdentity);

							// tbtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.stop));
						} else { // cancel download

							// TODO: if no internet
							// tbtn.setChecked(true);
							// return;

							// TODO: appendChapterIdentityForDownloading

							// tbtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.download));
						}
					}
				});

				return row;
				// }
			}
		}

		// This is the array adapter, it takes the context of the activity as a
		// first // parameter, the type of list view as a second parameter and
		// your array as a third parameter
		ChaptersAdapter arrayAdapter = new ChaptersAdapter(cl);
		ListView lv = (ListView) findViewById(R.id.chapters_list_view);

		lv.setAdapter(arrayAdapter);

		return true;
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

	private void startChapter(String chid) {
		if (!chid.equalsIgnoreCase(playingChapter)
				|| !bookId.equalsIgnoreCase(playingBookId)) {
			checkChapter(chid);

			if (mediaPlayer.isPlaying())
				db_SaveTrackProgress();

			//progressbar.setMax(gs.s().metaSizeForChapter(bookId, chid));

			File f = new File(gs.s().pathForBookAndChapter(bookId, chid));
			File ff = new File(gs.s().pathForBookFinished(bookId, chid));
			if(!f.exists())
			{
				NeedToStartWithFirstDownloadedBytes = true;
				downloadManager.LoadTrack(LoadingType.TextAndFirstChapter,
					bookId, chid);
			}
			else if(!ff.exists())
			{
				downloadManager.LoadTrack(LoadingType.TextAndFirstChapter,
						bookId, chid);
			}
				
			mediaPlayerInit(bookId, chid);


			//handlePlayPause();
			int m = gs.s().metaLengthForChapter(bookId, chid);
			progressbar.setMax(m);
			progressbar.setProgress(0);

			int sp = (int) gs.s().calcDownProgressForBook(bookId, chid);
			progressbar.setSecondaryProgress((progressbar.getMax() * sp) / 100);
		}
	}

	static int rowIdx = 0;

	private class Clicker1 implements AdapterView.OnItemClickListener {
		public void onItemClick(AdapterView<?> a, View view, int position,
				long id) {

			// message box
			// Toast.makeText(getApplicationContext(),
			// "Click ListItem Number " + position, Toast.LENGTH_LONG)
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

				float progress = gs.s().calcDownProgressForBook(bookId, c.cId);
				View listItem = getListItem(position);

				if (progress < 100.0) {
					ToggleButton btn = (ToggleButton) listItem
							.findViewById(R.id.btn_download);
					btn.performClick();
				}

				startChapter(c.cId);

			} catch (Exception e) {
				Toast.makeText(getBaseContext(), e.getMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);

		final ListView searchList = (ListView) findViewById(R.id.chapters_list_view);
		searchList.setClickable(true);
		searchList.setOnItemClickListener(new Clicker1());

		Intent myLocalIntent = getIntent();
		Bundle myBundle = myLocalIntent.getExtras();
		bookId = myBundle.getString("bid");
		db_InsertMyBook(bookId);
		// ///////////////////////////////////////////
		File dir = new File(gs.s().pathForBookMeta(bookId));

		if (dir.exists() == false)
			requestBookMeta();

		updateMeta();

		if (mediaPlayer == null)
			CreateMediaPlayer();

		if (server == null) {
			server = new AudioServer();
			server.init();
			server.start();
		}

		downloadManager = DownloadManager.s(getApplicationContext());
		downloadManager.BindGlobalListener(this);

		// TODO: change to useful code
		Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(mediaPlayer.isPlaying())
					mediaPlayer.pause();
				else
					mediaPlayer.start();
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
				if (mediaPlayer.isPlaying()) {
					// if (pausePlayButton.isChecked())
					// pausePlayButton.toggle();

					int seconds = (int) mediaPlayer.getCurrentPosition() / K;
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
				SeekTo(seekBar.getProgress(), false);		
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

	String initPlayingBookId;
	String initPlayingTrackId;
	public void mediaPlayerInit(String bid, String chid) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());

		String playUrl = "http://127.0.0.1:" + server.getPort() + "/file://"
				+ FileManager.PathToAudioFile(bid, chid);

		// TODO: set file size from meta
		playUrl += "?size="+gs.s().metaSizeForChapter(bid, chid)+"?range=0";

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
		mediaPlayerState = PlayerState.PL_NOT_READY;
		
		initPlayingBookId = bid;
		initPlayingTrackId = chid;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());

		mediaPlayerState = PlayerState.PL_ERROR;
		
		playingBookId = initPlayingBookId;
		playingChapter = initPlayingTrackId;
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub
		// synchronized (this)
		// {
		// if (mediaPlayer != null)
		// IsPrepared = true;
		// if (playerPosition > 0 && !usingOpenCoreMediaFramework)
		// mediaPlayer.seekTo(playerPosition * K);
		// tmpPosition = playerPosition;
		// }
		mediaPlayerState = PlayerState.PL_READY;
		mediaPlayer.start();

		playingBookId = initPlayingBookId;
		playingChapter = initPlayingTrackId;
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
			
			
			if (NeedToStartWithFirstDownloadedBytes && progress > 0)
			{
				NeedToStartWithFirstDownloadedBytes = false;
				// start player
				mediaPlayerInit(bookID, trackID);
			}
		}

		if(!bookId.equalsIgnoreCase(playingBookId))
		{ 
			Log.i("MyTrace:","++ Отображается оглавление другой книги!");
			return;
		}
		
		for (int i = 0; i < chapters.size(); i++) {
			if( chapters.get(i).cId == trackID)
			{
				View v = getListItem(i);
				if(v != null)
				{
					final ProgressBar pb = (ProgressBar) v.findViewById(R.id.chapter_progress);
					final int val = (int)gs.s().calcDownProgressForBook(bookID, trackID);
					 
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
