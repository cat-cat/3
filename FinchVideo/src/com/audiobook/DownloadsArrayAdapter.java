package com.audiobook;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import dataProvider.dbProvider.fileManager.FileManager;

import ru.old.DownloadManager;
import ru.old.Load;

import com.audiobook2.R;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class DownloadsArrayAdapter extends ArrayAdapter<Load> {

	XPathFactory factory = XPathFactory.newInstance();
	XPath xPath = factory.newXPath();
	private final Activity context;
	private final ArrayList<Load> loadings;


	public DownloadsArrayAdapter(Activity context, ArrayList<Load> l) {
		super(context, R.layout.downloads_list_item, l);
		this.context = context;
		this.loadings = l;
	}

//	private static String prevBookId;
//	private static String tmpBookTitle;
//	private static String tmpBookMeta;
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = context.getLayoutInflater();
		final View view = inflater.inflate(R.layout.downloads_list_item, null);
		final Load l = loadings.get(position);
		// setup textview
		final TextView tv = (TextView) view.findViewById(R.id.video_text);
		tv.setText("");
		final TextView tv2 = (TextView) view.findViewById(R.id.text_title);
		tv2.setText("");


		class loadTask extends AsyncTask<Void,Void,String[]>
		{

			@Override
			protected String[] doInBackground(Void... arg0) {

//				if(!l.bookId.equalsIgnoreCase(prevBookId))
//				{
//					prevBookId = l.bookId;
//					StringReader sr =  new StringReader(tmpBookMeta);
//					if(is==null)
//						return new String[]{"",""};// error
					
					// first set title
					String title = null;
					String chapter = null;
					try {
						String tmpBookMeta = gs.s().fileToString(gs.s().pathForBookMeta(l.bookId));
						InputSource is = new InputSource( new StringReader(tmpBookMeta));
						title = xPath.evaluate("/abooks/abook/title",is);
						is = new InputSource(new StringReader(tmpBookMeta));
						chapter = xPath.evaluate(String.format("/abooks/abook/content/track[@number='%s']/name",l.trackNumber),is);
					} catch (XPathExpressionException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
//				}


				return new String[]{title,chapter};
			}

			@Override
			protected
			void onPostExecute(String[] args)
			{
				// setup textview
				String videoText = args[1]; // chapter
				tv.setText(videoText);
				String titleText = args[0]; // title
				tv2.setText(titleText);
			}


		}
		new loadTask().execute();

		// setup button
		Button deleteButton = (Button) view.findViewById(R.id.btn_delete);
		deleteButton.setTag(position);

		deleteButton.setOnClickListener(
				new Button.OnClickListener() {
					@Override
					public void onClick(View v) {
						Integer index = (Integer) v.getTag();
						loadings.remove(index.intValue());
						DownloadManager.s(context).RemoveFromQuery(l.bookId, l.trackNumber);
						notifyDataSetChanged();
					}
				});

		return view;
	}

}
