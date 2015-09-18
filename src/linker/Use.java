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
	private Symbol symbol;
	private int address;
	private int line;
	private int id;
	
	/**
	 * 
	 */
	public Use(Symbol symbol, int address, int line, int id)
	{
		setSymbol(symbol);
		setAddress(address);
		setLine(line);
		setId(id);
	}

	public Symbol getSymbol() {
		return symbol;
	}

	public void setSymbol(Symbol symbol) {
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

} // End of the Use  class
