package cn.anxinhainan.fairy;

import java.util.*;

public class FairyEventList {
	private ArrayList<FairyChannelInfo> list = new ArrayList<FairyChannelInfo>();
	
	public synchronized void put(FairyChannelInfo obj) {
		list.add(obj);
		
		notify();
	}
	
	public synchronized FairyChannelInfo get() throws Exception {
		FairyChannelInfo obj = null;
		
		if (list.size() == 0) {
			wait();
		} 
		
		if (list.size() > 0) {
			obj = list.remove(0);
		}
		
		return obj;
	}
}
