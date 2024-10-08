package io.github.yemouu.SCLInterpreter;

public class Token {
  public final TokenType TYPE;
  public final String VALUE;

  public Token(TokenType TYPE, String VALUE) {
    this.TYPE = TYPE;
    this.VALUE = VALUE;
  }

  public static boolean expect(TokenType expectedType, Token token) {
    if (token == null) return false;
    return expectedType == token.TYPE;
  }

  public static boolean expect(TokenType expectedType, String expectedValue, Token token) {
    if (token == null) return false;
    return (expectedType == token.TYPE) && expectedValue.equals(token.VALUE);
  }

  public static void expectOrError(TokenType expectedType, Token token)
      throws UnexpectedTokenException {
    if (!expect(expectedType, token))
      throw new UnexpectedTokenException(
          String.format("Expected token with type %s, got %s", expectedType, token));
  }

  public static void expectOrError(TokenType expectedType, String expectedValue, Token token)
      throws UnexpectedTokenException {
    if (!expect(expectedType, expectedValue, token))
      throw new UnexpectedTokenException(
          String.format(
              "Expected token with type %s and value %s, got token %s",
              expectedType, expectedValue, token));
  }

  public String toString() {
    return TYPE + ":" + VALUE;
  }
}
