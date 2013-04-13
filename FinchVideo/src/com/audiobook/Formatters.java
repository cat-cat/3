package com.audiobook;

import android.text.Html;

/**
 * Преобразуют информацию в String для отображения в интерфейсе.
 */
public class Formatters
{
	/**
	 * Преобразует секунды в "чч:мм:сс", причём если один из блоков отсутствует
	 * то он не отображается.
	 * @param seconds
	 * @return
	 */
	public static String Time(int seconds)
	{
//		StringBuffer sb = new StringBuffer(5);
//		int minutes = seconds / 60;
//		if(minutes < 10)
//			sb.append("0");	
//		sb.append(Integer.toString(minutes));
//		sb.append(":");
//		seconds = seconds - minutes * 60;
//		if(seconds < 10)
//			sb.append("0");
//		sb.append(Integer.toString(seconds));
//		return sb.toString();
		
		StringBuilder sb = new StringBuilder(9);
		int hours = seconds / 3600;
		if(hours > 0)
		{
			if(hours < 10)
				sb.append("0");
			sb.append(Integer.toString(hours) + ":");
			seconds = seconds - hours * 3600;
		}
		int minutes = seconds / 60; 
		if(minutes < 10)
			sb.append("0");
		sb.append(Integer.toString(minutes) + ":");
		seconds = seconds - minutes * 60;
		if(seconds < 10)
			sb.append("0");
		sb.append(Integer.toString(seconds));
		return sb.toString();
	}
	
	/**
	 * Время которое осталось до возможности скачать следущий бесплатный трек.
	 * @param seconds
	 * @return время в формате "чч часов мм минут"
	 */
	public static String TimeToLoadNextFreeChapter(int seconds)
	{
		StringBuilder sb = new StringBuilder(20);
		int hours = seconds / 3600;
		if(hours > 0)
		{
			sb.append(hours);
			if(hours > 10 && hours < 20)
				sb.append(" часов ");
			else
				switch(hours % 10)
				{
					case 1:
						sb.append(" час ");
						break;
					case 2: case 3: case 4:
						sb.append(" часа ");
						break;
					case 5: case 6: case 7: case 8: case 9: case 0:
						sb.append(" часов ");
						break;
				}
			seconds = seconds - hours * 3600;
		}
		int minutes = seconds / 60;
		if(minutes > 0)
		{
			sb.append(minutes);
			if(minutes > 10 && minutes < 20)
				sb.append(" минут");
			else
				switch(minutes % 10)
				{
					case 1:
						sb.append(" минута");
						break;
					case 2: case 3: case 4:
						sb.append(" минуты");
						break;
					case 5: case 6: case 7: case 8: case 9: case 0:
						sb.append(" минут");
						break;
				}
		}
		return sb.toString();
	}
	
	public static String Format(String text)
	{
		return Html.fromHtml(text.replace("\n", "<br/>")).toString();
	}
}
