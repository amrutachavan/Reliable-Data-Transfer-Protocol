import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
/**
 * @author Amruta Chavan(agc9066)
 * This class is to create a main server
 * */
class UDPReceiver extends Thread{

	DatagramSocket UDPServerSocket;
	int UDPServerListenPort;
	UDPReceiver udpServerU;
	String fname;
	int exptSeqNo;
	boolean runServer;
	FileWriter fw;
	boolean isQuietMode;
	int packetCount;
	int totalCount=0;
	BufferedWriter bw;
	FileOutputStream foutput;
	String hshofFileRecieved;

	//constructor to initialize variables
	public UDPReceiver(int uport,boolean isQ){
		try{
			UDPServerListenPort =uport;
			UDPServerSocket = new DatagramSocket(UDPServerListenPort);    
			isQuietMode=isQ;
			fname="output.txt";
			exptSeqNo=0;
			runServer=true;
			packetCount=0;
			//	fw = new FileWriter(fname);
			fw = new FileWriter(fname,true);
			bw = new BufferedWriter(fw);
			foutput = new FileOutputStream(new File(fname));
		}catch(Exception e ){
			e.printStackTrace();
		}		
	}

	/**
	 * This is the run method
	 * */
	public void run(){
		System.out.println("UDP RECEIVER SERVER LISTENING ON PORT "+UDPServerListenPort);
		while(runServer){
			UDPServerListen();
		}	
		try {
			fw.close();
			bw.close();
			foutput.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("FILE RECIVE IS COMPLETE!!");
		String hashOfFileReceived=calculateMD5(fname);
		System.out.println("The MD5 hash is ::"+hshofFileRecieved);
		new File(fname).delete();
	}	

	//this method is used to accept request
	public void UDPServerListen(){
		try{			
			byte MessageData[] =new byte[2048];			
			//Datagram packet to receive packet
			DatagramPacket receivePacket = new DatagramPacket(MessageData, MessageData.length);
			//Receive packet on UDP socket
			UDPServerSocket.receive(receivePacket);
			//store the request receive time to calculate processing time

			System.out.println("\n\nREQUEST RECEIVED BY SERVER");
			replyFromServer(receivePacket);

		}catch(Exception e){
			e.printStackTrace();
		}

	}
	/*
	 * This function is to calculate checksum of each packet
	 * */
	public int calculateChecksum(byte data[],int read){
		int chk=0;
		for(int k=0;k<data.length;k++){
			chk=chk+data[k];
		}
		return ((((chk+13)*999+1000)%17)+53);
	}
	/*
	 * This function is to calculate the MD5 hash of the function
	 * */
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
	//method to reply to client
	public void replyFromServer(DatagramPacket receivePacket){

		try{
			Message min = new Message();
			byte data[]= receivePacket.getData();
			totalCount=min.ackNo;
			ByteArrayInputStream bip = new ByteArrayInputStream(data);
			ObjectInputStream oi = new ObjectInputStream(bip);
			min = (Message)oi.readObject();		
			hshofFileRecieved=min.msg;
			if(!isQuietMode){
				System.out.println("THE REQUEST RECEIVED IS ::");
				min.PrintMessageValues();
				System.out.println("CHECKSUM AT RECEIVE END IS ::"+calculateChecksum(min.getData(),min.getData().length));	
			}
			if(packetCount==min.ackNo){
				int ack=-10;
				runServer=false;
				Message mout = new Message();
				mout.setAckNo(ack);
				//construct the packet	
				ByteArrayOutputStream bop = new ByteArrayOutputStream();
				ObjectOutputStream op = new ObjectOutputStream(bop);
				op.flush();
				op.writeObject(mout);
				byte MessageData[] = bop.toByteArray();		
				DatagramPacket dpacket = new DatagramPacket(MessageData, MessageData.length,receivePacket.getAddress(),min.getAckPno());
				UDPServerSocket.send(dpacket);
				runServer=false;
			}

			if(min.seqNo==exptSeqNo&& (min.getChecksum()==calculateChecksum(min.getData(),min.getData().length))){
				if(!isQuietMode){
					System.out.println("PACKET RECEIVED IS NOT CORRUPT AND OF EXPECTED SEQUENCE\nSERVER IS WRITING TO FILE");
				}
				//set the ack for the packet
				packetCount++;
				int ack=min.getSeqNo()+1;
				//min.PrintMessageValues();					
				//String s = Arrays.toString(min.getData());
				byte[] s = min.getData();
				foutput.write(s);
				foutput.flush();
				//char[]dt=new String(s).toCharArray();
				//System.out.println(s);				
				//bw.write(dt);
				//bw.flush();
				//fw.close();
				//construct the message to be sent
				Message mout = new Message();
				mout.setAckNo(ack);
				//increment the expected sequence numbers
				exptSeqNo++;

				//construct the packet
				ByteArrayOutputStream bop = new ByteArrayOutputStream();
				ObjectOutputStream op = new ObjectOutputStream(bop);
				op.flush();
				op.writeObject(mout);
				byte MessageData[] = bop.toByteArray();		
				DatagramPacket dpacket = new DatagramPacket(MessageData, MessageData.length,receivePacket.getAddress(),min.getAckPno());

				if(!isQuietMode){
					System.out.println("SERVER SENDING REPLY WITH ACK::"+ack);
				}
				//send the reply message
				UDPServerSocket.send(dpacket);			
			}
			//if the file receive is complete then reply
			else{
				if(packetCount==totalCount){
					int ack=-10;
					runServer=false;
					Message mout = new Message();
					mout.setAckNo(ack);
					//construct the packet	
					ByteArrayOutputStream bop = new ByteArrayOutputStream();
					ObjectOutputStream op = new ObjectOutputStream(bop);
					op.flush();
					op.writeObject(mout);
					byte MessageData[] = bop.toByteArray();		
					DatagramPacket dpacket = new DatagramPacket(MessageData, MessageData.length,receivePacket.getAddress(),min.getAckPno());
					UDPServerSocket.send(dpacket);
				}
				else{
					if(!isQuietMode){
						System.out.println("PACKET RECEIVED IS CORRUPT OR NOT OF EXPECTED SEQUENCE\nSERVER IS SENDING ACK WITH EXPECTED SEQ NO::"+exptSeqNo+"\n");
					}
					int ack=exptSeqNo;
					//construct the message to be sent
					Message mout = new Message();
					mout.setAckNo(ack);
					//construct the packet	
					ByteArrayOutputStream bop = new ByteArrayOutputStream();
					ObjectOutputStream op = new ObjectOutputStream(bop);
					op.flush();
					op.writeObject(mout);
					byte MessageData[] = bop.toByteArray();		
					DatagramPacket dpacket = new DatagramPacket(MessageData, MessageData.length,receivePacket.getAddress(),min.getAckPno());

					if(!isQuietMode){
						System.out.println("SERVER SENDING REPLY WITH ACK::"+ack);
					}
					//send the reply message
					UDPServerSocket.send(dpacket);	
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}	

}