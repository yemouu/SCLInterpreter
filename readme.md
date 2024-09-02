# SCL Interpreter
This is the group project for Kennesaw State University's 'CS 4308 - Concepts of Programming Languages' course.
For this project we were tasked with writing a lexer, parser, and interpreter for fictional(?) language called SCL.
Our interpreter didn't need to interpret the entirety of this fictional language, just the sample file of our [choice](examples/bitops1.scl).

## Building and Executing
If you are using NixOS, you can run `nix build`.
This will automatically get the dependencies needed to build this project as well as build the project itself.
Alternatively, you can run `nix run .# -- <filename>` to build and run this project in the same command.

Otherwise, you will need the following dependencies:
  - jdk17+
  - maven

To build, use maven:
```sh
mvn package
```

To run, excute:
```sh
java -jar target/scli-1.0.0.jar <filename>
```
