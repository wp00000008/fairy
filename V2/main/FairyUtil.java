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
    
    public static byte[] objectToStream(Object obj) throws Exception {
    	/*
    	 * package: header + object stream bytes
    	 * header: 4 bytes, the total length of the request/response package
    	 */
    	
    	// object stream
    	
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bao);
		
		oos.writeObject(obj);
		
		byte[] bin_object = bao.toByteArray();
		
		// header
		
		byte[] header = new byte[4];
		
		FairyUtil.setHeader(header, bin_object.length + 4);
		
		// output
		
		ByteArrayOutputStream bao2 = new ByteArrayOutputStream();
		
		bao2.write(header);
		bao2.write(bin_object);
		
		bin_object = bao2.toByteArray();
		
		bao.close();
		bao2.close();
		
		return bin_object;
	} 
    
    public static Object streamToObject(InputStream is) throws Exception {
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
			throw new Exception("error response object length");
		}
		
		total = len;
		
		bao.write(buffer, 0, len);
		
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
		
		buffer = bao.toByteArray();
		
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer, 4, buffer.length - 4));
		
		Object obj = ois.readObject();
		
		bao.close();
		ois.close();
		
		return obj;
	}
}



