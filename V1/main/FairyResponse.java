
package cn.anxinhainan.fairy;

import java.io.*;

// Response: Fairy Server -> Fairy Client

public class FairyResponse implements Serializable {
	private String exceptionMessage;
	private Object responseObject;
	
	public String getExceptionMessage() {
		return exceptionMessage;
	}
	
	public Object getResponseObject() {
		return responseObject;
	}
	
	public void setExceptionMessage(String msg) {
		exceptionMessage = msg;
	}
	
	public void setResponseObject(Object obj) {
		responseObject = obj;
	}
}
