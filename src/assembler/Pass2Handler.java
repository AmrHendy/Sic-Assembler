package assembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.Data;
import elements.Literal;
import exception.StatementException;
import obline.imp.EndRecord;
import obline.imp.HeaderRecord;
import obline.imp.TextRecord;
import obline.interfaces.Obline;
import statement.Directive;
import statement.IStatement;
import storage.IntermediateFileHandler;
import storage.ListingFileHandler;
import storage.ObjectCodeHandler;
import tools.Checker;

public class Pass2Handler {

    private Map<String, String> symbolTable;
    private ArrayList<Obline> obLines;
    private List<ArrayList<String>> intermediateFileContent;
    private List<String> intermediateFile, listingFile;
    private boolean error;

    public Pass2Handler(String intermediateFileAddress, String listingFileAddress, String objectFileAddress)
            throws StatementException {
        intermediateFile = new ArrayList<String>();
        listingFile = new ArrayList<String>();
        intermediateFileContent = new ArrayList<ArrayList<String>>();
        symbolTable = new HashMap<String, String>();
        obLines = new ArrayList<>();
        error = false;

        // 01_load file into memory
        IntermediateFileHandler.loadFile(intermediateFileAddress, symbolTable, intermediateFileContent,
                intermediateFile);
        // 02_generate listing file
        generateListingFile();
        // 03_store listing file to disk
        ListingFileHandler.storeFile(listingFile, listingFileAddress);
        if (!error) {
            ObjectCodeHandler.WriteFile(obLines, objectFileAddress);
            System.out.println("Successfully path2 completed");
        } else {
            throw new StatementException("src File is not valid so listing file contains error messeges");
        }
    }

    private void generateListingFile() {
        int ind = 0;
        int len = 0;
        StringBuilder content = new StringBuilder();
        String StartingAddress = new String();
        String endOperand = new String();
        for (String line : this.intermediateFile) {
            ind++;
            String objectCode = "";
            if (line.trim().startsWith(".")) {
                this.listingFile.add(line);
                ind--;
                continue;
            }
            // System.out.println(Arrays.toString(this.intermediateFileContent.get(ind
            // - 1).toArray()));
            if (Data.statementTable
                    .get(intermediateFileContent.get(ind - 1).get(2).toLowerCase()) instanceof Directive) {
                objectCode = generateDirectiveObjectCode(intermediateFileContent.get(ind - 1));
                if (Data.statementTable.get(intermediateFileContent.get(ind - 1).get(2).toLowerCase()).getOpName()
                        .equalsIgnoreCase("END")) {
                    endOperand = Data.symbolTable.get(intermediateFileContent.get(ind - 1).get(3));
                    if (endOperand == null) {
                        endOperand = intermediateFileContent.get(0).get(3);
                    }
                    endOperand = endOperand.replaceAll("0X", "");
                    endOperand = endOperand.replaceAll("\'", "");
                }
            } else {
                objectCode = generateInstructionObjectCode(this.intermediateFileContent.get(ind - 1));
            }
            /***********************
             * Object Line Generations
             *****************************/
            if (objectCode.equals("")) {
                if (len != 0) {
                    obLines.add(new TextRecord(StartingAddress, len, content.toString()));
                    content = new StringBuilder();
                    len = 0;
                }
            } else {
                if (len == 0) {
                    StartingAddress = intermediateFileContent.get(ind - 1).get(0);
                }
                if (len + (objectCode.length() / 2) <= 30) {
                    len += (objectCode.length() / 2);
                    content.append(objectCode);
                } else {
                    obLines.add(new TextRecord(StartingAddress, len, content.toString()));
                    len = objectCode.length() / 2;
                    StartingAddress = intermediateFileContent.get(ind - 1).get(0);
                    content = new StringBuilder();
                    content.append(objectCode);

                }
            }
            /************************
             * End of Object Code Generation
             *****************************/
            this.listingFile.add(line + "    " + objectCode);
            while (objectCode.length() > 6) {
                objectCode = objectCode.substring(6);
                line = "";
                while (line.length() < 44) {
                    line += " ";
                }
                this.listingFile.add(line + "    " + objectCode);
            }

            if (intermediateFileContent.size() == ind)
                break;
        }
        if (len != 0) {
            obLines.add(new TextRecord(StartingAddress, len, content.toString()));
        }
        /***************************
         * Adding Start and End Record
         *****************************/
        int first = Integer.parseInt(intermediateFileContent.get(0).get(0), 16);
        int last = Integer.parseInt(intermediateFileContent.get(intermediateFileContent.size() - 1).get(0), 16);
        obLines.add(0, new HeaderRecord(intermediateFileContent.get(0).get(1), intermediateFileContent.get(0).get(0),
                last - first));
        obLines.add(new EndRecord(endOperand));
        /*****************************************/
    }

    private String generateInstructionObjectCode(ArrayList<String> instructionContent) {
        IStatement statement = Data.statementTable.get(instructionContent.get(2).toLowerCase());
        String opCode = Integer.toHexString(Integer.parseInt(statement.getOpCode()));
        if (opCode.length() < 2)
            opCode = "0" + opCode;
        String operandAddress;
        int indOfColon = instructionContent.get(3).indexOf(',');
        boolean indexing = true;
        if (indOfColon == -1) {
            indOfColon = instructionContent.get(3).length();
            indexing = false;
        }
        operandAddress = symbolTable.get(instructionContent.get(3).substring(0, indOfColon).trim());
        // check max length of operand
        if (Checker.convertFromHexaToDeca(operandAddress) > Math.pow(2, 15)) {
            error = true;
            this.listingFile.add("==> Exceeding max limit for operand in 15 bits");
            return new String("");
        }
        if (operandAddress == null && statement.getNumberOfOperands() != 0) {
            if (Checker.checkHexaAddress(instructionContent.get(3).trim())) {
                String address = instructionContent.get(3).trim();
                operandAddress = address.substring(3, address.length() - 1);
            } else if (Checker.checkStar(instructionContent.get(3).trim())) {
                operandAddress = instructionContent.get(0).trim();
            }else if(literalTable.get(instructionContent.get(3).substring(0, indOfColon).trim())){
            	Literal litObject=literalTable.get(instructionContent.get(3).substring(0, indOfColon).trim());
            	operandAddress=litObject.getAddress();
            } else {
                error = true;
                this.listingFile.add("==> illegal Operand not existing in symtab");
                return new String("");
            }
        }
        while (operandAddress != null && operandAddress.length() < 6)
            operandAddress = "0" + operandAddress;
        if (statement.getNumberOfOperands() == 0) {
            operandAddress = "000000";
        }
        return opCode + concatenate(indexing ? 1 : 0, operandAddress.charAt(2)) + operandAddress.substring(3);
    }

    private String generateDirectiveObjectCode(ArrayList<String> directiveContent) {
        String result = "";
        if (directiveContent.get(2).equalsIgnoreCase("BYTE")) {
            String data = directiveContent.get(3).trim();
            if (data.charAt(0) == 'X' || data.charAt(0) == 'x') {
                if (data.charAt(1) == '\'' && data.charAt(data.length() - 1) == '\'') {
                    result = data.substring(2, directiveContent.get(3).length() - 1);
                }
            } else {
                result = getHexaFromChars(data.substring(2, data.length() - 1));
            }
        } else if (directiveContent.get(2).equalsIgnoreCase("WORD")) {
            result = Checker.getHexaFromDecimal(directiveContent.get(3));
        }else if(directiveContent.get(2).equalsIgnoreCase("*")){
        	//
        	result=literalTable.get(directiveContent.get(1)).getValue();
        }
        return result;
    }

    private char concatenate(int x, char hexaDigit) {
        return (char) ((int) hexaDigit + (x * 8));
    }

    private String getHexaFromChars(String stringValue) {
        String value = "";
        for (int i = stringValue.length() - 1; i >= 0; i--) {
            value = Integer.toHexString(((int) stringValue.charAt(i))) + value;
        }
        return value;
    }

}