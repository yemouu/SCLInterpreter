public abstract class TypedNumericValue extends TypedValue {
  public TypedNumericValue(SCLTypes type, String value) {
    super(type, value);
  }

  public abstract TypedNumericValue binaryAnd(TypedNumericValue rightSide);

  public abstract TypedNumericValue binaryOr(TypedNumericValue rightSide);

  public abstract TypedNumericValue binaryXor(TypedNumericValue rightSide);

  public abstract TypedNumericValue leftShift(TypedNumericValue rightSide);

  public abstract TypedNumericValue negate();

  public abstract TypedNumericValue rightShift(TypedNumericValue rightSide);
}
