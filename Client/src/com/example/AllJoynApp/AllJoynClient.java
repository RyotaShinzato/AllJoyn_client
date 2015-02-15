package com.example.AllJoynApp;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.client.R;

import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class AllJoynClient extends ActionBarActivity {

	static {
		System.loadLibrary("alljoyn_java");
	}
	
	BusHandler mBusHandler;
	private ArrayAdapter<String> mListViewArrayAdapter;
	private ListView mListView;
		
	private ProgressDialog mProgressDialog;
	
	IntentFilter intentFilter;
	Myservice_receiver receiver;
	Intent intent = null;
	
	private static final String TAG = "Client";
	private boolean beacon_found;
	
	private static final int START_CONNECT_PROGRESS = 1;
	private static final int STOP_PROGRESS= 2;
	private static final int MESSAGE_REPLY = 3;
	private static final int MESSAGE_PING = 4;
	private static final int START_SCAN_PROGRESS = 5;
	private static final int BEACON_NOT_FOUND = 6;
		
	
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch (msg.what){
			case START_CONNECT_PROGRESS:
				mProgressDialog = ProgressDialog.show(AllJoynClient.this,"","DISCOVERING",true,true);
				break;
			case STOP_PROGRESS:
				mProgressDialog.dismiss();
				break;
			case MESSAGE_REPLY:
				String rep = (String)msg.obj;
				Log.d(TAG,rep);
				mListViewArrayAdapter.add(rep);
				break;
			case MESSAGE_PING:
				String ping = (String)msg.obj;
				mListViewArrayAdapter.add(ping);
				break;
			case START_SCAN_PROGRESS:
				mProgressDialog = ProgressDialog.show(AllJoynClient.this,"","SCANNING",true,true);				
				break;
			case BEACON_NOT_FOUND:
				String message = "iBeacon not found";
				Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show();
			default:
				break;
			}
		}
	};
	
	
		
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG,"起動");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        startService(new Intent(getBaseContext(),AllJoynService.class));
                
        mListViewArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mListView = (ListView) findViewById(R.id.ListView);
        mListView.setAdapter(mListViewArrayAdapter);
        
        receiver = new Myservice_receiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction("MY_ACTION");
        registerReceiver(receiver,intentFilter);
                
        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        
        
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());
        
        
        //FINDボタン押された処理
        Button btn_find = (Button)findViewById(R.id.find);
        btn_find.setOnClickListener(new View.OnClickListener(){
        	@Override
        	public void onClick(View v){
        		Log.d(TAG,"btn_find clicked");
        		        		
        		//service切る
        		stopService(new Intent(getBaseContext(), AllJoynService.class));
        		//progressバー表示
        		mHandler.sendEmptyMessage(START_CONNECT_PROGRESS);
        		
        		//2待ってコネクト
        		mHandler.postDelayed(new Runnable(){
        			@Override
        			public void run(){
        				mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
        			}
        		},2000);
        	}
        });
        
        //SCANボタン押された処理
        Button btn_scan = (Button)findViewById(R.id.scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
        	@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(TAG,"btn_scan clicked");
				
				beacon_found = false;
				
				//ListViewクリア
				mListViewArrayAdapter.clear();
				
				//iBeaconスキャン
        		mBluetoothAdapter.startLeScan(mLeScanCallback);
        		mHandler.sendEmptyMessage(START_SCAN_PROGRESS);
        		
        		//2秒後にスキャン停止
        		mHandler.postDelayed(new Runnable(){
        			@Override
        			public void run(){
        				mBluetoothAdapter.stopLeScan(mLeScanCallback);;
        				mHandler.sendEmptyMessage(STOP_PROGRESS);
        				
        				//ビーコン発見できなかった処理
        				if(beacon_found == false){
        					String message = "iBeacon not found";
        					Message msg = mBusHandler.obtainMessage(BusHandler.PING,message);
        			        mBusHandler.sendMessage(msg);
        					mHandler.sendEmptyMessage(BEACON_NOT_FOUND);
        				}
        			}
        		}, 2000);
			}
		});
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    	Log.d(TAG,"ですとろーい");
    }
    
    public void startService(){
    	Log.d(TAG,"startService() called");
    	mHandler.postDelayed(new Runnable(){
    		@Override
    		public void run(){
    			startService(new Intent(getBaseContext(),AllJoynService.class));
    		}
    	}, 5000);
    }
    
    public void startConnect(){
    	Log.d(TAG,"startConnect()");
    	mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
    }
    
    class BusHandler extends Handler {
    	private static final String SERVICE_NAME = "org.alljoyn.bus.samples.simple";
    	private static final short CONTACT_PORT=42;
    	
    	private BusAttachment mBus;
    	private ProxyBusObject mProxyObj;
    	private SimpleInterface mSimpleInterface;
    	    	
    	private int mSessionId;
    	private boolean mIsInASession;
    	private boolean mIsConnected;
    	private boolean mIsStoppringDiscovery;
    	
    	public static final int CONNECT = 1;
    	public static final int JOIN_SESSION = 2;
    	public static final int DISCONNECT = 3;
    	public static final int PING = 4;
    	    	
    	   public BusHandler(Looper looper) {
    	      super(looper);
    	      
    	      mIsInASession = false;
    	      mIsConnected = false;
    	      mIsStoppringDiscovery = false;
    	   }

    	   @Override
    	   public void handleMessage(Message msg) {
    	      switch (msg.what) {
    	      case CONNECT:{
    	    	      	    	  
    	    	  org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
      	    	  
    	    	  mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);

    	    	  
    	    	  mBus.registerBusListener(new BusListener() {
    	    		  
    	    		  //つながったら呼ばれる
                      @Override
                      public void foundAdvertisedName(String name, short transport, String namePrefix) {
                      	Log.d(TAG,"foundadvertisedname呼ばれた");
                      	if(!mIsConnected) {
                      		Log.d(TAG,"mISConnected");
                      	    Message msg = obtainMessage(JOIN_SESSION);
                      	    msg.arg1 = transport;
                      	    msg.obj = name;
                      	    sendMessage(msg);
                      	}
                      }
                  });
    	    	      	    	  
    	    	  Status status = mBus.connect();
    	    	  Log.d(TAG,"connect: "+status);
    	    	  if(Status.OK != status){
    	    		  finish();
    	    		  return;
    	    	  }
    	    	  
    	    	  status = mBus.findAdvertisedName(SERVICE_NAME);
    	    	  Log.d(TAG,"findadvertisedname: "+status);
    	    	  if(Status.OK != status){
    	    		  finish();
    	    		  return;
    	    	  }
    	    	      	    	  
    	    	      	    	  
    	    	  break;
    	      }
    	      
    	      case JOIN_SESSION:{
    	    	  Log.d(TAG,"join_session呼ばれた");
    	    	  if(mIsStoppringDiscovery){
    	    		  Log.d(TAG,"stoppingdiscovery");
    	    		  break;
    	    	  }
    	    	  short contactPort = CONTACT_PORT;
    	    	  SessionOpts sessionOpts = new SessionOpts();
    	    	  sessionOpts.transports = (short)msg.arg1;
    	    	  sessionOpts.isMultipoint = true;
    	    	  Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
    	    	  
    	    	  Status status = mBus.joinSession((String) msg.obj, contactPort, sessionId, sessionOpts, new SessionListener(){
    	    		 @Override
    	    		 public void sessionLost(int sessionId, int reason){
    	    			 mIsConnected = false;
    	    		 }
    	    	  });
    	    	  Log.d(TAG,"joinnsesson: "+status);
    	    	  
    	    	  if(status == Status.OK){
    	    		  mProxyObj = mBus.getProxyBusObject(SERVICE_NAME, "/Service", sessionId.value, new Class<?>[]{ SimpleInterface.class});
    	    		  mSimpleInterface = mProxyObj.getInterface(SimpleInterface.class);

    	    		  mSessionId = sessionId.value;
    	    		  mIsConnected = true;
    	    		  mHandler.sendEmptyMessage(STOP_PROGRESS);
    	    	  }
    	    	  break;
    	      }
    	      
    	      case DISCONNECT:{
    	    	  mIsStoppringDiscovery = true;
    	    	  if(mIsConnected){
    	    		  //Status status =  mBus.leaveSession(mSessionId);
    	    		  Log.d(TAG,"leavesession");
    	    	  }
    	    	  mBus.disconnect();
    	    	  getLooper().quit();
    	    	  break;
    	      }
    	      case PING:{
    	    	  try{
    	    		  if(mSimpleInterface != null){
    	    			  mSimpleInterface.Ping((String) msg.obj);
    	    			  //mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_REPLY,reply));
    	    			  startService();
    	    			  mIsConnected = false;
    	    		  }
    	    	  }catch(BusException ex){
    	    		  Log.d(TAG,"exception "+ex);
    	    	  }
    	    	  break;
    	      }
    	      default:
    	         break;
    	      }
    	   }
    }
    
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			// TODO Auto-generated method stub
						
			Log.d(TAG,"onLeScanよばれた");
			beacon_found = true;
			
			if(scanRecord.length > 30)
		    {
		        //iBeacon の場合 6 byte 目から、 9 byte 目はこの値に固定されている。
		        if((scanRecord[5] == (byte)0x4c) && (scanRecord[6] == (byte)0x00) &&
		        (scanRecord[7] == (byte)0x02) && (scanRecord[8] == (byte)0x15))
		        {
		            String uuid = IntToHex2(scanRecord[9] & 0xff) 
		                        + IntToHex2(scanRecord[10] & 0xff)
		                        + IntToHex2(scanRecord[11] & 0xff)
		                        + IntToHex2(scanRecord[12] & 0xff)
		                        + "-"
		                        + IntToHex2(scanRecord[13] & 0xff)
		                        + IntToHex2(scanRecord[14] & 0xff)
		                        + "-"
		                        + IntToHex2(scanRecord[15] & 0xff)
		                        + IntToHex2(scanRecord[16] & 0xff)
		                        + "-"
		                        + IntToHex2(scanRecord[17] & 0xff)
		                        + IntToHex2(scanRecord[18] & 0xff)
		                        + "-"
		                        + IntToHex2(scanRecord[19] & 0xff)
		                        + IntToHex2(scanRecord[20] & 0xff)
		                        + IntToHex2(scanRecord[21] & 0xff)
		                        + IntToHex2(scanRecord[22] & 0xff)
		                        + IntToHex2(scanRecord[23] & 0xff)
		                        + IntToHex2(scanRecord[24] & 0xff);
		 
		            String major = IntToHex2(scanRecord[25] & 0xff) + IntToHex2(scanRecord[26] & 0xff);
		            String minor = IntToHex2(scanRecord[27] & 0xff) + IntToHex2(scanRecord[28] & 0xff);
		            String message = "UUID: " + uuid + "\n RSSI: " + String.valueOf(rssi);
		            Log.d(TAG,"UUID: "+uuid + "RSSI: "+ rssi);
		            Message msg = mBusHandler.obtainMessage(BusHandler.PING,message);
		            mBusHandler.sendMessage(msg);
		            mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_REPLY,message));
		        }
		    }
		    
		}
		
		private String IntToHex2(int i) {
			char hex_2[] = {Character.forDigit((i>>4) & 0x0f,16),Character.forDigit(i&0x0f, 16)};
		    String hex_2_str = new String(hex_2);
		    return hex_2_str.toUpperCase();
		}
	};
	
	public class Myservice_receiver extends BroadcastReceiver{
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.d("Myserive_receiver","onReceive");
			
			Bundle bundle = intent.getExtras();
			String message = bundle.getString("message");
			String not_found = "iBeacon not found";
			
			if(message.equals(not_found)){
				mHandler.sendEmptyMessage(BEACON_NOT_FOUND);
			}
			Log.d("My_serive_receiver",message);
			mListViewArrayAdapter.clear();
			mListViewArrayAdapter.add(message);
		}
	}
}
