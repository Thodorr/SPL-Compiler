package de.thm.mni.compilerbau.phases._01_scanner;

import de.thm.mni.compilerbau.utils.SplError;
import de.thm.mni.compilerbau.phases._02_03_parser.Sym;
import de.thm.mni.compilerbau.absyn.Position;
import de.thm.mni.compilerbau.table.Identifier;
import de.thm.mni.compilerbau.CommandLineOptions;
import java_cup.runtime.*;

%%

%class Scanner
%public
%line
%column
%cup
%eofval{
    return new java_cup.runtime.Symbol(Sym.EOF, yyline + 1, yycolumn + 1);   //This needs to be specified when using a custom sym class name
%eofval}

%{
    public CommandLineOptions options = null;

    private Symbol symbol(int type) {
      return new Symbol(type, yyline + 1, yycolumn + 1);
    }

    private Symbol symbol(int type, Object value) {
      return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }
%}

InputCharacter = [^\r\n]
DecIntLiteral = [0-9]*
HexIntLiteral = 0x[0-9a-fA-F]+
EscapedCharacter = "n"
CharLiteral = "'" ({InputCharacter} | ("\\" {EscapedCharacter}) | "\\") "'"

%%

// Regular expressions for tokens
<YYINITIAL> {

    "//"(.)* { /* Do nothing */ }
    [\t\n\r ]+ { /* Do nothing */ }

    "else" { return symbol(Sym.ELSE); }
    "if" { return symbol(Sym.IF); }
    "of" { return symbol(Sym.OF); }
    "proc" { return symbol(Sym.PROC); }
    "ref" { return symbol(Sym.REF); }
    "type" { return symbol(Sym.TYPE); }
    "var" { return symbol(Sym.VAR); }
    "while" { return symbol(Sym.WHILE); }
    "(" { return symbol(Sym.LPAREN); }
    ")" { return symbol(Sym.RPAREN); }
    "[" { return symbol(Sym.LBRACK); }
    "]" { return symbol(Sym.RBRACK); }
    "{" { return symbol(Sym.LCURL); }
    "}" { return symbol(Sym.RCURL); }
    "=" { return symbol(Sym.EQ); }
    "#" { return symbol(Sym.NE); }
    "<" { return symbol(Sym.LT); }
    "<=" { return symbol(Sym.LE); }
    ">" { return symbol(Sym.GT); }
    ">=" { return symbol(Sym.GE); }
    ":=" { return symbol(Sym.ASGN); }
    ":" { return symbol(Sym.COLON); }
    "," { return symbol(Sym.COMMA); }
    ";" { return symbol(Sym.SEMIC); }
    "+" { return symbol(Sym.PLUS); }
    "-" { return symbol(Sym.MINUS); }
    "*" { return symbol(Sym.STAR); }
    "/" { return symbol(Sym.SLASH); }
    "array" { return symbol(Sym.ARRAY); }
    [_a-zA-Z][_a-zA-Z0-9]* { return symbol(Sym.IDENT, new Identifier(yytext())); }

    {HexIntLiteral} { return symbol(Sym.INTLIT, Integer.parseInt(yytext().substring(2), 16));}

    {DecIntLiteral} { return symbol(Sym.INTLIT, Integer.parseInt(yytext(), 10));}

    {CharLiteral} {
            String literal = yytext().substring(1, yytext().length() - 1);
            char value;
            if (literal.length() == 1) {
                value = literal.charAt(0);
            } else if (literal.charAt(0) == '\\') {
                switch (literal.charAt(1)) {
                    case 'n': value = '\n'; break;
                    case 't': value = '\t'; break;
                    case 'r': value = '\r'; break;
                    case '\\': value = '\\'; break;
                    case '\'': value = '\''; break;
                    default: throw SplError.IllegalCharacter(new Position(yyline + 1, yycolumn + 1), yytext().charAt(1));
                }
            } else {
                throw SplError.IllegalCharacter(new Position(yyline + 1, yycolumn + 1), yytext().charAt(0));
            }
            return symbol(Sym.INTLIT, (int) value);
      }

    <<EOF>> { return symbol(Sym.EOF); }

    . { throw SplError.IllegalCharacter(new Position(yyline + 1, yycolumn + 1), yytext().charAt(0)); }
}
[^]		{throw SplError.IllegalCharacter(new Position(yyline + 1, yycolumn + 1), yytext().charAt(0));}
