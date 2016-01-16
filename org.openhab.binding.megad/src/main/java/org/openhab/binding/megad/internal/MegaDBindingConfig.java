package org.openhab.binding.megad.internal;

import org.openhab.core.items.Item;
import org.openhab.core.binding.BindingConfig;


public class MegaDBindingConfig implements BindingConfig {
	public String host;
	public int pt;
	Class<? extends Item> itemType;
	@Override
	public String toString() {
		return "From: " + host + "\npt: " + pt;
	}
	public MegaDBindingConfig() {
		super();
	}
	
	public MegaDBindingConfig(MegaDBindingConfig c) {
		super();
		this.host = c.host;
		this.pt = c.pt;
		this.itemType = c.itemType;
	}
	
	
	

}