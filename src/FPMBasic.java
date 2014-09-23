//package com.example.android.BluetoothChat;

import java.math.BigInteger;

/*
 * This is the basic class for Fine-grained Private Matching (FPM)
 * @author Jinxue Zhang
 * @date	February 9, 2012
 */

public abstract class FPMBasic {

	// The range of grain
	int grain;
	
	// The number of attributes in the vector
	int nAttributes;
	
	// the number of extended attributs
	int nExtAttributes;
	
	/* instantiating an object of Paillier cryptosystem */
	Paillier paillier;
	
	/*  The original profile */
	int[] profile;
	
	/*  The extended profile for sending and receiving parts */
	int[] extSentProfile, extReceiveProfile;
	
	/* The public keys for Paillier Cryptographic system */
	public BigInteger n, g;
	
	/* The Paillier sets from file */
	public PaillierFromBinFile paillierFile;


	String printProfile(int[] profile){
		String str = new String();
		
		str = "[";
		for(int i = 0; i < profile.length; i ++)
			str += profile[i] + " ,";
		
		str += "]";
		return str;
	}

	/*
	 * Extend the original profile to a flat vector
	 */
	abstract void extendProfile();
	
	/*
	 * The whole process is composed of three steps between the sender Alice and the receiver Bob
	 */
	/*
	 * As sender, Alice builds the sending message, which is the Paillier encryption output
	 */
	abstract byte[] buildBinSendingMessage();
	
	/*
	 * As a receiver, Bob computes the middle value by using sender's public key <n, g>
	 */
	abstract String buildMidResultbyRecWithPubKeys(String senderMessage, String n, String g);

	abstract String buildMidResultbyRecWithPubKeys(byte[] senderMessage, String n, String g);

	/*
	 * As a sender, Alice computes the final matching result after receiving the middle result.
	 */
	abstract int profileMatching(String matchMid);

	String buildSendingMessage() {
		// TODO Auto-generated method stub
		return null;
	}
}
