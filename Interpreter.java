import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: refactor
public class Interpreter {
  private final List<List<Token>> statements;
  private List<List<List<Token>>> subprograms = new ArrayList<>();
  private List<List<Token>> subprogramBuilder = new ArrayList<>();
  private Map<String, TypedValue> identifiers = new HashMap<>();

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

  public void execute() {
    for (int i = 0; i < statements.size(); i++) {
      List<Token> statement = statements.get(i);
      Token startToken = statement.get(0);

      switch (startToken.VALUE) {
        case "import":
          _import(statement);
          break;
        case "symbol":
          symbol(statement);
          break;
        case "global":
          i += global(statement, i);
          break;
        case "implementations":
          i += implementations(statement, i);
          break;
        default:
          throw new UnexpectedTokenException(
              "Unexpected token with type "
                  + startToken.TYPE
                  + " and value "
                  + startToken.VALUE
                  + ". Was expecting either import, symbol, global, or implementations");
      }
    }

    // Use main as our default entry point
    if (!identifiers.containsKey("main"))
      throw new MissingMainException("Tried to execute subprogram main but it was never defined");

    TypedValue main = identifiers.get("main");

    if (!main.TYPE.equals("subprogram"))
      throw new NotASubprogramException("Tried to execute main, but it was not a subprogram");

    callSubprogram(Integer.parseInt(main.VALUE));
  }

  private void _import(List<Token> statement) {
    log("Processing import");
    if (statement.size() != 3)
      throw new UnexpectedNumberOfArguments(
          "Expecting 1 argument but got " + (statement.size() - 2));

    Token module = statement.get(1);

    // There isn't any actual modules for us to import
    System.out.println("importing " + module.VALUE);
  }

  private void symbol(List<Token> statement) {
    log("Processing symbol");
    if (statement.size() < 4)
      throw new UnexpectedNumberOfArguments(
          "Expecting atleast 2 arguments but got " + (statement.size() - 2));

    Token identifier = statement.get(1);

    // TODO: implement expression evaluation
    // We have an expression we nee to evaluate, we can do this yet.
    // We need to get the rest of the statement and evaluate it and put the result of that
    // evaluation into the identifier
    Token value;
    if (statement.size() > 4) {
      value = evaluateExpression(new ArrayList<>(statement.subList(2, statement.size() - 1)));
    } else value = statement.get(2);

    // 1. Get token's type
    //   tokens of type literals are strings
    //   constants can be either a number or byte
    //     bytes are represented as hex values in this language and they end with h
    String type;
    if (value.TYPE == TokenType.LITERAL) type = "string";
    else if (value.VALUE.charAt(0) == '0' && value.VALUE.charAt(value.VALUE.length() - 1) == 'h')
      type = "byte";
    else if (value.VALUE.contains(".")) type = "float";
    else type = "integer";

    // 2. Create TypedValue object
    TypedValue typedValue = new TypedValue(type, value.VALUE);

    // 3. Add TypedValue to identifiers hashmap
    identifiers.put(identifier.VALUE, typedValue);

    log("Defining symbol " + identifier.VALUE + " with value " + value.VALUE);
  }

  private Token evaluateExpression(List<Token> expression) {
    Token result = null;

    // We need to implement PEMDAS
    // Parenthesis
    // Exponents
    // Multiplication / Dision
    // Addition / Subtraction
    //
    // Lucky for us, our test file only uses bitwise operators with parenthesis being used for
    // grouping
    // So we really need to implement P - Parenthesis

    // 1. Check for parenthesis
    // 2. Do operations inside of parenthesis
    // 3. Combine everything at the end

    // On our first pass, we will check for parenthesis.
    // If we find any, and we find that they are matching, we turn that into a sub-expression and
    // call back into this function with the smaller expression.
    // When we get our token back, we will replace the expression we just evaluated with the token
    // and continue looking for more parenthesis until there are none left.
    List<Integer> openIndexs = new ArrayList<>();
    List<Integer> closeIndexs = new ArrayList<>();

    for (int i = 0; i < expression.size(); i++) {
      Token token = expression.get(i);
      if (token.TYPE == TokenType.SPECIAL_SYMBOL && token.VALUE.equals("(")) openIndexs.add(i);
      else if (token.TYPE == TokenType.SPECIAL_SYMBOL && token.VALUE.equals(")"))
        closeIndexs.add(i);
    }

    if (openIndexs.size() != closeIndexs.size())
      throw new UnmatchedToken("Found a '(' without a matching ')'");

    // This code actually breaks with nesting because the indexes of our open and close Parenthesis
    // change. After each iteration, we need to shift the close indexes by the number of elements
    // inside of
    // the close parenthesis
    while (openIndexs.size() != 0) {
      // log("closeIndexs.get(0): " + closeIndexs.get(0));
      // log("expression.size(): " + expression.size());
      // log(expression.get(0).VALUE);
      // log(expression.get(expression.size() - 1).VALUE);
      List<Token> subExpression =
          expression.subList(openIndexs.get(openIndexs.size() - 1), closeIndexs.get(0) + 1);

      Token parenthesisValue = evaluate(new ArrayList<>(subExpression));
      subExpression.clear();
      subExpression.add(parenthesisValue);
      openIndexs.remove(openIndexs.size() - 1);
      closeIndexs.remove(0);
    }

    // We should now have a flat expression that we can evaluate
    // NOTE: This is duplicated code, refactor this later

    // Sanity check for the correct tokens within the expression
    for (int i = 0; i < expression.size(); i++) {
      Token token = expression.get(i);
      switch (token.TYPE) {
        case IDENTIFIER:
        case OPERATOR:
        case CONSTANT:
          continue;
        default:
          throw new UnexpectedTokenException(
              "Unexpected token of type " + token.TYPE + " and value " + token.VALUE);
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

    Token firstToken = expression.get(0);
    Token secondToken = expression.get(1);

    if (firstToken.TYPE == TokenType.OPERATOR && firstToken.VALUE.equals("negate")) {
      int value;

      if (secondToken.TYPE == TokenType.IDENTIFIER) {
        TypedValue typedValue = identifiers.get(secondToken.VALUE);
        value =
            Integer.parseUnsignedInt(
                typedValue.VALUE.substring(1, typedValue.VALUE.length() - 1), 16);

      } else {
        value =
            Integer.parseUnsignedInt(
                firstToken.VALUE.substring(1, firstToken.VALUE.length() - 1), 16);
      }

      value = ~value;
      String valueStr = Integer.toHexString(value);
      valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

      result = new Token(TokenType.CONSTANT, valueStr);

    } else {
      // The third token may not exist
      Token thirdToken = expression.get(2);

      int valueFirst;
      int valueSecond;
      int valueReturn;

      if (firstToken.TYPE == TokenType.IDENTIFIER) {
        TypedValue typedValue = identifiers.get(firstToken.VALUE);
        valueFirst =
            Integer.parseUnsignedInt(
                typedValue.VALUE.substring(1, typedValue.VALUE.length() - 1), 16);
      } else {
        valueFirst =
            Integer.parseUnsignedInt(
                firstToken.VALUE.substring(1, firstToken.VALUE.length() - 1), 16);
      }

      // In the case of lshift and rshift, the third token is a normal integer
      if (thirdToken.TYPE == TokenType.IDENTIFIER) {
        TypedValue typedValue = identifiers.get(thirdToken.VALUE);
        if (!typedValue.TYPE.equals("byte"))
          valueSecond = Integer.parseUnsignedInt(typedValue.VALUE);
        else
          valueSecond =
              Integer.parseUnsignedInt(
                  typedValue.VALUE.substring(1, typedValue.VALUE.length() - 1), 16);
      } else {
        // The third token is a normal constant
        if (thirdToken.VALUE.charAt(0) != '0'
            && thirdToken.VALUE.charAt(thirdToken.VALUE.length() - 1) != 'h')
          valueSecond = Integer.parseUnsignedInt(thirdToken.VALUE);
        else
          valueSecond =
              Integer.parseUnsignedInt(
                  thirdToken.VALUE.substring(1, thirdToken.VALUE.length() - 1), 16);
      }

      String valueStr;
      switch (secondToken.VALUE) {
        case "band":
          valueReturn = valueFirst & valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
        case "bor":
          valueReturn = valueFirst | valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
        case "bxor":
          valueReturn = valueFirst ^ valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
        case "lshift":
          valueReturn = valueFirst << valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
        case "rshift":
          valueReturn = valueFirst >>> valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
      }
    }

    return result;
  }

  private Token evaluate(List<Token> expression) {
    Token result = null;
    // Remove the ( )
    expression.remove(expression.size() - 1);
    expression.remove(0);

    // Sanity check for the correct tokens within the expression
    for (int i = 0; i < expression.size(); i++) {
      Token token = expression.get(i);
      switch (token.TYPE) {
        case IDENTIFIER:
        case OPERATOR:
        case CONSTANT:
          continue;
        default:
          throw new UnexpectedTokenException(
              "Unexpected token of type " + token.TYPE + " and value " + token.VALUE);
      }
    }

    // NOTE: The only operators we accept right now are = and the bitwise operators band, bor, bxor,
    // negate, lshift, and rshift. All of these operators except for negate take in 2 operands.
    // For the sake of simplicity, we expect that each expression that comes through here will only
    // consist of 2 values and one operator. We do not support <value> band <value> band <value>. If
    // you wanted to express this expression while using our interpreter, you would need to use ( )
    // to separate operations. e.g. (<value> band <value>) band <value>

    Token firstToken = expression.get(0);
    Token secondToken = expression.get(1);

    if (firstToken.TYPE == TokenType.OPERATOR && firstToken.VALUE.equals("negate")) {
      int value;

      if (secondToken.TYPE == TokenType.IDENTIFIER) {
        TypedValue typedValue = identifiers.get(secondToken.VALUE);
        value =
            Integer.parseUnsignedInt(
                typedValue.VALUE.substring(1, typedValue.VALUE.length() - 1), 16);

      } else {
        value =
            Integer.parseUnsignedInt(
                firstToken.VALUE.substring(1, firstToken.VALUE.length() - 1), 16);
      }

      value = ~value;
      String valueStr = Integer.toHexString(value);
      valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

      result = new Token(TokenType.CONSTANT, valueStr);

    } else {
      // The third token may not exist if we were to go into negate
      Token thirdToken = expression.get(2);
      int valueFirst;
      int valueSecond;
      int valueReturn;

      if (firstToken.TYPE == TokenType.IDENTIFIER) {
        TypedValue typedValue = identifiers.get(firstToken.VALUE);
        valueFirst =
            Integer.parseUnsignedInt(
                typedValue.VALUE.substring(1, typedValue.VALUE.length() - 1), 16);
      } else {
        valueFirst =
            Integer.parseUnsignedInt(
                firstToken.VALUE.substring(1, firstToken.VALUE.length() - 1), 16);
      }

      // In the case of lshift and rshift, the third token is a normal integer
      if (thirdToken.TYPE == TokenType.IDENTIFIER) {
        TypedValue typedValue = identifiers.get(thirdToken.VALUE);
        if (!typedValue.TYPE.equals("byte"))
          valueSecond = Integer.parseUnsignedInt(typedValue.VALUE);
        else
          valueSecond =
              Integer.parseUnsignedInt(
                  typedValue.VALUE.substring(1, typedValue.VALUE.length() - 1), 16);
      } else {
        // The third token is a normal constant
        if (thirdToken.VALUE.charAt(0) != '0'
            && thirdToken.VALUE.charAt(thirdToken.VALUE.length() - 1) != 'h')
          valueSecond = Integer.parseUnsignedInt(thirdToken.VALUE);
        else
          valueSecond =
              Integer.parseUnsignedInt(
                  thirdToken.VALUE.substring(1, thirdToken.VALUE.length() - 1), 16);
      }

      String valueStr;
      switch (secondToken.VALUE) {
        case "band":
          valueReturn = valueFirst & valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
        case "bor":
          valueReturn = valueFirst | valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
        case "bxor":
          valueReturn = valueFirst ^ valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
        case "lshift":
          valueReturn = valueFirst << valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
        case "rshift":
          valueReturn = valueFirst >>> valueSecond;
          valueStr = Integer.toHexString(valueReturn);
          // When zero, the hex value isnt padded which can cause errors when we try to grab the
          // last 2 hex digits
          if (valueStr.length() < 2) valueStr = "0" + valueStr;
          valueStr = "0" + valueStr.substring(valueStr.length() - 2, valueStr.length()) + "h";

          result = new Token(TokenType.CONSTANT, valueStr);
          break;
      }
    }

    return result;
  }

  // Global will be managing multiple statements so it will return its
  private int global(List<Token> statement, int statementsIndex) {
    log("Processing global");
    return 1 + variables(statements.get(statementsIndex + 1), statementsIndex + 1);
  }

  private int variables(List<Token> statement, int statementsIndex) {
    log("Processing variables");
    int advanceTokens = 0;

    List<Token> nextStatement = statements.get(++statementsIndex);
    while (nextStatement.get(0).VALUE.equals("define")) {
      advanceTokens++;
      define(nextStatement);

      if ((statementsIndex + 1) < statements.size())
        nextStatement = statements.get(++statementsIndex);
      else break;
    }

    return advanceTokens;
  }

  private void define(List<Token> statement) {
    log("Processing define");
    Token identifier = statement.get(1);

    String type;
    // Some types are a combination of two tokens (e.g. unsigned integer)
    if (statement.size() > 6) {
      type = "";
      for (int i = 4; i < statement.size() - 1; i++) {
        type += statement.get(i).VALUE + " ";
      }
      type = type.substring(0, type.length() - 1);
    } else type = statement.get(statement.size() - 2).VALUE;

    TypedValue typedValue = new TypedValue(type, null);
    identifiers.put(identifier.VALUE, typedValue);

    log("Defining variable " + identifier.VALUE + " with type " + type);
  }

  private int implementations(List<Token> statement, int statementsIndex) {
    log("Processing implementations");
    // We currently only check for one function when there could be multiple
    return 1 + function(statements.get(statementsIndex + 1), statementsIndex + 1);
  }

  private int function(List<Token> statement, int statementsIndex) {
    log("Processing function");

    // TODO: create a helper function to add identifiers to the identifier list.
    // Currently we could accidentially define the same identifier twice. This should be prevented
    // by the parser but it wouldn't hurt to do the check in both places.
    Token identifier = statement.get(1);
    TypedValue typedValue = new TypedValue("subprogram", Integer.toString(subprograms.size()));

    identifiers.put(identifier.VALUE, typedValue);

    log("Defining subprogram " + identifier.VALUE + " with address " + typedValue.VALUE);

    int advanceIndex = 1;
    advanceIndex +=
        variables(statements.get(statementsIndex + advanceIndex), statementsIndex + advanceIndex);

    advanceIndex += 1;
    advanceIndex +=
        begin(statements.get(statementsIndex + advanceIndex), statementsIndex + advanceIndex);

    return advanceIndex;
  }

  private int begin(List<Token> statement, int statementsIndex) {
    log("Processing begin");
    int advanceTokens = 0;

    List<Token> nextStatement = statements.get(++statementsIndex);
    while (nextStatement.get(0).VALUE.equals("endfun")
        || nextStatement.get(0).VALUE.equals("set")
        || nextStatement.get(0).VALUE.equals("exit")
        || nextStatement.get(0).VALUE.equals("display")) {
      advanceTokens++;

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

      if ((statementsIndex + 1) < statements.size())
        nextStatement = statements.get(++statementsIndex);
      else break;
    }

    return advanceTokens;
  }

  private void set(List<Token> statement) {
    log("Processing set");

    Token identifier = statement.get(1);
    TypedValue typedValue = identifiers.get(identifier.VALUE);
    if (typedValue == null)
      throw new VariableNotYetDefined(
          "Tried to assign value to " + identifier.VALUE + " but it was not defined yet.");

    // We are not setting the variable to an expression
    if (statement.size() == 5) {
      // TODO: We should check to make sure that the value we are assigning matches the type
      // TODO: We need to get the value from identifiers
      Token value = statement.get(3);
      if (value.TYPE == TokenType.IDENTIFIER) {
        TypedValue newValue = identifiers.get(value.VALUE);
        typedValue = new TypedValue(typedValue.TYPE, newValue.VALUE);
        // log(typedValue.TYPE);
      } else typedValue = new TypedValue(typedValue.TYPE, value.VALUE);
    } else {
      // We need to get the expression from the statement and pass it to evaluateExpression() which
      // should return us a single token. From there we can match the value with the identifier
      Token value = evaluateExpression(new ArrayList<>(statement.subList(3, statement.size() - 1)));
      typedValue = new TypedValue(typedValue.TYPE, value.VALUE);
    }

    identifiers.replace(identifier.VALUE, typedValue);

    log(
        "set identifier "
            + identifier.VALUE
            + " to value "
            + typedValue.VALUE
            + " with type "
            + typedValue.TYPE);
  }

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
