package cn.anxinhainan.fairy;

import java.io.*;
import java.nio.channels.*;

public class FairyChannelInfo {
	public final static int STAGE_READ = 0;
	public final static int STAGE_WORK = 1;
	public final static int STAGE_WRITE = 2;
	
	// stage
	
	public int stage = FairyChannelInfo.STAGE_READ;
	
	// request info
	
	public int request_length = 0;
	public int request_income = 0;
	
	public ByteArrayOutputStream request_stream = new ByteArrayOutputStream();
	
	public byte[] request_bytes;
	
	// response info
	
	public byte[] response_buffer;
	public int response_sent = 0;
	
	// IO info
	
	public Selector selector;
	public SocketChannel channel;
	public long lastActive;
	
	public FairyChannelInfo(Selector selector, SocketChannel channel) {
		this.selector = selector;
		this.channel = channel;
		this.lastActive = System.currentTimeMillis();
	}
}
