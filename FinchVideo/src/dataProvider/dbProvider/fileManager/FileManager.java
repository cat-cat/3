package dataProvider.dbProvider.fileManager;


import com.audiobook.gs;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import dataProvider.entities.Track;

import ru.old.Errors;

import android.graphics.Bitmap;
import android.os.Environment;
import android.os.StatFs;
//import android.util.Log;

/**
 * Файловый менеджер, ответственный за загрузку, сохранение текста, аудио, изображений
 * на карточку памяти.
 * @author mikalaj
 *
 */
public class FileManager {
	/**
	 * Свойство, характеризующее карточку как доступную на чтение/запись.
	 */
	private boolean mExternalStorageAvailable = false;
	/**
	 * Свойство, характеризующее карточку как доступную на чтение.
	 */
	private boolean mExternalStorageWriteable = false;
	/**
	 * Поле, хранящее название папки для аудио.
	 */
	public static final String audio = "/Android/data/com.audiobook/audio";
	/**
	 * Поле, хранящее название папки для текста.
	 */
	public static final String text = "/Android/data/com.audiobook/text";
	/**
	 * Поле, хранящее название папки для изоражений книг.
	 */
	public static final String images = "/Android/data/com.audiobook/book_images";
	/**
	 * Поле, хранящее название папки для каталога.
	 */
	public static final String catalog = "/Android/data/com.audiobook/catalog.xml";
	/**
	 * Имя последнего использовавшегося файла.
	 */
	private String name;
	/**
	 * Имя последней использовавшейся папки.
	 */
	private String lastUse;
//	private InputStream netSource;
	public static final int lowMemory = 100;
	/**
	 * Создание файлового менеджена. Проверка доступности
	 * карты памяти.
	 */
	public FileManager(){
	}
	/**
	 * Получение последнего использовавшегося имени файла.
	 * @return имя последнего файла.
	 */
	public String getName(){
		return name;
	}	
	
	/**
	 * Проверяет доступ к ExternalStorage.
	 * @return true если доступ есть, false если доступа нет.
	 */
	public static final boolean IsStorageAvaliable()
	{
		String state = Environment.getExternalStorageState();
		return (Environment.MEDIA_MOUNTED.equalsIgnoreCase(state) 
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equalsIgnoreCase(state)); 
	}
		
	private static boolean DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);

	    fileOrDirectory.delete();
	    return true;
	}
	
	public static boolean delete(String path)
	{
		boolean result = false;
		File dir = new File(path);
		if(dir.exists())
			result = DeleteRecursive(dir);
		else
			result = true;
		
		return result;		
	}
	/**
	 * Удаление всех файлов относящихся к книге.
	 * @param bookId - идентификационный номер книги.
	 * @param trackId - идентифиакционный номер трека.
	 * @return
	 */
	public static boolean DeleteBook(String bookId)
	{		
		boolean result = false;
		File dir = new File(gs.s().dirsForBook(bookId));
		if(dir.exists())
			result = DeleteRecursive(dir);
		else
			result = true;
		
		return result;
	}
	
	/**
	 * Удаляет все файлы, которые находятся в директории 
	 * <code>getExternalStorageDirectory() + "/Android/data/com.audiobook.audiobook/"</code>
	 * Вложенные директории не удаляются.
	 */
	public static void DropAllFiles()
	{
		DeleteRecursive(new File(gs.s().baseDir() + "/Android/data/com.audiobook.audiobook"));
	}
	/**
	 * check the size of avaliable storage
	 * @return the size of avaliable memory or zero if
	 * avaliable memory > lowMemomory
	 */
	public static long AvaliableMemory(){
		StatFs stat = new StatFs(gs.s().baseDir());
		long bytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
		long megAvailable = bytesAvailable / 1048576;
		if(megAvailable<lowMemory)
			return megAvailable;
		return 0;
	}
	
}
