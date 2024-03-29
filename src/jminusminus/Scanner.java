// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Hashtable;

import static jminusminus.TokenKind.*;

/**
 * A lexical analyzer for j--, that has no backtracking mechanism.
 */
class Scanner {
    
    // End of file character.
    public final static char EOFCH = CharReader.EOFCH;

    // Keywords in j--.
    private Hashtable<String, TokenKind> reserved;

    // Source characters.
    private CharReader input;

    // Next unscanned character.
    private char ch;

    // Whether a scanner error has been found.
    private boolean isInError;

    // Source file name.
    private String fileName;

    // Line number of current token.
    private int line;

    /**
     * Constructs a Scanner from a file name.
     *
     * @param fileName name of the source file.
     * @throws FileNotFoundException when the named file cannot be found.
     */
    public Scanner(String fileName) throws FileNotFoundException {
        this.input = new CharReader(fileName);
        this.fileName = fileName;
        isInError = false;
        
        // Keywords in j--
        reserved = new Hashtable<String, TokenKind>();
        reserved.put(ABSTRACT.image(), ABSTRACT);
        reserved.put(ASSERT.image(), ASSERT);
        reserved.put(BOOLEAN.image(), BOOLEAN);
        reserved.put(BREAK.image(), BREAK);
        reserved.put(BYTE.image(), BYTE);
        reserved.put(CASE.image(), CASE);
        reserved.put(CATCH.image(), CATCH);
        reserved.put(CHAR.image(), CHAR);
        reserved.put(CLASS.image(), CLASS);
        reserved.put(CONST.image(), CONST);
        reserved.put(CONTINUE.image(), CONTINUE);
        reserved.put(DEFAULT.image(), DEFAULT);
        reserved.put(DO.image(), DO);
        reserved.put(DOUBLE.image(), DOUBLE);
        reserved.put(ELSE.image(), ELSE);
        reserved.put(ENUM.image(), ENUM);
        reserved.put(EXTENDS.image(), EXTENDS);
        reserved.put(FINAL.image(),FINAL);
        reserved.put(FINALLY.image(),FINALLY);
        reserved.put(FLOAT.image(), FLOAT);
        reserved.put(FOR.image(), FOR);
        //reserved.put(FALSE.image(), FALSE); Handling false separately since it's not case sensitive.
        reserved.put(IF.image(), IF);
        reserved.put(GOTO.image(), GOTO);
        reserved.put(IMPLEMENTS.image(), IMPLEMENTS);
        reserved.put(IMPORT.image(), IMPORT);
        reserved.put(INSTANCEOF.image(), INSTANCEOF);
        reserved.put(INT.image(), INT);
        reserved.put(INTERFACE.image(), INTERFACE);
        reserved.put(LONG.image(), LONG);
        reserved.put(NATIVE.image(), NATIVE);
        reserved.put(NEW.image(), NEW);
        // reserved.put(NULL.image(), NULL); Handling null separately since it's not case sensitive
        reserved.put(PACKAGE.image(), PACKAGE);
        reserved.put(PRIVATE.image(), PRIVATE);
        reserved.put(PROTECTED.image(), PROTECTED);
        reserved.put(PUBLIC.image(), PUBLIC);
        reserved.put(RETURN.image(), RETURN);
        reserved.put(SHORT.image(), SHORT);
        reserved.put(STATIC.image(), STATIC);
        reserved.put(STRICTFP.image(), STRICTFP);
        reserved.put(SUPER.image(), SUPER);
        reserved.put(SWITCH.image(), SWITCH);
        reserved.put(THIS.image(), THIS);
        // Scanners Added
        reserved.put(TRY.image(), TRY);
        reserved.put(CATCH.image(),CATCH);
        reserved.put(FINALLY.image(),FINALLY);
        // reserved.put(TRUE.image(), TRUE); Handling true separately since it's not case sensitive.
        reserved.put(UNDERSCORE.image(), UNDERSCORE);
        reserved.put(UNTIL.image(), UNTIL);
        reserved.put(VOID.image(), VOID);
        reserved.put(WHILE.image(), WHILE);
        reserved.put(THROW.image(), THROW);
        reserved.put(THROWS.image(), THROWS);
        // Prime the pump.
        nextCh();
    }

    /**
     * Scans and returns the next token from input.
     *
     * @return the next scanned token.
     */
    public TokenInfo getNextToken() {
        StringBuffer buffer;
        boolean moreWhiteSpace = true;
        boolean scientificNotation = false;
        boolean floatingPoint = false;
        String[] suffixes = {"l","f","d"};
        int multilineCount = 0;
        while (moreWhiteSpace) {
            while (isWhitespace(ch)) {
                nextCh();
            }
            if (ch == '/') {
                nextCh();
                if (ch == '/') {
                    // CharReader maps all new lines to '\n'.
                    while (ch != '\n' && ch != EOFCH) {
                        nextCh();
                    }
                // Checks for Multiline comments to ignore (Cody Dukes)
                } else if (ch == '*') {
                	// Multi-line comment started.
                	multilineCount++;
                	nextCh();
                	while (multilineCount > 0) {
                        if (ch == '/') {
                            nextCh();
                            if (ch == '*') {
                                nextCh();
                                multilineCount++;
                            }
                        }
                		if (ch == '*') {
                			nextCh();
                			if (ch == '/') {
                				multilineCount--;
                			}
                		}
                        if (ch == EOFCH) {
                            reportScannerError("Reached end of file before closing a multiline comment.");
                            break;
                        }
                		nextCh();
                	}
                } else {
                    reportScannerError("Operator / is not supported in j--");
                }
            } else {
                moreWhiteSpace = false;
            }
        }
        line = input.line();
        switch (ch) {
            case ',':
                nextCh();
                return new TokenInfo(COMMA, line);
            case '.':// Checks for float/double literals starting with . as well as . and ... separators (Tanner Denson)
                nextCh();
                if(!isDigit(ch)) {
                    if(ch == '.') {
                        nextCh();
                        if(ch == '.') {
                            nextCh();
                            return new TokenInfo(VAR_ARGS, line);
                        }
                        else {
                            reportScannerError("Unidentified input token: '..'");
                            getNextToken();
                        }
                    } else {
                        return new TokenInfo(DOT, line);
                    }
                } else {
                    buffer = new StringBuffer();
                    buffer.append('.');
                    buffer.append(ch);
                    nextCh();
                    scientificNotation = false;
                    while (isDigit(ch) || ch == 'e' || ch == 'E' || ch == '_') {
                        if(ch == 'e' || ch == 'E') {
                            char previousChar = buffer.charAt(buffer.length() - 1);
                            if(previousChar == '_') {
                                reportScannerError("Underscores must be within digits.");
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                            else if(scientificNotation) {
                                reportScannerError("Cannot have two 'e'/'E' in literal.");
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            } else {
                                buffer.append(ch);
                                nextCh();
                                scientificNotation = true;
                                if(ch == '+' || ch == '-') {
                                    buffer.append(ch);
                                    nextCh();
                                }
                            }
                        }
                        else if(ch == '_') {
                            char previousChar = buffer.charAt(buffer.length() - 1);
                            if(previousChar == 'e' || previousChar == 'E') {
                                reportScannerError("Underscores must be within digits.");
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                        }
                        buffer.append(ch);
                        nextCh();
                }
                if(ch == 'f' || ch == 'F') {
                    buffer.append(ch);
                    nextCh();
                    return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
                } else if (ch == 'l' || ch == 'L') { //Jarvis K
                    buffer.append(ch);
                    nextCh();
                    return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                } else {
                    if(ch == 'd' || ch == 'D') {
                        buffer.append(ch);
                        nextCh();
                    }
                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                }
                }
            case '[':
                nextCh();
                return new TokenInfo(LBRACK, line);
            case '{':
                nextCh();
                return new TokenInfo(LCURLY, line);
            case '(':
                nextCh();
                return new TokenInfo(LPAREN, line);
            case ']':
                nextCh();
                return new TokenInfo(RBRACK, line);
            case '}':
                nextCh();
                return new TokenInfo(RCURLY, line);
            case ')':
                nextCh();
                return new TokenInfo(RPAREN, line);
            case ';':
                nextCh();
                return new TokenInfo(SEMI, line);
            case '*':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(STAR_ASSIGN, line);
                }
                return new TokenInfo(STAR, line);
            case '/':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(DIV_ASSIGN, line);
                }
                return new TokenInfo(DIV, line);
            case '+':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(PLUS_ASSIGN, line);
                } else if (ch == '+') {
                    nextCh();
                    return new TokenInfo(INC, line);
                } else {
                    return new TokenInfo(PLUS, line);
                }
            case '-':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(MINUS_ASSIGN, line);
                } else if (ch == '-') {
                    nextCh();
                    return new TokenInfo(DEC, line);
                } else if (ch == '>') {
                    nextCh();
                    return new TokenInfo(LAMBDA, line);
                } else {
                    return new TokenInfo(MINUS, line);
                }
            case '%':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(MOD_ASSIGN, line);
                } else {
                    return new TokenInfo(MOD, line);
                }
            case '=':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(EQUAL, line);
                } else {
                    return new TokenInfo(ASSIGN, line);
                }
            case '>':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(GE, line);
                }
                else if (ch == '>') {
                    nextCh();
                    if (ch == '=') {
                        nextCh();
                        return new TokenInfo(RS_ASSIGN, line);
                    }
                    else if (ch == '>') {
                        nextCh();
                        if(ch == '=') {
                            nextCh();
                            return new TokenInfo(RS_UNSIGNED_ASSIGN, line);
                        }
                        else {
                            return new TokenInfo(RS_UNSIGNED, line);
                        }
                    }
                    return new TokenInfo(RS, line);
                }
                return new TokenInfo(GT, line);
            case '<':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(LE, line);
                } 
                else if(ch == '<') {
                    nextCh();
                    if(ch == '=') {
                        nextCh();
                        return new TokenInfo(LS_ASSIGN, line);
                    }
                    else {
                        return new TokenInfo(LS, line);
                    }
                }
                else {
                    /* reportScannerError("Operator < is not supported in j--");
                    return getNextToken();*/
                    return new TokenInfo(LT, line);
                }
            case '!':
                nextCh();
                if(ch == '=') {
                    nextCh();
                    return new TokenInfo(NOT_EQUAL, line);
                } else {
                    return new TokenInfo(LNOT, line);
                }
            case '&':
                nextCh();
                if (ch == '&') {
                    nextCh();
                    return new TokenInfo(LAND, line);
                } 
                else if (ch == '=') {
                    nextCh();
                    return new TokenInfo(AND_ASSIGN, line);
                } else {
                    /*reportScannerError("Operator & is not supported in j--");
                    return getNextToken();*/
                    return new TokenInfo(AND, line);
                }
            case '|':
                nextCh();
                if (ch == '|') {
                    nextCh();
                    return new TokenInfo(LOR, line);
                } 
                else if (ch == '=') {
                    nextCh();
                    return new TokenInfo(OR_ASSIGN, line);
                } else {
                    return new TokenInfo(OR, line);
                }
            case '^':
                nextCh();
                if (ch == '=') {
                    nextCh();
                    return new TokenInfo(XOR_ASSIGN, line);
                } else {
                    return new TokenInfo(XOR, line);
                }
            case '@':
                nextCh();
                return new TokenInfo(AT, line);

            case ':':
                nextCh();
                if(ch == ':') {
                    nextCh();
                    return new TokenInfo(METHOD_REFERENCE, line);
                } else {
                    return new TokenInfo(COLON, line);
                }
            case '~':
                nextCh();
                return new TokenInfo(COMPLEMENT, line);
            case '?':
                nextCh();
                return new TokenInfo(QUESTION, line);
            case '\'':
                buffer = new StringBuffer();
                buffer.append('\'');
                nextCh();
                if (ch == '\\') {
                    nextCh();
                    buffer.append(escape());
                } else {
                    buffer.append(ch);
                    nextCh();
                }
                if (ch == '\'') {
                    buffer.append('\'');
                    nextCh();
                    return new TokenInfo(CHAR_LITERAL, buffer.toString(), line);
                } else {
                    // Expected a ' ; report error and try to recover.
                    reportScannerError(ch + " found by scanner where closing ' was expected");
                    while (ch != '\'' && ch != ';' && ch != '\n') {
                        nextCh();
                    }
                    return new TokenInfo(CHAR_LITERAL, buffer.toString(), line);
                }
            case '"':
                buffer = new StringBuffer();
                buffer.append("\"");
                nextCh();
                while (ch != '"' && ch != '\n' && ch != EOFCH) {
                    if (ch == '\\') {
                        nextCh();
                        buffer.append(escape());
                    } else {
                        buffer.append(ch);
                        nextCh();
                    }
                }
                if (ch == '\n') {
                    reportScannerError("Unexpected end of line found in string");
                } else if (ch == EOFCH) {
                    reportScannerError("Unexpected end of file found in string");
                } else {
                    // Scan the closing "
                    nextCh();
                    buffer.append("\"");
                }
                return new TokenInfo(STRING_LITERAL, buffer.toString(), line);
            case EOFCH:
                return new TokenInfo(EOF, line);
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':// Added scanning for number literals not starting with 0 (Tanner Denson)
                floatingPoint = false;
                scientificNotation = false;
                buffer = new StringBuffer();
                buffer.append(ch);
                nextCh();
                while (isDigit(ch) || ch == '_' || ch == '.' || ch == 'e' || ch == 'E') {
                    if(ch == 'e' || ch == 'E') {
                        char previousChar = buffer.charAt(buffer.length() - 1);
                        if(previousChar == '_') {
                            reportScannerError("Underscores must be within digits.");
                            if(floatingPoint) {
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            } else {
                                return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                            }
                        }
                        else if(scientificNotation) {
                            reportScannerError("Cannot have two 'e'/'E' in literal.");
                            return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                        } else {
                            buffer.append(ch);
                            nextCh();
                            scientificNotation = true;
                            if(ch == '+' || ch == '-') {
                                buffer.append(ch);
                                nextCh();
                            }
                        }
                    }
                    else if(ch == '.') {
                        char previousChar = buffer.charAt(buffer.length() - 1);
                        if(previousChar == '_' || previousChar == '.' || previousChar == 'e' || previousChar == 'E') {
                            reportScannerError("Cannot have a '.' next to an '_' or '.' or after 'e'/'E'.");
                            if(floatingPoint || scientificNotation) {
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                            return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                        }
                        buffer.append(ch);
                        nextCh();
                        floatingPoint = true;
                    }
                    else if(ch == '_') {
                        char previousChar = buffer.charAt(buffer.length() - 1);
                        if(!isDigit(ch)) {
                            reportScannerError("Cannot have an '_' unless next to digits.");
                            return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                        }
                        buffer.append(ch);
                        nextCh();
                    }
                    buffer.append(ch);
                    nextCh();
                }

                if(buffer.charAt(buffer.length()-1) == '_') {
                    reportScannerError("Cannot have an '_' at end of literal or before suffix");
                }
                for (String suffix : suffixes) {
                    if (suffix.equalsIgnoreCase(Character.toString(ch))) {
                        switch(suffix) {
                            case "l":
                                if(!floatingPoint) {
                                    buffer.append(ch);
                                    nextCh();
                                    return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                                } else {
                                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                                }
                            case "f":
                                buffer.append(ch);
                                nextCh();
                                return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
                            case "d":
                                buffer.append(ch);
                                nextCh();
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                        }
                    }
                }

                if(floatingPoint) {// There was an '.' in the literal and no suffix so defaults to double
                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                } else {
                    return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                }
            case '0':
            /* Notes: Hexadecimal scientific notation uses p/P instead of e/E
             * literals cannot start or end with _ and _ cannot be next to anything but digits
             * There is an optional sign +/- before the exponent in scientific notation
             * Exponent part in scientific notation must be integer (no decimal point)
             * For extra credit: Need binary (0b) octal (0) and hex (0x) these along with suffixes are not case sensitive
             * Scientific notation literals will be doubles unless it has the float suffix
             */

                // Cody Dukes & Jarvis Kampe
                buffer = new StringBuffer();
                buffer.append(ch);
                nextCh();
                // Hexadecimal
                if (ch == 'x' || ch == 'X') {
                    scientificNotation = false;
                    buffer.append(ch);
                    nextCh();
                    floatingPoint = false;
                    while (isDigit(ch) || (ch >= 65 && ch <= 70) || (ch >= 97 && ch <= 102) || ch == 'p' || ch == 'P' || ch == '_' || ch == '.') {
                        if(ch == '_') {
                            char previousChar = buffer.charAt(buffer.length() - 1);
                            if(!(isDigit(ch) || (ch >= 65 && ch <= 70) || (ch >= 97 && ch <= 102))) { // If not a hex digit...
                                reportScannerError("Cannot have an '_' unless next to digits.");
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                            buffer.append(ch);
                            nextCh();
                        } else if(ch == 'p' || ch == 'P') {
                            char previousChar = buffer.charAt(buffer.length() - 1);
                            if(previousChar == '_') {
                                reportScannerError("Cannot have an '_' unless next to digits.");
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                            scientificNotation = true;
                            buffer.append(ch);
                            nextCh();
                            if(ch == '+' || ch == '-') {
                                buffer.append(ch);
                                nextCh();
                            }
                        } else if(ch == '.') {
                            char previousChar = buffer.charAt(buffer.length() - 1);
                            if(ch == '_') {
                                reportScannerError("Cannot have an underscore besides anything but digits.");
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                            if (floatingPoint || scientificNotation) {
                                reportScannerError("Cannot have a second '.' in literal or in exponent of scientific notation.");
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            } else {
                                buffer.append(ch);
                                nextCh();
                                floatingPoint = true;
                            }
                        } else {
                            if(scientificNotation) {
                                if(isDigit(ch)) {
                                    buffer.append(ch);
                                    nextCh();
                                } else {
                                    if(ch == 'f' || ch == 'F') {
                                        buffer.append(ch);
                                        nextCh();
                                        return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
                                    } else if(ch == 'd' || ch == 'D') {
                                        buffer.append(ch);
                                        nextCh();
                                    } else {
                                        return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                                    }
                                }
                            } else {
                                buffer.append(ch);
                                nextCh();
                            }
                        }
                    }
                    for (String suffix : suffixes) {
                        if (suffix.equalsIgnoreCase(Character.toString(ch))) {
                            switch(suffix) {
                                case "l":
                                    buffer.append(ch);
                                    nextCh();
                                    return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                                case "f":
                                    buffer.append(ch);
                                    nextCh();
                                    return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
                                case "d":
                                    buffer.append(ch);
                                    nextCh();
                                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                        }
                    }
                    return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                    // Binary
                } else if (ch == 'b' || ch == 'B') {
                    buffer.append(ch);
                    nextCh();
                    while (ch == '0' || ch == '1' || ch == '_') {
                        if(ch == '_') {
                            char previousChar = buffer.charAt(buffer.length() - 1);
                            if(previousChar == 'b') {
                                reportScannerError("Underscore must be between digits.");
                                return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                            } else {
                                buffer.append(ch);
                                nextCh();
                            }
                        } else {
                            buffer.append(ch);
                            nextCh();
                        }
                    }
                    if(ch == 'l' || ch == 'L') {
                        buffer.append(ch);
                        nextCh();
                        return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                    }
                    return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                    // Octal and normal numbers with leading zeros
                } else if ((ch >= 48 && ch <= 55) || ch == '_') {
                    buffer.append(ch);
                    nextCh();
                    while (ch >= 48 && ch <= 55) {
                        buffer.append(ch);
                        nextCh();
                    }
                    scientificNotation = false;
                    floatingPoint = false;
                    // numbers starting with 0 that aren't in octal
                    while(isDigit(ch) || ch == 'e' || ch == 'E' || ch == '.' ) {
                        if(ch == '.') {
                            if(scientificNotation || floatingPoint) {
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            } else {
                                buffer.append(ch);
                                nextCh();
                                floatingPoint = true;
                            }
                        }
                        if(ch == 'e' || ch == 'E') {
                            if(scientificNotation) {
                                return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                            scientificNotation = true;
                            buffer.append(ch);
                            nextCh();
                        } else {
                            buffer.append(ch);
                            nextCh();
                        }
                    }
                    for (String suffix : suffixes) {
                        if (suffix.equalsIgnoreCase(Character.toString(ch))) {
                            switch(suffix) {
                                case "l":
                                    if(!(floatingPoint || scientificNotation)) {
                                        buffer.append(ch);
                                        nextCh();
                                        return new TokenInfo(LONG_LITERAL, buffer.toString(), line);
                                    } else {
                                        return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                                    }
                                case "f":
                                    buffer.append(ch);
                                    nextCh();
                                    return new TokenInfo(FLOAT_LITERAL, buffer.toString(), line);
                                case "d":
                                    buffer.append(ch);
                                    nextCh();
                                    return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                            }
                        }
                    }
                    if(scientificNotation || floatingPoint) {
                        return new TokenInfo(DOUBLE_LITERAL, buffer.toString(), line);
                    } else {
                        return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                    }
                }
                else {
                    return new TokenInfo(INT_LITERAL, buffer.toString(), line);
                }
            default:
                if (isIdentifierStart(ch)) {
                    buffer = new StringBuffer();
                    while (isIdentifierPart(ch)) {
                        buffer.append(ch);
                        nextCh();
                    }
                    String identifier = buffer.toString();
                    if (reserved.containsKey(identifier)) {
                        return new TokenInfo(reserved.get(identifier), line);
                    } else { // Moved false,true,null here instead of in the hashtable since they're not case sensitive to make it easier (Tanner Denson)
                        if(identifier.toLowerCase().equals("false")) {
                            return new TokenInfo(FALSE, line);
                        }

                        if(identifier.toLowerCase().equals("true")) {
                            return new TokenInfo(TRUE, line);
                        }

                        if(identifier.toLowerCase().equals("null")) {
                            return new TokenInfo(NULL, line);
                        }

                        return new TokenInfo(IDENTIFIER, identifier, line);
                    }
                } else {
                    reportScannerError("Unidentified input token: '%c'", ch);
                    nextCh();
                    return getNextToken();
                }
        }
    }

    /**
     * Returns true if an error has occurred, and false otherwise.
     *
     * @return true if an error has occurred, and false otherwise.
     */
    public boolean errorHasOccurred() {
        return isInError;
        
    }

    /**
     * Returns the name of the source file.
     *
     * @return the name of the source file.
     */
    public String fileName() {
        return fileName;
    }

    // Scans and returns an escaped character.
    private String escape() {
        switch (ch) {
            case 'b':
                nextCh();
                return "\\b";
            case 't':
                nextCh();
                return "\\t";
            case 'n':
                nextCh();
                return "\\n";
            case 'f':
                nextCh();
                return "\\f";
            case 'r':
                nextCh();
                return "\\r";
            case '"':
                nextCh();
                return "\\\"";
            case '\'':
                nextCh();
                return "\\'";
            case '\\':
                nextCh();
                return "\\\\";
            default:
                reportScannerError("Badly formed escape: \\%c", ch);
                nextCh();
                return "";
        }
    }

    // Advances ch to the next character from input, and updates the line number.
    private void nextCh() {
        line = input.line();
        try {
            ch = input.nextChar();
        } catch (Exception e) {
            reportScannerError("Unable to read characters from input");
        }
    }

    // Reports a lexical error and records the fact that an error has occurred. This fact can be
    // ascertained from the Scanner by sending it an errorHasOccurred message.
    private void reportScannerError(String message, Object... args) {
        isInError = true;
        System.err.printf("%s:%d: error: ", fileName, line);
        System.err.printf(message, args);
        System.err.println();
    }

    // Returns true if the specified character is a digit (0-9), and false otherwise.
    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    // Returns true if the specified character is a whitespace, and false otherwise.
    private boolean isWhitespace(char c) {
        return (c == ' ' || c == '\t' || c == '\n' || c == '\f');
    }

    // Returns true if the specified character can start an identifier name, and false otherwise.
    private boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '$');
    }

    // Returns true if the specified character can be part of an identifier name, and false
    // otherwise.
    private boolean isIdentifierPart(char c) {
        return (isIdentifierStart(c) || isDigit(c));
    }
}

/**
 * A buffered character reader, which abstracts out differences between platforms, mapping all new
 * lines to '\n', and also keeps track of line numbers.
 */
class CharReader {
    // Representation of the end of file as a character.
    public final static char EOFCH = (char) -1;

    // The underlying reader records line numbers.
    private LineNumberReader lineNumberReader;

    // Name of the file that is being read.
    private String fileName;

    /**
     * Constructs a CharReader from a file name.
     *
     * @param fileName the name of the input file.
     * @throws FileNotFoundException if the file is not found.
     */
    public CharReader(String fileName) throws FileNotFoundException {
        lineNumberReader = new LineNumberReader(new FileReader(fileName));
        this.fileName = fileName;
    }

    /**
     * Scans and returns the next character.
     *
     * @return the character scanned.
     * @throws IOException if an I/O error occurs.
     */
    public char nextChar() throws IOException {
        return (char) lineNumberReader.read();
    }

    /**
     * Returns the current line number in the source file.
     *
     * @return the current line number in the source file.
     */
    public int line() {
        return lineNumberReader.getLineNumber() + 1; // LineNumberReader counts lines from 0
    }

    /**
     * Returns the file name.
     *
     * @return the file name.
     */
    public String fileName() {
        return fileName;
    }

    /**
     * Closes the file.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        lineNumberReader.close();
    }
}
