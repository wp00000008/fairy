package cn.anxinhainan.fairy.test;

import cn.anxinhainan.fairy.*;

public class TestServer implements FairyService {
	public byte[] onRpcCall(byte[] request) {
		return ("Hello " + new String(request)).getBytes();
	}
	
	public static void main(String[] args) throws Exception {
		TestServer server = new TestServer();
		
		FairyServer.startup(server, 9000, 6);
		
		System.out.println("Server startup");
	}
}
