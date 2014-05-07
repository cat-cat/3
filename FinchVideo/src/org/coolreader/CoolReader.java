package org.coolreader;

//import org.coolreader.crengine.BackgroundThread;

import org.coolreader.crengine.BaseActivity;
import org.coolreader.crengine.Bookmark;
import org.coolreader.crengine.Engine;
import org.coolreader.crengine.FileInfo;
import org.coolreader.crengine.PositionProperties;
import org.coolreader.crengine.ReaderAction;
import org.coolreader.crengine.ReaderView;
import org.coolreader.crengine.ReaderViewLayout;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class CoolReader extends BaseActivity {

	private ReaderView mReaderView;
	private ReaderViewLayout mReaderFrame;
	private Engine mEngine;

	
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
//			        mReaderFrame.getToolBar().setOnActionHandler(new OnActionHandler() {
//						@Override
//						public boolean onActionSelected(ReaderAction item) {
//							if (mReaderView != null)
//								mReaderView.onAction(item);
//							return true;
//						}
//					});
					task.run();
					setContentView(mReaderFrame);
					mReaderView.getSurface().setFocusable(true);
					mReaderView.getSurface().setFocusableInTouchMode(true);
					mReaderView.getSurface().requestFocus();
//					if (initialBatteryState >= 0)
//						mReaderView.setBatteryState(initialBatteryState);
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
		runInReader(new Runnable() {
			@Override
			public void run() {
				mReaderView.loadDocument(item, callback);
			}
		});
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
