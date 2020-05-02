
package cn.anxinhainan.fairy;

import java.io.*;
import java.net.*;

public class FairyClient {
	private Socket client;
	private InputStream is;
	private OutputStream os;
	private String host;
	private int port;
	
	public FairyClient(String host, int port) throws Exception {
		this.host = host;
		this.port = port;
		
		this.connect();
	}
	
	public Object rpcCall(Object command, Object requestData) throws Exception {
		FairyResponse response;
		String exceptionMessage;
		Object obj;
		
		try {
			os.write(FairyUtil.objectToStream(new FairyRequest(command, requestData)));
			
			response = (FairyResponse)FairyUtil.streamToObject(is);
			
			exceptionMessage = response.getExceptionMessage();		
			obj = response.getResponseObject();
		} catch (Exception e) {
			throw e;
		}
			
		if (exceptionMessage != null) {
			throw new Exception("Server Exception: " + exceptionMessage);
		}
		
		return obj;
	}
	
	public void close() {
		try { if (client != null) client.close(); } catch (Exception e) {}
	}
	
	private void connect() throws Exception {
		client = new Socket(host, port);
		
		client.setSoTimeout(300 * 1000);
		
		is = client.getInputStream();
		os = client.getOutputStream();	
	}
}






