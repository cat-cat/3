package dataProvider.internetProvider.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import ru.old.AudioFile;

import android.util.Log;
import android.util.Xml;

public class TrackDownloadUrlParser
{
//	private static final String XML_ABOOK = "abooks";
	private static final String XML_URL = "url";
	private static final String XML_PART_NUM = "num";

	private static final String XML_ERROR = "error";
//	private static final String XML_CODE = "code";
	
	public String Parse(InputStream stream, AudioFile file)
	{
		String url = null;
		InputStreamReader reader = null;
		XmlPullParser xpp = null;
		try
		{
			reader = new InputStreamReader(stream);
			xpp = Xml.newPullParser();
			xpp.setInput(reader);
			
			int eventType = xpp.getEventType();
			String xmlName = null;
	        while (eventType != XmlPullParser.END_DOCUMENT)
	        {
	        	if(eventType == XmlPullParser.START_TAG)
                {
                	xmlName = xpp.getName();
                  	if (XML_URL.equalsIgnoreCase(xmlName))
                  	{
                  		url = new String(xpp.nextText());
                  	}
                  	else if ( XML_PART_NUM.equalsIgnoreCase(xmlName) )
                  	{
                  		file.partNumber = Integer.parseInt(xpp.nextText());
                  	}
                  	else if (XML_ERROR.equalsIgnoreCase(xmlName))
                  	{
                  		file.error = true;
                  		break;
                  	}
                }
	            eventType = xpp.next();
	        }
		}
		catch (XmlPullParserException e)
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
			if(xpp != null)
				try
				{
					xpp.setInput(null);
				}
				catch (XmlPullParserException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if(reader != null)
				try
				{
					reader.close();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
		return url;
	}
}
