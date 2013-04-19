package ru.old;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 *  Абстрактный класс обеспечивающий доступ к мета-данным и потоку содержащему аудиокнигу.
 */
abstract class DataSource
{
	/**
	 * Возвращает MIME-тип (стандартно "audio/mp3") трека.  
	 * @return Строковое представление MIME-типа трека.
	 */
	public abstract String GetContentType();

	/**
	 * Создаёт и открывает канал для чтения данных.
	 * Creates and opens an input stream that returns the contents of the resource.
	 * This method must be implemented.
	 * @return An <code>InputStream</code> to access the resource.
	 * @throws IOException If the implementing class produces an error when opening the stream.
	 */
	public abstract ReadableByteChannel CreateReadChannel() throws IOException;

	/**
	 * Возвращает размер в байтах который будет передан.
	 * @return размер трека в байтах или -1 если размер определить нельзя.
	 */
	public long GetContentLength()
	{
		return -1;
	}
	
	/**
	 * 
	 * Возвращает размер трека в байтах.
	 * Если range == -1 или 0, то вернёт значение равное {@link #getContentLength()}
	 * @return размер в байтах или -1 если размер определить нельзя.
	 */
	public long GetLength()
	{
		return -1;
	}
	
	/**
	 * Возвращает номер байта с которого будет передаваться трек.
	 * Значение 0 передаётся если трек воспроизводится с начала.
	 * @return номер байта.
	 */
	public abstract long RangeFrom();
	
	public abstract long RangeTo();
	
}
