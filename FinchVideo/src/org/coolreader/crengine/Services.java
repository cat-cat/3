package org.coolreader.crengine;

import org.coolreader.crengine.Engine.HyphDict;

import android.os.Handler;
import android.util.Log;

public class Services {
	
	private static Engine mEngine;
	private static Scanner mScanner;
	private static History mHistory;
//	private static CoverpageManager mCoverpageManager;
//    private static FileSystemFolders mFSFolders;

	public static Engine getEngine() { return mEngine; }
	public static Scanner getScanner() { return mScanner; }
	public static History getHistory() { return mHistory; }
//    public static CoverpageManager getCoverpageManager() { return mCoverpageManager; }
//    public static FileSystemFolders getFileSystemFolders() { return mFSFolders; }

	public static void startServices(BaseActivity activity) {
		Log.i("MyTrace", "CoolReader: " + "First activity is created");
		// testing background thread
		//mSettings = activity.settings();
		
		BackgroundThread.instance().setGUIHandler(new Handler());
				
		mEngine = Engine.getInstance(activity);
		
        String code = activity.settings().getProperty(ReaderView.PROP_HYPHENATION_DICT, Engine.HyphDict.RUSSIAN.toString());
        Engine.HyphDict dict = HyphDict.byCode(code);
		mEngine.setHyphenationDictionary(dict);
		
       	mScanner = new Scanner(activity, mEngine);
       	mScanner.initRoots(mEngine.getMountedRootsMap());

       	mHistory = new History(mScanner);
		mScanner.setDirScanEnabled(activity.settings().getBool(ReaderView.PROP_APP_BOOK_PROPERTY_SCAN_ENABLED, true));
//		mCoverpageManager = new CoverpageManager();

//        mFSFolders = new FileSystemFolders(mScanner);
	}

	public static void stopServices() {
		Log.i("MyTrace", "CoolReader: " + "Last activity is destroyed");
//		if (mCoverpageManager == null) {
//			Log.i("MyTrace", "CoolReader: " + "Will not destroy services: finish only activity creation detected");
//			return;
//		}
//		mCoverpageManager.clear();
		BackgroundThread.instance().postBackground(new Runnable() {
			@Override
			public void run() {
				Log.i("MyTrace", "CoolReader: " + "Stopping background thread");
				if (mEngine == null)
					return;
				mEngine.uninit();
				BackgroundThread.instance().quit();
				mEngine = null;
			}
		});
		mHistory = null;
		mScanner = null;
//		mCoverpageManager = null;
	}
}
