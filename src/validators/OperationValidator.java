package validators;

import exception.StatementException;
import statement.IStatement;

public class OperationValidator implements IValidator {

	public OperationValidator() {
	}

	@Override
	public Boolean validate(String content, IStatement operation) throws StatementException {
		if (operation != null)
			return true;
		throw new StatementException("Invalid Operation");
	}
}
