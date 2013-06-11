package uk.lmfm.amarino;

import uk.lmfm.amarino.BTDevice;

public class Globals{
	
private static Globals instance;
	
	// vars
	private ShoeManager shoeManager;
	private BTDevice device;
	
	/** Constructors **/
	
	private Globals(){}
	
	public static synchronized Globals getInstance() {
		if(instance == null) {
			instance = new Globals();
		}
		return instance;
	}
	
	/** Getters and Setters **/
	
	public void setShoeManager(ShoeManager sm) {
		this.shoeManager = sm;
	}
	
	public ShoeManager getShoeManager() {
		return this.shoeManager;
	}
	
	public void setDevice(BTDevice d) {
		this.device = d;
	}
	
	public BTDevice getDevice() {
		return this.device;
	}
	
}

