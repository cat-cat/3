package dataProvider.internetProvider.helpers;

public final class ConnectionErrorCodes
{
	public static final int NO_INTERNET = 2001;
	
	/** Истекло время жизни сессии, необходимо вызвать метод connect */
	public static final int WRONG_SESSION = 1001;
	
	/** Превышен лимит бесплатных скачиваний на текущий день */
	public static final int CANT_LOAD_MORE_FREE_PARTS = 1002;
	
	/** Нет данных для ответа по запрашиваемому методу и параметрам.  */
	public static final int HAVENT_DATA_FOR_REQUEST = 1003;
}
