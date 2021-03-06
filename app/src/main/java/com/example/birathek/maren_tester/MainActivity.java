package com.example.birathek.maren_tester;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID

    //Dette er EOL-symbolet!!! Hvis ikke linja slutter med "!" venter den på mer!
    final byte delimiter = 33;
    int readBufferPosition = 0;

    public void connectToBt(){
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);

            if (!mmSocket.isConnected()){
                Log.e("Aquarium", "Not connected. Connecting");
                mmSocket.connect();
            } else {
                Log.e("Aquarium", "Already connected");
            }
            Toast.makeText(MainActivity.this, "Connection established", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e("Aquarium", "Failed to connect. Retrying");
            connectToBt();
        }
    }

    public void sendBtMsg(String msg2send){
        //UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID

        try {

            String msg = msg2send;

            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());
            Log.e("Aquarium", "Sent msg");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e("Aquarium", "Failed to send msg");
            sendBtMsg(msg2send);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Handler handler = new Handler();

        final TextView myLabel = (TextView) findViewById(R.id.btResult);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        while(pairedDevices.size() == 0) {
            pairedDevices = mBluetoothAdapter.getBondedDevices();
        }

        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("raspberrypi"))
                {
                    Log.e("Aquarium",device.getName());

                    mmDevice = device;
                    final BluetoothDevice device1 = device;

                    handler.post(new Runnable() {
                        public void run() {
                            String text = "Waiting for ";
                            myLabel.setText(text + device1.getName());
                        }
                    });

                    break;
                }
            }
        }

        final class workerThread implements Runnable {

            //private String btMsg;

            public workerThread() {
                //public workerThread(String msg) {
                //btMsg = msg;
            }


            public void run() {

                //sendBtMsg(btMsg);
                while (!Thread.currentThread().isInterrupted()) {
                    int bytesAvailable;

                    try {

                        final InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {

                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.e("Aquarium recv bt", "bytes available");
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);

                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    //The variable data now contains our full command

                                    //Her blir outputtet fra Pi-en skrevet
                                    handler.post(new Runnable() {
                                        public void run() {
                                            Log.e("Aquarium", "Changing text to: " + data);
                                            myLabel.setText(data);
                                        }
                                    });
                                    break;


                                } else {
                                    Log.e("Aquarium", "pushing readBufferPosition forward");
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        handler.post(new Runnable() {
                            public void run() {
                                final String text = "Failed to connect";
                                myLabel.setText(text);
                            }
                        });
                        e.printStackTrace();
                    }

                }
            }
        }

        //Cycle

        connectToBt();
        (new Thread(new workerThread())).start();

    }

    //TODO: Lage en funksjon som leser ping-meldinger fra Pi og svarer på
    //      disse for å bekrefte status som tilkoblet. Brukeren trenger
    //      ikke se disse, men de bør loggføres.



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        //kommentert
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}