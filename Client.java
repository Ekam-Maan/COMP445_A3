package comp445_A3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;



public class Client {
	private static int c_port = 41830;
	private static int r_port = 3000;
	private static int s_port = 8007;
	private static	InetAddress ip;
	private static int window = 4;
	private static ArrayList<Packet> packets;
	private static long waitTime = 1000;

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Scanner sc= new Scanner(System.in);
		DatagramSocket ds= new DatagramSocket(c_port);
		ip = InetAddress.getLocalHost();
		System.out.print(">$ ");
		String s = sc.nextLine();
		 //System.out.println("sent size: "+ s.length());
		 byte[] recv = new byte[1024];
			Packet rp;
			
		byte[] userBuff = new byte[s.getBytes().length];
		//System.out.println(data(userBuff));
		userBuff = s.getBytes();
		ArrayList<Packet> pList = makePackets(userBuff);
		int[] sentQ = new int[window];
		long[] startTimes = new long[window];
		long[] endTimes = new long[window];
		int[] packID= new int[window];
		for(int i =0; i<sentQ.length; i++) {
			sentQ[i] = -1;
		}
		boolean canSend = true;
		int sent=0;
		int psent=0;
		int arecv=0;
		
		
		for(int i=0; i<pList.size()+1; i++) { // this for loop is for sending the data
			
			Timer timer= new Timer();
			
			System.out.println("itr: "+i);
			if(pList.size()<window && psent == pList.size()) {
				canSend= false;
			}
			if(pList.size() == 0 || (psent == arecv && psent >0)) break;
			if(sent < window && canSend) {
				
				DatagramPacket packet= new DatagramPacket(pList.get(i).toBytes(), pList.get(i).toBytes().length, ip, r_port);
				ds.send(packet);
				System.out.println("Sent: pack: "+pList.get(i).getSequenceNumber());
				sentQ[i%(window-1)]= (int) pList.get(i).getSequenceNumber();
				packID[i%(window-1)] = i;
				startTimes[i%(window-1)] = System.currentTimeMillis();
				endTimes[i%(window-1)] = 0;
				canSend = false;
				if(sentQ[(i+1)%(window-1)]== -1){
					canSend= true;
				}
				++sent;
				++psent;
			}
			
			else {
				--i;
				canSend = false;
				DatagramPacket ackpacket= new DatagramPacket(recv, recv.length);
				System.out.println("client waiting for reponse...........");
				ds.setSoTimeout((int) waitTime);
				try {
					ds.receive(ackpacket);
				} catch(SocketTimeoutException e) {
					System.out.println("NO ACK RECIEVED");
				}
			    
			    System.out.println("Packet recieved");
			    rp = Packet.fromBytes(recv);
			    if(rp.getType() == 0) {// checking if the recieved packet is of type ACK
			    	int t = getIndex(sentQ,(int)rp.getSequenceNumber());
			    	System.out.println("ACK recieved for pac: "+(int)rp.getSequenceNumber());
			    	if(rp.getSequenceNumber() == sentQ[0]) {
			    		sentQ[0]=-1;
			    		canSend = true;
			    		endTimes[i%(window-1)] = System.currentTimeMillis();
			    		--sent;
			    		++arecv;
			    	}
			    	else if(t != -1){
			    		sentQ[t]=-1;
			    		endTimes[i%(window-1)] = System.currentTimeMillis();
			    		--sent;
			    		++arecv;
			    	}
			    }
			    recv = new byte[1024];
			}
			
			
			
			for(int j = 0 ; j < startTimes.length ; j++) {
				if(startTimes[j] == 0) continue;
				if(System.currentTimeMillis() - startTimes[j] > waitTime) {
					DatagramPacket packet= new DatagramPacket(pList.get(packID[j]).toBytes(), pList.get(packID[j]).toBytes().length, ip, r_port);
					ds.send(packet);
					System.out.println("Sent again pack: "+pList.get(packID[j]).getSequenceNumber());
				
					startTimes[j%(window-1)] = System.currentTimeMillis();
					endTimes[j%(window-1)] = 0;
				}
			}
			
		}
		
		//===============  Now receiving the response from the server =====================
		while(true) {
					DatagramPacket packet= new DatagramPacket(recv, recv.length);
					System.out.println("client waiting for data reponse...........");
				    ds.receive(packet);
				    rp = Packet.fromBytes(recv);
				    Packet ackPacket = makePacket(0,(int) rp.getSequenceNumber(), new byte[1]);
				    DatagramPacket Ackpacket= new DatagramPacket(ackPacket.toBytes(),ackPacket.toBytes().length, ip, r_port);
		     
					ds.send(Ackpacket);
					
				    if(rp.getType()==10) { // this indicates the last packet
				    	//System.out.println("Recieved last Packet");
				    	break;
				    }
				    System.out.println("Server: "+ data(rp.getPayload()));
				//    System.out.println("Server recieved client all data..........."+ rp.toString().length());
				    recv = new byte[1024]; // clearing buffer
		 }
	
		
	}
	
	
	
	
	
	
	
	private static StringBuilder data(byte[] byt) {
		if(byt == null) {
			return null;
		}
		StringBuilder ret = new StringBuilder();
		int i=0;
		while((i<byt.length && byt[i] != 0)) {
			ret.append((char)byt[i]);
			i++;
		}
		//System.out.println("Stopped at "+ i);
		return ret;
		
	}
	
	private static int getIndex(int[] arr, int target) {
		for( int i = 0 ; i<arr.length; i++) {
			if(arr[i]== target)
				return i;
		}
		return -1;
	}
	
	private static ArrayList<Packet> makePackets(byte[] userBuff) {
		ArrayList<Packet> packetList = new ArrayList();
		try {
			ip = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long sq = 0;
		byte[] buff;
		if(userBuff.length < 1014) {
			buff = userBuff;
			Packet pt = new Packet.Builder()
	                .setType(0)
	                .setSequenceNumber(1L)
	                .setPortNumber(s_port)
	                .setPeerAddress(ip)
	                .setPayload(buff)
	                .create();
			packetList.add(pt);
			++sq;
			//DatagramPacket packet= new DatagramPacket(p.toBytes(), p.toBytes().length, ip, r_port);
			//DatagramPacket packet= new DatagramPacket(p.toBytes(), p.toBytes().length, ip, r_port);
		}else {
			
			int si = 0;
			int ei = 1014;
			int left = userBuff.length;
			
			while (true) {
				//System.out.println("si : ei > "+ si +":"+ ei);
				buff = Arrays.copyOfRange(userBuff, si, ei);
				Packet pt = new Packet.Builder()
		                .setType(0)
		                .setSequenceNumber(++sq)
		                .setPortNumber(s_port)
		                .setPeerAddress(ip)
		                .setPayload(buff)
		                .create();
				packetList.add(pt);
				left -= ei-si;
				if(left < 1) {
					
					break;
				}
				si = ei;
				if(left > 1014) {
					ei = ei+1014;
				}
				else {
					ei = ei+left;
				}
			}
			//System.out.println("out of while loop");
			

		}
		
		Packet last_p = new Packet.Builder()
                .setType(10)
                .setSequenceNumber(++sq)
                .setPortNumber(s_port)
                .setPeerAddress(ip)
                .setPayload("last packet".getBytes())
                .create();
		packetList.add(last_p);
		return packetList;
	}
	
	private static Packet makePacket(int type, int sq, byte[] bf) {
		return (new Packet.Builder()
                .setType(0)
                .setSequenceNumber(1L)
                .setPortNumber(s_port)
                .setPeerAddress(ip)
                .setPayload(bf)
                .create());
		
	}
	
	
	
	
	
	
	
	
	

}
