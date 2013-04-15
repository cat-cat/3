package com.oreilly.demo.android.pa.finchvideo;

import java.io.IOException;

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
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
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
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
		
        Intent myLocalIntent = getIntent();
        Bundle myBundle = myLocalIntent.getExtras();
        bookId = myBundle.getString("bid");
        db_InsertMyBook(bookId);

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
