public class SCLUnsignedInteger extends TypedNumericValue {
  public SCLUnsignedInteger(String value) {
    super(SCLTypes.UNSIGNED_INTEGER, value);
  }

  // This operation will lose percision
  public SCLByte toSCLByte() {
    String hexStr = Integer.toHexString(Integer.parseUnsignedInt(VALUE));
    if (hexStr.length() < 2) hexStr = "0" + hexStr;

    return new SCLByte("0" + hexStr + "h");
  }

  public TypedNumericValue binaryAnd(TypedNumericValue rightHandSide) {
    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightHandSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightHandSide.VALUE.substring(1, 3), 16);
    else rhs = Integer.parseUnsignedInt(rightHandSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs & rhs));
  }

  public TypedNumericValue binaryOr(TypedNumericValue rightHandSide) {
    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightHandSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightHandSide.VALUE.substring(1, 3), 16);
    else rhs = Integer.parseUnsignedInt(rightHandSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs | rhs));
  }

  public TypedNumericValue binaryXor(TypedNumericValue rightHandSide) {
    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightHandSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightHandSide.VALUE.substring(1, 3), 16);
    else rhs = Integer.parseUnsignedInt(rightHandSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs ^ rhs));
  }

  public TypedNumericValue leftShift(TypedNumericValue rightHandSide) {
    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightHandSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightHandSide.VALUE.substring(1, 3), 16);
    else rhs = Integer.parseUnsignedInt(rightHandSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs << rhs));
  }

  public TypedNumericValue negate() {
    int rawValue = Integer.parseUnsignedInt(VALUE);
    return new SCLUnsignedInteger(Integer.toUnsignedString(~rawValue));
  }

  public TypedNumericValue rightShift(TypedNumericValue rightHandSide) {
    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightHandSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightHandSide.VALUE.substring(1, 3), 16);
    else rhs = Integer.parseUnsignedInt(rightHandSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs >>> rhs));
  }
}
