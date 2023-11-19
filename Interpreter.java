import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {
  // This is the list of statements from our parser. We use the index combined with
  // getNextStatement() and peekNextStatement() to traverse this list and interpret the file
  private final List<List<Token>> statements;
  private int index = -1;

  // HashMap storing all of our identifiers and their values. Our custom TypedValue class holds the
  // type information and value information allowing us to be type aware while handling operation.
  private Map<String, TypedValue> identifiers = new HashMap<>();

  // Stores a subprogram which is a list of statements. Although our current implementation only
  // supports executing main, if we were to enable the interpreter to execute other subprograms,
  // they would be stored in this data structure alongside main.
  private List<List<List<Token>>> subprograms = new ArrayList<>();
  private List<List<Token>> subprogramBuilder = new ArrayList<>();

  // Boolean flag to control if log messages should be printed to stderr.
  private boolean verbose = false;

  // Constructor. Creates a parser object that will parse our file and return to us the statements
  // for us to interpret.
  public Interpreter(File file) {
    Parser parser = new Parser(file);
    parser.begin();
    this.statements = parser.getStatements();
  }

  // Same as above but gives control over the verbose flag.
  public Interpreter(File file, boolean verbose) {
    this(file);
    this.verbose = verbose;
  }

  // Print messages to stderr based on the verbose flag.
  private void log(String message) {
    if (!verbose) return;
    System.err.println(message);
  }

  // Check for and return the value of the next statement. This doesn't advance the index.
  private List<Token> peekNextStatement() {
    if (index + 1 >= 0 && index + 1 < statements.size()) return statements.get(index + 1);
    else return null;
  }

  // Similar to the above function but advances the index. If there isn't a next statement, throws
  // an exception instead.
  private List<Token> getNextStatement() {
    if (index + 1 >= 0 && index + 1 < statements.size()) return statements.get(++index);
    else throw new StatementNotFoundException();
  }

  // Does an inplace mutation of the statement provided. Each identifier token is replaced with its
  // real value. This doesn't work for identifiers that are subprograms.
  private void replaceIdentifiers(List<Token> statement) {
    for (int i = 0; i < statement.size(); i++) {
      Token token = statement.get(i);
      if (Token.expect(TokenType.IDENTIFIER, token)) {
        Token identifier = identifiers.get(token.VALUE).toToken();
        if (identifier.VALUE == null)
          throw new VariableIsNullException(
              "Tried to use " + token.VALUE + " before it had a value");

        statement.set(i, identifier);
        log(statement.get(i) + " replaced with " + token);
      }
    }
  }

  // Start the interpretation process. We loop over all of the statements and interpret each line by
  // line. In this specific method, we look for top level statements (import, symbol, global, and
  // implementations) and call the corresponding methods to interpret each statement. The methods we
  // call here will call other methods as needed to interpret everything
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
          throw new UnexpectedTokenException("Unexpected token " + startToken);
      }
    }

    log("Done interpreting");

    log("Calling main");
    // After we interpret the file, we look for the main subprogram which should've been defined in
    // the file and call it.
    if (!identifiers.containsKey("main"))
      throw new MissingMainException("Tried to execute subprogram main but it was never defined");

    TypedValue main = identifiers.get("main");

    if (main.TYPE != SCLTypes.SUBPROGRAM)
      throw new NotASubprogramException("Tried to execute main, but it was not a subprogram");

    callSubprogram(Integer.parseInt(main.VALUE));
  }

  // Interpret import statements
  private void _import(List<Token> statement) {
    log("Processing import");
    if (statement.size() != 3)
      throw new UnexpectedNumberOfArgumentsException(
          "Expecting 1 argument but got " + (statement.size() - 2));

    Token module = statement.get(1);

    // There isn't any actual modules for us to import If we were to actually implement this, we
    // would need to make sure that the module is a file, and that we can parse it. The module would
    // need to be parsed, and interpreted and the results of that interpretation would need to be
    // given back to us to use while interpreting this file

    log("Importing " + module.VALUE);
  }

  // Interpret symbol statements
  private void symbol(List<Token> statement) {
    log("Processing symbol");
    if (statement.size() < 4)
      throw new UnexpectedNumberOfArgumentsException(
          "Expecting atleast 2 arguments but got " + (statement.size() - 2));

    // Check if the identifier was already defined
    Token identifier = statement.get(1);
    if (identifiers.containsKey(identifier.VALUE))
      throw new VariableAlreadyDefinedException("Tried defining " + identifier.VALUE + " twice");

    // Resolve the remaining portion of the statement to a single token
    replaceIdentifiers(statement.subList(2, statement.size() - 1));
    evaluateExpr(statement.subList(2, statement.size() - 1));

    // Assign the value to the identifier inside of the identifiers HashMap
    TypedValue value = TypedValue.toTypedValue(statement.get(2));
    identifiers.put(identifier.VALUE, value);

    log("Defining symbol " + identifier + " with value " + value);
  }

  // Evaluate arithmetic expressions
  private void evaluateExpr(List<Token> expr) {
    // Find the indexes of each opening and closing parenthesis and add them to an array
    List<Integer> start = new ArrayList<>();
    List<Integer> end = new ArrayList<>();
    for (int i = 0; i < expr.size(); i++) {
      Token token = expr.get(i);
      if (Token.expect(TokenType.SPECIAL_SYMBOL, "(", token)) start.add(i);
      else if (Token.expect(TokenType.SPECIAL_SYMBOL, ")", token)) end.add(i);
    }

    // Ensure that each each parenthesis has a matching pair.
    if (start.size() != end.size())
      throw new UnmatchedTokenException("Uneven amount of opening and closing parenthesis.");

    // If we have indexes in our arrays, evaluate the expressions between the parenthesis.
    while (start.size() != 0) {
      List<Token> subExpr = expr.subList(start.get(start.size() - 1), end.get(0) + 1);

      // After evaluating the expression, we need to shift the indexes of the closing parenthesis by
      // the number of elements that were originally inside of expression before evaluation.
      int shift = end.get(0) - start.get(start.size() - 1);
      for (int i = 0; i < end.size(); i++) end.set(i, end.get(i) - shift);

      // Evaluate the inner expression
      evaluateGroup(subExpr);

      // Remove the used indexes
      start.remove(start.size() - 1);
      end.remove(0);
    }

    // We have a flat expression now (no parenthesis) and we can evaluate the final expression
    evaluate(expr);
  }

  // Helper method for evaluating expressions inside of parenthesis
  private void evaluateGroup(List<Token> expr) {
    // Remove the parenthesis
    expr.remove(expr.size() - 1);
    expr.remove(0);

    // Evaluate the expression
    evaluate(expr);
  }

  // Helper method for evaluating expressions.  The only operators we support right now are = and
  // the bitwise operators (band, bor, bxor, negate, lshift, and rshift). We only use the equals
  // operator in assigning values to  identifiers so we should never see it inside of an expression.
  private void evaluate(List<Token> expr) {
    // Quick check to make sure that we have the correct types of tokens
    for (Token token : expr) {
      switch (token.TYPE) {
        case IDENTIFIER:
        case OPERATOR:
        case CONSTANT:
          continue;
        default:
          throw new UnexpectedTokenException("Unexpected token " + token);
      }
    }

    // We define a custom TypedNumericValue class here. This class has type information for
    // constants as well as provides the operations that we need while additionally doing proper
    // type casting from byte to unsigned integer and unsigned integer to byte.
    TypedNumericValue result;

    // If the first token is negate, run the second token's negate method to get the result.
    // If the first token was anything else, we have a different bitwise operator.
    Token first = expr.get(0);
    if (Token.expect(TokenType.OPERATOR, "negate", first))
      result = ((TypedNumericValue) TypedValue.toTypedValue(expr.get(1))).negate();
    else {
      // Get the lhs and rhs as TypedNumericValues
      TypedNumericValue lhs = (TypedNumericValue) TypedValue.toTypedValue(first);
      TypedNumericValue rhs = (TypedNumericValue) TypedValue.toTypedValue(expr.get(2));

      // Figure out what operation we need to do
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
          throw new UnexpectedTokenException("Unexpected token, " + second);
      }
    }

    // We have our result, so we can replace the expression with it. (In place mutation)
    expr.clear();
    expr.add(result.toToken());
  }

  // Interpret global statements
  private void global(List<Token> statement) {
    log("Processing global");
    variables(getNextStatement());
  }

  // Interpret variables statements
  private void variables(List<Token> statement) {
    log("Processing variables");

    // Continuously grab the next token if its the define keyword
    List<Token> nextStatement = peekNextStatement();
    while (nextStatement.get(0).VALUE.equals("define")) {
      define(getNextStatement());

      nextStatement = peekNextStatement();
      if (nextStatement == null) break;
    }
  }

  // Interpret define statements
  private void define(List<Token> statement) {
    log("Processing define");
    Token identifier = statement.get(1);

    if (identifiers.containsKey(identifier.VALUE))
      throw new VariableAlreadyDefinedException("Tried defining " + identifier.VALUE + " twice");

    // Some types are a combination of two tokens (e.g. unsigned integer) so we use this for loop to
    // make sure that we get the entire type. We also need to make sure we get rid of the trialing
    // space at the end of the type string we created.
    String type = "";
    for (int i = 4; i < statement.size() - 1; i++) type += statement.get(i).VALUE + " ";
    type = type.substring(0, type.length() - 1);

    // Based on the type we built above, make a TypedValue object. We currently only have three
    // implemented, so if the type isnt a string or byte, we treat it as an unsigned integer.
    TypedValue typedValue;
    if (type.equals("string")) typedValue = new SCLString(null);
    else if (type.equals("byte")) typedValue = new SCLByte(null);
    else typedValue = new SCLUnsignedInteger(null);

    // Assign the identifier with its type information
    identifiers.put(identifier.VALUE, typedValue);
    log("Defining variable " + identifier.VALUE + " with type " + type);
  }

  // Interpret implementations statements Our parser supports multiple subprogram but our
  // interpreter is only capable of running the subprogram main
  private void implementations(List<Token> statement) {
    log("Processing implementations");
    function(getNextStatement());
  }

  // Interpret function statements
  private void function(List<Token> statement) {
    log("Processing function");

    Token identifier = statement.get(1);
    TypedValue typedValue = new SCLSubprogram(Integer.toString(subprograms.size()));

    identifiers.put(identifier.VALUE, typedValue);
    log("Defining " + identifier + " with " + typedValue);

    variables(getNextStatement());
    begin(getNextStatement());
  }

  // Interpret begin statements
  private void begin(List<Token> statement) {
    log("Processing begin");

    // Continuously get the next statement if its either of endfun, set, exit, or display
    List<Token> nextStatement = peekNextStatement();
    while (Token.expect(TokenType.KEYWORD, "endfun", nextStatement.get(0))
        || Token.expect(TokenType.KEYWORD, "set", nextStatement.get(0))
        || Token.expect(TokenType.KEYWORD, "exit", nextStatement.get(0))
        || Token.expect(TokenType.KEYWORD, "display", nextStatement.get(0))) {

      // If the next statement starts with display, exit, or set, we add it to the subprogramBuilder
      // array. When we reach the endfun keyword, we add the subprogramBuilder array to the
      // subprograms array and create a new subprogramBuilder array. We also exit the while loop.
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

  // Interpret set statements
  private void set(List<Token> statement) {
    log("Processing set");

    Token identifier = statement.get(1);
    TypedValue originalValue = identifiers.get(identifier.VALUE);
    if (originalValue == null)
      throw new VariableNotDefinedException(
          "Tried to assign value to " + identifier.VALUE + " but it was not defined yet.");

    replaceIdentifiers(statement.subList(3, statement.size() - 1));
    evaluateExpr(statement.subList(3, statement.size() - 1));

    // This is the value we are setting the identifier to. Based on the type of the identifier and
    // the value, we may need to emit an error or perform a type conversion.
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

    // Update the identifier's information
    identifiers.replace(identifier.VALUE, originalValue);

    log("Set identifier " + identifier.VALUE + " to value " + originalValue);
  }

  // Interpret display statements
  private void display(List<Token> statement) {
    log("Processing display");
    // Remove the end of statement token
    statement.remove(statement.size() - 1);

    // Remove the display keyword
    statement.remove(0);

    // Replace all identifiers
    replaceIdentifiers(statement);

    // This current method currently doesn't allow us to evaluate expressions before printing them
    // if the expression is within the display statement. To do this, we would need to isolate each
    // expression and send it to evaluateExpr(). The user would need to put the expression into a
    // variable if they wish to display the expression.
    for (Token token : statement) {
      switch (token.TYPE) {
        case LITERAL:
          // We doesn't currently handle escape sequences. If we were going to, we would need to
          // search for the next backslash in the string and replace it with the character it is
          // suppose to represent.
          System.out.print(token.VALUE.substring(1, token.VALUE.length() - 1));
          break;
        case CONSTANT:
          System.out.print(token.VALUE);
          break;
        case SPECIAL_SYMBOL:
          Token.expectOrError(TokenType.SPECIAL_SYMBOL, ",", token);
          continue;
        default:
          throw new UnexpectedTokenException("Unxpected token, " + token);
      }
    }

    System.out.println();
  }

  // Call subprograms. The index we take in is the subprogram's position in the subprograms array.
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
          throw new UnexpectedTokenException("Unxpected token, " + firstToken);
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
