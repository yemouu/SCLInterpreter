public abstract class TypedNumericValue extends TypedValue {
  public TypedNumericValue(SCLTypes type, String value) {
    super(type, value);
  }

  public abstract TypedNumericValue binaryAnd(TypedNumericValue rightHandSide);

  public abstract TypedNumericValue binaryOr(TypedNumericValue rightHandSide);

  public abstract TypedNumericValue binaryXor(TypedNumericValue rightHandSide);

  public abstract TypedNumericValue leftShift(TypedNumericValue rightHandSide);

  public abstract TypedNumericValue negate();

  public abstract TypedNumericValue rightShift(TypedNumericValue rightHandSide);
}
