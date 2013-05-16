package com.audiobook;
import java.io.StringReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;


public class BookActivity extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_activity);
        
        final String bookId = getIntent().getExtras().getString("bid");
        gs.s().displayBookImage(bookId, (ImageView)findViewById(R.id.bookd_image));
        
        new AsyncTask<Void,Void,Bundle>()
        {

			@Override
			protected Bundle doInBackground(Void... arg0) {
			    
				String bookMeta = gs.s().fileToString(gs.s().pathForBookMeta(bookId));
				XPathFactory factory = XPathFactory.newInstance();
				XPath xPath = factory.newXPath();

				Bundle b = new Bundle();
				
				String bookDescription = "";
				try {
					bookDescription = xPath.evaluate("/abooks/abook/description",
							new InputSource(new StringReader(bookMeta)));
				} catch (XPathExpressionException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				b.putString("description", bookDescription);
				
				String bookTitle = "";
				try {
					 bookTitle = xPath.evaluate("/abooks/abook/title",
							new InputSource(new StringReader(bookMeta)));
				} catch (XPathExpressionException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				b.putString("title", bookTitle);
				
				String bookPrice = "";
				try {
					bookPrice = xPath.evaluate("/abooks/abook/price",
							new InputSource(new StringReader(bookMeta)));
				} catch (XPathExpressionException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				b.putString("price", bookPrice);
			    
				
				String bookAuthors = "";
				try {
					bookAuthors = xPath.evaluate("/abooks/abook/authors",
							new InputSource(new StringReader(bookMeta)));
				} catch (XPathExpressionException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				b.putString("authors", bookAuthors);
				
				String bookReaders = "";
				try {
					bookReaders = xPath.evaluate("/abooks/abook/readers",
							new InputSource(new StringReader(bookMeta)));
				} catch (XPathExpressionException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				b.putString("readers", bookReaders);
				
				String bookLength = "0";
				try {
					bookLength = xPath.evaluate("/abooks/abook/length",
							new InputSource(new StringReader(bookMeta)));
				} catch (XPathExpressionException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				int bLength = Integer.parseInt(bookLength);
				String length = String.format("%d ч. %d мин.", bLength / 3600, (bLength % 3600) / 60);
				b.putString("length", length);

				
				
				String bookSize = "0";
				try {
					bookSize = xPath.evaluate("/abooks/abook/size",
							new InputSource(new StringReader(bookMeta)));
				} catch (XPathExpressionException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				
				int bSize = Integer.parseInt(bookSize);
				String size = String.format("%.1f Мб", bSize / 1024.0f / 1024.0f);
				b.putString("size", size);
				
			    
				return b;
			}
			
			@Override
			protected void onPostExecute(Bundle b)
			{
				
				((TextView)  findViewById(R.id.bookd_title)).setText(b.getString("title"));
				((TextView)  findViewById(R.id.bookd_description)).setText(b.getString("description"));
				((TextView)  findViewById(R.id.authors_value)).setText(b.getString("authors"));
				((TextView)  findViewById(R.id.readers_value)).setText("Чтец(ы): "+b.getString("readers"));
				((TextView)  findViewById(R.id.price_value)).setText("$"+b.getString("price"));
				
				((TextView)  findViewById(R.id.length_value)).setText("Длительность: "+b.getString("length"));

				((TextView)  findViewById(R.id.size_value)).setText("Размер: "+b.getString("size"));
				
			}
        	
        }.execute();

	
	
	}

}
