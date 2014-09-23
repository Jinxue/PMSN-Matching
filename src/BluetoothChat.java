/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package com.example.android.BluetoothChat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // For performance information
    private static final String FPM = "FPM";
    //private static final String STAT = "STAT";
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Message for public key exchange
    public static final int MESSAGE_KEY = 6;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    // The Paillier encryptions from file
    private PaillierFromBinFile pff = null;
    private FPMBasic fpm = null;
    public static String KEY_N;
    public static String KEY_G;
    public String counterKeyN;
    public String counterKeyG;
    //private boolean keyState = false;
    
    // For test
    BigOperationTest bigTest = null;

    // The components for FPM testing
    private Button mFPMButton;
    private Button mFPMSettingButton;
    private Button mBigIntegerTest;
    private Button mTestingD, mTestingLamda;
    private Spinner mProtocol;
    private EditText mGrain;
    private EditText mNAttr;
    private EditText mTestTimes;
    
    private int grain;
    private int nAttr;
    private int testTimes, currentTime;
    private int protocol;
    private boolean grainFixed; // true: fix the grain; false: fix the # attributes
    
    // For time measurement
    private long sendStart, sendEnd, recStart, recEnd;
    private long[][] timeSendRecord, timeReceiveRecord;
    
    // For logging
    public PrintWriter logOut;
    
    private boolean onlyOneTest;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Set the spinner for protocol selection
        mProtocol = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.protocols, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mProtocol.setAdapter(adapter);
        mProtocol.setOnItemSelectedListener(new MyOnItemSelectedListener());
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) {
            	setupChat();
            	setupFPM();
            }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                
                // A test for Paillier
                //if(message.equals("paillier")){
                	/*String[] messages = fpm.buildSendingMessageArray();
                	for (String mes : messages)
                		sendMessage("paillier" + mes);*/
                	//message = "paillier" + fpm.buildSendingMessage();
                //}

                sendMessage(message);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    
    // Setup the FPM testing
    private void setupFPM() {
        Log.i(FPM, "setupFPM()");
        
        pff = new PaillierFromBinFile("paillier2000b.txt");
        KEY_N = pff.n.toString();
        KEY_G = pff.g.toString();
 
        // Initialize the components
        this.mGrain = (EditText) findViewById(R.id.editText_grain);
        this.mNAttr = (EditText) findViewById(R.id.editText_nAttr);
        this.mTestTimes = (EditText) findViewById(R.id.editText_times);
        this.mFPMButton = (Button)findViewById(R.id.button_fpm);
        this.mFPMSettingButton = (Button) findViewById(R.id.button_set);
        
        this.mFPMSettingButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				// Store the parameters from the form 
				if(mGrain.getText() == null)
					grain = 5;
				else
					grain = Integer.parseInt(mGrain.getText().toString());

				if(mNAttr.getText() == null)
					nAttr = 5;
				else
					nAttr = Integer.parseInt(mNAttr.getText().toString());
				
				if(mTestTimes.getText() == null)
					testTimes = 5;
				else
					testTimes = Integer.parseInt(mTestTimes.getText().toString());
				
				startOneTesting(true);
//				// Then set the protocol
//				switch(protocol){
//				case 0:
//					fpm = new FPMLevel1(pff, grain, nAttr);
//					break;
//				case 1:
//					fpm = new FPMLevel2(pff, grain, nAttr);
//					break;
//				case 2:
//					fpm = new FPMLevel3(pff, grain, nAttr);
//					break;
//				case 3:
//					fpm = new FPMLevel4(pff, grain, nAttr);
//					break;
//				}
//				
//				timeSendRecord = new long[4][testTimes];
//				timeReceiveRecord = new long[3][testTimes];
//				currentTime = 0;
//				
//				logFileName = "log" + grain + "_" + nAttr + "_" + (protocol+1) + ".txt";
//				try {
//					createFileOnDevice();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				//Log.i(FPM, "Set the parameters. Grain: " + grain + ",#Attributes: " + nAttr + ",test times: " + 
//				//		testTimes + "Protocol: level " + (protocol + 1));
//				logOut.println("Set the parameters. Grain: " + grain + ",#Attributes: " + nAttr + ",test times: " + 
//						testTimes + "Protocol: level " + (protocol + 1));
			}
        });
        /*this.mGrain.addTextChangedListener(new TextWatcher(){

			public void afterTextChanged(Editable text) {
				// TODO Auto-generated method stub
				if(text.length() == 0)
					grain = 5;
				else
					grain = Integer.parseInt(text.toString());
			}*/

        // Set the listener for FPM button
        this.mFPMButton.setOnClickListener(new OnClickListener(){

			public void onClick(View v) {
				// TODO Auto-generated method stub
				buildAndSendBinMessage();
				onlyOneTest = true;
			}
        });

        this.mTestingD = (Button) findViewById(R.id.button_d);
        this.mTestingLamda = (Button) findViewById(R.id.button_lamda);

        this.mTestingD.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				// Fix the grain
				grain = 5;
				testTimes = 2;
				
				grainFixed = true;
				nAttr = 250;
				protocol = 3;
				
				onlyOneTest = false;
				/*for (int pt = 0; pt < 4; pt++){
					for(int d = 100; d <= 800; d = d + 100){
						nAttr = d;
						protocol = pt;*/
						startOneTesting(true);
						
						// First we need to transmit the protocol and wait a second
						String message = "protocol" + grain + "," + nAttr + "," + protocol + "," + testTimes;
						sendMessage(message);
						try {
		    				Thread.sleep(1000);
		    			} catch (InterruptedException e) {
		    				// TODO Auto-generated catch block
		    				e.printStackTrace();
		    			}

						buildAndSendBinMessage();
					//}
				//}
			}
        });
        
        this.mTestingLamda.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				// Fix the #attributes
				nAttr = 100;
				testTimes = 100;
				
				grainFixed = false;
				grain = 2;
				protocol = 0;
				
				onlyOneTest = false;
				
				/*for (int pt = 0; pt < 4; pt++){
					for(int gr = 2; gr <= 20; gr ++){
						grain = gr;
						protocol = pt;*/
						startOneTesting(true);
						
						// First we need to transmit the protocol and wait a second
						String message = "protocol" + grain + "," + nAttr + "," + protocol + "," + testTimes;
						sendMessage(message);
		               try {
		    				Thread.sleep(1000);
		    			} catch (InterruptedException e) {
		    				// TODO Auto-generated catch block
		    				e.printStackTrace();
		    			}

						buildAndSendBinMessage();
					//}
				//}
	
			}
        	
        });
        
        this.mBigIntegerTest = (Button)findViewById(R.id.button_big);
        this.mBigIntegerTest.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				// TODO Auto-generated method stub
                // Compute the operation time cost
                bigTest = new BigOperationTest();
                bigTest.startTesting();
//                bigTest.exponentiation();
//                bigTest.multiplication();
//                bigTest.encryptionPaillier();
//                bigTest.decryptionPaillier();
//            		
//                bigTest = new BigOperationTest(2048, 1024);
//                bigTest.exponentiation();
//                bigTest.multiplication();
			}
        	
        });
        
        // Send the public keys <n, g>
        //String myKeys = "key" + BluetoothChat.KEY_N + "," + BluetoothChat.KEY_G;
        //sendMessage(myKeys);
    }

//    public void onItemSelected(AdapterView<?> parent, View v, int position,
//			long id) {
//		this.protocol = mProtocol.getItemAtPosition(position).toString();
//	}
    public void startOneTesting(boolean sendOrRecv){
		// Then set the protocol
		switch(protocol){
		case 0:
			fpm = new FPMLevel1(pff, grain, nAttr);
			break;
		case 1:
			fpm = new FPMLevel2(pff, grain, nAttr);
			break;
		case 2:
			fpm = new FPMLevel3(pff, grain, nAttr);
			break;
		case 3:
			fpm = new RSV(pff, grain, nAttr);
			break;
		}
		
		timeSendRecord = new long[4][testTimes];
		timeReceiveRecord = new long[3][testTimes];
		currentTime = 0;
		
		String logFileName;
		if(sendOrRecv)
			logFileName = "log_send_" + grain + "_" + nAttr + "_" + (protocol+1) + ".txt";
		else
			logFileName = "log_recv_" + grain + "_" + nAttr + "_" + (protocol+1) + ".txt";
		
		try {
			createFileOnDevice(logFileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Log.i(FPM, "Set the parameters. Grain: " + grain + ",#Attributes: " + nAttr + ",test times: " + 
		//		testTimes + "Protocol: level " + (protocol + 1));
		logOut.println("Set the parameters.");
		logOut.println("Grain: " + grain + ",#Attributes: " + nAttr + ",test times: " + 
				testTimes + ",Protocol: level " + (protocol + 1));
    }
    
	void buildAndSendCharMessage() {
		// TODO Auto-generated method stub
		
		long lStart, lEnd;
		lStart = System.currentTimeMillis();
		sendStart = lStart;
		String message = "paillier" + fpm.buildSendingMessage();
		lEnd = System.currentTimeMillis();
		timeSendRecord[0][currentTime] = lEnd - lStart;
		
		sendMessage(message);
	}

	void buildAndSendBinMessage() {
		// TODO Auto-generated method stub
		byte[] bMessage;
		
		// Write a magic beginning number
		long lStart, lEnd;
		lStart = System.currentTimeMillis();
		sendStart = lStart;
		bMessage = fpm.buildBinSendingMessage();
		lEnd = System.currentTimeMillis();
		timeSendRecord[0][currentTime] = lEnd - lStart;
		
		sendBinMessage(bMessage);
	}

	void sendStat(){
		// First for send record
		logOut.println("Statistics for sending party.");
		logOut.println("*************************************");
		for (int i = 0; i < 4; i ++){
			stat(timeSendRecord[i], testTimes);
		}
		
		logOut.close();
	}
	
	void receiveStat(){
		logOut.println("Statistics for Receiving party.");
		logOut.println("*************************************");
		for (int i = 0; i < 3; i ++){
			stat(timeReceiveRecord[i], testTimes);
		}
		
		this.logOut.close();
	}
	
	// Stat for each vector
	void stat(long[] timeUsed, int times){
		double max, min, mean, std, median;
		Arrays.sort(timeUsed);
		min = timeUsed[0];
		max = timeUsed[times - 1];
		median = (timeUsed[times / 2] + timeUsed[times / 2 - 1]) / 2;
		
		double sum = 0;
		for (int i = 0; i < times; i++){
			sum += timeUsed[i];
		}		
		mean = sum / times;
	
		std = 0;
		for (int i = 0; i < times; i++){
			std += (timeUsed[i] - mean) * (timeUsed[i] - mean);
		}		
		std = Math.sqrt(std / times);

		logOut.println("Total:" + sum + ", Max: " + max + ", Min: " + min + ", Mean: " + mean + ", Median: " + median + ", Std: " + std);
		//Log.i(TAG, "--------------------------------------");
	}

	
    private void createFileOnDevice(String logFileName) throws IOException {
            /*
             * Function to initially create the log file and it also writes the time of creation to file.
             */
            File Root = Environment.getExternalStorageDirectory();
            if(Root.canWrite()){
                 File LogFile = new File(Root, logFileName);
                 FileWriter logWriter = new FileWriter(LogFile, false);
                 logOut = new PrintWriter(logWriter);
                 Date date = new Date();
                 logOut.println("Logged at " + String.valueOf(date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds()));
            }
        }
    
    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
        	
    		// Then send it
            int length = message.length();
        	
            // We send the length of the message at first for reliable transmission
            mChatService.write(("lengthc" + Integer.toString(length)).getBytes());
         
               try {
    				Thread.sleep(1000);
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}

        	
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            
            logOut.println("Will send the message with Bytes: " + send.length);
            
            long lStart, lEnd;
            lStart = System.currentTimeMillis();
            mChatService.write(send);
            lEnd = System.currentTimeMillis();
            timeSendRecord[1][currentTime] = lEnd - lStart;
            
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //if(message.length() < 64)
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    private void sendBinMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
        	
    		// Then send it
            int length = message.length;
        	
            // We send the length of the message at first for reliable transmission
            mChatService.write(("lengthb" + Integer.toString(length)).getBytes());
         
               try {
    				Thread.sleep(1000);
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}

        	
            // Get the message bytes and tell the BluetoothChatService to write
            //byte[] send = message.getBytes();
            
            logOut.println("Will send the message with Bytes: " + length);
            
            long lStart, lEnd;
            lStart = System.currentTimeMillis();
            mChatService.write(message);
            lEnd = System.currentTimeMillis();
            timeSendRecord[1][currentTime] = lEnd - lStart;
            
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //if(message.length() < 64)
            mOutEditText.setText(mOutStringBuffer);
        }
    }
    
    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    static int mesLength= 0;
    static StringBuffer totalMessage;
    static ByteArrayOutputStream totalBytes;
    static boolean byteOrChar;

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                if(writeMessage.length() < 64)
                	mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                // We need to handle large data transmission
                
                if(byteOrChar){
                	try {
						totalBytes.write(readBuf);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	if(totalBytes.size() == mesLength){
                		recEnd = System.currentTimeMillis();
                		byteOrChar = false;
                       	String display = processBinMessage(totalBytes.toByteArray());
                    	mConversationArrayAdapter.add(mConnectedDeviceName+":  " + display);
                	}
                	return;
                }

                String readMessage = new String(readBuf, 0, msg.arg1);
                if (readMessage.startsWith("lengthc")){
            		mesLength = Integer.parseInt(readMessage.substring(7));
            		totalMessage = new StringBuffer();
        
            		recStart = System.currentTimeMillis();
            		byteOrChar = false;
            		break;
            	}else if(readMessage.startsWith("lengthb")){
            		mesLength = Integer.parseInt(readMessage.substring(7));
            		totalBytes = new ByteArrayOutputStream();
            		recStart = System.currentTimeMillis();
            		byteOrChar = true;
            		break;
            	}

                // Then it must be string
                totalMessage.append(readMessage);
                //Log.i(FPM, "Received length: " + totalMessage.length() + " and expected length: " + mesLength);
                
                if(totalMessage.length() == mesLength){
                	recEnd = System.currentTimeMillis();
                   	String display = processMessage(totalMessage.toString());
                	mConversationArrayAdapter.add(mConnectedDeviceName+":  " + display);
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

   String processBinMessage(byte[] recMessage){
		// The data receiving time 
		timeReceiveRecord[0][currentTime] = recEnd - recStart;
		
		long lStart, lEnd;
		
		lStart = System.currentTimeMillis();
		String midValue = fpm.buildMidResultbyRecWithPubKeys(recMessage, this.counterKeyN, this.counterKeyG);
		lEnd = System.currentTimeMillis();
		
		// The data processing time
		timeReceiveRecord[1][currentTime] = lEnd - lStart;
		// Then send it

		lStart = System.currentTimeMillis();
		sendMessage("mid" + midValue);
		lEnd = System.currentTimeMillis();
		
		// The middle data transmission time
		timeReceiveRecord[2][currentTime] = lEnd - lStart;
		
		if(currentTime < testTimes - 1)
			currentTime ++;
		else{
			this.receiveStat();
			currentTime = 0;
		}
		
		return "Send the middle result successfully";
		//mConversationArrayAdapter.add(mConnectedDeviceName+":  " + display);
   }
    
    String processMessage(String recMessage){
    	String display = recMessage;
    	// The receiver receives the message
    	if(//!keyState && 
    			recMessage.startsWith("key")){
    		String[] keys = recMessage.substring(3).split(",");
    		this.counterKeyN = keys[0];
    		this.counterKeyG = keys[1];
    		//keyState = true;
    		display = "Get keys successfully";
    		Log.i(FPM, "Get the public key from paired party.");
    	}
    	else if(recMessage.startsWith("protocol")){
    		String[] protos = recMessage.substring(8).split(",");
    		this.grain = Integer.parseInt(protos[0]);
    		this.nAttr = Integer.parseInt(protos[1]);
    		this.protocol = Integer.parseInt(protos[2]);
    		this.testTimes = Integer.parseInt(protos[3]);
    		this.startOneTesting(false);
    	}
    	else if(recMessage.startsWith("paillier")){
    		
    		// The data receiving time 
    		timeReceiveRecord[0][currentTime] = recEnd - recStart;
    		
    		long lStart, lEnd;
    		String sentMessage = recMessage.substring(8);
    		
    		lStart = System.currentTimeMillis();
    		String midValue = fpm.buildMidResultbyRecWithPubKeys(sentMessage, this.counterKeyN, this.counterKeyG);
    		lEnd = System.currentTimeMillis();
    		
    		// The data processing time
    		timeReceiveRecord[1][currentTime] = lEnd - lStart;
    		// Then send it

    		lStart = System.currentTimeMillis();
    		sendMessage("mid" + midValue);
    		lEnd = System.currentTimeMillis();
    		
    		// The middle data transmission time
    		timeReceiveRecord[2][currentTime] = lEnd - lStart;
    		
    		if(currentTime < testTimes - 1)
    			currentTime ++;
    		else{
    			this.receiveStat();
    			currentTime = 0;
    		}
    		
    		display = "Send the middle result successfully";
    		//mConversationArrayAdapter.add(mConnectedDeviceName+":  " + display);
    	}
    	// The sender receives the back message
    	else if(recMessage.startsWith("mid")){
    		String sentMessage = recMessage.substring(3);
    		
    		long lStart, lEnd;
    		lStart = System.currentTimeMillis();
    		int finalValue = fpm.profileMatching(sentMessage);
    		lEnd = System.currentTimeMillis();
    		sendEnd = lEnd;
    		timeSendRecord[2][currentTime] = lEnd - lStart;
    		timeSendRecord[3][currentTime] = sendEnd - sendStart;
            
    		display = "The final matching score " + finalValue;
            // Finally we finish a FPM process, and begin the next one
            
            if(currentTime + 1 < testTimes){
            	currentTime ++;
            	this.buildAndSendBinMessage();
            }
            else{
            	this.sendStat();
            	currentTime = 0;
            	
            	if(!onlyOneTest)
            		startNewSending();
            }
    	}
    	/*else 
    		display = recMessage;*/
    	
    	return display;
    }
    
    private void startNewSending() {
		// TODO Auto-generated method stub
    	// Fix grain, and change the # attributes
    	if(grainFixed){
			if(nAttr < 500)
				nAttr = nAttr + 50;
			else if(protocol < 3){
				protocol ++;
				nAttr = 50;
			} else{
				//return;
				
				// Instead of return, we continue to the next testing directly
				nAttr = 100;
				testTimes = 2;
				
				grainFixed = false;
				grain = 2;
				protocol = 3;
			}
		}else{
			if(grain < 10)
				grain ++;
			else if(protocol < 3){
				protocol ++;
				grain = 2;
			} else
				return;
		}
		
    	startOneTesting(true);
			
		// First we need to transmit the protocol and wait a second
		String message = "protocol" + grain + "," + nAttr + "," + protocol + "," + testTimes;
		sendMessage(message);
        try {
        	Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		buildAndSendBinMessage();
	}
    
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

    
    public class MyOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent,
            View view, int pos, long id) {
          //Toast.makeText(parent.getContext(), "The planet is " +
          //    parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
   		protocol = pos;
	       // Our Paillier public keys
        }

        public void onNothingSelected(@SuppressWarnings("rawtypes") AdapterView parent) {
          // Do nothing.
        }
    }
}
