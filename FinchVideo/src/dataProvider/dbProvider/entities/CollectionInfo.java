package dataProvider.dbProvider.entities;

import java.util.Date;

/**
 * Класс представляющий набор полей - столбцов
 * в таблице коллекции.
 * @author mikalaj
 *
 */
public class CollectionInfo {
	
	/** Идентификационный номер книги. */
	public int id;
	
	/** Статус загрузки книги (в процентах). */
	public int download_status;
	
	/** Cтатус прочтения книги (в процентах). */
	public int read_status;
	
	/** Флаг: true - если трек бесплатный. */
	public boolean demo;
	/**
	 * свободное время.
	 */
//	public int free_time;
	
	/** Флаг: true если бесплатный трек уже был прослушан полностью. */
	public boolean free_listened;
	
	/** Номер бесплатной части */
	public int free_part;
	
	/** Количество бесплатных частей. */
	public int free_parts_count;
	
	/** Дата последнего прослушивания. */
	public Date last_listen;
	
	/**
	 * Процент закачки текстовой части книги. (0 - 100)
	 */
	public int textDownloadProcentage;
	
	/**
	 * {@link dataProvider.entities.Track#id} последнего прослушанного трека,
	 * если ещё ни одного трека не было прослушано то равно -1;
	 */
	public int last_track;
	
	/**
	 * Дата добавления в коллекцию.
	 */
	public Date date_added;
	
	/**
	 * Конструктор.
	 * @param id ид.
	 * @param download_status статус загрузки.
	 * @param read_status статус прочтения.
	 * @param demo режим демо.
	 * @param free_time свободное время.
	 * @param last_listen последнее прослушивание.
	 */
	public CollectionInfo(int id, int download_status, int read_status, boolean demo, int free_part, int free_parts_count, boolean free_listened, Date last_listen, int last_track, int textDownloadProcentage, Date date_added){
		this.id = id;
		this.download_status = download_status;
		this.read_status = read_status;
		this.demo = demo;
		this.free_listened = free_listened;
		this.free_part = free_part;
		this.free_parts_count = free_parts_count;
		this.last_listen = last_listen;
		this.last_track = last_track;
		this.textDownloadProcentage = textDownloadProcentage;
		this.date_added = date_added;
	}
	/**
	 * Пустой конструктор для удобства.
	 */
	public CollectionInfo(){;}
	
	public static final int FULL_LOADED = 100;
	
	public static final String TEXT_FLAG = "text";
	
//	public static final int FREE_TIME = 900;
}
