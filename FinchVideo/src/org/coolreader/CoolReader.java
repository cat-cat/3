package org.coolreader;

//import org.coolreader.crengine.BackgroundThread;

import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.CRToolBar.OnActionHandler;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.ReaderViewLayout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class CoolReader extends BaseActivity {

	private ReaderView mReaderView;
	private ReaderViewLayout mReaderFrame;
	private Engine mEngine;

	/** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
    	startServices();
    	
//    	Intent intent = getIntent();
//    	if (intent != null && intent.getBooleanExtra("EXIT", false)) {
//    		log.i("CoolReader.onCreate() - EXIT extra parameter found: exiting app");
//   		    finish();
//   		    return;
//    	}
    	
    
		super.onCreate(savedInstanceState);		
		
		
		// apply settings
    	onSettingsChanged(settings(), null);
   	

		mEngine = Engine.getInstance(this);

		
		
		
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
    	        
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		

        //Services.getEngine().showProgress( 0, R.string.progress_starting_cool_reader );

		//this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
        //       WindowManager.LayoutParams.FLAG_FULLSCREEN );
//		startupView = new View(this) {
//		};
//		startupView.setBackgroundColor(Color.BLACK);


//		if ( DeviceInfo.FORCE_LIGHT_THEME ) {
//			setTheme(android.R.style.Theme_Light);
//			getWindow().setBackgroundDrawableResource(drawable.editbox_background);
//		}
//		if ( DeviceInfo.FORCE_LIGHT_THEME ) {
//			mFrame.setBackgroundColor( Color.WHITE );
//			setTheme(R.style.Dialog_Fullscreen_Day);
//		}
		
//		mFrame.addView(startupView);
//        log.i("initializing browser");
//        log.i("initializing reader");
//        
//        fileToLoadOnStart = null;
//		Intent intent = getIntent();
//		if ( intent!=null && Intent.ACTION_VIEW.equals(intent.getAction()) ) {
//			Uri uri = intent.getData();
//			if ( uri!=null ) {
//				fileToLoadOnStart = extractFileName(uri);
//			}
//			intent.setData(null);
//		}
        
		showRootWindow();
		
    }

	
	@Override
	protected void onResume() {
		super.onResume();
		//Properties props = SettingsManager.instance(this).get();
		
		if (mReaderView != null)
			mReaderView.onAppResume();
	}
	
	public void showRootWindow() {
		runInReader(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}});
	}
	
	private void runInReader(final Runnable task) {
		waitForCRDBService(new Runnable() {
			@Override
			public void run() {
				if (mReaderFrame != null) {
					task.run();
					setContentView(mReaderFrame);
					mReaderView.getSurface().setFocusable(true);
					mReaderView.getSurface().setFocusableInTouchMode(true);
					mReaderView.getSurface().requestFocus();
				} else {
					mReaderView = new ReaderView(CoolReader.this, mEngine, settings());
					mReaderFrame = new ReaderViewLayout(CoolReader.this, mReaderView);
			        mReaderFrame.getToolBar().setOnActionHandler(new OnActionHandler() {
						@Override
						public boolean onActionSelected(ReaderAction item) {
							if (mReaderView != null)
								mReaderView.onAction(item);
							return true;
						}
					});
					task.run();
					setContentView(mReaderFrame);
					mReaderView.getSurface().setFocusable(true);
					mReaderView.getSurface().setFocusableInTouchMode(true);
					mReaderView.getSurface().requestFocus();
//					if (initialBatteryState >= 0)
//						mReaderView.setBatteryState(initialBatteryState);
					
					loadDocument("/storage/sdcard1/Books/russkiy_eksperiment [librs.net].fb2", null);
				}
			}
		});
		
	}

	
	public void showReader() {
		runInReader(new Runnable() {
			@Override
			public void run() {
				// do nothing
			}
		});
	}

	public void updateCurrentPositionStatus(FileInfo book, Bookmark position, PositionProperties props) {
		mReaderFrame.getStatusBar().updateCurrentPositionStatus(book, position, props);
	}
	
	public void loadDocument(final String item, final Runnable callback)
	{
		mReaderView.loadDocument(item, callback);
	}
	
	public void loadDocument( FileInfo item )
	{
		loadDocument(item, null);
	}
	
	public void loadDocument( FileInfo item, Runnable callback )
	{
		Log.d("MyTrace", "CoolReader: " + "Activities.loadDocument(" + item.pathname + ")");
		loadDocument(item.getPathName(), null);
	}
	
	public void openURL(String url) {
		try {
			Intent i = new Intent(Intent.ACTION_VIEW);  
			i.setData(Uri.parse(url));  
			startActivity(i);
		} catch (Exception e) {
			Log.e("MyTrace", "CoolReader: " + "Exception " + e + " while trying to open URL " + url);
			showToast("Cannot open URL " + url);
		}
	}


}
