/**
 * 
 */
package linker;

/**
 * @author Jason Yao
 *
 */
public class ProgramText 
{
	// Globally accessible object attributes
	private int text;
	private int line;
	private int position;

	/**
	 * Program text class constructor
	 */
	public ProgramText(int text, int line, int position)
	{
		setText(text);
		setLine(line);
		setPosition(position);
	} // End of the program text constructor

	public int getText() {
		return text;
	}

	public void setText(int text) {
		this.text = text;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

} // End of the program text class
