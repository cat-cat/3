package ru.librofon.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.nio.channels.Channel;

import org.apache.http.HttpResponse;

import ru.audiobook.MyStackTrace;
import ru.librofon.Errors;
//import ru.librofon.collection.Player;

//import dataProvider.dbProvider.database.Requests;


import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * Представляет общий класс для загрузки любого файла.
 * Наследники: TextLoad, AudioLoad.
 */
abstract public class Load implements Runnable
{

	protected boolean isRunning;
	protected boolean isErrorWaiting;
	protected boolean canSendProgress;
	protected Thread thread;
	
	protected Context context;
	
	/** количество вызовов stop */
	protected char stopCount;
	
	public int bookId;
	public String trackNumber;
	
	/** Слушатель от {@link DownloadManager} за окончанием загрузки */
	protected IManagerObserver managerListener;
	
	/** Размер буфера для чтения данных из Интернет. */
	public static final int BUFFER_SIZE = 1024 * 15;
	
	protected static final int HAVE_LOADING_ERROR = -1;
	protected static final int ALL_LOADED = 100;
	
	//protected Requests requests;
	
	public ErrorThread errorThread;
	public static final int MAX_ERRORS = 800;
	public static final int MIN_ATTEMPTS_PERIOD = 30;
	public static final int MIN_TIME_TO_SLEEP = 1500; // 1.5 секунды
	public static final int MIDDLE_ATTEMPTS_PERIOD = 70;
	public static final int MIDDLE_TIME_TO_SLEEP = 4000; // 4 секунды
	public static final int MAX_TIME_TO_SLEEP = 18000; // 18 секунд
	public int errors;
	
	public Load(Context serviceContext, int bookId, String trackNumber, IManagerObserver listener)
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		this.bookId = bookId;
		this.trackNumber = trackNumber;
		this.managerListener = listener;
		this.context = serviceContext;
		
		errorThread = null;
		this.errors = 0;
		stopCount = 0;
	}
	
	public void ChangeManagerListener(IManagerObserver listener)
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		this.managerListener = listener;
	}
	
	/**
	 * Загрузка находится в активной фазе: загржает информацию или ждёт попытки для посстановления после ошибки.
	 * @return
	 */
	public boolean IsActive()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		return (isRunning || isErrorWaiting);
	}
	
	public void Start()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		isRunning = true;
		isErrorWaiting = false;
		canSendProgress = true;
		stopCount = 0;
//		if(thread == null)
		thread = new Thread(this);
		thread.start();
	}
	
	abstract boolean CanRestart();
	
	public boolean Restart()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
//		if(Player.onForeground && Player.bookId == bookId)
//			errors = 1;
//		else
//			errors++;
		
		if ( errors > 1000)
			return false;
		
//		if(errors < MAX_ERRORS)
//		{
			if(errorThread != null)
				try
				{
					errorThread.interrupt();
				}
				catch(SecurityException  e)
				{
					e.printStackTrace();
				}
			
			try
			{
				errorThread = new ErrorThread(errors);
				errorThread.start();
			}
			catch(IllegalThreadStateException e)
			{
				e.printStackTrace();
			}
			return true;
		
//		}
//		return false;
	}
	
	public void Stop()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
//		Log.d("Load", "Stop " + trackNumber);
		
		canSendProgress = false;
		isRunning = false;
		isErrorWaiting = false;
		
		boolean catchError = false;
		if(errorThread != null) // && errorThread.isAlive())
		{
			try
			{
				thread.interrupt();
			}
			catch(SecurityException  ex)
			{
				catchError = true;
			}
		}
		if (thread != null)
		{
			try
			{
				thread.interrupt();
			}
			catch(SecurityException  ex)
			{
				catchError = true;
			}
			
			try
			{
				thread.join(4000);
			}
			catch(InterruptedException ex)
			{
				catchError = true;
			}
		}
		
		if(catchError)
			onError(Errors.THREAD_STOP_ERROR);
		
		if(managerListener != null && stopCount == 0)
			managerListener.onStop(bookId, trackNumber, false);
	}
	
	public void Pause()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		stopCount = 1;
		
		Stop();
		
		if(managerListener != null)
			managerListener.onPause(bookId, trackNumber);
	}
	
	private class ErrorThread extends Thread
	{
		private final int SLEEP_TIME;
		
		public ErrorThread(int errors)
		{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
			if ( errors < MIN_ATTEMPTS_PERIOD )
				SLEEP_TIME = MIN_TIME_TO_SLEEP;
			else if ( errors < MIDDLE_ATTEMPTS_PERIOD )
				SLEEP_TIME = MIDDLE_TIME_TO_SLEEP;
			else
				SLEEP_TIME = MAX_TIME_TO_SLEEP;
		}
		
		@Override
		public void run()
		{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
			isErrorWaiting = true;
			isRunning = false;
			
			if (thread != null)
			{
				try
				{
					thread.interrupt();
				}
				catch(SecurityException  ex)
				{
					onError(Errors.THREAD_STOP_ERROR);
				}
				
				try
				{
					thread.join(4000);
				}
				catch(InterruptedException ex)
				{
					onError(Errors.THREAD_STOP_ERROR);
				}
			}
			
			try
			{
				Thread.sleep(SLEEP_TIME);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			isRunning = true;
			isErrorWaiting = false;
			thread = new Thread(Load.this);
			thread.start();
		}
	}
	
	public void run()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());

	}
	/**
	 * Проверить доступность сохранения данных на SD-CARD.
	 * @return true если на карточку можно записать данные, false во всех остальных случаях.
	 */
	protected boolean CheckCard()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		boolean result = false;
		
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state))
		{
		    result = true;
		}
		else
		{
			if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
			{
				onError(Errors.EXTERNAL_STORAGE_CANT_WRITE);
			}
			else
			{
				onError(Errors.EXTERNAL_STORAGE_NOT_AVAILABLE);
			}
		}
		return result;
	}
	
	/**
	 * Получить данные от треке из БД.
	 * @return экземпляр {@link DownloadTrack} если трек есть в базе, null если нет.
	 */
	//abstract protected void GetDataFromDb();
	
	/**
	 * Сохранение состояния закачки трека в БД.
	 * @param track данные которые нужно сохранить.
	 * @return true если сохранено, false если при сохранениии произошла ошибка.
	 */
	//abstract protected void SaveDataToDB();
	
	/**
	 * Открыть/создать файл для записи данных.
	 * @param typeOpen тип данных которые будут записаны (аудио или текст)
	 * @return файл с произвольым доступом {@link RandomAccessFile} 
	 *     или <b><code>NULL</code></b> если произошла ошибка.
	 */
	protected RandomAccessFile OpenFile(String fileName)
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		RandomAccessFile writeFile = null;

		File file = new File(fileName);
		if(file.exists() == false)
		{
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				onError(Errors.FILE_CANT_CREATE);
				return null;
			}
		}
		
		try
		{
			writeFile = new RandomAccessFile(file, "rw");
		}
		catch (FileNotFoundException e)
		{		
			onError(Errors.FILE_NOT_FOUND);
			return null;
		}
		
		return writeFile;
	}

	/**
	 * Открыть соединение с сервером для получения данных.
	 * @param downloadedBytes количество скачанных байт.
	 * @return открытое соединение.
	 */
	abstract protected HttpResponse OpenConnection(long downloadedBytes);
	
	/**
	 * Закрывает все открытые ресурсы.
	 * При закрытии <b>перехватывает и не делает ничего</b> для исключений типа {@link IOException}.
	 * @param file
	 * @param channel
	 * @param connection
	 * @param inputStream
	 */
	protected void Close(RandomAccessFile file, Channel channel, HttpURLConnection connection, final InputStream inputStream) 
	{
//		if(requests != null)
//			requests.closeDb();
		
//		if(inputStream != null)
//			new Thread(new Runnable() {
//				
//				@Override
//				public void run()
//				{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
//					try
//					{
//						if(inputStream.available() <= 1)
//							inputStream.close();
//						//Будем отваливаться по таймауту
//					}
//					catch (IOException e)
//					{
//						
//					}
//				}
//			}).start();
		
		if(connection != null)
			connection.disconnect();
		
		if(channel != null)
			try
			{
				channel.close();
			}
			catch (IOException e)
			{
			}
		
		if(file != null)
			try
			{
				file.close();
			}
			catch (IOException e)
			{
			}
	}
	
	public int GetDownloadProcentage()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		return 0;
	}
	
	public void onError(Errors error)
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		if((isRunning || error == Errors.DB_ACCESS_ERROR || error == Errors.DB_ACTION_ERROR ) 
				&& managerListener != null)
		{
//			isRunning = false;
			isErrorWaiting = true;
			managerListener.onError(bookId, trackNumber, error);
		}
		isRunning = false;
	}

	public void onStart()
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		if(errors <= 0)
			if(managerListener != null)
				managerListener.onStart(bookId, trackNumber);
	}

	public void onProgressChanged(int progress)
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		if(managerListener != null)
			if(canSendProgress || progress == 100)
				managerListener.onProgressChanged(bookId, trackNumber, progress);
	}

	public void onStop(boolean isComplete)
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		stopCount++;
		if(managerListener != null)
			managerListener.onStop(bookId, trackNumber, isComplete);
			
	}

	public void onTextLoadComplete(int bookID, String trackID)
	{Log.d("MyTrace", "Load:" + MyStackTrace.func3());
		if(managerListener != null)
			managerListener.onTextLoadComplete(bookID, trackNumber);
	}

}
