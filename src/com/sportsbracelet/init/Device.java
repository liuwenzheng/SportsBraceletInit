package com.sportsbracelet.init;

import java.io.Serializable;

public class Device implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String name;
	public String address;
	public String rssi;
	public boolean isConnected;
}
