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

  // NOTE: doesn't account for subprograms
  private void replaceIdentifiers(List<Token> statement) {
    for (int i = 0; i < statement.size(); i++) {
      Token token = statement.get(i);
      if (Token.expect(TokenType.IDENTIFIER, token)) {
        Token identifier = identifiers.get(token.VALUE).toToken();
        if (identifier.VALUE == null)
          throw new TypeMismatchException("Tried to use " + token.VALUE + " before it had avalue");

        statement.set(i, identifier);
        log(statement.get(i) + " replaced with " + token);
      }
    }
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
    // If we were to actually implement this, we would need to make sure that the module is a file,
    // and that we can parse it. The module would need to be parsed, and interpreted and the results
    // of that interpretation would need to be given back to us to use while interpreting this file

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

    replaceIdentifiers(statement.subList(2, statement.size() - 1));
    evaluateExpr(statement.subList(2, statement.size() - 1));

    TypedValue value = TypedValue.toTypedValue(statement.get(2));
    identifiers.put(identifier.VALUE, value);

    log("Defining symbol " + identifier + " with value " + value);
  }

  private void evaluateExpr(List<Token> expr) {
    // Go through the entire expression and take note of the position of each opening and closing
    // parenthesis.
    List<Integer> start = new ArrayList<>();
    List<Integer> end = new ArrayList<>();
    for (int i = 0; i < expr.size(); i++) {
      Token token = expr.get(i);
      if (Token.expect(TokenType.SPECIAL_SYMBOL, "(", token)) start.add(i);
      else if (Token.expect(TokenType.SPECIAL_SYMBOL, ")", token)) end.add(i);
    }

    // Check if each parenthesis has a matching pair.
    if (start.size() != end.size())
      throw new UnmatchedTokenException("Uneven amount of opening and closing parenthesis.");

    while (start.size() != 0) {
      List<Token> subExpr = expr.subList(start.get(start.size() - 1), end.get(0) + 1);

      int shift = end.get(0) - start.get(start.size() - 1);
      for (int i = 0; i < end.size(); i++) end.set(i, end.get(i) - shift);

      evaluateGroup(subExpr);
      start.remove(start.size() - 1);
      end.remove(0);
    }

    // We should now have a flat expression that we can evaluate
    evaluate(expr);
  }

  private void evaluateGroup(List<Token> expr) {
    // Remove the ( )
    expr.remove(expr.size() - 1);
    expr.remove(0);

    evaluate(expr);
  }

  // private Token evaluate(List<Token> expression) {
  private void evaluate(List<Token> expr) {
    // Sanity check for the correct tokens within the expression
    for (int i = 0; i < expr.size(); i++) {
      Token token = expr.get(i);
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
    //       negate, lshift, and rshift. All of these operators except for negate take in 2
    //       operands. For the sake of simplicity, we expect that each expression that comes through
    //       here will only consist of 2 values and one operator. We do not support <value> band
    //       <value> band <value>. If you wanted to express this expression while using our
    //       interpreter, you would need to use ( ) to separate operations. e.g. (<value> band
    //       <value>) band <value>

    if (expr.size() == 1) return;

    TypedNumericValue result;
    Token first = expr.get(0);

    if (Token.expect(TokenType.OPERATOR, "negate", first))
      result = ((TypedNumericValue) TypedValue.toTypedValue(expr.get(1))).negate();
    else {
      TypedNumericValue lhs = (TypedNumericValue) TypedValue.toTypedValue(first);
      TypedNumericValue rhs = (TypedNumericValue) TypedValue.toTypedValue(expr.get(2));

      Token second = expr.get(1);
      Token.expectOrError(TokenType.OPERATOR, second);
      switch (second.VALUE) {
        case "band":
          result = lhs.bitwiseAnd(rhs);
          break;
        case "bor":
          result = lhs.bitwiseOr(rhs);
          break;
        case "bxor":
          result = lhs.bitwiseXor(rhs);
          break;
        case "lshift":
          result = lhs.leftShift(rhs);
          break;
        case "rshift":
          result = lhs.rightShift(rhs);
          break;
        default:
          throw new UnexpectedTokenException(
              "Expected either band, bor, bxor, lshift, or rshift, got " + second);
      }
    }

    expr.clear();
    expr.add(result.toToken());
  }

  // Global will be managing multiple statements so it will return its
  private void global(List<Token> statement) {
    log("Processing global");
    variables(getNextStatement());
  }

  private void variables(List<Token> statement) {
    log("Processing variables");

    List<Token> nextStatement = peekNextStatement();
    while (nextStatement.get(0).VALUE.equals("define")) {
      define(getNextStatement());

      nextStatement = peekNextStatement();
      if (nextStatement == null) break;
    }
  }

  private void define(List<Token> statement) {
    log("Processing define");
    Token identifier = statement.get(1);

    if (identifiers.containsKey(identifier.VALUE))
      throw new TypeMismatchException("Tried defining " + identifier.VALUE + " twice");

    // Some types are a combination of two tokens (e.g. unsigned integer)
    String type;
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
    // NOTE: Our parser supports multiple subprogram but our interpreter is only capable of running
    //       the subprogram main
    function(getNextStatement());
  }

  private void function(List<Token> statement) {
    log("Processing function");

    Token identifier = statement.get(1);
    TypedValue typedValue = new SCLSubprogram(Integer.toString(subprograms.size()));

    identifiers.put(identifier.VALUE, typedValue);
    log("Defining " + identifier + " with " + typedValue);

    variables(getNextStatement());
    begin(getNextStatement());
  }

  private void begin(List<Token> statement) {
    log("Processing begin");

    List<Token> nextStatement = peekNextStatement();
    while (Token.expect(TokenType.KEYWORD, "endfun", nextStatement.get(0))
        || Token.expect(TokenType.KEYWORD, "set", nextStatement.get(0))
        || Token.expect(TokenType.KEYWORD, "exit", nextStatement.get(0))
        || Token.expect(TokenType.KEYWORD, "display", nextStatement.get(0))) {

      nextStatement = getNextStatement();
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

      nextStatement = peekNextStatement();
      if (nextStatement == null) break;
    }
  }

  private void set(List<Token> statement) {
    log("Processing set");

    Token identifier = statement.get(1);
    TypedValue originalValue = identifiers.get(identifier.VALUE);
    if (originalValue == null)
      throw new VariableNotDefinedException(
          "Tried to assign value to " + identifier.VALUE + " but it was not defined yet.");

    replaceIdentifiers(statement.subList(3, statement.size() - 1));
    evaluateExpr(statement.subList(3, statement.size() - 1));

    TypedValue newValue = TypedValue.toTypedValue(statement.get(3));

    switch (originalValue.TYPE) {
      case STRING:
        if (newValue.TYPE != originalValue.TYPE)
          throw new TypeMismatchException("Tried assigning " + newValue + " to " + originalValue);
        originalValue = new SCLString(newValue.VALUE);
        break;
      case BYTE:
        if (newValue.TYPE != originalValue.TYPE)
          originalValue = ((SCLUnsignedInteger) newValue).toSCLByte();
        else originalValue = new SCLByte(newValue.VALUE);
        break;
      case UNSIGNED_INTEGER:
        if (newValue.TYPE != originalValue.TYPE)
          originalValue = ((SCLByte) newValue).toSCLUnsignedInteger();
        else originalValue = new SCLUnsignedInteger(newValue.VALUE);
        break;
      default:
        throw new NotImplementedException();
    }

    identifiers.replace(identifier.VALUE, originalValue);

    log("set identifier " + identifier.VALUE + " to value " + originalValue);
  }

  private void display(List<Token> statement) {
    log("Processing display");
    // Remove the end of statement token
    statement.remove(statement.size() - 1);

    // Remove the display keyword
    statement.remove(0);

    // Replace all identifiers
    replaceIdentifiers(statement);

    // This current method currently doesn't allow us to evaluate expressions before printing them.
    // The user would need to put the expression into a variable.
    for (Token token : statement) {
      switch (token.TYPE) {
        case LITERAL:
          // NOTE: Doesn't handle escape sequences. Not necessary for our test file.
          System.out.print(token.VALUE.substring(1, token.VALUE.length() - 1));
          break;
        case CONSTANT:
          System.out.print(token.VALUE);
          break;
        case SPECIAL_SYMBOL:
          Token.expectOrError(TokenType.SPECIAL_SYMBOL, ",", token);
          continue;
        default:
          throw new UnexpectedTokenException(
              "Unxpected " + token + ", expected either literal, constant, or special_symbol");
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
