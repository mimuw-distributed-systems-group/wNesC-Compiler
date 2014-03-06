package pl.edu.mimuw.nesc.lexer;

import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

/**
 * Maps the code of token to the name of token. Useful for debugging input
 * stream of tokens.
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public final class TokenPrinter {

	public TokenPrinter() {
	}

	public void print(int token, Object data) {
		System.out.print(" " + getPrintName(token, data) + " ");
	}

	private String getPrintName(int token, Object data) {

		switch (token) {
		case IDENTIFIER:
			return "IDENTIFIER <<" + data + ">>";
		case COMPONENTREF:
			return "COMPONENTREF <<" + data + ">>";
		case TYPEDEF_NAME:
			return "TYPENAME <<" + data + ">>";

		case CHARACTER_LITERAL:
			return "<CHAR>";
		case INTEGER_LITERAL:
			return "<INT>";
		case STRING_LITERAL:
			return "<STRING <<" + data + ">>>";

		case QUESTION:
			return "?";
		case ARROW:
			return "->";
		case LEFT_ARROW:
			return "<-";
		case AT:
			return "@";

		case STAR:
			return "*";
		case DIV:
			return "/";
		case MOD:
			return "%";
		case PLUS:
			return "+";
		case MINUS:
			return "-";

		case EQ:
			return "=";

		case COLON:
			return ":";
		case COMMA:
			return ",";
		case DOT:
			return ".";
		case SEMICOLON:
			return ";";

		case LBRACE:
			return "{";
		case RBRACE:
			return "}";
		case LBRACK:
			return "[";
		case RBRACK:
			return "]";
		case LPAREN:
			return "(";
		case RPAREN:
			return ")";

			/* A */
		case AS:
			return "AS";
		case ASM_KEYWORD:
			return "__asm__";
		case ATOMIC:
			return "ATOMIC";
		case ATTRIBUTE:
			return "__ATTRIBUTE__";

			/* B */

			/* C */
		case CALL:
			return "CALL";
		case CHAR:
			return "CHAR";
		case COMMAND:
			return "COMMAND";
		case COMPONENTS:
			return "COMPONENTS";
		case CONFIGURATION:
			return "CONFIGURATION";
		case CONST:
			return "CONST";

			/* D */
		case DOUBLE:
			return "DOUBLE";

			/* E */
		case ELSE:
			return "ELSE";
		case ENUM:
			return "ENUM";
		case EXTERN:
			return "EXTERN";

			/* F */
		case FLOAT:
			return "FLOAT";

			/* G */
			/* H */
			/* I */
		case IF:
			return "IF";
		case IMPLEMENTATION:
			return "IMPLEMENTATION";
		case INLINE:
			return "INLINE";
		case INT:
			return "INT";
		case INTERFACE:
			return "INTERFACE";

			/* J */
			/* K */
			/* L */
		case LONG:
			return "LONG";

			/* M */
		case MODULE:
			return "MODULE";

			/* N */
		case NORACE:
			return "NORACE";
			/* O */
			/* P */
		case PROVIDES:
			return "PROVIDES";

			/* R */
		case RETURN:
			return "RETURN";

			/* S */
		case SIGNAL:
			return "SIGNAL";
		case SIZEOF:
			return "SIZEOF";
		case SHORT:
			return "SHORT";
		case STATIC:
			return "STATIC";
		case STRUCT:
			return "STRUCT";

			/* T */
		case TYPEDEF:
			return "TYPEDEF";

			/* U */
		case USES:
			return "USES";

			/* V */
		case VOLATILE:
			return "VOLATILE";
		case VOID:
			return "VOID";

		case WHILE:
			return "WHILE";

		default:
			return String.valueOf(token);
		}
	}

}
