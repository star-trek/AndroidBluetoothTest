package com.example.bluetoothexample;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.EditText;  
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothExampleActivity extends Activity implements OnClickListener, OnSeekBarChangeListener, OnTouchListener, OnDragListener
{
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @TargetApi(11)
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
        	Log.i("find", "No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
        	Log.i("find", "Bluetooth Device List");
            for(BluetoothDevice device : pairedDevices)
            {
            	Log.i("find", device.getName());
                if(device.getName().equals("RCCAR")) 
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
        try {
			mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
	        mmSocket.connect();
	        mmOutputStream = mmSocket.getOutputStream();
	        mmInputStream = mmSocket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

        Log.i("find", "Bluetooth Opened");
        ImageView surfaceView = (ImageView)findViewById(R.id.surfaceView1);
        surfaceView.setOnTouchListener(this);
        surfaceView.setOnDragListener(this);
        
        //sendSerial((char)127, (char)127);
    }
    
    void sendSerial(char lr, char fb){
        byte[] buf = new byte[4];
        buf[0] = (byte) 0xFF;
        buf[1] = (byte) lr;
        buf[2] = (byte) fb;
        buf[3] = (byte) (buf[0]^buf[1]^buf[2]);
        try {
        	mmOutputStream.write(buf);
        } catch (IOException ex) {
        }
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
        	Toast.makeText(this, "No bluetooth adapter available", Toast.LENGTH_LONG);
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("FireFly-108B")) 
                {
                    mmDevice = device;
                    break;
                }
            }
        }
    	Toast.makeText(this, "Bluetooth Device Found", Toast.LENGTH_LONG);
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);        
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

    	Toast.makeText(this, "Bluetooth Opened", Toast.LENGTH_LONG);
    }

    void beginListenForData()
    {
        final Handler handler = new Handler(); 
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {                
               while(!Thread.currentThread().isInterrupted() && !stopWorker)
               {
                    try 
                    {
                        int bytesAvailable = mmInputStream.available();                        
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Log.i("data", data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } 
                    catch (IOException ex) 
                    {
                        stopWorker = true;
                    }
               }
            }
        });

        workerThread.start();
    }

    void sendData() throws IOException
    {
        mmOutputStream.write('A');
    	Toast.makeText(this, "Data Sent", Toast.LENGTH_LONG);
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
    	Toast.makeText(this, "Bluetooth Closed", Toast.LENGTH_LONG);
    }

    private void showMessage(String theMsg) {
        Toast msg = Toast.makeText(getBaseContext(),
                theMsg, (Toast.LENGTH_LONG)/160);
        msg.show();
    }

	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
        sendSerial((char)127, (char)(progress-128));
	}

	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	public boolean onTouch(View v, MotionEvent event) {
        double x = ((event.getX()/v.getWidth())-0.5)*2*127;
        double y = ((event.getY()/v.getHeight())-0.5)*2*127;
        Log.i("touch", "x="+x+", y="+y);
        
		 switch (event.getAction()) {
	         case MotionEvent.ACTION_DOWN:
	         case MotionEvent.ACTION_MOVE:
	             //double x = ((event.getX()/v.getWidth())-0.5)*2*127;
	            // double y = ((event.getY()/v.getHeight())-0.5)*2*127;
	             
	             sendSerial((char)-x, (char)-y);
	             break;
	         case MotionEvent.ACTION_UP:
	             sendSerial((char)0, (char)0);
	             break;
	     }
		return false;
	}

	@TargetApi(11)
	public boolean onDrag(View v, DragEvent event) {
        double x = ((event.getX()/v.getWidth())-0.5)*2*127;
        double y = ((event.getY()/v.getHeight())-0.5)*2*127;
        Log.i("touch", "x="+x+", y="+y);
		// TODO Auto-generated method stub
		return false;
	}
}