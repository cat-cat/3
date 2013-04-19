package ru.old;

//import dataProvider.dbProvider.entities.Book;
//import dataProvider.dbProvider.entities.CollectionInfo;
//import dataProvider.entities.BookInfo;
//import dataProvider.entities.FullInfo;
//import ru.old.download.DownloadManager;
//import ru.old.download.DownloadService;

/**
 * Содержит коды всех возможных ошибок возникающих в приложении.
 * Используется как параметр для создания {@link ErrorDialog}.
 */
public enum Errors
{
	/** 
	 * Никакой ошибки нет.
	 */
	NO,
	
	/**
	 * Показывает что ошибка произошла, но не известно какая именно.
	 */
	UNDEFINED,

	/** Добавление в коллекцию уже купленной книги */ 
	DUPLICATED,
	
	/** 
	 * Ошибка которая не может возникнуть, потому что не может возникнуть никогда.
	 * Желательно чтобы в релизе таких ошибок не было.
	 */
	MYSTICAL,
	
	/** Ошибка при попытке расшифровать файл */
	LEFT_CRYPT_KEY,
	
	// Ошибки плеера
	/** Отсутствует текст */
	PLAYER_NO_TEXT,
	/** Отсутствует аудио */
	PLAYER_NO_AUDIO,
	/** Отсутствует время для бесплатного прослушивания */
	PLAYER_NO_FREE_TIME,
	
	//Внутренниие ошибки, сигнализирующие что что-по пошло не так.
	//При нормалбном ходе работы проявляться не должны.
	/** Не удалось получить информацию о книге. Объекты {@link Book}, {@link BookInfo}, {@link CollectionInfo}, {@link FullInfo} и т.д.   */
	INTERN_CANT_GET_BOOK, 
	/** Не удалось получить информацию о треках книги (не была загружена полная информация о книге). 
	 * Появляется в {@link ru.old.collection.Player.LoadBookInformation}, когда соршена отложенная покупка,
	 * а информации о книге не загружена.
	 */
	INTERN_CANT_GET_TRACKS,
	
	//Ошибки бизнес-логики
	/** Попытка получить в прокат больше чем три книги */
	RULE_MORE_THEN_THREE_FREE_BOOK,
	/** Попытка скачать следующий бесплатный отрывок книги.
	 * Или попытка скачать больше трёх бесплатных отывков в день */
	RULE_ONE_FREE_TRACK_FOR_BOOK_PER_DAY,
	
	RULE_FREE_LOADING_LIMIT,
	
//	RULE_MORE_THAT_USER_CAN_DOWNLOAD_PER_DAY,
	
	//Ошибки связанные с ExternalStorage
	/** Отсутствует доступ к ExternalStorage. */
	EXTERNAL_STORAGE_NOT_AVAILABLE,
	/** ExternalStorage не доступен для записи. */
	EXTERNAL_STORAGE_CANT_WRITE, 
	/** Для записи файла необходимо больше места на ExternalStorage. */
	EXTERNAL_STORAGE_NEED_MORE_SPACE, 
	
	//Ошибки связанные c работой с файлами
	/** Отсутствует доступ к файлу. */
	FILE_CANT_ACCESS,
//	/** Невозможно создать файл. */
	FILE_CANT_CREATE,
	/** Файл не найден. Когда открывается без проверки на существование. */
	FILE_NOT_FOUND,
	/** Ошибка создания канала для чтения/записи в файл. */
	FILE_CANT_OPEN_CHANNEL,
	/** Ошибка чтения из потока связанного с файлом. */
	FILE_CHANEL_READ_ERROR,
	/** Ошибка записи в поток связанны с файлом. */
	FILE_CHANEL_WRITE_ERROR,
	
	//Ошибки связанные с Интернет
	/** Нет доступа к Интернет. */
	INTERNET_NOT_AVAILABLE,
	/** Не правильно сформированный URL для закачки (Пользователю показывать нельзя, так как это внутреняя ошибка приложения). */
	INTERNER_BAD_URL, 
	/** Нельзя получить доступ к серверу */
	INTERNET_SERVER_NOT_AVAILABLE, 
	/** Ошибка чтения ответа сервера. Впринципе аналогична {@link #INTERNET_SERVER_NOT_AVAILABLE}. */
	INTERNET_CANT_READ,
	/** Ошибка когда сервер по запросу не отдаёт текстовую часть книги. То есть адрес правильный, а по нему ничего не лежит. */
	INTERNET_SERVER_DONT_RETURN_TEXT_CONTENT,
	/** Ошибка когда сервер по запросу не отдаёт аудио часть трека. То есть адрес правильный, а по нему ничего не лежит. */
	INTERNET_SERVER_DONT_RETURN_AUDIO_CONTENT,
	/** Ошибка взаимодействия с сервером */
	INTERNET_OPERATION_ERROR,
	
	//Ошибки связанные с внутренней базой данных.
	/** К базе данных отсутствует доступ. */
	DB_ACCESS_ERROR,
	/** Произошла ошибка при каком-либо(insert, update, delete) действии с бд. */
	DB_ACTION_ERROR,
	
	THREAD_ERROR,
	THREAD_STOP_ERROR,
	
	//Ошибки сервиса закачки
	/** Ошибка подключения к сервису {@link DownloadService} - не удалось получить менеджер {@link DownloadManager}. */
	DOWNLOAD_CANT_GET_MANAGER,
	/** Ошибка возникающая когда невозможно начать загрузку трека. */
	DOWNLOAD_CANT_START_TRACK_LOADING,
	/** Ошибка возникающая когда невозможно остановить загрузку трека. */
	DOWNLOAD_CANT_STOP_TRACK_LOADING
	;
	
	
	/**
	 * Преобразования из числа в Errors.
	 * Если переданный индекс несоответствует количеству элементов то резульатом будет {@link #MYSTICAL}
	 * @param index числовой номер ошибки.
	 * @return мнемоническое представление ошибки.
	 */
	public static Errors valueOf(int index)
    {
		Errors[] values = Errors.values();
        if (index < 0 || index >= values.length)
        {
            return MYSTICAL;
        }
        return values[index];
    }
}
