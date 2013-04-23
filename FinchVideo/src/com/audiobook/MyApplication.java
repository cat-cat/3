package com.audiobook;

import java.io.File;

import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.download.HttpClientImageDownloader;

import dataProvider.dbProvider.fileManager.FileManager;

import android.app.Application;
import android.os.Environment;

public class MyApplication extends Application {
	private static MyApplication singleton;
	
	public MyApplication getInstance(){
		return singleton;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		singleton = this;
		 HttpParams params = new BasicHttpParams();
	        // Turn off stale checking. Our connections break all the time anyway,
	        // and it's not worth it to pay the penalty of checking every time.
	        HttpConnectionParams.setStaleCheckingEnabled(params, false);
	        // Default connection and socket timeout of 10 seconds. Tweak to taste.
	        HttpConnectionParams.setConnectionTimeout(params, 10 * 1000);
	        HttpConnectionParams.setSoTimeout(params, 10 * 1000);
	        HttpConnectionParams.setSocketBufferSize(params, 8192);

	        // Don't handle redirects -- return them to the caller. Our code
	        // often wants to re-POST after a redirect, which we must do ourselves.
	        HttpClientParams.setRedirecting(params, false);
	        // Set the specified user agent and register standard protocols.
	        HttpProtocolParams.setUserAgent(params, "android:2.2");
	        SchemeRegistry schemeRegistry = new SchemeRegistry();
	        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

	        ClientConnectionManager manager = new ThreadSafeClientConnManager(params, schemeRegistry);

	        File cacheDir = new File( Environment.getExternalStorageDirectory()+"/Android/data/com.audiobook/cache");
	        ImageLoaderConfiguration config =
	                new ImageLoaderConfiguration
	                        .Builder(getApplicationContext())
	                       // .defaultDisplayImageOptions(defaultOptions)
	                        .discCache(new UnlimitedDiscCache(cacheDir))
	                        .threadPoolSize(2)
	                        .memoryCache(new WeakMemoryCache())
	                        .imageDownloader(new HttpClientImageDownloader(getApplicationContext(), new DefaultHttpClient(manager, params)))
	                        .build();//		ImageLoaderConfiguration config  =
//
//		          ImageLoaderConfiguration.createDefault(getApplicationContext());
				ImageLoader.getInstance().init(config);
	}
}

