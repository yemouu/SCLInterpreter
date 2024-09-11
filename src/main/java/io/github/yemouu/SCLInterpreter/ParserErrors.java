package io.github.yemouu.SCLInterpreter;

class UnexpectedTokenException extends RuntimeException {
  public UnexpectedTokenException(String errorMessage) {
    super(errorMessage);
  }
}

class TokenNotFoundException extends RuntimeException {}

class IdentifierAleadyDefinedException extends RuntimeException {
  public IdentifierAleadyDefinedException(String errorMessage) {
    super(errorMessage);
  }
}

class IdentifierNotDefinedException extends RuntimeException {
  public IdentifierNotDefinedException(String errorMessage) {
    super(errorMessage);
  }
}
