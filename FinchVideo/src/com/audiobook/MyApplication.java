package com.audiobook;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import android.app.Application;

public class MyApplication extends Application {
	private static MyApplication singleton;
	
	public MyApplication getInstance(){
		return singleton;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		
		ImageLoaderConfiguration config  =

		          ImageLoaderConfiguration.createDefault(getApplicationContext());
				ImageLoader.getInstance().init(config);
	}
}

