/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.megad.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.binding.megad.MegaDBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Timofey Fedyanin
 * @since 1.8.0
 */
public class MegaDGenericBindingProvider extends AbstractGenericBindingProvider implements MegaDBindingProvider {
	final static Logger logger = LoggerFactory.getLogger(MegaDGenericBindingProvider.class);
	
	/** {@link Pattern} which matches a binding configuration part */
	private static final Pattern BASE_CONFIG_PATTERN = Pattern.compile("(=)\\[(.*?)\\](\\s|$)");
	
	//TODO Check IP
	/** {@link Pattern} which matches an binding */
	private static final Pattern BINDING_PATTERN = Pattern.compile("(.*):(.*)");
	
	//TODO concurrent
	private static long lastModificationTime = 0l;

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "megad";
	}

	/**
	 * @{inheritDoc}
	 */
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		return;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		if (bindingConfig != null) {
			MegaDBindingConfig config = parseBindingConfig(item, bindingConfig);
			lastModificationTime = System.currentTimeMillis();
			addBindingConfig(item, config);
		}
		else {
			logger.warn("bindingConfig is NULL (item=" + item + ") -> process bindingConfig aborted!");
		}
	}
	
	
	private MegaDBindingConfig parseBindingConfig(Item item,
			String bindingConfig) throws BindingConfigParseException {
		MegaDBindingConfig config = new MegaDBindingConfig();
		config.itemType = item.getClass();
		
		Matcher matcher = BASE_CONFIG_PATTERN.matcher(bindingConfig);
		
		if (!matcher.matches()) {
			throw new BindingConfigParseException("bindingConfig '" + bindingConfig + "' doesn't contain a valid binding configuration");
		}
		matcher.reset();
				
		while (matcher.find()) {
			String direction = matcher.group(1);
			String bindingConfigPart = matcher.group(2);
			
			if (direction.equals("=")) {				
				matcher = BINDING_PATTERN.matcher(bindingConfigPart);
				
				if (!matcher.matches()) {
					throw new BindingConfigParseException("bindingConfig '" + bindingConfig + "' doesn't contain a valid binding-configuration. A valid configuration is matched by the RegExp '(.*):(.*)'");
				}
				matcher.reset();
				
				
				while (matcher.find()) {
					
					config.host = matcher.group(1);
					config.pt = Integer.parseInt(matcher.group(2));				
					logger.debug(config.toString());
				}
				
			}			
			else {
				throw new BindingConfigParseException("Unknown command given! Configuration must start with '<' or '>' ");
			}
		}
		
		return config;
	}



	public static long getLastModificationTime() {
		return lastModificationTime;
	}

	public Map<String, MegaDBindingConfig> getConfigs() {
		HashMap<String, MegaDBindingConfig> map = new HashMap<String, MegaDBindingConfig>();
		for(String name : bindingConfigs.keySet()){
			map.put(name, (MegaDBindingConfig) bindingConfigs.get(name));
		}
		return map;
	}
	
	
	
	
}
