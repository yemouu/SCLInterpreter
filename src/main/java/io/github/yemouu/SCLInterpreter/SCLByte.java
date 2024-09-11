package io.github.yemouu.SCLInterpreter;

public class SCLByte extends TypedNumericValue {
  public SCLByte(String value) {
    super(SCLTypes.BYTE, value);
  }

  // Check if a string is a SCL hex.
  // SCL hexes start with 0 and end with h and can be either 3 or 4 characters long
  public static boolean isSCLByte(String str) {
    return ((str.length() == 4 || str.length() == 3)
        && str.charAt(0) == '0'
        && str.charAt(str.length() - 1) == 'h');
  }

  // Convert SCLByte to an SCLUnsignedInteger
  public SCLUnsignedInteger toSCLUnsignedInteger() {
    if (VALUE == null) throw new VariableNotDefinedException();

    return new SCLUnsignedInteger(
        Integer.toUnsignedString(
            Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16)));
  }

  public TypedNumericValue bitwiseAnd(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    // Grab the 2 characters inside of the SCL hex value and parse it as an unsigned integer. We
    // don't hard code the end index because our SCL hex value could be either 3 or 4 characters.
    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs & rhs));
  }

  public TypedNumericValue bitwiseOr(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs | rhs));
  }

  public TypedNumericValue bitwiseXor(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs ^ rhs));
  }

  public TypedNumericValue leftShift(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs << rhs));
  }

  public TypedNumericValue negate() {
    if (VALUE == null) throw new VariableNotDefinedException();

    int rawValue = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);
    return new SCLByte(intToSCLHex(~rawValue));
  }

  public TypedNumericValue rightShift(TypedNumericValue rightSide) {
    if (VALUE == null || rightSide == null) throw new VariableNotDefinedException();

    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs >>> rhs));
  }

  private String intToSCLHex(int integer) {
    String hex = Integer.toHexString(integer);
    if (hex.length() < 2) return "0" + hex + "h";
    return "0" + hex.substring(hex.length() - 2) + "h";
  }
}
