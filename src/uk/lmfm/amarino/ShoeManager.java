
package uk.lmfm.amarino;

import java.io.Serializable;

import android.content.Context;
import at.abraxas.amarino.Amarino;

/**
 * 
 * @author Mathieu Cretien
 *
 * $Id: ShoeManager.java 444 2013-06-11 13:11:59Z mcretien $
 */
public class ShoeManager implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Constant
	private static final String leftShoeAddress  = "00:06:66:4E:D9:07";
	private static final String rightShoeAddress = "00:06:66:4E:E0:BD";
	
	// BTDevices
	private BTDevice leftShoe;
	private BTDevice rightShoe;
	
	private Context context;
	
	// Constructor
	public ShoeManager (Context _context) {
		
		// Set BTDevices
		leftShoe  = new BTDevice(leftShoeAddress);
		leftShoe.setSide("left");
		rightShoe = new BTDevice(rightShoeAddress);
		rightShoe.setSide("right");
		context = _context;
	}
	
	// Arduino methods
	public void sendDataToLeftShoe(char FLAG, String data) {
		
		Amarino.sendDataToArduino(this.context, this.getLeftShoe().address, FLAG, data);
	}
	
	public void sendDataToRightShoe(char FLAG, String data) {
		
		Amarino.sendDataToArduino(this.context, this.getRightShoe().address, FLAG, data);
	}
	
	// Getters and Setters
	
	public BTDevice getShoe(String address) {
		
		if (address.equals(leftShoe.address)) {
			return leftShoe;
		} else if (address.equals(rightShoe.address)) {
			return rightShoe;
		}
		
		return null;
	}
	
	public BTDevice getLeftShoe() {
		return leftShoe;
	}
	
	public BTDevice getRightShoe() {
		return rightShoe;
	}
	
	public BTDevice[] getShoesDevices() {
		
		BTDevice[] shoes;
		shoes = new BTDevice[2];
		
		shoes[0] = leftShoe;
		shoes[1] = rightShoe;
		
		return shoes;
	}
	
	
	
}
