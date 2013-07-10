package com.audiobook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import ru.old.Crypt;

public class MyFileInputStream extends FileInputStream {

	int rangeFrom = 0;
	
	@Override
	public
	long 	skip(long byteCount)
	{
		long d = 0;
		try {
			d = super.skip(byteCount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		rangeFrom += d;
		
		return d;
	}
	
	@Override
	public
	int read(byte[] buff, int byteOffset, int byteCount)
	{
		int readBytes = 0;
		try {
			readBytes = super.read(buff, byteOffset, byteCount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		// decrypt stream
//		File dir = Environment.getExternalStorageDirectory();
//
//	     FileInputStream fis = new FileInputStream(new File(dir,"003.3gp"));
//
//	                        byte[] bya=read(new File(dir,"003.3gp"));
		
		byte myKey = 1;
		final byte[] key = Crypt.GenerateKey(myKey);;
	    final short keyLength = (short) (key.length - ((myKey == 0) ? 2 : 1));
	    short keyIndex = (short) (rangeFrom  % (keyLength + 1));

    	for(int i = 0; i < readBytes; i++)
    	{
    		buff[i] = (byte) (buff[i] ^ key[keyIndex]);
    		if(keyIndex < keyLength)
	        	keyIndex++;
	        else
	        	keyIndex = 0;
    	}


		
		
		
		
		return readBytes;
	}
	
	public MyFileInputStream(File file) throws FileNotFoundException {
		super(file);
		// TODO Auto-generated constructor stub
	}

}
