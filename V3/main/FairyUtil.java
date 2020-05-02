package cn.anxinhainan.fairy;

import java.io.*;

public class FairyUtil {
	public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        
        return result;
    }
	
	public static void setHeader(byte[] result, int i) {
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
    }
 
    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        
        for(int i = 0; i < 4; i++) {
            int shift= (3-i) * 8;
            value +=(bytes[i] & 0xFF) << shift;
        }
        
        return value;
    }
    
    public static byte[] bytesToStream(byte[] bytes) throws Exception {
    	ByteArrayOutputStream bao2 = new ByteArrayOutputStream();
		
		bao2.write(FairyUtil.intToByteArray(bytes.length + 4));
		bao2.write(bytes);
		
		return bao2.toByteArray();
	} 
    
    public static byte[] streamToBytes(InputStream is) throws Exception {
		int len, total_len, total;
		byte[] buffer = new byte[64 * 1024];
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		
		// header: 4 bytes, the total length of the request/response package
		
		len = is.read(buffer);
		
		if (len < 4) {
			throw new Exception("error response header");
		}
		
		total_len = FairyUtil.byteArrayToInt(buffer);
		
		if (total_len <= 4) {
			throw new Exception("error response length");
		}
		
		total = len;
		
		bao.write(buffer, 4, len - 4);
		
		// object stream
		
		while (total < total_len) {
			len = is.read(buffer);
			
			if (len <= 0) {
				throw new Exception("input stream closed");
			}
			
			bao.write(buffer, 0, len);
			
			total += len;
			
			if (total == total_len) {
				break;
			}
			
			if (total > total_len) {
				throw new Exception("error response");
			}
		}
		
		return bao.toByteArray();
	}
}



