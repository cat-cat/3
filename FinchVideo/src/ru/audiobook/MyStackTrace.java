package ru.audiobook;

public final class MyStackTrace {
	public static String func3()
	{
		String out = Thread.currentThread().getStackTrace()[3].getLineNumber()+":"+Thread.currentThread().getStackTrace()[3].getMethodName();
		return out;
	}
}
