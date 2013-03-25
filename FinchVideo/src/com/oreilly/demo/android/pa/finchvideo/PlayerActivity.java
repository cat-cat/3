package com.oreilly.demo.android.pa.finchvideo;

import ru.librofon.download.LoadingType;
import ru.librofon.download.DownloadManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class PlayerActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_player);
		
		DownloadManager downloadManager = new DownloadManager(getApplicationContext());
		downloadManager.LoadTrack(LoadingType.TextAndFirstChapter, 75316, "01_01");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_player, menu);
		return true;
	}

}
