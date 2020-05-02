
package cn.anxinhainan.fairy;

import java.net.*;

public class FairyServer extends Thread {
	private FairyService rpcService;
	private ServerSocket serverSocket;
	
	public static void startup(FairyService rpcService, int port) throws Exception {
		new FairyServer(rpcService, port).start();
	}
	
	private FairyServer(FairyService rpcService, int port) throws Exception {
		this.rpcService = rpcService;
		
		this.serverSocket = new ServerSocket(port);
	}
	
	public void run() {
		// accept client socket and create a thread to deal with the client
		
		while (true) {
			try {
				Socket client = serverSocket.accept();
				
				new FairyServerThread(rpcService, client).start();
			} catch (Exception e) {
				try {Thread.sleep(5000L);} catch (Exception f) {}
			}
		}
	}
}


