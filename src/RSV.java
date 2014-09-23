
/*
 * For comparison, we implemented RSV[15] in the paper
 *
 * @author Jinxue Zhang
 * @date	February 12, 2012
 */


//package com.example.android.BluetoothChat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class RSV extends FPMBasic {

	
	/*
	 * Constructor from Paillier File
	 */
	RSV(String fn){
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

	RSV(PaillierFromBinFile pf, int grn, int nAtt){
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

		System.out.println("Profile: " + printProfile(profile));
		System.out.println("Extended sending profile: " + printProfile(extSentProfile));
		System.out.println("Extended receiving profile: " + printProfile(extReceiveProfile));
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
	}

	@Override
	String buildMidResultbyRecWithPubKeys(byte[] senderMessage, String n,
			String g) {
		// TODO Auto-generated method stub
		return buildMidResultbyRecWithPubKeys(senderMessage, new BigInteger(n), new BigInteger(g));
	}

	/*
	 * Compute the middle value by using sender's public key <n, g>
	 */
	private String buildMidResultbyRecWithPubKeys(byte[] receiveMessage,
			BigInteger n, BigInteger g) {
		// TODO Auto-generated method stub
		ByteArrayInputStream bais = new ByteArrayInputStream(receiveMessage); 
		
		byte[] item = new byte[256];
		//bais.read(item, 0, 256);
		// Firstly, compute -2ab
		BigInteger dotRes = Paillier.PublicPaillierEncryption(n, g, new BigInteger("0"));
		for(int i = 0; i < nExtAttributes; i ++){
			
			bais.read(item, 0, 256);
			
			// b_ba = -2b
			/*BigInteger b_ba = new BigInteger("0");
			
			if(this.extReceiveProfile[i] == 1){
				b_ba = b_ba.subtract(new BigInteger("2"));
			}*/
			
			BigInteger b_ba = n;
			
			if(this.extReceiveProfile[i] == 1){
				b_ba = b_ba.subtract(new BigInteger("2")).mod(n);
			}

			dotRes = dotRes.multiply(new BigInteger(item).modPow(b_ba, n.multiply(n))).mod(n.multiply(n));
		}
		
		// Secondly, compute the receiver square
		int v2 = 0;
		for(int i = 0; i < nExtAttributes; i ++){
			v2 += this.extReceiveProfile[i] * this.extReceiveProfile[i];
		}
		BigInteger encV2 = Paillier.PublicPaillierEncryption(n, g, new BigInteger(Integer.toString(v2))); 
		
		// Finally, compute the matching result from receiver
		BigInteger matchMid = encV2.multiply(dotRes).mod(n.multiply(n));
		
		return matchMid.toString();

	}

	@Override
	String buildMidResultbyRecWithPubKeys(String senderMessage, String n,
			String g) {
		// TODO Auto-generated method stub
		return null;
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
		
		System.out.println("Final matching score: " + partVal.intValue());
		return partVal.intValue();
	}
	
 	public static void main(String[] str) {
		RSV fpm1 = new RSV("paillier2000b.txt");
		RSV fpm2 = new RSV("paillier2000b.txt");
		
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
