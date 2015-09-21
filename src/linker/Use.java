/**
 * 
 */
package linker;

/**
 * @author Jason Yao
 *
 */
public class Use {

	// Globally accessible object attributes
	private String symbol; // The symbol attached to the use
	private int address; // The relative address in the useLine
	private int line; // The current useLine

	
	/**
	 * 
	 */
	public Use(String symbol, int address, int line)
	{
		setSymbol(symbol);
		setAddress(address);
		setLine(line);
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}
} // End of the Use  class
