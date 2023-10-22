import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Scanner;

// Scanner had to be remade because we could not get the previous version to work and so both
// the new scanner and parser are together in 1 file
public class Parser {

  // This is the Scanner Section of the program
  // The method will take in an empty arraylist of strings and fill it with tokens from the file
  public static int SCLScanner(ArrayList<String> tokens) throws FileNotFoundException {
    int lines = 1;

    // This is wehere the user is prompted to enter the file name
    Scanner userInput = new Scanner(System.in);
    System.out.println("Enter the file name");
    String filename = userInput.nextLine();
    userInput.close();
    File sourceCodeFile = new File(filename);
    Scanner fileScan = new Scanner(sourceCodeFile);

    // This loop reads each line of the file using a Scanner object called fileScan.
    // For each line, it splits the line into an array of strings using the split method with a
    // space as the delimiter.
    // It then iterates over the array of strings and adds each non-empty string to an ArrayList
    // called tokens.
    // The loop also increments a variable called numberOfLines for each line that is read from the
    // file. Finally, the loop returns the total number of lines in the file as an integer value.
    while (fileScan.hasNext()) {

      String thisLine = fileScan.nextLine();

      // creae an array of strings that is split up by every space " "
      String[] lineArray = thisLine.split(" ");

      // for the length of the current line, we are adding into the parameter tokens, the value of
      // token from the given file
      for (int i = 0; i < lineArray.length; i++) {

        // the if statement stops whitespace from being counted as a token
        if (!lineArray[i].equals("")) {

          // adding into tokens the value of the current token in file
          tokens.add(lineArray[i]);
        }
      }
      // number of lines is incremented and eventually returned to ensure main method is iterating
      // through all lines of the file
      ++lines;
    }
    fileScan.close();
    return lines;
  }

  // This is the first half of the parser, which is the tokenType method.
  // This method takes in a string and returns the type of token it is
  // The types we have are identidier, keyword, operator, and constant
  public static String tokenType(String token) {

    // The method initializes an empty string variable called tok_id. It then creates an ArrayList
    // of String objects called identifiers that contains a list of valid identifier names in the
    // SCL language.
    String tok_id = "";
    ArrayList<String> identifiers =
        new ArrayList<String>(
            Arrays.asList(
                "vara", "varb", "varc1", "varc2", "varc3", "varc4", "d1", "d2", "a", "b", "c"));

    // The method then creates a ListIterator object called identifiersIterator that iterates over
    // the identifiers ArrayList.
    // Giving whatever token it is the identifier token type
    ListIterator identifiersIterator = identifiers.listIterator();
    while (identifiersIterator.hasNext()) {
      if (identifiersIterator.next().equals(token)) {
        tok_id = "Identfier";
      }
    }

    // The method then creates an ArrayList of String objects called keywords that contains a list
    // of valid keywords in the SCL language.
    ArrayList<String> keywords =
        new ArrayList<String>(
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
                "main"));

    // The method then creates a ListIterator object called keywordsIterator that iterates over the
    // keywords ArrayList.
    // If it finds a token that fits in the keywords arraylist, it will give it the keyword token
    // type
    ListIterator<String> keywordsIterator = keywords.listIterator();
    while (keywordsIterator.hasNext()) {
      if (keywordsIterator.next().equals(token)) {
        tok_id = "Keyword";
      }
    }

    // The method then creates an ArrayList of String objects called operators that contains a list
    // of valid operators in the SCL language.
    ArrayList<String> operators =
        new ArrayList<String>(
            Arrays.asList(
                "+", "-", "*", "/", "=", "%", "<", ">", "<=", ">=", "==", "!=", "(", ")", "++",
                "--", "!", "&&", "||", "+=", "-=", "*=", "/=", "%="));
    // The method then creates a ListIterator object called operatorIterator that iterates over the
    // operators ArrayList.
    ListIterator<String> operatorIterator = operators.listIterator();
    while (operatorIterator.hasNext()) {
      if
      // Switch case that will give the token the operator of the specific token type
      (operatorIterator.next().equals(token)) {
        switch (token) {
          case "+":
            tok_id = "Plus operator";
            break;
          case "-":
            tok_id = "Minus operator";
            break;
          case "*":
            tok_id = "Times operator";
            break;
          case "/":
            tok_id = "Divide operator";
            break;
          case "%":
            tok_id = "Modulo operator";
            break;
          case "<":
            tok_id = "Less than operator";
            break;
          case ">":
            tok_id = "Greater than operator";
            break;
          case "<=":
            tok_id = "Less than operator";
            break;
          case ">=":
            tok_id = "Greater than or equal to operator";
            break;
          case "=":
            tok_id = "Assignment operator";
            break;
          case "==":
            tok_id = "Equal operator";
            break;
          case "!=":
            tok_id = "Nnot equal operator";
            break;
          case "(":
            tok_id = "Left parenthesis operator";
            break;
          case ")":
            tok_id = "Right parenthesis operator";
            break;
          case "++":
            tok_id = "increment operator";
            break;
          case "--":
            tok_id = "Decrement operator";
            break;
          case "!":
            tok_id = "Not operator";
            break;
          case "&&":
            tok_id = "And operator";
            break;
          case "||":
            tok_id = "or operator";
            break;

          default:
            tok_id = "Unknown operator";
            break;
        }
      }
    }

    // The method then creates a for loop that iterates over each character in the token string.
    // If the character is a digit, the method gives the token the constant token type.
    for (char myChar : token.toCharArray()) {
      if (Character.isDigit(myChar)) {
        tok_id = "Constant";
      }
    }
    // returns the token type
    return tok_id;
  }

  // Main method and second half of parser, which is listing the import, description, global
  // declarations, function, and exit tokens sections
  public static void main(String[] args) throws FileNotFoundException {
    // The array list where all of our generated token will be stored
    ArrayList<String> tokens = new ArrayList<String>();

    // Passes the empty tokens arraylist through the SCLScanner method
    SCLScanner(tokens);

    // Once all the inputs have been tokenized and stored in the tokens arraylist, the main method
    // iterates over the tokens arraylist and prints what Statement parses what tokens
    for (int i = 0; i < tokens.size(); i++) {
      // This is for all the import statement tokens
      if (i == 0) {
        System.out.println("Import Token Found: Parsing Import Statement...");
        System.out.print("Token," + " " + (i + 1) + ": ");
        System.out.print(tokens.get(i + 1));
        System.out.println("\t" + tokenType((String) tokens.get(i + 1)) + "");
      }
      // This is for all the description statement tokens
      else if (i == 2) {
        System.out.println("Description Token Found: Parsing Description...");
        for (int j = 3; j < 9; j++) {
          System.out.print("Token," + " " + (j) + ": ");
          System.out.print(tokens.get(j));
          System.out.println("\t" + tokenType((String) tokens.get(j)) + "");
        }
      }
      // This is for all the global declarations statement tokens
      else if (i == 9) {
        System.out.println("Global Declarations tokens found: Parsing Global Declarations...");
        for (int j = 10; j < 32; j++) {
          System.out.print("Token," + " " + (j) + ": ");
          System.out.print(tokens.get(j));
          System.out.println("\t" + tokenType((String) tokens.get(j)) + "");
        }
      }
      // This is for all the function variable statement tokens
      else if (i == 33) {
        System.out.println("Function variable tokens found: Parsing Function...");
        for (int j = 37; j < 77; j++) {
          System.out.print("Token," + " " + (j) + ": ");
          System.out.print(tokens.get(j));
          System.out.println("\t" + tokenType((String) tokens.get(j)) + "");
        }
      }
      // This is for all the function begin statement tokens
      else if (i == 77) {
        System.out.println("Function begin token found: Parsing Function...");
        for (int j = 78; j < 208; j++) {
          System.out.print("Token," + " " + (j) + ": ");
          System.out.print(tokens.get(j));
          System.out.println("\t" + tokenType((String) tokens.get(j)) + "");
        }
      }

      // Once the exit token is found, the parsing will end
      else if (i == 209) {
        System.out.println("Exit token found: Ending parsing...");
      }
      System.out.print("");
    }
  }
}
