
package cn.anxinhainan.fairy;

import java.io.*;
import java.net.*;

public class FairyClient {
	private Socket client;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	
	public FairyClient(String host, int port) throws Exception {
		// connect to server 
		
		client = new Socket(host, port);
		
		// set timeout, otherwise you may not be able to return from socket read
		
		client.setSoTimeout(300 * 1000);
		
		// raw socket stream
		
		InputStream is = client.getInputStream();
		OutputStream os = client.getOutputStream();
		
		// object socket stream
		
		oos = new ObjectOutputStream(os);
		ois = new ObjectInputStream(is);		
	}
	
	public Object rpcCall(Object command, Object requestData) throws Exception {
		// send request object to server
		
		oos.writeObject(new FairyRequest(command, requestData));
		
		// get response object from server, ... object exchanges ... so easy
		
		FairyResponse response = (FairyResponse)ois.readObject();
		String exceptionMessage = response.getExceptionMessage();
		
		if (exceptionMessage != null) {
			throw new Exception("Server Exception: " + exceptionMessage);
		}
		
		return response.getResponseObject();
	}
}
