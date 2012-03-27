package jist.swans.net;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import jist.swans.Constants;
import jist.swans.misc.Message;
import jist.swans.net.NetMessage.Ip;
import jist.swans.trans.TransTcp.TcpMessage;
import jist.swans.trans.TransUdp.UdpMessage;

public interface FloodingInterface {
	
	// SlotTime : Maximum Packet Size / BandWidth
	final long SLOT_TIME = NetIp.THRESHOLD_FRAGMENT * 8 * Constants.SECOND / Constants.BANDWIDTH_DEFAULT;
	
	// Cache size
	final int size = 128; 
	
	// Debug mode
	final boolean DEBUG = false;
	
	// ipmsg's payload class
	final Class msgType[] = {UdpMessage.class, TcpMessage.class};
	
	/**
	 * to get wait time for re-broadcast
	 * @return
	 */
	public long getSleepTime();
	
	/**
	 * cache list add ipmsg
	 * @param ipmsg
	 */
	public void addCache(Ip ipmsg);
	
	/**
	 * 
	 * @param ipmsg
	 * @return
	 */
	public boolean isContain(Ip ipmsg);
	
	/**
	 * 
	 * @param ipmsg
	 * @return
	 */
	public boolean isRetransmit(NetMessage.Ip ipmsg);
	
	/**
	 * check packet and payload's Message class 
	 * @param ipmsg
	 * @return
	 */
	public boolean checkMsg(Ip ipmsg);
	
	/**
	 * debug message
	 * @param str
	 */
	public void printlnDebug(String str);
	
	/**
	 * Basic flooding scheme
	 * if packet receive and ttl > 1
	 * then re-broadcast packet
	 * @author NSL
	 *
	 */
	public static class BasicFlooding implements FloodingInterface{

		// cache list
		LinkedList list;
		
		Random rand;
		
		/** The maximum amount of jitter before sending a packet. */
		public static final long TRANSMISSION_JITTER = 1 * Constants.MILLI_SECOND;
		
		private static class CacheInfo{
			
			// payload
			Message msg;
			
			// msg source
			NetAddress src;
			
			public CacheInfo(Ip ipmsg){
				msg = ipmsg.getPayload();
				src = ipmsg.getSrc();
			}
			
			public String toString(){
				return "src : "+src+" msg : "+msg;
			}
		}
		
		public BasicFlooding(int seed){
			list = new LinkedList();
			rand = new Random(seed);
		}
		
		private int getIndex(Ip ipmsg){
			
			Message payload = ipmsg.getPayload();
			
			NetAddress src = ipmsg.getSrc();
			
			Iterator it = list.iterator();
			
			int i = 0;
			
			while(it.hasNext()){
				CacheInfo info = (CacheInfo)it.next();
				if(info.src.equals(src) && info.msg.equals(payload))return i;
				
				i++;
			}
			return -1;
		}

		public boolean isContain(Ip ipmsg){
			
			Message payload = ipmsg.getPayload();
			
			NetAddress src = ipmsg.getSrc();
			
			Iterator it = list.iterator();
			
			while(it.hasNext()){
				CacheInfo info = (CacheInfo)it.next();	
				if(info.src.equals(src) && info.msg.equals(payload))return true;
			}
			return false;
		}

		public void addCache(Ip ipmsg) {
			
			if(checkMsg(ipmsg) == false)return;
			
			CacheInfo addInfo;
			
			if(isContain(ipmsg)){
				
				CacheInfo oldInfo = (CacheInfo)list.get(getIndex(ipmsg));
				
				printlnDebug("cache list contains ipmsg : "+oldInfo);

				addInfo = new CacheInfo(ipmsg);
				
				list.remove(oldInfo);
				list.add(addInfo);
				
			}else{
				if(list.size() >= size){
					CacheInfo msg = (CacheInfo) list.removeFirst();
					printlnDebug("cache list's size is exceeded. remove first ipmsg : "
							+msg+" and add ipmsg : "+ipmsg);
					addInfo = new CacheInfo(ipmsg);
					list.add(addInfo);
				}else{
					addInfo = new CacheInfo(ipmsg);
					printlnDebug("cache list add : "+addInfo);
					list.add(addInfo);
				}
			}
		}

		
		public boolean checkMsg(Ip ipmsg) {
			
			if(ipmsg.getDst().equals(NetAddress.ANY)){	
				for(int i = 0 ; i < msgType.length; i++){
					if(ipmsg.getPayload().getClass() == msgType[i])
						return true;
				}
			}
			return false;
		}
		
		public long getSleepTime() {
			return (long)(rand.nextDouble()*TRANSMISSION_JITTER);
		}
		
		public boolean isRetransmit(Ip ipmsg) {
			
			if(checkMsg(ipmsg) == false)return false;
			
			if(isContain(ipmsg)){
				printlnDebug(ipmsg+" is already send.");
				return false;
			}
			
			return true;
		}


		
		public void printlnDebug(String str) {
			if(DEBUG)
				System.out.println(str);			
		}



	}
	
	/**
	 * Probabilistic flooding scheme
	 * if packet receive and ttl > 1
	 * then re-broadcast packet
	 * @author NSL
	 *
	 */
	public static class ProbabilisticFlooding implements FloodingInterface{
		
		// probability of re-broadcast
		private final double PROBABILITY = 0.5;

		// cache list
		LinkedList list;
		
		Random rand;
		
		private static class CacheInfo{
			
			// payload
			Message msg;
			
			// msg source
			NetAddress src;
			
			public CacheInfo(Ip ipmsg){
				msg = ipmsg.getPayload();
				src = ipmsg.getSrc();
			}
			
			public String toString(){
				return "src : "+src+" msg : "+msg;
			}
		}
		
		public ProbabilisticFlooding(int seed){
			list = new LinkedList();
			rand = new Random(seed);
		}
		
		private int getIndex(Ip ipmsg){
			
			Message payload = ipmsg.getPayload();
			
			NetAddress src = ipmsg.getSrc();
			
			Iterator it = list.iterator();
			
			int i = 0;
			
			while(it.hasNext()){
				CacheInfo info = (CacheInfo)it.next();
				
				if(info.src.equals(src) && info.msg.equals(payload))return i;
				
				i++;
			}
			return -1;
		}
		
		public boolean isContain(Ip ipmsg){
			
			Message payload = ipmsg.getPayload();
			
			NetAddress src = ipmsg.getSrc();
			
			Iterator it = list.iterator();
			
			while(it.hasNext()){
				CacheInfo info = (CacheInfo)it.next();
				
				if(info.src.equals(src) && info.msg.equals(payload))return true;
			}
			return false;
		}
		
		public void addCache(Ip ipmsg) {
			
			if(checkMsg(ipmsg) == false)return;
			
			CacheInfo addInfo;
			
			if(isContain(ipmsg)){
				
				CacheInfo oldInfo = (CacheInfo)list.get(getIndex(ipmsg));
				
				printlnDebug("cache list contains ipmsg : "+oldInfo);

				addInfo = new CacheInfo(ipmsg);
				
				list.remove(oldInfo);
				list.add(addInfo);
				
			}else{
				if(list.size() >= size){
					CacheInfo msg = (CacheInfo) list.removeFirst();
					printlnDebug("cache list's size is exceeded. remove first ipmsg : "
							+msg+" and add ipmsg : "+ipmsg);
					addInfo = new CacheInfo(ipmsg);
					list.add(addInfo);
				}else{
					addInfo = new CacheInfo(ipmsg);
					printlnDebug("cache list add : "+addInfo);
					list.add(addInfo);
				}
			}
		}
		
		
		public boolean checkMsg(Ip ipmsg) {
			
			if(ipmsg.getDst().equals(NetAddress.ANY)){	
				for(int i = 0 ; i < msgType.length; i++){
					if(ipmsg.getPayload().getClass() == msgType[i])
						return true;
				}
			}
			return false;
		}
		
		public long getSleepTime() {
			return (long) (rand.nextDouble()*SLOT_TIME);
		}
		
		public boolean isRetransmit(Ip ipmsg) {
			
			if(checkMsg(ipmsg) == false)return false;
			
			if(isContain(ipmsg)){
				printlnDebug(ipmsg+" is already send.");
				return false;
			}else{
				
				if(rand.nextFloat() > PROBABILITY){
					return true;
				}else{
					return false;
				}
			}
		}
		
		public void printlnDebug(String str) {
			if(DEBUG)
				System.out.println(str);				
		}

	}
	
	/**
	 * Counter Base flooding scheme
	 * if packet receive and ttl > 1
	 * then re-broadcast packet
	 * @author NSL
	 *
	 */
	public  static class CounterBaseFlooding implements FloodingInterface{
		
		// counter threshold
		private final int THRESHOLD = 4;
		
		// random range for sleep time
		private final int TIME_BASE = 3;
		
		// cache list
		LinkedList list;
		
		Random rand;
		
		private static class CacheInfo{
			
			// payload
			Message msg;
			
			// msg source
			NetAddress src;
			
			// receive num
			int recvNum;
			
			public CacheInfo(Ip ipmsg, int num){
				msg = ipmsg.getPayload();
				src = ipmsg.getSrc();
				recvNum = num;
			}
			
			public String toString(){
				return "src : "+src+" msg : "+msg+" recvNum : "+recvNum;
			}
		}

		public CounterBaseFlooding(int seed){
			list = new LinkedList();
			rand = new Random(seed);
		}
		
		public boolean isContain(Ip ipmsg){
			
			Message payload = ipmsg.getPayload();
			
			NetAddress src = ipmsg.getSrc();
			
			Iterator it = list.iterator();
			
			while(it.hasNext()){
				CacheInfo info = (CacheInfo)it.next();
				
				if(info.src.equals(src) && info.msg.equals(payload))return true;
			}
			return false;
		}
		
		private int getIndex(Ip ipmsg){
			
			Message payload = ipmsg.getPayload();
			
			NetAddress src = ipmsg.getSrc();
			
			Iterator it = list.iterator();
			
			int i = 0;
			
			while(it.hasNext()){
				CacheInfo info = (CacheInfo)it.next();
				
				if(info.src.equals(src) && info.msg.equals(payload))return i;
				
				i++;
			}
			return -1;
		}
		
		public void addCache(Ip ipmsg) {
			
			if(checkMsg(ipmsg) == false)return;
			
			CacheInfo addInfo;
			
			if(isContain(ipmsg)){
				
				CacheInfo oldInfo = (CacheInfo)list.get(getIndex(ipmsg));
				
				printlnDebug("cache list contains ipmsg : "+oldInfo);

				addInfo = new CacheInfo(ipmsg,oldInfo.recvNum+1);
				
				list.remove(oldInfo);
				list.add(addInfo);
				
			}else{
				if(list.size() >= size){
					CacheInfo msg = (CacheInfo) list.removeFirst();
					printlnDebug("cache list's size is exceeded. remove first ipmsg : "
							+msg+" and add ipmsg : "+ipmsg);
					addInfo = new CacheInfo(ipmsg,1);
					list.add(addInfo);
				}else{
					addInfo = new CacheInfo(ipmsg,1);
					printlnDebug("cache list add : "+addInfo);
					list.add(addInfo);
				}
			}
		}
		
		
		public void updateCache(Ip ipmsg){
			
			if(checkMsg(ipmsg) == false)return;
			
			CacheInfo addInfo;
			
			if(isContain(ipmsg)){
				
				CacheInfo oldInfo = (CacheInfo)list.get(getIndex(ipmsg));
				
				printlnDebug("cache list contains ipmsg : "+oldInfo);
				
				if(oldInfo.recvNum < 4){
					addInfo = new CacheInfo(ipmsg,THRESHOLD);
				}else{
					addInfo = new CacheInfo(ipmsg,oldInfo.recvNum+1);
				}
				
				list.remove(oldInfo);
				list.add(addInfo);
				
			}else{
				return;
			}
			
		}
		
		public boolean checkMsg(Ip ipmsg) {
			
			if(ipmsg.getDst().equals(NetAddress.ANY)){	
				for(int i = 0 ; i < msgType.length; i++){
					if(ipmsg.getPayload().getClass() == msgType[i])
						return true;
				}
			}
			return false;
		}
		
		public long getSleepTime() {
			return (long) (rand.nextDouble()*TIME_BASE*2*SLOT_TIME);
		}

		
		public boolean isRetransmit(Ip ipmsg) {
			
			if(checkMsg(ipmsg) == false)return false;
			
			if(isContain(ipmsg)){
				
				CacheInfo msg = (CacheInfo)list.get(getIndex(ipmsg));
				
				if(msg.recvNum >= THRESHOLD){
					return false;
				}else{
					return true;
				}
			}else{
				
				return true;

			}
		}


		
		public void printlnDebug(String str) {
			if(DEBUG)
				System.out.println(str);			
		}
		
	}

}
