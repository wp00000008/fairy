
package cn.anxinhainan.fairy;

import java.io.*;

// Request: Fairy Client -> Fairy Server

public class FairyRequest implements Serializable {
	private Object command;		
	private Object request;		
	
	public FairyRequest(Object command, Object request) {
		this.command = command;
		this.request = request;
	}
	
	public Object getCommand() {
		return command;
	}

	public Object getRequest() {
		return request;
	}
}
