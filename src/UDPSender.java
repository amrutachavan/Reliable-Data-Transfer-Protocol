import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

/**
 * @author Amruta Chavan(agc9066)
 * This class is to create a udp client
 * */
public class UDPSender extends Thread {

	String serverAddress;

	static DatagramSocket UDPClientSocket,ackSoc;
	int serverPort;
	int clientPort;
	static int dataSize=-1;
	File inFile;
	String fname;
	int timeOut=1000;
	static ArrayList<Message> messageQ=new ArrayList<Message>();
	static ArrayList<DatagramPacket > waitQ = new ArrayList<DatagramPacket >();
	static ArrayList<Message> msgQ = new ArrayList<Message>();
	boolean isQuietMode;
	static int indx=0,win=1,cnt=0,ssthresh=355,resendCnt=0;;
	static float cwnd=1;
	static int packetCount=0;
	static Object listen=new Object();
	static int resend =-1;
	static int msgNoResend=-1;
	public int Type;
	public static int prevAck=0;
	public static int finalAck=0;
	public UDPSender(String ip,int sport,String fn,int time,boolean isQ){

		serverAddress= ip;
		timeOut=time;
		serverPort=sport;
		fname=fn;
		isQuietMode=isQ;
		//listen = new Object();
		try{
			System.out.println("");
			UDPClientSocket = new DatagramSocket();
			ackSoc= new DatagramSocket(7001);
			ackSoc.setSoTimeout(time);

			System.out.println("UDP SENDER CLIENT STARTED ON PORT "+UDPClientSocket.getLocalPort());
			constMessages();

		}catch(Exception e){
			//System.out.println("In timeout exception");
			e.printStackTrace();}
	}
	/**
	 * This method is to construct message from file and store it in message Queue
	 * */
	public void run(){
		if(this.Type==1){
			requestUDP();
		}
		else{

			responseUDP();
		}
	}
	public void constMessages(){
		int msgid=-1;
		byte [] data= new byte[1300];
		try{
			FileInputStream in = new FileInputStream(fname);
			int rc = in.read(data);
			while(rc != -1)
			{
				msgid++;
				dataSize++;
				Message mout = new Message();
				int checkSum;
				checkSum= calculateChecksum(data,rc);					
				mout.setMessageValues(msgid,dataSize,Arrays.copyOf(data,rc),checkSum);
				mout.setPno(7001);
				//add the messages to a queue
				messageQ.add(mout);		
				data= new byte[1300];
				rc = in.read(data); 
			}
		}catch(Exception e){}		
	}
	/*
	 * This method is to calculate checksum of the character array in the payload
	 * */

	public int calculateChecksum(byte data[],int read){
		int chk=0;
		for(int k=0;k<data.length;k++){
			chk=chk+data[k];
		}
		return ((((chk+13)*999+1000)%17)+53);
	}

	public String calculateMD5(String fname){
		String checksum = null;
		String hashtext="";
		try {
			FileInputStream fis = new FileInputStream(fname);
			byte[] buff = new byte[8192];
			MessageDigest md = MessageDigest.getInstance("MD5");

			//Using MessageDigest update() method to provide input

			int numOfBytesRead;
			while( (numOfBytesRead = fis.read(buff)) !=-1){
				md.update(buff, 0, numOfBytesRead);
			}
			byte[] hash = md.digest();

			BigInteger number = new BigInteger(1, hash);
			hashtext = number.toString(16);
			// Now we need to zero pad it if you actually want the full 32 chars.
			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}

		} catch (Exception ex) {

		} 
		return hashtext;
	}
	/**
	 * This method is to send packets to the server
	 * */	
	public void requestUDP(){

		ArrayList<Boolean> ack = new ArrayList<Boolean>();

		Message mout=null;
		int sentCount=0;

		DatagramPacket dpacket,dpacket1=null;
		try{
			System.out.println("\n\nSENDING CLIENT REQUEST");
			String hash1=calculateMD5(fname);
			while(finalAck!=-10){

				synchronized (listen) {
					if(resend==1){
						sentCount=packetCount;
					}
					if(!isQuietMode){
						System.out.println("THE CWND IS::"+cwnd);
					}
					//add the message to be sent to the waitQ. The number of messages should be equal to cwnd
					int k;
					for(k=sentCount;k<(((int)cwnd)+sentCount)&& k<messageQ.size();k++){//(int)cwnd && k<messageQ.size()
						mout= messageQ.get(k);
						mout.setWno(k);
						msgQ.add(mout);
						//messageQ.remove(0);
						mout.setAckNo(dataSize);
						mout.setHash(hash1);
						//create the data packet from the message
						InetAddress inetIP = InetAddress.getByName(serverAddress);
						//to get byte array of the object
						ByteArrayOutputStream bop = new ByteArrayOutputStream();
						ObjectOutputStream op = new ObjectOutputStream(bop);
						op.writeObject(mout);
						op.flush();
						byte MessageData[] = bop.toByteArray();
						int size = MessageData.length;
						dpacket = new DatagramPacket(MessageData, MessageData.length,inetIP,serverPort);
						waitQ.add(dpacket);
						cnt=0;				
						//sending all the packets
						if(!isQuietMode){
							System.out.println("\nTHE REQUEST SENT IS");
							mout.PrintMessageValues();

						}				
						UDPClientSocket.send(dpacket);
						//Thread.sleep(30);

						waitQ.remove(0);
						msgQ.remove(0);
					}
					sentCount=sentCount+k;

					listen.wait();
				}

			}
			System.out.println("FILE SEND IS COMPLETE!!");
			String hash2=calculateMD5(fname);
			System.out.println("The MD5 hash is ::"+hash2);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/*
	 * This method gets the response from the server
	 * */
	public void responseUDP(){
		int mId=-1;
		//int resendCnt;
		while(finalAck!=-10){							//while(packetCount<=dataSize)

			try{
				if(!isQuietMode){
					System.out.println("\nCLIENT WAITING FOR RESPONSE");
				}
				byte MessageData[] = new byte[2048];			
				//Datagram packet to receive packet
				DatagramPacket receivePacket = new DatagramPacket(MessageData, MessageData.length);
				//Receive packet on UDP socket
				ackSoc.receive(receivePacket);
				packetCount++;
				Message min= new Message();
				//Convert byte array to Message object again
				ByteArrayInputStream bip = new ByteArrayInputStream(receivePacket.getData());
				ObjectInputStream oi = new ObjectInputStream(bip);
				min = (Message)oi.readObject();	
				//get the ack no from the message
				mId=min.ackNo;
				finalAck=mId;
				//wait till ack for all packets is received
				//System.out.println("WaitQ size is::"+waitQ.size());

				try {					

					if(!isQuietMode&&mId!=-10){

						System.out.println("RECEIVED ACK::"+mId);
					}
					if(prevAck==mId){							

						resendCnt++;
						if(resendCnt==3){
							//resendCnt=0;
							synchronized(listen){
								ssthresh=Math.max(2,(int)cwnd/2);
								//reset cwnd to 1
								cwnd=1;
								if(!isQuietMode){
									System.out.println("RECEIVED 3 DUPLICATE INCORRECT ACK::"+mId);
									System.out.println("RESENDING PACKETS");
								}
								resendCnt=0;
								packetCount=mId;
								resend =1;
								msgNoResend=resendCnt;
								if(!isQuietMode){
									System.out.println("THE CWND IS RESET TO::"+cwnd+"\nTHE SSHTHRESH IS::"+ssthresh);
								}	
								listen.notify();
							}
						}
					}	
					else{
						synchronized(listen){
							resendCnt=0;
							//increment the cwnd
							if(cwnd<ssthresh){
								//increment the command window by one
								cwnd=cwnd+1;
								if(!isQuietMode){
									System.out.println("THE CWND IS INCREMENTED TO::"+cwnd);	
								}
								listen.notify();
							}else{
								cwnd=cwnd+(1/cwnd);
								if(!isQuietMode){
									System.out.println("THE CWND IS INCREMENTED TO::"+cwnd);
								}
								listen.notify();
							}
						}

					}
					//if timeout happens then -5 is returned

				}catch(Exception e){
					e.printStackTrace();
				} 
				prevAck=mId;

			}catch(Exception e){
				//timeout code here		
				synchronized(listen){
					if(!isQuietMode){
						System.out.println("TIMEOUT OCCURED SO RESENDING PACKETS");
					}
					resendCnt=0;
					//set threshold to cwnd/2
					ssthresh=Math.max(2,(int)cwnd/2);
					//reset cwnd to 1
					cwnd=1;
					resend =1;
					packetCount=prevAck;
					msgNoResend=resendCnt;
					if(!isQuietMode){
						System.out.println("THE CWND IS RESET TO::"+cwnd+"\nTHE SSHTHRESH IS::"+ssthresh);
					}
					listen.notify();
				}
			}
		}
	}

}