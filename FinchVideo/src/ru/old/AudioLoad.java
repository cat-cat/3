package ru.old;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.audiobook.MyStackTrace;
import com.audiobook.gs;

import dataProvider.dbProvider.fileManager.FileManager;
import dataProvider.entities.Track;
import dataProvider.internetProvider.handlers.TrackDownloadUrlParser;
import dataProvider.internetProvider.helpers.CommandBuilder;
import dataProvider.internetProvider.helpers.Commands;
import dataProvider.internetProvider.helpers.SourceProvider;
//import ru.old.Errors;
//import ru.old.MyApp;
//import android.hardware.Camera.ErrorCallback;
//import dataProvider.dbProvider.database.ItemNotFoundException;
//import dataProvider.dbProvider.database.Requests;
//import dataProvider.internetProvider.Server;
//import dataProvider.internetProvider.helpers.ConnectionErrorCodes;

/**
 * Загружает аудио-часть трека.
 * Можно использовать как для загрузки оплаченного, так и бесплатного трека.
 * 
 * Если файл бесплатного трека лежит на карточке, то его надо сначала удалить, а
 * потом уже закачивать трек.
 * 
 * Чтобы закачать бесплатный файл, надо сначала удалить прошлый бесплатный файл с карточки,
 * и присвоить полю {@link Track#audioDownloadProcentage} значение 0
 */
public class AudioLoad extends Load
{
	public static final short MAX_DOWNLOAD_ERRORS = 4;
	public short downloadErrors = 0;
	
	private AudioFile downloadInfo;
	
	private Track track;
	
	private boolean needIntermediateSave = false;
	
	public AudioLoad(Context serviceContext, String bookId, String trackId, IManagerObserver listener)
	{	super(serviceContext, bookId, trackId, listener);
	Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
	}
	
	@Override
	boolean CanRestart()
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
		if ( downloadInfo == null )
			return true;
//		if ( downloadInfo.errorCode != ConnectionErrorCodes.CANT_LOAD_MORE_FREE_PARTS)
//			return true;
		if ( !Track.FREE_TRACK_NUMBER.equalsIgnoreCase(trackNumber) )
			return true;
		
		return false;
//		if ( downloadInfo == null 
//				|| Track.FREE_TRACK_NUMBER.equalsIgnoreCase(trackNumber) 
//				|| downloadInfo.errorCode != ConnectionErrorCodes.CANT_LOAD_MORE_FREE_PARTS)
//			return true;
//		else
//			return false;
	}
	
	@Override
	public int GetDownloadProcentage()
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
		if(track != null)
			return track.audioDownloadProcentage;
		else
			return 0;
	}
	
	private void RefreshCreatedDate()
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
		if(track.created_at == null)
			track.created_at = Calendar.getInstance().getTime();
	}
	
	private void RefreshExpiresDate(Date expires)
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
		if(track.created_at == null)
			RefreshCreatedDate();
		
		if(expires != null && !expires.before(track.created_at) ) // expires.after(track.created_at))
			track.to = expires;
		else
			track.to = track.created_at;
//			track.GenerateNewExpiresDateToNextDay();
	}
	
	
	//@Override
//	protected void GetDataFromDb()
//	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
//		try
//		{
//			requests = new Requests(context.getApplicationContext());
//			requests.openIfClosed();
//			track = requests.getTrackByBookTrackId(bookId, trackNumber);
//		}
//		catch (ItemNotFoundException e)
//		{
//			e.printStackTrace();
//		//onError(Errors.DB_ACCESS_ERROR);
//		}
//		catch (SQLException e)
//		{
//			try
//			{
//				Thread.sleep(300);
//			}
//			catch (InterruptedException e2)
//			{
//				e2.printStackTrace();
//			}
//			try
//			{
//				if(requests == null)
//					requests = new Requests(context.getApplicationContext());
//				requests.openIfClosed();
//				track = requests.getTrackByBookTrackId(bookId, trackNumber);
//			}
//			catch (ItemNotFoundException e1)
//			{
//				e1.printStackTrace();
//			//onError(Errors.DB_ACCESS_ERROR);
//			}
//			catch (SQLException e1)
//			{
//				e1.printStackTrace();
//			//onError(Errors.DB_ACCESS_ERROR);
//			}
//		}
//	}

	//@Override
	protected void SaveDataToDB()
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
//		if(requests != null && track != null)
//		{
//			try
//			{
//				//requests.SaveTrackInfoForDownload(track);
//				//if(requests.SaveTrackInfoForDownload(track) == false)
//				//onError(Errors.DB_ACCESS_ERROR);
//			}
//			catch(SQLException e)
//			{
//				try
//				{
//					Thread.sleep(500);
//				}
//				catch (InterruptedException e1)
//				{
//					e1.printStackTrace();
//				}
//				try
//				{
//					//requests.SaveTrackInfoForDownload(track);
//					//if(requests.SaveTrackInfoForDownload(track) == false)
//					//onError(Errors.DB_ACCESS_ERROR);
//				}
//				catch(SQLException e1)
//				{
//				//onError(Errors.DB_ACCESS_ERROR);
//				}
//			}
//		}
	}
	
	private void UpdateProcentageInDB(int procentage)
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
//		if(requests != null)
//		{
//			try
//			{
//				//requests.UpdateDownloadProcentage(bookId, trackNumber, procentage);
//			}
//			catch(SQLException e)
//			{
////				Log.e("Download", "Save procentage error " + Integer.toString(bookId) + " : " + trackNumber + " - " + Integer.toString(procentage) + " %");
////				e.printStackTrace();
//			}
//		}
	}

	private void UpdateTrackTypeInDB()
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
//		if(requests != null)
//		{
//			try
//			{
//				//requests.UpdateTrackType(track.id, track.keyType);
//			}
//			catch(SQLException e)
//			{
////				Log.e("Download", "Update type error " + Integer.toString(bookId) + " : " + trackNumber + " - " + Byte.toString(track.keyType));
////				e.printStackTrace();
//			}
//		}
	}
	
	private void UpdateFreePartsCount(int partNumber)
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
//		if(requests != null)
//		{
//			try
//			{
//				//requests.UpdateFreeBookInfo(bookId, partNumber);
//			}
//			catch(SQLException e)
//			{
////				e.printStackTrace();
//			}
//		}
	}
	
	public AudioFile getTrackDownloadUrl(String requestUrl)
	{
		AudioFile file = new AudioFile();
		SourceProvider sp = new SourceProvider();
		try
		{
			InputStream source = sp.provideAudio(requestUrl, file);
			
//		BufferedReader in = null;
//		try
//		{
//			in = new BufferedReader(new InputStreamReader(source));
//		}
//		catch (IllegalStateException e3)
//		{
//			// TODO Auto-generated catch block
//			e3.printStackTrace();
//		}
//		String line = "";  
//        try
//		{
//			while ((line = in.readLine()) != null) 
//				Log.d("Download", line);
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//        try
//		{
//			in.close();
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//			
//        	source = sp.provideAudio(requestUrl, file);
			if ( source != null )
			{
				TrackDownloadUrlParser parser = new TrackDownloadUrlParser();
				file.url = parser.Parse(source, file);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return file;
	}
	
	@Override
	public HttpResponse OpenConnection(long downloadedBytes)
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());		
//		downloadInfo = null;
		CommandBuilder cb = new CommandBuilder();
		if(track.isFree())
			cb.AddCommand(Commands.getAbookFreeAudio);
		else
			cb.AddCommand(Commands.getAbookAudioByTrack);
		cb.addParam(String.valueOf(bookId));
		if(track.isFree() == false)
			cb.addParam(trackNumber);
		// mychanges
//		Server server = new Server(context);
//		downloadInfo = server.getTrackDownloadUrl(cb.GetCommand());
		//downloadInfo = getTrackDownloadUrl(cb.GetCommand());
		String devid = gs.s().deviceId();
		String url = String.format("http://%s/lrs_get_mm_file.php?bid=%s&fileid=%s&devid=%s",gs.s().Host(),bookId,trackNumber,devid);
		// DOWNLOAD THE PROJECT JSON FILE
		HttpResponse response = gs.s().srvResponse(url);
		String responseBody = gs.s().responseString(response);								
		            
        try {
        	gs.s().handleSrvError(responseBody);
			ArrayList<String> nl = gs.s().getNodeList("//chapter_path", responseBody);
			url = nl.get(0);
			url.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		
//		if(downloadInfo.url == null)
//		{
////		//onError(Errors.INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT);
//			if(downloadInfo.error)
//			{
//				downloadErrors++;
//
//				if(track.isFree())
//				{
//					RefreshExpiresDate(downloadInfo.expires);
//					SaveDataToDB();
//					//if ( downloadInfo.errorCode == ConnectionErrorCodes.CANT_LOAD_MORE_FREE_PARTS )
//					//onError(Errors.RULE_ONE_FREE_TRACK_FOR_BOOK_PER_DAY);
//					//else
//					//onError(Errors.RULE_FREE_LOADING_LIMIT);
//					
//					
////					if(track.CanDownloadNextFreeTrack())
////					{
//////						RefreshExpiresDate(downloadInfo.expires);
////						SaveDataToDB();
////						if(track.CanDownloadNextFreeTrack())
////						//onError(Errors.INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT);
////						else
////						{
//////							RefreshExpiresDate(null);
////							SaveDataToDB();
////						//onError(Errors.RULE_ONE_FREE_TRACK_FOR_BOOK_PER_DAY);
////						}
////					}
////					else
////					{
//////						RefreshExpiresDate(null);
////						SaveDataToDB();
////					//onError(Errors.RULE_ONE_FREE_TRACK_FOR_BOOK_PER_DAY);
////					}
//				}
//				else
//				{
//				//onError(Errors.INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT);
//				}
//			}
//			else
//			{
//				if(track.audioDownloadProcentage == 100)
//				{
//					if(track.file.length <= 0)
//					{
//						if(downloadInfo.duration > 0)
//							track.file.length = downloadInfo.duration;
//						else
//							track.file.length = CalculateDuration();
//					}
//					SaveDataToDB();
//					onStop(true);
//				}
//				else
//				{
//				//onError(Errors.INTERNET_SERVER_NOT_AVAILABLE);
//				}
//			}
//			return null;
//		}
//		else
//		{
//			if(track.isFree())
//			{
//				//для поддержки динамического изменения количества бесплатных частей 
//				UpdateFreePartsCount(downloadInfo.partNumber - 1);
//				if ( track.created_at == null )
//				{
//					RefreshCreatedDate();
////					RefreshExpiresDate(null);
//					RefreshExpiresDate(downloadInfo.expires);
//					SaveDataToDB();
//				}
//			}
//		}
//		
		HttpGet httpRequest = null;
		try
		{
			String deviceId = "a9f094672e47283b6fc7af77ddef829b";
			//httpRequest = new HttpGet( new URL(downloadInfo.url).toURI());
			httpRequest = new HttpGet( new URL(url).toURI());
			httpRequest.addHeader(SourceProvider.HEADER_DEVICE, deviceId);
			httpRequest.addHeader(SourceProvider.HEADER_USER_AGENT, SourceProvider.USER_AGENT);
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		if(downloadedBytes != 0)
			httpRequest.addHeader("Range", "bytes=" + Long.toString(downloadedBytes) + "-");

		AbstractHttpClient httpclient = new DefaultHttpClient();
//		httpclient.setCookieStore(SourceProvider.GetSessionCookies());
//		HttpResponse response = null;
		try
		{
			response = (HttpResponse)httpclient.execute(httpRequest);
		}
		catch (ClientProtocolException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return response;
	}
	
	@Override
	public void run()
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
		onStart();
		if(!SourceProvider.IsNetAvailable())
		{
		//onError(Errors.INTERNET_NOT_AVAILABLE);
			return;
		}
		
		//GetDataFromDb();
//		requests = new Requests(context.getApplicationContext());
//		requests.openIfClosed();
//		requests.UpdateDownloadProcentage(bookId, trackNumber, 100);

		track = new Track();
		//track.audioDownloadProcentage = (int) gs.s().calcDownProgressForBook(bookId, trackNumber);
		track.bookId = bookId;
		track.created_at = null;
//		track.file.bitrate = 192;
		track.file.size = gs.s().metaSizeForChapter(bookId, trackNumber);
		//track.file.length = gs.s().metaLengthForChapter(bookId, trackNumber);
		track.from = null;
//		track.id = 36;
		track.isFree = false;
//		track.keyType = 114;
		track.listen = 0;
//		track.name = "Брожение умов";
//		track.number = "01_01";
//		track.offset = 0;
		track.to = null;
//		requests.UpdateTrackType(track.id, track.keyType);


		if(track == null)
		{
		//onError(Errors.DB_ACCESS_ERROR);
			return;
		}
		
		if(CheckCard() == false)
		{
		//onError(Errors.EXTERNAL_STORAGE_NOT_AVAILABLE);
			SaveDataToDB();
			return;
		}
		
		String fileName = FileManager.PathToAudioFile(bookId, trackNumber);
		RandomAccessFile file = OpenFile(fileName);
		if(file == null)
		{
		//onError(Errors.FILE_CANT_ACCESS);
			Close(file, null, null, null);
			return;
		}
		
		//Загруженное количество байт
		long downloadedBytes = 0;
		try
		{
			downloadedBytes = file.length();
		}
		catch (IOException e1)
		{
		//onError(Errors.FILE_CANT_ACCESS);
			Close(file, null, null, null);
			return;
		}
		
		//Проверка на 0, так как размер бесплатной части заранее не известен.
		if(track.file.size != 0 && downloadedBytes == track.file.size)
		{
			//Если открытый файл имеет тот же размер, что лежит в track.file.size
			//значит файл закачан полность
			track.audioDownloadProcentage = 100;
			Close(file, null, null, null);
			if(track.file.length <= 0)
				track.file.length = CalculateDuration();
			SaveDataToDB();
			onStop(true);
			return;
		}
		
//		if(track.keyType == 0)
//		{
//			//Удалить файл.
//			if(downloadedBytes != 0)
//			{
//				FileManager.DeleteTrackFiles(bookId, trackNumber);
//				track.audioDownloadProcentage = 0;
//				downloadedBytes = 0;
//				UpdateProcentageInDB(0);
//			}
////			track.GenerateType();
////			UpdateTrackTypeInDB();
//		}
		
		FileChannel channel = file.getChannel();
		try
		{
			if(downloadedBytes != 0)
				channel.position(downloadedBytes);
		}
		catch (IOException e)
		{
		//onError(Errors.FILE_CANT_OPEN_CHANNEL);
			Close(file, channel, null, null);
			return;
		}

		// Открыть соединие с сервером для получения оставшейся части файла.
		HttpResponse response = OpenConnection(downloadedBytes);
		if(response == null)
		{
			Close(file, channel, null, null);
			return;
		}
		
//		Log.d("AudioLoad", "Book: " + Integer.toString(bookId) + "  Track: '" + trackNumber + "' StatusLine: " + response.getStatusLine().getReasonPhrase() + " - Code: " + Integer.toString(response.getStatusLine().getStatusCode()));
		
		if(!track.isFree())
		{
			StatusLine statusLine = response.getStatusLine(); 
			if(statusLine != null && statusLine.getStatusCode() == 416)
			{
				track.audioDownloadProcentage = 100;
				SaveDataToDB();
				onStop(true);
				return;
			}
		}
		
		
		if(track.isFree() && track.created_at == null)
		{
			RefreshCreatedDate();
			RefreshExpiresDate(null);
			SaveDataToDB();
		}
		
//		if(downloadInfo.duration > 0)
//		{
//			if(track.file.length <= 0)
//			{
//				track.file.length = downloadInfo.duration;
//				SaveDataToDB(); //FIXME проверить надо ли такое
//			}
//			else
//				track.file.length = downloadInfo.duration;
//		}
		
		HttpEntity entity = response.getEntity();
		if(entity == null)
		{
			Log.d("Download", "Audio Entitiy == null");
		//onError(Errors.INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT);
			Close(file, channel, null, null);
			return;
		}
		
		long bytesToDownload = entity.getContentLength();
		Log.d("Download", "Audio Size: " + Long.toString(bytesToDownload));
		if(bytesToDownload == -1)
		{
			//Страница не найдена
		//onError(Errors.INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT);
			Close(file, channel, null, null);
			return;
		}
		if(track.isFree() && track.file.size == 0)
		{
			track.file.size = bytesToDownload + downloadedBytes;
			SaveDataToDB();
		}
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long sdAvailSize = (long)stat.getAvailableBlocks() *(long)stat.getBlockSize();
		if(bytesToDownload >= sdAvailSize)
		{
		//onError(Errors.EXTERNAL_STORAGE_NEED_MORE_SPACE);
			Close(file, channel, null, null);
			return;
		}
		
		//Открываем поток для записи данных
		InputStream is = null;
		try
		{
			is = entity.getContent();
		}
		catch (IllegalStateException e2)
		{
			e2.printStackTrace();
		}
		catch (IOException e2)
		{
			//FIXME исправить обработку неправильной отдачи от сервера
			if(bytesToDownload < 500)
			{
				track.audioDownloadProcentage = 100;
				Close(file, channel, null, null);
				if(track.file.length <= 0)
					track.file.length = CalculateDuration();
				SaveDataToDB();
				onStop(true);
			}
			else
			{
			//onError(Errors.INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT);
				Close(file, channel, null, null);
				SaveDataToDB();
//				onStop(false);
			}
			return;
		}
		
		
		//Размер трека делённое 100 для удобства вычисления процентов 
		if(track.file.size < 100)
		{
		//onError(Errors.MYSTICAL);
			Close(file, channel, null, null);
			return;
		}
		final long size = track.file.size / 100;
		// Определяет когда будут сохранены промежуточные результаты загрузки
		final long meredian = bytesToDownload - downloadedBytes; 
		if(meredian > 5000000)
			needIntermediateSave = true;
		
		final boolean needFastSave = (bytesToDownload - downloadedBytes) > 3000000;
		
		int percent = (int) (downloadedBytes / size);
		track.audioDownloadProcentage = percent;
		onProgressChanged(percent);
		try
		{
			final byte[] transportKey = Crypt.GenerateKey(true);
			final short transportKeyLength = (short) (transportKey.length - 2);
		    short transportKeyIndex = (short) (downloadedBytes % (transportKeyLength + 1));
		    
		    final byte[] storeKey = Crypt.GenerateKey(false);
		    final short storeKeyLength = (short) (storeKey.length - 1);
		    short storeKeyIndex = (short) (downloadedBytes % (storeKeyLength + 1));
			
//		    final byte[] key = Crypt.GenerateRekey();
//		    final short keyLength = (short) (key.length - 2);
//		    short keyIndex = (short) (downloadedBytes % (keyLength + 1));

		    // mychange: commented to support v2 API
			byte[] buffer = new byte[BUFFER_SIZE];
			int readed = -1;		
			while(isRunning && (readed = is.read(buffer)) != -1)
			{
//				for(int i = 0; i < readed; i++)
//		    	{
//					buffer[i] = (byte) (buffer[i] ^ transportKey[transportKeyIndex] ^ storeKey[storeKeyIndex]);
//		    		if(transportKeyIndex < transportKeyLength)
//		    			transportKeyIndex++;
//			        else
//			        	transportKeyIndex = 0;
//		    		if(storeKeyIndex < storeKeyLength)
//		    			storeKeyIndex++;
//		    		else
//		    			storeKeyIndex = 0;
////					buffer[i] = (byte) (buffer[i] ^ key[keyIndex]);
////					if(keyIndex < keyLength)
////						keyIndex++;
////		    		else
////		    			keyIndex = 0;
//		    	}

				try
				{
					channel.write(ByteBuffer.wrap(buffer, 0, readed));
				}
				catch (IOException e1)
				{
				//onError(Errors.FILE_CHANEL_WRITE_ERROR);
					SaveDataToDB();
					Close(file, channel, null, is);
					percent = -1;
					return;
				}
				downloadedBytes += readed;
				percent = (int) (downloadedBytes / size);
				//Показываем прогресс с шагом 1%
				if(track.audioDownloadProcentage != percent)
				{
					if(needFastSave)
						UpdateProcentageInDB(percent);

					track.audioDownloadProcentage = percent;
					onProgressChanged(percent);
				}
				
				if(isRunning && needIntermediateSave && downloadedBytes > meredian)
				{
					SaveDataToDB();
					needIntermediateSave = false;
				}
			}
		}
		catch (IOException e)
		{
			//Internet connection error!
			e.printStackTrace();
		//onError(Errors.INTERNET_CANT_READ);
			SaveDataToDB();
			Close(file, channel, null, is);
			percent = -1;
			return;
		}
		
		if(track.audioDownloadProcentage > 100 
				|| downloadedBytes == track.file.size 
				|| (track.audioDownloadProcentage == 99 && percent == 99))
			track.audioDownloadProcentage = 100;
		
		if(track.audioDownloadProcentage == 100)
			try
			{
				is.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

		if(track.audioDownloadProcentage == 100 && track.file.length <= 0)
			track.file.length = CalculateDuration();
			
		SaveDataToDB();
		Close(file, channel, null, is);
		if ( stopCount == 0 )
			onStop(track.audioDownloadProcentage == 100);
	}

	public int CalculateDuration()
	{Log.d("MyTrace", "AudioLoad: " + MyStackTrace.func3());
		int result = 0;
		AudioServer server = null;
		MediaPlayer mediaPlayer = null;
		try
		{
			server = new AudioServer();
			server.init();
			server.start();
			mediaPlayer = new MediaPlayer();
			String playUrl = "http://127.0.0.1:" + server.getPort()	+ "/file://" 
					+ FileManager.PathToAudioFile(bookId, trackNumber);
			mediaPlayer.setDataSource(playUrl);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
	        mediaPlayer.prepare();
	        result = mediaPlayer.getDuration() / 1000;
		}
		catch (IllegalArgumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalStateException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if(server != null)
				server.stop();
			if(mediaPlayer != null)
				mediaPlayer.release();
		}
        
		return result;
	}
}
