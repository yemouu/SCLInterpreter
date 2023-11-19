public abstract class TypedValue {
  public final SCLTypes TYPE;
  public final String VALUE;

  public TypedValue(SCLTypes type, String value) {
    this.TYPE = type;
    this.VALUE = value;
  }

  public static TypedValue toTypedValue(Token token) {
    switch (token.TYPE) {
      case LITERAL:
        return new SCLString(token.VALUE);
      case CONSTANT:
        if (SCLByte.isSCLByte(token.VALUE)) return new SCLByte(token.VALUE);
        else return new SCLUnsignedInteger(token.VALUE);
      default:
        throw new UnexpectedTokenException(
            "Expected a token of type literal or constant, got " + token);
    }
  }

  public String toString() {
    return TYPE + ":" + VALUE;
  }

  public Token toToken() {
    TokenType type;

    switch (TYPE) {
      case STRING:
        type = TokenType.LITERAL;
        break;
      default:
        type = TokenType.CONSTANT;
        break;
    }

    return new Token(type, VALUE);
  }
}
