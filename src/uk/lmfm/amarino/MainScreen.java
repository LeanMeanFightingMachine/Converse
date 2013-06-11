/*
  Amarino - A prototyping software toolkit for Android and Arduino
  Copyright (c) 2010 Bonifaz Kaufmann.  All right reserved.
  
  This application and its library is free software; you can redistribute
  it and/or modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package uk.lmfm.amarino;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import at.abraxas.amarino.AmarinoIntent;
import uk.lmfm.amarino.Globals;
import uk.lmfm.amarino.log.Logger;
import uk.lmfm.converse.ConverseMapActivity;

/**
 * 
 * @author Bonifaz Kaufmann
 *
 * $Id: MainScreen.java 444 2010-06-10 13:11:59Z abraxas $
 */
public class MainScreen extends Activity {
	
	private static final String TAG = "ConverseMainScreen";
	
	// Shoes interface
	private ShoeManager shoeManager;
	
	// UI Elements
	private Button connectButton;
	private TextView introTextView;
	private ProgressBar connectLoader;
	private ImageView successImageView;
	
	boolean isBound = false;
	MyHandler handler = new MyHandler();
	
	BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			if (action == null) return;
			Logger.d(TAG, action + " received");
			
			if (AmarinoIntent.ACTION_CONNECTED_DEVICES.equals(action)){
				
				updateDeviceStates(intent.getStringArrayExtra(AmarinoIntent.EXTRA_CONNECTED_DEVICE_ADDRESSES));
				return;
			}
			
			final String address = intent.getStringExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS);
			if (address == null) return;

			Message msg = new Message();
			
			if (AmarinoIntent.ACTION_CONNECTED.equals(action)){
				msg.what = MyHandler.CONNECTED;
			}
			else if (AmarinoIntent.ACTION_DISCONNECTED.equals(action)){
				msg.what = MyHandler.DISCONNECTED;
			}
			else if (AmarinoIntent.ACTION_CONNECTION_FAILED.equals(action)){
				msg.what = MyHandler.CONNECTION_FAILED;
			}
			else if (AmarinoIntent.ACTION_PAIRING_REQUESTED.equals(action)){
				msg.what = MyHandler.PAIRING_REQUESTED;
			}
			else return;
			
			msg.obj = address;
			handler.sendMessage(msg);
		}
	}; 
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(TAG, "onCreate");
        setContentView(R.layout.main);
        
        // UI
        //getWindow().setBackgroundDrawableResource(R.drawable.walking);
        
        // Shoes interface
        shoeManager = new ShoeManager(this);
        
        // View elements
        connectButton = (Button) findViewById(R.id.button1);
        introTextView = (TextView) findViewById(R.id.textView1);
        connectLoader = (ProgressBar) findViewById(R.id.progressBar1);
        connectLoader.setVisibility(ProgressBar.INVISIBLE);
        successImageView = (ImageView) findViewById(R.id.imageView2);
        successImageView.setVisibility(ImageView.INVISIBLE);
        
        connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				// onClick
				
				if ( connectButton.getText().equals(getString(R.string.connect)) )
				{
					// Connect shoes
					connectShoe(shoeManager.getLeftShoe().address);
					connectShoe(shoeManager.getRightShoe().address);
					// UI
					connectButton.setEnabled(false);
					connectButton.setText(R.string.connecting);
					introTextView.setText(R.string.mainscreen_description_short);
					connectLoader.setVisibility(ProgressBar.VISIBLE);
					
				} else if ( connectButton.getText().equals(getString(R.string.disconnect)) )
				{	
					// Connect shoes
					disconnectShoe(shoeManager.getLeftShoe().address);
					disconnectShoe(shoeManager.getRightShoe().address);					
					// UI
					connectButton.setText(R.string.disconnecting);
					
				} else if ( connectButton.getText().equals(getString(R.string.gomap)) ) {
					// Load map
					Logger.d(TAG, "Read for map");
					
					Globals g = Globals.getInstance();
					g.setShoeManager(shoeManager);
					Intent in = new Intent(getApplicationContext(), ConverseMapActivity.class);
					startActivity(in);
					
					//Amarino.sendDataToArduino(MainScreen.this, shoeManager.getLeftShoe().address, 'A', "200,200,");
				}
			}
		});
    }
    
    private void connectShoe(String address) {
		Intent i = new Intent(MainScreen.this, AmarinoService.class);
		i.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address);
		i.setAction(AmarinoIntent.ACTION_CONNECT);
		startService(i);
	}
    
    private void disconnectShoe(String address) {
		Intent i = new Intent(MainScreen.this, AmarinoService.class);
		i.putExtra(AmarinoIntent.EXTRA_DEVICE_ADDRESS, address);
		i.setAction(AmarinoIntent.ACTION_DISCONNECT);
		startService(i);
	}
    
    private void disconnectAllShoes() {
    	disconnectShoe(shoeManager.getLeftShoe().address);
		disconnectShoe(shoeManager.getRightShoe().address);
    }
    
    @Override
	protected void onStart() {
		super.onStart();
	}
    
    @Override
	protected void onResume() {
		super.onResume();
		Logger.d(TAG, "onResume");
		// listen for device state changes
		IntentFilter intentFilter = new IntentFilter(AmarinoIntent.ACTION_CONNECTED_DEVICES);
		//intentFilter.addAction(AmarinoIntent.ACTION_CONNECTED);
	    //intentFilter.addAction(AmarinoIntent.ACTION_DISCONNECTED);
	    intentFilter.addAction(AmarinoIntent.ACTION_CONNECTION_FAILED);
	    intentFilter.addAction(AmarinoIntent.ACTION_PAIRING_REQUESTED);
	    registerReceiver(receiver, intentFilter);
	    
	    // Disconnect shoes
	    disconnectAllShoes();
	    
	    // request state of devices
	    Intent intent = new Intent(this, AmarinoService.class);
		intent.setAction(AmarinoIntent.ACTION_GET_CONNECTED_DEVICES);
		startService(intent);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		disconnectAllShoes();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
	}
	
	private void updateDeviceStates(String[] connectedDevices){
		if (connectedDevices == null) {
			Logger.d(TAG, "no connected devices");
			for (BTDevice device: shoeManager.getShoesDevices())
			{
				Message msg = new Message();
				msg.what = MyHandler.DISCONNECTED;
				msg.obj = device.address;
				handler.sendMessage(msg);
			}
			
			connectButton.setEnabled(true);
			connectButton.setText(R.string.connect);
			
			return;
		}

		Logger.d(TAG, "connected devices detected: " + connectedDevices.length);
		
		for (BTDevice device : shoeManager.getShoesDevices()){
			boolean connected = false;
			Message msg = new Message();
			// this is normally a very short list, not matter that this is in O(n^2)
			for (int i=0; i<connectedDevices.length; i++){
				if (connectedDevices[i].equals(device.address)){
					msg.what = MyHandler.CONNECTED;
					connected = true;
					break;
				}
			}
			if (!connected){
				msg.what = MyHandler.DISCONNECTED;
			}
			msg.obj = device.address;
			handler.sendMessage(msg);
		}
		
	}
	
	private class MyHandler extends Handler {
		
		protected static final int CONNECTED = 1;
		protected static final int DISCONNECTED = 2;
		protected static final int CONNECTION_FAILED = 3;
		protected static final int PAIRING_REQUESTED = 4;

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			final int what = msg.what;
			final String address = (String)msg.obj;
			
			switch (what) {
				case CONNECTED:
					// Left shoe is connected..
					//Toast.makeText(MainScreen.this, "Shoe " + shoeManager.getShoe(address).side + " connected", Toast.LENGTH_SHORT).show();
					shoeManager.getShoe(address).state = AmarinoIntent.CONNECTED;
					break;
				case CONNECTION_FAILED:
					// Left shoe failed
					//Toast.makeText(MainScreen.this, "Connection to " + shoeManager.getShoe(address).side + " shoe failed", Toast.LENGTH_SHORT).show();
					shoeManager.getShoe(address).state = AmarinoIntent.DISCONNECTED;
					// Connect button available
					//connectButton.setEnabled(true);
					//connectButton.setText(R.string.connect);
					break;
				case DISCONNECTED:
					// Left shoe disconnected
					//Toast.makeText(MainScreen.this, "Shoe " + shoeManager.getShoe(address).side + " disconnected", Toast.LENGTH_SHORT).show();
					shoeManager.getShoe(address).state = AmarinoIntent.DISCONNECTED;
					break;
				case PAIRING_REQUESTED:
					Toast.makeText(MainScreen.this,"Device is not paired!\n\nPlease pull-down the notification bar to pair your device.\n\n", Toast.LENGTH_LONG).show();
					break;
			}
			
			handleShoesState();
			
		} // end handleMessage()
		
		protected void handleShoesState() {
			
			// If none are connected, reset button
			if (shoeManager.getLeftShoe().state != AmarinoIntent.CONNECTED && shoeManager.getRightShoe().state != AmarinoIntent.CONNECTED)
			{
				connectLoader.setVisibility(ProgressBar.INVISIBLE);
				introTextView.setText(R.string.mainscreen_description);
				successImageView.setVisibility(ImageView.INVISIBLE);
				connectButton.setEnabled(true);
				connectButton.setText(R.string.connect);
			}
			
			// If only left shoe connected, connect to right
			if (shoeManager.getLeftShoe().state == AmarinoIntent.CONNECTED && shoeManager.getRightShoe().state != AmarinoIntent.CONNECTED)
			{	
				connectLoader.setVisibility(ProgressBar.VISIBLE);
				introTextView.setText(R.string.mainscreen_description_short);
				successImageView.setVisibility(ImageView.INVISIBLE);
				connectButton.setEnabled(false);
				connectButton.setText(R.string.connecting);
				
			}
			
			// If only right shoe connected, connect to left
			if (shoeManager.getLeftShoe().state != AmarinoIntent.CONNECTED && shoeManager.getRightShoe().state == AmarinoIntent.CONNECTED)
			{				
				connectLoader.setVisibility(ProgressBar.VISIBLE);
				introTextView.setText(R.string.mainscreen_description_short);
				successImageView.setVisibility(ImageView.INVISIBLE);
				connectButton.setEnabled(false);
				connectButton.setText(R.string.connecting);
			}
			
			// If both connected, allow map
			if (shoeManager.getLeftShoe().state == AmarinoIntent.CONNECTED && shoeManager.getRightShoe().state == AmarinoIntent.CONNECTED)
			{	
				connectLoader.setVisibility(ProgressBar.INVISIBLE);
				introTextView.setText(R.string.mainscreen_description_success);
				successImageView.setVisibility(ImageView.VISIBLE);
				connectButton.setEnabled(true);
				connectButton.setText(R.string.gomap);
			}
		}
		
	}

}
