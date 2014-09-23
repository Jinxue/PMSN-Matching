/*
 * Level-privacy for Fine-grained Private Matching (FPM)
 * @author Jinxue Zhang (jxzhang@asu.edu)
 * @date	February 9, 2012
 */
//package com.example.android.BluetoothChat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import android.util.Log;

public class FPMLevel3 extends FPMLevel2 {

	private static final String TAG = "FPMLevel3";
	
	/*
	 * The complementary variables for Level 3 protocol
	 * which satisfy sigma > sigma1> sigma2 >= 0
	 */
	int sigma, sigma1, sigma2;
	
	/*
	 * The threshold in level 3 protocol
	 */
	int threshold;

	FPMLevel3(){
		super();
		sigma = 5; 
		sigma1 = 4;
		sigma2 = 2;
		
		threshold = 5;
		Log.i(TAG, "Threshold: " + this.threshold);
	}
	
	FPMLevel3(String fn){
		super(fn);
		sigma = 5; 
		sigma1 = 4;
		sigma2 = 2;
		
		threshold = 5;
		System.out.println("Threshold: " + this.threshold);
	}

	FPMLevel3(PaillierFromBinFile pf, int grn, int nAtt){
		super(pf, grn, nAtt);
		sigma = 5; 
		sigma1 = 4;
		sigma2 = 2;
		
		threshold = 5;
		System.out.println("Threshold: " + this.threshold);
	}

	byte[] buildBinSendingMessage() {
		return this.buildBinSendingMessage(threshold);
	}
	
	byte[] buildBinSendingMessage(int value) {
		// TODO Auto-generated method stub
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		
		try {
			baos.write(paillier.Encryption(new BigInteger(Integer.toString(value))).toByteArray());
			baos.write(super.buildBinSendingMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	
	@Override
	String buildSendingMessage() {
		// TODO Auto-generated method stub
		return this.buildSendingMessage(this.threshold);
	}

	String buildSendingMessage(int value){
		String message = "";
		
		// We first need to send the threshold
		message = paillier.Encryption(new BigInteger(Integer.toString(value))).toString();

		// Then build the encrypted message like Protocol 2
		message = message + ";" + super.buildSendingMessage();
		
		return message;
		
	}

	String buildMidResultbyRecWithPubKeys(byte[] senderMessage, BigInteger n,
			BigInteger g) {
		// TODO Auto-generated method stub

		// The message is composed of both encrypted threshold and encrypted sending message
		//String[] strs = senderMessage.split(";");
		ByteArrayInputStream bais = new ByteArrayInputStream(senderMessage);
		byte[] th = new byte[256];
		bais.read(th, 0, 256);
		BigInteger thres =  new BigInteger(th);
		
		byte[] data = new byte[senderMessage.length - 256];
		bais.read(data, 0, senderMessage.length - 256);
		
		// First compute the E(sigma * U*V + sigma1)		
		BigInteger uvDot = new BigInteger(super.buildMidResultbyRecWithPubKeys(data, n, g));
		BigInteger sigmaDot = uvDot.modPow(new BigInteger(Integer.toString(this.sigma)), n.multiply(n));
		
		BigInteger encrySigma1 = Paillier.PublicPaillierEncryption(n, g, new BigInteger(Integer.toString(this.sigma1)));
		BigInteger sigmaUV = sigmaDot.multiply(encrySigma1).mod(n.multiply(n));
		
		// Then compute the E(sigma * threshold + sigma2)
		BigInteger sigmaThres = thres.modPow(new BigInteger(Integer.toString(this.sigma)), n.multiply(n));
		BigInteger encrySigma2 = Paillier.PublicPaillierEncryption(n, g, new BigInteger(Integer.toString(this.sigma2))); 
		BigInteger sigmaThresPlus = sigmaThres.multiply(encrySigma2).mod(n.multiply(n));
		
		return sigmaUV.toString() + "," + sigmaThresPlus.toString();
	}

	@Override
	String buildMidResultbyRecWithPubKeys(String senderMessage, BigInteger n,
			BigInteger g) {
		// TODO Auto-generated method stub

		// The message is composed of both encrypted threshold and encrypted sending message
		String[] strs = senderMessage.split(";");
		
		BigInteger thres =  new BigInteger(strs[0]);
		
		// First compute the E(sigma * U*V + sigma1)		
		BigInteger uvDot = new BigInteger(super.buildMidResultbyRecWithPubKeys(strs[1], n, g));
		BigInteger sigmaDot = uvDot.modPow(new BigInteger(Integer.toString(this.sigma)), n.multiply(n));
		
		BigInteger encrySigma1 = Paillier.PublicPaillierEncryption(n, g, new BigInteger(Integer.toString(this.sigma1)));
		BigInteger sigmaUV = sigmaDot.multiply(encrySigma1).mod(n.multiply(n));
		
		// Then compute the E(sigma * threshold + sigma2)
		BigInteger sigmaThres = thres.modPow(new BigInteger(Integer.toString(this.sigma)), n.multiply(n));
		BigInteger encrySigma2 = Paillier.PublicPaillierEncryption(n, g, new BigInteger(Integer.toString(this.sigma2))); 
		BigInteger sigmaThresPlus = sigmaThres.multiply(encrySigma2).mod(n.multiply(n));
		
		return sigmaUV.toString() + "," + sigmaThresPlus.toString();
	}

	@Override
	int profileMatching(String matchMid) {
		// TODO Auto-generated method stub
		// The matching message is composed of E(sigma * U*V + sigma1) and E(sigma * threshold + sigma2), 
		// They are separate by ","
		String[] encrypts = matchMid.split(",");
		BigInteger sigmaDot = paillier.Decryption(new BigInteger(encrypts[0]));
		BigInteger sigmaThresd = paillier.Decryption(new BigInteger(encrypts[1]));
		
		int ret = (sigmaDot.intValue() <= sigmaThresd.intValue()) ? 1 : 0;
		Log.i(TAG, "Compute the final matching score in the sender with result " + ret);
		return ret;
	}

 	public static void main(String[] str) {
		FPMLevel3 fpm1 = new FPMLevel3("paillier2000b.txt");
		FPMLevel3 fpm2 = new FPMLevel3("paillier2000b.txt");
		
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
