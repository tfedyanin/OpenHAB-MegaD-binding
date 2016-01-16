package org.openhab.binding.megad.internal;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openhab.core.events.EventPublisher;


public class MegaListener implements Runnable {
	private int port;
	private long timeout;
	private MegaDBinding binding;
	private ServerSocket serverSocket;
	private volatile boolean isTerminated = false;
	private ScheduledExecutorService service = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	private final EventPublisher publisher;

	public int getPort() {
		return port;
	}

	public long getTimeout() {
		return timeout;
	}

	public MegaListener(int port, long timeout, MegaDBinding binding, EventPublisher publisher) throws IOException {
		super();
		this.port = port;
		this.timeout = timeout;
		this.binding = binding;
		this.publisher = publisher;		
	}

	public void run() {
		
		try {
			serverSocket = new ServerSocket(port);
			while(!isTerminated){
				Socket accept = serverSocket.accept();
//				System.err.println(accept.getInetAddress().toString());
//				System.err.println(accept.getPort());
				
				SessionTask task = new SessionTask(accept, binding.getProviders(), publisher);
				final Future<?> handler = service.submit(task);
				service.schedule(new Runnable() {					
					public void run() {
						handler.cancel(true);						
					}
				}, timeout, TimeUnit.MILLISECONDS);				
			}
		} catch (Throwable t) {
			
		} finally {
			stop();
		}
	}
	
	
	public void terminate() {
		isTerminated = true;
		stop();
	}

	private void stop() {
		try {
			serverSocket.close();
		} catch (Throwable ignored) {
			//System.out.println(ignored.getMessage());
		}
		
	}



}
