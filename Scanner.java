import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class Scanner {
  private static final Set<String> KEYWORDS =
      new HashSet<>(
          Arrays.asList(
              "import",
              "description",
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
              "byte",
              "main")); // hardcode keywords
  private static final Set<Character> OPERATORS =
      new HashSet<>(
          Arrays.asList(
              '+', '-', '*', '/', '%', '=', '<', '>', '&', '|', '^')); // hardcode operators
  private static final Set<Character> Special_Symbols =
      new HashSet<>(
          Arrays.asList(
              '~', '!', '?', ':', ',', ';', '(', ')', '{', '}', '[',
              ']')); // hardcode special symbols

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java SCLScanner <filename>");
      return;
    }

    String filename = args[0];
    List<String> tokens = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
      String line;

      // While loop that is used to read the scl file line by line
      while ((line = reader.readLine()) != null) {

        // The StringTokenizer constructor takes two arguments: the input line to be tokenized, and
        // a String containing the delimiters to use for tokenization.
        // The true argument at the end of the StringTokenizer constructor specifies that the
        // delimiters should be included as tokens in the output.
        StringTokenizer tokenizer = new StringTokenizer(line, " \t\n\r\f\"\'/", true);
        String currentToken = "";

        // The while loop iterates over each token in the input file using the StringTokenizer
        // object
        while (tokenizer.hasMoreTokens()) {
          String token = tokenizer.nextToken(); // Get next token, in new line if necessary

          // If the current token is a forward slash /, the code block checks the next token to
          // determine if it is the start of a single-line or multi-line comment.
          if (token.equals("/")) { // start of comment
            if (tokenizer.hasMoreTokens()) {

              String nextToken = tokenizer.nextToken();
              if (nextToken.equals("/")) { // single-line comment
                break;

                // If the next token is an asterisk *, the code block recognizes the comment as a
                // multi-line comment and enters a
                // nested while loop to skip over all the tokens until the end of the comment is
                // reached.
              } else if (nextToken.equals("*")) { // multi-line comment
                while (tokenizer.hasMoreTokens()) {
                  token = tokenizer.nextToken();

                  if (token.equals("*")) {
                    if (tokenizer.hasMoreTokens()) {
                      nextToken = tokenizer.nextToken();
                      if (nextToken.equals("/")) {
                        break;
                      } else {
                        token += nextToken;
                      }
                    }
                  }
                }
              }

              // If the next token is not a comment, the code block concatenates the current token
              // and the next token and continues tokenizing the input file.
              else { // not a comment
                token += nextToken;
              }
            }

          }

          // If the current token is a double quote ", the code block recognizes it as the start of
          // a string literal and enters
          // a nested while loop to read all the tokens until the end of the string literal is
          // reached.
          else if (token.equals("\"")) { // start of string literal
            currentToken = token;
            while (tokenizer.hasMoreTokens()) {
              token = tokenizer.nextToken();
              currentToken += token;
              if (token.equals("\"")) { // end of string literal
                break;
              }
            }
            tokens.add("LITERAL: " + currentToken);
          }

          // If the current token is a keyword, the code block adds it to the tokens list with a
          // KEYWORD token type.
          else if (KEYWORDS.contains(token)) {
            tokens.add("KEYWORD: " + token);
          }

          // If the current token is an operator, the code block adds it to the tokens list with an
          // OPERATOR token type.
          else if (OPERATORS.contains(token.charAt(0))) {
            tokens.add("OPERATOR: " + token);
          }

          // If the current token is a special symbol, the code block adds it to the tokens list
          // with a SPECIAL_SYMBOL token type.
          else if (Special_Symbols.contains(token.charAt(0))) {
            tokens.add("SPECIAL_SYMBOL: " + token);
          }

          // If the current token is a numeric constant, the code block adds it to the tokens list
          // with a CONSTANT token type.
          else if (isNumeric(token)) {
            tokens.add("CONSTANT: " + token);

            // If the current token is not a keyword, operator, special symbol, or numeric constant,
            // the code block adds it to the tokens list with an IDENTIFIER token type.
          } else if (!token
              .trim()
              .isEmpty()) { // skip over whitespace and other non-token characters
            tokens.add("IDENTIFIER: " + token);
          }
        }
      }

      // The for loop iterates over the tokens list and prints each token on a new line.
      for (String token : tokens) {
        System.out.println(token);
      }

      // Write tokens to file
      try (PrintWriter writer = new PrintWriter("tokens.txt")) {
        for (String token : tokens) {
          writer.println(token);
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // The isNumeric method checks if a string is a numeric constant by attempting to parse it as a
  // double.
  private static boolean isNumeric(String str) {
    try {
      Double.parseDouble(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
