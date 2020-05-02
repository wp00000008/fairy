package cn.anxinhainan.fairy.test;

import cn.anxinhainan.fairy.FairyClient;

public class BenchMark extends Thread {
	public void run() {
		try {
			FairyClient client = new FairyClient("localhost", 9000);
			
			for (int ii=0; ii<10000; ii++) {
				client.rpcCall("a.b.c.command001", "Army");
				//System.out.println(ii+1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		doIt(1);
        doIt(5);
        doIt(10);
        doIt(20);
        doIt(50);
        doIt(100);
        
        System.out.println("Done");
        System.in.read();
	}
	
	public static void doIt(int threads) throws Exception {
        long tm = System.currentTimeMillis();
        
        Thread[] list = new Thread[threads];
        
        for (int ii=0; ii<list.length; ii++) {
        	list[ii] = new BenchMark();
        	list[ii].start();
        }
        
        for (int ii=0; ii<list.length; ii++) {
        	list[ii].join();
        }
        
        System.out.println("[ " + threads + " ] MSecs: " + (System.currentTimeMillis() - tm));
    }
}
