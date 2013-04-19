package com.audiobook;

import java.util.ArrayList;
import java.util.List;

import com.android.vending.billing.util.IabHelper;
import com.android.vending.billing.util.IabResult;
import com.android.vending.billing.util.Inventory;
import com.android.vending.billing.util.Purchase;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class InfoActivity extends Activity {
	private void m(String msg)
	{
			// message box
			 Toast.makeText(getApplicationContext(),
			 msg,
			 Toast.LENGTH_LONG)
			 .show();
	}
 	
	@Override
	public void onDestroy() {
	   super.onDestroy();
	   if (mHelper != null) mHelper.dispose();
	   mHelper = null;
	}
	
	IabHelper mHelper;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_activity);
        

        // setup mHelper
 	   // compute your public key and store it in base64EncodedPublicKey
 	   mHelper = new IabHelper(this, gs.pk);
 	   mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
 		   public void onIabSetupFinished(IabResult result) {
 		      if (!result.isSuccess()) {
 		         // Oh noes, there was a problem.
 		         Log.e("MyTrace:", "Billing: Problem setting up In-app Billing: " + result);
 		         return;
 		      }
 		      
 		         // Hooray, IAB is fully set up!
 		      	m("IAB ready!");
 		   }
 		});

        
		// setup restore button
		((Button) findViewById(R.id.btn_restore))
		.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
					   public void onQueryInventoryFinished(IabResult result,
							      Inventory inventory) {

							      if (result.isFailure()) {
							        // handle error here
							    	Log.e("MyTrace:","**err getting purchases");   
							      }
							      else {
							        // does the user have the premium upgrade?
							        boolean ok = inventory.hasPurchase(gs.testProduct);
							        if(ok)
							        {
								        mHelper.consumeAsync(inventory.getPurchase(gs.testProduct), null);
								        // update UI accordingly
								        m("ready restoring!");
							        }
							      }
							   }
							});
			}
		});
        
    }

}
