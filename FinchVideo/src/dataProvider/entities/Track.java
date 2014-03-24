package dataProvider.entities;

import java.util.Calendar;
import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.Random;
//import java.util.TimeZone;
//
//import android.util.Log;
//
//import ru.old.ServerDrivenSettings;

/**
 * Класс, представляющий информацию о треке.
 * @author mikalaj
 *
 */
public class Track {
	
	/**
	 * Значение поля {@link #number} для бесплатного трека.
	 */
	public static final String FREE_TRACK_NUMBER = "free";
	
	/**
	 * Значени поля {@link #name} для бесплатного трека.
	 */
	public static final String FREE_TRACK_NAME = "free_track";
	
	/**
	 * Если трек ещё не записан в БД, то его {@link #id} равен этому значению.
	 */
	public static final int DO_NOT_HAVE_ID = -1;
	/**
	 * Номер трека в базе данных. (использовать с осторожно, в основном api 
	 * в качестве номера трека необходимо использовать {@link #number}
	 */
	public int id;
	/**
	 * Номер трека.
	 */
	public String number;
	/**
	 * Имя трека.
	 */
	public String name;
	/**
	 * Файл, с которым связан трек.
	 */
	public File file;
	
	/**
	 * Процент закачки аудио части трека.
	 */
	public int audioDownloadProcentage;
	
	/**
	 * Время на которое прослушан трек.
	 */
	public int listen;
	
	/**
	 * Тип ключа для расшифровки.
	 * Если значение 0, то используется статичный ключ для передачи.
	 * Если значние (0;200] то в качестве ключа используется hashmd5 устройства.  
	 */
	//public byte keyType;
	
	public Track(){
		id = DO_NOT_HAVE_ID;
		audioDownloadProcentage = 0;
//		textDownloadProcentage = 0;
		listen = 0;
		offset = 0;
		file = new File();
		//keyType = 0;
	}
	/** 
	 * Получение строкового представления.
	 */
	public String bookId;
	public Date from;
	/**
	 * Для купленного трека - значение не известно.
	 * Дата когда можно будет скачать следующий бесплатный трек.
	 */
	public Date to;
	/** Дата создания трека. Для бесплатного трека - дата загрузки части.
	 * Для бесплатного трека может быть null, если ниодной части загружено не было. 
	 */
	public Date created_at;
	public boolean isFree = false;
	/**
	 * Нумар першага бачнага сімвала пры пракрутцы 
	 * тэкста.
	 */
	public int offset;
	public String toString(){
		String st = new String();
		st+="Track:\n";
		st+="Name:"+name+"\n";
		st+="Number:"+number+"\n";

		st+=file+"\n";
		return st;
	}
	public void clearValues(){
//		bookId = 0;
		number = null;
		name = null;
		from = null;
		to = null;
		created_at = null;
		isFree = false;
	}
	
	/**
	 * Проверяет бесплатный ли трек.
	 * @return true если трек относится к книге взятой в прокат.
	 */
	public boolean isFree()
	{
		return (isFree || FREE_TRACK_NUMBER.equalsIgnoreCase(number));
	}
	
	/**
	 * Проверяет можно ли закачать следующий бесплатный трек.
	 * Для треков купленной книги всегда возвращает true. 
	 * @return true - можно, false - нельзя.
	 */
//	public boolean CanDownloadNextFreeTrack()
//	{
//		Log.d("Load", "----------------------------------------------");
//		TimeZone tz = TimeZone.getTimeZone(TimeZone.getAvailableIDs(0)[0]);
//		Log.d("Load", "Current time = " + Calendar.getInstance().getTime().toGMTString());
//		if ( to != null )
//			Log.d("Load", "To = " + to.toGMTString());
//		if(isFree() && to != null)
//			return to.before(Calendar.getInstance().getTime());
//		return true;
//	}
	
	/**
	 * Генерация даты доступности следующего трека (начало следующего дня по Москве).
	 * Возращает эту дату и записывает значение её в {@link #to}
	 * @return дата (новый объект не связанный с {@link #to}, когда можно 
	 *     скачать слудующую бесплатную часть.
	 */
//	public Date GenerateNewExpiresDateToNextDay()
//	{
//		TimeZone zone = null;
//		try
//		{
//			zone = TimeZone.getTimeZone(TimeZone.getAvailableIDs(14400000)[0]);
//		}
//		catch(IndexOutOfBoundsException e)
//		{
//			zone = TimeZone.getDefault();
//		}
//		Calendar c = Calendar.getInstance(zone);
//		c.set(Calendar.HOUR_OF_DAY, 0);
//		c.set(Calendar.MINUTE, 0);
//		c.set(Calendar.SECOND, 0);
//		c.set(Calendar.MILLISECOND, 0);
//		
//		c.add(Calendar.HOUR_OF_DAY, ServerDrivenSettings.GetInstance().GetFreeDownloadPeriod());
//		
//		to = c.getTime();
//		return c.getTime();
//	}
	
	/**
	 * Генирирует псевдо случайное значение для {@link #keyType} 
	 * в диапазоне от (0;200]
	 */
//	public void GenerateType()
//	{
//		Random r = new Random();
//		keyType = (byte) (r.nextInt(200) + 1);
//		if(keyType == 0)
//			keyType = 1;
//	}
	
	public boolean IsCurrentListenPositionInEnd()
	{
		return file.length - listen < 2;
	}
}
