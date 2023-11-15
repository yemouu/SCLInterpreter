class UnexpectedTokenException extends RuntimeException {
  public UnexpectedTokenException() {}

  public UnexpectedTokenException(String errorMessage) {
    super(errorMessage);
  }
}

class TokenNotFoundException extends RuntimeException {}
