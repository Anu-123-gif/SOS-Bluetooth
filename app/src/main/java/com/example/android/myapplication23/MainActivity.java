package com.example.android.myapplication23;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;

public class MainActivity extends Activity {
    //    private final String DEVICE_NAME="MyBTBee";
    private final String DEVICE_ADDRESS="00:18:91:D8:24:8B";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Button startButton, sendButton,clearButton,stopButton;
    TextView textView;
    EditText editText;
    boolean deviceConnected=false;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;
    private final int REQUEST_READ_PHONE_STATE=1;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    EditText edtSmsNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        Button sendB = (Button) findViewById(R.id.send_btn);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
            // Toast.makeText(this, "W?", Toast.LENGTH_SHORT);
        }
        else {// Toast.makeText(this, "!!!", Toast.LENGTH_SHORT);
            sendB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendMessage("1");
                }
            });
        }

        setUiEnabled(false);

    }

    public void setUiEnabled(boolean bool)
    {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(!bool);

    }

    public boolean BTinit()
    {
        boolean found=false;
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device=iterator;
                    found=true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    public void onClickStart(View view) {
        if(BTinit())
        {
            if(BTconnect())
            {
                setUiEnabled(true);
                deviceConnected=true;
                beginListenForData();
                textView.append("\nConnection Opened!\n");
            }

        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run()
                                {
                                    textView.append(string);
                                    sendMessage(textView.getText().toString());
                                }
                            }, 5000);

                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        textView.append("\nSent Data:"+string+"\n");

    }

    public void onClickStop(View view) throws IOException {
        stopThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected=false;
        textView.append("\nConnection Closed!\n");
    }

    public void onClickClear(View view) {
        textView.setText("");
    }

    public void sendMessage(String string) {

        String last = string.substring(string.length() - 1);
        String secondlast = "0";
        if(string.length() > 1)
            secondlast = string.substring(string.length()-2);

        if (last.equals("1") && secondlast.equals("0")) {
            textView.setText("1");
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(editText.getText().toString(), null /*"\"+919934029007\",145"*/, "It worked!! btw Bhaskar is in Danger :P", null, null);
                Toast.makeText(getApplicationContext(), "Your Message Sent",
                        Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                Toast.makeText(getApplicationContext(), ex.getMessage().toString(),
                        Toast.LENGTH_LONG).show();
            }

        }
    }
}