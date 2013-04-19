package com.audiobook;

import java.util.ArrayList;

import dataProvider.dbProvider.fileManager.FileManager;

import ru.old.DownloadManager;
import ru.old.Load;

import com.audiobook.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class DownloadsArrayAdapter extends ArrayAdapter<Load> {
	  private final Activity context;
	  private final ArrayList<Load> loadings;


	  public DownloadsArrayAdapter(Activity context, ArrayList<Load> l) {
	    super(context, R.layout.downloads_list_item, l);
	    this.context = context;
	    this.loadings = l;
	  }

	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    View view = convertView;
	    if (view == null) {
	      LayoutInflater inflater = context.getLayoutInflater();
	      view = inflater.inflate(R.layout.downloads_list_item, null);
	      final Load l = loadings.get(position);
	      
	      // setup textview
          TextView tv = (TextView)
          view.findViewById(R.id.video_text);
          String videoText = l.trackNumber;
          tv.setText(videoText);
	      
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
	    }


	    return view;
	  }
} 