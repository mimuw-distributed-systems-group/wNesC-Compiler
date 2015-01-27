package pl.edu.mimuw.nesc.lexer;

import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

import org.anarres.cpp.Token;

import com.google.common.collect.ImmutableMap;

/**
 * Factory of symbols passed from lexer to parser. Translates preprocessor
 * tokens' codes to parser tokens' codes.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 *
 */
final class SymbolFactory {

	public static final int UNKNOWN_TOKEN = -1;

	/**
	 * Translates preprocessor/lexer identifier to either parser's keyword or
	 * identifier token.
	 *
	 * @param keywordOrIdentifier
	 *            keyword or identifier name
	 * @return keyword code or identifier code
	 */
	public static int getSymbolCode(String keywordOrIdentifier) {
		if (KEYWORDS.containsKey(keywordOrIdentifier)) {
			return KEYWORDS.get(keywordOrIdentifier);
		} else {
			return IDENTIFIER;
		}
	}

	/**
	 * Translates preprocessor/lexer token's code to parser token's code. It
	 * handles all kind of tokens except of keywords/identifiers, whitespaces,
	 * literals, comments.
	 *
	 * @param tokenType
	 *            token type
	 * @return symbol code
	 */
	public static int getSymbolCode(int tokenType) {
		final Integer result = DIRECT_EQUIVALENTS.get(tokenType);
		if (result != null) {
			return result;
		}
		return UNKNOWN_TOKEN;
	}

	private static final ImmutableMap<Integer, Integer> DIRECT_EQUIVALENTS;
	private static final ImmutableMap<String, Integer> KEYWORDS;

	static {
		KEYWORDS = ImmutableMap.<String, Integer> builder()
				/*
				 * nesc
				 */
				.put("as", AS)
				.put("abstract", ABSTRACT)
				.put("async", ASYNC)
				.put("atomic", ATOMIC)
				.put("call", CALL)
				.put("command", COMMAND)
				.put("component", COMPONENT)
				.put("components", COMPONENTS)
				.put("configuration", CONFIGURATION)
				.put("event", EVENT)
				.put("extends", EXTENDS)
				.put("generic", GENERIC)
				.put("implementation", IMPLEMENTATION)
				.put("interface", INTERFACE)
				.put("module", MODULE)
				.put("new", NEW)
				.put("norace", NORACE)
				.put("post", POST)
				.put("provides", PROVIDES)
				.put("signal", SIGNAL)
				.put("task", TASK)
				.put("uses", USES)
				.put("nx_struct", NX_STRUCT)
				.put("nx_union", NX_UNION)
				/*
				 * c
				 */
				.put("auto", AUTO)
				.put("break", BREAK)
				.put("case", CASE)
				.put("char", CHAR)
				.put("const", CONST)
				.put("continue", CONTINUE)
				.put("default", DEFAULT)
				.put("do", DO)
				.put("double", DOUBLE)
				.put("else", ELSE)
				.put("enum", ENUM)
				.put("extern", EXTERN)
				.put("float", FLOAT)
				.put("for", FOR)
				.put("goto", GOTO)
				.put("if", IF)
				.put("inline", INLINE)
				.put("int", INT)
				.put("long", LONG)
				.put("register", REGISTER)
				.put("restrict", RESTRICT)
				.put("return", RETURN)
				.put("short", SHORT)
				.put("signed", SIGNED)
				.put("sizeof", SIZEOF)
				.put("static", STATIC)
				.put("struct", STRUCT)
				.put("switch", SWITCH)
				.put("typedef", TYPEDEF)
				.put("union", UNION)
				.put("unsigned", UNSIGNED)
				.put("void", VOID)
				.put("volatile", VOLATILE)
				.put("while", WHILE)
				/*
				 * GNU extensions
				 */
				.put("asm", ASM_KEYWORD)
				.put("offsetof", OFFSETOF)
				.put("__alignof__", ALIGNOF)
				.put("__asm", ASM_KEYWORD)
				.put("__asm__", ASM_KEYWORD)
				.put("__attribute", ATTRIBUTE)
				.put("__attribute__", ATTRIBUTE)
				.put("__builtin_offsetof", OFFSETOF)
				.put("__builtin_va_arg", VA_ARG)
				.put("__complex", COMPLEX)
				.put("__complex__", COMPLEX)
				.put("__const", CONST)
				.put("__const__", CONST)
				.put("__extension__", EXTENSION)
				.put("__imag", IMAGPART)
				.put("__imag__", IMAGPART)
				.put("__inline", INLINE)
				.put("__inline__", INLINE)
				.put("__label__", LABEL)
				.put("__real", REALPART)
				.put("__real__", REALPART)
				.put("__restrict", RESTRICT)
				.put("__signed", SIGNED)
				.put("__signed__", SIGNED)
				.put("__typeof", TYPEOF)
				.put("__typeof__", TYPEOF)
				.put("__volatile", VOLATILE)
				.put("__volatile__", VOLATILE)
				.build();

		DIRECT_EQUIVALENTS = ImmutableMap.<Integer, Integer> builder()
				/*
				 * Punctuators
				 */
				.put(castToInt('['), LBRACK)
				.put(castToInt(']'), RBRACK)
				.put(castToInt('('), LPAREN)
				.put(castToInt(')'), RPAREN)
				.put(castToInt('{'), LBRACE)
				.put(castToInt('}'), RBRACE)
				.put(castToInt(':'), COLON)
				.put(castToInt(';'), SEMICOLON)
				.put(castToInt('.'), DOT)
				.put(castToInt(','), COMMA)
				.put(castToInt('?'), QUESTION)
				/*
				 * Arithmetic
				 */
				.put(Token.DEC, MINUSMINUS)
				.put(Token.INC, PLUSPLUS)
				.put(castToInt('*'), STAR)
				.put(castToInt('/'), DIV)
				.put(castToInt('%'), MOD)
				.put(castToInt('+'), PLUS)
				.put(castToInt('-'), MINUS)
				.put(castToInt('&'), AND)
				.put(castToInt('^'), XOR)
				.put(castToInt('|'), OR)
				.put(castToInt('~'), TILDE)
				.put(castToInt('!'), NOT)
				.put(Token.LSH, LSHIFT)
				.put(Token.RSH, RSHIFT)
				.put(Token.LAND, ANDAND)
				.put(Token.LOR, OROR)
				/*
				 * Relational
				 */
				.put(castToInt('<'), LT)
				.put(castToInt('>'), GT)
				.put(Token.LE, LTEQ)
				.put(Token.GE, GTEQ)
				.put(Token.EQ, EQEQ)
				.put(Token.NE, NOTEQ)
				/*
				 * Assignment
				 */
				.put(castToInt('='), EQ)
				.put(Token.MULT_EQ, MULEQ)
				.put(Token.DIV_EQ, DIVEQ)
				.put(Token.MOD_EQ, MODEQ)
				.put(Token.PLUS_EQ, PLUSEQ)
				.put(Token.SUB_EQ, MINUSEQ)
				.put(Token.LSH_EQ, LSHIFTEQ)
				.put(Token.RSH_EQ, RSHIFTEQ)
				.put(Token.AND_EQ, ANDEQ)
				.put(Token.XOR_EQ, XOREQ)
				.put(Token.OR_EQ, OREQ)
				/*
				 * Other
				 */
				.put(Token.ELLIPSIS, ELLIPSIS)
				.put(Token.ARROW, ARROW)
				.put(castToInt('@'), AT)
				/*
				 * EOF
				 */
				.put(Token.EOF, EOF)
				.build();
	}

	private static int castToInt(char character) {
		return (int) character;
	}

}
