package elements;

import tools.Checker;

public class Literal implements Operand {

    // 01_ATTRIBUTES
    // **************************************
    private String name;
    private String value;
    private String address;
    private int length;
    private boolean asserted;

    // 02_CONSTRUCTOR
    // **************************************
    public Literal() {

    }

    public Literal(String name, String value, String address, int length) {
        this.name = name;
        this.value = value;
        this.address = address;
        this.length = length;
        this.asserted = false;
    }

    public Literal(String literal, String address) {
        this.address = address;
        this.name = literal;
        this.length = calculateLength(literal.substring(1).trim());
        this.value = calculateValue(literal.substring(1).trim());
        this.asserted = false;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getAddress() {
        return address;
    }

    public int getLength() {
        return length;
    }

    public String generateStatement() {
        StringBuilder statement = new StringBuilder("");
        // * =X'12'
        while (statement.length() < 8) {
            statement.append(" ");
        }
        statement.append(" *");
        while (statement.length() < 15) {
            statement.append(" ");
        }
        statement.append("  ");
        statement.append(name);
        // return
        return String.valueOf(statement);
    }

    public void setAddress(String address) {
        this.address = address;
        this.asserted = true;
    }

    public boolean isAsserted() {
        return asserted;
    }

    private int calculateLength(String literalContent) {
        if (Checker.checkStringData(literalContent)) {
            return literalContent.length() - 3;
        }
        if (Checker.checkHexaData(literalContent)) {
            return (literalContent.length() - 3) / 2;
        }
        if (Checker.checkDecimalData(literalContent)) {
            // Long number = Long.parseLong(literalContent);
            // String binaryRepresentation =
            // Long.toBinaryString(Math.abs(number));
            // int len =
            // (binaryRepresentation.length()/8)+(binaryRepresentation.length()%8==0
            // ? 1: 0);
            // if(number<0){
            // len++;
            // }
            // return len;
            return 3;
        }
        return 0;
    }

    private String calculateValue(String literalContent) {
        String value = new String();
        if (literalContent.charAt(0) == 'X' || literalContent.charAt(0) == 'x') {
            value = literalContent.substring(2, literalContent.length() - 1);
        } else if (literalContent.charAt(0) == 'C' || literalContent.charAt(0) == 'c') {
            value = Checker.getHexaFromChars(literalContent.substring(2, literalContent.length() - 1));
        } else {
            value = Checker.getHexaFromDecimal(literalContent);
        }
        return value;
    }

}
