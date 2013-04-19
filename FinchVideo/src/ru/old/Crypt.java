package ru.old;

//import ru.old.MyApp;

public class Crypt
{
	static String deviceId = "a9f094672e47283b6fc7af77ddef829b";
	public static byte[] GenerateKey(boolean useDefault)
	{
		if(useDefault)
			return "1qazXSW@3edc".getBytes();
		else
		{
			byte[] key = deviceId.getBytes();
			final int length = key.length - 1;
			final int size = (length + 1) / 2;
			byte tmp = 0;
			for(int i = 0; i < size; i++ )
			{
				tmp = key[i];
				key[i] = key[length - i];
				key[length - i] = tmp;
			}
			return key;
		}
	}
	
	public static byte[] GenerateKey(byte type)
	{
		return GenerateKey(type == 0);
	}
	
	public static byte[] GenerateRekey()
	{
		byte[] mask = "1qazXSW@3edc".getBytes();
		final short maskSize = (short) (mask.length - 2);
		short maskIndex = 0;
		
		byte[] key = deviceId.getBytes();
		final int length = key.length - 1;
		final int size = (length + 1) / 2;
		byte tmp = 0;
		for(int i = 0; i < size; i++ )
		{
			tmp = key[i];
			key[i] = key[length - i];
			key[length - i] = tmp;
		}
		
		for(int i = 0; i <= length; i++)
		{
			key[i] = (byte) (key[i] ^ mask[maskIndex]);
			if(maskIndex < maskSize)
				maskIndex++;
			else
				maskIndex = 0;
		}
		return key;
	}
}
