import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {
  private final List<List<Token>> statements;
  private int index = -1;

  private Map<String, TypedValue> identifiers = new HashMap<>();

  private List<List<List<Token>>> subprograms = new ArrayList<>();
  private List<List<Token>> subprogramBuilder = new ArrayList<>();

  private boolean verbose = false;

  public Interpreter(File file) {
    Parser parser = new Parser(file);
    parser.begin();
    this.statements = parser.getStatements();
  }

  public Interpreter(File file, boolean verbose) {
    this(file);
    this.verbose = verbose;
  }

  private void log(String message) {
    if (!verbose) return;
    System.err.println(message);
  }

  private List<Token> peekNextStatement() {
    if (index + 1 >= 0 && index + 1 < statements.size()) return statements.get(index + 1);
    else return null;
  }

  private List<Token> getNextStatement() {
    if (index + 1 >= 0 && index + 1 < statements.size()) return statements.get(++index);
    else throw new NotImplementedException();
  }

  public void execute() {
    while (peekNextStatement() != null) {
      List<Token> statement = getNextStatement();
      Token startToken = statement.get(0);

      switch (startToken.VALUE) {
        case "import":
          _import(statement);
          break;
        case "symbol":
          symbol(statement);
          break;
        case "global":
          global(statement);
          break;
        case "implementations":
          implementations(statement);
          break;
        default:
          throw new UnexpectedTokenException(
              "Unexpected token "
                  + startToken
                  + ", was expecting either import, symbol, global, or implementations");
      }
    }

    // Use main as our default entry point
    if (!identifiers.containsKey("main"))
      throw new MissingMainException("Tried to execute subprogram main but it was never defined");

    TypedValue main = identifiers.get("main");

    if (main.TYPE != SCLTypes.SUBPROGRAM)
      throw new NotASubprogramException("Tried to execute main, but it was not a subprogram");

    callSubprogram(Integer.parseInt(main.VALUE));
  }

  private void _import(List<Token> statement) {
    log("Processing import");
    if (statement.size() != 3)
      throw new UnexpectedNumberOfArgumentsException(
          "Expecting 1 argument but got " + (statement.size() - 2));

    Token module = statement.get(1);

    // There isn't any actual modules for us to import
    log("importing " + module.VALUE);
  }

  private void symbol(List<Token> statement) {
    log("Processing symbol");
    if (statement.size() < 4)
      throw new UnexpectedNumberOfArgumentsException(
          "Expecting atleast 2 arguments but got " + (statement.size() - 2));

    Token identifier = statement.get(1);
    if (identifiers.containsKey(identifier.VALUE))
      throw new TypeMismatchException("Tried defining " + identifier.VALUE + " twice");

    Token value;
    if (statement.size() > 4)
      value = evaluateExpression(new ArrayList<>(statement.subList(2, statement.size() - 1)));
    else value = statement.get(2);

    // Create a TypedValue using the token's type and value information
    TypedValue typedValue;
    if (value.TYPE == TokenType.LITERAL) typedValue = new SCLString(value.VALUE);
    else if (SCLByte.isSCLByte(value.VALUE)) typedValue = new SCLByte(value.VALUE);
    // We can't differentiate between unsigned and signed ints, shorts, or longs at this stage
    else typedValue = new SCLUnsignedInteger(value.VALUE);

    // Add TypedValue to identifiers hashmap
    identifiers.put(identifier.VALUE, typedValue);

    log("Defining symbol " + identifier.VALUE + " with value " + value.VALUE);
  }

  // TODO: Investigate modifying the original list of statements rather than cloning it
  // TODO: Return a TypedNumericValue instead of a Token
  private Token evaluateExpression(List<Token> expression) {
    // Go through the entire expression and take note of the position of each opening and closing
    // parenthesis.
    List<Integer> openIndexs = new ArrayList<>();
    List<Integer> closeIndexs = new ArrayList<>();

    for (int i = 0; i < expression.size(); i++) {
      Token token = expression.get(i);
      if (Token.expect(TokenType.SPECIAL_SYMBOL, "(", token)) openIndexs.add(i);
      else if (Token.expect(TokenType.SPECIAL_SYMBOL, ")", token)) closeIndexs.add(i);
    }

    // Check if each parenthesis has a matching pair.
    if (openIndexs.size() != closeIndexs.size())
      throw new UnmatchedTokenException("Uneven amount of opening and closing parenthesis.");

    while (openIndexs.size() != 0) {
      List<Token> subExpression =
          expression.subList(openIndexs.get(openIndexs.size() - 1), closeIndexs.get(0) + 1);

      int indexShift = (closeIndexs.get(0)) - openIndexs.get(openIndexs.size() - 1);
      for (int i = 0; i < closeIndexs.size(); i++)
        closeIndexs.set(i, closeIndexs.get(i) - indexShift);

      Token parenthesisValue = evaluateParenthesis(new ArrayList<>(subExpression));
      subExpression.clear();
      subExpression.add(parenthesisValue);
      openIndexs.remove(openIndexs.size() - 1);
      closeIndexs.remove(0);
    }

    // We should now have a flat expression that we can evaluate
    return evaluate(expression);
  }

  private Token evaluate(List<Token> expression) {
    // Sanity check for the correct tokens within the expression
    for (int i = 0; i < expression.size(); i++) {
      Token token = expression.get(i);
      switch (token.TYPE) {
        case IDENTIFIER:
        case OPERATOR:
        case CONSTANT:
          continue;
        default:
          throw new UnexpectedTokenException("Unexpected token " + token);
      }
    }

    // NOTE: The only operators we accept right now are = and the bitwise operators band, bor, bxor,
    // negate, lshift, and rshift. All of these operators except for negate take in 2 operands.
    // For the sake of simplicity, we expect that each expression that comes through here will only
    // consist of 2 values and one operator. We do not support <value> band <value> band <value>. If
    // you wanted to express this expression while using our interpreter, you would need to use ( )
    // to separate operations. e.g. (<value> band <value>) band <value>

    // Our expression could have been resolved to a single token after handling parenthesis
    if (expression.size() == 1) return expression.get(0);

    TypedNumericValue returnTypedValue;
    Token firstToken = expression.get(0);

    // TODO: find a more elegant way to do this
    if (Token.expect(TokenType.OPERATOR, "negate", firstToken)) {
      Token secondToken = expression.get(1);
      if (Token.expect(TokenType.IDENTIFIER, secondToken))
        returnTypedValue = ((TypedNumericValue) identifiers.get(secondToken.VALUE)).negate();
      else if (Token.expect(TokenType.CONSTANT, secondToken))
        if (SCLByte.isSCLByte(secondToken.VALUE))
          returnTypedValue = new SCLByte(secondToken.VALUE).negate();
        else returnTypedValue = new SCLUnsignedInteger(secondToken.VALUE).negate();
      else
        throw new UnexpectedTokenException(
            "Expected a token with type constant, got " + secondToken);
    } else {
      TypedNumericValue lhs;
      TypedNumericValue rhs;

      if (Token.expect(TokenType.IDENTIFIER, firstToken))
        lhs = (TypedNumericValue) identifiers.get(firstToken.VALUE);
      else {
        if (SCLByte.isSCLByte(firstToken.VALUE)) lhs = new SCLByte(firstToken.VALUE);
        else lhs = new SCLUnsignedInteger(firstToken.VALUE);
      }

      Token thirdToken = expression.get(2);
      if (Token.expect(TokenType.IDENTIFIER, thirdToken))
        rhs = (TypedNumericValue) identifiers.get(thirdToken.VALUE);
      else {
        if (SCLByte.isSCLByte(thirdToken.VALUE)) rhs = new SCLByte(thirdToken.VALUE);
        else rhs = new SCLUnsignedInteger(thirdToken.VALUE);
      }

      Token secondToken = expression.get(1);
      switch (secondToken.VALUE) {
        case "band":
          returnTypedValue = lhs.bitwiseAnd(rhs);
          break;
        case "bor":
          returnTypedValue = lhs.bitwiseOr(rhs);
          break;
        case "bxor":
          returnTypedValue = lhs.bitwiseXor(rhs);
          break;
        case "lshift":
          returnTypedValue = lhs.leftShift(rhs);
          break;
        case "rshift":
          returnTypedValue = lhs.rightShift(rhs);
          break;
        default:
          throw new UnexpectedTokenException(
              "Expected either band, bor, bxor, lshift, or rshift, got " + secondToken);
      }
    }

    return returnTypedValue.toToken();
  }

  // TODO: This can most likely be squashed into the previous function
  private Token evaluateParenthesis(List<Token> expression) {
    // Remove the ( )
    expression.remove(expression.size() - 1);
    expression.remove(0);

    return evaluate(expression);
  }

  // Global will be managing multiple statements so it will return its
  private void global(List<Token> statement) {
    log("Processing global");
    variables(getNextStatement());
  }

  private void variables(List<Token> statement) {
    log("Processing variables");

    List<Token> nextStatement = peekNextStatement();
    while (nextStatement != null && nextStatement.get(0).VALUE.equals("define")) {
      List<Token> stmt = getNextStatement();
      define(stmt);
      nextStatement = peekNextStatement();
    }
  }

  // TODO: This could likely be cleaner
  private void define(List<Token> statement) {
    log("Processing define");
    Token identifier = statement.get(1);

    String type;
    if (identifiers.containsKey(identifier.VALUE))
      throw new TypeMismatchException("Tried defining " + identifier.VALUE + " twice");

    // Some types are a combination of two tokens (e.g. unsigned integer)
    if (statement.size() > 6) {
      type = "";
      for (int i = 4; i < statement.size() - 1; i++) {
        type += statement.get(i).VALUE + " ";
      }
      type = type.substring(0, type.length() - 1);
    } else type = statement.get(statement.size() - 2).VALUE;

    TypedValue typedValue;
    if (type.equals("string")) typedValue = new SCLString(null);
    else if (type.equals("byte")) typedValue = new SCLByte(null);
    else typedValue = new SCLUnsignedInteger(null);

    identifiers.put(identifier.VALUE, typedValue);

    log("Defining variable " + identifier.VALUE + " with type " + type);
  }

  private void implementations(List<Token> statement) {
    log("Processing implementations");
    // TODO: We currently only check for one function when there could be multiple
    function(getNextStatement());
  }

  private void function(List<Token> statement) {
    log("Processing function");

    Token identifier = statement.get(1);
    TypedValue typedValue = new SCLSubprogram(Integer.toString(subprograms.size()));

    identifiers.put(identifier.VALUE, typedValue);

    log("Defining subprogram " + identifier.VALUE + " with address " + typedValue.VALUE);

    variables(getNextStatement());
    begin(getNextStatement());
  }

  private void begin(List<Token> statement) {
    log("Processing begin");

    // TODO: peek the next statement instead of consuming immediately
    List<Token> nextStatement = getNextStatement();
    while (nextStatement.get(0).VALUE.equals("endfun")
        || nextStatement.get(0).VALUE.equals("set")
        || nextStatement.get(0).VALUE.equals("exit")
        || nextStatement.get(0).VALUE.equals("display")) {

      switch (nextStatement.get(0).VALUE) {
        case "display":
        case "exit":
        case "set":
          subprogramBuilder.add(nextStatement);
          break;
        case "endfun":
          // If we want to allow nested subprograms, we need check the identifier that comes after
          // endfun. Our parser currently doesn't accept function declarations within begin
          // statements so this isn't a case we should run into. The first endfun we see should end
          // the function we are in.
          subprograms.add(subprogramBuilder);
          subprogramBuilder = new ArrayList<>();
          break;
      }

      if (peekNextStatement() == null) break;
      nextStatement = getNextStatement();
    }
  }

  private void set(List<Token> statement) {
    log("Processing set");

    Token identifier = statement.get(1);
    TypedValue typedValue = identifiers.get(identifier.VALUE);
    if (typedValue == null)
      throw new VariableNotYetDefined(
          "Tried to assign value to " + identifier.VALUE + " but it was not defined yet.");

    // TODO: throw an error if there is a type mismatch between strings and other types
    // TODO: this is ugly consider having a Token to TypedValue conversion function
    if (statement.size() == 5) {
      Token value = statement.get(3);
      if (value.TYPE == TokenType.IDENTIFIER) {
        TypedValue newValue = identifiers.get(value.VALUE);
        if (typedValue.TYPE == SCLTypes.STRING) typedValue = new SCLString(newValue.VALUE);
        else if (typedValue.TYPE == SCLTypes.BYTE)
          if (newValue.TYPE == SCLTypes.UNSIGNED_INTEGER)
            typedValue = ((SCLUnsignedInteger) newValue).toSCLByte();
          else typedValue = new SCLByte(newValue.VALUE);
        else if (newValue.TYPE == SCLTypes.BYTE)
          typedValue = ((SCLByte) newValue).toSCLUnsignedInteger();
        else typedValue = new SCLUnsignedInteger(newValue.VALUE);
        // Need to check the type of the raw value itself as well.
      } else if (typedValue.TYPE == SCLTypes.STRING) typedValue = new SCLString(value.VALUE);
      else if (typedValue.TYPE == SCLTypes.BYTE) typedValue = new SCLByte(value.VALUE);
      else typedValue = new SCLUnsignedInteger(value.VALUE);
    } else {
      Token value = evaluateExpression(new ArrayList<>(statement.subList(3, statement.size() - 1)));

      if (typedValue.TYPE == SCLTypes.STRING) typedValue = new SCLString(value.VALUE);
      else if (typedValue.TYPE == SCLTypes.BYTE) typedValue = new SCLByte(value.VALUE);
      else typedValue = new SCLUnsignedInteger(value.VALUE);
    }

    identifiers.replace(identifier.VALUE, typedValue);

    log("set identifier " + identifier.VALUE + " to value " + typedValue);
  }

  // TODO: Consider just modifying the statement list instead of copying
  private void display(List<Token> statement) {
    log("Processing display");
    // Clone the statements so we can modify it to make displaying its contents easier.
    List<Token> statementCopy = new ArrayList<>(statement);

    // Remove the end of statement token
    statementCopy.remove(statementCopy.size() - 1);

    // Remove the display keyword
    statementCopy.remove(0);

    // This current method currently doesn't allow us to evaluate expressions before printing them.
    // The user would need to put the expression into a variable.
    for (Token token : statementCopy) {
      switch (token.TYPE) {
        case IDENTIFIER:
          System.out.print(identifiers.get(token.VALUE).VALUE);
          break;
        case LITERAL:
          // We need to remove the extra quotes
          // We may also need to correctly display escaped characters
          System.out.print(token.VALUE.substring(1, token.VALUE.length() - 1));
          // System.out.print(token.VALUE);
          break;
        case CONSTANT:
          System.out.print(token.VALUE);
          break;
        case SPECIAL_SYMBOL:
          continue;
        default:
          throw new UnexpectedTokenException(
              "Unxpected token "
                  + token.TYPE
                  + ", expected either identifier, literal, constant, or special_symbol");
      }
    }

    System.out.println();
  }

  private void callSubprogram(int subprogram) {
    log("Processing subprogram call");
    for (List<Token> statement : subprograms.get(subprogram)) {
      Token firstToken = statement.get(0);
      switch (firstToken.VALUE) {
        case "set":
          set(statement);
          break;
        case "display":
          display(statement);
          break;
        case "exit":
          return;
        default:
          throw new UnexpectedTokenException(
              "Unxpected " + firstToken + ", expected either set, display, or exit");
      }
    }
  }

  public static void main(String[] args) throws FileNotFoundException {
    if (args.length != 1) {
      System.err.println("Usage: java SCLScanner <filename>");
      return;
    }

    String filename = args[0];
    File file = new File(filename);

    // Interpreter interpreter = new Interpreter(file, true);
    Interpreter interpreter = new Interpreter(file);
    interpreter.execute();
  }
}
