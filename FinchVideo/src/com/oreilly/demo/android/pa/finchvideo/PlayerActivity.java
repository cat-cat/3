package com.oreilly.demo.android.pa.finchvideo;

import java.io.IOException;

import dataProvider.dbProvider.fileManager.FileManager;
import ru.audiobook.MyStackTrace;
import ru.librofon.Errors;
import ru.librofon.audioserver.AudioServer;
import ru.librofon.download.IManagerObserver;
import ru.librofon.download.LoadingType;
import ru.librofon.download.DownloadManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class PlayerActivity extends Activity implements OnCompletionListener,
OnPreparedListener, OnErrorListener, IManagerObserver {
	DownloadManager downloadManager;
//	private static int bookId = 75316;
//	private static String chapterId = "01_01"; 
	/** Сервер к которому отправлется запрос на воспроизведения аудиокниги. */
	private static AudioServer server;
	/** Плеер который используется для проигрывания треков */
	private static MediaPlayer mediaPlayer;

		
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
        		downloadManager.LoadTrack(LoadingType.TextAndFirstChapter, 75316, "01_01"); // Юмористические
            }
        });
		button = (Button) findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
        		downloadManager.LoadTrack(LoadingType.TextAndFirstChapter, 43727, "01_01"); // Alice
            }
        });
		button = (Button) findViewById(R.id.button3);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
        		downloadManager.LoadTrack(LoadingType.TextAndFirstChapter, 61933, "01_01"); // Этюд в багровых тонах (аудиоспектакль)
            }
        });
		
		//PlayStart(bookId, chapterId);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_player, menu);
		return true;
	}

	public void PlayStart(int bid, String chid)
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
	public void onError(int bookID, String trackID, Errors error) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStart(int bookID, String trackID) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProgressChanged(int bookID, String trackID, int progress) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub
		if(progress==1)
			PlayStart(bookID, trackID);
		
	}

	@Override
	public void onTextLoadComplete(int bookID, String trackID) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStop(int bookID, String trackID, boolean isComplete) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBookLoaded(int bookID, String[] trackIDs) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPause(int bookID, String trackID) {Log.d("MyTrace", "PlayerActivity: " + MyStackTrace.func3());
		// TODO Auto-generated method stub
		
	}

}
