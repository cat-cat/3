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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import dataProvider.dbProvider.fileManager.FileManager;
import dataProvider.internetProvider.helpers.ConnectionErrorCodes;
import dataProvider.internetProvider.helpers.SourceProvider;

import ru.librofon.Errors;

import java.util.ArrayList;
import android.content.Context;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.util.Log;


public class gs {
	// The Android's default system path of your application database.
	private final String basePath = "/Android/data/com.audiobook.audiobook/";
	private Context ctx;
	private String DB_NAME = "database.db";
		
    private static gs   _instance;

    private String android_id = Secure.ANDROID_ID;

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
	
	public String pathForBuy(String bid)
	{
    	String newDirPath = dirsForBook(bid);
    	String path = newDirPath + "/buy";
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
    	
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
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
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
	    String result = null;
		try {
			result = xPath.evaluate(xpath, ele);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return result;
    }
    
    public ArrayList<String> getNodeList(String[] args) throws Exception {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        NodeList shows = (NodeList) xPath.evaluate(args[0], new InputSource(new StringReader(args[1])), XPathConstants.NODESET);
        ArrayList<String> nl = new ArrayList<String>();
        for (int i = 0; i < shows.getLength(); i++) {
          Element show = (Element) shows.item(i);
          nl.add(xPath.evaluate("string()", show));
        }
        return nl;
      }

    public String Host()
    {
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
    
    public String dbname()
    {
    	return DB_NAME;
    }
    
    public String dbpath()
    {
    	return Environment.getExternalStorageDirectory() +  "/Android/data/" + ctx.getPackageName() + "/databases/";
    }
    
   private gs() {}
   
   public static gs s()
    {
        if (_instance == null)
        {
            _instance = new gs();
       }
        return _instance;
    }

}
