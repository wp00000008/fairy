
package cn.anxinhainan.fairy;

import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

public class FairyServer extends Thread {
	private Selector selector;
	private HashMap<SocketChannel, FairyChannelInfo> channelMap = new HashMap<SocketChannel, FairyChannelInfo>();
	private FairyEventList eventList = new FairyEventList();
	
	private ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
	
	public static void startup(FairyService rpcService, int port, int workerThreads) throws Exception {
		new FairyServer(rpcService, port, workerThreads).start();
	}
	
	public static void startup(FairyService rpcService, int port) throws Exception {
		new FairyServer(rpcService, port, Runtime.getRuntime().availableProcessors() + 1).start();
	}
	
	private FairyServer(FairyService rpcService, int port, int workerThreads) throws Exception {
		for (int ii=0; ii<workerThreads; ii++) {
			new FairyServerThread(rpcService, eventList).start();
		}
		
		ServerSocketChannel serverChannel = ServerSocketChannel.open();  
		serverChannel.configureBlocking(false);  
        serverChannel.socket().bind(new InetSocketAddress(port), 65536); 
        this.selector = Selector.open();  
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	}
	
	public void run() {
		while (true) {
			try {
				io();
			} catch (Exception e) {
				FairyDebugger.debug("Exception: "+e.getMessage());
			}
		}
	}
	
	private void io() throws Exception {
		this.selector.select(30 * 1000L);
		
		Iterator ite = this.selector.selectedKeys().iterator();  

        while (ite.hasNext()) {
            SelectionKey key = (SelectionKey) ite.next();  
            
            ite.remove();  
            
            if (key.isAcceptable()) {  
            	ServerSocketChannel server = (ServerSocketChannel)key.channel();  
                SocketChannel channel = server.accept(); 
                
                this.channelMap.put(channel, new FairyChannelInfo(this.selector, channel));
                
                channel.configureBlocking(false);  
                channel.register(this.selector, SelectionKey.OP_READ);
            } else {
            	SocketChannel channel = (SocketChannel)key.channel(); 
            	
            	try {
        			if (key.isReadable()) {
        				clientRead(channel);
        			}
        			
        			if (key.isWritable()) {
        				clientWrite(channel);
        			}
        		} catch (Exception e) {
        			FairyDebugger.debug("Exception: "+e.getMessage());
        			
        			clientClose(channel);
        		}
            }
        }
        
        // clean up zombie clients
        
        ite = this.channelMap.keySet().iterator();
        
        while (ite.hasNext()) {
        	SocketChannel channel = (SocketChannel) ite.next();  
        	FairyChannelInfo info = this.channelMap.get(channel);
        	
        	if (info == null || System.currentTimeMillis() - info.lastActive > 3600 * 1000L) {
        		clientClose(channel);
        	}
        }
	}
	
	private void clientRead(SocketChannel channel) throws Exception {
		FairyDebugger.debug("Read: "+channel);
		
		buffer.clear();
		
		FairyChannelInfo info = this.channelMap.get(channel);
		
		if (info == null) {
    		throw new Exception("error client: "+channel);
    	}
		
		if (info.stage != FairyChannelInfo.STAGE_READ) {
			throw new Exception("error stage: "+channel);
		}
		
		int len = channel.read(buffer);
		
    	if (len < 0) {
    		throw new Exception("client read error: "+channel);
    	}
    	
    	if (len == 0) {
    		FairyDebugger.debug("Read 0: "+channel);
    		
    		return;
    	}
    	
    	byte[] indata = buffer.array();
    	
    	if (info.request_length <= 0) {
    		if (len < 4) {
    			throw new Exception("error request header: "+channel);
    		}
    		
    		info.request_length = FairyUtil.byteArrayToInt(indata);
    		
    		if (info.request_length <= 4) {
    			throw new Exception("error request object length: "+channel);
    		}
    	}
    	
    	info.request_income += len;
    	info.request_stream.write(indata, 0, len);
    	
    	// keep reading
    	
    	while (info.request_income < info.request_length) {
    		len = channel.read(buffer);
    		
    		if (len == 0) {
    			break;
    		}
    		
    		if (len < 0) {
        		throw new Exception("client read error: "+channel);
        	}
    		
    		indata = buffer.array();
    		
    		info.request_income += len;
    		info.request_stream.write(indata, 0, len);
    	}
    	
    	// request done ?
    	
    	if (info.request_income > info.request_length) {
			throw new Exception("error request: "+channel);
		}
    	
    	if (info.request_income == info.request_length) {
    		indata = info.request_stream.toByteArray();
    		
    		info.stage = FairyChannelInfo.STAGE_WORK;
    		
    		info.request_bytes = info.request_stream.toByteArray();
    		
    		info.request_income = 0;
    		info.request_length = 0;
    		info.request_stream.reset();
    		
    		eventList.put(info);
    	}
    	
    	info.lastActive = System.currentTimeMillis();
	}
	
	private void clientWrite(SocketChannel channel) throws Exception {
		FairyDebugger.debug("Write: "+channel);
		
		FairyChannelInfo info = this.channelMap.get(channel);
		
		if (info == null) {
    		throw new Exception("error client: "+channel);
    	}
		
		if (info.stage != FairyChannelInfo.STAGE_WRITE) {
			throw new Exception("error stage: "+channel);
		}
		
		if (info.response_buffer == null || info.response_buffer.length == 0 || info.response_buffer.length < info.response_sent) {
			throw new Exception("error response data: "+channel);
		}
		
		int len;
		
		while (info.response_sent < info.response_buffer.length) {
			len = channel.write(
					ByteBuffer.wrap(info.response_buffer, info.response_sent, info.response_buffer.length - info.response_sent));
			
			if (len == 0) {
				break;
			}
			
			if (len < 0) {
				throw new Exception("error client write: "+channel);
			}
			
			info.response_sent += len;
		}
		
		// write done ?
		
		if (info.response_sent > info.response_buffer.length) {
			throw new Exception("error response: "+channel);
		}
    	
    	if (info.response_sent == info.response_buffer.length) {
    		info.stage = FairyChannelInfo.STAGE_READ;
    		
    		info.response_buffer = null;
    		info.response_sent = 0;
    		
    		channel.register(this.selector, SelectionKey.OP_READ);
    	}
    	
    	info.lastActive = System.currentTimeMillis();
	}
	
	private void clientClose(SocketChannel channel) {
		FairyDebugger.debug("Closed: "+channel);
		
		channelMap.remove(channel);
		
		try {channel.close();} catch (Exception f) {}
	}
}


















