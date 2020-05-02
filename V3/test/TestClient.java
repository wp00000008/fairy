package cn.anxinhainan.fairy.test;

import cn.anxinhainan.fairy.*;

public class TestClient {
	public static void main(String[] args) throws Exception {
		FairyClient client = new FairyClient("localhost", 9000);
		byte[] response;
		
		for (int ii=0; ii<10; ii++) {
			response = client.rpcCall("Army".getBytes());
			
			System.out.println(ii+": "+new String(response));
		}
		
		client.close();
	}
}
