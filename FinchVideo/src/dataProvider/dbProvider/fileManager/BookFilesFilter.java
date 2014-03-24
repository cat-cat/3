package dataProvider.dbProvider.fileManager;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Фильтр для определения файлов относящихся к книге. 
 *
 */
class BookFilesFilter implements FilenameFilter
{
	/**
	 * Префикс по которому будет производиться поиск файлов.
	 */
	private String prefix;
	
	/**
	 * Создание фильтра для поиска файлов относящихся к книге.
	 * @param bookId Идентификационный номер книги.
	 */
	public BookFilesFilter(String bookId)
	{
		prefix = bookId + ".";
	}
	
	@Override
	public boolean accept(File dir, String filename)
	{
		return filename.startsWith(prefix);
	}
}
