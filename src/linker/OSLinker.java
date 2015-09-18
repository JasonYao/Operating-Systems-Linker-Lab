/**
 * Package
 */
package linker;

/**
 * Imports
 */
import java.util.Hashtable;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * @author Jason Yao
 *This class implements a 2-pass linker dedicated to mapping an object module
 */
public class OSLinker
{
	// Global statically available variables
	private static Hashtable<String, Symbol> symbolTable = null;
	private static ArrayList<ProgramText> programText = null;
	private static ArrayList<Integer> lineOffsets = null;
	private static ArrayList<Use> useList = null;

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{		
		/* SECTION 0: FILE INPUT */
		Scanner input = new Scanner(System.in);

		/* SECTION 1: 1ST PASS */

		/* VARIABLE INITIALISATIONS */
		symbolTable = new Hashtable<String, Symbol>(); // Creates a hash table
		programText = new ArrayList<ProgramText>(); // Creates the program text array
		lineOffsets = new ArrayList<Integer>(); // Creates the line offsets used in relative address mapping
		lineOffsets.add(0); // Adds the base addition
		useList = new ArrayList<Use>();
		int lineCount = 0;
		int baseAddress = 0;
		int offset = 0;
		boolean isIndicator = true;
		int internalLineCount = 0;

		/* END OF SECTION 1: 1ST PASS */
		// Iterates through the file
		while(input.hasNextLine())
		{
			String currentLine = input.nextLine();

			// Sets the internal line count to see which part of the module we're at
			int moduloLineCount = lineCount + 1;
			if (moduloLineCount > 3)
				moduloLineCount = moduloLineCount % 3;

			if (moduloLineCount % 3 == 0)
			{
				// Case 1: line is a program text line
				// Iterates through the program text line
				StringTokenizer tokenizer = new StringTokenizer(currentLine);
				String currentText = null;
				int tokenCount = 0;
				while (tokenizer.hasMoreTokens())
				{
					currentText = tokenizer.nextToken();
					if (isIndicator)
					{
						isIndicator = false;
						lineOffsets.add(Integer.parseInt(currentText));
						// Case 1.1: token is an indicator number, and so skips it
					}
					else
					{
						// Case 1.2: token is program text, increments the offset and adds it to the arraylist
						++offset;
						ProgramText newText = new ProgramText(Integer.parseInt(currentText), lineCount, tokenCount);
						programText.add(newText);
						++internalLineCount;
					}
					++tokenCount;
				}
			}
			else if (moduloLineCount % 2 == 0)
			{
				// Case 2: line is a use line
				// Iterates through the program text line
				StringTokenizer tokenizer = new StringTokenizer(currentLine);
				String currentText = null;
				String currentSymbol = null;
				while (tokenizer.hasMoreTokens())
				{
					currentText = tokenizer.nextToken();
					if (isIndicator)
					{
						// Case 2.1: token is an indicator number, and so skips it
						isIndicator = false;
					}
					else
					{
						// Case 2.2: token is either a symbol, a use, or a deliminator (-1)
						if (currentText.equals("-1"))
						{
							// Case 2.2.1: token is a deliminator (-1), and does nothing
						}
						else if (isNumeric(currentText))
						{
							// Case 2.2.2: token is a use, adds it to the symbol's use
							Symbol actualSymbol = symbolTable.get(currentSymbol);
							if (actualSymbol == null)
							{
								System.err.println("ERROR: Symbol is used but not defined, using value 111");
								// Creates a new symbol with a definition of 111
								Symbol newSymbol = new Symbol(currentSymbol, 111);
								symbolTable.put(currentSymbol, newSymbol);

								// Creates a new use
								int position = internalLineCount + Integer.parseInt(currentText);
								useList.add(new Use(newSymbol, Integer.parseInt(currentText), internalLineCount, 
										position));
							}
							else
							{
								useList.add(new Use(actualSymbol, Integer.parseInt(currentText), internalLineCount,
										internalLineCount + Integer.parseInt(currentText)));
							}
						}
						else
						{
							// Case 2.2.3: token is a symbol, sets the current symbol to it
							currentSymbol = currentText;
						}
					}	
				}
			}		
			else
			{
				// Case 3: line is a symbol definition line
				StringTokenizer tokenizer = new StringTokenizer(currentLine);
				String currentSymbol = null;

				// Iterates through the symbol definition line
				while (tokenizer.hasMoreTokens())
				{
					String currentToken = tokenizer.nextToken();
					if (isIndicator)
					{
						isIndicator = false;
						// Case 3.1: token is an indicator
					}
					else if (!isNumeric(currentToken))
					{
						// Case 3.2: token is a symbol
						currentSymbol = currentToken;

						// Checks if the symbol is in the symbol table
						if (symbolTable.containsKey(currentSymbol))
						{
							// Case 3.2.1: symbol is in the symbol table [ERROR, symbol is doubly defined]
							System.err.println("ERROR: Symbol is doubly defined, using latest definition");
						}
						else
						{
							// Case 3.2.2: symbol is not in the symbol table
							// Adds a new symbol to the symbol table
							Symbol newSymbol = new Symbol(currentSymbol, -1);
							symbolTable.put(currentSymbol, newSymbol);
						}		
					}
					else
					{
						// Case 3.3: token is a symbol definition
						// Adds the use to the current symbol value
						int symbolDefinition = Integer.parseInt(currentToken) + baseAddress + offset;
						symbolTable.get(currentSymbol).setDefinition(symbolDefinition);
					}
				}
			}
			++lineCount;
			isIndicator = true;
		}
		input.close(); // Reached the end of the file

		/* Builds each program line */
		ArrayList<String> programTextLines = new ArrayList<String>();
		int currentLineBeingAdded = programText.get(1).getLine();
		String currentProgramTextBeingAdded = "";

		for (int i = 0; i < programText.size(); ++i)
		{
			if (currentLineBeingAdded == programText.get(i).getLine())
			{
				currentProgramTextBeingAdded = currentProgramTextBeingAdded + programText.get(i).getText() + " ";
			}
			else
			{
				programTextLines.add(currentProgramTextBeingAdded);
				currentProgramTextBeingAdded = "";
				currentLineBeingAdded = programText.get(i).getLine();
				currentProgramTextBeingAdded = currentProgramTextBeingAdded + programText.get(i).getText() + " ";
			}
		}
		programTextLines.add(currentProgramTextBeingAdded);

		/* SECTION 2: 2ND PASS */
		int externalLineOffset = 0;
		int inlineIndividualProgramTextCount = 0;
		for (int programLine = 0; programLine < programTextLines.size(); ++programLine)
		{
			// Tokenizes each line
			StringTokenizer currentProgramTextTokenizer = new StringTokenizer(programTextLines.get(programLine));
			while (currentProgramTextTokenizer.hasMoreTokens())
			{

				String currentProgramTextToken = currentProgramTextTokenizer.nextToken();
				int operand = Integer.parseInt(currentProgramTextToken) % 10;
				ProgramText currentNode = null;

				for (int i = 0; i < programText.size(); ++i)
				{
					if (programText.get(i).getText() == Integer.parseInt(currentProgramTextToken))
						currentNode = programText.get(i);
				}

				// Begins iterating through each of the program texts
				if ((operand == 1) || (operand == 2))
				{
					// Case 1: program text has an immediate operand [UNCHANGED] OR Case 2: program text has an absolute address [UNCHANGED]
					int truncatedOriginal = Integer.parseInt(currentProgramTextToken.substring(0, 4));
					currentNode.setText(truncatedOriginal);
					++inlineIndividualProgramTextCount;
				}
				else if (operand == 3)
				{
					// Case 3: program text has a relative address [CHANGED]
					// NOTE: THE FOLLOW CODE WORKS BECAUSE MATHS. ANY ATTEMPT TO RATIONALISE THIS CODE WILL MAKE YOUR HEAD EXPLODE.
					// YOU'VE BEEN WARNED
					int programTextOriginalLine = currentNode.getLine();
					
					int deleteCount = 0;
					while (programTextOriginalLine > -1)
					{
						programTextOriginalLine -= 3;
						++deleteCount;
					}
					
					int textOffset = 0;
					for (int arrayIterator = 0; arrayIterator < deleteCount; ++arrayIterator)
					{textOffset += lineOffsets.get(arrayIterator);}
					// Text offset has now added all program line offsets prior to the current line

					// Builds the new 4-digit address
					int truncatedOriginal = Integer.parseInt(currentProgramTextToken.substring(0, 4));
					int newText = truncatedOriginal + textOffset;

					if (newText > 9999)
					{
						// relative address exceeds the size of the module [ERROR]
						System.err.println("ERROR: relative address exceeds the size of the module, using the last module address");
						newText = Integer.parseInt(currentProgramTextToken.substring(0,4));
					}
					currentNode.setText(newText);
				}
				else if (operand == 4)
				{
					// Case 4: program text has an external address [CHANGED]
					ProgramText currentProgram = null;
					Use currentUse = null;	
					for (int i = 0; i < programText.size(); ++i)
					{
						if (programText.get(i).getText() == Integer.parseInt(currentProgramTextToken))
						{
							currentProgram = programText.get(i);
							break;
						}
					}

					for (int i = 0; i < useList.size(); ++i)
					{
						if (programText.get(useList.get(i).getId()).getText() == currentProgram.getText())
						{
							currentUse = useList.get(i);
							break;
						}
					}

					int attachedDefinition = currentUse.getSymbol().getDefinition();
					if ((99 < attachedDefinition) && (attachedDefinition < 1000))
					{
						// Case 4.1: symbol has a size of 3
						String opCode = currentProgramTextToken.substring(0, 1);
						int newText = Integer.parseInt(opCode + Integer.toString(attachedDefinition));
						currentProgram.setText(newText);
					} // End of case 4.1
					else if ((9 < attachedDefinition) && (attachedDefinition < 100))
					{
						// Case 4.2: symbol has a size of 2
						String opCode = currentProgramTextToken.substring(0, 2);
						int newText = Integer.parseInt(opCode + Integer.toString(attachedDefinition));
						currentProgram.setText(newText);
					} // End of case 4.2
					else if ((0 <= attachedDefinition) && (attachedDefinition < 10)) 
					{
						// Case 4.3: symbol has a size of 1
						String opCode = currentProgramTextToken.substring(0, 3);
						int newText = Integer.parseInt(opCode + Integer.toString(attachedDefinition));
						currentProgram.setText(newText);
					} // End of case 4.3
					++inlineIndividualProgramTextCount;

					/* END OF SECTION 2: 2ND PASS */
				}
			}
			// Line has ended, and so add up the individual program texts
			externalLineOffset = externalLineOffset + inlineIndividualProgramTextCount;
			inlineIndividualProgramTextCount = 0;
		}

		/* SECTION 3: FILE OUTPUT */
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("Linker.output", "UTF-8");
			// Outputs to file the symbol definitions
			Enumeration<String> symbols = symbolTable.keys();
			String symbolKey;
			writer.println("Symbol Table");
			while(symbols.hasMoreElements())
			{
				symbolKey = (String) symbols.nextElement();
				Symbol printSymbol = symbolTable.get(symbolKey);
				writer.println(printSymbol.getName() + "=" + printSymbol.getDefinition());
			}

			writer.println();
			writer.println("Memory Map:");
			// Outputs to file the symbol definitions
			for (int i = 0; i < programText.size(); ++i)
			{
				writer.println(i + ":\t" + programText.get(i).getText());
			}
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			System.err.println("ERROR: Write permissions are required in order to output file,"
					+ " please escalate privileges or run in allowed directory");
		}
		finally
		{writer.close();}

		/* END OF SECTION 3: FILE OUTPUT */
	} // End of the main class

	/* Helper methods */
	private static boolean isNumeric(String input)  
	{  
		try  
		{Integer.parseInt(input);}  
		catch(NumberFormatException nfe)  
		{return false;}  
		return true;  
	}

} // End of the OSLinker class

