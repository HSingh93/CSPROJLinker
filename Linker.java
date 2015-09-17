import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Linker Lab # 1
 * @author Harminder Singh 
 * @email HPS251@nyu.edu
 *
 */
public class Linker {
	//Used in firstPass to define symbol values
	private static int relocationConstant;
	//Contains symbol + value of symbol
	private static Map<String, Integer> symbolTable;
	//Contains all addresses
	private static Map<String, Integer> baseAddress;
	//List of smybol names, used for error checking
	private static ArrayList<String> symbolNames;
	//List of address values after resolving external references
	private static ArrayList<Integer> addressList;
	//List of # program text, used for pass two error checking
	private static ArrayList<Integer> relocList;
	//List used for error checking uses (Object module part 2)
	private static Map<Integer, String> useList;
	//Contains uses  
	private static ArrayList<String> useListName;
	//Contains module definitions - used for error checking
	private static Map<String, Integer> moduleDefinition;
	//List used for error checking
	private static ArrayList<String> errorList;
	//Used for error checking with module def
	private static Map<String, Integer> moduleDefinitionIgnored;
	//Used for error checking, aids moduleDefinitionIgnored map
	private static ArrayList<String> moduleDefIgnored;
	
	/**
	 * This function takes in a filename string and returns a scanner object out of it.
	 * It uses a bufferedReader and FileReader to create the scanner object.
	 * @param fileName - Reads string filename, and returns scanner object
	 * @return - Scanner object 
	 */
	public static Scanner getFile(String fileName) {
			Scanner inputFile = null;
		try {
			inputFile = new Scanner(new BufferedReader(new FileReader(fileName)));
		}
		catch(Exception e) {
			System.out.println("Error reading file " + fileName + ". Program is exited.");
			System.exit(1);
		}
		return inputFile;
	}
	
	/**
	 * This function performs the first pass. 
	 * Creates symbolTable and assigns values to symbols
	 * Stores base addresses
	 * @param inputFile - Scanner object used to traverse through
	 */
	private static void passOne(Scanner inputFile) {
		
		//Increase based on program text
		relocationConstant = 0;
		errorList = new ArrayList<String>();
		useList = new HashMap<Integer, String>();
		//Keep track of which module the symbol was defined in
		moduleDefinition = new HashMap<String, Integer>();
		addressList = new ArrayList<Integer>();
		ArrayList<String> multipleDefinitions = new ArrayList<String>();
		symbolNames = new ArrayList<String>();
		symbolTable = new HashMap<String, Integer>();
		baseAddress = new HashMap<String, Integer>();
		relocList = new ArrayList<Integer>();
		relocList.add(relocationConstant);
		useListName = new ArrayList<String>();
		String symbolVal = null;
		int symbolAddress = 0;
		int moduleNum = 0;
		
		int numLine = 0;
		int numLineCount = 0;
		
		while(inputFile.hasNext()) {
			
			numLine = inputFile.nextInt();
			numLineCount++;
			
			//Object module part 1 check -> Definitions
			if((numLineCount - 1) % 3 == 0) {
				for(int i = 0; i < numLine; i ++) {
					if(inputFile.hasNext()) {
						symbolVal = inputFile.next();
					}
					if(inputFile.hasNext()) {
						symbolAddress = inputFile.nextInt() + relocationConstant;
					}
					if(!symbolTable.containsKey(symbolVal)) {
						symbolTable.put(symbolVal, symbolAddress);
						symbolNames.add(symbolVal);
						moduleDefinition.put(symbolVal, moduleNum);
						baseAddress.put(symbolVal, relocationConstant);
					}
					else{
						multipleDefinitions.add(symbolVal);
						symbolTable.put(symbolVal, symbolAddress);
					}
				}
				moduleNum++;
			}
			
			//Object module part 2 check -> Use cases
			if((numLineCount - 2 ) % 3 == 0 ) {
				int i = 0;
				String symbol = null;
				while(i < numLine) {
					symbolVal = inputFile.next();
					if(!symbolVal.matches("[-+]?\\d*\\.?\\d+")) {
						symbol = symbolVal;
					}
					else{
						if(Integer.parseInt(symbolVal) == -1) {
							i++;
						}
						else {
							int used = Integer.parseInt(symbolVal) + relocationConstant;
							if(!useListName.contains(symbol)) {
								useListName.add(symbol);
							}
							if(useList.containsKey(used)) {
								errorList.add(symbol);
							}
							useList.put(used, symbol);
						}
					}
				}
			}
			
			//Object module part 3 check -> Program text
			if((numLineCount - 3) % 3 == 0 ) {
				int i = 0;
				relocationConstant += numLine;
				relocList.add(relocationConstant);
				while(i < numLine) {
					symbolAddress = inputFile.nextInt();
					addressList.add(symbolAddress);
					i++;
				}
			}
			
		}
		//Print display after first pass (Symbol table + errors encountered in first pass)
		System.out.println("Symbol Table");
		for(String name : symbolNames) {
			if(symbolTable.get(name) > relocationConstant) {
				symbolTable.put(name, relocationConstant);
				System.out.println(name + "=" + symbolTable.get(name) + " Error: Definition exceeds module size; last word in module used. ");
			}
			else {
				if(multipleDefinitions.contains(name)) {
					System.out.println(name + "=" + symbolTable.get(name) + " Error: This variable is multiply defined; last value used.");
				}
				else {
					System.out.println(name + "=" + symbolTable.get(name));
				}
			}
		}
		System.out.println("");
		
	}
	
	/**
	 * This is the second pass. It resolves external references and catches errors accompanied with resolving addresses.
	 * @param inputFile - Scanner object used to traverse through file
	 */
	private static void passTwo(Scanner inputFile) {
		
		int numLine = 0;
		int numLineCount = 0;
		String symbolVal = null;
		int symbolAddress = 0;
		int currIndex = 0;
		int baseAddress = 0;
		
		int startRelocIndex = 1;
		int finRelocIndex = 1;
		if(relocList.size() > 2) {
			finRelocIndex = 2;
		}
		int relocVal = relocList.get(startRelocIndex);
		int moduleNum = 0;
		moduleDefinitionIgnored = new HashMap<String, Integer>();
		moduleDefIgnored = new ArrayList<String>();
		
		while(inputFile.hasNext()) {
			
			numLine = inputFile.nextInt();
			numLineCount++;
			
			//Object module part 1 check -> Definitions
			if((numLineCount - 1) % 3 == 0) {
				//Already defined symbols, therefore just loop past them
				for(int i = 0; i < numLine; i ++) {
					if(inputFile.hasNext()) {
						symbolVal = inputFile.next();
					}
					if(inputFile.hasNext()) {
						symbolAddress = inputFile.nextInt() + relocationConstant;
					}
				}
			}
			
			//Object module part 2 check -> Use cases
			if((numLineCount - 2) % 3 == 0) {
				int i = 0;
				String symbol = null;
				while(i < numLine) {
					symbolVal = inputFile.next();
					if(symbolVal.matches("[-+]?\\d*\\.?\\d+")) {
						if(Integer.parseInt(symbolVal) == -1) {
							i++;
						}
						else {
							int used = Integer.parseInt(symbolVal);
							if(useList.containsKey(used)) {
								if(used >= relocVal) {
									useList.remove(used);
									moduleDefinitionIgnored.put(symbol, moduleNum);
									moduleDefIgnored.add(symbol);
								}
							}
						}
					}
					else {
						symbol = symbolVal;
					}
				}
				
				startRelocIndex = finRelocIndex;
				if(finRelocIndex < relocList.size() - 1) {
					finRelocIndex++;
				}
				relocVal = relocList.get(startRelocIndex);
				moduleNum++;
			}
			
			//Resolve external addresses / Object module part 3 check -> Program text
			if((numLineCount - 3) % 3 ==0 ) {
				int i = 0;
				String symbol = null;
				while(i < numLine) {
					int rAddress = addressList.get(currIndex);
					if(useList.containsKey(currIndex)) {
						symbol = useList.get(currIndex);
						if(!symbolTable.containsKey(symbol)) {
							System.out.println("Error: Current symbol: " + symbol + " is not defined. Using 111 for location - " + currIndex);
							//Resolve address without symbol definition
							replaceAddress(rAddress, 111, baseAddress, currIndex);
						}
						else {
							int symbVal = symbolTable.get(symbol);
							replaceAddress(rAddress, symbVal, baseAddress, currIndex);
						}
						currIndex++;
					}
					else {
						//Resolve address without symbol
						replaceAddress(rAddress, -1, baseAddress, currIndex);
						currIndex++;
					}
					symbolAddress = inputFile.nextInt();
					i++;
				}
				baseAddress += numLine;
			}
		}
	}
	
	/**
	 * This function resolves addresses based on input parameters.
	 * @param rAddress - Address value to resolve
	 * @param symbolVal - value associated with symbol
	 * @param baseAddress - relative address
	 * @param listLoc - location in final list (addressList)
	 */
	private static void replaceAddress(int rAddress, int symbolVal, int baseAddress, int listLoc) {
		int changeAddress = rAddress;
		//-1 indicates that a symbol isn't used in this resolving
		if(symbolVal == -1) {
			char[] digits = String.valueOf(changeAddress).toCharArray();
			digits = Arrays.copyOf(digits, digits.length-1);
			changeAddress = Integer.parseInt(new String(digits));
			if((rAddress % 10) == 2) {
				int last3 = (changeAddress % 1000);
				if(last3 > 299) {
					System.out.println("Error: Absolute address exceeds machine size; largest address used. - Location " + listLoc);
					changeAddress = (changeAddress/1000)*1000 + 299;
					addressList.set(listLoc, changeAddress);
				}
			}
			if((rAddress % 10) == 3) {
				int replace = (changeAddress % 1000) + baseAddress;
				changeAddress = (changeAddress/1000)*1000 + replace%1000;
				int last3 = (changeAddress % 1000);
				if(last3 > relocationConstant) {
					System.out.println("Error: Relative address exceeds module size; largest module address used. Location - " + listLoc);
					changeAddress = (changeAddress/1000)*1000 + baseAddress;
					addressList.set(listLoc, changeAddress);
				}
				addressList.set(listLoc, changeAddress);
			}
			if((rAddress % 10) == 4 || (rAddress % 10) == 1 || (rAddress % 10) == 2) {
				addressList.set(listLoc, changeAddress);
			}
		}
		//Use symbol + value to resolve address
		else {
			char[] digits = String.valueOf(changeAddress).toCharArray();
			int val = Character.getNumericValue(digits[4]);
			digits = Arrays.copyOf(digits, digits.length-1);
			changeAddress = Integer.parseInt(new String(digits));
			if(val == 2) {
				symbolVal -= baseAddress;
				changeAddress = (changeAddress/1000)*1000 + symbolVal%1000;
				int last3 = (changeAddress % 1000);
				if(last3 > 299) {
					System.out.println("Error: Absolute address exceeds machine size; largest address used. - Location " + listLoc);
					changeAddress = (changeAddress/1000)*1000 + 299;
					addressList.set(listLoc, changeAddress);
				}
				addressList.set(listLoc, changeAddress);
			}
			if(val == 3) {
				symbolVal += baseAddress;
				changeAddress = (changeAddress/1000)*1000 + symbolVal%1000;
				int last3 = (changeAddress % 1000);
				if(last3 > relocationConstant) {
					System.out.println("Error: Relative address exceeds module size; largest module address used. Location - " + listLoc);
					changeAddress = (changeAddress/1000)*1000 + baseAddress;
					addressList.set(listLoc, changeAddress);
				}
				addressList.set(listLoc, changeAddress);
			}
			if(val == 4 || val == 1) {
				changeAddress = (changeAddress/1000)*1000 + symbolVal%1000;
				addressList.set(listLoc, changeAddress);
			}
		}
	}
	
	/**
	 * This is the main function that executes Lab #1.
	 * @param args - May be filename
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		//Use arg[0] as fileName, if not existing, use a scanner to read user input from keyboard (Only once)
		Scanner inputFile = null;
		String fileName = null;
		if(args.length > 0) {
			fileName = args[0];
			inputFile = getFile(args[0]);
		}
		else {
			Scanner sc = new Scanner(System.in);
			System.out.println("Enter filename below: ");
			fileName = sc.nextLine();
			inputFile = getFile(fileName);
		}
		//If inputFile (Scanner was created) execute LAB#1
		if(inputFile != null) {
			//Pass one
			passOne(inputFile);
			//Restart scanner object from beginning of file
			inputFile = getFile(fileName);
			//Pass two
			passTwo(inputFile);
			//Error checks
			if(!errorList.isEmpty()) {
				for(String symbol : errorList) {
					System.out.println("Error: Multiple variables used in instruction; all but last ignored. Instruction used for symbol: " + symbol);
				}
			}
			//Error checks
			if(!moduleDefIgnored.isEmpty()) {
				for(String symbol : moduleDefIgnored) {
					int moduleNum = moduleDefinitionIgnored.get(symbol);
					System.out.println("Error: Use of " + symbol + " in module " + moduleNum + " exceeds module size; use ignored.");
				}
			}
			
			//Print memory map
			System.out.println("");
			System.out.println("Memory Map");
			int i = 0;
			for(Integer address : addressList) {
				if(i < 10) {
					System.out.println(i + ":" + "   " + address);
				}
				else {
					System.out.println(i + ":" + "  " + address);
				}
				i++;
			}
			System.out.println("");
			//Error checks
			for(String names : symbolNames) {
				if(!useListName.contains(names)) {
					int moduleNum = moduleDefinition.get(names);
					System.out.println("Warning: " + names + " was defined in module " + moduleNum + " but never used.");
				}
			}
		}
		//Counldn't open file
		else {
			System.out.println("Error: Couldn't open file! Program is exiting.");
		}
	}
	
}
