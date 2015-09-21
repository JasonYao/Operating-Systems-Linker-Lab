/**
 * 
 */
package linker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;

/**
 * @author Jason Yao
 *
 */
public class RobustOSLinker
{
	// Global statically available variables
	private static Hashtable<String, Symbol> symbolTable = null;
	private static ArrayList<ProgramText> programText = null;
	private static ArrayList<String> errors = null;
	private static ArrayList<Use> useList = null;
	private static ArrayList<Integer> lineOffsets = null;
	private static ArrayList<Integer> indicatorList = null;
	private static int MACHINE_SIZE;

	/**
	 * Runs the one pass linker program
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			/* VARIABLE INITIALISATIONS */
			symbolTable = new Hashtable<String, Symbol>(); // Creates a hash table
			programText = new ArrayList<ProgramText>(); // Creates the program text array
			errors = new ArrayList<String>(); // Creates a list of all errors to be printed
			useList = new ArrayList<Use>(); // Creates a list of external symbol uses
			lineOffsets = new ArrayList<Integer>(); // Creates a list of the line offsets in the program text
			lineOffsets.add(0); // Adds the base addition
			indicatorList = new ArrayList<Integer>();
			MACHINE_SIZE = 300;

			/* START OF SECTION 0: 1ST PASS */
			firstPass();
			/* END OF SECTION 0: 1ST PASS */

			/* START OF SECTION 1: ERROR CHECKING PART 1*/
			firstErrorChecking();
			/* END OF SECTION 1: ERROR CHECKING PART 1*/

			/* START OF SECTION 2: DATA PROCESSING */
			process();
			/* END OF SECTION 2: DATA PROCESSING */

			/* START OF SECTION 3: ERROR CHECKING PART 2*/
			finalErrorChecking();
			/* END OF SECTION 3: ERROR CHECKING PART 2*/

			/* START OF SECTION 4: FILE OUTPUT */
			fileOutput();
			/* END OF SECTION 4: FILE OUTPUT */
		}
		catch (Exception e)
		{System.out.println("Something is on fire");}
	} // End of the main method

	/**
	 * Identifies any last-round error checking that is required
	 */
	private static void finalErrorChecking()
	{
		// [CASE 0] CHECKS IF SYMBOL IS DEFINED BUT NOT USED
		Set<String> keys = symbolTable.keySet();
		for(String key: keys)
		{
			if ((symbolTable.get(key).isUsed() == false) && (key != null))
				errors.add(key + " was defined but never used");
		} // End of case 0
	} // End of the final error checking method

	/**
	 * Maps everything without reading from the input a second time
	 */
	private static void process()
	{
		int operand = 0;
		int currentText = 0;
		int externalLineOffset = 0;
		int inlineIndividualProgramTextCount = 0;
		// Iterates through all the program texts
		for (int programCount = 0; programCount < programText.size(); ++programCount)
		{
			currentText = programText.get(programCount).getText();
			operand =  currentText % 10; // I is teh smarts. Hear me rawr.
			int middleBlock = Integer.parseInt(Integer.toString(currentText).substring(1, 4));

			if (operand == 2)
			{
				// [CASE 0] [NO CHANGE]: Absolute address
				if (middleBlock >= MACHINE_SIZE)
				{
					// [CASE 0.1] [CHANGE]: Absolute address exceeds the size of the machine
					String largestValidAddress = Integer.toString(MACHINE_SIZE - 1); 
					int overflow = Integer.parseInt(Integer.toString(currentText).substring(0,1) + largestValidAddress);
					programText.get(programCount).setText(overflow);
					errors.add("Absolute address exceeds machine size; largest address of " + largestValidAddress 
							+ " used in module " + programCount);
				}
				else
				{
					// [CASE 0] [NO CHANGE]: Absolute address is valid
					int truncatedOriginal = Integer.parseInt(Integer.toString(currentText).substring(0, 4));
					programText.get(programCount).setText(truncatedOriginal);
				}
			}
			else if (operand == 1)
			{
				// [CASE 2] [NO CHANGE]: IMMIEDIATE OPERAND
				int truncatedOriginal = Integer.parseInt(Integer.toString(currentText).substring(0, 4));
				programText.get(programCount).setText(truncatedOriginal);
			} // End of the immediate mapping
			else if (operand == 3)
			{
				// [CASE 3] [CHANGE]: RELATIVE ADDRESS
				// NOTE: THE FOLLOW CODE WORKS BECAUSE MATHS. ANY ATTEMPT TO RATIONALISE THIS CODE WILL MAKE YOUR HEAD EXPLODE.
				// YOU'VE BEEN WARNED.
				ProgramText currentProgram = null;
				int internalProgramCount = 0;
				for (int i = 0; i < programText.size(); ++i)
				{
					if (programText.get(i).getText() == currentText)
					{
						currentProgram = programText.get(i);
						internalProgramCount = i;
						break;
					}
				}
				int programTextOriginalLine = currentProgram.getLine();

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
				int truncatedOriginal = Integer.parseInt(Integer.toString(currentProgram.getText()).substring(0, 4));
				int newText = truncatedOriginal + textOffset;

				int indicator = indicatorList.get(currentProgram.getLine());
				if (operand > indicator)
				{
					// [ERROR]: Relative address exceeds the size of the module
					errors.add("Relative address exceeds module size; largest module address used in module " + internalProgramCount);
					int opCode = Integer.parseInt(Integer.toString(currentProgram.getText()).substring(0, 1));
					if ((internalProgramCount > 99) && (internalProgramCount < 1000))
					{
						// [CASE 3.0]: Internal is 3-digits
						newText = Integer.parseInt(Integer.toString(opCode) + internalProgramCount);
					}
					else if ((internalProgramCount > 9) && (internalProgramCount < 100))
					{
						// [CASE 3.1]: Internal is 2-digits
						newText = Integer.parseInt(Integer.toString(opCode) + "0" + internalProgramCount);
					}
					else if ((internalProgramCount >= 0) && (internalProgramCount < 10))
					{
						// [CASE 3.2]: Internal is 1-digit
						newText = Integer.parseInt(Integer.toString(opCode) + "00" + internalProgramCount);
					}
				}
				currentProgram.setText(newText);
			} // End of the relative address mapping
			else if (operand == 4)
			{
				// [CASE 4] [CHANGE]: EXTERNAL ADDRESS
				ProgramText currentProgram = null;
				Use currentUse = null;	
				for (int i = 0; i < programText.size(); ++i)
				{
					if (programText.get(i).getText() == currentText)
					{
						currentProgram = programText.get(i);
						break;
					}
				}

				for (int i = 0; i < useList.size(); ++i)
				{
					if ((useList.get(i).getLine() + 1 == currentProgram.getLine()) &&
							useList.get(i).getAddress() == currentProgram.getPosition())
					{
						currentUse = useList.get(i);
						break;
					}
				}

				Symbol currentSymbol = symbolTable.get(currentUse.getSymbol());
				if (currentSymbol == null)
				{
					// [CASE 4.0] [ERROR] Symbol is used but not defined
					errors.add(currentUse.getSymbol() + " is not defined; 111 used");
					String opCode = Integer.toString(currentText).substring(0, 1);
					int newText = Integer.parseInt(opCode + "111");
					currentProgram.setText(newText);
				}
				else
				{
					currentSymbol.setUsed(true);
					int attachedDefinition = currentSymbol.getDefinition();
					if ((99 < attachedDefinition) && (attachedDefinition < 1000))
					{
						// Case 4.1: symbol has a size of 3
						String opCode = Integer.toString(currentText).substring(0, 1);
						int newText = Integer.parseInt(opCode + Integer.toString(attachedDefinition));
						currentProgram.setText(newText);
					} // End of case 4.1
					else if ((9 < attachedDefinition) && (attachedDefinition < 100))
					{
						// Case 4.2: symbol has a size of 2
						String opCode = Integer.toString(currentText).substring(0, 1);
						int newText = Integer.parseInt(opCode + "0" + Integer.toString(attachedDefinition));
						currentProgram.setText(newText);
					} // End of case 4.2
					else if ((0 <= attachedDefinition) && (attachedDefinition < 10)) 
					{
						// Case 4.3: symbol has a size of 1
						String opCode = Integer.toString(currentText).substring(0, 1);
						int newText = Integer.parseInt(opCode + "00" +Integer.toString(attachedDefinition));
						currentProgram.setText(newText);
					} // End of case 4.3
				}
				++inlineIndividualProgramTextCount;
			} // End of the external address mapping
		} // End of the program text iteration
	} // End of the process method

	/**
	 * Outputs to file all symbol definitions, memory address mappings, and any errors detected.
	 */
	private static void fileOutput()
	{
		PrintWriter writer = null;
		try {
			// Checks for bin directory existance
			File binDirectory = new File("bin");
			if (binDirectory.exists())
			{
				if (binDirectory.isDirectory())
				{
					// Does nothing
				}
			}
			else
			{binDirectory.mkdir();}

			System.out.println("Outputting file to Linker.output");
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
			{writer.println(i + ":\t" + programText.get(i).getText());}

			writer.println();
			// Outputs to file any errors encountered
			for (int error = 0; error < errors.size(); ++error)
			{writer.println("Warning: " + errors.get(error) + ".");}
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			System.err.println("ERROR: Write permissions are required in order to output file,"
					+ " please escalate privileges or run in allowed directory");
		}
		finally
		{writer.close();}
	} // End of the file output method

	/**
	 * Identifies any first-round error checking that is required
	 */
	private static void firstErrorChecking()
	{
		// [CASE 0] CHECKS FOR MULTIPLE SYMBOLS USED IN SAME INSTRUCTION
		for (int useX = 0; useX < useList.size(); ++useX)
		{
			Use firstUseToBeCompared = useList.get(useX);
			for (int useY = 0; useY < useList.size() - 1; ++useY)
			{
				Use secondUseToBeCompared = useList.get(useY);
				if (useX == useY)
				{
					// [CASE 0.0] Does nothing, is the same use, so no point in being compared
				}
				else
				{
					// [CASE 0.1] Different uses, so must compare
					if ((firstUseToBeCompared.getLine() == secondUseToBeCompared.getLine()) &&
							(firstUseToBeCompared.getAddress() == secondUseToBeCompared.getAddress())
							&& (!firstUseToBeCompared.getSymbol().equals(secondUseToBeCompared.getSymbol())))
					{
						// Different symbol for the same use, will need to use the latest one
						if (useX < useY)
						{
							// [CASE 0.1.0] USE_X IS FIRST, useY's symbol will be used
							errors.add("Multiple variables used in instruction; all but last ignored in module " + 
									useList.get(useX).getLine());
							useList.remove(useX);
						}
						else
						{
							// [CASE 0.1.1] USE_Y IS FIRST, useX's symbol will be used
							errors.add("Multiple variables used in instruction; all but last ignored in line " + 
									useList.get(useY).getLine());
							useList.remove(useY);
						}
					}
				}
			}
		} // End of case 0: checking for multiple symbols in the same instruction
	} // End of the error checking method

	/**
	 * Reads in from file, and populates symbol and uses tables
	 */
	private static void firstPass()
	{
		// File IO
		Scanner input = new Scanner(System.in);
		try
		{
			String currentToken = null;
			int lineCount = 0; // Can also be thought of as an indicator count, and is the actual line count
			int moduloLineCount = 0; // A pseudo line count so the modulo operator can be utilized for code clarity
			int offset = 0; // The offset in the number of program texts so far
			// Reads in the file from standard input
			while (input.hasNext())
			{
				currentToken = input.next();
				int indicator = Integer.parseInt(currentToken);
				indicatorList.add(indicator);

				// First read will always be guaranteed to be an indicator (scanner will auto advance until so)
				moduloLineCount = lineCount + 1;

				if (moduloLineCount >= 3)
					moduloLineCount %= 3;

				if (moduloLineCount % 3 == 0)
				{
					// [CASE 1]: Current line is a program text line
					// Blindly adds all program text to the array
					lineOffsets.add(indicator);
					for (int inLineProgramTextCount = 0; inLineProgramTextCount < indicator; ++inLineProgramTextCount)
					{
						int text = Integer.parseInt(input.next());
						ProgramText newProgramText = new ProgramText(text, lineCount, inLineProgramTextCount);
						RobustOSLinker.programText.add(newProgramText);
						++offset;
					}
				} // End of case 1: current line is a program text line
				else if (moduloLineCount % 2 == 0)
				{
					// [CASE 2]: Current line is a use line
					// Iterates through the current use line, adding all uses to the array
					for (int numberOfSymbolsUsed = 0; numberOfSymbolsUsed < indicator; ++numberOfSymbolsUsed)
					{	
						String currentSymbolName = input.next();
						int currentUseLocation = Integer.parseInt(input.next());
						// Iterates through until reading the terminating number (-1)
						while (currentUseLocation != -1)
						{
							Use newUse = new Use(currentSymbolName, currentUseLocation, lineCount);
							useList.add(newUse);
							currentUseLocation = Integer.parseInt(input.next()); // Advances to the next use address or terminating num
						}
					}
				} // End of case 2: current line is a use line
				else
				{
					// [CASE 3]: Current line is a symbol definition line
					String currentSymbolName = null;
					int currentSymbolDefinition = 0;

					// Iterates through the current symbol line, adding all symbols to the hash table
					for (int currentTokenInSymbolLine = 0; currentTokenInSymbolLine < indicator; ++currentTokenInSymbolLine)
					{
						currentSymbolName = input.next(); // 1st read is always a symbol
						currentSymbolDefinition = Integer.parseInt(input.next()) + offset;
						Symbol newSymbol = new Symbol(currentSymbolName, currentSymbolDefinition);

						// Checks if the symbol already exists
						if (symbolTable.containsKey(currentSymbolName))
						{
							// Symbol was found, updates symbol instead of making new one
							symbolTable.replace(currentSymbolName, newSymbol);
							errors.add("The variable " + currentSymbolName + 
									" is multiply defined; last value of " + currentSymbolDefinition + " will be used");
						}
						else
						{symbolTable.put(currentSymbolName, newSymbol);}
					}
				} // End of case 3: current line is a symbol definition line
				// After line read is completed, increments lineCount and continues
				++lineCount;
			} // End of input reading
		}
		catch (Exception e)
		{
			System.err.println("First pass is on fire");
			System.err.println(e.getMessage());
		}
		finally
		{input.close();}
	} // End of the first pass method

	/* START OF HELPER METHODS */

	/**
	 * Given a string input, checks to see if the value can be parsed into a number
	 * @param input some string input
	 * @return true if input is numeric, false if input is not numeric
	 */
	private static boolean isNumeric(String input)  
	{  
		try  
		{Integer.parseInt(input);}  
		catch(NumberFormatException nfe)  
		{return false;}  
		return true;  
	}
	/* END OF HELPER METHODS */

	//	// Global statically available variables
	//	private static Hashtable<String, Symbol> symbolTable = null;
	//	private static ArrayList<ProgramText> programText = null;
	//	private static ArrayList<Integer> lineOffsets = null;
	//	private static ArrayList<Use> useList = null;
	//
	//	/**
	//	 * @param args
	//	 */
	//	public static void main(String[] args)
	//	{		
	//		/* SECTION 0: FILE INPUT */
	//		Scanner input = new Scanner(System.in);
	//
	//		/* SECTION 1: 1ST PASS */
	//
	//		/* VARIABLE INITIALISATIONS */
	//		symbolTable = new Hashtable<String, Symbol>(); // Creates a hash table
	//		programText = new ArrayList<ProgramText>(); // Creates the program text array
	//		lineOffsets = new ArrayList<Integer>(); // Creates the line offsets used in relative address mapping
	//		lineOffsets.add(0); // Adds the base addition
	//		useList = new ArrayList<Use>();
	//		int lineCount = 0;
	//		int baseAddress = 0;
	//		int offset = 0;
	//		boolean isIndicator = true;
	//		int internalLineCount = 0;
	//
	//		/* END OF SECTION 1: 1ST PASS */
	//		// Iterates through the file
	//		while(input.hasNextLine())
	//		{
	//			String currentLine = input.nextLine();
	//
	//			// Sets the internal line count to see which part of the module we're at
	//			int moduloLineCount = lineCount + 1;
	//			if (moduloLineCount > 3)
	//				moduloLineCount = moduloLineCount % 3;
	//
	//			if (moduloLineCount % 3 == 0)
	//			{
	//				// Case 1: line is a program text line
	//				// Iterates through the program text line
	//				StringTokenizer tokenizer = new StringTokenizer(currentLine);
	//				String currentText = null;
	//				int tokenCount = 0;
	//				while (tokenizer.hasMoreTokens())
	//				{
	//					currentText = tokenizer.nextToken();
	//					if (isIndicator)
	//					{
	//						isIndicator = false;
	//						lineOffsets.add(Integer.parseInt(currentText));
	//						// Case 1.1: token is an indicator number, and so skips it
	//					}
	//					else
	//					{
	//						// Case 1.2: token is program text, increments the offset and adds it to the arraylist
	//						++offset;
	//						ProgramText newText = new ProgramText(Integer.parseInt(currentText), lineCount, tokenCount);
	//						programText.add(newText);
	//						++internalLineCount;
	//					}
	//					++tokenCount;
	//				}
	//			}
	//			else if (moduloLineCount % 2 == 0)
	//			{
	//				// Case 2: line is a use line
	//				// Iterates through the program text line
	//				StringTokenizer tokenizer = new StringTokenizer(currentLine);
	//				String currentText = null;
	//				String currentSymbol = null;
	//				while (tokenizer.hasMoreTokens())
	//				{
	//					currentText = tokenizer.nextToken();
	//					if (isIndicator)
	//					{
	//						// Case 2.1: token is an indicator number, and so skips it
	//						isIndicator = false;
	//					}
	//					else
	//					{
	//						// Case 2.2: token is either a symbol, a use, or a deliminator (-1)
	//						if (currentText.equals("-1"))
	//						{
	//							// Case 2.2.1: token is a deliminator (-1), and does nothing
	//						}
	//						else if (isNumeric(currentText))
	//						{
	//							// Case 2.2.2: token is a use, adds it to the symbol's use
	//							Symbol actualSymbol = symbolTable.get(currentSymbol);
	//							if (actualSymbol == null)
	//							{
	//								System.err.println("ERROR: Symbol is used but not defined, using value 111");
	//								// Creates a new symbol with a definition of 111
	//								Symbol newSymbol = new Symbol(currentSymbol, 111);
	//								symbolTable.put(currentSymbol, newSymbol);
	//
	//								// Creates a new use
	//								int position = internalLineCount + Integer.parseInt(currentText);
	//								useList.add(new Use(newSymbol, Integer.parseInt(currentText), internalLineCount, 
	//										position));
	//							}
	//							else
	//							{
	//								useList.add(new Use(actualSymbol, Integer.parseInt(currentText), internalLineCount,
	//										internalLineCount + Integer.parseInt(currentText)));
	//							}
	//						}
	//						else
	//						{
	//							// Case 2.2.3: token is a symbol, sets the current symbol to it
	//							currentSymbol = currentText;
	//						}
	//					}	
	//				}
	//			}		
	//			else
	//			{
	//				// Case 3: line is a symbol definition line
	//				StringTokenizer tokenizer = new StringTokenizer(currentLine);
	//				String currentSymbol = null;
	//
	//				// Iterates through the symbol definition line
	//				while (tokenizer.hasMoreTokens())
	//				{
	//					String currentToken = tokenizer.nextToken();
	//					if (isIndicator)
	//					{
	//						isIndicator = false;
	//						// Case 3.1: token is an indicator
	//					}
	//					else if (!isNumeric(currentToken))
	//					{
	//						// Case 3.2: token is a symbol
	//						currentSymbol = currentToken;
	//
	//						// Checks if the symbol is in the symbol table
	//						if (symbolTable.containsKey(currentSymbol))
	//						{
	//							
	//							// Case 3.2.1: symbol is in the symbol table [ERROR, symbol is doubly defined]
	//							System.err.println("ERROR: Symbol " + currentSymbol + " is doubly defined, using latest definition"
	//									+ " with " );
	//						}
	//						else
	//						{
	//							// Case 3.2.2: symbol is not in the symbol table
	//							// Adds a new symbol to the symbol table
	//							Symbol newSymbol = new Symbol(currentSymbol, -1);
	//							symbolTable.put(currentSymbol, newSymbol);
	//						}		
	//					}
	//					else
	//					{
	//						// Case 3.3: token is a symbol definition
	//						// Adds the use to the current symbol value
	//						int symbolDefinition = Integer.parseInt(currentToken) + baseAddress + offset;
	//						symbolTable.get(currentSymbol).setDefinition(symbolDefinition);
	//					}
	//				}
	//			}
	//			++lineCount;
	//			isIndicator = true;
	//		}
	//		input.close(); // Reached the end of the file
	//
	//		/* Builds each program line */
	//		ArrayList<String> programTextLines = new ArrayList<String>();
	//		int currentLineBeingAdded = programText.get(1).getLine();
	//		String currentProgramTextBeingAdded = "";
	//
	//		for (int i = 0; i < programText.size(); ++i)
	//		{
	//			if (currentLineBeingAdded == programText.get(i).getLine())
	//			{
	//				currentProgramTextBeingAdded = currentProgramTextBeingAdded + programText.get(i).getText() + " ";
	//			}
	//			else
	//			{
	//				programTextLines.add(currentProgramTextBeingAdded);
	//				currentProgramTextBeingAdded = "";
	//				currentLineBeingAdded = programText.get(i).getLine();
	//				currentProgramTextBeingAdded = currentProgramTextBeingAdded + programText.get(i).getText() + " ";
	//			}
	//		}
	//		programTextLines.add(currentProgramTextBeingAdded);
	//
	//		/* SECTION 2: 2ND PASS */
	//		int externalLineOffset = 0;
	//		int inlineIndividualProgramTextCount = 0;
	//		for (int programLine = 0; programLine < programTextLines.size(); ++programLine)
	//		{
	//			// Tokenizes each line
	//			StringTokenizer currentProgramTextTokenizer = new StringTokenizer(programTextLines.get(programLine));
	//			while (currentProgramTextTokenizer.hasMoreTokens())
	//			{
	//
	//				String currentProgramTextToken = currentProgramTextTokenizer.nextToken();
	//				int operand = Integer.parseInt(currentProgramTextToken) % 10;
	//				ProgramText currentNode = null;
	//
	//				for (int i = 0; i < programText.size(); ++i)
	//				{
	//					if (programText.get(i).getText() == Integer.parseInt(currentProgramTextToken))
	//						currentNode = programText.get(i);
	//				}
	//
	//				// Begins iterating through each of the program texts
	//				if ((operand == 1) || (operand == 2))
	//				{
	//					// Case 1: program text has an immediate operand [UNCHANGED] OR Case 2: program text has an absolute address [UNCHANGED]
	//					int truncatedOriginal = Integer.parseInt(currentProgramTextToken.substring(0, 4));
	//					currentNode.setText(truncatedOriginal);
	//					++inlineIndividualProgramTextCount;
	//				}
	//				else if (operand == 3)
	//				{
	//					// Case 3: program text has a relative address [CHANGED]
	//					// NOTE: THE FOLLOW CODE WORKS BECAUSE MATHS. ANY ATTEMPT TO RATIONALISE THIS CODE WILL MAKE YOUR HEAD EXPLODE.
	//					// YOU'VE BEEN WARNED
	//					int programTextOriginalLine = currentNode.getLine();
	//
	//					int deleteCount = 0;
	//					while (programTextOriginalLine > -1)
	//					{
	//						programTextOriginalLine -= 3;
	//						++deleteCount;
	//					}
	//
	//					int textOffset = 0;
	//					for (int arrayIterator = 0; arrayIterator < deleteCount; ++arrayIterator)
	//					{textOffset += lineOffsets.get(arrayIterator);}
	//					// Text offset has now added all program line offsets prior to the current line
	//
	//					// Builds the new 4-digit address
	//					int truncatedOriginal = Integer.parseInt(currentProgramTextToken.substring(0, 4));
	//					int newText = truncatedOriginal + textOffset;
	//
	//					if (newText > 9999)
	//					{
	//						// relative address exceeds the size of the module [ERROR]
	//						System.err.println("ERROR: relative address exceeds the size of the module, using the last module address");
	//						newText = Integer.parseInt(currentProgramTextToken.substring(0,4));
	//					}
	//					currentNode.setText(newText);
	//				}
	//				else if (operand == 4)
	//				{
	//					// Case 4: program text has an external address [CHANGED]
	//					ProgramText currentProgram = null;
	//					Use currentUse = null;	
	//					for (int i = 0; i < programText.size(); ++i)
	//					{
	//						if (programText.get(i).getText() == Integer.parseInt(currentProgramTextToken))
	//						{
	//							currentProgram = programText.get(i);
	//							break;
	//						}
	//					}
	//
	//					for (int i = 0; i < useList.size(); ++i)
	//					{
	//						if (programText.get(useList.get(i).getId()).getText() == currentProgram.getText())
	//						{
	//							currentUse = useList.get(i);
	//							break;
	//						}
	//					}
	//
	//					int attachedDefinition = currentUse.getSymbol().getDefinition();
	//					if ((99 < attachedDefinition) && (attachedDefinition < 1000))
	//					{
	//						// Case 4.1: symbol has a size of 3
	//						String opCode = currentProgramTextToken.substring(0, 1);
	//						int newText = Integer.parseInt(opCode + Integer.toString(attachedDefinition));
	//						currentProgram.setText(newText);
	//					} // End of case 4.1
	//					else if ((9 < attachedDefinition) && (attachedDefinition < 100))
	//					{
	//						// Case 4.2: symbol has a size of 2
	//						String opCode = currentProgramTextToken.substring(0, 2);
	//						int newText = Integer.parseInt(opCode + Integer.toString(attachedDefinition));
	//						currentProgram.setText(newText);
	//					} // End of case 4.2
	//					else if ((0 <= attachedDefinition) && (attachedDefinition < 10)) 
	//					{
	//						// Case 4.3: symbol has a size of 1
	//						String opCode = currentProgramTextToken.substring(0, 3);
	//						int newText = Integer.parseInt(opCode + Integer.toString(attachedDefinition));
	//						currentProgram.setText(newText);
	//					} // End of case 4.3
	//					++inlineIndividualProgramTextCount;
	//
	//					/* END OF SECTION 2: 2ND PASS */
	//				}
	//			}
	//			// Line has ended, and so add up the individual program texts
	//			externalLineOffset = externalLineOffset + inlineIndividualProgramTextCount;
	//			inlineIndividualProgramTextCount = 0;
	//		}
	//
	//		/* SECTION 3: FILE OUTPUT */
	//		PrintWriter writer = null;
	//		try {
	//			// Checks for bin directory existance
	//			File binDirectory = new File("bin");
	//			if (binDirectory.exists())
	//			{
	//				if (binDirectory.isDirectory())
	//				{
	//					// Does nothing
	//				}
	//			}
	//			else
	//			{binDirectory.mkdir();}
	//
	//			writer = new PrintWriter("Linker.output", "UTF-8");
	//			// Outputs to file the symbol definitions
	//			Enumeration<String> symbols = symbolTable.keys();
	//			String symbolKey;
	//			writer.println("Symbol Table");
	//			while(symbols.hasMoreElements())
	//			{
	//				symbolKey = (String) symbols.nextElement();
	//				Symbol printSymbol = symbolTable.get(symbolKey);
	//				writer.println(printSymbol.getName() + "=" + printSymbol.getDefinition());
	//			}
	//
	//			writer.println();
	//			writer.println("Memory Map:");
	//			// Outputs to file the symbol definitions
	//			for (int i = 0; i < programText.size(); ++i)
	//			{
	//				writer.println(i + ":\t" + programText.get(i).getText());
	//			}
	//		} catch (FileNotFoundException | UnsupportedEncodingException e) {
	//			System.err.println("ERROR: Write permissions are required in order to output file,"
	//					+ " please escalate privileges or run in allowed directory");
	//		}
	//		finally
	//		{writer.close();}
	//
	//		/* END OF SECTION 3: FILE OUTPUT */
	//	} // End of the main class
	//
	//	/* Helper methods */
	//	private static boolean isNumeric(String input)  
	//	{  
	//		try  
	//		{Integer.parseInt(input);}  
	//		catch(NumberFormatException nfe)  
	//		{return false;}  
	//		return true;  
	//	}


} // End of the robust os linker class
