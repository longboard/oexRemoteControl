package com.example.oexremotecontrol;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity {
	
	//Bluetooth related variables
	private static final String TAG = "THINBTCLIENT";
    private static final boolean D = true;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    // Well known SPP UUID (will *probably* map to
    // RFCOMM channel 1 (default) if not in use);
    // see comments in onResume().
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // ==> hardcode your server's MAC address here <==
    private static String address =  "00:06:66:06:E9:7B"; //"30:85:A9:5C:D6:D4";
	
	
	public static TextView vehicleName;
	public static Button toggleVehicleCntrl;
	
	public static SeekBar rudderSeekBar;
	public static SeekBar thrusterSeekBar;
	
	public static TextView rudderValue;
	public static TextView thrusterValue;
	
	public static int rudderDegrees; // +/-20
	public static int thrusterRPM;   // +/-1000
	
	public static TextView nmeaString;
	
	public static boolean whichVehicle; // boolean to store value of which vehicle we want to control
	
	public static Timer timer;
	public static TimerTask timerTask;
	
	public boolean enableDisableButtonPress = false;
	public int enableDisableState = 0; // initialize to disabled
	
	public static int vehicleID = 0;
	
	//we are going to use a handler to be able to run in our TimerTask
	final Handler handler = new Handler();
	
	public static void updateNmeaString(){
		String tempStringToBuild = "";
		String tempRudderString = "";
	    String tempThrusterString = "";
		
		byte[] b;
		int checksum = 0;
		
		if(whichVehicle){
			vehicleID = 2;
		}else{
			vehicleID = 3;
		}
		
		if(rudderDegrees <= 0){tempRudderString = "" + (String.format("%03d",rudderDegrees));}
		if(rudderDegrees > 0){tempRudderString = "" + (String.format("+%02d",rudderDegrees));}
		
		if(thrusterRPM <= 0){tempThrusterString = "" + String.format("%05d",thrusterRPM);}
		if(thrusterRPM > 0){tempThrusterString = "" + String.format("+%04d",thrusterRPM);}
		
		tempStringToBuild = "RCMOV," + String.valueOf(vehicleID) + "," + tempRudderString + "," + tempThrusterString;
		
		b = tempStringToBuild.getBytes();
		for(int i=0; i<b.length;i++){
			checksum = checksum ^ b[i];
		}
		
		tempStringToBuild = "$" + tempStringToBuild + "*" + String.format("%2x",checksum);
		
		nmeaString.setText(tempStringToBuild);
	}
	
	public void toggleVehicleControl(View v) {
		whichVehicle = ((ToggleButton) v).isChecked();
	
		
	    if (whichVehicle) {
	    	vehicleName.setText("Groucho");
	    } else {
	    	vehicleName.setText("Harpo");
	    }
	    updateNmeaString();
	}
	
	public void toggleEnableDisable(View v){
			enableDisableButtonPress = true;
			enableDisableState = enableDisableState ^ 1;
			
			   if (whichVehicle) {
				    if(enableDisableState==1){vehicleName.setText("Groucho R/C Enabled");}else{vehicleName.setText("Groucho R/C Disabled");}
			   } else {
			    	if(enableDisableState==1){vehicleName.setText("Harpo R/C Enabled");}else{vehicleName.setText("Harpo R/C Disabled");}
			   }
	}
	
	public void allStop(View v){
		rudderDegrees = 0;
		thrusterRPM = 0;
		rudderValue.setText(String.valueOf(rudderDegrees));
		thrusterValue.setText(String.valueOf(thrusterRPM));
		rudderSeekBar.setProgress(50);
		thrusterSeekBar.setProgress(50);
		updateNmeaString();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Bluetooth setup
		if (D)
            Log.e(TAG, "+++ ON CREATE +++");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not available.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this,
                    "Please enable your BT and re-run this program.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (D)
            Log.e(TAG, "+++ DONE IN ON CREATE, GOT LOCAL BT ADAPTER +++");
		

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		
		// When this returns, it will 'know' about the server,
        // via it's MAC address.
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        // We need two things before we can successfully connect
        // (authentication issues aside): a MAC address, which we
        // already have, and an RFCOMM channel.
        // Because RFCOMM channels (aka ports) are limited in
        // number, Android doesn't allow you to use them directly;
        // instead you request a RFCOMM mapping based on a service
        // ID. In our case, we will use the well-known SPP Service
        // ID. This ID is in UUID (GUID to you Microsofties)
        // format. Given the UUID, Android will handle the
        // mapping for you. Generally, this will return RFCOMM 1,
        // but not always; it depends what other BlueTooth services
        // are in use on your Android device.
        try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
                Log.e(TAG, "ON RESUME: Socket creation failed.", e);
        }

        // Discovery may be going on, e.g., if you're running a
        // 'scan for devices' search from your handset's Bluetooth
        // settings, so we call cancelDiscovery(). It doesn't hurt
        // to call it, but it might hurt not to... discovery is a
        // heavyweight process; you don't want it in progress when
        // a connection attempt is made.
        mBluetoothAdapter.cancelDiscovery();

        // Blocking connect, for a simple client nothing else can
        // happen until a successful connection is made, so we
        // don't care if it blocks.
        try {
                btSocket.connect();
                Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");
        } catch (IOException e) {
                try {
                        btSocket.close();
                } catch (IOException e2) {
                        Log.e(TAG,
                                "ON RESUME: Unable to close socket during connection failure", e2);
                }
        }
		
	}
	

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		 if (D)
             Log.e(TAG, "- ON PAUSE -");

		 if (outStream != null) {
             try {
                     outStream.flush();
             } catch (IOException e) {
                     Log.e(TAG, "ON PAUSE: Couldn't flush output stream.", e);
             }
		 }

		 try     {
             btSocket.close();
		 } catch (IOException e2) {
             Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
		 }
		 
		 timer.cancel();
	     timer = null;
	
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		 // Create a data stream so we can talk to server.
        if (D)
                Log.e(TAG, "+ ABOUT TO SAY SOMETHING TO SERVER +");

        try {
                outStream = btSocket.getOutputStream();
        } catch (IOException e) {
                Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
        }

        String message = "\r\nOEX Remote Control Program!\r\n";
        byte[] msgBuffer = message.getBytes();
        try {
                outStream.write(msgBuffer);
        } catch (IOException e) {
                Log.e(TAG, "ON RESUME: Exception during write.", e);
        }
        
        startTimer();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	//public static class PlaceholderFragment extends Fragment implements View.OnClickListener{
	public static class PlaceholderFragment extends Fragment {
		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			toggleVehicleCntrl = (ToggleButton) rootView.findViewById(R.id.toggleVehicle);
			vehicleName = (TextView) rootView.findViewById(R.id.vehicleName);
			
			rudderSeekBar = (SeekBar)rootView.findViewById(R.id.rudderSetting);
			thrusterSeekBar = (SeekBar)rootView.findViewById(R.id.thrusterSetting);
			
			rudderValue = (TextView)rootView.findViewById(R.id.rudderVal);
			thrusterValue = (TextView)rootView.findViewById(R.id.thrusterVal);
			
			nmeaString = (TextView)rootView.findViewById(R.id.nmeaString);
			
			rudderSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					rudderDegrees = (int) (((double)(progress-50)/100.0)*40); 
					rudderValue.setText(String.valueOf(rudderDegrees));
					updateNmeaString();
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
			});
			
			thrusterSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					thrusterRPM = (int) (((double)(progress-50)/100.0)*2000); 
					thrusterValue.setText(String.valueOf(thrusterRPM));
					updateNmeaString();
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
			});
			
			
			return rootView;
		}
		
	}
	
	  public void startTimer() {
		  //set a new Timer
		  timer = new Timer();
		  //initialize the TimerTask's job
		  initializeTimerTask();
		  //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
		  timer.schedule(timerTask, 250, 250); //
	  }
	  
	  public void stoptimertask(View v) {
	      //stop the timer, if it's not already null
	      if (timer != null) {
		     timer.cancel();
		     timer = null;
	      }
	  }
	  
	  public void initializeTimerTask() {
		  timerTask = new TimerTask() {
		  public void run() {
		       //use a handler to run a toast that shows the current timestamp
		       handler.post(new Runnable() {
		            public void run() {
		                String messageToOEX;
		                byte[] msgBuffer;
		            	
		            	if( (!enableDisableButtonPress)){
		            		if(enableDisableState == 1){
		            			messageToOEX = nmeaString.getText().toString() + "\r\n";
		            			msgBuffer = messageToOEX.getBytes();
		            			try {
		            				outStream.write(msgBuffer);
		            			} catch (IOException e) {
		            				Log.e(TAG, "ON RESUME: Exception during write.", e);
		            			}
		       				}
		            	}else{
		            		//messageToOEX = "button press!";
		            		if(enableDisableState == 1){
		            			messageToOEX = "RCSTATE," + String.valueOf(vehicleID) + "," +"1";
		            		}else
		            		{
		            			messageToOEX = "RCSTATE," + String.valueOf(vehicleID) + "," +"0";
		            		}
		            		byte[] b;
		            		int checksum = 0;
		            		
		            		b = messageToOEX.getBytes();
		            		for(int i=0; i<b.length;i++){
		            			checksum = checksum ^ b[i];
		            		}
		            		
		            		messageToOEX = "$" + messageToOEX + "*" + String.format("%2x",checksum)  + "\r\n";
		            		enableDisableButtonPress = false;
		            		
		            		msgBuffer = messageToOEX.getBytes();
		            		try {
		            			 outStream.write(msgBuffer);
		            		} catch (IOException e) {
		                         Log.e(TAG, "ON RESUME: Exception during write.", e);
		            		}
		            	}
		            	
		            
		            	 
		                 //show the toast
		                 //int duration = Toast.LENGTH_SHORT;  
		                 //Toast toast = Toast.makeText(getApplicationContext(), messageToOEX, duration);
		                 //toast.show();
		                 }
		             });
		        }
		   };
	  }



	
	
}
