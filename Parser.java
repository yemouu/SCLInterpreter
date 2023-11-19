import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Parser {
  // We will use this to make sure that each identifier is defined before it is used as well as
  // ensuring that no identifier is defined twice. This doesn't take into account the scope of
  // variables and variable scopes aren't handled in our parser at all.
  private List<String> identifiers = new ArrayList<>();

  // This is what will be passed to the Interpreter to process. We put each token into a list until
  // we reach the end of statement token. When we reach the end of token statement, we add the
  // statement to the list of statements and create a new list of tokens for the next statement.
  private List<List<Token>> statements = new ArrayList<>();
  private List<Token> statementBuilder = new ArrayList<>();

  // The tokens from the parser. It is marked as final because we don't need to and shouldn't need
  // to edit it. The index is what we will use to progres through the the list of tokens and is
  // controlled by the getNextToken() method. We can look one token forwards and backwards using the
  // peekNextToken() and peekPrevToken() methods. Realistically, we could look at any specific point
  // in the list but it usually isn't that helpful.
  private final List<Token> tokens;
  private int index = -1;

  // Controls if the log() calls will display text to stderr or not.
  private boolean verbose = false;

  // Constructor. Creates a SCLScanner class to tokenize a file and populate the tokens variable.
  public Parser(File file) {
    SCLScanner scanner = new SCLScanner();
    scanner.tokenize(file);
    this.tokens = scanner.getTokens();
  }

  // Same as above but allows control over the verbose variable.
  public Parser(File file, boolean verbose) {
    this(file);
    this.verbose = verbose;
  }

  // "Consumes" the next token and returns it to the caller. If there is no next token, throws a
  // TokenNotFoundException.
  public Token getNextToken() {
    if (index < tokens.size()) return tokens.get(++index);
    else throw new TokenNotFoundException();
  }

  // Looks at the previous token in our list of tokens and returns it. If there isn't a previous
  // token, returns null. This doesn't "consume" the value. Helpful for checking syntax errors.
  private Token peekPrevToken() {
    if ((index - 1) >= 0) return tokens.get(index - 1);
    else return null;
  }

  // Same as above but looks for the next token.
  private Token peekNextToken() {
    if ((index + 1) < tokens.size()) return tokens.get(index + 1);
    else return null;
  }

  // Return our list of statements. This is how the list gets passed to the interpreter.
  public List<List<Token>> getStatements() {
    return statements;
  }

  // Simple method to optionally print messages detailing the parser's execution.
  private void log(String message) {
    if (!verbose) return;
    System.err.println(message);
  }

  // Parser entry point. We continuously call start() untill we have no more tokens left. At that
  // point, the entire file has been parsed.
  public void begin() {
    while (peekNextToken() != null) {
      start();
    }
  }

  // Start will parse the top level statements of a file. In our test file, these top level
  // statements are import, symbol, global, and implementations. There could be more in the entire
  // SCL language. If we get a token we don't expect, we throw an error an UnexpectedTokenException.
  // The error message will state what token was unexpected making adding new tokens easier or
  // allowing the user to easily find any typo or error in their program. When we find one of our
  // expected tokens, we call a specific function to deal with that statement or series of
  // statements.
  private void start() {
    Token nextToken = getNextToken();

    log("Next token is of type " + nextToken.TYPE + " and value " + nextToken.VALUE);
    Token.expectOrError(TokenType.KEYWORD, nextToken);

    switch (nextToken.VALUE) {
      case "import":
        _import(nextToken);
        break;
      case "symbol":
        symbol(nextToken);
        break;
      case "global":
        global(nextToken);
        break;
      case "implementations":
        implementation(nextToken);
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  // Simple method that logs when a token is found. This helps standardize these kinds of messages
  // so that we don't have to change this in multiple places.
  private void foundToken(Token token) {
    log(String.format("Found a token with type %s and value %s", token.TYPE, token.VALUE));
    statementBuilder.add(token);
  }

  // Parsing import. We ensure that the token is the token that we expect and because we know that
  // only literals can appear after the import token, we call literal() next to parse import's
  // argument.
  // This is the format that most of the remaining functions will follow. They will validate their
  // own token and then look to call another function that makes sense to call based on its own
  // token.
  private void _import(Token token) {
    log("Entering imports");
    Token.expectOrError(TokenType.KEYWORD, "import", token);
    foundToken(token);

    log("Expecting a literal");
    literal(getNextToken());
  }

  // Parse the literal token
  private void literal(Token token) {
    log("Entering literal");
    Token.expectOrError(TokenType.LITERAL, token);
    foundToken(token);

    // The prevToken shouldn't be null if we made it this far, but it doesn't hurt to check.
    Token prevToken = peekPrevToken();
    if (prevToken == null) throw new TokenNotFoundException();

    // If our previous token was the import token, we can end this statement and method here.
    if (Token.expect(TokenType.KEYWORD, "import", prevToken)) {
      log("Previous token was keyword:import, expecting end of statement");
      endOfStatement(getNextToken());
      return;
    }

    log("Expecting an operator, special_symbol, end of statement, or nothing");

    Token nextToken = peekNextToken();
    if (nextToken == null) return;

    switch (nextToken.TYPE) {
      case SPECIAL_SYMBOL:
        special_symbol(getNextToken());
        break;
      case END_OF_STATEMENT:
        endOfStatement(getNextToken());
        break;
      default:
        break;
    }
  }

  // Parse the end of statement token
  private void endOfStatement(Token token) {
    log("Entering end of statement");
    Token.expectOrError(TokenType.END_OF_STATEMENT, token);
    foundToken(token);

    // Add the list of tokens to our statements list and then create a new list of tokens for the
    // next statement.
    statements.add(statementBuilder);
    statementBuilder = new ArrayList<>();
  }

  // Parse the symbol keyword
  private void symbol(Token token) {
    log("Entering symbols");
    Token.expectOrError(TokenType.KEYWORD, "symbol", token);
    foundToken(token);

    log("Expecting an identifier");
    identifier(getNextToken());
  }

  // Parse the identifier token
  private void identifier(Token token) {
    log("Entering identifiers");
    Token.expectOrError(TokenType.IDENTIFIER, token);
    foundToken(token);

    // Check to see if the identifier is being defined or accessed. We can tell if the identifier is
    // being defined if peekPrevToken() reveals either the define, function, or symbol token. If
    // neither of those tokens are revealed, the identifier is being accessed.
    // If the identifier is being defined, check if it was already defined. If it was, this is a
    // parse error. If the identifier is being accessed, check if it was already defined. If it
    // wasn't this is also a parse error.
    Token prevToken = peekPrevToken();
    if (prevToken == null) throw new TokenNotFoundException();

    if (Token.expect(TokenType.KEYWORD, "define", prevToken)
        || Token.expect(TokenType.KEYWORD, "symbol", prevToken)
        || Token.expect(TokenType.KEYWORD, "function", prevToken)) {
      if (identifiers.contains(token.VALUE))
        throw new IdentifierAleadyDefinedException(
            "Identifier " + token.VALUE + "  was already defined");

      identifiers.add(token.VALUE);
      log("New identifier " + token.VALUE + "was added to identifiers list");
    } else if (!identifiers.contains(token.VALUE))
      throw new IdentifierNotDefinedException(
          "Tried accessing identifier " + token.VALUE + " but it was not defined yet");

    // If we have endfun as our previous keyword, our next token should be end of statement
    if (Token.expect(TokenType.KEYWORD, "endfun", prevToken)) {
      log("Previous token was " + prevToken + ", expecting end of statement");
      endOfStatement(getNextToken());
      return;
    }

    log("Expecting a literal, constant, operator, special_symbol, end of statement, or nothing");

    Token nextToken = peekNextToken();
    if (nextToken == null) return;

    switch (nextToken.TYPE) {
      case LITERAL:
        literal(getNextToken());
        break;
      case CONSTANT:
        constant(getNextToken());
        break;
      case OPERATOR:
        operator(getNextToken());
        break;
      case SPECIAL_SYMBOL:
        special_symbol(getNextToken());
        break;
      case END_OF_STATEMENT:
        endOfStatement(getNextToken());
        break;
      default:
        break;
    }
  }

  // Parse constant token
  private void constant(Token token) {
    log("Entering constants");
    Token.expectOrError(TokenType.CONSTANT, token);
    foundToken(token);

    log("Expecting operator, special_symbol, or nothing");

    Token nextToken = peekNextToken();
    if (nextToken == null) return;

    switch (nextToken.TYPE) {
      case OPERATOR:
        operator(getNextToken());
        break;
      case SPECIAL_SYMBOL:
        special_symbol(getNextToken());
        break;
      case END_OF_STATEMENT:
        endOfStatement(getNextToken());
        break;
      default:
        break;
    }
  }

  // Parse operator token. The only operators we support are = and the bitwise operators, band, bor,
  // bxor, lshift, rshift, and negate.
  private void operator(Token token) {
    log("Entering operator");
    Token.expectOrError(TokenType.OPERATOR, token);
    foundToken(token);

    // If our operator is =, we should be expecting either a literal, constant, identifier,
    // operator, or special_symbol. Additonally, our previous token should be an identifier.
    if (Token.expect(TokenType.OPERATOR, "=", token)) {
      Token.expectOrError(TokenType.IDENTIFIER, peekPrevToken());

      Token nextToken = getNextToken();
      log("Expecting a constant, identifier, operator, or special_symbol");
      switch (nextToken.TYPE) {
        case CONSTANT:
          constant(nextToken);
          break;
        case IDENTIFIER:
          identifier(nextToken);
          break;
        case OPERATOR:
          operator(nextToken);
          break;
        case SPECIAL_SYMBOL:
          special_symbol(nextToken);
          break;
        default:
          throw new UnexpectedTokenException(
              String.format(
                  "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
      }
      return;
    }

    // If our operator is negate, we should be expecting either a constant, identifier, or
    // special_symbol next while we expect either a special_symbol, or operator:= before this token.
    if (Token.expect(TokenType.OPERATOR, "negate", token)) {
      Token prevToken = peekPrevToken();
      if (!Token.expect(TokenType.OPERATOR, "=", prevToken)
          && !Token.expect(TokenType.SPECIAL_SYMBOL, "(", prevToken))
        throw new UnexpectedTokenException(
            "Expected either OPERATOR:= or SPECIAL_SYMBOL:(, got " + prevToken);

      log("Expecting a literal, constant, identifier, operator, or special_symbol");
      Token nextToken = getNextToken();
      switch (nextToken.TYPE) {
        case CONSTANT:
          constant(nextToken);
          break;
        case IDENTIFIER:
          identifier(nextToken);
          break;
        case SPECIAL_SYMBOL:
          special_symbol(nextToken);
          break;
        default:
          throw new UnexpectedTokenException(
              String.format(
                  "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
      }
      return;
    }

    // If our operator is band, bor, bxor, lshift, or rshift, we should be expecting either a
    // constant, identifier, or special_symbol next. These should also be our previous tokens.

    log("Expecting a constant, identifier, or special_symbol");

    Token nextToken = getNextToken();
    switch (nextToken.TYPE) {
      case CONSTANT:
        constant(nextToken);
        break;
      case IDENTIFIER:
        identifier(nextToken);
        break;
      case SPECIAL_SYMBOL:
        special_symbol(nextToken);
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  // Parse special symbols. The only special symbols we currently support are comma, (, and ).
  private void special_symbol(Token token) {
    log("Entering special_symbol");
    Token.expectOrError(TokenType.SPECIAL_SYMBOL, token);
    foundToken(token);

    // If our special symbol is a comma, we expect either a literal, constant, or identifier.
    if (Token.expect(TokenType.SPECIAL_SYMBOL, ",", token)) {
      log("Expecting literal, constant, or identifier");

      Token nextToken = peekNextToken();
      if (nextToken == null) return;
      switch (nextToken.TYPE) {
        case LITERAL:
          literal(getNextToken());
          break;
        case CONSTANT:
          constant(getNextToken());
          break;
        case IDENTIFIER:
          identifier(getNextToken());
          break;
        default:
          throw new UnexpectedTokenException(
              "Expected either literal, constant, or identifier, got" + nextToken);
      }
      return;
    }

    // If our special symbol is (, we expect either a constant, operator, special_symbol, or
    // identifier.
    if (Token.expect(TokenType.SPECIAL_SYMBOL, "(", token)) {
      log("Expecting constant or identifier");

      Token nextToken = peekNextToken();
      if (nextToken == null) return;
      switch (nextToken.TYPE) {
        case CONSTANT:
          constant(getNextToken());
          break;
        case OPERATOR:
          operator(getNextToken());
          break;
        case SPECIAL_SYMBOL:
          special_symbol(getNextToken());
          break;
        case IDENTIFIER:
          identifier(getNextToken());
          break;
        default:
          throw new UnexpectedTokenException(
              "Expected either literal, constant, or identifier, got" + nextToken);
      }
      return;
    }

    // If our special symbol is ), we expect either a constant, identifier, operator,
    // special_symbol, or end of statement.
    if (Token.expect(TokenType.SPECIAL_SYMBOL, ")", token)) {
      log("Expecting constant, identifier, or end of statement");

      Token nextToken = peekNextToken();
      if (nextToken == null) return;
      switch (nextToken.TYPE) {
        case CONSTANT:
          constant(getNextToken());
          break;
        case IDENTIFIER:
          identifier(getNextToken());
          break;
        case OPERATOR:
          operator(getNextToken());
          break;
        case SPECIAL_SYMBOL:
          special_symbol(getNextToken());
          break;
        case END_OF_STATEMENT:
          endOfStatement(getNextToken());
          break;
        default:
          throw new UnexpectedTokenException(
              "Expected either literal, constant, or identifier, got" + nextToken);
      }
      return;
    }
  }

  // Parse global keyword
  private void global(Token token) {
    log("Entering globals");
    Token.expectOrError(TokenType.KEYWORD, "global", token);
    foundToken(token);

    log("Expecting declarations");
    declarations(getNextToken());
  }

  // Parse declarations keyword. This is a multiline statement, so we consume an end of statement
  // before we can get the next token we need.
  private void declarations(Token token) {
    log("Entering declarations");
    Token.expectOrError(TokenType.KEYWORD, "declarations", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());

    log("Back in declarations");
    log("Expecting variables");
    variables(getNextToken());
  }

  // Parse variables keyword
  private void variables(Token token) {
    log("Entering variables");
    Token.expectOrError(TokenType.KEYWORD, "variables", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());

    log("Back in variables");
    log("Expecting defines");
    // Continuously parse the next token until we reach a token that indicates that we are done. In
    // this case, either the implementations keyword or the begin keyword.
    Token nextToken = peekNextToken();
    while (nextToken != null
        && !Token.expect(TokenType.KEYWORD, "implementations", nextToken)
        && !Token.expect(TokenType.KEYWORD, "begin", nextToken)) {
      define(getNextToken());
      nextToken = peekNextToken();
    }
  }

  // Parse define keyword
  private void define(Token token) {
    log("Entering define");
    Token.expectOrError(TokenType.KEYWORD, "define", token);
    foundToken(token);

    log("Expecting an identifier");
    identifier(getNextToken());

    log("Back in define");
    log("Expecting of");
    of(getNextToken());
  }

  // Parse of keyword
  private void of(Token token) {
    log("Entering of");
    Token.expectOrError(TokenType.KEYWORD, "of", token);
    foundToken(token);

    log("Expecting type");
    type(getNextToken());
  }

  // Parse type keyword
  private void type(Token token) {
    log("Entering type");
    Token.expectOrError(TokenType.KEYWORD, "type", token);
    foundToken(token);

    log("Expecting either unsigned, integer, short, long, or byte");

    Token nextToken = getNextToken();
    switch (nextToken.VALUE) {
      case "unsigned":
        unsigned(nextToken);
        break;
      case "integer":
        integer(nextToken);
        break;
      case "short":
        _short(nextToken);
        break;
      case "long":
        _long(nextToken);
        break;
      case "byte":
        _byte(nextToken);
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  // Parse unsigned keyowrd
  private void unsigned(Token token) {
    log("Entering unsigned");
    Token.expectOrError(TokenType.KEYWORD, "unsigned", token);
    foundToken(token);

    log("Expecting either integer, short, or long");

    Token nextToken = getNextToken();
    switch (nextToken.VALUE) {
      case "integer":
        integer(nextToken);
        break;
      case "short":
        _short(nextToken);
        break;
      case "long":
        _long(nextToken);
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  // Parse integer keyword
  private void integer(Token token) {
    log("Entering integer");
    Token.expectOrError(TokenType.KEYWORD, "integer", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());
  }

  // Parse short keyword
  private void _short(Token token) {
    log("Entering short");
    Token.expectOrError(TokenType.KEYWORD, "short", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());
  }

  // Parse long keyword
  private void _long(Token token) {
    log("Entering long");
    Token.expectOrError(TokenType.KEYWORD, "long", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());
  }

  // Parse byte keyword
  private void _byte(Token token) {
    log("Entering byte");
    Token.expectOrError(TokenType.KEYWORD, "byte", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());
  }

  // Parse implementation keyword
  private void implementation(Token token) {
    log("Entering implementations");
    Token.expectOrError(TokenType.KEYWORD, "implementations", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());

    log("Back in implementations");
    log("Expecting function");
    // Continuously parse the next token until we reach the end of the file.
    Token nextToken = peekNextToken();
    while (peekNextToken() != null) {
      if (Token.expect(TokenType.KEYWORD, "function", nextToken)) function(getNextToken());
      else throw new UnexpectedTokenException("Expected function keyword but got, " + nextToken);
    }
  }

  // Parse function keyword
  private void function(Token token) {
    log("Entering function");
    Token.expectOrError(TokenType.KEYWORD, "function", token);
    foundToken(token);

    log("Expecting identifer");
    identifier(getNextToken());

    log("Back in function");
    log("Expecting is");
    is(getNextToken());

    log("Back in function");
    log("Expecting endfun");
    endfun(getNextToken());
  }

  // Parse is keyword
  private void is(Token token) {
    log("Entering is");
    Token.expectOrError(TokenType.KEYWORD, "is", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());

    log("Expecting variables");
    variables(getNextToken());

    log("Back in is");
    log("Expecting begin");
    _begin(getNextToken());
  }

  // Parse begin keyword
  private void _begin(Token token) {
    log("Entering begin");
    Token.expectOrError(TokenType.KEYWORD, "begin", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());

    log("Back in begin");
    log("Expecting either set, display, or exit");
    // Continuously parse the next token until we reach a token that indicates that we are done. In
    // this case, the endfun keyword.
    Token nextToken = peekNextToken();
    while (!Token.expect(TokenType.KEYWORD, "endfun", nextToken)) {
      switch (nextToken.VALUE) {
        case "set":
          set(getNextToken());
          break;
        case "display":
          display(getNextToken());
          break;
        case "exit":
          exit(getNextToken());
          break;
        default:
          throw new UnexpectedTokenException(
              String.format(
                  "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
      }

      nextToken = peekNextToken();
    }
  }

  // Parse set keyword
  private void set(Token token) {
    log("Entering set");
    Token.expectOrError(TokenType.KEYWORD, "set", token);
    foundToken(token);

    log("Expecting an identifier");
    identifier(getNextToken());
  }

  // Parse display keyword
  private void display(Token token) {
    log("Entering display");
    Token.expectOrError(TokenType.KEYWORD, "display", token);
    foundToken(token);

    log("Expecting an identifier or literal");
    Token nextToken = getNextToken();

    switch (nextToken.TYPE) {
      case IDENTIFIER:
        identifier(nextToken);
        break;
      case LITERAL:
        literal(nextToken);
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  // Parse exit keyword
  private void exit(Token token) {
    log("Entering exit");
    Token.expectOrError(TokenType.KEYWORD, "exit", token);
    foundToken(token);

    log("Expecting end of statement");
    endOfStatement(getNextToken());
  }

  // Parse endfun keyword
  private void endfun(Token token) {
    log("Entering endfun");
    Token.expectOrError(TokenType.KEYWORD, "endfun", token);
    foundToken(token);

    log("Expecting identifier next");
    identifier(getNextToken());
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("Usage: java SCLScanner <filename>");
      return;
    }

    String filename = args[0];
    File file = new File(filename);

    Parser parser = new Parser(file);
    parser.begin();

    List<List<Token>> statements = parser.getStatements();
    for (List<Token> statement : statements) {
      for (Token token : statement) {
        if (Token.expect(TokenType.IDENTIFIER, token) || Token.expect(TokenType.CONSTANT, token))
          System.out.print(token + " ");
        else System.out.print(token.VALUE + " ");
      }
      System.out.println();
    }
  }
}
