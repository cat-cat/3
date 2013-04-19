package dataProvider.internetProvider.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

//import oauth.signpost.http.HttpRequest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.xml.sax.SAXException;

import android.os.Build;
import android.util.Log;

//import ru.old.Debug;
//import ru.old.MyApp;
import ru.old.AudioFile;
//import twitter4j.internal.http.HttpResponseEvent;

import dataProvider.internetProvider.handlers.ConnectHandler;

/**
 * Класс, ответственный за подключение к серверу
 * и предоставление необохдимых ресурсов.
 * @author mikalaj
 *
 */
public class SourceProvider {
	private static final String deviceId = "a9f094672e47283b6fc7af77ddef829b";
	private static final String Tag = "SourceProvider";
	public static final String HEADER_DEVICE = "deviceId";
	public static final String HEADER_USER_AGENT = "User-Agent"; 
	public static final String USER_AGENT = "Android/" + Build.VERSION.RELEASE;
	/**
	 * Входной поток, который будет возвращён пользователю.
	 */
	private InputStream in;
	/**
	 * Класс для соединения.
	 */
	HttpURLConnection connection;
	/**
	 * Ключ сессии.
	 */
	private static String _sessid;
	
	private static CookieStore cookies;
	
	/**
	 * метод для установки ключа сессии.
	 * @param sessid
	 */
	public static void SetSessid(String sessid){
		_sessid = sessid;
	}
	/**
	 * метод, который предоставляет ресурс по урл
	 * и добавляет необходимые параметры в куки.
	 * @param server строка подключения.
	 * @return Входной поток InputStream
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public InputStream provideSource(String server) throws IOException {
		HttpGet httpRequest = null;
		try {
			httpRequest = new HttpGet( new URL(server).toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		httpRequest.addHeader(HEADER_DEVICE, deviceId);
		httpRequest.addHeader(HEADER_USER_AGENT, USER_AGENT);
		
		AbstractHttpClient httpclient = new DefaultHttpClient();
		
        httpclient.setCookieStore(SourceProvider.GetSessionCookies());
        
		HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
		HttpEntity entity = response.getEntity();
		in = entity.getContent();
		if(in==null)
			Log.i(Tag, "Returning null inStream");
		return in;
	}
	
	public InputStream provideAudio(String server, AudioFile file) throws IOException 
	{
//		HttpGet httpRequest = null;
//		try 
//		{
//			httpRequest = new HttpGet( new URL(server).toURI());
//		} 
//		catch (URISyntaxException e) 
//		{
//			e.printStackTrace();
//		}
//		httpRequest.addHeader(HEADER_DEVICE, MyApp.md5DeviceId);
//		httpRequest.addHeader(HEADER_USER_AGENT, USER_AGENT);
//		
//		AbstractHttpClient httpclient = new DefaultHttpClient();
//        httpclient.setCookieStore(SourceProvider.GetSessionCookies());
//        
//		HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
		
		int attempt = 3;
		HttpResponse response = null;
		do
		{
			response = GetResponse(server);
			attempt--;
			if ( response == null )
				try { Thread.sleep(500); } catch (InterruptedException e) {}
		} while ( attempt > 0 && response == null );

		if ( response != null )
		{
			Header h = response.getFirstHeader("Librofon-Errcode"); 
			if ( h != null )
			{
				try
				{
					file.errorCode = Integer.parseInt(h.getValue());
				}
				catch (NumberFormatException e)
				{
					file.errorCode = ConnectionErrorCodes.WRONG_SESSION;
				}
			}
			
			if ( file.errorCode == ConnectionErrorCodes.WRONG_SESSION )
				Reconnect();
			
			h = response.getFirstHeader("Content-Duration");
			if ( h != null )
				file.SetDuration(h.getValue());
			h = response.getFirstHeader("Date");
			if ( h != null )
				file.SetDate(h.getValue());
			h = response.getFirstHeader("Expires");
			if ( h != null )
				file.SetExpires(h.getValue());
			
			HttpEntity entity = response.getEntity();
			in = entity.getContent();
			return in;
		}
		return null;
	}
	
	private HttpResponse GetResponse(String url)
	{
		HttpResponse response = null;
		
		HttpGet httpRequest = null;
		try 
		{
			httpRequest = new HttpGet( new URL(url).toURI());
		} 
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch (URISyntaxException e) 
		{
			e.printStackTrace();
		}
		
		if ( httpRequest != null )
		{
			httpRequest.addHeader(HEADER_DEVICE, deviceId);
			httpRequest.addHeader(HEADER_USER_AGENT, USER_AGENT);
			
			AbstractHttpClient httpclient = new DefaultHttpClient();
	        httpclient.setCookieStore(SourceProvider.GetSessionCookies());
	        
			try
			{
				response = (HttpResponse) httpclient.execute(httpRequest);
				Header h = response.getFirstHeader("Librofon-Errcode");
				if ( h != null )
				{
					try
					{
						if ( Integer.parseInt(h.getValue()) == ConnectionErrorCodes.WRONG_SESSION )
							response = null;
					}
					catch (NumberFormatException e)
					{
						response = null;
					}
				}
			}
			catch (ClientProtocolException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return response;
	}

	/**
	 * Метод, для отсоединения от сервера.
	 */
	public void disconnect(){
		try {
			in.close();
		} catch (IOException e) {
//			Log.e(Tag,"IOerror.");
		}
	    connection.disconnect(); 	
	}
	
	/**
	 * Получение потока содержащего обложку для книги.
	 * @param server адрес по которому осуществляется доступ к обложке.
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public InputStream provideSourceImage(String server) throws IllegalStateException, IOException {
		InputStream instream = null;
		try
	    {
		    HttpGet	httpRequest = new HttpGet( new URL(server).toURI());
		    httpRequest.addHeader(HEADER_DEVICE, deviceId);
		    httpRequest.addHeader(HEADER_USER_AGENT, USER_AGENT);
	        AbstractHttpClient httpclient = new DefaultHttpClient();
	        httpclient.setCookieStore(SourceProvider.GetSessionCookies());
	        
	        HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
	        HttpEntity entity = response.getEntity();
	        BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity); 
	        instream = bufHttpEntity.getContent();  
	    }
	    catch (URISyntaxException e)
	    {
	    	//TODO Remove
	    	e.printStackTrace();
	    }
	    catch (MalformedURLException e)
	    {
	    	//TODO Remove
	    	e.printStackTrace();
	    }
		return instream;
	}
	
	public InputStream provideSourceVerifyPurchaseTest(String signature, String signedData, String bookId) 
			throws IllegalStateException, IOException
	{
		
		HttpPost request = null;
//		if(Debug.TEST_MODE)
//			request = new HttpPost("http://www.librofon.ru/service/orderAndroidTest");
//			//request = new HttpPost("http://test.librofon.ru/service/orderAndroidTest");
//		else
			request = new HttpPost("http://www.librofon.ru/service/orderAndroid");
		
		request.addHeader(HEADER_DEVICE, deviceId);
		request.addHeader(HEADER_USER_AGENT, USER_AGENT);
		ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new BasicNameValuePair("params[]", signature));
		parameters.add(new BasicNameValuePair("params[]", signedData));
//		if(Debug.TEST_MODE)
//			parameters.add(new BasicNameValuePair("params[]", bookId));
        UrlEncodedFormEntity formEntity = null;
		try
		{
			formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		} 
        request.setEntity(formEntity);        
		AbstractHttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        client.getParams().setParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, Boolean.FALSE);
        client.setCookieStore(SourceProvider.GetSessionCookies());
        
        HttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        if(entity == null)
        	return null;
		return entity.getContent();
	}
	
	synchronized public static void ReconnectWithAttempt(int attempt)
	{
		
	}
	
	synchronized public static void Reconnect()
	{
		try
		{
			CommandBuilder cb = new CommandBuilder();
			cb.AddCommand(Commands.Connect);
			if(deviceId != null)
				cb.addParam(deviceId);
			HttpGet httpRequest = null;
			try
			{
				httpRequest = new HttpGet( new URL(cb.GetCommand()).toURI());
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
			}
			httpRequest.addHeader(HEADER_DEVICE, deviceId);
			httpRequest.addHeader(HEADER_USER_AGENT, USER_AGENT);
			AbstractHttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
			HttpEntity entity = response.getEntity();
			InputStream in = entity.getContent();

			ConnectHandler handler = new ConnectHandler();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = null;
			try {
				parser = factory.newSAXParser();
				parser.parse(in, handler);
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}		
			SetSessid(handler.getSessid());
			cookies = httpclient.getCookieStore();
			List<Cookie> list = cookies.getCookies();
			if(list == null || list.isEmpty() || !list.get(0).getName().equalsIgnoreCase("sessid"))
			{
				BasicClientCookie c = new BasicClientCookie("sessid", _sessid);
				c.setDomain("librofon.ru");
				c.setPath("/");
				c.setAttribute("path", "/");
				cookies = new BasicCookieStore();
				cookies.addCookie(c);	
			}
			
			BasicClientCookie c = new BasicClientCookie("dev", "time");
			c.setDomain("dev.time");
			c.setPath("/");
			c.setAttribute("path", "/");
			c.setExpiryDate(new Date(System.currentTimeMillis() + 1200000));
			cookies.addCookie(c);
		}
		catch(IOException e)
		{
			//e.printStackTrace();
		}
	}

	synchronized public static CookieStore GetSessionCookies()
	{
		if(cookies != null)
		{
//			if(cookies.clearExpired(new Date()) && cookies.getCookies().size() < 1)
			if(cookies.clearExpired(new Date()))
				Reconnect();
		}
		else
			Reconnect();
		return cookies;
	}
	
	/**
	 * Проверяется доступность интернет, путём 'дёрганья' google.ru.
	 * @return true если интернет доступен.
	 */
	public static boolean IsNetAvailable()
	{
		boolean result = TryConnect("http://google.ru");
		if(!result)
			result = TryConnect("http://ya.ru");
		return result;
	}
	
	private static boolean TryConnect(String site)
	{
		HttpURLConnection connection = null;
        try
        {
        	URL url = new URL(site);
        	connection = (HttpURLConnection) url.openConnection();
        	connection.setConnectTimeout(1000 * 5);
        	connection.connect();
            if(connection.getResponseCode() == 200)
            	return true;
        }
        catch (MalformedURLException e1)
        {
            ;
        }
        catch (IOException e)
        {
            ;
        }
        finally
        {
        	if(connection != null)
        		connection.disconnect();
        }
        return false;
	}
}