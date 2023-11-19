public class SCLUnsignedInteger extends TypedNumericValue {
  public SCLUnsignedInteger(String value) {
    super(SCLTypes.UNSIGNED_INTEGER, value);
  }

  // Convert unsigned integer to byte
  // This operation will cause a loss of percision
  public SCLByte toSCLByte() {
    if (VALUE == null) throw new VariableNotDefinedException();

    String hexStr = Integer.toHexString(Integer.parseUnsignedInt(VALUE));
    if (hexStr.length() < 2) hexStr = "0" + hexStr;

    return new SCLByte("0" + hexStr + "h");
  }

  public TypedNumericValue bitwiseAnd(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      // Grab the 2 characters inside of the SCL hex value and parse it as an unsigned integer. We
      // don't hard code the end index because our SCL hex value could be either 3 or 4 characters.
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs & rhs));
  }

  public TypedNumericValue bitwiseOr(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs | rhs));
  }

  public TypedNumericValue bitwiseXor(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs ^ rhs));
  }

  public TypedNumericValue leftShift(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs << rhs));
  }

  public TypedNumericValue negate() {
    if (VALUE == null) throw new VariableNotDefinedException();

    int rawValue = Integer.parseUnsignedInt(VALUE);
    return new SCLUnsignedInteger(Integer.toUnsignedString(~rawValue));
  }

  public TypedNumericValue rightShift(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs = Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLUnsignedInteger(Integer.toUnsignedString(lhs >>> rhs));
  }
}
