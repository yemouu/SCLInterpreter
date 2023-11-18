// TODO: the VALUE can be empty, so check before doing any operations and throw errors if the VALUE
//       is null
public class SCLByte extends TypedNumericValue {
  public SCLByte(String value) {
    super(SCLTypes.BYTE, value);
  }

  public static boolean isSCLByte(String str) {
    return ((str.length() == 4 || str.length() == 3)
        && str.charAt(0) == '0'
        && str.charAt(str.length() - 1) == 'h');
  }

  public SCLUnsignedInteger toSCLUnsignedInteger() {
    return new SCLUnsignedInteger(
        Integer.toUnsignedString(
            Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16)));
  }

  public TypedNumericValue binaryAnd(TypedNumericValue rightSide) {
    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs & rhs));
  }

  public TypedNumericValue binaryOr(TypedNumericValue rightSide) {
    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs | rhs));
  }

  public TypedNumericValue binaryXor(TypedNumericValue rightSide) {
    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs ^ rhs));
  }

  public TypedNumericValue leftShift(TypedNumericValue rightSide) {
    int lhs = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);

    int rhs;
    if (rightSide.TYPE == SCLTypes.BYTE)
      rhs =
          Integer.parseUnsignedInt(rightSide.VALUE.substring(1, rightSide.VALUE.length() - 1), 16);
    else rhs = Integer.parseUnsignedInt(rightSide.VALUE);

    return new SCLByte(intToSCLHex(lhs << rhs));
  }

  public TypedNumericValue negate() {
    int rawValue = Integer.parseUnsignedInt(VALUE.substring(1, VALUE.length() - 1), 16);
    return new SCLByte(intToSCLHex(~rawValue));
  }

  public TypedNumericValue rightShift(TypedNumericValue rightSide) {
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
