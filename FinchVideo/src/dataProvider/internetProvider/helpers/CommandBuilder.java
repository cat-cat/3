package dataProvider.internetProvider.helpers;

import java.net.URLEncoder;

//import ru.old.Debug;

/**
 * 
 * @author mikalaj
 *	Класс, ответственный за скрытие реальных команд от пользователя, 
 *  и за их построение.
 */
public class CommandBuilder {
//	private String Tag = getClass().getName();
	/**
	 * Хост, к которому подключаемся.
	 */
//	private static final String HOST_TEST = "http://test.librofon.ru/service/";
	private static final String HOST_PRODUCTION = "http://librofon.ru/service/";
	/**
	 * Изменяемая строка, которая хранит текущую команду.
	 */
	private StringBuffer currentCommand;
	/**
	 * Булево свойство, характеризующее установлена ли команда.
	 */
	private boolean commandSetted;
	/**
	 * свойство хранящее количество параметров.
	 */
	private int paramNumber;
	/**
	 * Инициализация.
	 */
	public CommandBuilder(){
		currentCommand = new StringBuffer();
		resetBuilder();
	}
	/**
	 * Метод, удаляющий добавленные команды и параметры.
	 */
	public void resetBuilder(){
		if(currentCommand.length()!=0)
			currentCommand.delete(0, currentCommand.length()-1);
//		if ( Debug.TEST_MODE )
//			currentCommand.append(HOST_TEST);
//		else
			currentCommand.append(HOST_PRODUCTION);
		commandSetted = false;
		paramNumber = 0;
	}
	/**
	 * Метод добавляющий команду.
	 * @param command Строка команды.
	 */
	public void AddCommand(String command){
		if(!commandSetted){
			currentCommand.append(command);
			currentCommand.append("/?params[]=");
			commandSetted = true;
		}
	}
	/**
	 * Метод добавляющий параметр.
	 * @param param параметр.
	 */
	public void addParam(String param){
		if(paramNumber!=0)
			currentCommand.append("&params[]=");
		param = URLEncoder.encode(param);
		currentCommand.append(param);
		paramNumber++;
	}
	/**
	 * Метод возвращающий текущую команду.
	 * @return Строка команды.
	 */
	public String GetCommand(){
//		Log.d("CommandBuilder", "Created command is "+currentCommand.toString());
		return currentCommand.toString();
	}	
}
