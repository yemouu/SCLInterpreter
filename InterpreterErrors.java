class MissingMainException extends RuntimeException {
  public MissingMainException(String errorMessage) {
    super(errorMessage);
  }
}

class NotASubprogramException extends RuntimeException {
  public NotASubprogramException(String errorMessage) {
    super(errorMessage);
  }
}

class UnexpectedNumberOfArgumentsException extends RuntimeException {
  public UnexpectedNumberOfArgumentsException(String errorMessage) {
    super(errorMessage);
  }
}

class VariableNotDefinedException extends RuntimeException {
  public VariableNotDefinedException() {
    super();
  }

  public VariableNotDefinedException(String errorMessage) {
    super(errorMessage);
  }
}

class VariableIsNullException extends RuntimeException {
  public VariableIsNullException() {
    super();
  }

  public VariableIsNullException(String errorMessage) {
    super(errorMessage);
  }
}

class VariableAlreadyDefinedException extends RuntimeException {
  public VariableAlreadyDefinedException() {
    super();
  }

  public VariableAlreadyDefinedException(String errorMessage) {
    super(errorMessage);
  }
}

class TypeMismatchException extends RuntimeException {
  public TypeMismatchException() {
    super();
  }

  public TypeMismatchException(String errorMessage) {
    super(errorMessage);
  }
}

class UnmatchedTokenException extends RuntimeException {
  public UnmatchedTokenException(String errorMessage) {
    super(errorMessage);
  }
}

class EndOfStatementsException extends RuntimeException {
  public EndOfStatementsException() {}
}

class NotImplementedException extends RuntimeException {
  public NotImplementedException() {}
}
