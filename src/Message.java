import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

/*
 * @author Amruta Chavan agc9066
 * The message object is used to store message details
 * */
class Message implements Serializable{
	
	int seqNo;
	int ackNo;
	//char data[];
	byte[] data;
	int checkSum;
	int wno;
	int port2;
	String msg;
	public Message(){
		data=new byte[1300];
	}
	
	/*
	 * set the message attributes*/
	public void setMessageValues(int msgid,int aN,byte b[],int c){		
		seqNo = msgid;
		ackNo =aN;
		data=b;
		checkSum=c;
	}
	/*
	 * method to getSeqNo*/
	public void setHash(String h){
		msg=h;
	}
	public int getSeqNo(){
		return seqNo;
	}
	public int getWNo(){
		return wno;
	}
	public void setWno(int Wno){
		wno=Wno;
	}
	
	public int getAckPno(){
		return port2;
	}	
	public void setPno(int Wno){
		port2=Wno;
	}
	
	public int getAckNo(){
		return ackNo;
	}	
	public byte[] getData(){
		return data;
	}
	
	public int getChecksum(){
		return checkSum;
	}
	
	public void setSeqNo(int s){
		seqNo=s;		
	}
	
	public void setAckNo(int a){
		ackNo=a;
	}
	
	public void setdata(byte d[]){
		data= d;
	}
	
	public void setCheckSum(int c){
		checkSum=c;
	}
	public void PrintMessageValues(){
		System.out.println("SeqId\tCheckSum\t");
		System.out.println(seqNo+"\t"+checkSum);		
	}
}