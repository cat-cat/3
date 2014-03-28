package ru.old;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.audiobook.MyStackTrace;
import com.audiobook.gs;


//import dataProvider.dbProvider.database.ItemNotFoundException;
//import dataProvider.dbProvider.database.Requests;
//import dataProvider.dbProvider.entities.Book;
import dataProvider.dbProvider.entities.CollectionInfo;
import dataProvider.dbProvider.fileManager.FileManager;
import dataProvider.entities.Track;
import dataProvider.internetProvider.helpers.SourceProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
//import android.util.Log;
import android.util.Log;
import android.widget.Toast;

/**
 * <p>   Менеджер обеспечивающий загрузку файлов из Интернет.
 * В данные момент загружает по одному треку за раз.
 * </p>
 * 
 * <p>   При добавлении закачки считывает из БД информацию от треках (таблица Track), 
 * и загружает данные, после завершения загрузки записывает результат в БД.</p>
 *  
 * <p>   Внешнее activity может обрабатывать как все события происходящими со всеми
 * закачками, так и события для отдельных закачек.</p>
 * 
 * <p>   Одновременно может использоваться только один {@link #globalListener}. Чтобы 
 * избежать ошибок в <code>Activity</code> в <code>onResume</code> следует добавлять 
 * наблюдателя {@link DownloadManager#BindGlobalListener(IManagerObserver)}, а в 
 * <code>onPause</code> убирать его {@link DownloadManager#UnbindGlobalListener()}.</p>
 * 
 * <p>   При добавлении книги {@link #LoadBook(LoadingType, int)}, она разделяется на 
 * треки, которые добавляются в список загрузок {@link #loadings}.
 * Наблюдать за ходом загрузки книге в целом нельзя, только по треково.
 * 
 * Загрузка аудио осуществляется последовательно.
 * </p>
 */
public class DownloadManager
{
	/**
	 * Контекст сервиса, из него будет осуществляться доступ к БД.
	 */
	Context context;
	/**
	 * Список текущих загрузок.
	 */
	ArrayList<Load> loadings;
	
	Boolean canStartAudioLoad = true;
	
	/**
	 * Менеджер для получения состояния wifi.
	 */
	WifiManager wifiManager;
	
	/**
	 * Менеджер для проверки состояния настроек.
	 */
	SharedPreferences preferences;
	
	//Requests requests;
	
	boolean pauseNoWifi = false;
	
	/**
	 * Наблюдатель от Activity, который хочет получать информацию обо всех закачках,
	 * и уже самостоятельно её обрабатывать.
	 */
	IManagerObserver globalListener = null;
	
	/**
	 * Собственный наблюдатель для менеджера, который передаёт все сообщения
	 * {@link #globalListener}, если он существует.
	 */
	IManagerObserver ownListener = new IManagerObserver() {
		
		@Override
		public void onError(String bookID, String trackID, Errors error)
		{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//			Log.d("Download", "Download Manager " + trackID + " Error: " + error.name() );
			if(trackID == null)
			{
				loadings.clear();
				//requests.RemoveBookFromDownloadList(bookID);
				return;
			}

			Load load = null;
			
//			if(error == Errors.INTERNET_CANT_READ 
//					|| error == Errors.INTERNET_OPERATION_ERROR
//					|| error == Errors.INTERNET_CANT_READ
//					|| error == Errors.INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT
//					|| error == Errors.INTERNET_SERVER_DONT_RETURN_TEXT_CONTENT
//					|| error == Errors.INTERNET_SERVER_NOT_AVAILABLE
//					|| error == Errors.RULE_ONE_FREE_TRACK_FOR_BOOK_PER_DAY )
////					|| error == Errors.RULE_MORE_THAT_USER_CAN_DOWNLOAD_PER_DAY)
//				SourceProvider.Reconnect();

			if(error != Errors.EXTERNAL_STORAGE_NEED_MORE_SPACE
				&& error != Errors.EXTERNAL_STORAGE_NOT_AVAILABLE)
			{
				synchronized (loadings)
				{
					int size = loadings.size();
					for(int i = 0; i < size; i++)
					{
						load = loadings.get(i);
						if(load.bookId.equalsIgnoreCase(bookID)  && load.trackNumber.equalsIgnoreCase(trackID))
						{
//							if(load instanceof AudioLoad)
//							{
//								if(((AudioLoad)load).downloadErrors < AudioLoad.MAX_DOWNLOAD_ERRORS)
//								{
//									if(load.Restart())
//										return;
//								}
//								if(error == Errors.RULE_ONE_FREE_TRACK_FOR_BOOK_PER_DAY)
//								{
//									requests.MarkFreeBookAsListen(bookID);
//									MyApp.SetCollectionUpdateFlag(true);
//								}
//							}
//							else
//								if(load.Restart())
//									return;
							if ( load.CanRestart() && load.Restart() )
								return;
						}
					}
				}
			}
			
			if(globalListener != null)
				synchronized (globalListener)
				{
					Errors realError = error;
					if(error == Errors.INTERNET_CANT_READ || error == Errors.INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT || error == Errors.INTERNET_SERVER_DONT_RETURN_TEXT_CONTENT || error == Errors.INTERNET_SERVER_NOT_AVAILABLE)
						if(!SourceProvider.IsNetAvailable())
							realError = Errors.INTERNET_NOT_AVAILABLE;
					
					if(!CollectionInfo.TEXT_FLAG.equalsIgnoreCase(trackID))
						globalListener.onError(bookID, trackID, realError);
					else
						if(load != null)
							globalListener.onError(bookID, trackID, realError);
				}
			
			if(!CollectionInfo.TEXT_FLAG.equalsIgnoreCase(trackID))
			{
				//requests.RemoveBookFromDownloadList(bookID);
				Load runningLoad = null;
				synchronized (loadings)
				{
					int size = loadings.size();
					for(int i = 0; i < size; i++)
					{
						load = loadings.get(i);
						if(load.bookId.equalsIgnoreCase(bookID))
						{
							if(load.IsActive())
//								load.Stop();
								runningLoad = load;
							else
							{
								if(globalListener != null)
									synchronized (globalListener)
									{
										globalListener.onStop(bookID, load.trackNumber, load.GetDownloadProcentage() >= 100);
									}
								loadings.remove(i);
								size--;
								i--;
							}
						}
						size = loadings.size();
					}
				}
				if(runningLoad != null)
					onStop(bookID, runningLoad.trackNumber, runningLoad.GetDownloadProcentage() >= 100);
			}
			else
			{
				//requests.RemoveTextLoadFromDownloadList(bookID);
				synchronized (loadings)
				{
					int size = loadings.size();
					for(int i = 0; i < size; i++)
					{
						load = loadings.get(i);
						if(load.bookId.equalsIgnoreCase(bookID) 
							&& CollectionInfo.TEXT_FLAG.equalsIgnoreCase(load.trackNumber))
						{
							loadings.remove(i);
							break;
						}
					}
				}
			}
			
			if(globalListener != null && load != null)
				synchronized (globalListener)
				{
					if(globalListener != null)
						globalListener.onStop(bookID, trackID, load.GetDownloadProcentage() >= 100);
				}
		}

		@Override
		public void onStart(String bookID, String trackID)
		{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
			if(globalListener != null)
				synchronized (globalListener)
				{
					if(globalListener != null)
						globalListener.onStart(bookID, trackID);
				}
		}

		@Override
		public void onProgressChanged(String bookID, String trackID, int progress)
		{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
			if(globalListener != null)
				synchronized (globalListener)
				{
					if(globalListener != null)
						globalListener.onProgressChanged(bookID, trackID, progress);
				}
		}
		
		@Override
		public void onTextLoadComplete(String bookID, String trackID)
		{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
			if(globalListener != null)
				synchronized (globalListener)
				{
					if(globalListener != null)
						globalListener.onTextLoadComplete(bookID, trackID);
				}
		}

		/** Запускает следующую загрузку. Следующая загрузка ищется сначала списка. */ 
		private void StartNextLoading()
		{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
			boolean loadingStarted = false;
			if(!pauseNoWifi)
			{
				for(Load l : DownloadManager.this.loadings)
					if(l instanceof AudioLoad)
					{
						loadingStarted = true;
						if(!l.IsActive())
							l.Start();
						break;
					}
			}
			
			synchronized (canStartAudioLoad)
			{
				if(loadingStarted)
					canStartAudioLoad = Boolean.FALSE;
				else
					canStartAudioLoad = Boolean.TRUE;
			}
		}
		
		@Override
		public void onPause(String bookID, String trackID)
		{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
			if ( globalListener != null )
				synchronized (globalListener)
				{
					if ( globalListener != null )
						globalListener.onPause(bookID, trackID);
				}
			StartNextLoading();
		}
		
		@Override
		public void onStop(String bookID, String trackID, boolean isComplete)
		{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//			Log.d("Download", "----- Before cycle -----");
//			for(Load l : loadings)
//				Log.d("Download", Integer.toString(l.bookId) + " : " + l.trackNumber);
//			Log.i("Download", "------------------------");
					
//			Load loading = null;
//			synchronized (DownloadManager.this.loadings)
//			{
//				final int size = loadings.size();
//				for(int i = 0; i < size; i++)
//				{
//					loading = loadings.get(i);
//					if(loading.bookId.equalsIgnoreCase(bookID) && loading.trackNumber.equalsIgnoreCase(trackID))
//					{
//						if(!pauseNoWifi && loading instanceof AudioLoad)
//						{
//							int startI = i + 1;
//							if((startI) < size)
//							{
//								boolean loadingStarted = false; 
//								for(int j = startI; j < size; j++)
//								{
//									Load load = loadings.get(j);
//									if(load instanceof AudioLoad && !load.isRunning)
//									{
//										loadingStarted = true;
//										synchronized (canStartAudioLoad)
//										{
//											canStartAudioLoad = Boolean.FALSE;
//										}
//										load.Start();
//										break;
//									}
//								}
//								if(!loadingStarted)
//									synchronized (canStartAudioLoad)
//									{
//										canStartAudioLoad = Boolean.TRUE;
//									}
//							}
//							else
//							{
//								synchronized (canStartAudioLoad)
//								{
//									canStartAudioLoad = Boolean.TRUE;
//								}
//							}
//						}
//						loadings.remove(i);
//						break;
//					}
//				}
//			}
			
			Load loading = null;
			int size = loadings.size();
			synchronized (DownloadManager.this.loadings)
			{
				for(int i = 0; i < size; i++)
				{
					try
					{
						loading = loadings.get(i);
						if(loading.bookId.equalsIgnoreCase(bookID) && loading.trackNumber.equalsIgnoreCase(trackID))
						{
							if(globalListener != null)
								synchronized (globalListener)
								{
									if(globalListener != null)
										globalListener.onStop(bookID, trackID, isComplete);
								}
							loadings.remove(i);
							break;
						}
					}
					catch(NullPointerException e)
					{
						e.printStackTrace();
					}
					catch(IndexOutOfBoundsException e)
					{
						e.printStackTrace();
					}
				}
			}
			
			boolean loadingStarted = false;
			if(!pauseNoWifi)
			{
				for(Load l : DownloadManager.this.loadings)
					if(l instanceof AudioLoad)
					{
						loadingStarted = true;
						if(!l.IsActive())
							l.Start();
						break;
					}
			}
			
			synchronized (canStartAudioLoad)
			{
				if(loadingStarted)
					canStartAudioLoad = Boolean.FALSE;
				else
					canStartAudioLoad = Boolean.TRUE;
			}

			
//			if(loading != null && !pauseNoWifi)
//				requests.RemoveFromDownloadList(loading);
			
//			if(loading != null && globalListener != null)
//				synchronized (globalListener)
//				{
//					if(globalListener != null)
//						globalListener.onStop(bookID, trackID, isComplete);
//				}
		
//			Log.d("Download", "----- After cycle -----");
//			for(Load l : loadings)
//				Log.d("Download", Integer.toString(l.bookId) + " : " + l.trackNumber);
//			Log.i("Download", "------------------------");
		}

		@Override
		public void onBookLoaded(String bookID, String[] trackIDs)
		{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
			if(globalListener != null)
				synchronized (globalListener)
				{
					if(globalListener != null)
						globalListener.onBookLoaded(bookID, trackIDs);
				}
		}
	};
	
	/** Конструктор для создания менеджера, контролирует закачки
	 * @param context
	 */
	private DownloadManager(Context context)
	{
		//TODO куда-нибудь перенести
		//На всякий случай, создание папок для текста и аудио.
//		new Thread(new Runnable() {
//			
//			@Override
//			public void run()
//			{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//				new File(Environment.getExternalStorageDirectory() + FileManager.audio).mkdirs();
//				new File(Environment.getExternalStorageDirectory() + FileManager.text).mkdirs();
//			}
//		}).start();

		this.context = context;
		loadings = new ArrayList<Load>();

		wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		preferences = PreferenceManager.getDefaultSharedPreferences(context);

//		requests = new Requests(context.getApplicationContext());
	}

	private DownloadManager(){}
	private static DownloadManager _instance = null;
	public static DownloadManager s(Context context)
    {
        if (_instance == null)
        {
            _instance = new DownloadManager(context);
       }
        return _instance;
    }
	
	boolean HaveQuery()
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		boolean result = false;
		synchronized (loadings)
		{
			result = loadings.size() > 0;
		}
		return result;
	}
	
	/**
	 * Загрузить данные для трека.
	 * Если уже загружена какая-то часть файла, то будут загружаться недостающая часть данных.
	 * @param type данные которые необходимо загрузить. Доступны следующие варианты:
	 *      {@link LoadingType#Chapter}, {@link LoadingType#Text},  
	 *      {@link LoadingType#Free} и 
	 * @param bookID идентификационный номер книги
	 * @param trackNumber номер трека {@link Track#number}
	 */
	public void LoadTrack(final LoadingType type, final String bookId, final String trackNumber)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//		if(trackNumber == null)
//		{
//			Log.e("Download Manager", "Track == null in " + Integer.toString(bookId));
//			return;
//		}

//		Log.d("Test", "Start load " + Integer.toString(bookId) + " : " + trackNumber);
		final boolean startLoading = (preferences.getBoolean(Const.PREF_WIFI_ONLY_DOWNLOAD, false)) ? (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) : true;
		switch(type)
		{
			case Text:
			{
//				TextLoad text = new TextLoad(context, bookId, CollectionInfo.TEXT_FLAG, ownListener);
				synchronized (loadings)
				{
//					if(AddLoading(text, LoadingType.Text) && startLoading)
//						text.Start();
				}
				break;
			}
			case TextAndFirstChapter:
			{
//				TextLoad text = new TextLoad(context, bookId, CollectionInfo.TEXT_FLAG, ownListener);
				AudioLoad audio = new AudioLoad(context, bookId, trackNumber, ownListener);
				synchronized (loadings)
				{
//					if(AddLoading(text, LoadingType.Text) && startLoading)
//						text.Start();
//					synchronized (canStartAudioLoad)
//					{
//						AddLoadingToFront(LoadingType.TextAndFirstChapter,bookId,trackNumber);
//						if(AddLoading(audio, LoadingType.Chapter) && startLoading)
						AddLoading(audio, LoadingType.Chapter);
//							if(canStartAudioLoad == Boolean.TRUE && startLoading)
//							{
								//StopAllLoadings(false);
								audio.Start();
								canStartAudioLoad = Boolean.FALSE;
//							}
//					}
					
				}
				break;
			}
			case Chapter:
			{
				AudioLoad audio = new AudioLoad(context, bookId, trackNumber, ownListener);
				synchronized (loadings)
				{
					synchronized (canStartAudioLoad)
					{
						if(AddLoading(audio, LoadingType.Chapter) && startLoading)
							if(canStartAudioLoad == Boolean.TRUE)
							{
								audio.Start();
								canStartAudioLoad = Boolean.FALSE;
							} else
							{
								// TODO:
								 //message box
//								 Toast.makeText(null,
//								 "добавлено в очередь загрузки", Toast.LENGTH_SHORT)
//								 .show();
							}
					}
				}
				break;
			}
			case Free:
			{
//				TextLoad text = new TextLoad(context, bookId, CollectionInfo.TEXT_FLAG, ownListener);
				AudioLoad audio = new AudioLoad(context, bookId, trackNumber, ownListener);
				synchronized (loadings)
				{
//					if(AddLoading(text, LoadingType.Text) && startLoading)
//						text.Start();
					synchronized (canStartAudioLoad)
					{
						if(AddLoading(audio, LoadingType.Chapter) && startLoading)
							if(canStartAudioLoad == Boolean.TRUE)
							{
								audio.Start();
								canStartAudioLoad = Boolean.FALSE;
							}
					}
				}
				break;
			}
			case NextFree:
			{
				new Thread(new Runnable() {
					
					@Override
					public void run()
					{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
						Stop(bookId, true);
//						FileManager.DeleteFreeFiles(bookId);
//						requests.DeleteFreeChapter(bookId);
//						requests.UpdateFreeBookInfo(bookId);
						
//						TextLoad text = new TextLoad(context, bookId, CollectionInfo.TEXT_FLAG, ownListener);
						AudioLoad audio = new AudioLoad(context, bookId, trackNumber, ownListener);
						synchronized (loadings)
						{
//							AddLoading(text, LoadingType.Text);
//							if(startLoading)
//								text.Start();
							
							synchronized (canStartAudioLoad)
							{
								if(AddLoading(audio, LoadingType.Chapter) && startLoading)
									if(canStartAudioLoad == Boolean.TRUE)
									{
										audio.Start();
										canStartAudioLoad = Boolean.FALSE;
									}
							}
						}
					}
				}).start();
				break;
			}
		}
//		Log.d("Download", "----- Loadings list -----");
//		for(Load l : loadings)
//			Log.d("Download", Integer.toString(l.bookId) + " : " + l.trackNumber);
//		Log.d("Download", "----- Loadings list -----");
	}
	
	/**
	 * А хорош ли нынче Интернет?
	 * @return true - хорошо, false - совсем плох, помрёт уж скоро
	 */
	public boolean IsGoodInternet()
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		boolean canStartLoad = (preferences.getBoolean(Const.PREF_WIFI_ONLY_DOWNLOAD, false)) ? (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) : true;
		if ( canStartLoad )
		{
			ConnectivityManager connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
			if ( activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE 
					&& ( activeNetworkInfo.getSubtype() ==  TelephonyManager.NETWORK_TYPE_GPRS
					|| activeNetworkInfo.getSubtype() ==  TelephonyManager.NETWORK_TYPE_EDGE ) )
				canStartLoad = false;
		}
		return canStartLoad;
	}
	
	public boolean AddLoadingToFront(final LoadingType type, final String bookId, final String trackNumber)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		final boolean canAddToQuery = IsGoodInternet();
		
		if ( canAddToQuery )
		{
			new Thread(new Runnable() {
				
				@Override
				public void run()
				{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
					AudioLoad load = new AudioLoad(context, bookId, trackNumber, ownListener);
					//requests.AddToDownloadList(load, type);
					synchronized (loadings)
					{
						loadings.add(0, load);
						if ( canStartAudioLoad == Boolean.FALSE )
						{
							Load l = null;
							int size = loadings.size();
							for ( int i = 1; i < size; i++ )
							{
								try
								{
									l = loadings.get(i);
								}
								catch(IndexOutOfBoundsException e)
								{
									break;
								}
								if ( l instanceof AudioLoad )
								{
									if ( l.IsActive() )
									{
										if ( !l.bookId.equalsIgnoreCase( bookId)  && !l.trackNumber.equalsIgnoreCase(trackNumber) )
											l.Pause();
										else
										{
											try
											{
												loadings.remove(0);
											}
											catch(IndexOutOfBoundsException e)
											{
												break;
											}
											break;
										}
									}
									else
									{
										if ( l.bookId.equalsIgnoreCase(bookId) && l.trackNumber.equalsIgnoreCase(trackNumber) )
										{
											try
											{
												loadings.remove(i);
												i--;
												size--;
											}
											catch(IndexOutOfBoundsException e)
											{
												break;
											}
										}
									}
								}
							}
//							for ( Load l : loadings)
//							{
//								if ( l.IsActive() 
//										&& l instanceof AudioLoad 
//										&& l.bookId != bookId 
//										&& !l.trackNumber.equalsIgnoreCase(trackNumber) )
//									l.Pause();
//							}
						}
						else
						{
							synchronized (canStartAudioLoad)
							{
								load.Start();
								canStartAudioLoad = Boolean.FALSE;
							}
						}
					}
				}
			}).start();
		}
		return canAddToQuery;
	}
	
	/**
	 * Добавление трека в очередь осуществляется в отдельном потоке.
	 * Если уже загружена какая-то часть файла, то будут загружаться недостающая часть данных.
	 * @param type данные которые необходимо загрузить. Доступны следующие варианты:
	 *      {@link LoadingType#Chapter}, {@link LoadingType#Text},  
	 *      {@link LoadingType#Free} и 
	 * @param bookID идентификационный номер книги
	 * @param trackNumber номер трека {@link Track#number}
	 */
	public void FastAddToQueue(final LoadingType type, final String bookId, final String trackNumber)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		new Thread(new Runnable() {
			
			@Override
			public void run()
			{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
				LoadTrack(type, bookId, trackNumber);
			}
		}).start();
	}
	
	/**
	 * Добавление уникальной закачки в текущий список закачек {@link #loadings} 
	 * и в БД {@link FastSave#AddToDownloadList(Context, Load, LoadingType)}
	 * @param load - закачка которую необходимо добавить
	 * @param type - тип закачки {@link LoadingType#Text} или {@link LoadingType#Chapter}
	 * @return true - если закачка добавлена;
	 *    false - если не добавлена в список так как она является дубликатом.
	 */
	private boolean AddLoading(Load load, LoadingType type)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		final int size = loadings.size();
		int index = size;
		if(LoadingType.Text == type)
		{
//			for(Load loading : loadings)
//				if( loading instanceof TextLoad
//					&& loading.bookId.equalsIgnoreCase(load.bookId) 
//					&& (
//							loading.trackNumber == null
//							|| loading.trackNumber.equalsIgnoreCase(load.trackNumber)
//						)
//				   )
					return false;
		}
		else
		{
			for(int i = size - 1; i >= 0; i--)
			{
				Load loading = loadings.get(i);
				if( loading instanceof AudioLoad && loading.bookId.equalsIgnoreCase(load.bookId))
				{
					if(loading.trackNumber.equalsIgnoreCase(load.trackNumber))
						return false;
					else
					{
						if(loading.trackNumber.compareToIgnoreCase(load.trackNumber) > 0 )
						{
							if(loading.IsActive())
								break;
							else
								index = i;
						}
					}
				}
			}
		}
		loadings.add(index, load);
		//requests.AddToDownloadList(load, type);
//		FastSave.AddToDownloadList(context.getApplicationContext(), load, type);
		return true;
	}
	
//	boolean CommitSuicide()
//	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//		return !requests.HaveElementsInDowloadList() && loadings.isEmpty();
//	}
	
	void StartWifi()
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		new Thread(new Runnable() {

			@Override
			public void run()
			{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
				//ArrayList<Load> saved = requests.GetDownloadList();
			ArrayList<Load> saved = null;
				if(saved != null)
				{
					for(Load load : saved)
						load.ChangeManagerListener(DownloadManager.this.ownListener);
	
					synchronized (DownloadManager.this.loadings)
					{
						loadings.addAll(saved);
						synchronized (canStartAudioLoad)
						{
							for(Load load : loadings)
							{
								if(load instanceof AudioLoad)
								{
									if(canStartAudioLoad == Boolean.TRUE)
									{
										load.Start();
										canStartAudioLoad = Boolean.FALSE;
									}
								}
								else
								{
									load.Start();
								}
							}
						}
					}
				}
			}
		}).start();
	}
	
//	/**
//	 * Сохранить список загрузок по wifi
//	 */
//	public void SaveDownloadList()
//	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//		File list = new File(context.getFilesDir() +  "/loadings.xml");
//		if(list.exists())
//			list.delete();
//		if(loadings != null)
//			synchronized (loadings)
//			{
//				try
//				{
//					list.createNewFile();
//					
//					Writer writer = new FileWriter(list);
//					XmlSerializer serializer = Xml.newSerializer();
//					serializer.setOutput(writer);
//				    serializer.startDocument("UTF-8", true);
//				    
//				    for(Load load : loadings)
//						load.SaveToXml(null);
//				    		
//				    serializer.endDocument();
//				    serializer.flush();
//				    writer.close();
//				}
//				catch (IOException e)
//				{
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//	}
//	
//	public void LoadDownloadList()
//	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//		File list = new File(context.getFilesDir() +  "/loadings.xml");
//		if(list.exists())
//		{
//			//Загрузить данные для каждого трека
//			XmlPullParser xpp = Xml.newPullParser();
//			try
//			{
//				Reader reader =  new FileReader(list);
//				xpp.setInput(reader);
//				
//				//global.LoadPreviousTurn(xpp);
//				
//				xpp.setInput(null);
//				reader.close();
//			}
//			catch (FileNotFoundException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			catch (XmlPullParserException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			catch (IOException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
	
	/**
	 * Загрузить данные для книги вцелом.
	 * Для каждого трека создаются объекты {@link TrackLoad}, которые осуществляют
	 * загрузки книги потреково.
	 * @param type тип загрузки. Доступны следующие варианты: {@link LoadingType#AllBook}
	 *     и {@link LoadingType#AllText}
	 * @param bookID идентификационный номер книги
	 */
	public void LoadBook(final String bookId)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		new Thread(new Runnable() {
			
			@Override
			public void run()
			{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//!!! ЗАГРУЗКА НЕДОСТАЮЩЕЙ ЧАСТИ ТРЕКА				//Загрузка текста
				LoadTrack(LoadingType.Text, bookId, CollectionInfo.TEXT_FLAG);
				
				//Получение списка данных из бд.
//				Requests request = new Requests(context);
				// mychanges
//				String[] trackNumbers = requests.TrackIDsForBook(bookId);
				String[] trackNumbers = null;
				if(trackNumbers != null)
				{
//					int length = trackNumbers.length;
//					for(int i = 0; i < length; i++)
//						LoadTrack(LoadingType.Chapter, bookId, trackNumbers[i]);
					synchronized (loadings)
					{
						int size = loadings.size();
						Load load = null;
						int index = -1;
						for(int i = 0; i < size; i++)
						{
							load = loadings.get(i);
							if(load.bookId.equalsIgnoreCase(bookId) && load instanceof AudioLoad)
							{
								index = i;
								break;
							}
						}
						if(index > -1)
						{
							//Удаляем все что после трека
							index++;
							for(int i = index; i < size; i++)
							{
								load = loadings.get(i);
								if(load.bookId.equalsIgnoreCase(bookId) && load instanceof AudioLoad)
								{
									loadings.remove(i);
									i--;
									size--;
								}
							}
						}
						else
							index = size;
						
						//Добавляем треки после текущего загружаемого
						final int length = trackNumbers.length;
						final boolean startLoading = (preferences.getBoolean(Const.PREF_WIFI_ONLY_DOWNLOAD, false)) ? (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) : true;
						for(int i = 0; i < length; i++)
						{
							AudioLoad audio = new AudioLoad(context, bookId, trackNumbers[i], ownListener);
							loadings.add(index + i, audio);
//							requests.AddToDownloadList(audio, LoadingType.Chapter);
							synchronized (canStartAudioLoad)
							{
								if(startLoading && canStartAudioLoad == Boolean.TRUE)
								{
									audio.Start();
									canStartAudioLoad = Boolean.FALSE;
								}
							}
						}
					}
				}
				//Треки добавлены.
				if(ownListener != null)
					ownListener.onBookLoaded(bookId, trackNumbers);
			}
		}).start();
	}
	
	/**
	 * Добавляет наблюдателя за всеми событиями {@link DownloadManager}.
	 * @param listener экземпляр класса реализующий интерфейс {@link IManagerObserver},
	 *     который будет получать события от {@link DownloadManager}.
	 * @return true если обработчик добавлен, 
	 *     false если обработчик не добавлен.
	 * Возврат false возможен в случае, если не был убран предыдущий обработчик. 
	 */
	public boolean BindGlobalListener(IManagerObserver listener)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		globalListener = listener;
		return true;
	}
	
	/**
	 * Убирает наблюдателя за всеми событиями {@link DownloadManager}.
	 */
	public void UnbindGlobalListener()
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		if(globalListener != null)
		{
			synchronized (globalListener)
			{
				globalListener = null;
			}
		}
	}
	
	/**
	 * Находит в текущем списке закачек, закачку удовлетворящую параметрам.
	 * @param bookID идентификационный номер книги.
	 * @param trackID идентификационный номер трека.
	 * @return true если найдена, false если не найдена.
	 */
	public boolean IsHaveTrack(String bookID, String trackID)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		boolean result = false;
		synchronized (loadings)
		{
			for(Load load : loadings)
				if(load.bookId.equalsIgnoreCase(bookID) && load.trackNumber.equalsIgnoreCase(trackID))
				{
					result = true;
					break;
				}
		}
		return result;
	}
	
	/**
	 * Получить массик идентификационных номеров треков загружаемых в данный момент.
	 * @param bookID идентификационный номер книги.
	 * @return массив содержащий <code>String[]</code> номеров треков и процент закачки трека, 
	 *     если они (треки) есть. Если нет совпадений то <code>NULL</code>.
	 */
	public String[] TrackIDsForBook(String bookID)
	{
		ArrayList<String> list = new ArrayList<String>();
		synchronized (loadings)
		{
			for(Load load : loadings)
				if(load.bookId.equalsIgnoreCase(bookID))
				{
					list.add(new String(load.trackNumber));
					list.add(Integer.toString(load.GetDownloadProcentage()));
				}
		}
		
		if(list.size() > 0)
			return (String[])list.toArray(new String[0]);
		else
			return null;
	}
	
	/**
	 * Возвращает трек который находится в состоянии закачки для книги.
	 * @param bookId
	 * @return {@link Track#number} трека или <code>null</code> если для книги нет закачек в данный момент.
	 */
	public String[] TrackInDownload(String bookId)
	{
		synchronized (loadings)
		{
			for(Load load : loadings)
				if(load.bookId.equalsIgnoreCase(bookId) && load instanceof AudioLoad && load.IsActive())
					return new String[] { load.trackNumber, Integer.toString(load.GetDownloadProcentage()) };
		}
		return null;
	}
	
	/**
	 * Получение списка аудио закачек находящихся в очереди.
	 * @return
	 */
//	public int GetAudioLoadingsInQuery()
//	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//		int count = 0;
//		synchronized (loadings)
//		{
//			for(Load load : loadings)
//				if(load instanceof AudioLoad)
//					count++;
//		}
////		if(count == 0)
////			synchronized (canStartAudioLoad)
////			{
////				canStartAudioLoad = Boolean.TRUE;
////			}
//		return count;
//	}
	
	/**
	 * Получение названия книги, у которой сейчас загружается аудиофрагмент.
	 * @return <li>Название книги 
	 *	<li> NULL если книги нет 
	 *	<li> EMPTY STRING если произошла ошибка.
	 */
	public String GetCurrentLoadingBookName()
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		String bookName = null;
		String bookId = "-1";
		
		synchronized (loadings) 
		{
			for (Load load : loadings)
				if ( load instanceof AudioLoad )
				{
					bookId.equalsIgnoreCase(load.bookId);
					break;
				}
		}
		
//		if ( bookId >= 0 )
//		{
//			if ( requests != null )
//				try 
//				{
//					Book book = requests.getBookById(bookId);
//					if ( book != null )
						bookName = "My title";
//				} 
//				catch (ItemNotFoundException e) 
//				{
//	
//				}
//			if ( bookName == null )
//				bookName = "";
//		}
		return bookName;
	}
	
	public int GetDownloadProcentage(String bookId, String trackNumber)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		synchronized (loadings)
		{	
			for(Load load : loadings)
				if(load.bookId.equalsIgnoreCase(bookId)  && load.trackNumber.equalsIgnoreCase(trackNumber))
					return load.GetDownloadProcentage();
		}
		return -1;
	}
	
	public ArrayList<Load> CurrentDownloadBooks()
	{
		ArrayList<Load> ids = new ArrayList<Load>();
		synchronized (loadings)
		{
			for(Load load : loadings)
				ids.add(load);
		}
		return ids;
	}
	
	/**
	 * Проверяет есть ли в очереди закачки треки относящиеся к книге.
	 * @param bookId идентификационный номер книги.
	 * @return <code><b>true</b></code> - если есть,
	 *      <code><b>false</b></code> - если закачек относящихся к книге нет.
	 */
	public boolean IsAnyTrackByBookLoading(String bookId)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		boolean result = false;
		synchronized (loadings)
		{
			for(Load load : loadings)
				if(load.bookId.equalsIgnoreCase(bookId) )
				{
					result = true;
					break;
				}
		}
		return result;
	}
	
	public boolean IsAnyAudioTrackInLoadingList(String bookId)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		synchronized (loadings)
		{
			for(Load load : loadings)
				if(load.bookId.equalsIgnoreCase(bookId)  && load instanceof AudioLoad)
					return true;
		}
		return false;
	}
	
	public boolean IsAnyAudioTrackLoading(String bookId)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		synchronized (loadings)
		{
			for(Load load : loadings)
				if(load.bookId.equalsIgnoreCase(bookId) && load instanceof AudioLoad && load.IsActive())
					return true;
		}
		return false;
	}
	
	public int GetBookAudioDownloadProcentage(String bookId)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		int procent = -1;
		synchronized (loadings)
		{
			for(Load load : loadings)
				if(load.bookId.equalsIgnoreCase(bookId)  && load instanceof AudioLoad)
					procent += load.GetDownloadProcentage();
		}
		return procent;
	}
	
	/**
	 * Останавливает закачку всех треков книги.
	 * @param bookId идентификационный номер книги.
	 * @param removeFromQuery true если закачку надо удалить из очереди закачек.
	 */
	public void Stop(String bookId, boolean removeFromQuery)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		synchronized (loadings)
		{
			int size = loadings.size();
			Load load = null;
			for(int i = 0; i < size; i++)
			{
				try
				{
					load = loadings.get(i);
					if(load.bookId.equalsIgnoreCase(bookId))
						Stop(bookId, load.trackNumber, true, true);
				}
				catch(IndexOutOfBoundsException e)
				{
//					Log.e("Download", "Stop IndexOutOfBoundsException");
//					e.printStackTrace();
				}
			}
//			for(Load load : loadings)
//				if(load.bookId.equalsIgnoreCase(bookId))
//					load.Stop();
		}
	}
	
	public void ForseStop(String bookId)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//		ArrayList<Track> tracks = requests.GetTracksForPlayer(bookId, false);
//		Stop(bookId, CollectionInfo.TEXT_FLAG, true, true);
//		for(Track track : tracks)
//			Stop(bookId, track.number, true, true);
		
		ArrayList<Load> bookLoadings = new ArrayList<Load>(6);
		synchronized (loadings)
		{
			Load load;
			int size = loadings.size();
			for(int i = 0; i < size; i++)
			{
				try
				{
					load = loadings.get(i);
					if(load.bookId.equalsIgnoreCase(bookId))
					{
						bookLoadings.add(load);
						loadings.remove(i);
						i--;
						size--;
					}
				}
				catch(IndexOutOfBoundsException e)
				{
					e.printStackTrace();
				}
			}
		}
		
//		requests.RemoveBookFromDownloadList(bookId);

		for(Load load : bookLoadings)
			load.Stop();
	}
	
	/**
	 * Остановить закачку трека.
	 * @param bookID идентификационный номер книги.
	 * @param trackNumber идентификационный номер трека.
	 * @param removeFromQuery true если закачку надо удалить из очереди закачек.
	 */
	public void Stop(final String bookId, final String trackNumber, final boolean removeFromQuery, final boolean removeFromList)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		new Thread(new Runnable() {
			
			@Override
			public void run()
			{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//				Log.d("Download", "----- Before Stop -----");
//				for(Load l : DownloadManager.this.loadings)
//					Log.d("Download", Integer.toString(l.bookId) + " : " + l.trackNumber);
//				Log.i("Download", "------------------------");
				
				boolean needRemoveFromDB = false;
				Load loading = null;
				synchronized (DownloadManager.this.loadings)
				{
					final int size = loadings.size();
					for(int i = 0; i < size; i++)
					{
						loading = loadings.get(i);
						if(loading.bookId.equalsIgnoreCase(bookId) && loading.trackNumber.equalsIgnoreCase(trackNumber))
						{
							if(loading.IsActive())
							{
								loading.Stop();
								if(removeFromList)
								{
//									try
//									{
//										loadings.remove(i);
//									}
//									catch(IndexOutOfBoundsException e)
//									{
//										e.printStackTrace();
//									}
									
									needRemoveFromDB = true;
									if(loadings.size() == 0)
										synchronized (canStartAudioLoad)
										{
											canStartAudioLoad = true;
										}
								}
							}
							else
							{
								if(removeFromList)
								{
									try
									{
										loadings.remove(i);
									}
									catch(IndexOutOfBoundsException e)
									{
										e.printStackTrace();
									}
								}
								
								if(removeFromQuery)
								{
									needRemoveFromDB = true;
									if(loadings.size() == 0)
										synchronized (canStartAudioLoad)
										{
											canStartAudioLoad = true;
										}
								}
							}
							break;
						}
					}
				}
//				if(removeFromQuery && needRemoveFromDB && loading != null)
//					requests.RemoveFromDownloadList(loading);
				
//				Log.d("Download", "----- After Stop -----");
//				for(Load l : DownloadManager.this.loadings)
//					Log.d("Download", Integer.toString(l.bookId) + " : " + l.trackNumber);
//				Log.i("Download", "------------------------");
			}
		}).start();
	}
	
	public void RemoveFromQuery(final String bookId, final String trackNumber)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		new Thread(new Runnable() {
			
			@Override
			public void run()
			{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
				boolean needRemoveFromDB = false;
				Load loading = null;
				synchronized (DownloadManager.this.loadings)
				{
					final int size = loadings.size();
					for(int i = 0; i < size; i++)
					{
						loading = loadings.get(i);
						if(loading.bookId.equalsIgnoreCase(bookId) && loading.trackNumber.equalsIgnoreCase(trackNumber))
						{
							if(loading.IsActive())
								loading.Stop();
							else
							{
								loadings.remove(i);
								needRemoveFromDB = true;
							}
							break;
						}
					}
				}
//				if(needRemoveFromDB && loading != null)
//					requests.RemoveFromDownloadList(loading);
			}
		}).start();
	}
	
	/**
	 * Остановить все закачки.
	 */
	public void StopAllLoadings(boolean stopBecauseWifi)
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
//		pauseNoWifi = stopBecauseWifi;
		synchronized (loadings)
		{
			
			for (Load load : loadings)
			{
				load.ChangeManagerListener(null);
				if(load.IsActive())
					load.Stop();
			}
			//loadings.clear();
		}
//		synchronized (canStartAudioLoad)
//		{
//			canStartAudioLoad = Boolean.TRUE;
//		}
	}
	
	public void StopByWifi()
	{Log.d("MyTrace", "DownloadManager: " + MyStackTrace.func3());
		synchronized (loadings)
		{
			for(Load load : loadings)
			{
				load.ChangeManagerListener(null);
				if(globalListener != null)
					globalListener.onStop(load.bookId, load.trackNumber, load.GetDownloadProcentage() == 100);
			}
			for(Load load : loadings)
				if(load.IsActive())
					load.Stop();
			loadings.clear();
			synchronized (canStartAudioLoad)
			{
				canStartAudioLoad = Boolean.TRUE;
			}
		}
	}
}
