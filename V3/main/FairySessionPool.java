package cn.anxinhainan.fairy;

import java.util.*;

public class FairySessionPool extends Thread {
	private ArrayList<FairyClient> sessionList = new ArrayList<FairyClient>();
	private HashMap<FairyClient, Boolean> sessionBusy = new HashMap<FairyClient, Boolean>();
	private HashMap<FairyClient, Long> sessionLastActive = new HashMap<FairyClient, Long>();
	
	private boolean terminated = false;
	private String host;
	private int port, maxSessions, maxIdleSeconds;
	
	public static FairySessionPool createPool(String host, int port, int maxSessions, int maxIdleSeconds) {
		FairySessionPool pool = new FairySessionPool(host, port, maxSessions, maxIdleSeconds);
		
		if (maxIdleSeconds <= 0) {
			maxIdleSeconds = 3600;
		}
		
		pool.start();
		
		return pool;
	}
	
	private FairySessionPool(String host, int port, int maxSessions, int maxIdleSeconds) {
		this.host = host;
		this.port = port;
		this.maxSessions = maxSessions;
		this.maxIdleSeconds = maxIdleSeconds;
	}
	
	public synchronized FairyClient getSession() throws Exception {
		FairyClient session;
		
		for (int ii=0; ii<sessionList.size(); ii++) {
			session = sessionList.get(ii);
			
			if (!session.isClosed() && sessionBusy.get(session).equals(Boolean.FALSE)) {
				sessionBusy.put(session, Boolean.TRUE);
				sessionLastActive.put(session, new Long(System.currentTimeMillis()));
				
				return session;
			}
		}
		
		if (maxSessions > 0 && sessionList.size() >= maxSessions) {
			throw new Exception("Pool full");
		}
		
		session = new FairyClient(this.host, this.port);
		
		sessionList.add(session);		
		sessionBusy.put(session, Boolean.TRUE);
		sessionLastActive.put(session, new Long(System.currentTimeMillis()));
		
		return session;
	}
	
	public synchronized void closeSession(FairyClient session) {
		if (session.isClosed()) {
			sessionList.remove(session);
			sessionBusy.remove(session);
			sessionLastActive.remove(session);
			
			return;
		}
		
		sessionBusy.put(session, Boolean.FALSE);
		sessionLastActive.put(session, new Long(System.currentTimeMillis()));
	}
	
	private synchronized void cleanIdleSessions() {
		FairyClient session;
		Long lastActive;
		
		for (int ii=sessionList.size()-1; ii>=0; ii--) {
			session = sessionList.get(ii);
			lastActive = sessionLastActive.get(session);
			
			if (sessionBusy.get(session).equals(Boolean.FALSE) 
					&& (session.isClosed() || System.currentTimeMillis() - lastActive.longValue() > this.maxIdleSeconds * 1000L)) {
				session.close();
				
				sessionList.remove(ii);
				sessionBusy.remove(session);
				sessionLastActive.remove(session);
			}
		}
	}
	
	public void run() {
		while (!terminated) {
			try { Thread.sleep(1 * 1000L); } catch (Exception e) {}
			
			cleanIdleSessions();
		}
	}
	
	public void closePool() throws Exception {
		FairyClient session;
		
		for (int ii=sessionList.size()-1; ii>=0; ii--) {
			session = sessionList.get(ii);
			session.close();
		}
		
		terminated = true;
		
		this.interrupt();
		this.join();
	}
}









