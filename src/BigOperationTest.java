/*
 * Testing the computation overhead of big integer on Android platform.
 *
 * @author Jinxue Zhang
 * @date	February 10, 2012
 */
 
//package com.example.android.BluetoothChat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import android.os.Environment;
import android.util.Log;

public class BigOperationTest {

	private static final String TAG = "BigOperationTest";
	
	int length, lengthMod;
	
	BigInteger n, nsquare;

	BigInteger a, b;
	//BigInteger[] c;
	
	//int times;
	
	long start, end;
	
	//long[] timeUsed;
	
	Paillier paillier;
	
	PrintWriter logOut;
	
	// Constructor
	BigOperationTest(){
		// Do nothing
	}
	
	BigOperationTest(int len, int lenmod){
		initial(len, lenmod);
	}

	void initial(int len, int lenmod){
		length = len; 
		lengthMod = lenmod;
		n = new BigInteger(lengthMod, new Random());
		nsquare = n.multiply(n);
		
		a = new BigInteger(length, new Random());
		b = new BigInteger(length, new Random());
		
		
		paillier = new Paillier();
		
		String logFileName = "logBigIntegerTest_" + len + "_" + lenmod + ".txt";
		File Root = Environment.getExternalStorageDirectory();
        if(Root.canWrite()){
             File LogFile = new File(Root, logFileName);
             FileWriter logWriter = null;
			try {
				logWriter = new FileWriter(LogFile, false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
             logOut = new PrintWriter(logWriter);
             Date date = new Date();
             logOut.println("Logged at " + String.valueOf(date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds()));
        }
	}
	
	void exponentiation(){
		int times = 100;
		
		long[] timeUsed = new long[times];
		BigInteger c = null;

		for (int i = 0; i < times; i++){
			start = System.currentTimeMillis();
			c = a.modPow(b, nsquare);
			end = System.currentTimeMillis();
			timeUsed[i] = end - start;
		}
		
		String message = "The testing result for "+ length +"-bit exponentiation with result length: " + c.bitLength();
		Log.i(TAG, message);
		logOut.println(message);
		stat(timeUsed);
	}

	void multiplication(){
		int times = 10000;
		
		long[] timeUsed = new long[times];
		BigInteger c = null;

		for (int i = 0; i < times; i++){
			//start = System.currentTimeMillis();
			start = System.nanoTime();
			c = a.multiply(b).mod(nsquare);
			//end = System.currentTimeMillis();
			end = System.nanoTime();
			timeUsed[i] = end - start;
		}
		String message = "The testing result (nano seconds) for "+ length +"-bit multiplication with result length: " + c.bitLength();
		
		Log.i(TAG, message);
		logOut.println(message);
		stat(timeUsed);
	}

	BigInteger[] encryptionPaillier(){
		int times = 100;
		
		long[] timeUsed = new long[times];
		BigInteger[] c = new BigInteger[times];

		for (int i = 0; i < times; i++){
			start = System.currentTimeMillis();
			c[i] = paillier.Encryption(a);
			end = System.currentTimeMillis();
			timeUsed[i] = end - start;
		}
		
		String message ="The testing result (milli seconds) for "+ length +"-bit Paillier encryption with result length: "
				+ c[0].bitLength(); 
		Log.i(TAG, message);
		logOut.println(message);
		stat(timeUsed);
		
		return c;
	}

	void decryptionPaillier(BigInteger[] c){
		int times = c.length;
		
		long[] timeUsed = new long[times];

		for (int i = 0; i < times; i++){
			start = System.currentTimeMillis();
			a = paillier.Decryption(c[i]);
			end = System.currentTimeMillis();
			timeUsed[i] = end - start;
		}
		
		String message = "The testing result for "+ length +"-bit Paillier decryption with result length: "
				+ a.bitLength();
		Log.i(TAG, message);
		logOut.println(message);
		stat(timeUsed);
	}

	void stat(long[] timeUsed){
		int times = timeUsed.length;
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

		String message = "Total:" + sum + ",Mean: " + mean + ",Max: " + max + ",Min: " + min + ",Median: " + median + ",Std: " + std; 
		Log.i(TAG, message);
		Log.i(TAG, "--------------------------------------");
		
		logOut.println(" & " + mean + " & " + max + " & " + min + " & " + median + " & " + std);
		logOut.println("--------------------------------------");
	}
	
	public void startTesting(){
		initial(1024, 1024);
		this.multiplication();
		this.exponentiation();
		BigInteger[] c = this.encryptionPaillier();
		this.decryptionPaillier(c);
		logOut.close();
		
		initial(2048, 1024);
		this.multiplication();
		this.exponentiation();
		
		logOut.close();
	}
	
	public static void main(String[] str) {
		BigOperationTest test = new BigOperationTest(1024, 1024);
		test.exponentiation();
		test.multiplication();
		BigInteger[] c = test.encryptionPaillier();
		test.decryptionPaillier(c);
		
		test = new BigOperationTest(2048, 1024);
		test.exponentiation();
		test.multiplication();
		
		
		test.startTesting();
	}
}
