package com.audiobook;


import java.io.StringReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.util.ArrayList;
import android.content.Context;
import android.os.Environment;
import android.util.Log;


public class gs {
	// The Android's default system path of your application database.
	private Context ctx;
	private String DB_NAME = "database.db";
		
    private static gs   _instance;

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
