package com.audiobook;

import junit.framework.Assert;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

public class MyShop {

	private static MyShop   _instance;

	public boolean startWithBook(String bid, boolean isfree)
	{
		String devid = gs.s().deviceId();
		// make request
		HttpResponse response = gs.s().srvResponse(String.format("http://%s/buy.php?bid=%s&dev=%s&bt=1", gs.s().Host(), bid, devid));
		String responseString = gs.s().responseString(response);
		Assert.assertEquals(0, gs.s().handleSrvError(responseString));
		String fileName = gs.s().pathForBuy(bid);
		boolean success = gs.s().createFileAtPath(fileName, responseString);
		Assert.assertTrue(success);

		if(responseString.contains("yes"))
			return true;
		else
			return false;
	}
	
	private MyShop() {
	}

	public static MyShop s()
	{
		if (_instance == null)
		{
			_instance = new MyShop();
		}
		return _instance;
	}
}
