/*
 * Load the Paillier data from the ASCII file
 * @author Jinxue Zhang
 * @date	February 11, 2012
 */
//package com.example.android.BluetoothChat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import android.os.Environment;
import android.util.Log;

public class PaillierFromFile {
	
	final static String TAG = "PFM";
	
	String fileName = "paillierEnc";
	
	int grain;
	
	int nAttr;
	
	BigInteger n, g, lambda;
	
	private Paillier paillier;
	
	private String[][] paillierEnc; 
	
	// Load from file
	PaillierFromFile(String fn){
		fileName = fn;
		loadPaillierFromFile();
		paillier = new Paillier(n, g, lambda);
	}
	
	void loadPaillierFromFile(){
		try {
			FileReader fr = new FileReader(Environment.getExternalStorageDirectory()+ "/" + fileName);
			BufferedReader br = new BufferedReader(fr);
			// The first line is N, G, and Lambda
			String[] cryptoPara = br.readLine().split(",");
			n = new BigInteger(cryptoPara[0]);
			g = new BigInteger(cryptoPara[1]);
			lambda = new BigInteger(cryptoPara[2]);
			
			//System.out.println("N: " + n + "with bit length: " + n.bitLength());
			//System.out.println("G: " + g + "with bit length: " + g.bitLength());
			//System.out.println("Lambda: " + lambda);
			
			// The second line is grain, #attributes
			String[] fpmPara = br.readLine().split(",");
			grain = Integer.parseInt(fpmPara[0]);
			nAttr = Integer.parseInt(fpmPara[1]);
			
			// The rest of the file is the encrypted items
			paillierEnc = new String[grain][nAttr];
			for (int i = 0; i < grain; i ++){
				for (int j = 0; j < nAttr; j ++){
					paillierEnc[i][j] = br.readLine();
				}
			}
			fr.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	 * Get the Paillier instance
	 */
	public Paillier getPaillier(){
		return this.paillier;
	}
	
	/*
	 * Get an encrypted Paillier item
	 */
	public String getOneEncPaillierByGrain(int gn){
		if(gn > this.grain - 1){
			Log.i(TAG, "The grain exceeds!");
			return null;
		}
		
		Random rand = new Random();
		return this.paillierEnc[gn][rand.nextInt(this.nAttr)];
	}
	
	public static void main(String[] args){
		PaillierFromFile pf = new PaillierFromFile("paillier.txt");
		Paillier paillier = pf.getPaillier();
		
		for (int i = 0; i < 10; i ++){
			BigInteger res = paillier.Decryption(new BigInteger(pf.getOneEncPaillierByGrain(i)));
			System.out.println(res);
		}
	}
}
