package ru.old;


/**
 * Получает все сообщения от загрузок.
 *
 */
public interface IManagerObserver
{
	/** Возникает при возникновении ошибки при закачке. 
	 * @param bookID идентификационный номер книги
	 * @param trackID иденитификационны номер трека
	 * @param error тип ошибки
	 */
	void onError(String bookID, String trackID, Errors error);
	
	/** Возникает при начале загрузки (открыто соединение с сервером и файл для записи). 
	 * @param bookID идентификационный номер книги
	 * @param trackID иденитификационны номер трека 
	 */
	void onStart(String bookID, String trackID);
	
	/** Возникает при изменении прогресса закачки.
	 * @param bookID идентификационный номер книги
	 * @param trackID иденитификационны номер трека
	 * @param progress процент выполнения
	 */
	void onProgressChanged(String bookID, String trackID, int progress);
	
	/**
	 * Вызывается когда завершается текст для трека загружен полностью. 
	 * @param bookID идентификационный номер книги
	 * @param trackID иденитификационны номер трека
	 */
	void onTextLoadComplete(String bookID, String trackID);
	
	/** Вызывается при остановки закачки по любой причине.
	 * @param bookID идентификационный номер книги
	 * @param trackID иденитификационны номер трека
	 * @param isComplete true если закачка завершена, иначе false  
	 */
	void onStop(String bookID, String trackID, boolean isComplete);
	
	/**
	 * Вызывается при добавлении всех треков книги в список загрузки.
	 * @param bookID идентификационный номер книги.
	 * @param trackIDs список добавленых треков.
	 * Если треки не были найдены по какой-либо причине, то вернёт <code>NULL</code>.
	 */
	void onBookLoaded(String bookID, String[] trackIDs);
	
	/**
	 * Вызывается при приостановке загрузки трека.
	 * @param bookID идентификационный номер книги.
	 * @param trackIDs список добавленых треков.
	 */
	void onPause(String bookID, String trackID);

}
