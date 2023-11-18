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

class UnexpectedNumberOfArguments extends RuntimeException {
  public UnexpectedNumberOfArguments(String errorMessage) {
    super(errorMessage);
  }
}

class VariableNotYetDefined extends RuntimeException {
  public VariableNotYetDefined(String errorMessage) {
    super(errorMessage);
  }
}

class UnmatchedToken extends RuntimeException {
  public UnmatchedToken(String errorMessage) {
    super(errorMessage);
  }
}
