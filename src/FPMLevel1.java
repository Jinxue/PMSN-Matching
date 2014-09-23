/*
 * Level-1 Privacy for Fine-grained Private Matching (FPM)
 * @author Jinxue Zhang
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

public class FPMLevel1 extends FPMBasic{
	
	private static final String TAG = "FPMLevel1";

	/*
	 * This is the lazy construtor
	 */
	FPMLevel1(){
		
		int grn = 10;
		int nAtt = 10;
	
		Random random = new Random();
		int[] prof = new int[nAtt];
		/* Fill the profiles of sender and receiver randomly */
		for (int i = 0; i < nAtt; i ++)
			prof[i] = random.nextInt(grn);
		
		this.Init(grn, nAtt, prof);
	}
	
	
	FPMLevel1(int grn, int nAtt){
		
		Random random = new Random();
		int[] prof = new int[nAtt];
		/* Fill the profiles of sender and receiver randomly */
		for (int i = 0; i < nAtt; i ++)
			prof[i] = random.nextInt(grn);

		this.Init(grn, nAtt, prof);
	}

	/*
	 * We also accept the user-defined profiles
	 */
	FPMLevel1(int grn, int nAtt, int[] prof){
		this.Init(grn, nAtt, prof);
	}

	/*
	 * Constructor from Paillier File
	 */
	FPMLevel1(String fn){
		int grn = 10;
		int nAtt = 10;
	
		Random random = new Random();
		int[] prof = new int[nAtt];
		/* Fill the profiles of sender and receiver randomly */
		for (int i = 0; i < nAtt; i ++)
			prof[i] = random.nextInt(grn);
		

		// Load the Paillier data from file
		this.paillierFile = new PaillierFromBinFile(fn);
		this.paillier = this.paillierFile.getPaillier();
		
		this.Init(grn, nAtt, prof);
	}

	FPMLevel1(PaillierFromBinFile pf, int grn, int nAtt){
//		int grn = 10;
//		int nAtt = 10;
	
		Random random = new Random();
		int[] prof = new int[nAtt];
		/* Fill the profiles of sender and receiver randomly */
		for (int i = 0; i < nAtt; i ++)
			prof[i] = random.nextInt(grn);
		

		// Load the Paillier data from file
		this.paillierFile = pf;
		this.paillier = this.paillierFile.getPaillier();
		
		this.Init(grn, nAtt, prof);
	}

	/*
	 * The initialization function for FPM Level 1 protocol
	 */
	void Init(int grn, int nAtt, int[] prof){
		this.grain = grn;
		this.nAttributes = nAtt;
		this.nExtAttributes = (grn - 1) * nAtt;
		this.profile = prof;
		this.extendProfile();
		
		//paillier = new Paillier();
		this.n = paillier.n;
		this.g = paillier.g;

		Log.i(TAG, "Profile: " + printProfile(profile));
		Log.i(TAG, "Extended sending profile: " + printProfile(extSentProfile));
		Log.i(TAG, "Extended receiving profile: " + printProfile(extReceiveProfile));
	}

	/*
	 * Extend the original profile to a flat vecto, in both sending and receiving parts
	 */
	@Override
	void extendProfile(){
		
		this.extSentProfile = new int[nExtAttributes];
		this.extReceiveProfile = new int[nExtAttributes];
		
		for (int d = 0; d < this.nAttributes; d++){
			int lamda = profile[d];
			
			for (int j = 0; j < lamda; j ++){
				extSentProfile[d * (this.grain - 1) + j] = 1;
				extReceiveProfile[d * (this.grain - 1) + j] = 1;
			}
		}
	}

	/*
	 * As sender, build the sending message, which is the Paillier encryption output
	 */
	@Override
	byte[] buildBinSendingMessage(){
		//ByteArrayOutputStream baos = new ByteArrayOutputStream(nExtAttributes * 256);
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
		
		Log.i("TIME", "" + (end-start));
		
		return baos.toByteArray();
	}

	//@Override
	byte[] buildBinSendingMessageNew(){
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
		
		Log.i("TIME", "" + (end-start));
		
		return message;
	}

	private String buildMidResultbyRecWithPubKeys2(byte[] receiveMessage,
			BigInteger n, BigInteger g) {
		// TODO Auto-generated method stub
			
		byte[] item = new byte[256];
		//bais.read(item, 0, 256);
		// Firstly, compute E(u^.v^)
		BigInteger dotRes = Paillier.PublicPaillierEncryption(n, g, new BigInteger("0"));
		int srcPos = 0;
		long start = System.currentTimeMillis();
		
		for(int i = 0; i < nExtAttributes; i ++){
			
			if(this.extReceiveProfile[i] == 1){
				System.arraycopy(receiveMessage, srcPos, item, 0, 256);
				dotRes = dotRes.multiply(new BigInteger(item)).mod(n.multiply(n));
			}else 
				//bais.skip(256);
				srcPos += 256;
		}
		long end = System.currentTimeMillis();
		Log.i("TIME", "copy and multiple from the middle value building: "+ (end-start));
		

		srcPos = 0;
		start = System.currentTimeMillis();
		for(int i = 0; i < nExtAttributes; i ++){
			
			if(this.extReceiveProfile[i] == 1){
				System.arraycopy(receiveMessage, srcPos, item, 0, 256);
				//dotRes = dotRes.multiply(new BigInteger(item)).mod(n.multiply(n));
			}else 
				//bais.skip(256);
				srcPos += 256;
		}
		end = System.currentTimeMillis();
		Log.i("TIME", "only copy from the middle value building: "+ (end-start));

		// Secondly, compute the E((n-2)u^.v^)
		
		BigInteger powerDotRes = dotRes.modPow(n.subtract(new BigInteger("2")), n.multiply(n));
		
		// Thirdly, compute the receiver square
		int v2 = 0;
		for(int i = 0; i < nExtAttributes; i ++){
			v2 += this.extReceiveProfile[i] * this.extReceiveProfile[i];
		}
		BigInteger encV2 = Paillier.PublicPaillierEncryption(n, g, new BigInteger(Integer.toString(v2))); 
		
		// Finally, compute the matching result from receiver
		BigInteger matchMid = encV2.multiply(powerDotRes).mod(n.multiply(n));
		
		return matchMid.toString();

	}

	@Override
	String buildSendingMessage(){
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
	
	String[] buildSendingMessageArray(){
		String[] message = new String[nExtAttributes];
		
		for(int i = 0; i < nExtAttributes; i ++){
			//message[i] = paillier.Encryption(new BigInteger(Integer.toString(extSentProfile[i]))).toString();
			message[i] = this.paillierFile.getOneEncPaillierByGrain(extSentProfile[i]);
		}
		return message;
	}
	
	/*
	 * As a receiver, build the middle result 
	 */
	String buildMidResultbyRec(String senderMessage){
		// Split the message
		String [] extAttributs;
		extAttributs = senderMessage.split(",");
		
		// Firstly, compute E(u^.v^)
		BigInteger dotRes = paillier.Encryption(new BigInteger("0"));
		for(int i = 0; i < nExtAttributes; i ++){
			if(this.extReceiveProfile[i] == 1)
				dotRes = dotRes.multiply(new BigInteger(extAttributs[i])).mod(paillier.nsquare);
		}

		// Secondly, computer the exponential value
		BigInteger powerDotRes = dotRes.modPow(paillier.n.subtract(new BigInteger("2")), paillier.nsquare);
		
		// Thirdly, compute the receiver square
		int v2 = 0;
		for(int i = 0; i < nExtAttributes; i ++){
			v2 += this.extReceiveProfile[i] * this.extReceiveProfile[i];
		}
		BigInteger encV2 = paillier.Encryption(new BigInteger(Integer.toString(v2))); 
		
		// Finally, compute the matching result from receiver
		BigInteger matchMid = encV2.multiply(powerDotRes).mod(paillier.nsquare);
		
		return matchMid.toString();
	}
	
	/*
	 * Compute the middle value by using sender's public key <n, g>
	 */
	@Override
	String buildMidResultbyRecWithPubKeys(byte[] senderMessage, String n,
			String g) {
		// TODO Auto-generated method stub
		return buildMidResultbyRecWithPubKeys(senderMessage, new BigInteger(n), new BigInteger(g));
	}

	private String buildMidResultbyRecWithPubKeys(byte[] receiveMessage,
			BigInteger n, BigInteger g) {
		// TODO Auto-generated method stub
		ByteArrayInputStream bais = new ByteArrayInputStream(receiveMessage); 
		
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
		
		Log.i("TIME", "Old-copy and multiple from the middle value building: " + (end-start));

		bais.reset();
		
		start = System.currentTimeMillis();
		for(int i = 0; i < nExtAttributes; i ++){
			
			if(this.extReceiveProfile[i] == 1){
				bais.read(item, 0, 256);
				//dotRes = dotRes.multiply(new BigInteger(item)).mod(n.multiply(n));
			}else 
				bais.skip(256);
		}
		end = System.currentTimeMillis();
		
		Log.i("TIME", "Old-only copy from the middle value building: " + (end-start));

		// Secondly, compute the E((n-2)u^.v^)
		
		BigInteger powerDotRes = dotRes.modPow(n.subtract(new BigInteger("2")), n.multiply(n));
		
		// Thirdly, compute the receiver square
		int v2 = 0;
		for(int i = 0; i < nExtAttributes; i ++){
			v2 += this.extReceiveProfile[i] * this.extReceiveProfile[i];
		}
		BigInteger encV2 = Paillier.PublicPaillierEncryption(n, g, new BigInteger(Integer.toString(v2))); 
		
		// Finally, compute the matching result from receiver
		BigInteger matchMid = encV2.multiply(powerDotRes).mod(n.multiply(n));
		
		return matchMid.toString();

	}

	@Override
	String buildMidResultbyRecWithPubKeys(String senderMessage, String n, String g){
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
		
		// Secondly, compute the E((n-2)u^.v^)
		
		BigInteger powerDotRes = dotRes.modPow(n.subtract(new BigInteger("2")), n.multiply(n));
		
		// Thirdly, compute the receiver square
		int v2 = 0;
		for(int i = 0; i < nExtAttributes; i ++){
			v2 += this.extReceiveProfile[i] * this.extReceiveProfile[i];
		}
		BigInteger encV2 = Paillier.PublicPaillierEncryption(n, g, new BigInteger(Integer.toString(v2))); 
		
		// Finally, compute the matching result from receiver
		BigInteger matchMid = encV2.multiply(powerDotRes).mod(n.multiply(n));
		
		Log.i(TAG, "Build the middle message at the receiver.");
		return matchMid.toString();
	}

	
	/*
	 * As a sender, compute the final matching result after receiving the middle result.
	 */
	@Override
	int profileMatching(String matchMid){
				
		int u2 = 0;
		for(int i = 0; i < nExtAttributes; i ++){
			u2 += this.extSentProfile[i] * this.extSentProfile[i] ;
		}

		/* Since Paillier cryptographic system can not represent minus value directly, we need to add it with u^2 first
		 * to make sure that the result is non-negative 
		*/
		BigInteger encU2 = paillier.Encryption(new BigInteger(Integer.toString(u2))); 
		
		BigInteger partVal = paillier.Decryption(encU2.multiply(new BigInteger(matchMid)).mod(paillier.nsquare));
		
		Log.i(TAG, "Final matching score: " + partVal.intValue());
		return partVal.intValue();
	}
	
 	public static void main(String[] str) {
		FPMLevel1 fpm1 = new FPMLevel1("paillier2000b.txt");
		FPMLevel1 fpm2 = new FPMLevel1("paillier2000b.txt");
		
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
