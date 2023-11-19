public abstract class TypedNumericValue extends TypedValue {
  public TypedNumericValue(SCLTypes type, String value) {
    super(type, value);
  }

  public abstract TypedNumericValue bitwiseAnd(TypedNumericValue rightSide);

  public abstract TypedNumericValue bitwiseOr(TypedNumericValue rightSide);

  public abstract TypedNumericValue bitwiseXor(TypedNumericValue rightSide);

  public abstract TypedNumericValue leftShift(TypedNumericValue rightSide);

  public abstract TypedNumericValue negate();

  public abstract TypedNumericValue rightShift(TypedNumericValue rightSide);
}
