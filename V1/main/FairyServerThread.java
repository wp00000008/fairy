
package cn.anxinhainan.fairy;

import java.io.*;
import java.net.*;

public class FairyServerThread extends Thread {
	private FairyService rpcService;
	
	private Socket client;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	
	public FairyServerThread(FairyService rpcService, Socket client) throws Exception {
		this.rpcService = rpcService;
		this.client = client;
		
		// set timeout, otherwise you may not be able to return from socket read
		
		this.client.setSoTimeout(3600 * 1000);
		
		// raw socket stream
		
		InputStream is = this.client.getInputStream();
		OutputStream os = this.client.getOutputStream();
		
		// object socket stream
		
		ois = new ObjectInputStream(is);
		oos = new ObjectOutputStream(os);
	}
	
	public void run() {
		while (true) {
			try {
				// get request object from client
				
				FairyRequest request = (FairyRequest)ois.readObject();
				
				// serve the client and send response to client, ... object exchanges ... so easy
				
				FairyResponse response = new FairyResponse();
				
				response.setExceptionMessage(null);
				
				try {
					Object obj = rpcService.onRpcCall(request.getCommand(), request.getRequest());
					
					response.setResponseObject(obj);
				} catch (Exception e) {
					response.setExceptionMessage(e.getMessage());
				}
				
				oos.writeObject(response);
			} catch (Exception e) {
				// end the service if client closes or something wrong
				
				break;
			}
		}
	}
}


















