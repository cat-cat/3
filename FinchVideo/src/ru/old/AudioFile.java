package ru.old;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

//import android.util.Log;

public class AudioFile
{
	private static final DateFormat dateParser;
	static
	{
		dateParser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
		dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private static final DateFormat durationParser;
	static
	{
		durationParser = new SimpleDateFormat("HH:mm:ss.SSZ");
	}
	
	public String url;
	
	/** Длительность файла в секундах*/
	public int duration;
	
	public void SetDuration(String value)
	{
		try
		{
//			Log.d("Librofon", "Audio duration value: " + value);
			duration = (int) ((durationParser.parse(value + "+0000").getTime()) / 1000);
//			Log.d("Librofon", "Audio duration: " + Integer.toString(duration));
		}
		catch (ParseException e)
		{
			e.printStackTrace();
			duration = 0;
		}
	}
	
	public Date date;
	
	public void SetDate(String value)
	{
		try
		{
			date = dateParser.parse(value.replace(" GMT", ""));
		}
		catch (ParseException e)
		{
			e.printStackTrace();
			date = new Date();
		}
	}
	
	public Date expires;
	
	public void SetExpires(String value)
	{
		try
		{
			expires = dateParser.parse(value.replace(" GMT", ""));
		}
		catch (ParseException e)
		{
			e.printStackTrace();
			expires = new Date();
		}
	}
	
	/** Номер скачиваемоей бесплатной части */
	public int partNumber;
	
	/** Свидетельствует о возврате сервером кода ошибки, через xml */
	public boolean error;
	
	/** Код ошибки который возвращается через заголовок HTTP */
	public int errorCode; 
	
	public AudioFile()
	{
		error = false;
		errorCode = 0;
		duration = 0;
		partNumber = 0;
	}
}
