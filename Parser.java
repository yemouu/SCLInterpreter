import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

// TODO: Do more checks during parsing
//       Currently, most checks being done are purely for the type of token we expect next rather
//       than the token's type. This is fine for parsing some tokens but can lead to issues for
//       operators. Some operators need two operands while others only need one.
// TODO: Create a list of statements that we can pass to the interpreter to get output of each file
// TODO: Consider having identifier, and other functions return their token value
public class Parser {
  private class KeyValuePair {
    public final String IDENT;
    public String VALUE;

    public KeyValuePair(String IDENT, String VALUE) {
      this.IDENT = IDENT;
      this.VALUE = VALUE;
    }
  }

  private List<Token> tokens;
  private int index = -1;
  private List<KeyValuePair> identifiers = new ArrayList<>();

  private String tokenFoundFormatString = "Found a token with type %s and value %s";
  private boolean verbose = false;

  public Parser(File file) {
    SCLScanner scanner = new SCLScanner();
    scanner.tokenize(file);
    this.tokens = scanner.getTokens();
  }

  public Parser(File file, boolean verbose) {
    this(file);
    this.verbose = verbose;
  }

  public Token getNextToken() throws TokenNotFoundException {
    if (index < tokens.size()) return tokens.get(++index);
    else throw new TokenNotFoundException();
  }

  private Token peekPrevToken() {
    if ((index - 1) >= 0) return tokens.get(index - 1);
    else return null;
  }

  private Token peekNextToken() {
    if ((index + 1) < tokens.size()) return tokens.get(index + 1);
    else return null;
  }

  public boolean identifierExists(String identifier) {
    for (KeyValuePair ident : identifiers) if (ident.IDENT.equals(identifier)) return true;
    return false;
  }

  private boolean expect(TokenType expectedTokenType, Token token) {
    return expectedTokenType == token.TYPE;
  }

  private boolean expect(TokenType expectedTokenType, String expectedTokenValue, Token token) {
    return (expectedTokenType == token.TYPE) && expectedTokenValue.equals(token.VALUE);
  }

  private void expectOrError(TokenType expectedType, Token token) throws UnexpectedTokenException {
    if (!expect(expectedType, token))
      throw new UnexpectedTokenException(
          String.format(
              "Expected token with type %s, got token with type %s and value %s",
              expectedType, token.TYPE, token.VALUE));
  }

  private void expectOrError(TokenType expectedType, String expectedValue, Token token)
      throws UnexpectedTokenException {
    if (!expect(expectedType, expectedValue, token))
      throw new UnexpectedTokenException(
          String.format(
              "Expected token with type %s and value %s, got a token with type %s and value %s",
              expectedType, expectedValue, token.TYPE, token.VALUE));
  }

  private void log(String message) {
    if (!verbose) return;
    System.err.println(message);
  }

  public void begin() {
    while (peekNextToken() != null) {
      start();
    }
  }

  private void start() {
    Token nextToken = getNextToken();

    log("Next token is of type " + nextToken.TYPE + " and value " + nextToken.VALUE);
    expectOrError(TokenType.KEYWORD, nextToken);

    switch (nextToken.VALUE) {
      case "import":
        imports(nextToken);
        break;
      case "symbol":
        symbols(nextToken);
        break;
      case "global":
        globals(nextToken);
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

  private void imports(Token token) {
    log("Entering imports");
    expectOrError(TokenType.KEYWORD, "import", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting a literal next");
    literal(getNextToken());
  }

  private void literal(Token token) {
    log("Entering literal");
    expectOrError(TokenType.LITERAL, token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting operator, special_symbol, or nothing next");

    Token nextToken = peekNextToken();
    if (nextToken == null) return;

    switch (nextToken.TYPE) {
      case OPERATOR:
        operator(getNextToken());
        break;
      case SPECIAL_SYMBOL:
        special_symbol(getNextToken());
        break;
      default:
        break;
    }
  }

  private void symbols(Token token) {
    log("Entering symbols");
    expectOrError(TokenType.KEYWORD, "symbol", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting an identifier next");
    identifier(getNextToken());
  }

  private void identifier(Token token) {
    log("Entering identifiers");
    expectOrError(TokenType.IDENTIFIER, token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    // TODO: Add all identifiers and their values to some sort of datastructure so they can be
    // recalled later.
    log("Expecting a literal, constant, operator, special_symbol, or nothing next");

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
      default:
        break;
    }
  }

  private void constant(Token token) {
    log("Entering constants");
    expectOrError(TokenType.CONSTANT, token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting operator, special_symbol, or nothing next");

    Token nextToken = peekNextToken();
    if (nextToken == null) return;

    switch (nextToken.TYPE) {
      case OPERATOR:
        operator(getNextToken());
        break;
      case SPECIAL_SYMBOL:
        special_symbol(getNextToken());
        break;
      default:
        break;
    }
  }

  // TODO: Validate that each operator has the opperands that it needs.
  //       The helper fuction peekPrevToken() should help here.
  private void operator(Token token) {
    log("Entering operator");
    expectOrError(TokenType.OPERATOR, token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting a literal, constant, identifier, operator, or special_symbol next");

    Token nextToken = getNextToken();
    switch (nextToken.TYPE) {
      case LITERAL:
        literal(nextToken);
        break;
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
  }

  private void special_symbol(Token token) {
    log("Entering special_symbol");
    expectOrError(TokenType.SPECIAL_SYMBOL, token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting literal, constant, identifier, or nothing next");

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
        break;
    }
  }

  private void globals(Token token) {
    log("Entering globals");
    expectOrError(TokenType.KEYWORD, "global", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting declarations next");
    declarations(getNextToken());
  }

  private void declarations(Token token) {
    log("Entering declarations");
    expectOrError(TokenType.KEYWORD, "declarations", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting variables next");
    variables(getNextToken());
  }

  private void variables(Token token) {
    log("Entering variables");
    expectOrError(TokenType.KEYWORD, "variables", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting define next");

    Token nextToken = peekNextToken();
    while (nextToken != null
        && !expect(TokenType.KEYWORD, "implementations", nextToken)
        && !expect(TokenType.KEYWORD, "begin", nextToken)) {
      define(getNextToken());
      nextToken = peekNextToken();
    }
  }

  private void define(Token token) {
    log("Entering define");
    expectOrError(TokenType.KEYWORD, "define", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting an identifier next");
    identifier(getNextToken());
    log("Back in define");

    log("Expecting of next");
    of(getNextToken());
  }

  private void of(Token token) {
    log("Entering of");
    expectOrError(TokenType.KEYWORD, "of", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting type next");
    type(getNextToken());
  }

  private void type(Token token) {
    log("Entering type");
    expectOrError(TokenType.KEYWORD, "type", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting either unsigned, integer, short, long, or byte next");

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

  private void unsigned(Token token) {
    log("Entering unsigned");
    expectOrError(TokenType.KEYWORD, "unsigned", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting either integer, short, or long next");

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

  private void integer(Token token) {
    log("Entering integer");
    expectOrError(TokenType.KEYWORD, "integer", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void _short(Token token) {
    log("Entering short");
    expectOrError(TokenType.KEYWORD, "short", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void _long(Token token) {
    log("Entering long");
    expectOrError(TokenType.KEYWORD, "long", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void _byte(Token token) {
    log("Entering byte");
    expectOrError(TokenType.KEYWORD, "byte", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void implementation(Token token) {
    log("Entering implementations");
    expectOrError(TokenType.KEYWORD, "implementations", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting function next");
    function(getNextToken());
  }

  private void function(Token token) {
    log("Entering function");
    expectOrError(TokenType.KEYWORD, "function", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting identifer next");
    identifier(getNextToken());

    log("Back in function");
    log("Expecting is next");
    is(getNextToken());

    log("Back in function");
    log("Expecting endfun next");
    endfun(getNextToken());
  }

  private void is(Token token) {
    log("Entering is");
    expectOrError(TokenType.KEYWORD, "is", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting variables next");
    variables(getNextToken());

    log("Back in is");
    log("Expecting begin next");
    _begin(getNextToken());
  }

  private void _begin(Token token) {
    log("Entering begin");
    expectOrError(TokenType.KEYWORD, "begin", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting either set, display, or exit next");

    Token nextToken = peekNextToken();
    while (nextToken != null && !expect(TokenType.KEYWORD, "endfun", nextToken)) {
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

  private void set(Token token) {
    log("Entering set");
    expectOrError(TokenType.KEYWORD, "set", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting an identifier next");
    identifier(getNextToken());
  }

  private void display(Token token) {
    log("Entering display");
    expectOrError(TokenType.KEYWORD, "display", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting an identifier or literal next");

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

  private void exit(Token token) {
    log("Entering exit");
    expectOrError(TokenType.KEYWORD, "exit", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void endfun(Token token) {
    log("Entering endfun");
    expectOrError(TokenType.KEYWORD, "endfun", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting identifier next");
    identifier(getNextToken());
  }

  public static void main(String[] args) throws FileNotFoundException {
    if (args.length != 1) {
      System.err.println("Usage: java SCLScanner <filename>");
      return;
    }

    String filename = args[0];
    File file = new File(filename);

    Parser parser = new Parser(file, true);
    parser.begin();
  }
}
