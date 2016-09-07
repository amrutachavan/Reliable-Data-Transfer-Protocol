import java.net.InetAddress;
/*
 * @author Amruta Chavan agc9066
 * 
 * This class is to run the main program
 * */

public class fcntcp {

	public static void main(String []args){
		String opt = args[0];
		int p=1;
		switch(opt){
		//if server is to be started
		case ("-s") :
			int srvPort;
			boolean quiet=false;
			srvPort=Integer.parseInt(args[args.length-1]);
			if(args[p].equals("-q")){
				quiet=true;
			}	
			UDPReceiver udpServer;
	
			try{
				String tsIp = InetAddress.getLocalHost().getHostAddress();
				System.out.println("\nTHE IP OF MAIN SERVER IS::"+tsIp+"\n");		
			}catch(Exception e){}
			udpServer = new UDPReceiver(srvPort,quiet);
			Thread udp = new Thread(udpServer);
			udp.start();	
	
			break;
		//if client is to be started	
		case("-c"):
			String fname=null,serverAdd=null;
			int servPort;
			int time=1000;
			boolean q=false;
			serverAdd=args[args.length-2];
			servPort=Integer.parseInt(args[args.length-1]);				
			if(args[p].equals("-f")){
				fname=args[p+1];
				p+=2;
			}
			if(args[p].equals("-t")){
				time=Integer.parseInt(args[p+1]);
				p+=2;
			}
			if(args[p].equals("-q")){
				q=true;
			}
			//creste client object
			UDPSender tC1 = new UDPSender(serverAdd,servPort,fname,time,q);
			tC1.Type=1;
			Thread t1 =new Thread(tC1);
			t1.start();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tC1.Type=2;
			Thread t2 = new Thread(tC1);
			t2.start();
			break;
		}
	}
}
