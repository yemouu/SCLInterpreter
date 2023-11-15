import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

// TODO: Do more checks during parsing
//       Currently, most checks being done are purely for the type of token we expect next rather
//       than the token's type. This is fine for parsing some tokens but can lead to issues for
//       operators. Some operators need two operands while others only need one.
// TODO: We are failing to check for null in multiple areas of the code
//       We should throw errors instead of null
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

  public Token getNextToken() {
    if (index < tokens.size()) return tokens.get(++index);
    else return null;
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

  private void expectOrError(TokenType expectedTokenType, Token token)
      throws UnexpectedTokenException {
    if (!expect(expectedTokenType, token))
      System.err.println(
          String.format(
              "Expected token with type %s, got token with type %s",
              expectedTokenType, token.TYPE));
  }

  private void expectOrError(TokenType expectedTokenType, String expectedTokenValue, Token token)
      throws UnexpectedTokenException {
    if (!expect(expectedTokenType, expectedTokenValue, token))
      throw new UnexpectedTokenException(
          String.format(
              "Expected token with type %s and value %s, got a token with type %s and value %s",
              expectedTokenType, expectedTokenValue, token.TYPE, token.VALUE));
  }

  private void log(String formatString, Object... args) {
    if (!verbose) return;
    System.err.println(String.format(formatString, args));
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
    if (token == null) return;

    log("Entering imports");
    expectOrError(TokenType.KEYWORD, "import", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting a literal next");
    // TODO: Missing null checks
    literal(getNextToken());
  }

  private void literal(Token token) {
    if (token == null) return;

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
    if (token == null) return;

    log("Entering symbols");
    expectOrError(TokenType.KEYWORD, "symbol", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting an identifier next");
    // TODO: Missing null checks
    identifier(getNextToken());
  }

  private void identifier(Token token) {
    if (token == null) return;

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
    if (token == null) return;

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
    if (token == null) return;

    log("Entering operator");
    expectOrError(TokenType.OPERATOR, token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log(
        "Expecting a literal, constant, identifier, operator, or special_symbol next");

    Token nextToken = peekNextToken();
    if (nextToken == null) throw new TokenNotFoundException();

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
      case OPERATOR:
        operator(getNextToken());
        break;
      case SPECIAL_SYMBOL:
        special_symbol(getNextToken());
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  private void special_symbol(Token token) {
    if (token == null) return;

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
    if (token == null) return;

    log("Entering globals");
    expectOrError(TokenType.KEYWORD, "global", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting declarations next");
    // TODO: Missing null checks
    declarations(getNextToken());
  }

  private void declarations(Token token) {
    if (token == null) return;

    log("Entering declarations");
    expectOrError(TokenType.KEYWORD, "declarations", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting variables next");
    // TODO: Missing null checks
    variables(getNextToken());
  }

  private void variables(Token token) {
    if (token == null) return;

    log("Entering variables");
    expectOrError(TokenType.KEYWORD, "variables", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting define next");

    // TODO: Missing null checks
    while (!expect(TokenType.KEYWORD, "implementations", peekNextToken())
        && !expect(TokenType.KEYWORD, "begin", peekNextToken())) define(getNextToken());
  }

  private void define(Token token) {
    if (token == null) return;

    log("Entering define");
    expectOrError(TokenType.KEYWORD, "define", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));

    log("Expecting an identifier next");
    // TODO: Missing null checks
    identifier(getNextToken());
    log("Back in define");

    log("Expecting of next");
    // TODO: Missing null checks
    of(getNextToken());
  }

  private void of(Token token) {
    if (token == null) return;

    log("Entering of");
    expectOrError(TokenType.KEYWORD, "of", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting type next");
    // TODO: Missing null checks
    type(getNextToken());
  }

  private void type(Token token) {
    if (token == null) return;

    log("Entering type");
    expectOrError(TokenType.KEYWORD, "type", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting either unsigned, integer, short, long, or byte next");

    // TODO: Missing null checks
    Token nextToken = peekNextToken();
    switch (nextToken.VALUE) {
      case "unsigned":
        unsigned(getNextToken());
        break;
      case "integer":
        integer(getNextToken());
        break;
      case "short":
        _short(getNextToken());
        break;
      case "long":
        _long(getNextToken());
        break;
      case "byte":
        _byte(getNextToken());
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  private void unsigned(Token token) {
    if (token == null) return;

    log("Entering unsigned");
    expectOrError(TokenType.KEYWORD, "unsigned", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting either integer, short, or long next");

    // TODO: Missing null checks
    Token nextToken = peekNextToken();
    switch (nextToken.VALUE) {
      case "integer":
        integer(getNextToken());
        break;
      case "short":
        _short(getNextToken());
        break;
      case "long":
        _long(getNextToken());
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  private void integer(Token token) {
    if (token == null) return;

    log("Entering integer");
    expectOrError(TokenType.KEYWORD, "integer", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void _short(Token token) {
    if (token == null) return;

    log("Entering short");
    expectOrError(TokenType.KEYWORD, "short", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void _long(Token token) {
    if (token == null) return;

    log("Entering long");
    expectOrError(TokenType.KEYWORD, "long", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void _byte(Token token) {
    if (token == null) return;

    log("Entering byte");
    expectOrError(TokenType.KEYWORD, "byte", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void implementation(Token token) {
    if (token == null) return;

    log("Entering implementations");
    expectOrError(TokenType.KEYWORD, "implementations", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting function next");
    // TODO: Missing null checks
    function(getNextToken());
  }

  private void function(Token token) {
    if (token == null) return;

    log("Entering function");
    expectOrError(TokenType.KEYWORD, "function", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting identifer next");
    // TODO: Missing null checks
    identifier(getNextToken());

    log("Back in function");
    log("Expecting is next");
    // TODO: Missing null checks
    is(getNextToken());

    log("Back in function");
    log("Expecting endfun next");
    // TODO: Missing null checks
    endfun(getNextToken());
  }

  private void is(Token token) {
    if (token == null) return;

    log("Entering is");
    expectOrError(TokenType.KEYWORD, "is", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting variables next");
    // TODO: Missing null checks
    variables(getNextToken());

    log("Back in is");
    log("Expecting begin next");
    // TODO: Missing null checks
    _begin(getNextToken());
  }

  private void _begin(Token token) {
    if (token == null) return;

    log("Entering begin");
    expectOrError(TokenType.KEYWORD, "begin", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting either set, display, or exit next");

    // TODO: Missing null checks
    while (!expect(TokenType.KEYWORD, "endfun", peekNextToken())) {
      Token nextToken = peekNextToken();
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
    }
  }

  private void set(Token token) {
    if (token == null) return;

    log("Entering set");
    expectOrError(TokenType.KEYWORD, "set", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting an identifier next");
    // TODO: Missing null checks
    identifier(getNextToken());
  }

  private void display(Token token) {
    if (token == null) return;

    log("Entering display");
    expectOrError(TokenType.KEYWORD, "display", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting an identifier or literal next");

    Token nextToken = peekNextToken();
    if (nextToken == null) throw new TokenNotFoundException();

    switch (nextToken.TYPE) {
      case IDENTIFIER:
        identifier(getNextToken());
        break;
      case LITERAL:
        literal(getNextToken());
        break;
      default:
        throw new UnexpectedTokenException(
            String.format(
                "Unexpected token with type %s and value %s", nextToken.TYPE, nextToken.VALUE));
    }
  }

  private void exit(Token token) {
    if (token == null) return;

    log("Entering exit");
    expectOrError(TokenType.KEYWORD, "exit", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
  }

  private void endfun(Token token) {
    if (token == null) return;

    log("Entering endfun");
    expectOrError(TokenType.KEYWORD, "endfun", token);

    log(String.format(tokenFoundFormatString, token.TYPE, token.VALUE));
    log("Expecting identifier next");
    // TODO: Missing null checks
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
