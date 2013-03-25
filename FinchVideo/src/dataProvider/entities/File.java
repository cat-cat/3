package dataProvider.entities;

/**
 * Класс описывающий файл.
 * @author mikalaj
 *
 */
public class File {
	/**
	 * Размер файла.
	 */
	public long size;
	/**
	 * Длина файла.
	 */
	public int length;
	/**
	 * Размер дорожки файла(битрейт).
	 */
	public int bitrate;
	/**
	 * Получение строкового представления.
	 */
	public String toString(){
		String st = new String();
		st+="File\n";
		st+="Length: "+String.valueOf(length)+"\n";
		st+="Size: "+String.valueOf(size)+"\n";
		st+="bitrate: "+String.valueOf(bitrate)+"\n";
		return st;
	}
	public void clearValues(){
		this.size = 0;
		this.length = 0;
		this.bitrate = 0;
	}
}
