import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO: Add end of statement keyword
// Each statement should be a single line, so the newline character should be the end of statement
// keyword. End of statement will help us catch errors as well as know when we have the entire
// statement to execute
public class SCLScanner {
  // hardcode keywords
  private static final Set<String> KEYWORDS =
      new HashSet<>(
          Arrays.asList(
              "import",
              "symbol",
              "global",
              "variables",
              "define",
              "of",
              "type",
              "unsigned",
              "integer",
              "short",
              "long",
              "implementations",
              "function",
              "is",
              "begin",
              "set",
              "display",
              "exit",
              "endfun",
              "declarations",
              "byte"));

  // hardcode operators
  private static final Set<String> OPERATORS =
      new HashSet<>(Arrays.asList("=", "band", "bor", "bxor", "negate", "lshift", "rshift"));

  // hardcode special symbols
  private static final Set<String> SPECIAL_SYMBOLS = new HashSet<>(Arrays.asList(",", "(", ")"));

  // This is where our tokens will be stored.
  // Methods will be provided to access the tokens.
  private List<Token> tokens;

  // Parse the file for all tokens while excluding comments and docstrings.
  public void tokenize(File file) {
    // Create a new ArrayList to hold all of the tokens.
    tokens = new ArrayList<Token>();

    // Try opening the file using the UTF-8 character set
    try (FileReader fileReader = new FileReader(file, Charset.forName("UTF-8"))) {
      // Initialize token and character.
      // The token variable will be what we use to build up each token
      // before adding it to our array as we read from the file character by character.
      String token = "";
      int character;

      // Read each character of the file.
      // If we get -1 back, we have reached the end of the file.
      while ((character = fileReader.read()) != -1) {
        // Check for comments.
        // When we see a forward slash, we will look at the next character in the file.
        // We also enter this block if we see that description is our token.
        if (((character == '/') && token.equals("")) || token.equals("description")) {
          int nextCharacter = fileReader.read();

          // If the next character is an asterisk, we are in a multi multi line comment.
          // From here we can chew through the file until we reach the character combination `*/`.
          // We also enter this block if we see that description is our token.
          // We assume that description is a docstring and works like a multi line comment.
          if (nextCharacter == '*' || token.equals("description")) {
            int previousCharacter;

            // We keep track of both the next and the previous character to ensure that we are
            // stopping at `*/` and not at a single `*` or `/`
            while (character != -1) {
              previousCharacter = character;
              character = fileReader.read();

              // We found `*/`, break out of the while true loop
              if ((previousCharacter == '*') && (character == '/')) break;
            }

            // In most cases token is already empty, but if we got into this block because
            // we saw that description was our token, we need to empty the token otherwise
            // we lose the rest of the file because we continuously enter this block of code.
            token = "";
            // There is nothing else to do this iteration, read the next character.
            continue;
          } else if (nextCharacter == '/') {
            // If instead of an asterisk we find another forward slash,
            // we read to the end of the line and then read the next character.
            while ((character = fileReader.read()) != -1) if (character == '\n') break;
            // Nothing left to do this iteration
            continue;
          } else {
            // If we didn't find either, then we aren't looking at a comment.
            // The current implementation may not separate the operands of division
            // if they aren't separated by a space.
            // This implementation would also add whitespace to the token if the operands
            // of division where separated by a space.
            token += (char) character;
            token += (char) nextCharacter;

            // Because we added the new characters to the token already,
            // we can read the next character.
            continue;
          }
        }

        // This block of code will look for strings.
        // When we find a quotation mark we assume that this is the start of a string.
        // We then chew through the file looking for the next quotation mark.
        // This implementation does not account for escaped qutation marks.
        if (character == '"') {
          // Add the quotation mark to the token
          token += (char) character;

          // Add all the characters inbetween the quotation marks to the token
          while ((character = fileReader.read()) != -1)
            if (character != '"') token += (char) character;
            else break;

          // Add the ending quotation mark to the token
          token += (char) character;
          // Clear the character value so that the string immediately gets tokenized
          character = ' ';
        }

        // When we hit a whitespace, or special symbols we know that it is time to tokenize our
        // token. We check for special symbols here as well because these symbols are often attached
        // to other tokens instead of being separated by a space. Catching them here gives us a
        // chance at tokenizing them.
        if (Character.isWhitespace(character)
            || SPECIAL_SYMBOLS.contains(String.valueOf((char) character))) {
          // This isn't a particularly elegant solution. Every special symbol that can appear
          // infront of a token would need to added to the token like this instead of like every
          // other token. This affects `(`, `[` `{` as well as other special symbols that may appear
          // infront of other tokens.
          if (character == '(') token += (char) character;

          // If the token is empty there is no point in trying to progress.
          if (!token.equals("")) {
            // Here we check the token we built against our known keywords, operators, and special
            // symbols. Strings are checked depending on if they start and end with quotation marks
            // while constants are checked by if they start with a digit. We assume that everything
            // else is an identifier.
            if (KEYWORDS.contains(token)) tokens.add(new Token(TokenType.KEYWORD, token));
            else if (OPERATORS.contains(token)) tokens.add(new Token(TokenType.OPERATOR, token));
            else if (SPECIAL_SYMBOLS.contains(token))
              tokens.add(new Token(TokenType.SPECIAL_SYMBOL, token));
            else if (token.startsWith("\"") && token.endsWith("\""))
              tokens.add(new Token(TokenType.LITERAL, token));
            else if (Character.isDigit(token.charAt(0)))
              tokens.add(new Token(TokenType.CONSTANT, token));
            else tokens.add(new Token(TokenType.IDENTIFIER, token));
            token = "";
          }

          // If the current character was a whitespace, read the next character instead of
          // adding the whitespace into the next token.
          if (Character.isWhitespace(character)) continue;

          // This isn't a particularly elegant solution. Every special symbol that can appear
          // infront of a token would need to be skipped like this. Without doing this, the
          // special character would appear as its own token as well as with whatever identifer
          // or constant it was nexed too. This affects `(`, `[` `{` as well as other special
          // symbols that may appear infront of other tokens.
          if (character == '(') continue;
        }

        // Add the current character to the token
        token += (char) character;
      }
    } catch (FileNotFoundException error) {
      error.printStackTrace();
      System.exit(1);
    } catch (IOException error) {
      error.printStackTrace();
      System.exit(1);
    }
  }

  // Return a list of tokens to the parser
  public List<Token> getTokens() {
    return tokens;
  }

  // Helper to escape string literals in json
  private String jsonStringLiteralHelper(String str) {
    StringBuilder sb = new StringBuilder(str);

    // The order here doesn't really matter, but inerting to the start of the string first
    // will change the length which makes inserting to the end a bit awkward.
    sb.insert(str.length() - 1, "\\");
    sb.insert(0, "\\");

    return sb.toString();
  }

  // Build and return the tokens in json format
  public String toJson() {
    String json = "{";
    for (int i = 0; i < tokens.size(); i++) {
      Token token = tokens.get(i);
      json +=
          "\n\t\"Token_"
              + i
              + "\": {\n\t\t\"Type\": \""
              + token.TYPE
              + "\",\n\t\t\"value\": \""
              + (token.TYPE != TokenType.LITERAL ? token.VALUE : jsonStringLiteralHelper(token.VALUE))
              + "\"\n\t}";
      if ((i + 1) != tokens.size()) json += ",";
    }
    return json += "\n}";
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java SCLScanner <filename>");
      return;
    }

    String filename = args[0];
    File file = new File(filename);
    SCLScanner SCLScanner = new SCLScanner();
    SCLScanner.tokenize(file);

    for (Token token : SCLScanner.getTokens()) System.out.println(token.TYPE + ":\t" + token.VALUE);
    System.out.println(SCLScanner.toJson());
  }
}
