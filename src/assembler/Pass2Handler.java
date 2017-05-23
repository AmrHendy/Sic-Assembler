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
    private Map<String, Literal> literalTable;
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
        literalTable = new HashMap<String, Literal>();
        obLines = new ArrayList<>();
        error = false;

        // 01_load file into memory
        IntermediateFileHandler.loadFile(intermediateFileAddress, symbolTable, literalTable, intermediateFileContent,
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

            IStatement currentStatement = Data.statementTable
                    .get(intermediateFileContent.get(ind - 1).get(2).toLowerCase());
            if (currentStatement == null) {
                // new added statement in intermediate file to handle literals
                objectCode = literalTable.get(intermediateFileContent.get(ind - 1).get(3).trim()).getValue();
            } else {
                // correct statement form original src file

                if (currentStatement instanceof Directive) {
                    objectCode = generateDirectiveObjectCode(intermediateFileContent.get(ind - 1));
                    if (currentStatement.getOpName().equalsIgnoreCase("END")) {
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
            }

            /***********************
             * Object Line Generations
             *****************************/
            if (objectCode.equals("")) {
                if (notTextRecordEnder(this.intermediateFileContent.get(ind - 1).get(2))) {

                } else if (this.intermediateFileContent.get(ind - 1).get(2).equalsIgnoreCase("ORG")
                        && this.intermediateFileContent.get(ind - 1).get(0).equalsIgnoreCase(this.intermediateFileContent.get(ind)
                                .get(0))) {

                } else if (len != 0) {
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
        operandAddress = this.symbolTable.get(instructionContent.get(3).substring(0, indOfColon).trim());
        if (operandAddress == null && statement.getNumberOfOperands() != 0) {
            if (Checker.checkHexaAddress(instructionContent.get(3).trim().substring(0, indOfColon))) {
                String address = instructionContent.get(3).trim().substring(0, indOfColon);
                operandAddress = address.substring(3, address.length() - 1);
            } else if (Checker.checkStar(instructionContent.get(3).trim().substring(0, indOfColon))) {
                operandAddress = instructionContent.get(0).trim();
            } else if (Checker.checkLiteral(instructionContent.get(3).trim())) {
                operandAddress = (this.literalTable.get(instructionContent.get(3).substring(0, indOfColon).trim()))
                        .getAddress();
            } else {
                error = true;
                this.listingFile.add("==> illegal Operand not existing in symtab nor literal Table");
                return new String("");
            }
        }

        // check max length of operand
        if (Checker.convertFromHexaToDeca(operandAddress) > Math.pow(2, 15)) {
            error = true;
            this.listingFile.add("==> Exceeding max limit for operand in 15 bits");
            return new String("");
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
                result = Checker.getHexaFromChars(data.substring(2, data.length() - 1));
            }
        } else if (directiveContent.get(2).equalsIgnoreCase("WORD")) {
            result = Checker.getHexaFromDecimal(directiveContent.get(3));
        }
        return result;
    }

    private char concatenate(int x, char hexaDigit) {
        return (char) ((int) hexaDigit + (x * 8));
    }

    private boolean notTextRecordEnder(String symbol) {
        /** Remove ORG **/
        return symbol.equalsIgnoreCase("EQU") || symbol.equalsIgnoreCase("LTORG") || symbol.equalsIgnoreCase("END");
    }

}