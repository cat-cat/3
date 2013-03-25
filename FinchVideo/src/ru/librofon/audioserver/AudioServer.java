package ru.librofon.audioserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.StringTokenizer;


import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicStatusLine;

//import ru.librofon.MyApp;


import android.media.MediaPlayer;

/**
 * Сервер который расшифровывает и передаёт аудиопоток.
 * Класс <b>НЕЛЬЗЯ</b> использовать как реализацию {@link Runnable}.
 * 
 * </p>Пример использования:<br/>
 * <b>Для создания необходимо использовать следующий код</b><br/>
 * <code>
 * AudioServer server = new AudioServer(); <br/>
 * server.init(); <br/>
 * server.start(); <br/>
 * </code> <br/>
 * 
 * <b>Завершения работы сервера:</b><br/>
 * <code>
 * if(server.isRunning)<br/>
 *      server.stop(); <br/>
 * </code> <br/>
 * 
 * <b>Для того чтобы воспроизвести какой-либо трек, вначале необходимо сформировать GET запрос 
 * к серверу. Запрос формируется следующим образом:</b><br/>
 * 1 - URL для воспроизведения файла, который хранится локально<br/>
 * <code>
 * String trackUrl = AudioServer.SERVER_ADDRESS + server.getPort() + "/" + "file://" + 
 * FileManager.PathToAudioFile(идентификационный_номер_книги, идентификационный_номер_трека);
 * </code><br/> 
 * 2 - URL для воспроизведения файла, который хранится на сервере<br/>
 * <code>
 * CommandBuilder cb = new CommandBuilder();<br/>
 * cb.AddCommand(Commands.getAbookAudioByTrack);<br/>
 * cb.addParam(String.valueOf(идентификационный_номер_книги));<br/>
 * cb.addParam(String.valueOf(идентификационный_номер_трека));<br/>
 * String trackUrl = AudioServer.SERVER_ADDRESS + server.getPort() + "/" + cb.GetCommand();<br/>
 * </code><br/>
 * 
 * <b>Для воспроизведения аудио ранее созданный URL передаётся в MediaPlayer</b><br/>
 * <code>
 * mediaPlayer.setDataSource(trackUrl);<br/>
 * </code><br/>
 * 
 * <b>Если {@link #IsSeekableStream()} возвращает true, то поддерживается перемещение по потоку.
 * Для перемещения используется стандартный метод {@link MediaPlayer#seekTo}</b><br/>
 * <code>
 * mediaPlayer.seekTo(233334);
 * </code>
 */
public class AudioServer implements Runnable
{

	public static final String TAG = "AudioServer";
	
	public static final String SERVER_ADDRESS = "http://127.0.0.1:";
	public static final String SERVER_PLAY_FROM_FILE = "file://";
	
	private int port = 0;
	private boolean isRunning = false;
	private ServerSocket socket;
	private Thread thread;
	
//	private byte encryptionType;  
	
//	synchronized void SetEncryptionType(byte newType)
//	{
//		encryptionType = newType;
//	}
//	
//	synchronized byte GetEncryptionType()
//	{
//		return encryptionType;
//	}
	/**
	 * Можно ли перемещаться по потоку.
	 */
	private boolean seekable = true;
	
	private boolean parseUrlToRange = false;

	
	static final byte[] Mp3Header = {
			'T','A','G', 
			't','i','t','l','e','t','i','t','l','e','t','i','t','l','e','t','i','t','l','e','t','i','t','l','e','t','i','t','l','e',
			'a','r','t','i','s','t','a','r','t','i','s','t','a','r','t','i','s','t','a','r','t','i','s','t','a','r','t','i','s','t',
			'a','l','b','u','m','a','l','b','u','m','a','l','b','u','m','a','l','b','u','m','a','l','b','u','m','a','l','b','u','m',
			'2','0','1','1',
			0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
			1,
			0,
			(byte)255
			};
	
	/**
	 * Проверяет возможность перемещения по потоку.
	 * 
	 * По потоку можно перемещаться если идёт воспроизведения локально файла,
	 * при воспроизведении с сервера напрямую перемещение не поддерживается. 
	 * 
	 * @return true если можно переходить по потоку,
	 * false - если такой возможности нет.
	 */
	public boolean IsSeekableStream()
	{
		 return seekable;
	}

	/**
	* Возвращает порт который прослушивает сервер. Адрес сервера всегда localhost (127.0.0.1). 
	* @return номер порта.
	*/
	public int getPort()
	{
		return port;
	}


	/**
	 * Подготавливает сервер к запуску.
	 * 
	 * Необходимо вызывать только один раз для каждого экземпляра сервера.
	 * После подготовки сервер может быть запущен или остановлен в любое время.
	 */
	public void init()
	{
//		encryptionType = 0;
		// mychanges
		//parseUrlToRange = MyApp.UseOpenCoreMode;
		parseUrlToRange = true;
		try
		{
			socket = new ServerSocket(port, 0, InetAddress.getByAddress(new byte[] {127, 0, 0, 1 }));
			//socket.setSoTimeout(5000);
			port = socket.getLocalPort();
//			Log.d(TAG, "Server stated at " + socket.getInetAddress().getHostAddress() + ":" + port);
		}
		catch (UnknownHostException e)
		{
//			Log.e(TAG, "Error initializing server", e);
//			e.printStackTrace();
		}
		catch (IOException e)
		{
//			Log.e(TAG, "Error initializing server", e);
//			e.printStackTrace();
		}
	}
	
	/**
	 * Запуск сервера.
	 * Сервер запускается в отдельном потоке.
	 */
	public void start()
	{
		isRunning = true;
		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Остановка работы сервера.
	 * 
	 * Чтобы остановить сервером прослушку порта, может понадобится около 5 секунд,
	 * поэтому вызов метода блокируется на это время.
	 */
	public void stop()
	{
		isRunning = false;
		if (thread == null)
		{
//			Log.w(TAG, "Server was stopped without being started.");
			return;
		}
//		Log.d(TAG, "Stopping server.");
		thread.interrupt();
		try
		{
			thread.join(5000);
		}
		catch (InterruptedException e)
		{
//			Log.w(TAG, "Server was interrupted while stopping", e);
//			e.printStackTrace();
		}
	}

	public boolean isRunning()
	{
		return isRunning;
	}


	/**
	 * Используется внутри класса, запуск осуществляется методом {@link AudioServer#start()}.
	 * 
	 * Класс <b>НЕЛЬЗЯ</b> использовать как реализацию {@link Runnable}.
	 */
	@Override
	public void run()
	{
//		Log.d(TAG, "running");
		while (isRunning)
		{
			try
			{
				Socket client = socket.accept();
				if (client == null)
					continue;
				DataSource data = GetDataSource(ParseRequest(client));
				processRequest(data, client);
			}
			catch (SocketTimeoutException e)
			{
				// Do nothing
//				e.printStackTrace();
//				Log.d(TAG, "run - SocketTimeoutException");
			}
			catch (IOException e)
			{
//				Log.e(TAG, "Error connecting to client", e);
//				e.printStackTrace();
			}
		}
//		Log.d(TAG, "Server interrupted or stopped. Shutting down.");
	}


	/**
	 * Формирует нужный {@link DataSource} для трека и возвращает его.
	 * 
	 * @param request Путь к аудиокниге (файл или интернет)
	 * @return {@link DataSource} который возвращает метаданные и поток.
	 */
	private DataSource GetDataSource(String[] request)
	{
		DataSource ds = null;
		if(request != null)
		{
			// Удаляет первое '/' из адреса трека
			String path = request[0].substring(1);
			long rangeFrom = 0;
			long rangeTo = 0;
			long size = 0;
			if(request[1] != null)
				rangeFrom = Long.parseLong(request[1]);
			if(request[2] != null)
				rangeTo = Long.parseLong(request[2]);
			if(request[3] != null)
				try
				{
					size = Long.parseLong(request[3]);
				}
				catch(java.lang.NumberFormatException e)
				{
					e.printStackTrace();
					size = Long.parseLong(request[3].substring(0, request[3].length() - 1));
				}
		
			if(path.substring(0, 4).equalsIgnoreCase("file"))
			{
				seekable = true;
				ds = new LocalSource(path, rangeFrom, rangeTo, size);
			}
//			else
//			{
//				seekable = false;
//				ds = new RemoteSource(path, range);
//			}
		}

		return ds;
	}

	/**
	 * Читает HTTP GET запрос клиента и разбирает его на используемые составляющие:
	 * адрес файла и номер байта с которого будет передаваться файл.
	 * 
	 * @return - Массив String состоящий из двух элементов:
	 *      String[1] - адрес трека;
	 *      String[2] - (rangeFrom)номер байта с которого необходимо начинать передачу файла.
	 *           может быть null, если в запросе отсутствует.</br>
	 *      String[3] - (rangeTo)номер байта до которого необходимо передавать данные
	 *           может быть null, если в запросе отсутствует.</br>
	 *      String[4] - (size) предопределённый размер данных (когда файл ещё не скачался полностью)
	 *           может быть null, если в запросе отсутствует.</br>
	 *  - Если произошла ошибка то будет возвращено null вместо массива.
	 */
	private String[] ParseRequest(Socket client)
	{
		String firstLine = null;
		String rangeFrom = null;
		String rangeTo = null;
		try
		{
			InputStream is = client.getInputStream();
			// Буфер в 8к это слишком много - черевато боком.
			// Так как запрос врядли будет слишком большим, то на него хватит и 1к.
			BufferedReader reader = new BufferedReader(new InputStreamReader(is), 1024);
			firstLine = reader.readLine();
			if(firstLine == null)
				return null;
			
			String line = null;
			while(true)
			{
				line = reader.readLine();
				if(line == null || line.length()==0)
					break;
				
//				Log.d(TAG, line);
				
				if("Range".equalsIgnoreCase(line.substring(0, 5)))
				{
					int index = line.indexOf('-');
					if(index > -1)
					{
						rangeFrom = line.substring(13, index);
						if( index + 2 < line.length())
							rangeTo = line.substring(index+1);
					}
					break;
				}
				
//				if(line.startsWith("User-Agent"))
//				{
//					if(line.indexOf("OpenCORE") != -1)
//					{
//						parseUrlToRange = true;
//						Log.w("AudioServer", "- Need use Crutches :-(" );
//					}
//				}
			}
		}
		catch (IOException e)
		{
//			Log.e(TAG, "Error parsing request from client", e);
			return null;
		}
		
		try
		{
			StringTokenizer st = new StringTokenizer(firstLine);
			st.nextToken(); // Skip method
			String tmp = st.nextToken();
			String size = null;
			int indexSize = tmp.indexOf("?size=");
			if(indexSize != -1)
			{
				size = tmp.substring(indexSize + 6);
				int index = size.indexOf('?');
				if(index != -1)
					size = size.substring(0, index);
			}

//			if(Build.VERSION.SDK_INT < 9 && parseUrlToRange)
			if(parseUrlToRange)
			{
				int indexRange = tmp.indexOf("?range=");
				if(indexRange != -1)
				{
					useCrutches = true;
					rangeFrom = tmp.substring(indexRange + 7);
				}
			}

			int indexFilePath = tmp.indexOf('?');
			if(indexFilePath == -1)
				indexFilePath = tmp.length();
			
			//return new String[] { URLDecoder.decode(tmp.substring(0, indexFilePath), "x-www-form-urlencoded"), rangeFrom, rangeTo, size};
			return new String[] { URLDecoder.decode(tmp.substring(0, indexFilePath), "UTF-8"), rangeFrom, rangeTo, size};
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}
	
	boolean useCrutches = false;
	
	/**
	 * Возврашает ответ клиенту по протоколу HTTP,
	 * включая заголовки (если получится) и контент.
	 */
	private void processRequest(DataSource dataSource, Socket client)
		throws IllegalStateException, IOException
	{
//		final byte cryptType = encryptionType;
		if (dataSource == null)
		{
//			Log.e(TAG, "Invalid (null) resource.");
			if(client != null)
				client.close();
			return;
		}
		
		//Создание заголовка ответа httpString.
		StringBuilder httpString = new StringBuilder();
		long rangeFrom = dataSource.RangeFrom();
		if(rangeFrom <= 0 || parseUrlToRange)
		{
			if(rangeFrom < 0)
				rangeFrom = 0;
			//Заголовок ответа, когда передача трека осуществляется сначала
			httpString.append(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 0)
				, HttpStatus.SC_OK, "OK"));
			httpString.append("\r\n");
			
			httpString.append("Content-Type: ").append(dataSource.GetContentType());
			httpString.append("\r\n");
			
			httpString.append("Content-Length: ").append(dataSource.GetContentLength());
			httpString.append("\r\n");
			
			httpString.append("Accept-Ranges: bytes");
			httpString.append("\r\n");
			
			httpString.append("Connection: close");
			httpString.append("\r\n");
		}
		else
		{
			//Заголовок ответа, когда передача осуществляется с какого-то момента
			httpString.append(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 0)
				, HttpStatus.SC_PARTIAL_CONTENT, "Partial Content"));
			httpString.append("\r\n");
			
			httpString.append("Accept-Ranges: bytes");
			httpString.append("\r\n");
			
			long rangeTo = dataSource.RangeTo();
			long length = dataSource.GetLength();
			
			httpString.append("Content-Range: bytes ")
				.append(dataSource.RangeFrom())
				.append("-");
			if(rangeTo > 0)
				httpString.append(rangeTo);
			else
				httpString.append(length - 1);
			
			httpString.append("/")
				.append(length);
			httpString.append("\r\n");

			httpString.append("Content-Length: ").append(dataSource.GetContentLength());
			httpString.append("\r\n");

			httpString.append("Connection: close");
			httpString.append("\r\n");
			
			httpString.append("Content-Type: ").append(dataSource.GetContentType());
			httpString.append("\r\n");
		}

		httpString.append("\r\n");
		
//		Log.i(TAG, httpString.toString());
		
		OutputStream clientStream = client.getOutputStream();
		ReadableByteChannel data = null;
		try
		{
			byte[] buffer = httpString.toString().getBytes();
			clientStream.write(buffer, 0, buffer.length);

			data = dataSource.CreateReadChannel();
			if(data == null)
				return;
			
			byte[] buff;
			ByteBuffer bbuffer = ByteBuffer.allocate(1024 * 50);
	
			// mychanges use of keys
//			final byte[] key = Crypt.GenerateKey(cryptType);
//		    final short keyLength = (short) (key.length - ((cryptType == 0) ? 2 : 1));
			byte myKey = 1;
			final byte[] key = Crypt.GenerateKey(myKey);
		    final short keyLength = (short) (key.length - ((myKey == 0) ? 2 : 1));
		    short keyIndex = (short) (rangeFrom % (keyLength + 1));
		    
		    long contentLength = dataSource.GetContentLength();
			long contentReaded = 0;
			int readBytes = 0;
		    final int WAITING_TIME = 20; //60
		    int wait = WAITING_TIME;
			while (isRunning )
			{
				if(useCrutches)
				{
					clientStream.write(Mp3Header, 0, Mp3Header.length);
					useCrutches = false;
				}

				readBytes = data.read(bbuffer);
				if (readBytes < 1)
				{
					if(contentReaded < contentLength && wait > 0)
					{
						wait--;
						Thread.sleep(1000);
						continue;
					}
					break;
				}
				wait = WAITING_TIME;
				
				contentReaded += readBytes;
				buff = bbuffer.array();
				
				//TODO Перенести в нормальный класс.
		    	for(int i = 0; i < readBytes; i++)
		    	{
		    		buff[i] = (byte) (buff[i] ^ key[keyIndex]);
		    		if(keyIndex < keyLength)
			        	keyIndex++;
			        else
			        	keyIndex = 0;
		    	}
		    	clientStream.write(buff, 0, readBytes);
				
				bbuffer.rewind();
			}
		}
		catch (SocketException e)
		{
			// Игнорирование разрыва соединения клиентом.
//			Log.w(TAG, "Ignoring " + e.getMessage());
//			e.printStackTrace();
		}
		catch (IOException e)
		{
//			Log.e(TAG, "Error getting content stream.", e);
//			e.printStackTrace();
		}
		catch (InterruptedException e)
		{
//			Log.e(TAG, "Error in sleep thread.", e);
//			e.printStackTrace();
		}
		catch (Exception e)
		{
//			Log.e(TAG, "Error streaming file content.", e);
//			e.printStackTrace();
		}
		finally
		{
			if (data != null)
				data.close();
			if(client != null)
				client.close();
			
//			Log.e(TAG, "Server finally worked");
		}
	}
	
	
}
