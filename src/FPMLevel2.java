/*
 * Level-2 privacy for Fine-grained Private Matching (FPM)
 * @author Jinxue Zhang (jxzhang@asu.edu)
 * @date	February 10, 2012
 */

//package com.example.android.BluetoothChat;


import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import android.util.Log;

public class FPMLevel2 extends FPMBasic {

	private static final String TAG = "FPMLevel2";

	int[] weightedList;
	
	FPMLevel2(){
		this.Init();
	}
	
	/*
	 * Constructor from Paillier File
	 */
	FPMLevel2(String fn){
		// Load the Paillier data from file
		this.paillierFile = new PaillierFromBinFile(fn);
		this.paillier = this.paillierFile.getPaillier();
		this.Init();
	}

	FPMLevel2(PaillierFromBinFile pf, int grn, int nAtt){
		// Load the Paillier data from file
		this.paillierFile = pf;
		this.paillier = this.paillierFile.getPaillier();
		this.Init(grn, nAtt);
	}

	
	/*
	 * The initialization function for FPM Level 2 protocol
	 */
	void Init(int grn, int nAtt){
//		int grn = 5;
//		int nAtt = 2;
	
		Random random = new Random();
		int[] prof = new int[nAtt];
		int[] weighted = new int[nAtt];
		
		/* Fill the profiles of sender and receiver randomly */
		for (int i = 0; i < nAtt; i ++){
			prof[i] = random.nextInt(grn);
			weighted[i] = random.nextInt(3);
		}
		
		this.grain = grn;
		this.nAttributes = nAtt;
		this.nExtAttributes = grn * nAtt;
		this.profile = prof;
		this.weightedList = weighted;
		this.extendProfile();
		
		//paillier = new Paillier();
		this.n = paillier.n;
		this.g = paillier.g;

		System.out.println("Profile: " + printProfile(profile));
		System.out.println("Profile weight: " + printProfile(this.weightedList));

		System.out.println("Extended sending profile: " + printProfile(extSentProfile));
		System.out.println("Extended receiving profile: " + printProfile(extReceiveProfile));
		}

	void Init(){
		int grn = 5;
		int nAtt = 2;
	
		Random random = new Random();
		int[] prof = new int[nAtt];
		int[] weighted = new int[nAtt];
		
		/* Fill the profiles of sender and receiver randomly */
		for (int i = 0; i < nAtt; i ++){
			prof[i] = random.nextInt(grn);
			weighted[i] = random.nextInt(3);
		}
		
		this.grain = grn;
		this.nAttributes = nAtt;
		this.nExtAttributes = grn * nAtt;
		this.profile = prof;
		this.weightedList = weighted;
		this.extendProfile();
		
		//paillier = new Paillier();
		this.n = paillier.n;
		this.g = paillier.g;

		System.out.println("Profile: " + printProfile(profile));
		System.out.println("Profile weight: " + printProfile(this.weightedList));

		System.out.println("Extended sending profile: " + printProfile(extSentProfile));
		System.out.println("Extended receiving profile: " + printProfile(extReceiveProfile));
		}

	@Override
	void extendProfile() {
		// TODO Auto-generated method stub
		this.extSentProfile = new int[nExtAttributes];
		this.extReceiveProfile = new int[nExtAttributes];
		
		for (int d = 0; d < this.nAttributes; d++){
			int lamda = profile[d];
			
			for (int j = 0; j < this.grain; j ++)
				this.extSentProfile[d * this.grain + j] = Math.abs(lamda - j); //* this.weightedList[d];
			
			this.extReceiveProfile[d * this.grain + lamda] = 1;
		}
	}

	/*
	 * As sender, build the sending message, which is the Paillier encryption output
	 */
	/*@Override
	byte[] buildBinSendingMessage(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		
		for(int i = 0; i < nExtAttributes; i ++){
			//message += paillier.Encryption(new BigInteger(Integer.toString(extSentProfile[i]))).toString();
			try {
				baos.write(this.paillierFile.getOneByteEncPaillierByGrain(extSentProfile[i]));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Add a separating character
		}
		
		return baos.toByteArray();
	}*/

	//@Override
	byte[] buildBinSendingMessageOld(){
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		//BufferedOutputStream bos = new BufferedOutputStream(baos);
		
		
		long start = System.currentTimeMillis();
		for(int i = 0; i < nExtAttributes; i ++){
			//message += paillier.Encryption(new BigInteger(Integer.toString(extSentProfile[i]))).toString();
			try {
				baos.write(this.paillierFile.getOneByteEncPaillierByGrain(extSentProfile[i]));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Add a separating character
		}
		
		long end = System.currentTimeMillis();
		
		Log.i("TIME", "Protocol2-" + (end-start));
		
		return baos.toByteArray();
	}

	@Override
	byte[] buildBinSendingMessage(){
		byte[] message = new byte[nExtAttributes * 256];
		
		int destPos = 0;
		
		long start = System.currentTimeMillis();
		for(int i = 0; i < nExtAttributes; i ++){
			//message[i] = paillier.Encryption(new BigInteger(Integer.toString(extSentProfile[i]))).toString();
			//message[i] = this.paillierFile.getOneEncPaillierByGrain(extSentProfile[i]);
			System.arraycopy(this.paillierFile.getOneByteEncPaillierByGrain(extSentProfile[i]), 0, message, destPos, 256);
			destPos += 256;
		}
		long end = System.currentTimeMillis();
		
		Log.i("TIME", "Protocol 2 - new-" + (end-start));
		
		return message;
	}

	
	String buildSendingMessage() {
		// TODO Auto-generated method stub
		StringBuilder message = new StringBuilder();
		
		for(int i = 0; i < nExtAttributes; i ++){
			//message += paillier.Encryption(new BigInteger(Integer.toString(extSentProfile[i]))).toString();
			message.append(this.paillierFile.getOneEncPaillierByGrain(extSentProfile[i]));

			// Add a separating character
			message.append(",");
		}
		
		int len = message.length();
		message = message.deleteCharAt(len - 1);
		
		Log.i(TAG, "Build the sending message with length " + len);
		return message.toString();
	}

	/*
	 * As a receiver, build the middle result 
	 */
	@Override
	String buildMidResultbyRecWithPubKeys(byte[] senderMessage, String n,
			String g) {
		// TODO Auto-generated method stub
		return buildMidResultbyRecWithPubKeys2(senderMessage, new BigInteger(n), new BigInteger(g));
	}

	String buildMidResultbyRecWithPubKeys(byte[] senderMessage, BigInteger n, BigInteger g){
		
		ByteArrayInputStream bais = new ByteArrayInputStream(senderMessage); 
		
		byte[] item = new byte[256];
		//bais.read(item, 0, 256);
		// Firstly, compute E(u^.v^)
		BigInteger dotRes = Paillier.PublicPaillierEncryption(n, g, new BigInteger("0"));
		bais.mark(nExtAttributes * 512);

		long start = System.currentTimeMillis();
		for(int i = 0; i < nExtAttributes; i ++){
			
			if(this.extReceiveProfile[i] == 1){
				bais.read(item, 0, 256);
				dotRes = dotRes.multiply(new BigInteger(item)).mod(n.multiply(n));
			}else 
				bais.skip(256);
		}
		long end = System.currentTimeMillis();
		
		Log.i("TIME", "2Old-copy and multiple from the middle value building: " + (end-start));

		bais.reset();
		
		start = System.currentTimeMillis();
		@SuppressWarnings("unused")
		BigInteger temp = null;
		for(int i = 0; i < nExtAttributes; i ++){
			
			if(this.extReceiveProfile[i] == 1){
				bais.read(item, 0, 256);
				temp = new BigInteger(item);
				//dotRes = dotRes.multiply(new BigInteger(item)).mod(n.multiply(n));
			}else 
				bais.skip(256);
		}
		end = System.currentTimeMillis();
		
		Log.i("TIME", "Old-only copy from the middle value building: " + (end-start));


		
//		// Firstly, compute E(u^.v^)
//		BigInteger dotRes = Paillier.PublicPaillierEncryption(n, g, new BigInteger("0"));
//		for(int i = 0; i < nExtAttributes; i ++){
//			if(this.extReceiveProfile[i] == 1)
//				dotRes = dotRes.multiply(new BigInteger(extAttributs[i])).mod(n.multiply(n));
//		}
		
		// Secondly, compute the E(u^.v^) with one more encryption
		
		BigInteger moreDotRes = Paillier.PublicMoreEncryption(n, g, dotRes);
		
		return moreDotRes.toString();
	}
	
	String buildMidResultbyRecWithPubKeys2(byte[] senderMessage, BigInteger n, BigInteger g){
		
		//ByteArrayInputStream bais = new ByteArrayInputStream(senderMessage); 

		int srcPos = 0;
		
		byte[] item = new byte[256];
		//bais.read(item, 0, 256);
		// Firstly, compute E(u^.v^)
		BigInteger dotRes = Paillier.PublicPaillierEncryption(n, g, new BigInteger("0"));
		BigInteger nsquare = n.multiply(n);
		long start = System.currentTimeMillis();
		@SuppressWarnings("unused")
		BigInteger temp;
		for(int i = 0; i < nExtAttributes; i ++){
			
			if(this.extReceiveProfile[i] == 1){
				System.arraycopy(senderMessage, srcPos, item, 0, 256);
				//dotRes = dotRes.multiply(new BigInteger(item)).mod(n.multiply(n));
				temp = new BigInteger(item);
			}else 
				//bais.skip(256);
				srcPos += 256;
		}
		long end = System.currentTimeMillis();
		Log.i("TIME", "2New copy from the middle value building: "+ (end-start));

		srcPos = 0;
		start = System.currentTimeMillis();
		int num = 0;
		for(int i = 0; i < nExtAttributes; i ++){
			
			
			if(this.extReceiveProfile[i] == 1){
				//bais.read(item, 0, 256);
				System.arraycopy(senderMessage, srcPos, item, 0, 256);
				dotRes = dotRes.multiply(new BigInteger(item)).mod(nsquare);
				num ++;
			}else 
				//bais.skip(256);
				srcPos += 256;
		}
		end = System.currentTimeMillis();
		
		Log.i("TIME", num + " -2New-copy and multiple from the middle value building: " + (end-start));
//		// Firstly, compute E(u^.v^)
//		BigInteger dotRes = Paillier.PublicPaillierEncryption(n, g, new BigInteger("0"));
//		for(int i = 0; i < nExtAttributes; i ++){
//			if(this.extReceiveProfile[i] == 1)
//				dotRes = dotRes.multiply(new BigInteger(extAttributs[i])).mod(n.multiply(n));
//		}
		
		// Secondly, compute the E(u^.v^) with one more encryption
		
		BigInteger moreDotRes = Paillier.PublicMoreEncryption(n, g, dotRes);
		
		return moreDotRes.toString();
	}

	
	@Override
	String buildMidResultbyRecWithPubKeys(String senderMessage, String n,
			String g) {
		// TODO Auto-generated method stub
		return buildMidResultbyRecWithPubKeys(senderMessage, new BigInteger(n), new BigInteger(g));
	}
	
	String buildMidResultbyRecWithPubKeys(String senderMessage, BigInteger n, BigInteger g){
	
		// Split the message
		String [] extAttributs;
		extAttributs = senderMessage.split(",");
		
		// Firstly, compute E(u^.v^)
		BigInteger dotRes = Paillier.PublicPaillierEncryption(n, g, new BigInteger("0"));
		for(int i = 0; i < nExtAttributes; i ++){
			if(this.extReceiveProfile[i] == 1)
				dotRes = dotRes.multiply(new BigInteger(extAttributs[i])).mod(n.multiply(n));
		}
		
		// Secondly, compute the E(u^.v^) with one more encryption
		
		BigInteger moreDotRes = Paillier.PublicMoreEncryption(n, g, dotRes);
		
		Log.i(TAG, "Build the middle message at the receiver.");
		return moreDotRes.toString();
	}

	@Override
	int profileMatching(String matchMid) {
		// TODO Auto-generated method stub
		BigInteger partVal = paillier.Decryption(new BigInteger(matchMid));
		
		Log.i(TAG, "Final matching score: " + partVal.intValue());
		return partVal.intValue();
	}

 	public static void main(String[] str) {
		FPMLevel2 fpm1 = new FPMLevel2("paillier2000b.txt");
		FPMLevel2 fpm2 = new FPMLevel2("paillier2000b.txt");
		
		//String sendMessage = fpm1.buildSendingMessage();
		//String receiveMessage = sendMessage;
		//String midValue = fpm2.buildMidResultbyRecWithPubKeys(receiveMessage, fpm1.n, fpm1.g);
		
		byte[] sendMessage = fpm1.buildBinSendingMessage();
		byte[] receiveMessage = sendMessage;
		
		
		String midValue = fpm2.buildMidResultbyRecWithPubKeys(receiveMessage, fpm1.n, fpm1.g);
		
		int finalValue = fpm1.profileMatching(midValue);
		
		System.out.println(finalValue);
	}
}
