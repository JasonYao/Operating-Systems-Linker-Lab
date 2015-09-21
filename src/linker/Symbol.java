/**
 * 
 */
package linker;


/**
 * @author Jason Yao
 *
 */
public class Symbol
{
	// Globally accessible object attributes
	private String name;
	private int definition;
	private boolean isUsed;

	/**
	 * 
	 */
	public Symbol(String name, int definition)
	{
		setName(name);
		setDefinition(definition);
		setUsed(false);
	} // End of the constructor

	public boolean isUsed() {
		return isUsed;
	}

	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDefinition() {
		return definition;
	}

	public void setDefinition(int definition) {
		this.definition = definition;
	}

} // End of the symbol class
