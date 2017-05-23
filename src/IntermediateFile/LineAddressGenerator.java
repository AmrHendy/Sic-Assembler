package IntermediateFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import data.Data;
import elements.EquateHandler;
import elements.ORGHandler;
import exception.StatementException;
import statement.IStatement;
import statement.Operation;
import tools.Checker;

public class LineAddressGenerator {

    private List<String> intermediateContent;
    private int currentLineAddress, nextLineAddress;
    public static boolean error;
    /**********************Flag for error in EQU***********************/
    private boolean errorInEqu = false;

    public LineAddressGenerator() {
        intermediateContent = new ArrayList<String>();
        currentLineAddress = Checker.convertFromHexaToDeca(new String("000000"));
        nextLineAddress = currentLineAddress;
        error = false;
    }

    public void setInitialAddress(String startOperand) {
        currentLineAddress = Checker.convertFromHexaToDeca(startOperand.substring(3, startOperand.length() - 1));
        nextLineAddress = currentLineAddress;
    }


 
	//general for original statmenets like operations and directives 
	public String appendStatement(String originalStatement, String statementContent[], IStatement statement)
			throws StatementException {
		if (statement instanceof Operation) {
			nextLineAddress = calculateNextLineAddress();
		} else {
			nextLineAddress = calculateNextLineAddress(statementContent);
		}
		// 02_generate current line for intermediate file
		if (currentLineAddress >= Math.pow(2, 15) || nextLineAddress >= Math.pow(2, 15) ||(currentLineAddress<0)||(nextLineAddress<0)) {
			nextLineAddress = currentLineAddress;
			throw new StatementException("Out of Memory Range");
		}
		String address = Checker.getHexaFromDecimal(String.valueOf(currentLineAddress));
		if(statement.getOpName().equalsIgnoreCase("EQU")){
			address = EquateHandler.getAddressValue(statementContent[2].trim(),currentLineAddress);
			if (Checker.convertFromHexaToDeca(address) >= Math.pow(2, 15) || (Checker.convertFromHexaToDeca(address)<0)){
				throw new StatementException("Out of Memory Range");
			}
        }
		
        String line = address + "    " + originalStatement;
        intermediateContent.add(line);
        currentLineAddress = nextLineAddress;
        return address;
    }

    // for addition literal statements
    public String appendStatement(String literalStatement, int literalLength) throws StatementException {
        if (currentLineAddress >= Math.pow(2, 15) || nextLineAddress >= Math.pow(2, 15)) {
            nextLineAddress = currentLineAddress;
            throw new StatementException("Out of Memory Range");
        }
        String address = Checker.getHexaFromDecimal(String.valueOf(currentLineAddress));
        String line = address + "    " + literalStatement;
        intermediateContent.add(line);
        nextLineAddress = currentLineAddress + literalLength;
        currentLineAddress = nextLineAddress;
        return address;
    }
    

    public void appendError(String originalStatement, String errorMessege) {
        error = true;
        intermediateContent.add(new String(". ****" + errorMessege));
        intermediateContent
                .add(Checker.getHexaFromDecimal(String.valueOf(currentLineAddress)) + "    " + originalStatement);
    }

    public void appendComment(String originalStatement) {
        intermediateContent.add(new String("          " + originalStatement));
    }

    public List<String> getContent() {
        return this.intermediateContent;
    }

    private int calculateNextLineAddress(String currentStatement[]) {
        if (currentStatement[1].equalsIgnoreCase("RESW")) {
            nextLineAddress += 3 * Integer.parseInt(currentStatement[2]);
        } else if (currentStatement[1].equalsIgnoreCase("RESB")) {
            nextLineAddress += Integer.parseInt(currentStatement[2]);
        } else if (currentStatement[1].equalsIgnoreCase("WORD")) {
            nextLineAddress += 3;
        } else if (currentStatement[1].equalsIgnoreCase("BYTE")) {
            String chaPattern = "(C*?)(')(.+)(')";
            Matcher queryMatcher = Pattern.compile(chaPattern).matcher(currentStatement[2]);
            if (queryMatcher.matches()) {
                String value = queryMatcher.group(3);
                nextLineAddress += value.length();
            }
            String hexPattern = "(X*?)(')(.+)(')";
            queryMatcher = Pattern.compile(hexPattern).matcher(currentStatement[2]);
            if (queryMatcher.matches()) {
                String value = queryMatcher.group(3);
                nextLineAddress += Math.ceil(value.length() * 1. / 2.);
            }    
        }
        // ORG
        if (currentStatement[1].equalsIgnoreCase("ORG")){
        	return ORGHandler.execute(currentStatement[2], currentLineAddress);
        }
        return nextLineAddress;
    }

    private int totalValue(String line, int start, int sign) {
        StringBuilder tmp = new StringBuilder();
        int i = start;
        for (; i < line.length(); i++) {
            if (line.charAt(i) == '+') {
                sign = 1;
                i++;
                break;
            } else if (line.charAt(i) == '-') {
                sign = -1;
                i++;
                break;
            }
            tmp.append(line.charAt(i));
        }
        return getValue(tmp.toString()) * sign + totalValue(line, start, sign);
    }

    private int getValue(String symbol) {
        int value = -1;
        if (Data.symbolTable.containsKey(symbol)) {
            value = Checker.convertFromHexaToDeca(Data.symbolTable.get(symbol));
        } else if (isDecimalNumber(symbol)) {
            value = Integer.valueOf(symbol);
        }
        if (value == -1) {
            errorInEqu = true;
        }
        return value;
    }

    private boolean isDecimalNumber(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) > '9' || line.charAt(i) < '0') {
                return false;
            }
        }
        return true;
    }

    private int calculateNextLineAddress() {
        return nextLineAddress + 3;
    }
}