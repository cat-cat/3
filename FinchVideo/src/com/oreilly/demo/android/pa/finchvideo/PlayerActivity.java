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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class PlayerActivity extends Activity implements OnCompletionListener,
OnPreparedListener, OnErrorListener, IManagerObserver {

	String bookId;

	static final int K = 1000;
	/** Отображает длительность трека в формате чч:мм:сс */
	private TextView durationTime;
	/** Отображает прогресс проигрывания аудиочасти трека и позволяет по нему перемещаться */
	private SeekBar progressbar;
	/** Отображает текущий время трека в формате чч:мм:сс */
	private TextView currentTime;

	DownloadManager downloadManager;
	//	private static int bookId = 75316;
	//	private static String chapterId = "01_01";

	/** Сервер к которому отправлется запрос на воспроизведения аудиокниги. */
	private static AudioServer server;

	/** Плеер который используется для проигрывания треков */
	private static MediaPlayer mediaPlayer;

	/**
	 * Поток отображающий время проигрывания, а также ведущий учёт времени для бесплатного трека.
	 */
	private Thread postTimeThread;


	private void db_InsertMyBook(String bid)
	{
		String query = "INSERT OR REPLACE INTO mybooks (abook_id, last_touched) VALUES (?, CURRENT_TIMESTAMP)";
		SQLiteDatabase db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
				SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.NO_LOCALIZED_COLLATORS);

		db.execSQL(query, new String[]{bid});
		db.close();
	}

	private void CreateMediaPlayer()
	{Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());Log.d("MyTrace", "PlayerService:" + MyStackTrace.func3());
	mediaPlayer = new MediaPlayer();
	mediaPlayer.setOnCompletionListener(this);
	mediaPlayer.setOnPreparedListener(this);
	mediaPlayer.setOnErrorListener(this);
	}

	private String requestBookMeta()
	{
		// TODO: alert user about loading chapters

		HttpResponse response = gs.s().srvResponse(String.format("http://%s/bookmeta.php?bid=%s&dev=%s", gs.s().Host(),bookId, gs.s().deviceId()));
		String responseString = gs.s().responseString(response);
		Assert.assertEquals(0, gs.s().handleSrvError(responseString));
		Header h = response.getFirstHeader("Bought");
		String bt = h.getValue();
		String contents = String.format("<r><bt>%s</bt></r>", bt);
		String fileName = gs.s(). pathForBuy(bookId);
		boolean success = gs.s().createFileAtPath(fileName, contents);
		Assert.assertTrue(success);
		success = gs.s().createFileAtPath(gs.s().pathForBookMeta(bookId), responseString);
		Assert.assertTrue(success);

		// TODO: checkBuyBook

		return responseString;
	}

	private boolean updateMeta()
	{
		String bookMeta = gs.s().fileToString(gs.s().pathForBookMeta(bookId));
		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		
		// first set title
		try {
			String bookTitle = xPath.evaluate("/abooks/abook/title", new InputSource(new StringReader(bookMeta)));
			( ( TextView ) findViewById(R.id.title)).setText ( bookTitle);
		} catch (XPathExpressionException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		NodeList shows = null;
		try {
			shows = (NodeList) xPath.evaluate("/abooks/abook/content/track", new InputSource(new StringReader(bookMeta)), XPathConstants.NODESET);
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


        class ChaptersAdapter extends ArrayAdapter<Chapter>
        {
        	private ArrayList<Chapter> chapters;
        	public ChaptersAdapter (ArrayList<Chapter>  ca)
        	{
        		super ( PlayerActivity.this, R.layout.chapter_list_item, ca );
        		chapters = ca;
        	}

        	public View getView ( int position, View convertView, ViewGroup parent )
        	{
//        		View row = convertView;
//
//        		if ( row == null )
//        		{
                    final LayoutInflater inflater =  getLayoutInflater ( );
        			View row = inflater.inflate(R.layout.chapter_list_item, null);

        			( ( TextView ) row.findViewById(R.id.chapter_name)).setText ( chapters.get(position).name);
        			return row;
//        		}
        	}
        }
        
	    
        // This is the array adapter, it takes the context of the activity as a first // parameter, the type of list view as a second parameter and your array as a third parameter
        ChaptersAdapter arrayAdapter =      
        new ChaptersAdapter( cl);
        ListView lv = (ListView) findViewById(R.id.chapters_list_view);
       
        lv.setAdapter(arrayAdapter);
        
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_player);

	Intent myLocalIntent = getIntent();
	Bundle myBundle = myLocalIntent.getExtras();
	bookId = myBundle.getString("bid");
	db_InsertMyBook(bookId);
	/////////////////////////////////////////////
	File dir = new File(gs.s().pathForBookMeta(bookId));

	if(dir.exists() == false)
		requestBookMeta();

	updateMeta();


	if(mediaPlayer==null)
		CreateMediaPlayer();

	if(server==null)
	{
		server = new AudioServer();
		server.init();
		server.start();
	}

	downloadManager = new DownloadManager(getApplicationContext());
	downloadManager.BindGlobalListener(this);

	// TODO: change to useful code
	Button button = (Button) findViewById(R.id.button1);
	button.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
			downloadManager.LoadTrack(LoadingType.TextAndFirstChapter, bookId, "01_01"); // Юмористические
		}
	});

	//PlayStart(bookId, chapterId);

	currentTime = (TextView) findViewById(R.id.player_current_time);
	durationTime = (TextView) findViewById(R.id.player_duration_time);
	progressbar = (SeekBar) findViewById(R.id.player_progressbar);
	// TODO: init progress controls
	progressbar.setProgress(0);
	progressbar.setMax(300);
	currentTime.setText("00:00");
	durationTime.setText("00:00");
	progressbar.setSecondaryProgress(270);

	postTimeThread = new Thread(new Runnable() {
		@Override
		public void run() {
			if(Thread.interrupted())
				return;
			if (mediaPlayer.isPlaying())
			{
				//					if (pausePlayButton.isChecked())
				//						pausePlayButton.toggle();

				int seconds = (int) mediaPlayer.getCurrentPosition() / K;
				currentTime.setText(Formatters.Time(seconds));
				progressbar.setProgress(seconds);
				//					if (trackInLoading
				//							&& progressbar.getSecondaryProgress() > 0
				//							&& progressbar.getProgress() >= progressbar
				//									.getSecondaryProgress() + 5)
				//					{
				//						player.Pause();
				//						if(sync != null)
				//							sync.stopSync();
				//					}
			}
			else 
			{
				//					if(!pausePlayButton.isChecked())
				//						pausePlayButton.toggle();
			}

			//				if(!hasTextShowedCheckeOnTimeUpdater)
			//				{
			//					if(textBook.getVisibility() != View.VISIBLE)
			//					{
			//						if(bookManager != null && bookManager.currentTrack != null)
			//						{
			//							errorLook = false;
			//							ShowText(bookManager.GetTrackText());
			//							hasTextShowedCheckeOnTimeUpdater = true;
			//						}
			//					}
			//				}

			currentTime.postDelayed(postTimeThread, 1000);
		}

	});
	postTimeThread.start();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.activity_player, menu);
	return true;
	}

	public void PlayStart(String bid, String chid)
	{Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());

	String playUrl = "http://127.0.0.1:" + server.getPort() + "/file://"
			+ FileManager.PathToAudioFile(bid, chid);

	// TODO: set file size from meta
	playUrl += "?size=25000000?range=0";

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
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub
	return false;
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub
	//		synchronized (this)
	//		{
	//			if (mediaPlayer != null)
	//				IsPrepared = true;
	//			if (playerPosition > 0 && !usingOpenCoreMediaFramework)
	//				mediaPlayer.seekTo(playerPosition * K);
	//			tmpPosition = playerPosition;
	//		}
	mediaPlayer.start();		
	}

	@Override
	public void onCompletion(MediaPlayer mp) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub

	}

	@Override
	public void onError(String bookID, String trackID, Errors error) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub

	}

	@Override
	public void onStart(String bookID, String trackID) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub

	}

	@Override
	public void onProgressChanged(String bookID, String trackID, int progress) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub
	if(progress==1)
		PlayStart(bookID, trackID);

	}

	@Override
	public void onTextLoadComplete(String bookID, String trackID) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub

	}

	@Override
	public void onStop(String bookID, String trackID, boolean isComplete) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub

	}

	@Override
	public void onBookLoaded(String bookID, String[] trackIDs) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub

	}

	@Override
	public void onPause(String bookID, String trackID) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
	// TODO Auto-generated method stub

	}

}
