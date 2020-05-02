package cn.anxinhainan.fairy.test;

import cn.anxinhainan.fairy.*;

public class TestClient {
	public static void main(String[] args) throws Exception {
		FairyClient client = new FairyClient("localhost", 9000);
		Object response;
		
		for (int ii=0; ii<10; ii++) {
			response = client.rpcCall("a.b.c.command001", "Army");
			
			System.out.println(ii+": "+response);
		}
		
		client.close();
	}
}
