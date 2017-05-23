package elements;

import data.Data;
import tools.Checker;

public class EquateHandler {

    public static boolean validateEquateOperands(String content) {
        content = content.trim();
        boolean flag = !content.trim().isEmpty();

        boolean expression = content.contains("+") || content.contains("-");
        String[] operands = getSeparatingOperands(content);

        if (expression && operands.length != 2)
            return false;
        for (String operand : operands) {
            flag = flag && checkOperand(operand);
        }
        return flag;
    }

    private static boolean checkOperand(String operandContent) {
        boolean flag = !operandContent.trim().isEmpty();
        flag = flag && (Checker.checkName(operandContent) || Checker.checkDecimalData(operandContent)
                || Checker.checkStar(operandContent) || Checker.checkLiteral(operandContent));
        if (Checker.checkName(operandContent.trim())) {
            flag = flag && Data.symbolTable.get(operandContent.trim()) != null;
        }
        if (Checker.checkLiteral(operandContent.trim())) {
            flag = flag && Data.literalTable.get(operandContent.trim()) != null
                    && Data.literalTable.get(operandContent.trim()).isAsserted();
        }
        return flag;
    }

    private static String[] getSeparatingOperands(String content) {
        String operands[];
        if (content.contains("+")) {
            operands = content.split("\\+");
        } else if (content.contains("-")) {
            operands = content.split("\\-");
        } else {
            operands = new String[1];
            operands[0] = content;
        }
        return operands;
    }

    public static String getAddressValue(String operandContent, int currentLocationCounter) {
        String[] operands = getSeparatingOperands(operandContent);
        int sign = 1;
        int finalAddress = 0;
        if (operandContent.contains("+")) {
            sign = 1;
        } else if (operandContent.contains("-")) {
            sign = -1;
        } else
            ;
        for (int i = 0; i < operands.length; i++) {
            String operand = operands[i];
            if (i > 0) {
                finalAddress += sign * getAddress(operand, currentLocationCounter);
            } else {
                finalAddress = getAddress(operand, currentLocationCounter);
            }
        }
        return Checker.getHexaFromDecimal(String.valueOf(finalAddress));
    }

    private static int getAddress(String operand, int currentLocationCounter) {
        if (Checker.checkStar(operand))
            return currentLocationCounter;
        if (Checker.checkName(operand))
            return Checker.convertFromHexaToDeca(Data.symbolTable.get(operand.trim()));
        if (Checker.checkLiteral(operand))
            return Checker.convertFromHexaToDeca(Data.literalTable.get(operand.trim()).getAddress());
        return Integer.parseInt(operand);
    }
}
