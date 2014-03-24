package ru.old;

public class Const
{
	public static final String PREF_KEY_PLAYER_BOOK = "player_book";
	public static final String PREF_KEY_PLAYER_TRACK = "player_track";
	
	public static final String PREF_KEY_LAST_ACCESS = "last_open";
	public static final String PREF_KEY_LAST_ACTIVITY = "last_activity";
	
	public static final String PREF_BOOK_ID = "book_id";
	
	public static final String PREF_OPEN_CORE_FRAMEWORK = "open_core";
	
	/** Идентификационный номер устройства */
	public static final String PREF_DEVICE_ID = "device_id";
	
	public static final String C2DM_SENDER_ID = "cdcomdistribution@gmail.com";
	/** Регистрационный id для c2dm */
	public static final String PREF_C2DM_REGISTRATION_ID = "registration_id";
	/** Зарегистрирован id на сервере librofon или нет */
	public static final String PREF_C2DM_REGISTRED_ON_LIBROFON = "c2dm_on_server";
	
	/** Отключение воспроизведения книги, когда приложение сворачивается.*/
	public static final String PREF_SWITCH_OFF_BOOK_DISPLACE = "switch_off_book_displace";
	/** Отключение плеера если наушники отключаются */
	public static final String PREF_STOP_ABOOK_HEADPHONE = "stop_abook_head_phon";
	/** Загрузка осуществляется только по wifi */
	public static final String PREF_WIFI_ONLY_DOWNLOAD = "downlad_book_wifi";
	
	/** Дата последней загрузки/обновления каталога */
	public static final String PREF_LAST_UPDATE = "lastUpdate";
	/** Количество книг полученных с последним обновлением */
	public static final String PREF_LAST_UPDATE_NEW_BOOKS = "lastUpdateNewBooks";
	
	/**
	 * Проверка ошибки обновления для версии 1.0.4
	 * В версии 1.0.3 при обновлении могли дублироваться авторы/жанры/издательства/чтецы
	 * для книги.
	 */
	public static final String NEED_CHECK_UPDATE_FAIL_104 = "check_update_fail_104";
	
	/** Время последней попытки обновления в миллисекундах */
	public static final String PREF_LAST_UPDATE_TRY = "last_update_try";
	//public static final long UPDATE_PERIOD = 2 * 60 * 60 * 1000L;
	public static final long UPDATE_PERIOD = 30 * 60 * 1000L;
	
	/** Час в секундах */
	public static final int HOUR = 60 * 60;
	/** Сутки в миллисекундах */
	public static final long RefreshTimeForDownloadFree = 24 * 60 * 60 * 1000;
	
	/** Показать плеер */
	public static final String SHOW_PLAYER = "ru.old.SHOW_PLAYER";
	
	/** Максимальное количество бесплатных открывком, которое можно скачать в день. */
//	public static final byte MAX_FREE_DOWNLOADS_PER_DAY = 3; 
	
	
	public static final int NOTIFICATION_C2DM = 2;

	/** Коэффициент используемы при переводе pt в sp */
	public static final float FONT_PT_TO_SP_COFFICIENT = 2.38f;
	
	public static final float FONT_PT_TO_MM_COFFICIENT = 0.357f;
	
	/** Длительность бесплатной части в секундах */
	public static final String PREF_FREE_PART_DURATION = "free_part_duration";
	/** Стандартная длительность бесплатной части в секундах  */ 
	public static final int VALUE_FREE_PART_DURATION = 900; 
	
	/** Время (в часах) ожидания для загрузки следующей бесплатной части. */
	public static final String PREF_FREE_PART_PERIOD = "free_part_pediod";
	/** Стандартный период ожидания (в часах). */ 
	public static final int VALUE_FREE_PART_PERIOD = 24; 
	
	public static final String PREF_KEY_DONT_NEED_DOWNLOAD_NUM_FREE_PARTS = "d_n_d_n_f_p";
	
	public static final String PREF_LAST_LOAD_RECOMMENDED = "last_recommended";
	public static final long RECOMMENDED_RELOAD_PERIOD = 10 * 60 * 1000L;
}
