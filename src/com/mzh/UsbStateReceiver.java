package com.mzh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class UsbStateReceiver extends BroadcastReceiver {

	private static final String TAG = "UsbStateReceiver";  
    public static final int USB_STATE_MSG = 0x00020;  
    public static final int USB_STATE_ON = 0x00021;  
    public static final int USB_STATE_OFF = 0x00022;  
    private Context mContext;
    public UsbStateReceiver(Context context) {  
    	mContext = context;  
    }
    
    public void registerReceiver() {  
        IntentFilter filter = new IntentFilter();  
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);  
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);  
        filter.addAction(Intent.ACTION_MEDIA_EJECT);  
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addDataScheme("file");  
        this.mContext.registerReceiver(this, filter);
    }
    
    public void unRegisterReceiver() {
        this.mContext.unregisterReceiver(this);  
    }
    
	@Override
	public void onReceive(Context arg0, Intent arg1) {
	    Log.v(TAG,"usb action = "+arg1.getAction());  
	    int state = 0;
	    if( arg1.getAction().equals(Intent.ACTION_MEDIA_MOUNTED ) ||  
	            arg1.getAction().equals(Intent.ACTION_MEDIA_CHECKING)){  
	    	state = USB_STATE_ON;  
	    }else{  
	    	state = USB_STATE_OFF;  
	    }  
	    Log.d("mzh", "state:"+state + " uri:"+arg1.getData());

	}

}
