
package cn.anxinhainan.fairy;

import java.io.*;
import java.net.*;

public class FairyClient {
	private Socket client;
	private InputStream is;
	private OutputStream os;
	private String host;
	private int port;
	private boolean failed = false;
	
	public FairyClient(String host, int port) throws Exception {
		this.host = host;
		this.port = port;
		
		this.connect();
	}
	
	public byte[] rpcCall(byte[] request_bytes) throws Exception {
		byte[] response;
		
		try {
			os.write(FairyUtil.bytesToStream(request_bytes));
			
			response = FairyUtil.streamToBytes(is);
		} catch (Exception e) {
			this.failed = true;
			
			throw e;
		}
		
		return response;
	}
	
	public void close() {
		try { if (client != null) client.close(); } catch (Exception e) {}
	}
	
	private void connect() throws Exception {
		client = new Socket(host, port);
		
		client.setSoTimeout(300 * 1000);
		
		is = client.getInputStream();
		os = client.getOutputStream();	
		
		this.failed = false;
	}
	
	public boolean isClosed() {
		return client == null || client.isClosed() || this.failed;
	}
}






