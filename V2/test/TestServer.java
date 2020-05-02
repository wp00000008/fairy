package cn.anxinhainan.fairy.test;

import cn.anxinhainan.fairy.*;

public class TestServer implements FairyService {
	public Object onRpcCall(Object command, Object request) throws Exception {
		if ("a.b.c.command001".equals(command)) {
			return "Hello " + request;
		}
		
		throw new Exception("error command: " + command);
	}
	
	public static void main(String[] args) throws Exception {
		TestServer server = new TestServer();
		
		FairyServer.startup(server, 9000);
		
		System.out.println("Server startup");
	}
}
