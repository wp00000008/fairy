
package cn.anxinhainan.fairy;

import java.nio.channels.*;

public class FairyServerThread extends Thread {
	private FairyService rpcService;
	private FairyEventList eventList;
	
	public FairyServerThread(FairyService rpcService, FairyEventList eventList) throws Exception {
		this.rpcService = rpcService;
		this.eventList = eventList;
	}
	
	public void run() {
		while (true) {
			try {
				// get a job
				FairyChannelInfo info = eventList.get();
				
				if (info == null) {
					continue;
				}
				
				// do the job
				
				FairyRequest request = info.request_object;
				
				FairyResponse response = new FairyResponse();
				
				response.setExceptionMessage(null);
				
				try {
					Object obj = rpcService.onRpcCall(request.getCommand(), request.getRequest());
					
					response.setResponseObject(obj);
				} catch (Exception e) {
					response.setExceptionMessage(e.getMessage());
				}
				
				// response object -> response stream bytes
				
				info.response_buffer = FairyUtil.objectToStream(response);
				info.response_sent = 0;
				
				// turn selector from READ to WRITE and wakeup it
				
				info.lastActive = System.currentTimeMillis();
				
				info.stage = FairyChannelInfo.STAGE_WRITE;
				
				info.channel.register(info.selector, SelectionKey.OP_WRITE);
				
				// wakeup and do the job !
				
				info.selector.wakeup();
			} catch (Exception e) {
				break;
			}
		}
	}
}











