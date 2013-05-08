package com.audiobook;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.audiobook.NetworkConnectivityListener.State;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import dataProvider.dbProvider.fileManager.FileManager;
import dataProvider.internetProvider.helpers.ConnectionErrorCodes;
import dataProvider.internetProvider.helpers.SourceProvider;

import ru.old.Errors;

import java.util.ArrayList;

import junit.framework.Assert;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;


public class gs extends Handler {
	static public SQLiteDatabase db=null;
	public static boolean shouldShowPlayerButton = false;
	XPathFactory factory = XPathFactory.newInstance();
	XPath xPath = factory.newXPath();

	//public static final String testProduct = "001.trash";
	//public static final String testProduct = "android.test.purchased";

	public static final String pk = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoDISZUCTLy5BM4YW9p4gAkS+4FH24zB2ecBWGQ4VQN9OLgc//lH5/evqXkyKkQl5PWDrY0jWpRSf4hxlsmbEl5qpOZ6hev7Wi4SitK6paShnFSe8GEpJ5GmYlU04I66CJW8q4eqKtqupCuXWfV01DKVgrSlGrQfjcVs5Z4SRkfbxEOFmOgkSKdtlrdvSBkavfvvkFC9KM7RTRx56WWAkm7JyV0w2xzBcNGNXQ8IXamYi+08QaJPnYYClEITStfWQRPdMQHHTEF1kfb2YaZ/UBQNqSY0ltBloERwV0d1m4/0siTW8EW77ogncBIghYWmi4bWtaLuL1QuQWJz8DfRXuwIDAQAB";

	// The Android's default system path of your application database.
	private final String basePath = "/Android/data/com.audiobook/";
	private Context ctx;
	private String DB_NAME = "books.db";

	private static gs   _instance;

	private String android_id ;

	private boolean db_PerformUpdate(String sql)
	{

		String[] array = sql.split(";");
		if(array.length == 0 || array[0].isEmpty())
			return false;

		try {
			db.beginTransaction();

			// loop
			for(String stmt : array)
			{
				String query = stmt;

				db.execSQL(query);
			}
			// end loop

			db.setTransactionSuccessful();
		} finally {
			gs.db.endTransaction();
		}

		return true;
	}

	public void db_setBuyBook(String bid, int bought)
	{
		String sql = String.format("UPDATE t_abooks"
				+" SET bought=%d WHERE abook_id='%s'", bought, bid);
				
		db.execSQL(sql);
	}
	
	private String db_GetLastUpdate()
	{
		String sql = "SELECT id, id _id"
				+" FROM [updates]"
				+ " ORDER BY id DESC"
				+ " LIMIT 0,1";

		Cursor c = null;
		try {
			c = db.rawQuery(sql, null);
		} catch (IllegalStateException e) {
			// TODO: handle exception
			e.printStackTrace();
			return "10000";
		}

		int idxid = c.getColumnIndex("id");
		String id = "";
		if (c.moveToFirst()) {
			do { 
				id = c.getString(idxid);
			}
			while (c.moveToNext());
		}
		c.close();
		return id;
	}

	private boolean updateCatalog()
	{		
		if(!new File(dbp()).exists())
			return false;

		//synchronized(this) {

		String updateid = db_GetLastUpdate();
		String url = String.format("http://%s/update.php?dev=%s&updateid=%s",gs.s().Host(),android_id, updateid);
		// DOWNLOAD THE PROJECT JSON FILE

		HttpClient trackhttpclient = new DefaultHttpClient();

		try {

			HttpGet httpget = new HttpGet(url);

			// Create a response handler

			ResponseHandler<String> responseHandler = new BasicResponseHandler();

			String responseBody = trackhttpclient.execute(httpget, responseHandler);

			try {
				gs.s().handleSrvError(responseBody);
				ArrayList<String> nl = gs.s().getNodeList("//sql", responseBody);
				if(nl.size()>0)
				{
					String sql = nl.get(0);
					db_PerformUpdate(sql);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally
			{

			}



			ArrayList<String> ar = null;
			try {
				ar = gs.s().getNodeList("//sql/@newbooks", responseBody);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// TODO: if connection bad ar will be null
			if(ar==null)
				return false;
			
			if (ar.size()>0) {
				final int nc = Integer.parseInt( ar.get(0));

				if (nc > 0) {
					
					new AsyncTask<Void,Void,Void>()
					{

						@Override
						protected Void doInBackground(Void... arg0) {
							// TODO Auto-generated method stub
							return null;
						}
						
						@Override
						protected void onPostExecute(Void args)
						{
							m(String.format("Обновление! Получено новых книг: %d", nc));							
						}
						
					}.execute();
					 
				}
			}
		} catch (ClientProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}finally{}

		return true;
	}

	private void m(String msg)
	{
		// update the UI
		// message box
		Toast.makeText(ctx,
				msg,
				Toast.LENGTH_SHORT)
				.show();
	}

	public boolean connected()
	{
		return nlistener.getState()==NetworkConnectivityListener.State.CONNECTED;
	}
	
	public void handleMessage(Message msg) 
	{
		if(nlistener.getState()==NetworkConnectivityListener.State.CONNECTED)
		{
			Log.i("MyTrace:", "++ update catalog is called");

			new AsyncTask<Void,Void,Void>()
			{

				@Override
				protected Void doInBackground(Void... arg0) {
					updateCatalog();
					return null;
				}
				
			}.execute();
		}
	}

	static int metaLengthReturnValue = 0;
	static String metaLengthPrevBid = "";
	static String metaLengthPrevChid = "";
	public int metaLengthForChapter(String bid, String chid) {
		if (!bid.equalsIgnoreCase(metaLengthPrevBid)
				|| !chid.equalsIgnoreCase(metaLengthPrevChid)) { // ratake
			// metasize from
			// xml for new
			// chapter
			metaLengthPrevBid = bid;
			metaLengthPrevChid = chid;
		} else {
			return metaLengthReturnValue;
		}

		String xml = fileToString(
				dirsForBook(bid) + "/bookMeta.xml");
		ArrayList<String> as = null;
		try {
			as = gs.s()
					.getNodeList(
							String.format(
									"//abook[@id='%s']/content/track[@number='%s']/file/length",
									bid, chid), xml);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (as.size() != 1) {
			Log.e("**err:", String.format(
					"**err: invalid meta size for book: %s, chpater: %s", bid,
					chid));
		} else
			metaLengthReturnValue = Integer.parseInt(as.get(0));

		return metaLengthReturnValue;
	}

	static int metaSizeReturnValue = 0;
	static String metaSizePrevBid = "";
	static String metaSizePrevChid = "";
	public int metaSizeForChapter(String bid, String chid) {
		if (!bid.equalsIgnoreCase(metaSizePrevBid)
				|| !chid.equalsIgnoreCase(metaSizePrevChid)) { // ratake
			// metasize from
			// xml for new
			// chapter
			metaSizePrevBid = bid;
			metaSizePrevChid = chid;
		} else {
			return metaSizeReturnValue;
		}

		String xml = fileToString(
				dirsForBook(bid) + "/bookMeta.xml");
		ArrayList<String> as = null;
		try {
			as = gs.s()
					.getNodeList(
							String.format(
									"//abook[@id='%s']/content/track[@number='%s']/file/size",
									bid, chid), xml);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (as.size() != 1) {
			Log.e("**err:", String.format(
					"**err: invalid meta size for book: %s, chpater: %s", bid,
					chid));
		} else
			metaSizeReturnValue = Integer.parseInt(as.get(0));

		return metaSizeReturnValue;
	}

	public int actualSizeForChapter(String bid, String chid) {
		int returnValue = 0;
		String pathToChapterAudio = pathForBookAndChapter(bid, chid);
		File file = new File(pathToChapterAudio);
		returnValue = (int) file.length();

		return returnValue;
	}

	public String fileToString(String path)
	{
		//Get the text file
		File file = new File(path);

		//Read text from file
		StringBuilder text = new StringBuilder();

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				text.append(line);
			}
			br.close();
		}
		catch (IOException e) {
			//You'll need to add proper error handling here
		}

		return text.toString();
	}

	public boolean createFileAtPath(String path, String contents){
		boolean success = true;
		try {
			File f = new File(path);		
			FileWriter fw = new FileWriter(f);
			fw.write(contents);
			fw.close();
		} 
		catch (FileNotFoundException e) {
			success = false;
			e.printStackTrace();
		}
		catch (IOException e) {
			success = false;
			e.printStackTrace();
		}
		return success;
	}

	public String deviceId()
	{
		return android_id;
	}
	
	public void setDeviceId(String id)
	{
		android_id = id;
	}

	public String responseString(HttpResponse response) {
		String content = null;
		try {
			HttpEntity entity = response.getEntity();

			//
			// Read the contents of an entity and return it as a String.
			//
			content = EntityUtils.toString(entity);
			//System.out.println(content);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return content;
	}

	public HttpResponse srvResponse(String url)
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
			//			httpRequest.addHeader(HEADER_DEVICE, deviceId);
			//			httpRequest.addHeader(HEADER_USER_AGENT, USER_AGENT);

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

	public String pathForBookFinished(String bid, String chid)
	{
		String newDirPath = dirsForBook(bid);
		String path = newDirPath + String.format("/ca/%sfinished!", chid);
		return path;		
	}

	public String pathForBuy(String bid)
	{
		String newDirPath = dirsForBook(bid);
		String path = newDirPath + "/buy";
		return path;		
	}

	public String pathForBookAndChapter(String bid, String chid)
	{
		String newDirPath = dirsForBook(bid);
		String path = newDirPath + "/ca/"+chid+".mp3";
		return path;		
	}

	public String pathForBookMeta(String bid)
	{
		String newDirPath = dirsForBook(bid);
		String path = newDirPath + "/bookMeta.xml";
		return path;
	}

	public String dirsForBook(String bid)
	{
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false)
			return null;

		String bookDirs = Environment.getExternalStorageDirectory() + basePath + bid;
		File dir = new File(bookDirs);
		if(dir.exists() == false)
			if(dir.mkdirs() == false)
				return null;

		String chaptersAudioPath = bookDirs + "/ca";
		File cadir = new File(chaptersAudioPath);
		if(cadir.exists() == false)
			if(cadir.mkdirs() == false)
				return null;


		return bookDirs;    	
	}

	public int handleSrvError(String err)
	{
		int result = 0;

		NodeList shows = null;
		try {
			shows = (NodeList) xPath.evaluate("//error", new InputSource(new StringReader(err)), XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < shows.getLength(); i++) {
			Element show = (Element) shows.item(i);
			try {
				String error = xPath.evaluate("string()", show);
				Log.e("MyError:",String.format("***srv Error: %s", error));
				result = Integer.parseInt(error);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return result;
	}
	public String xeval(String xpath, Element ele)
	{
		String result = null;
		try {
			result = xPath.evaluate(xpath, ele);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	DisplayImageOptions options = new DisplayImageOptions.Builder()
	.showStubImage(R.drawable.loader)
	//.showImageForEmptyUrl(R.drawable.image_for_empty_url)
	.cacheInMemory()
	.cacheOnDisc()
	.build();
	public void displayBookImage(String bid, ImageView iv)
	{
		String uri = "http://"+gs.s().Host()+"/books/"+bid+"/BookImage.jpg";
		iv.invalidate();
		ImageLoader.getInstance().displayImage(uri, iv, options);
	}

	public ArrayList<String> getNodeList(String xpath, String xml) throws Exception {
		NodeList shows = (NodeList) xPath.evaluate(xpath, new InputSource(new StringReader(xml)), XPathConstants.NODESET);
		ArrayList<String> nl = new ArrayList<String>();
		for (int i = 0; i < shows.getLength(); i++) {
			Node show = (Node) shows.item(i);
			nl.add(xPath.evaluate("string()", show));
		}
		return nl;
	}

	public String Host()
	{
		//return "192.168.0.100:8080";
		return "book-smile.ru";
	}
	public String dbp()
	{
		return dbpath() + dbname();
	}

	public void setContext(Context inContext)
	{
		ctx = inContext;
	}
	
	public void initNetNotifier()
	{		 
		// uses db on notifications
		nlistener = new NetworkConnectivityListener();
		nlistener.registerHandler(this, 1);
		nlistener.startListening(ctx);		
	}
	
	public void setDatabase()
	{
		 db = SQLiteDatabase.openDatabase(gs.s().dbp(), null,
					SQLiteDatabase.OPEN_READWRITE
							| SQLiteDatabase.NO_LOCALIZED_COLLATORS);
	}

	public String dbname()
	{
		return DB_NAME;
	}

	public String dbpath()
	{
		return Environment.getExternalStorageDirectory() +  "/Android/data/" + ctx.getPackageName() + "/databases/";
	}

	NetworkConnectivityListener nlistener;
	private gs() {
		//FileManager.DropAllFiles();
	}

	public static gs s()
	{
		if (_instance == null)
		{
			_instance = new gs();
		}
		return _instance;
	}

}
