package pl.edu.mimuw.nesc.parser;

import static pl.edu.mimuw.nesc.parser.Parser.Lexer.*;

import java.util.ArrayList;
import java.util.List;

import pl.edu.mimuw.nesc.parser.Symbol;
import pl.edu.mimuw.nesc.preprocessor.*;
import pl.edu.mimuw.nesc.ast.CString;
import pl.edu.mimuw.nesc.ast.gen.LexicalCst;

%%
%class Lexer
%public
%final
%type Symbol
%line
%column
%unicode
%pack

%eofval{
    return symbol(EOF);
%eofval}

%{
    private StringBuilder builder = null;
    private Undef.Builder undefBuilder = null;
    private Include.Builder includeBuilder = null;
    private Linemarker.Builder linemarkerBuilder = null;

    private PreprocessorListener listener;
    private List<String> errorList = new ArrayList<String>();
    
    public void setListener(PreprocessorListener listener) {
        this.listener = listener;
    }

    public boolean errors() {
        return !errorList.isEmpty();
    }

    public List<String> getErrorList() {
        return errorList;
    }
    
    private int getLine() {
        return yyline + 1;
    }
    
    private int getColumn() {
        return yycolumn + 1;
    }
    
    private Symbol symbol(int type) {
        return symbol(type, null);
    }
    
    private Symbol symbol(int type, Object value) {
    	return new Symbol(type, getLine(), getColumn(), value);
    }

    private void preprocessorDirective(Linemarker linemarker) {
        this.yyline = linemarker.getLineNumber();
        this.yycolumn = 0;
        if (this.listener != null) {
            this.listener.preprocessorDirective(linemarker);
        }
    }

    private void preprocessorDirective(PreprocessorDirective directive) {
        if (this.listener != null) {
            this.listener.preprocessorDirective(directive);
        }
    }
    
    private void finishDefine() {
    	this.yybegin(YYINITIAL);
        final String defineStr = this.builder.toString();
        final Define define = new Define(defineStr);
        this.builder = null;
        preprocessorDirective(define);
    }
    
    private void finishUndef() {
    	this.yybegin(YYINITIAL);
        final Undef undef = this.undefBuilder.build();
        this.undefBuilder = null;
        preprocessorDirective(undef);
    }
    
    private void finishInclude() {
        this.yybegin(YYINITIAL);
        final Include include = this.includeBuilder.build();
        this.includeBuilder = null;
        preprocessorDirective(include);
    }
    
    private void finishLinemarker() {
        this.yybegin(YYINITIAL);
        final Linemarker linemarker = this.linemarkerBuilder.build();
        this.linemarkerBuilder = null;
        preprocessorDirective(linemarker);
    }
    
    private void unexpectedCharacter() {
        errorList.add("(" + getLine() + ", " + getColumn() + ") Unexpected character: '" + yytext() + "'.");
    }

    private void unexpectedEndOfFile() {
        System.out.println("Unexpected end of file.");
    }

%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
MacroInputCharacter = [^\r\n\\]
WhiteSpace     = {LineTerminator} | [ \t\f]

Comment = {TraditionalComment} | {EndOfLineComment} | {DocumentationComment}
TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )*

/* Octal digit */
O	= [0-7]
/* Decimal digit */
D	= [0-9]
/* Non-zero decimal digit */
NZ	= [1-9]
/* Letter or _ */
L	= [a-zA-Z_]
/* All symbols available for identifiers */
A	= [a-zA-Z_0-9]
/* Hexadecimal digit */
H	= [a-fA-F0-9]
/* Hexadecimal prefix */
HP	= (0[xX])
/* Exponent part */
E	= ([Ee][+-]?{D}+)
/* Binary exponent part */
P	= ([Pp][+-]?{D}+)
/* Floating suffix */
FS	= (f|F|l|L)
/* Integer suffix */
IS	= (((u|U)(l|L|ll|LL)?)|((l|L|ll|LL)(u|U)?))
/* Character prefix */
CP	= (u|U|L)
/* String prefix */
SP	= (u8|u|U|L)
/* Escape sequence */
ES	= (\\(['\"\?\\abfnrtv]|[0-7]{1,3}|x[a-fA-F0-9]+))

Identifier = {L}{A}*

IntegerConstant =
	{HP}{H}+{IS}?		/* HexadecimalIntegerConstant */
	| {NZ}{D}*{IS}?		/* DecimalIntegerConstant */
	| "0"{O}*{IS}?		/* OctalIntegerConstant */

FloatingConstant =
	{D}+{E}{FS}?				/* */
	| {D}*"."{D}+{E}?{FS}?		/* */
	| {D}+"."{E}?{FS}?			/* */
	| {HP}{H}+{P}{FS}?			/* */
	| {HP}{H}*"."{H}+{P}{FS}?	/* */
	| {HP}{H}+"."{P}{FS}?		/* */

CharacterConstant = {CP}?"'"([^'\\\n]|{ES})+"'"
StringConstant = {SP}?\"([^\"\\\n]|{ES})*\"

%state PREPROCESSOR_DEFINE
%state PREPROCESSOR_UNDEF
%state PREPROCESSOR_INCLUDE
%state PREPROCESSOR_LINEMARKER

%%

<YYINITIAL> {
        /* 
         * nesC.
         */
        "as"					{ return symbol(AS); }
        "abstract"				{ return symbol(ABSTRACT); }
        "async"					{ return symbol(ASYNC); }
        "atomic"				{ return symbol(ATOMIC); }
        "call"					{ return symbol(CALL); }
        "command"				{ return symbol(COMMAND); }
		"component"				{ return symbol(COMPONENT); }
		"components"			{ return symbol(COMPONENTS); }
        "configuration"			{ return symbol(CONFIGURATION); }
        "event"					{ return symbol(EVENT); }
        "extends"				{ return symbol(EXTENDS); }
        "generic"				{ return symbol(GENERIC); }
        "implementation"		{ return symbol(IMPLEMENTATION); }
        "interface"				{ return symbol(INTERFACE); }
        "module"				{ return symbol(MODULE); }
        "new"					{ return symbol(NEW); }
        "norace"				{ return symbol(NORACE); }
        "post"					{ return symbol(POST); }
		"provides"				{ return symbol(PROVIDES); }
		"signal"				{ return symbol(SIGNAL); }
		"task"					{ return symbol(TASK); }
		"uses"					{ return symbol(USES); }
		
		"nx_struct"				{ return symbol(NX_STRUCT); }
		"nx_union"				{ return symbol(NX_UNION); }

		"<-"					{ return symbol(LEFT_ARROW); }
		"@"						{ return symbol(AT); }
	    
        /* 
         * GNU Extensions. 
         */
        "asm"                   { return symbol(ASM_KEYWORD); }
        "offsetof"              { return symbol(OFFSETOF); }
        "__alignof__"           { return symbol(ALIGNOF); }
        "__asm"                 { return symbol(ASM_KEYWORD); }
        "__asm__"               { return symbol(ASM_KEYWORD); }
        "__attribute"           { return symbol(ATTRIBUTE); }
        "__attribute__"         { return symbol(ATTRIBUTE); }
        "__builtin_offsetof"    { return symbol(OFFSETOF); }
        "__builtin_va_arg"      { return symbol(VA_ARG); }
        "__complex"             { return symbol(COMPLEX); }
        "__complex__"           { return symbol(COMPLEX); }
        "__const"               { return symbol(CONST); }
        "__const__"             { return symbol(CONST); }
        "__extension__"         { return symbol(EXTENSION); }
        "__imag"                { return symbol(IMAGPART); }
        "__imag__"              { return symbol(IMAGPART); }
        "__inline"              { return symbol(INLINE); }
        "__inline__"            { return symbol(INLINE); }
        "__label__"             { return symbol(LABEL); }
        "__real"                { return symbol(REALPART); }
        "__real__"              { return symbol(REALPART); }
        "__restrict"            { return symbol(RESTRICT); }
        "__signed"              { return symbol(SIGNED); }
        "__signed__"            { return symbol(SIGNED); }
        "__typeof"              { return symbol(TYPEOF); }
        "__typeof__"            { return symbol(TYPEOF); }
        "__volatile"            { return symbol(VOLATILE); }
        "__volatile__"          { return symbol(VOLATILE); }	
		
        /*
		 * C grammar.
		 */
		/* A.1 Lexical grammar. */
		
		/* A.1.1 Lexical elements. */
		
		/* A.1.2 Keywords. */
		"auto"					{ return symbol(AUTO); }
		"break"					{ return symbol(BREAK); }
		"case"					{ return symbol(CASE); }
		"char"					{ return symbol(CHAR); }
		"const"					{ return symbol(CONST); }
		"continue"				{ return symbol(CONTINUE); }
		"default"				{ return symbol(DEFAULT); }
		"do"					{ return symbol(DO); }
		"double"				{ return symbol(DOUBLE); }
		"else"					{ return symbol(ELSE); }
		"enum"					{ return symbol(ENUM); }
		"extern"				{ return symbol(EXTERN); }
		"float"					{ return symbol(FLOAT); }
		"for"					{ return symbol(FOR); }
		"goto"					{ return symbol(GOTO); }
		"if"					{ return symbol(IF); }
		"inline"				{ return symbol(INLINE); }
		"int"					{ return symbol(INT); }
		"long"					{ return symbol(LONG); }
		"register"				{ return symbol(REGISTER); }
		"restrict"				{ return symbol(RESTRICT); }
		"return"				{ return symbol(RETURN); }
		"short"					{ return symbol(SHORT); }
		"signed"				{ return symbol(SIGNED); }
		"sizeof"				{ return symbol(SIZEOF); }
		"static"				{ return symbol(STATIC); }
		"struct"				{ return symbol(STRUCT); }
		"switch"				{ return symbol(SWITCH); }
		"typedef"				{ return symbol(TYPEDEF); }
		"union"					{ return symbol(UNION); }
		"unsigned"				{ return symbol(UNSIGNED); }
		"void"					{ return symbol(VOID); }
		"volatile"				{ return symbol(VOLATILE); }
		"while"					{ return symbol(WHILE); }
	    
		/* A.1.7 Punctuators. */
		"["                     { return symbol(LBRACK); }
		"]"                     { return symbol(RBRACK); }
		"("                     { return symbol(LPAREN); }
		")"                     { return symbol(RPAREN); }
		"{"                     { return symbol(LBRACE); }
		"}"                     { return symbol(RBRACE); }
		
		":"						{ return symbol(COLON); }
		";"						{ return symbol(SEMICOLON); }
		"."                     { return symbol(DOT); }
		","						{ return symbol(COMMA); }
		"->"                    { return symbol(ARROW); }
		"?"						{ return symbol(QUESTION); }
		"..."					{ return symbol(ELLIPSIS); }
		
		"++"					{ return symbol(PLUSPLUS); }
		"--"					{ return symbol(MINUSMINUS); }
		
		"*"						{ return symbol(STAR); }
		"/"						{ return symbol(DIV); }
		"%"						{ return symbol(MOD); }
		"+"						{ return symbol(PLUS); }
		"-"						{ return symbol(MINUS); }
		"&"						{ return symbol(AND); }
		"^"						{ return symbol(XOR); }
		"|"						{ return symbol(OR); }
		"~"						{ return symbol(TILDE); }
		"!"						{ return symbol(NOT); }
		"<<"					{ return symbol(LSHIFT); }
		">>"					{ return symbol(RSHIFT); }
		"&&"					{ return symbol(ANDAND); }
		"||"					{ return symbol(OROR); }
		
		"<"						{ return symbol(LT); }
		">"						{ return symbol(GT); }
		"<="					{ return symbol(LTEQ); }
		">="					{ return symbol(GTEQ); }
		"=="					{ return symbol(EQEQ); }
		"!="					{ return symbol(NOTEQ); }
		
		"="						{ return symbol(EQ); }
		"*="					{ return symbol(MULEQ); }
		"/="					{ return symbol(DIVEQ); }
		"%="					{ return symbol(MODEQ); }
		"+="					{ return symbol(PLUSEQ); }
		"-="					{ return symbol(MINUSEQ); }
		"<<="					{ return symbol(LSHIFTEQ); }
		">>="					{ return symbol(RSHIFTEQ); }
		"&="					{ return symbol(ANDEQ); }
		"^="					{ return symbol(XOREQ); }
		"|="					{ return symbol(OREQ); }
		
		"<:"					{ return symbol(LBRACK); }
		":>"					{ return symbol(RBRACK); }
		"<%"					{ return symbol(LBRACE); }
		"%>"					{ return symbol(RBRACE); }
        
        /* A.1.3 Identifiers. */
		{Identifier}            { return symbol(IDENTIFIER, new Value.IdToken(yytext())); }
		
		/* A.1.4 Universal character names. */
		
		/* A.1.5 Constants. */
        {IntegerConstant}		{ return symbol(INTEGER_LITERAL, new LexicalCst(null, new CString(yytext()))); }
        {FloatingConstant}  	{ return symbol(FLOATING_POINT_LITERAL, new LexicalCst(null, new CString(yytext()))); }
        {CharacterConstant}		{ return symbol(CHARACTER_LITERAL, new LexicalCst(null, new CString(yytext()))); }
		
		/* A.1.6 String literals. */
		{StringConstant} 		{ return symbol(STRING_LITERAL, yytext()); }

        /* Preprocessor. */
        "#define"
            {
                this.builder = new StringBuilder();
                this.yybegin(PREPROCESSOR_DEFINE);
            }
        "#undef"
            {
                this.undefBuilder = Undef.builder();
                this.yybegin(PREPROCESSOR_UNDEF);
            }
        "#include"
            {
                this.includeBuilder = Include.builder();
                this.yybegin(PREPROCESSOR_INCLUDE);
            }
        "#"
            {
                this.linemarkerBuilder = Linemarker.builder();
                this.yybegin(PREPROCESSOR_LINEMARKER);
            }

        {Comment}               { /* ignore */ }
        {WhiteSpace}            { /* ignore */ }


        /* Unrecognized character/string. */
        .                       { unexpectedCharacter(); }
}

<PREPROCESSOR_DEFINE> {
    /*
     *  Regular characters.
     *  All characters except backslash and line terminators are allowed.
     */
    {MacroInputCharacter}+      { this.builder.append(yytext()); }
    /* Continue macro definition in the following line. */
    "\\"{LineTerminator}        { this.builder.append(yytext()); }
    /* End of macro definition. */
    {LineTerminator}			{ finishDefine(); }
    <<EOF>>                     { finishDefine(); }
    .                           { unexpectedCharacter(); }
}

/* The single identifier is expected. */
<PREPROCESSOR_UNDEF> {
    {Identifier}      			{ this.undefBuilder.identifier(yytext()); }
    {LineTerminator}			{ finishUndef(); }
    <<EOF>>						{ finishUndef(); }
    .                           { unexpectedCharacter(); }
}

<PREPROCESSOR_INCLUDE> {
    "\"" [^\r\n\"]+ "\""
        {
            this.includeBuilder.quotedPath(yytext());
            this.includeBuilder.isSystem(false);
        }
    "<" [^\r\n\"]+ ">"
        {
            this.includeBuilder.quotedPath(yytext());
            this.includeBuilder.isSystem(true);
        }
        
    {LineTerminator}			{ finishInclude(); }
    <<EOF>>						{ finishInclude(); }
    .                           { unexpectedCharacter(); }
}

/* Linemarker should contain exactly one line. */
<PREPROCESSOR_LINEMARKER> {
    /* Only integer and string constants are allowed. */
    {StringConstant}            { this.linemarkerBuilder.fileName(yytext()); }
    {IntegerConstant}           { this.linemarkerBuilder.addNumber(yytext()); }
    {LineTerminator}			{ finishLinemarker(); }
	<<EOF>>						{ finishLinemarker(); }

    .                           { unexpectedCharacter(); }
}
