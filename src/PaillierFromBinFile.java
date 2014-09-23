/*
 * Load the Paillier data from the binary file
 * @author Jinxue Zhang
 * @date	February 11, 2012
 */
//package com.example.android.BluetoothChat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import android.os.Environment;

public class PaillierFromBinFile {
	
	String fileName = "paillierEnc";
	
	int grain;
	
	int nAttr;
	
	BigInteger n, g, lambda;
	
	private Paillier paillier;
	
	private String[][] paillierEnc; 
	private byte[][][] paillierByte;
	
	// Load from file
	PaillierFromBinFile(String fn){
		fileName = fn;
		loadPaillierFromBinFile();
		paillier = new Paillier(n, g, lambda);
	}
	
	void loadPaillierFromBinFile(){
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
			//paillierEnc = new String[grain][nAttr];
			paillierByte = new byte[grain][nAttr][256];
			
			FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory()+ "/" +  "data-" + fileName);
			
			DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));

			for (int i = 0; i < grain; i ++){
				for (int j = 0; j < nAttr; j ++){
					byte[] b = new byte[256];
					//paillierEnc[i][j] = br.readLine();
					dis.read(b);
					paillierByte[i][j] = b;
				}
			}
			fr.close();
			dis.close();
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
			System.out.println("The grain exceeds!");
			return null;
		}
		
		Random rand = new Random();
		return this.paillierEnc[gn][rand.nextInt(this.nAttr)];
	}

	public byte[] getOneByteEncPaillierByGrain(int gn){
		if(gn > this.grain - 1){
			System.out.println("The grain exceeds!");
			return null;
		}
		
		Random rand = new Random();
		return this.paillierByte[gn][rand.nextInt(this.nAttr)];
	}

	public static void main(String[] args){
		PaillierFromBinFile pf = new PaillierFromBinFile("paillier2000b.txt");
		Paillier paillier = pf.getPaillier();
		
		
		
//		for (int i = 0; i < 20; i ++){
//			String str = pf.getOneEncPaillierByGrain(i);
//			byte[] by = str.getBytes();
//			
//			System.out.println(by.length);
//			
//			by = (new BigInteger(str)).toByteArray();
//			System.out.println(by.length);
//			
//			BigInteger res = paillier.Decryption(new BigInteger(by));
//			System.out.println(res);
//		}

		for (int i = 0; i < 20; i ++){
			byte[] by = pf.getOneByteEncPaillierByGrain(i);
			
			System.out.println(by.length);
			
			BigInteger res = paillier.Decryption(new BigInteger(by));
			System.out.println(res);
		}
}
}
