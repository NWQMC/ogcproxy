package gov.usgs.wqp.ogcproxy.utils;

import java.util.concurrent.TimeUnit;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class IdleConnectionMonitorThread extends Thread {
	private final HttpClientConnectionManager connMgr;
	private final int idleTimeMilliSeconds;
	private volatile boolean shutdown;

	public IdleConnectionMonitorThread(PoolingHttpClientConnectionManager connMgr, int idleTimeMilliSeconds) {
		super();
		this.connMgr = connMgr;
		this.idleTimeMilliSeconds = idleTimeMilliSeconds;
	}

	@Override
	public void run() {
		try {
			while (!shutdown) {
				synchronized (this) {
					wait(idleTimeMilliSeconds);
					connMgr.closeExpiredConnections();
					connMgr.closeIdleConnections(idleTimeMilliSeconds, TimeUnit.MILLISECONDS);
				}
			}
		} catch (InterruptedException ex) {
			shutdown();
		}
	}

	public void shutdown() {
		shutdown = true;
		synchronized (this) {
			notifyAll();
		}
	}

}
