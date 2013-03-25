package dataProvider.internetProvider.helpers;

/**
 * Класс содержит константы для упрощения формирования команд.
 * @author mikalaj
 */
public final class Commands {
	public static final String Connect = "connect";
	/**
	 * параметр - строка запроса query.
	 */
	public static final String getAbookCatalog = "getAbookCatalog";
	/**
	 * строка, позволяющая получить обновление списка аудиокниг.
	 */
	public static final String getAbookCatalogUpdate = "getAbookCatalogUpdate";
	/**
	 * строка, позволяющая узнать дату последнего обновления сервера.
	 */
	public static final String hasUpdate = "hasUpdate2";
	/**
	 * параметр - ид книги.
	 */
	public static final String getAbookById = "getAbookById";
	/**
	 * параметр - ид книги.
	 */
	public static final String getAbookPaymentHistory = "getAbookPaymentHistory";
	
	public static final String getAbookFreeDownloadHistory = "getAbookFreeDownloadHistory";
	
	/**
	 * параметр - ид книги.
	 */
	public static final String getAbookImage = "getAbookImage";
	/**
	 * параметр - ид книги.
	 */
	public static final String getAbookFreeAudio = "getAbookFreeAudio";
	/**
	 * параметр - ид книги..
	 */
	public static final String getAbookFreeText= "getAbookFreeText";
	/**
	 * параметр - ид книги.
	 * параметр - ид трека.
	 */
	public static final String getAbookAudioByTrack = "getAbookAudioByTrack";
	/**
	 * параметр - ид книги.
	 * параметр - ид трека.
	 */
	public static final String getAbookTextByTrack = "getAbookTextByTrack";
	public static final String getAbookText = "getAbookText";
	/**
	 * параметр - ид книги.
	 */
	//public static final String getAbookText = "getAbookText";
	/**
	 *ID аудиокниги (необязательный), 
	 *Начало временного периода (формат - «yyyy-mm-dd», необязательный),
	 *Окончание временного периода (формат - «yyyy-mm-dd», необязательный)
	 */
	public static final String getDownloadHistory = "getDownloadHistory";
	/**
	 * параметр - ид книги.
	 */
	public static final String getFreeAccessInfo = "getFreeAccessInfo";
	/**
	 * Строка, позволяющая получить список рекомендованных книг от сервера.
	 */
	public static final String getAbookRecommend = "getAbookRecommend";
	/**
	 * Строка, позволяющая получить список хит-книг от сервера.
	 */
	public static final String getAbookHit = "getAbookHit";
	/** 
	 * Метод осуществляющий проверку подписи транзации.
	 * В него необходимо передать два параметра: hash и response.
	 */
	public static final String VerifyPurchaseSignature = "orderAndroid";
	public static final String VerifyPurchaseSignatureTest = "orderAndroidTest";
	
	public static final String setPushAndroidDeviceToken = "setPushAndroidDeviceToken";
}
