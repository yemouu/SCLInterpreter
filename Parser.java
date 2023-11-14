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

  public Parser(File file) {
    SCLScanner scanner = new SCLScanner();
    scanner.tokenize(file);
    this.tokens = scanner.getTokens();
  }

  public Token getNextToken() {
    if (index < tokens.size()) return tokens.get(++index);
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

  // TODO: return an error instead of a boolean value
  private boolean expect(String expectedTokenType, Token token) {
    if (expectedTokenType.equals(token.TYPE)) return true;
    else { // TODO: This else clause should throw an error instead
      System.err.println(
          String.format("Parse Error: Expected %s, got %s", expectedTokenType, token.TYPE));
      return false;
    }
  }

  // TODO: return an error instead of a boolean value
  private boolean expect(String expectedTokenType, String expectedTokenValue, Token token) {
    if (expectedTokenType.equals(token.TYPE) && expectedTokenValue.equals(token.VALUE)) return true;
    else { // TODO: this else clause should throw and error instead of false
      System.err.println(
          String.format(
              "Parse Error: Expected %s with a value of %s, got %s with value of %s",
              expectedTokenType, expectedTokenValue, token.TYPE, token.VALUE));
      return false;
    }
  }

  public void begin() {
    while (peekNextToken() != null) {
      start();
    }
  }

  private void start() {
    Token nextToken = getNextToken();

    System.out.println("Next token is: " + nextToken.VALUE);

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
        // TODO: bring this error message inline with the other error messags
        System.err.println("Unexpected token");
        System.exit(1);
        break;
    }
  }

  private void imports(Token token) {
    System.out.println("Entering imports");
    if (!expect("keyword", "import", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting a literal next");
    literal(getNextToken());
  }

  private void literal(Token token) {
    System.out.println("Entering literal");
    if (!expect("literal", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting operator, special_symbol, or nothing next");

    Token nextToken = peekNextToken();
    switch (nextToken.TYPE) {
      case "operator":
        operator(getNextToken());
        break;
      case "special_symbol":
        special_symbol(getNextToken());
        break;
    }
  }

  private void symbols(Token token) {
    System.out.println("Entering symbols");
    if (!expect("keyword", "symbol", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting an identifier next");
    identifier(getNextToken());
  }

  private void identifier(Token token) {
    System.out.println("Entering identifiers");
    if (!expect("identifier", token)) System.exit(1);
    // TODO: Add all identifiers and their values to some sort of datastructure so they can be
    // recalled later.

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    // TODO: when called from define(), we aren't expecting anything
    System.out.println("Expecting a literal, constant, operator, special_symbol, or nothing next");

    // TODO: an end of statement keyword would be useful here.
    Token nextToken = peekNextToken();
    if (nextToken == null) return;
    switch (nextToken.TYPE) {
      case "literal":
        literal(getNextToken());
        break;
      case "constant":
        constant(getNextToken());
        break;
      case "operator":
        operator(getNextToken());
        break;
      case "special_symbol":
        special_symbol(getNextToken());
        break;
    }
  }

  private void constant(Token token) {
    System.out.println("Entering constants");
    if (!expect("constant", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting operator, special_symbol, or nothing next");

    Token nextToken = peekNextToken();
    switch (nextToken.TYPE) {
      case "operator":
        operator(getNextToken());
        break;
      case "special_symbol":
        special_symbol(getNextToken());
        break;
    }
  }

  private void operator(Token token) {
    System.out.println("Entering operator");
    if (!expect("operator", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println(
        "Expecting a literal, constant, identifier, operator, or special_symbol next");

    Token nextToken = peekNextToken();
    switch (nextToken.TYPE) {
      case "literal":
        literal(getNextToken());
        break;
      case "constant":
        constant(getNextToken());
        break;
      case "identifier":
        identifier(getNextToken());
        break;
      case "operator":
        operator(getNextToken());
        break;
      case "special_symbol":
        special_symbol(getNextToken());
        break;
      default:
        // TODO: bring this error message inline with the other error messags
        System.err.println("Unexpected token");
        System.exit(1);
        break;
    }
  }

  private void special_symbol(Token token) {
    System.out.println("Entering special_symbol");
    if (!expect("special_symbol", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting literal, constant, identifier, or nothing next");

    Token nextToken = peekNextToken();
    switch (nextToken.TYPE) {
      case "literal":
        literal(getNextToken());
        break;
      case "constant":
        constant(getNextToken());
        break;
      case "identifier":
        identifier(getNextToken());
        break;
    }
  }

  private void globals(Token token) {
    System.out.println("Entering globals");
    if (!expect("keyword", "global", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting declarations next");
    declarations(getNextToken());
  }

  private void declarations(Token token) {
    System.out.println("Entering declarations");
    if (!expect("keyword", "declarations", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting variables next");
    variables(getNextToken());
  }

  private void variables(Token token) {
    System.out.println("Entering variables");
    if (!expect("keyword", "variables", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting define next");

    while ((!peekNextToken().TYPE.equals("keyword")
            || !peekNextToken().VALUE.equals("implementations"))
        && (!peekNextToken().TYPE.equals("keyword") || !peekNextToken().VALUE.equals("begin")))
      define(getNextToken());
  }

  private void define(Token token) {
    System.out.println("Entering define");
    if (!expect("keyword", "define", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    // TODO: Consider having identifier, and other functions return their token value
    System.out.println("Expecting an identifier next");
    identifier(getNextToken());
    System.out.println("Back in define");

    System.out.println("Expecting of next");
    of(getNextToken());
  }

  private void of(Token token) {
    System.out.println("Entering of");
    if (!expect("keyword", "of", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting type next");
    type(getNextToken());
  }

  private void type(Token token) {
    System.out.println("Entering type");
    if (!expect("keyword", "type", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting either unsigned, integer, short, long, or byte next");

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
        // TODO: bring this error message inline with the other error messags
        System.err.println("Unexpected token");
        System.exit(1);
        break;
    }
  }

  private void unsigned(Token token) {
    System.out.println("Entering unsigned");
    if (!expect("keyword", "unsigned", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting either integer, short, or long next");

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
        // TODO: bring this error message inline with the other error messags
        System.err.println("Unexpected token");
        System.exit(1);
        break;
    }
  }

  private void integer(Token token) {
    System.out.println("Entering integer");
    if (!expect("keyword", "integer", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
  }

  private void _short(Token token) {
    System.out.println("Entering short");
    if (!expect("keyword", "short", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
  }

  private void _long(Token token) {
    System.out.println("Entering long");
    if (!expect("keyword", "long", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
  }

  private void _byte(Token token) {
    System.out.println("Entering byte");
    if (!expect("keyword", "byte", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
  }

  private void implementation(Token token) {
    System.out.println("Entering implementations");
    if (!expect("keyword", "implementations", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting function next");
    function(getNextToken());
  }

  private void function(Token token) {
    System.out.println("Entering function");
    if (!expect("keyword", "function", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting identifer next");
    identifier(getNextToken());

    System.out.println("Back in function");
    System.out.println("Expecting is next");
    is(getNextToken());

    System.out.println("Back in function");
    System.out.println("Expecting endfun next");
    endfun(getNextToken());
  }

  private void is(Token token) {
    System.out.println("Entering is");
    if (!expect("keyword", "is", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting variables next");
    variables(getNextToken());

    System.out.println("Back in is");
    System.out.println("Expecting begin next");
    _begin(getNextToken());
  }

  private void _begin(Token token) {
    System.out.println("Entering begin");
    if (!expect("keyword", "begin", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting either set, display, or exit next");

    while (!peekNextToken().TYPE.equals("keyword") || !peekNextToken().VALUE.equals("endfun")) {
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
          // TODO: bring this error message inline with the other error messags
          System.err.println("Unexpected token");
          System.err.println(getNextToken().VALUE);
          System.exit(1);
          break;
      }
    }
  }

  private void set(Token token) {
    System.out.println("Entering set");
    if (!expect("keyword", "set", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting an identifier next");
    identifier(getNextToken());
  }

  private void display(Token token) {
    System.out.println("Entering display");
    if (!expect("keyword", "display", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting an identifier or literal next");

    Token nextToken = peekNextToken();
    switch (nextToken.TYPE) {
      case "identifier":
        identifier(getNextToken());
        break;
      case "literal":
        literal(getNextToken());
        break;
      default:
        // TODO: bring this error message inline with the other error messags
        System.err.println("Unexpected token");
        System.exit(1);
        break;
    }
  }

  private void exit(Token token) {
    System.out.println("Entering exit");
    if (!expect("keyword", "exit", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
  }

  private void endfun(Token token) {
    System.out.println("Entering endfun");
    if (!expect("keyword", "endfun", token)) System.exit(1);

    System.out.println(String.format("Found a %s with value %s ", token.TYPE, token.VALUE));
    System.out.println("Expecting identifier next");
    identifier(getNextToken());
  }

  public static void main(String[] args) throws FileNotFoundException {
    if (args.length != 1) {
      System.out.println("Usage: java SCLScanner <filename>");
      return;
    }

    String filename = args[0];
    File file = new File(filename);

    Parser parser = new Parser(file);
    parser.begin();
  }
}
