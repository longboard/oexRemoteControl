package com.example.oexremotecontrol;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends ActionBarActivity {
	
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
	
	public static void updateNmeaString(){
		int vehicleID = 0;
		String tempStringToBuild;
		byte[] b;
		int checksum = 0;
		
		if(whichVehicle){
			vehicleID = 2;
		}else{
			vehicleID = 3;
		}
		tempStringToBuild = "RCMOV," + String.valueOf(vehicleID) + "," + String.format("%03d",rudderDegrees) + "," +
		      String.format("%05d",thrusterRPM);
		
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
	    	updateNmeaString();
	    } else {
	    	vehicleName.setText("Harpo");
	    	updateNmeaString();
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

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
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
	

}
