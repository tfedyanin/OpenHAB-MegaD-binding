/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.megad.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.megad.MegaDBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author Timofey Fedyanin
 * @since 1.8.0
 */
public class MegaDBinding extends AbstractActiveBinding<MegaDBindingProvider> {

	private static final Logger logger = 
			LoggerFactory.getLogger(MegaDBinding.class);
	
	private static long lastUpdateTime = 0l;
	private static long defaultTimeout = 5000;
	private static int defaultPort = 80;

	/** 
	 * the refresh interval which is used to poll values from the MegaD
	 * server (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 60000;

	
	/**
	 * 
	 */
	private long timeout = defaultTimeout;
	

	/**
	 * 
	 */
	private LinkedList<Integer> ports = new LinkedList<Integer>(Arrays.asList(defaultPort));
	
	private LinkedList<MegaListener> listeners = new LinkedList<MegaListener>();
	
	ExecutorService service = Executors.newCachedThreadPool();
	
	



	public MegaDBinding(){
		try {
			listeners.add(new MegaListener(defaultPort, defaultTimeout, this, eventPublisher));
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
	}


	/**
	 * Called by the SCR to activate the component with its configuration read from CAS
	 * 
	 * @param bundleContext BundleContext of the Bundle that defines this component
	 * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
	 */
	public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
		

		// the configuration is guaranteed not to be null, because the component definition has the
		// configuration-policy set to require. If set to 'optional' then the configuration may be null

		updateConfiguration(configuration);
		setProperlyConfigured(true);
		updateServer();
	}

	/**
	 * Called by the SCR when the configuration of a binding has been changed through the ConfigAdmin service.
	 * @param configuration Updated configuration properties
	 */
	public void modified(final Map<String, Object> configuration) {
		updateConfiguration(configuration);		
		setProperlyConfigured(true);
		updateServer();
	}

	
	
	private void updateConfiguration(final Map<String, Object> configuration){

		String refreshIntervalString = (String) configuration.get("refresh");
		if (StringUtils.isNotBlank(refreshIntervalString)) {
			refreshInterval = Long.parseLong(refreshIntervalString);
		}

		String serverTimeoutString = (String) configuration.get("timeout");
		if (StringUtils.isNotBlank(serverTimeoutString)){
			timeout = Long.parseLong(serverTimeoutString);
		}

		String portsString = (String) configuration.get("ports");
		if (StringUtils.isNotBlank(portsString)){
			ports.clear();
			String[] sPorts = portsString.split(",");
			for (String sPort: sPorts) {
				ports.add(Integer.parseInt(sPort));
			}
		}		
		
		updateServer();
	}

	/**
	 * Called by the SCR to deactivate the component when either the configuration is removed or
	 * mandatory references are no longer satisfied or the component has simply been stopped.
	 * @param reason Reason code for the deactivation:<br>
	 * <ul>
	 * <li> 0 – Unspecified
	 * <li> 1 – The component was disabled
	 * <li> 2 – A reference became unsatisfied
	 * <li> 3 – A configuration was changed
	 * <li> 4 – A configuration was deleted
	 * <li> 5 – The component was disposed
	 * <li> 6 – The bundle was stopped
	 * </ul>
	 */
	public void deactivate(final int reason) {
		ports = null;
		
		// deallocate resources here that are no longer needed and 
		// should be reset when activating this binding again
	}


	/**
	 * @{inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected String getName() {
		return "MegaD Refresh Service";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void execute() {
		// the frequently executed code (polling) goes here ...
		
		updateServer();
		logger.debug("execute() method is called!");
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand({},{}) is called!", itemName, command);
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveUpdate({},{}) is called!", itemName, newState);
	}


	@Override
	public void addBindingProvider(MegaDBindingProvider provider) {
		super.addBindingProvider(provider);
	}


	@Override
	public void removeBindingProvider(MegaDBindingProvider provider) {
		super.removeBindingProvider(provider);
	}
	
	private void updateServer(){
		if(lastUpdateTime > MegaDGenericBindingProvider.getLastModificationTime())
		{
			return;
		}
		
		for(MegaListener l : listeners){
			l.terminate();
		}
		try {
			service.awaitTermination(timeout, TimeUnit.MILLISECONDS);
			//System.out.println(b);
		} catch (InterruptedException e1) {
			logger.warn(e1.getMessage());
		}
		
		//TODO not all?
		listeners.clear();
		
		for(int port: ports){
			MegaListener l;
			try {
				l = new MegaListener(port, timeout, this, eventPublisher);
				listeners.add(l);
				service.execute(l);
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}						
		}
		lastUpdateTime = System.currentTimeMillis();		
	}
	
	public Collection<MegaDBindingProvider> getProviders(){
		//TODO clone?
		return providers;
	}
	

}
