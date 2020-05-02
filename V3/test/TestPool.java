package cn.anxinhainan.fairy.test;

import cn.anxinhainan.fairy.*;

public class TestPool {
	public static void main(String[] args) throws Exception {
		FairySessionPool pool = FairySessionPool.createPool("localhost", 9000, 0, 0);				
		FairyClient client = pool.getSession();
		byte[] response;
		
		for (int ii=0; ii<10; ii++) {
			response = client.rpcCall("Army".getBytes());
			
			System.out.println(ii+": "+new String(response));
		}
		
		pool.closeSession(client);
		
		pool.closePool();
	}
}
