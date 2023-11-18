public abstract class TypedValue {
  public final SCLTypes TYPE;
  public final String VALUE;

  public TypedValue(SCLTypes type, String value) {
    this.TYPE = type;
    this.VALUE = value;
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
