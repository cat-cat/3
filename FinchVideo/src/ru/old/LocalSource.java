package ru.old;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;

//import android.util.Log;

/**
 * Поток содержащий трек который храниться локально, то есть на телефоне.
 */
class LocalSource extends DataSource
{
	public static final String TAG = "AudioServer";

	private RandomAccessFile audio;
	
	private final long rangeFrom;
	
	private final long rangeTo;
	
	private final long trackSize;

	public LocalSource(String uri, long rangeFrom, long rangeTo, long size)
	{
		this.rangeFrom = rangeFrom;
		this.rangeTo = rangeTo;
		this.trackSize = size;
		try
		{
			int index = uri.indexOf('?');
			if(index == -1)
				audio = new RandomAccessFile(new File(URI.create(uri)), "r");
			else
				audio = new RandomAccessFile(new File(URI.create(uri.substring(0, index))), "r");
		}
		catch (FileNotFoundException e)
		{
	//		Log.e(TAG, "LocalSource - File not found");
	//		e.printStackTrace();
		}
		
		if(audio == null)
			return;
		
		if(this.rangeFrom > 0)
			try
			{
				audio.seek(rangeFrom);
			}
			catch (IOException e)
			{
	//			Log.e(TAG, "LocalSource - File cant seek.");
	//			e.printStackTrace();
			}
	}
	
	@Override
	public ReadableByteChannel CreateReadChannel() throws IOException
	{
		if(audio != null)
			return audio.getChannel();
		else
			return null;
	}


	@Override
	public String GetContentType()
	{
		//return "audio/mp3";
		return "audio/mpeg";
	}

	@Override
	public long GetContentLength()
	{
		if (audio == null)
		{
			return -1;
		}
		
		try
		{
			if(rangeTo > 0)
			{
				return rangeTo - rangeFrom + 1;
			}
			else
			{
				if(trackSize > 0)
					return trackSize - rangeFrom;
				else
					return audio.length() - rangeFrom;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return -1;
		}
	}
	
	@Override
	public long GetLength()
	{
		try
		{
			if(trackSize > 0)
				return trackSize;
			else
				return audio.length();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public long RangeFrom()
	{
		return rangeFrom;
	}
	
	@Override
	public long RangeTo()
	{
		return rangeTo;
	}
}
