/*
 * Max-distance matching (Protocol 4) for Fine-grained Private Matching (FPM)
 * @author Jinxue Zhang (jxzhang@asu.edu)
 * @date	February 11, 2012
 */
 
//package com.example.android.BluetoothChat;

import android.util.Log;

public class FPMLevel4 extends FPMLevel3 {

	private static final String TAG = "FPMLevel4";

	FPMLevel4(){
		super();
		threshold = 3;
		// Then compute the similarity socre Fi(u,v,t)
		simiScore();
		//System.out.println("Extended sending profile: " + printProfile(extSentProfile));
		Log.i(TAG, "Extended sending profile: " + printProfile(extSentProfile));
	}
	
	FPMLevel4(String fn){
		super(fn);
		threshold = 2;
		// Then compute the similarity socre Fi(u,v,t)
		simiScore();
		Log.i(TAG, "Extended sending profile: " + printProfile(extSentProfile));
	}
	
	FPMLevel4(PaillierFromBinFile pf, int grn, int nAtt){
		super(pf, grn, nAtt);
		threshold = 2;
		// Then compute the similarity socre Fi(u,v,t)
		simiScore();
		Log.i(TAG, "Extended sending profile: " + printProfile(extSentProfile));
	}
	
	void simiScore(){
		for (int i = 0; i < this.nExtAttributes; i++){
			this.extSentProfile[i] = (this.extSentProfile[i] <= this.threshold) ? 1 : 0;
		}
	}
	
	@Override
	void extendProfile() {
		// TODO Auto-generated method stub
		this.extSentProfile = new int[nExtAttributes];
		this.extReceiveProfile = new int[nExtAttributes];
		
		for (int d = 0; d < this.nAttributes; d++){
			int lamda = profile[d];
			
			for (int j = 0; j < this.grain; j ++)
				this.extSentProfile[d * this.grain + j] = Math.abs(lamda - j);
			
			this.extReceiveProfile[d * this.grain + lamda] = 1;
		}
	}

	byte[] buildBinSendingMessage() {
		// TODO Auto-generated method stub
		return super.buildBinSendingMessage(this.nAttributes);
	}

	@Override
	String buildSendingMessage() {
		// TODO Auto-generated method stub
		return super.buildSendingMessage(this.nAttributes);
	}
	
	int profileMatching(String matchMid) {
		// We need to inverse the result
		int ret = super.profileMatching(matchMid);
		return ret > 0 ? 0 : 1;
	}

 	public static void main(String[] str) {
		FPMLevel4 fpm1 = new FPMLevel4("paillier2000b.txt");
		FPMLevel4 fpm2 = new FPMLevel4("paillier2000b.txt");
		
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
