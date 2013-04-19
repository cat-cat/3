package com.audiobook;

import com.audiobook.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class CatalogAdapter extends ArrayAdapter<CatalogItem> {
	
	private CatalogItem[] catalogItems;
	
	  public CatalogAdapter(Context context, CatalogItem[] values) {
		    super(context, R.layout.video_list_item, values);
		    this.catalogItems = values;
		  }
	  
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			// TODO: implement right
			return convertView;
		}
}
