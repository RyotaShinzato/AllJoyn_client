package com.example.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Myservice_receiver extends BroadcastReceiver{

	MainActivity activity;
	
	private ArrayAdapter<String> mListViewArrayAdapter;
	private ListView mListView;
	
	@Override
	public void onReceive(Context context,Intent intent){
		Log.d("Myserive_receiver","onReceive");
		
		Bundle bundle = intent.getExtras();
		String message = bundle.getString("message");
		Log.d("My_serive_receiver",message);
		
		
	}
}
