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
		checkAvaliability();
	}
	/**
	 * Получение последнего использовавшегося имени файла.
	 * @return имя последнего файла.
	 */
	public String getName(){
		return name;
	}
	/**
	 * Метод, позволяющий сгенерировать полное имя файла.
	 * @param bookId ид книги.
	 * @param trackId ид трека.
	 * @return полное имя файла.
	 */
	private String makeName(String bookId, String trackId){
		String name = gs.s().pathForBookAndChapter(bookId, trackId);
		
//		name = new String();
//		name+=lastUse;
//		name+="/"+String.valueOf(bookId)+"."+trackId;
//		Log.d("Making name in filemanager", name);
		return name;
	}
	/**
	 * Метод, позволяющий сформировать полное имя файла при загрузке картинки.
	 * @param bookId ид книги.
	 * @return полное имя для файла изображения.
	 */
	private String makeName(int bookId){
		name = new String();
		name+=lastUse;
		name+="/"+String.valueOf(bookId)+".jpg";
		return name;
	}
	/**
	 * Метод, проверяющий доступность карты памяти.
	 */
	private void checkAvaliability(){
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}
	public InputStream getCatalogStream(){
		this.checkAvaliability();
		if(this.mExternalStorageAvailable){
			String fileName = Environment.getExternalStorageDirectory()+FileManager.catalog; 
			File f = new File(fileName);
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public void removeCatalog(){
		String fileName = Environment.getExternalStorageDirectory()+FileManager.catalog; 
		File f = new File(fileName);
		if(f.exists())
			f.delete();
	}
	public boolean saveCatalog(InputStream inStream) {
		boolean result = false; 
		if(this.mExternalStorageAvailable){
			String fileName = Environment.getExternalStorageDirectory() + FileManager.catalog; 
			File f = new File(fileName);
			FileOutputStream stream = null;
			try
			{
				stream = new FileOutputStream(f);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			if(inStream != null && stream != null)
			{
				BufferedOutputStream outbf = new BufferedOutputStream(stream);
				BufferedInputStream inbf = new BufferedInputStream(inStream);
				byte[] buf = new byte[256]; 
				int count = -1;
				try
				{
					while( (count = inbf.read(buf)) != -1 )
						outbf.write(buf, 0, count);
					result = true;
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
				finally
				{
					if(outbf != null)
						try
						{
							outbf.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					if(inbf != null)
						try
						{
							inbf.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
				}
			}
		}
		return result;
	}
	/**
	 * Метод, загружающий картинку по ид книги.
	 * @param bookId ид книги.
	 * @return входной поток, если удалось.
	 * @throws FileNotFoundException возникает, если файл не существует.
	 */
	public InputStream GetImage(int bookId) throws FileNotFoundException{
		if(!this.mExternalStorageAvailable) return null;
			this.lastUse = FileManager.images;
			String fileName = makeName(bookId);
			String st = Environment.getExternalStorageDirectory()+fileName; 
			File f = new File(st);			
			FileInputStream stream = new FileInputStream(f);
			return stream;
	}
	/**
	 * Метод, загружающий текст по ид книги и трека.
	 * @param bookId ид книги.
	 * @param trackId ид трека.
	 * @return входной поток, если удалось.
	 * @throws FileNotFoundException возникает, если файл не может быть обнаружен.
	 */
	public InputStream getText(String bookId, String trackId) throws FileNotFoundException{
		if(!this.mExternalStorageAvailable) return null;
		this.lastUse = FileManager.text;
		String fileName = makeName(bookId, trackId);
		String st = Environment.getExternalStorageDirectory()+fileName; 
		File f = new File(st);		
		FileInputStream stream = new FileInputStream(f);
		this.lastUse = FileManager.text;
		return stream;
	}
	/**
	 * Метод, позволяющий загрузить аудио по ид книги и трека.
	 * @param bookId ид книги.
	 * @param trackId ид трека.
	 * @return входной поток, если удалось.
	 * @throws FileNotFoundException возникает при отсутствии файла.
	 */
	public InputStream getAudio(String bookId, String trackId) throws FileNotFoundException{
		if(!this.mExternalStorageAvailable) return null;
			this.lastUse = FileManager.audio;
			String fileName = makeName(bookId, trackId);
			String st = Environment.getExternalStorageDirectory()+fileName;
			File f = new File(st);			
			FileInputStream stream = new FileInputStream(f);
			this.lastUse = FileManager.audio;
			return stream;
	}
	/**
	 * Метод, позволяющий сохранить картинку.
	 * @param inStream входной поток, из которого будем сохранять.
	 * @param bookId ид книги.
	 */
	public void SaveImage(InputStream inStream, int bookId){
		if(!this.mExternalStorageWriteable) return;
		File dir = new File(Environment.getExternalStorageDirectory()+images);
		if(!dir.exists()){
			dir.mkdirs();
		}
		try {
			this.lastUse = FileManager.images;
			String fileName = this.getName();
			String st = Environment.getExternalStorageDirectory()+fileName; 
			File f = new File(st);			
			FileOutputStream stream = new FileOutputStream(f);
			BufferedOutputStream outbf = new BufferedOutputStream(stream);
			BufferedInputStream inbf = new BufferedInputStream(inStream);
			byte[] buf = new byte[256]; 
			int count;
			while((count = inbf.read(buf))!=-1){
				outbf.write(buf, 0, count);
			}
			outbf.close();
			inbf.close();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Сохранение изображения на ExternalStorage из {@link Bitmap}. 
	 * @param image изображение которое нужно сохранить
	 * @param bookID идентификационный номер книги, к которому относится изображение.
	 */
	public void SaveImage(Bitmap image, int bookId)
	{
		if(!mExternalStorageWriteable)
			return;
		File dir = new File(Environment.getExternalStorageDirectory() + images);
		if(!dir.exists())
			dir.mkdirs();
		try
		{
			File imageFile = new File(dir.getAbsolutePath() + "/" + Integer.toString(bookId));
			FileOutputStream outStream = new FileOutputStream(imageFile);
			image.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
			outStream.flush();
			outStream.close();
		} 
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Метд, сохраняющий аудио.
	 * @param inStream поток, из которого будем сохранять.
	 * @param bookId ид книги. 
	 * @param trackId ид трека.
	 */
	public void SaveAudio(InputStream inStream, String bookId, String trackId){
		if(!this.mExternalStorageWriteable) return;
		File dir = new File(Environment.getExternalStorageDirectory()+audio);
		if(!dir.exists()){
			dir.mkdirs();
		}
		try {
			this.lastUse = FileManager.audio;
			String fileName = makeName(bookId, trackId);
			String st = Environment.getExternalStorageDirectory()+fileName; 
			File f = new File(st);			
			FileOutputStream stream = new FileOutputStream(f);
			BufferedOutputStream outbf = new BufferedOutputStream(stream);
			BufferedInputStream inbf = new BufferedInputStream(inStream);
			byte[] buf = new byte[256]; 
			int count;
			while((count = inbf.read(buf))!=-1){
				outbf.write(buf, 0, count);
			}
			outbf.close();
			inbf.close();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Метод сохранения текста.
	 * @param inStream поток, из которого сохраняем.
	 * @param bookId ид книги.
	 * @param trackId ид трека.
	 */
	public void SaveText(final InputStream inStream,String bookId,String trackId){
		if(!this.mExternalStorageWriteable) return;
		File dir = new File(Environment.getExternalStorageDirectory()+text);
		if(!dir.exists()){
			dir.mkdirs();
		}
		try {
			this.lastUse = FileManager.text;
			String fileName = makeName(bookId, trackId);
			String st = Environment.getExternalStorageDirectory()+fileName; 
			File f = new File(st);		
			FileWriter fw = new FileWriter(f);
			InputStreamReader isr = new InputStreamReader(inStream);
			char[] buf = new char[256]; 
			int count;
			while((count = isr.read(buf))!=-1){
				fw.write(buf, 0, count);
			}
			fw.close();
			isr.close();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void SaveString(String st){
		if(!this.mExternalStorageWriteable) return;
		File dir = new File(Environment.getExternalStorageDirectory()+text);
		if(!dir.exists()){
			dir.mkdirs();
		}
		try {
			this.lastUse = FileManager.text;
			String str = Environment.getExternalStorageDirectory()+"/test.txt"; 
			File f = new File(str);		
			FileWriter fw = new FileWriter(f);
			fw.write(st);
			fw.close();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
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
	
	/**
	 * Проверяет наличие аудио файла, и его доступность для чтения.
	 * @param bookId - идентификационный номер книги.
	 * @param trackNumber - номер трека {@link Track#number}
	 * @return true если файл существует и доступен для чтения.
	 * false во всех других случаях.
	 */
	public static final boolean IsFileAvaliable(String bookId, String trackNumber)
	{
		File file = new File(PathToAudioFile(bookId, trackNumber));
		return file.exists() && file.canRead();
	}
	
	/**
	 * Метод, загружающий картинку по ид книги.
	 * @param bookId ид книги.
	 * @return входной поток, если удалось.
	 * @throws FileNotFoundException возникает, если файл не существует.
	 */
	public static InputStream GetBookCover(int bookId) throws FileNotFoundException
	{
//		if(!this.mExternalStorageAvailable)
//			return null;
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false)
			return null;
		
		String path = PathToImageFile(bookId);
		File coverFile = new File(path);		
		if(coverFile.exists())
			return new FileInputStream(coverFile);
		else
			return null;
	}
	
	/**
	 * Получение адреса по которому должна храниться обложка книги.
	 * @param bookId идентификационный номер книги.
	 * @return строка содержащая путь к текстовому файлу.
	 */
	public static String PathToImageFile(int bookId)
	{
		return Environment.getExternalStorageDirectory() + images + "/" + Integer.toString(bookId);
	}
	
	/**
	 * Получение адреса по которому должен храниться текстовый файл.
	 * @param bookId идентификационный номер книги.
	 * @param trackId идентификационный номер трека.
	 * @return строка содержащая путь к текстовому файлу.
	 */
	public static String PathToTextFile(String bookId)
	{
		return Environment.getExternalStorageDirectory() + text + "/" + bookId;
	}
	
	/**
	 * Получение адреса по которому должен храниться аудио файл.
	 * @param bookId идентификационный номер книги.
	 * @param trackId идентификационный номер трека.
	 * @return строка содержащая путь к аудио файлу.
	 */
	public static String PathToAudioFile(String bookId, String trackId)
	{
		//return Environment.getExternalStorageDirectory() + audio + "/" + bookId + "." + trackId;
		return gs.s().pathForBookAndChapter(bookId, trackId);
	}
	
	/**
	 * Удаление всех файлов относящихся к книге.
	 * @param bookId идентификационный номер книги.
	 * @return информацию об ошибках произошедших при удалении:<br/>
	 * {@link Errors#EXTERNAL_STORAGE_NOT_AVAILABLE} - когда устройство хранения не доступно или доступно в режиме только чтения.<br/>
	 * {@link Errors#FILE_CANT_ACCESS} - если при удалении произошла ошибка доступа {@link SecurityException}<br/>
	 * {@link Errors#NO} - если при удалении ошибок не произошло, т.е. все файлы удалены.<br/>
	 * 
	 */
	public static Errors RemoveFilesAssociatedWithBook(String bookId)
	{
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false)
			return Errors.EXTERNAL_STORAGE_NOT_AVAILABLE;
		
		try
		{
			BookFilesFilter filter = new BookFilesFilter(bookId);
			
			File[] audioFiles = new File(Environment.getExternalStorageDirectory() + audio).listFiles(filter);
			if(audioFiles != null)
				for(File file : audioFiles)
					file.delete();
			
			File textFile = new File(PathToTextFile(bookId));
				if(textFile.exists())
					textFile.delete();
		}
		catch(SecurityException e)
		{
			return Errors.FILE_CANT_ACCESS;
		}	
		return Errors.NO;
	}
	
	/**
	 * Удаление всех файлов относящихся к треку.
	 * @param bookId - идентификационный номер книги.
	 * @param trackId - идентифиакционный номер трека.
	 * @return
	 */
	public static boolean DeleteTrackFiles(String bookId, String trackId)
	{
		boolean result = false;
		File file = new File(PathToAudioFile(bookId, trackId));
		if(file.exists())
			result = file.delete();
		else
			result = true;
		
		return result;
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
	 * Удаляет бесплатный файлы для книги.
	 * @param bookId идентификатор книги.
	 * @return true если файлы успешно удалены или не существовали, false если произошла ошибка
	 */
	public static boolean DeleteFreeFiles(String bookId)
	{
		boolean result = false;
		
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false)
			return result;

		File file = new File(PathToTextFile(bookId));
		if(file.exists())
			result = file.delete();
		else
			result = true;
		
		file = new File(PathToAudioFile(bookId, Track.FREE_TRACK_NUMBER));
		if(file.exists())
			result = result & file.delete();
		else
			result = result & true;
		
		return result;
	}

	/**
	 * Создаёт директории необходимые для приложения на внешнем устройстве хранения.
	 * @return результат создания директорий. <br/>
	 *     {@link Errors#NO} - если были созданы или создавать не нужно.<br/>
	 *     {@link Errors#EXTERNAL_STORAGE_NOT_AVAILABLE} - когда устройство хранения не доступно или доступно в режиме только чтения.<br/>
	 *     {@link Errors#FILE_CANT_CREATE} - когда не получилось создать цепочку дирректорий.<br/>
	 */
	public static Errors CreateApplicationDirs()
	{
		Errors result = Errors.NO;
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false)
			return Errors.EXTERNAL_STORAGE_NOT_AVAILABLE;
		
		File dir = new File(Environment.getExternalStorageDirectory() + images);
		if(dir.exists() == false)
			if(dir.mkdirs() == false)
				return Errors.FILE_CANT_CREATE;
		
		dir = new File(Environment.getExternalStorageDirectory() + text);
			if(dir.exists() == false)
				if(dir.mkdirs() == false)
					return Errors.FILE_CANT_CREATE;
		
		dir = new File(Environment.getExternalStorageDirectory() + audio);
			if(dir.exists() == false)
				if(dir.mkdirs() == false)
					return Errors.FILE_CANT_CREATE;
		return result;
	}
	
	/**
	 * Удаляет все файлы, которые находятся в директории 
	 * <code>getExternalStorageDirectory() + "/Android/data/com.audiobook.audiobook/"</code>
	 * Вложенные директории не удаляются.
	 */
	public static void DropAllFiles()
	{
		DeleteRecursive(new File(Environment.getExternalStorageDirectory() + "/Android/data/com.audiobook.audiobook"));

		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false)
			return;
		
		File dir = new File(Environment.getExternalStorageDirectory() + audio);
		if(dir != null)
		{
			File[] audioFiles = dir.listFiles();
			if(audioFiles != null)
				for(File file : audioFiles)
					file.delete();
		}
		
		dir = new File(Environment.getExternalStorageDirectory() + text);
		if(dir != null)
		{
			File[] textFiles = dir.listFiles();
			if(textFiles != null)
				for(File file : textFiles)
					file.delete();
		}
	}
	/**
	 * check the size of avaliable storage
	 * @return the size of avaliable memory or zero if
	 * avaliable memory > lowMemomory
	 */
	public static long AvaliableMemory(){
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long bytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
		long megAvailable = bytesAvailable / 1048576;
		if(megAvailable<lowMemory)
			return megAvailable;
		return 0;
	}
	
	public static void DeleteImages()
	{
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false)
			return;
		
		File dir = new File(Environment.getExternalStorageDirectory() + images);
		if(dir != null)
		{
			File[] files = dir.listFiles();
			for (File f : files)
				f.delete();
		}
	}
}
