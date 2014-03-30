/* TODO: borrowed from..., licence, etc... */
%code imports {
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.io.Files;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.*;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.issue.*;
import pl.edu.mimuw.nesc.lexer.TokenPrinter;
import pl.edu.mimuw.nesc.parser.value.*;
import pl.edu.mimuw.nesc.semantic.*;
import pl.edu.mimuw.nesc.semantic.nesc.*;
import pl.edu.mimuw.nesc.token.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
}

%define package {pl.edu.mimuw.nesc.parser}
%define public
%define parser_class_name {Parser}

%require "3.0"

/*
 * Enables verbose syntax error messages.
 * http://www.gnu.org/software/bison/manual/html_node/Error-Reporting.html
 */
%define parse.error verbose

%define annotations {@SuppressWarnings({"all"})}

/*
 * Expected S/R conflicts.
 */
%expect 12

/*
 * Start non-terminal of the grammar.
 */
%start dispatch

/*
 * ========== Token definitions ==========
 *
 * %token <semantic_value_type> name_or_names "alias"
 */

/* The dispatching (fake) tokens. */
%token <Symbol> DISPATCH_C DISPATCH_NESC DISPATCH_PARM DISPATCH_TYPE

/*
 * All identifiers that are not reserved words and are not declared typedefs
 * in the current block.
 */
%token <Symbol> IDENTIFIER "identifier"

/*
 * All identifiers that are declared typedefs in the current block. In some
 * contexts, they are treated just like IDENTIFIER, but they can also serve
 * as typespecs in declarations.
 */
%token <Symbol> TYPEDEF_NAME "typename"

/*
 * An identifier that is declared as a component reference in the current
 * block, and which is going to be used to refer to a typedef from the
 * component via the component-ref DOT identifier syntax.
 */
%token <Symbol> COMPONENTREF "component reference"

/*
 * Character or numeric constants. yylval is the node for the constant.
 * TODO: string or int/float/char ?
 */
%token <Symbol> INTEGER_LITERAL "integer literal"
%token <Symbol> FLOATING_POINT_LITERAL "float literal"
%token <Symbol> CHARACTER_LITERAL "character literal"
%token <Symbol> STRING_LITERAL "string literal"

/*
 * String constants in raw form.
 * TODO: What is MAGIC_STRING?
 */
%token <Symbol> MAGIC_STRING

/*
 * Reserved words that specify type.
 */
%token <Symbol> VOID "void"
%token <Symbol> CHAR "char"
%token <Symbol> SHORT "short"
%token <Symbol> INT "int"
%token <Symbol> LONG "long"
%token <Symbol> FLOAT "float"
%token <Symbol> DOUBLE "double"
%token <Symbol> SIGNED "signed"
%token <Symbol> UNSIGNED "unsigned"
%token <Symbol> COMPLEX "complex"
/*
 * Reserved words that specify storage class.
 */
%token <Symbol> TYPEDEF "typedef"
%token <Symbol> EXTERN "extern"
%token <Symbol> STATIC "static"
%token <Symbol> AUTO "auto"
%token <Symbol> REGISTER "register"
%token <Symbol> COMMAND "command"
%token <Symbol> EVENT "event"
%token <Symbol> ASYNC "async"
%token <Symbol> TASK "task"
%token <Symbol> NORACE "norace"

/*
 * Reserved words that qualify types/functions: "const" or "volatile",
 * "deletes".
 * FIXME: FN_QUAL, present in grammar but never pushed from lexer.
 */
%token <Symbol> CONST "const"
%token <Symbol> RESTRICT "restrict"
%token <Symbol> VOLATILE "volatile"
%token <Symbol> INLINE "inline"
%token <Symbol> FN_QUAL

/* the reserved words */
%token <Symbol> SIZEOF "sizeof"
%token <Symbol> ENUM "enum"
%token <Symbol> IF "if"
%token <Symbol> ELSE "else"
%token <Symbol> WHILE "while"
%token <Symbol> DO "do"
%token <Symbol> FOR "for"
%token <Symbol> SWITCH "switch"
%token <Symbol> CASE "case"
%token <Symbol> DEFAULT "default"

%token <Symbol> BREAK "break"
%token <Symbol> CONTINUE "continue"
%token <Symbol> RETURN "return"
%token <Symbol> GOTO "goto"
%token <Symbol> ASM_KEYWORD "asm"
%token <Symbol> TYPEOF "typeof"
%token <Symbol> ALIGNOF "alignof"


%token <Symbol> ATTRIBUTE "attribute"
%token <Symbol> EXTENSION "extension"
%token <Symbol> LABEL "label"

%token <Symbol> REALPART "realpart"
%token <Symbol> IMAGPART "imagpart"
%token <Symbol> VA_ARG "va_arg"
%token <Symbol> OFFSETOF "offsetof"

/* nesC reserved words */
%token <Symbol> ATOMIC "atomic"
%token <Symbol> USES "uses"
%token <Symbol> INTERFACE "interface"
%token <Symbol> COMPONENTS "components"
%token <Symbol> PROVIDES "provides"
%token <Symbol> MODULE "module"

%token <Symbol> INCLUDES "includes"
%token <Symbol> CONFIGURATION "configuration"
%token <Symbol> AS "as"
%token <Symbol> IMPLEMENTATION "implementation"
%token <Symbol> CALL "call"

%token <Symbol> SIGNAL "signal"
%token <Symbol> POST "post"
%token <Symbol> GENERIC "generic"
%token <Symbol> NEW "new"

%token <Symbol> NX_STRUCT "nx_struct"
%token <Symbol> NX_UNION "nx_union"
%token <Symbol> STRUCT "struct"
%token <Symbol> UNION "union"

/* words reserved for nesC's future. Some may never be used... */
%token <Symbol> ABSTRACT "abstract"
%token <Symbol> COMPONENT "component"
%token <Symbol> EXTENDS "extends"

%token <Symbol> TARGET_ATTRIBUTE0
%token <Symbol> TARGET_ATTRIBUTE1
%token <Symbol> TARGET_DEF

/*
 * All kind of parentheses, operators, etc.
 */
%token <Symbol> LBRACK "["
%token <Symbol> RBRACK "]"

%token <Symbol> LPAREN "("
%token <Symbol> RPAREN ")"

%token <Symbol> LBRACE "{"
%token <Symbol> RBRACE "}"

%token <Symbol> COLON ":"
%token <Symbol> SEMICOLON ";"
%token <Symbol> DOT "."
%token <Symbol> COMMA ","

%token <Symbol> ARROW "->"
%token <Symbol> LEFT_ARROW "<-"
%token <Symbol> AT "@"
%token <Symbol> QUESTION "?"
%token <Symbol> ELLIPSIS "..."

%token <Symbol> STAR "*"
%token <Symbol> DIV "/"
%token <Symbol> MOD "%"
%token <Symbol> PLUS "+"
%token <Symbol> MINUS "-"
%token <Symbol> AND "&"
%token <Symbol> XOR "^"
%token <Symbol> OR "|"
%token <Symbol> TILDE "~"
%token <Symbol> NOT "!"
%token <Symbol> LSHIFT "<<"
%token <Symbol> RSHIFT ">>"
%token <Symbol> ANDAND "&&"
%token <Symbol> OROR "||"

%token <Symbol> PLUSPLUS "++"
%token <Symbol> MINUSMINUS "--"

%token <Symbol> LT "<"
%token <Symbol> GT ">"
%token <Symbol> LTEQ "<="
%token <Symbol> GTEQ ">="
%token <Symbol> EQEQ "=="
%token <Symbol> NOTEQ "!="

%token <Symbol> EQ "="
%token <Symbol> MULEQ "*="
%token <Symbol> DIVEQ "/="
%token <Symbol> MODEQ "%="
%token <Symbol> PLUSEQ "+="
%token <Symbol> MINUSEQ "-="
%token <Symbol> LSHIFTEQ "<<="
%token <Symbol> RSHIFTEQ ">>="
%token <Symbol> ANDEQ "&="
%token <Symbol> XOREQ "^="
%token <Symbol> OREQ "|="


/*
 * ========== Precedences and associativity ==========
 *
 * The lower terminal's precedence is defined, the higher is the precedence.
 * Terminals in the same line has the same precedence.
 * More details can be found in the Internet :)
 */

/*
 * Add precedence rules to solve dangling else s/r conflict.
 */
%nonassoc    IF
%nonassoc    ELSE

/*
 * Define the operators' precedences.
 */
%right       EQ MULEQ DIVEQ MODEQ PLUSEQ MINUSEQ LSHIFTEQ RSHIFTEQ ANDEQ XOREQ OREQ
%right       QUESTION COLON
%left        OROR
%left        ANDAND
%left        OR
%left        XOR
%left        AND
%left        EQEQ NOTEQ
%left        LTEQ GTEQ LT GT
%left        LSHIFT RSHIFT
%left        PLUS MINUS
%left        STAR DIV MOD
%right       PLUSPLUS MINUSMINUS
%left        ARROW DOT LPAREN LBRACK

/*
 * ========== Non-terminals ==========
 *
 * Define the type of value returned by each production.
 * %type <field> name_list
 */
%type <AsmOperand> asm_operand
%type <LinkedList<AsmOperand>> asm_operands nonnull_asm_operands
%type <AsmStmt> maybeasm
%type <Attribute> nattrib
%type <LinkedList<Attribute>> maybe_attribute attribute
%type <LinkedList<Attribute>> attributes attribute_list nesc_attributes
%type <GccAttribute> attrib target_attribute
%type <NescAttribute> nastart
%type <Declaration> datadecl datadef decl extdef fndef
%type <FunctionDecl> fndef2
%type <LinkedList<Declaration>> datadecls decls extdefs
%type <LinkedList<Declaration>> initdecls initdecls_ notype_initdecls notype_initdecls_
%type <Declaration> nested_function notype_nested_function
%type <LinkedList<Declaration>> old_style_parm_decls
%type <Declaration> component_decl
%type <LinkedList<Declaration>> component_decl_list component_decl_list2
%type <VariableDecl> initdcl
%type <Declaration> component_declarator enumerator
%type <LinkedList<Declaration>> enumlist
%type <LinkedList<Declaration>> components
%type <LinkedList<Declaration>> components_notype
%type <Declaration> component_notype_declarator
%type <LinkedList<Declaration>> parmlist parmlist_1 parmlist_2 parms
%type <Declaration> parm notype_initdcl old_parameter just_datadef
%type <LinkedList<Declaration>> parmlist_or_identifiers identifiers parmlist_or_identifiers_1
%type <Declarator> declarator after_type_declarator notype_declarator
%type <Declarator> absdcl absdcl1 absdcl1_noea absdcl1_ea direct_absdcl1
%type <Declarator> parm_declarator
%type <NestedDeclarator> array_declarator fn_declarator array_or_fn_declarator
%type <NestedDeclarator> absfn_declarator array_or_absfn_declarator
%type <Expression> cast_expr expr expr_no_commas init
%type <LinkedList<Expression>> initlist_maybe_comma
%type <Expression> initelt primary string
%type <LinkedList<Expression>> initlist1
%type <Expression> initval restricted_expr
%type <LinkedList<Expression>> nonnull_exprlist nonnull_exprlist_ exprlist
%type <Designator> designator
%type <LinkedList<Designator>> designator_list
%type <Expression> unary_expr xexpr
%type <FunctionCall>function_call
%type <Expression> generic_type
%type <LinkedList<Expression>> typelist
%type <IdLabel> id_label
%type <LinkedList<IdLabel>> maybe_label_decls label_decls label_decl
%type <LinkedList<IdLabel>> identifiers_or_typenames
%type <Symbol> identifier type_parm
%type <ValueExpression> if_prefix
%type <ValueStatements> stmt_or_labels
%type <ValueStatement> simple_if stmt_or_label
%type <ValueLeftUnaryOp> unop
%type <Symbol> extension compstmt_start
%type <Symbol> sizeof alignof
%type <Label> label
%type <LinkedList<Statement>> stmts xstmts
%type <Statement> compstmt_or_error compstmt
%type <Statement> labeled_stmt stmt stmt_or_error atomic_stmt
%type <ConditionalStmt> do_stmt_start
%type <LinkedList<StringAst>> asm_clobbers
%type <LinkedList<TypeElement>> declspecs_nosc_nots_nosa_noea
%type <LinkedList<TypeElement>> declspecs_nosc_nots_nosa_ea
%type <LinkedList<TypeElement>> declspecs_nosc_nots_sa_noea
%type <LinkedList<TypeElement>> declspecs_nosc_nots_sa_ea
%type <LinkedList<TypeElement>> declspecs_nosc_ts_nosa_noea
%type <LinkedList<TypeElement>> declspecs_nosc_ts_nosa_ea
%type <LinkedList<TypeElement>> declspecs_nosc_ts_sa_noea
%type <LinkedList<TypeElement>> declspecs_nosc_ts_sa_ea
%type <LinkedList<TypeElement>> declspecs_sc_nots_nosa_noea
%type <LinkedList<TypeElement>> declspecs_sc_nots_nosa_ea
%type <LinkedList<TypeElement>> declspecs_sc_nots_sa_noea
%type <LinkedList<TypeElement>> declspecs_sc_nots_sa_ea
%type <LinkedList<TypeElement>> declspecs_sc_ts_nosa_noea
%type <LinkedList<TypeElement>> declspecs_sc_ts_nosa_ea
%type <LinkedList<TypeElement>> declspecs_sc_ts_sa_noea
%type <LinkedList<TypeElement>> declspecs_sc_ts_sa_ea
%type <LinkedList<TypeElement>> declspecs_ts
%type <LinkedList<TypeElement>> declspecs_nots
%type <LinkedList<TypeElement>> declspecs_ts_nosa
%type <LinkedList<TypeElement>> declspecs_nots_nosa
%type <LinkedList<TypeElement>> declspecs_nosc_ts
%type <LinkedList<TypeElement>> declspecs_nosc_nots
%type <LinkedList<TypeElement>> declspecs_nosc
%type <LinkedList<TypeElement>> declspecs
%type <Qualifier> type_qual maybe_type_qual
%type <Rid> scspec type_spec
%type <LinkedList<TypeElement>> eattributes
%type <TypeElement> type_spec_attr type_spec_nonattr
%type <TypeElement> type_spec_nonreserved_nonattr type_spec_reserved_attr
%type <TypeElement> type_spec_reserved_nonattr
%type <TypeElement> structdef structuse
%type <TypeElement> fn_qual
%type <LinkedList<TypeElement>> maybe_type_quals_attrs fn_quals
%type <AstType> typename
%type <Word> idword any_word tag
%type <LinkedList<Symbol>> fieldlist
%type <ValueStructKind> structkind

%type <ValueCallKind> callkind
%type <LinkedList<Declaration>> datadef_list
%type <LinkedList<Declaration>> parameters parameters1
%type <Declaration> requires provides
%type <LinkedList<Declaration>> requires_or_provides requires_or_provides_list
%type <LinkedList<Declaration>> requires_or_provides_list_
%type <LinkedList<Declaration>> parameterised_interface_list
%type <Declaration> parameterised_interface
%type <LinkedList<Declaration>> parameterised_interfaces
%type <Declaration> interface_parm
%type <LinkedList<Declaration>> interface_parms interface_parm_list
%type <LinkedList<Declaration>> component_parms
%type <Declaration> template_parm
%type <LinkedList<Declaration>> template_parms template_parmlist
%type <Declaration> target_def
%type <InterfaceRef> interface_ref interface_type
%type <ComponentRef> component_ref component_ref2
%type <LinkedList<ComponentRef>> component_list
%type <ComponentsUses> cuses
%type <Connection> connection
%type <Declaration> configuration_decl
%type <LinkedList<Declaration>> configuration_decls
%type <EndPoint> endpoint
%type <ParameterisedIdentifier> parameterised_identifier
%type <Implementation> iconfiguration imodule
%type <ValueBoolean> generic
%type <Expression> generic_arg
%type <LinkedList<Expression>> generic_arglist generic_args
%type <Interface> interface
%type <Component> component
%type <Component> module
%type <Component> configuration
%type <Component> binary_component

%type <StringAst> string_chain

%%

/*
 * There is no need to reverse the order of elements in lists.

nonnull_exprlist:
      nonnull_exprlist_
        { $$ = expression_reverse($1); }
    ;

nonnull_exprlist_:
      expr_no_commas
        { $$ = $1; }
    | nonnull_exprlist_ ',' expr_no_commas
        { $$ = expression_chain($3, $1); }
    ;
 *
 * In original parser only the beginning of the list is known, so that each
 * new element is put at the begining of the list. When the entire list is
 * build, the order of elements needs to be reversed.
 *
 * In this implementation new elements are appended to the list, so that the
 * proper order is preserved.
 */

/*
 * NOTE:FIXME: DISPATCH_X tokens are fake. They were created to avoid
 * conflicts. The selection of particular production depends (I believe) on
 * the extension of source file (.nc, .c, etc.).
 */

dispatch:
      DISPATCH_NESC interface
    { entityRoot = $2; }
    | DISPATCH_NESC component
    { entityRoot = $2; }
    | DISPATCH_C extdefs
    { entityRoot = null; }
    | DISPATCH_C
    { entityRoot = null; }
    | DISPATCH_PARM parm
    { entityRoot = $2; }
    | DISPATCH_PARM error
    { entityRoot = Declarations.makeErrorDecl(); }
    | DISPATCH_TYPE typename
    { entityRoot = $2; }
    | DISPATCH_TYPE error
    { entityRoot = null; }
    ;

/*
* TODO: What is ncheader and includes?
*/
ncheader:
      includes_list
    { this.extdefsFinish(); }
    | extdefs
    {
        this.extdefsFinish();
        this.extdefs = $1;
    }
    ;

includes_list:
      includes_list includes
    | /* empty */
    ;

includes:
      INCLUDES include_list SEMICOLON
    ;

include_list:
      identifier
    | include_list COMMA identifier
    ;

interface:
      ncheader INTERFACE idword
    {
		/* Push new level regardless the interface is generic or not. */
	    pushLevel(false);
    }
      interface_parms nesc_attributes LBRACE datadef_list RBRACE
    {
        final Interface iface = new Interface($2.getLocation(), $6, $3, $5, $8);
        iface.setEndLocation($9.getEndLocation());
        popLevel();
        $$ = iface;
    }
    ;

interface_parms:
      /* empty */
    { $$ = Lists.<Declaration>newList(); }
    | LT interface_parm_list GT
    { $$ = $2; }
    ;

interface_parm_list:
      interface_parm
    { $$ = Lists.<Declaration>newList($1); }
    | interface_parm_list COMMA interface_parm
    { $$ = Lists.<Declaration>chain($1, $3); }
    ;

interface_parm:
      type_parm nesc_attributes
    {
        final String paramName = $1.getValue();
        addTypename(paramName);
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        final TypeParmDecl decl = new TypeParmDecl($1.getLocation(), paramName, $2);
        decl.setEndLocation(endLocation);
        $$ = decl;
    }
    ;

type_parm:
      IDENTIFIER
    { $$ = $1; }
    ;

datadef_list:
      datadef_list just_datadef
    { $$ = Lists.<Declaration>chain($1, $2); }
    | just_datadef
    { $$ = Lists.<Declaration>newList($1); }
    ;

parameters:
      LBRACK
    {
        pushLevel(true);    // for parsing purposes
    }
      parameters1
    { $$ = $3; }
    ;

parameters1:
      parms RBRACK
    {
        $$ = $1;
    }
    | error RBRACK
    { $$ = Lists.<Declaration>newList(Declarations.makeErrorDecl()); }
    ;

component:
      ncheader module
    { $$ = $2; }
    | ncheader configuration
    { $$ = $2; }
    | ncheader binary_component
    { $$ = $2; }
    ;

module:
      generic MODULE idword
    {
        pushLevel(false);   // FIXME parmlevel
    }
      component_parms nesc_attributes LBRACE requires_or_provides_list RBRACE imodule
    {
        final Location location = $1.getLocation() != null ? $1.getLocation() : $2.getLocation();
        final Module module = new Module(location, $6, $3, $8, $10, $1.getValue(), $5);
        module.setEndLocation($10.getEndLocation());
        popLevel();
        $$ = module;
    }
    ;

configuration:
      generic CONFIGURATION idword
    {
        pushLevel(false);   // FIXME parmlevel
    }
      component_parms nesc_attributes LBRACE requires_or_provides_list RBRACE iconfiguration
    {
        final Location location = $1.getLocation() != null ? $1.getLocation() : $2.getLocation();
        final Configuration configuration = new Configuration(location, $6, $3, $8, $10, $1.getValue(), $5);
        configuration.setEndLocation($10.getEndLocation());
        popLevel();
        $$ = configuration;
    }
    ;

binary_component:
      COMPONENT idword nesc_attributes LBRACE requires_or_provides_list RBRACE
    {
        final BinaryComponentImpl dummy = new BinaryComponentImpl(null);
        final BinaryComponent component = new BinaryComponent($1.getLocation(), $3, $2, $5, dummy, false,
                Lists.<Declaration>newList());
        component.setEndLocation($6.getEndLocation());
        $$ = component;
    }
    ;

generic:
      GENERIC
    { $$ = new ValueBoolean($1.getLocation(), $1.getEndLocation(), true); }
    | /* empty */
    { $$ = new ValueBoolean(Location.getDummyLocation(), Location.getDummyLocation(), false); }
    ;

component_parms:
      /* empty */
    {
        $$ = Lists.<Declaration>newList();
    }
    | LPAREN template_parms RPAREN
    {
        $$ = $2;
    }
    ;

template_parms:
      /* empty */
    { $$ = Lists.<Declaration>newList(); }
    | template_parmlist
    { $$ = $1; }
    ;

template_parmlist:
      template_parm
    { $$ = Lists.<Declaration>newList($1); }
    | template_parmlist COMMA template_parm
    { $$ = Lists.<Declaration>chain($1, $3); }
    ;

/* A declaration of a template parameter, i.e., a regular
 parameter-like declaration (name required).
 The 'typedef t' syntax for declaring a type argument is detected
 inside declare_template_parameter */
/*
* A declaration of generic module parameter. It could be a regular
* parameter-like declaration or declaration of type ('typedef t').
*
*
* Example:
*     generic module AQueue(int n, typedef t) { ... }
*
* At this point there is made a distinction between regular and typedef
* declaration.
*/
template_parm:
      declspecs_ts xreferror after_type_declarator maybe_attribute
    {
        declareName($3, $1);
        $$ = NescSemantics.declareTemplateParameter(Optional.of($3), $1, $4);
    }
    | declspecs_ts xreferror notype_declarator maybe_attribute
    {
        declareName($3, $1);
        $$ = NescSemantics.declareTemplateParameter(Optional.of($3), $1, $4);
    }
    | declspecs_nots xreferror notype_declarator maybe_attribute
    {
        declareName($3, $1);
        $$ = NescSemantics.declareTemplateParameter(Optional.of($3), $1, $4);
    }
    | declspecs_ts xreferror
    {
        $$ = NescSemantics.declareTemplateParameter(Optional.<Declarator>absent(), $1, Lists.<Attribute>newList());
    }
    ;

requires_or_provides_list:
      requires_or_provides_list_
    { $$ = $1; }
    ;

requires_or_provides_list_:
      requires_or_provides_list_ requires_or_provides
    { $$ = Lists.<Declaration>chain($1, $2); }
    | /* empty */
    { $$ = Lists.<Declaration>newList(); }
    ;

requires_or_provides:
      requires
    { $$ = Lists.<Declaration>newList($1); }
    | provides
    { $$ = Lists.<Declaration>newList($1); }
    |
      just_datadef
    {
        // TODO check if null?
        if ($1 == null) {
            $$ = Lists.<Declaration>newList();
        } else {
            $$ = Lists.<Declaration>newList($1);
        }
    }
    ;

requires:
      USES parameterised_interface_list
    {
        final RequiresInterface requires = new RequiresInterface($1.getLocation(), $2);
        // list maybe empty (erroneous but possible case)
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), AstUtils.getEndLocation($2));
        requires.setEndLocation(endLocation);
        $$ = requires;
    }
    ;

provides:
      PROVIDES parameterised_interface_list
    {
        final ProvidesInterface provides = new ProvidesInterface($1.getLocation(), $2);
        // list maybe empty (erroneous but possible case)
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), AstUtils.getEndLocation($2));
        provides.setEndLocation(endLocation);
        $$ = provides;
    }
    ;

parameterised_interface_list:
      parameterised_interface
    { $$ = Lists.<Declaration>newList($1); }
    | LBRACE parameterised_interfaces RBRACE
    { $$ = $2; }
    ;

parameterised_interfaces:
      parameterised_interfaces parameterised_interface
    { $$ = Lists.<Declaration>chain($1, $2); }
    | parameterised_interface
    { $$ = Lists.<Declaration>newList($1); }
    ;

parameterised_interface:
      just_datadef
    { $$ = $1; }
    | interface_ref nesc_attributes SEMICOLON
    {
        NescComponents.declareInterfaceRef($1, Lists.<Declaration>newList(), $2);
        $1.setEndLocation($3.getEndLocation());
        $$ = $1;
    }
    | interface_ref parameters nesc_attributes SEMICOLON
    {
        NescComponents.declareInterfaceRef($1, $2, $3);
        $1.setEndLocation($4.getEndLocation());
        // NOTICE: corresponding pushLevel() called in parameters
        popLevel();
        $$ = $1;
    }
    ;

interface_ref:
      interface_type
    { $$ = $1; }
    | interface_type AS idword
    {
        $1.setAlias($3);
        $1.setEndLocation($3.getEndLocation()); // maybe updated in higher productions
        $$ = $1;
    }
    ;

interface_type:
      INTERFACE idword
    {
        requireInterface($2);
        final InterfaceRef ifaceRef = new InterfaceRef($1.getLocation(), $2, Lists.<Expression>newList(), null,
                Lists.<Declaration>newList(), Lists.<Attribute>newList());
        ifaceRef.setEndLocation($2.getEndLocation());   // maybe updated in higher productions
        $$ = ifaceRef;
    }
    | INTERFACE idword
    {
        requireInterface($2);
    }
      LT typelist GT
    {
        final InterfaceRef ifaceRef = new InterfaceRef($1.getLocation(), $2, $5, null, Lists.<Declaration>newList(),
                Lists.<Attribute>newList());
        ifaceRef.setEndLocation($6.getEndLocation());   // maybe updated in higher productions
        $$ = ifaceRef;
    }
    ;

typelist:
      generic_type
    { $$ = Lists.<Expression>newList($1); }
    | typelist COMMA generic_type
    { $$ = Lists.<Expression>chain($1, $3); }
    ;

iconfiguration:
      IMPLEMENTATION LBRACE configuration_decls RBRACE
    {
        final ConfigurationImpl impl = new ConfigurationImpl($1.getLocation(), $3);
        impl.setEndLocation($4.getEndLocation());
        $$ = impl;
    }
    ;

cuses:
      COMPONENTS component_list SEMICOLON
    {
        final ComponentsUses cuses = new ComponentsUses($1.getLocation(), $2);
        cuses.setEndLocation($3.getEndLocation());
        $$ = cuses;
    }
    ;

component_list:
      component_list COMMA component_ref
    { $$ = Lists.<ComponentRef>chain($1, $3); }
    | component_ref
    { $$ = Lists.<ComponentRef>newList($1); }
    ;

component_ref:
      component_ref2
    {
        requireComponent($1.getName());
        /* Put component's name into parser's symbol table. */
        final String componentAlias = $1.getName().getName();
        addComponentRef(componentAlias);
    }
    | component_ref2 AS idword
    {
        requireComponent($1.getName());
        /* Put component's alias into parser's symbol table. */
        final String componentAlias = $3.getName();
        addComponentRef(componentAlias);
        $1.setEndLocation($3.getEndLocation());
    }
    ;

component_ref2:
      idword
    {
        final ComponentRef ref = new ComponentRef($1.getLocation(), $1, null, false, Lists.<Expression>newList());
        ref.setEndLocation($1.getEndLocation());   // maybe updated in higher productions
        $$ = ref;
    }
    | NEW idword LPAREN generic_args RPAREN
    {
        final ComponentRef ref = new ComponentRef($1.getLocation(), $2, null, true, $4);
        ref.setEndLocation($5.getEndLocation());   // maybe updated in higher productions
        $$ = ref;
    }
    ;

generic_args:
      /* empty */
    { $$ = Lists.<Expression>newList(); }
    | generic_arglist
    { $$ = $1; }
    ;

generic_arglist:
      generic_arg
    { $$ = Lists.<Expression>newList($1); }
    | generic_arglist COMMA generic_arg
    { $$ = Lists.<Expression>chain($1, $3); }
    ;

generic_arg:
      expr_no_commas
    {
        Expressions.defaultConversionForAssignment($1);
        $$ = $1;
    }
    | generic_type
    { $$ = $1; }
    ;

generic_type:
      typename
    { $$ = NescSemantics.makeTypeArgument($1); }
    ;

configuration_decls:
      configuration_decls configuration_decl
    { $$ = Lists.<Declaration>chain($1, $2); }
    | /* empty */
    { $$ = Lists.<Declaration>newList(); }
    ;

configuration_decl:
      connection
    { $$ = $1; }
    | just_datadef
    { $$ = $1; }
    | cuses
    { $$ = $1; }
    ;

connection:
      endpoint EQ endpoint SEMICOLON
    { $$ = new EqConnection($1.getLocation(), $1, $3); }
    | endpoint ARROW endpoint SEMICOLON
    { $$ = new RpConnection($1.getLocation(), $1, $3); }
    | endpoint LEFT_ARROW endpoint SEMICOLON
    { $$ = new RpConnection($1.getLocation(), $3, $1); }
    ;

endpoint:
      endpoint DOT parameterised_identifier
    {
        $1.setIds(Lists.<ParameterisedIdentifier>chain($1.getIds(), $3));
        $$ = $1;
    }
    | parameterised_identifier
    { $$ = new EndPoint($1.getLocation(), Lists.<ParameterisedIdentifier>newList($1)); }
    ;

parameterised_identifier:
      idword
    {
        final ParameterisedIdentifier id = new ParameterisedIdentifier($1.getLocation(), $1,
                Lists.<Expression>newList());
        id.setEndLocation($1.getEndLocation());
        $$ = id;
    }
    | idword LBRACK nonnull_exprlist RBRACK
    {
        final ParameterisedIdentifier id = new ParameterisedIdentifier($1.getLocation(), $1, $3);
        id.setEndLocation($4.getEndLocation());
        $$ = id;
    }
    ;

imodule:
      IMPLEMENTATION LBRACE extdefs RBRACE
    {
        final ModuleImpl impl = new ModuleImpl($1.getLocation(), $3);
        impl.setEndLocation($4.getEndLocation());
        $$ = impl;
    }
    ;

/* the reason for the strange actions in this rule
is so that notype_initdecls when reached via datadef
can find a valid list of type and sc specs in $0. */
// NOTICE: not sure what is happening here
extdefs:
    {
        $<TypeElement>$ = null;
        $<LinkedList>$ = Lists.<TypeElement>newList();
        wasTypedef = false;
    }
    extdef
    {
        if ($2 == null) {
            $$ = Lists.<Declaration>newList();
        } else {
            $$ = Lists.<Declaration>newList($2);
        }
    }
    | extdefs
    {
        $<TypeElement>$ = null;
        $<LinkedList>$ = Lists.<TypeElement>newList();
        wasTypedef = false;
    }
    extdef
    {
        $$ = Lists.<Declaration>chain($1, $3);
    }
    ;

extdef:
      fndef
    { $$ = $1; }
    | datadef
    { $$ = $1; }
    | ASM_KEYWORD LPAREN expr RPAREN SEMICOLON
    {
        // FIXME
        AsmStmt asmStmt = new AsmStmt(null, $3, Lists.<AsmOperand>newList(),
                Lists.<AsmOperand>newList(), Lists.<StringAst>newList(),
                Lists.<TypeElement>newList());

        $$ = new AsmDecl(null, asmStmt);
    }
    | extension extdef
    {
        $$ = Declarations.makeExtensionDecl($1.getLocation(), $2.getEndLocation(), $2);
        // pedantic = 0
    }
    ;

datadef:
      setspecs notype_initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($2).get();
        $$ = Declarations.makeDataDecl(startLocation, $3.getEndLocation(), Lists.<TypeElement>newList(), $2);
        popDeclspecStack();
    }
    | just_datadef
    { $$ = $1; }
    ;

just_datadef:
      declspecs_nots setspecs notype_initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = Declarations.makeDataDecl(startLocation, $4.getEndLocation(), $1, $3);
        popDeclspecStack();
    }
    | declspecs_ts setspecs initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = Declarations.makeDataDecl(startLocation, $4.getEndLocation(), $1, $3);
        popDeclspecStack();
    }
    | declspecs setspecs SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($3.getLocation(), $1);
        $$ = Declarations.makeDataDecl(startLocation, $3.getEndLocation(), $1, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | error SEMICOLON
    {
        $$ = Declarations.makeErrorDecl();
    }
    | error RBRACE
    {
        $$ = Declarations.makeErrorDecl();
    }
    | SEMICOLON
    {
        // FIXME: null!
        $$ = null;
    }
    | target_def
    { $$ = $1; }
    ;

//TODO
target_def:
      TARGET_DEF identifier EQ expr SEMICOLON
    {
        $$ = null;
    }
    ;

// XXX: fndef and fndef2 - my favourite productions :)
fndef:
      declspecs_ts setspecs declarator fndef2
    { $$ = $4; }
    | declspecs_nots setspecs notype_declarator fndef2
    { $$ = $4; }
    | setspecs notype_declarator fndef2
    { $$ = $3; }
    ;

fndef2:
      maybeasm maybe_attribute
    {
        /* NOTICE: maybe asm can be null! */
        /* NOTICE: maybeasm is only here to avoid a s/r conflict */
        // TODO refuse_asm

        /*
         * NOTICE: $0 refers to the declarator that precedes fndef2 in fndef
         * (we can't  just save it in an action, as that causes s/r and
         * r/r conflicts)
         */
        final Declarator declarator = $<Declarator>0;

        final Location startLocation;
        if ($1 != null) {
            startLocation = $1.getLocation();
        } else if (!$2.isEmpty()) {
            startLocation = AstUtils.getStartLocation($2).get();
        } else {
            startLocation = declarator.getLocation();
        }
        final FunctionDecl decl = Semantics.startFunction(startLocation, pstate.declspecs, declarator, $2, true);
        // TODO: detect syntax error (check !type_funcional())
        $<FunctionDecl>$ = decl;
    }
    old_style_parm_decls
    {
        $<FunctionDecl>$ = Semantics.setOldParams($<FunctionDecl>3, $4);
    }
    compstmt_or_error
    {
        $$ = Semantics.finishFunction($<FunctionDecl>5, $6);
        //popLevel();     // FIXME: to pop or not to pop? test it.
        popDeclspecStack();
    }
    ;

identifier:
      IDENTIFIER
    { $$ = $1; }
    | TYPEDEF_NAME
    { $$ = $1; }
    ;

id_label:
      identifier
    {
        final IdLabel label = new IdLabel($1.getLocation(), $1.getValue());
        label.setEndLocation($1.getEndLocation());
        $$ = label;
    }
    ;

idword:
      identifier
    {
        final Word id = new Word($1.getLocation(), $1.getValue());
        id.setEndLocation($1.getEndLocation());
        $$ = id;
    }
    ;

unop:
      AND
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.ADDRESS_OF); }
    | MINUS
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.UNARY_MINUS); }
    | PLUS
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.UNARY_PLUS); }
    | PLUSPLUS
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.PREINCREMENT); }
    | MINUSMINUS
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.PREDECREMENT); }
    | TILDE
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.BITNOT); }
    | NOT
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.NOT); }
    | REALPART
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.REALPART); }
    | IMAGPART
    { $$ = new ValueLeftUnaryOp($1.getLocation(), $1.getEndLocation(), LeftUnaryOperation.IMAGPART); }
    ;

expr:
      nonnull_exprlist
    {
        /*
         * When there is only one element in the expression list, then we
         * return only this one.
         */
        if ($1.size() == 1) {
            $$ = $1.get(0);
        } else {
            final Location startLocation = AstUtils.getStartLocation($1).get();
            final Location endLocation = AstUtils.getEndLocation($1).get();
            final Comma comma = Expressions.makeComma(startLocation, $1);
            comma.setEndLocation(endLocation);
            $$ = comma;
        }
    }
    ;

exprlist:
      /* empty */
    { $$ = Lists.<Expression>newList(); }
    | nonnull_exprlist
    { $$ = $1; }
    ;

nonnull_exprlist:
      nonnull_exprlist_
    { $$ = $1; }
    ;

nonnull_exprlist_:
      expr_no_commas
    { $$ = Lists.newList($1); }
    | nonnull_exprlist_ COMMA expr_no_commas
    { $$ = Lists.chain($1, $3); }
    ;

callkind:
      CALL
    { $$ = new ValueCallKind($1.getLocation(), $1.getEndLocation(), NescCallKind.COMMAND_CALL); }
    | SIGNAL
    { $$ = new ValueCallKind($1.getLocation(), $1.getEndLocation(), NescCallKind.EVENT_SIGNAL); }
    | POST
    { $$ = new ValueCallKind($1.getLocation(), $1.getEndLocation(), NescCallKind.POST_TASK); }
    ;

unary_expr:
      primary
    { $$ = $1; }
    | callkind function_call
    {
        final FunctionCall call = $2;
        call.setLocation($1.getLocation());
        call.setCallKind($1.getCallKind());
        $$ = $2;
    }
    | STAR cast_expr
    {
        $$ = Expressions.makeDereference($1.getLocation(), $2);
    }
    /* __extension__ turns off -pedantic for following primary.  */
    | extension cast_expr
    {
        $$ = Expressions.makeExtensionExpr($1.getLocation(), $2);
    }
    | unop cast_expr
    {
        $$ = Expressions.makeUnary($1.getLocation(), $1.getOperation(), $2);
    }
    /* Refer to the address of a label as a pointer.  */
    | ANDAND id_label
    {
        $$ = Expressions.makeLabelAddress($1.getLocation(), $2);
    }
    | sizeof unary_expr
    {
        $$ = Expressions.makeSizeofExpr($1.getLocation(), $2);
    }
    | sizeof LPAREN typename RPAREN
    {
        $$ = Expressions.makeSizeofType($1.getLocation(), $4.getEndLocation(), $3);
    }
    | alignof unary_expr
    {
        $$ = Expressions.makeAlignofExpr($1.getLocation(), $2);
    }
    | alignof LPAREN typename RPAREN
    {
        $$ = Expressions.makeAlignofType($1.getLocation(), $4.getEndLocation(), $3);
    }
    ;

sizeof:
      SIZEOF
    { $$ = $1; }
    ;

alignof:
      ALIGNOF
    { $$ = $1; }
    ;

cast_expr:
      unary_expr
    { $$ = $1; }
    | LPAREN typename RPAREN cast_expr
    {
        $$ = Expressions.makeCast($1.getLocation(), $2, $4);
    }
    | LPAREN typename RPAREN LBRACE initlist_maybe_comma RBRACE
    {
        final InitList initList = Init.makeInitList($4.getLocation(), $6.getEndLocation(), $5);
        $$ = Expressions.makeCastList($1.getLocation(), $6.getEndLocation(), $2, initList);
    }
    ;

expr_no_commas:
      cast_expr
    { $$ = $1; }
    | expr_no_commas PLUS expr_no_commas
    {
        $$ = Expressions.makePlus($1, $3);
    }
    | expr_no_commas MINUS expr_no_commas
    {
        $$ = Expressions.makeMinus($1, $3);
    }
    | expr_no_commas STAR expr_no_commas
    {
        $$ = Expressions.makeTimes($1, $3);
    }
    | expr_no_commas DIV expr_no_commas
    {
        $$ = Expressions.makeDivide($1, $3);
    }
    | expr_no_commas MOD expr_no_commas
    {
        $$ = Expressions.makeModulo($1, $3);
    }
    | expr_no_commas LSHIFT expr_no_commas
    {
        $$ = Expressions.makeLshift($1, $3);
    }
    | expr_no_commas RSHIFT expr_no_commas
    {
        $$ = Expressions.makeRshift($1, $3);
    }
    | expr_no_commas LTEQ expr_no_commas
    {
        $$ = Expressions.makeLeq($1, $3);
    }
    | expr_no_commas GTEQ expr_no_commas
    {
        $$ = Expressions.makeGeq($1, $3);
    }
    | expr_no_commas LT expr_no_commas
    {
        $$ = Expressions.makeLt($1, $3);
    }
    | expr_no_commas GT expr_no_commas
    {
        $$ = Expressions.makeGt($1, $3);
    }
    | expr_no_commas EQEQ expr_no_commas
    {
        $$ = Expressions.makeEq($1, $3);
    }
    | expr_no_commas NOTEQ expr_no_commas
    {
        $$ = Expressions.makeNe($1, $3);
    }
    | expr_no_commas AND expr_no_commas
    {
        $$ = Expressions.makeBitand($1, $3);
    }
    | expr_no_commas OR expr_no_commas
    {
        $$ = Expressions.makeBitor($1, $3);
    }
    | expr_no_commas XOR expr_no_commas
    {
        $$ = Expressions.makeBitxor($1, $3);
    }
    | expr_no_commas ANDAND expr_no_commas
    {
        $$ = Expressions.makeAndand($1, $3);
    }
    | expr_no_commas OROR expr_no_commas
    {
        $$ = Expressions.makeOror($1, $3);
    }
    | expr_no_commas QUESTION expr COLON expr_no_commas
    {
        $$ = Expressions.makeConditional($1, $3, $5);
    }
    | expr_no_commas QUESTION COLON expr_no_commas
    {
        $$ = Expressions.makeConditional($1, null, $4);
    }
    | expr_no_commas EQ expr_no_commas
    {
        $$ = Expressions.makeAssign($1, $3);
    }
    | expr_no_commas MULEQ expr_no_commas
    {
        $$ = Expressions.makeTimesAssign($1, $3);
    }
    | expr_no_commas DIVEQ expr_no_commas
    {
        $$ = Expressions.makeDivideAssign($1, $3);
    }
    | expr_no_commas MODEQ expr_no_commas
    {
        $$ = Expressions.makeModuloAssign($1, $3);
    }
    | expr_no_commas PLUSEQ expr_no_commas
    {
        $$ = Expressions.makePlusAssign($1, $3);
    }
    | expr_no_commas MINUSEQ expr_no_commas
    {
        $$ = Expressions.makeMinusAssign($1, $3);
    }
    | expr_no_commas LSHIFTEQ expr_no_commas
    {
        $$ = Expressions.makeLshiftAssign($1, $3);
    }
    | expr_no_commas RSHIFTEQ expr_no_commas
    {
        $$ = Expressions.makeRshiftAssign($1, $3);
    }
    | expr_no_commas ANDEQ expr_no_commas
    {
        $$ = Expressions.makeBitandAssign($1, $3);
    }
    | expr_no_commas XOREQ expr_no_commas
    {
        $$ = Expressions.makeBitxorAssign($1, $3);
    }
    | expr_no_commas OREQ expr_no_commas
    {
        $$ = Expressions.makeBitorAssign($1, $3);
    }
    ;

primary:
      IDENTIFIER
    {
        $$ = Expressions.makeIdentifier($1.getLocation(), $1.getEndLocation(), $1.getValue(), true);
    }
    | INTEGER_LITERAL
    {
        final LexicalCst cst = new LexicalCst($1.getLocation(), $1.getValue());
        cst.setEndLocation($1.getEndLocation());
        $$ = cst;
    }
    | FLOATING_POINT_LITERAL
    {
        final LexicalCst cst = new LexicalCst($1.getLocation(), $1.getValue());
        cst.setEndLocation($1.getEndLocation());
        $$ = cst;
    }
    | CHARACTER_LITERAL
    {
        final LexicalCst cst = new LexicalCst($1.getLocation(), $1.getValue());
        cst.setEndLocation($1.getEndLocation());
        $$ = cst;
    }
    | string
    {
        $$ = $1;
    }
    | LPAREN expr RPAREN
    {
        $2.setParens(true);
        $$ = $2;
    }
    | LPAREN error RPAREN
    {
        $$ = Expressions.makeErrorExpr();
    }
    | LPAREN compstmt RPAREN
    {
        $$ = Expressions.makeCompoundExpr($1.getLocation(), $3.getEndLocation(), $2);
    }
    | function_call
    {
    	$$ = $1;
    }
    | VA_ARG LPAREN expr_no_commas COMMA typename RPAREN
    {
    	$$ = Expressions.makeVaArg($1.getLocation(), $6.getEndLocation(), Lists.newList($3), $5);
    }
    | OFFSETOF LPAREN typename COMMA fieldlist RPAREN
    {
    	// FIXME: fieldlist
    	$$ = Expressions.makeOffsetof($1.getLocation(), $6.getEndLocation(), $3, Lists.<String>newList());
    }
    | primary LBRACK nonnull_exprlist RBRACK
    {
        // FIXME: ambiguity: array reference or generic call?
        $$ = Expressions.makeArrayRef($1.getLocation(), $4.getEndLocation(), $1, $3);
    }
    | primary DOT identifier
    {
        // FIXME: ambiguity: field reference, interface dereference, component dereference?
        $$ = Expressions.makeFieldRef($1.getLocation(), $3.getEndLocation(), $1, $3.getValue());
    }
    | primary ARROW identifier
    {
        // FIXME: ambiguity: field reference, interface dereference, component dereference?
        Expression dereference = Expressions.makeDereference($1.getLocation(), $1);
        $$ = Expressions.makeFieldRef($1.getLocation(), $3.getEndLocation(), dereference, $3.getValue());
    }
    | primary PLUSPLUS
    {
        $$ = Expressions.makePostincrement($1.getLocation(), $2.getEndLocation(), $1);
    }
    | primary MINUSMINUS
    {
        $$ = Expressions.makePostdecrement($1.getLocation(), $2.getEndLocation(), $1);
    }
    ;

fieldlist:
      identifier
    { $$ = Lists.<Symbol>newList($1); }
    | fieldlist DOT identifier
    { $$ = Lists.chain($1, $3); }
    ;

function_call:
      primary LPAREN exprlist RPAREN
    { $$ = Expressions.makeFunctionCall($1.getLocation(), $4.getEndLocation(), $1, $3); }
    ;

string:
      string_chain
    { $$ = $1; }
    | MAGIC_STRING
    { $$ = Expressions.makeIdentifier($1.getLocation(), $1.getEndLocation(), $1.getValue(), false); }
  ;

/*
* The so called K&R style or old-style declaration is still supported.
* It was used before C was standarised (ANSI). This kind of declaration can
* be found in old, legacy code. (I do not believe nesC source code contains
* such code :) ).
*
* Example:
*     void f(i, c, fp)
*     int i;
*     char c;
*     float *fp;
*     {
*         ...
*     }
*
*/
old_style_parm_decls:
      /* empty */
    { $$ = Lists.<Declaration>newList(); }
    | datadecls
    { $$ = $1; }
    | datadecls ELLIPSIS
    {
        final EllipsisDecl ellipsis = new EllipsisDecl($2.getLocation());
        ellipsis.setEndLocation($2.getEndLocation());
        $$ = Lists.<Declaration>chain($1, ellipsis);
    }
    ;

/*
 * The following are analogous to decls and decl except that they do not
 * allow nested functions. They are used for old-style parm decls.
 */
datadecls:
      datadecl
    { $$ = Lists.<Declaration>newList($1); }
    | datadecls datadecl
    { $$ = Lists.<Declaration>chain($1, $2); }
    ;

/*
 * NOTICE: We don't allow prefix attributes here because they cause
 * reduce/reduce conflicts: we can't know whether we're parsing a function
 * decl with attribute suffix, or function defn with attribute prefix on first
 * old style parm.
 */
datadecl:
      declspecs_ts_nosa setspecs initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = Declarations.makeDataDecl(startLocation, $4.getEndLocation(), $1, $3);
        popDeclspecStack();
    }
    | declspecs_nots_nosa setspecs notype_initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = Declarations.makeDataDecl(startLocation, $4.getEndLocation(), $1, $3);
        popDeclspecStack();
    }
    | declspecs_ts_nosa setspecs SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($3.getLocation(), $1);
        $$ = Declarations.makeDataDecl(startLocation, $3.getEndLocation(), $1, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | declspecs_nots_nosa SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($2.getLocation(), $1);
        $$ = Declarations.makeDataDecl(startLocation, $2.getEndLocation(), $1, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    ;

/*
 * This combination which saves a lineno before a decl is the normal thing to
 * use, rather than decl itself. This is to avoid shift/reduce conflicts in
 * contexts where statement labels are allowed.
 */
decls:
      decl
    { $$ = Lists.<Declaration>newList($1); }
    | errstmt
    { $$ = Lists.<Declaration>newList(Declarations.makeErrorDecl()); }
    | decls decl
    { $$ = Lists.<Declaration>chain($1, $2); }
    | decl errstmt
    { $$ = Lists.<Declaration>newList(Declarations.makeErrorDecl()); }
    ;

/*
 * Records the type and storage class specs to use for processing the
 * declarators that follow.
 * Maintains a stack of outer-level values of pstate.declspecs, for the sake
 * of parm declarations nested in function declarators.
 */
setspecs:
    /* empty */
    {
        LOG.trace("setspecs");
        pushDeclspecStack();
         // the preceding element in production containing setspecs
        final LinkedList<TypeElement> list = (LinkedList<TypeElement>) $<Object>0;
        if (list == null || list.isEmpty()) {
            pstate.declspecs = Lists.<TypeElement>newList();
            pstate.wasTypedef = false;
        } else {
            // FIXME: ugly workaround for $<LinkedList<TypeElement>>0
            // bison does not handle <<>>
            pstate.declspecs = list;
            pstate.wasTypedef = wasTypedef;
        }
        pstate.attributes = Lists.<Attribute>newList();
        // TODO: check why we are not making $<telements$ an empty list
        wasTypedef = false;
        LOG.trace("Setting wasTypedef to false.");
    }
    ;

/*
 * Possibly attributes after a comma, which should be saved in
 * pstate.attributes.
 */
maybe_resetattrs:
      maybe_attribute
    {
        pstate.attributes = $1;
    }
    ;

decl:
      declspecs_ts setspecs initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = Declarations.makeDataDecl(startLocation, $4.getEndLocation(), $1, $3);
        popDeclspecStack();
    }
    | declspecs_nots setspecs notype_initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = Declarations.makeDataDecl(startLocation, $4.getEndLocation(), $1, $3);
        popDeclspecStack();
    }
    | declspecs_ts setspecs nested_function
    {
        $$ = $3;
        popDeclspecStack();
    }
    | declspecs_nots setspecs notype_nested_function
    {
        $$ = $3;
        popDeclspecStack();
    }
    | declspecs setspecs SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($3.getLocation(), $1);
        $$ = Declarations.makeDataDecl(startLocation, $3.getEndLocation(), $1, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | extension decl
    {
        $$ = Declarations.makeExtensionDecl($1.getLocation(), $2.getEndLocation(), $2);
        // pedantic = $1.i
    }
    ;

/* declspecs borrowed from gcc 3. I think it's really ugly, but I guess
 they (and therefore I) am stuck with this brokenness.
 The only redeeming feature is that it's cleaner than gcc 2
*/
/* A list of declaration specifiers.  These are:

 - Storage class specifiers (SCSPEC), which for GCC currently include
 function specifiers ("inline").

 - Type specifiers (type_spec_*).

 - Type qualifiers (TYPE_QUAL).

 - Attribute specifier lists (attributes).

 These are stored as a TREE_LIST; the head of the list is the last
 item in the specifier list.  Each entry in the list has either a
 TREE_PURPOSE that is an attribute specifier list, or a TREE_VALUE that
 is a single other specifier or qualifier; and a TREE_CHAIN that is the
 rest of the list.  TREE_STATIC is set on the list if something other
 than a storage class specifier or attribute has been seen; this is used
 to warn for the obsolescent usage of storage class specifiers other than
 at the start of the list.  (Doing this properly would require function
 specifiers to be handled separately from storage class specifiers.)

 The various cases below are classified according to:

 (a) Whether a storage class specifier is included or not; some
 places in the grammar disallow storage class specifiers (_sc or _nosc).

 (b) Whether a type specifier has been seen; after a type specifier,
 a typedef name is an identifier to redeclare (_ts or _nots).

 (c) Whether the list starts with an attribute; in certain places,
 the grammar requires specifiers that don't start with an attribute
 (_sa or _nosa).

 (d) Whether the list ends with an attribute (or a specifier such that
 any following attribute would have been parsed as part of that specifier);
 this avoids shift-reduce conflicts in the parsing of attributes
 (_ea or _noea).

 TODO:

 (i) Distinguish between function specifiers and storage class specifiers,
 at least for the purpose of warnings about obsolescent usage.

 (ii) Halve the number of productions here by eliminating the _sc/_nosc
 distinction and instead checking where required that storage class
 specifiers aren't present.  */

declspecs_nosc_nots_nosa_noea:
      type_qual
    { $$ = Lists.<TypeElement>newList($1); }
    | declspecs_nosc_nots_nosa_noea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_nosa_ea type_qual
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_nosc_nots_nosa_ea:
      declspecs_nosc_nots_nosa_noea eattributes
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_nosc_nots_sa_noea:
      declspecs_nosc_nots_sa_noea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_sa_ea type_qual
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_nosc_nots_sa_ea:
      eattributes
    { $$ = $1; }
    | declspecs_nosc_nots_sa_noea eattributes
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_nosc_ts_nosa_noea:
      type_spec_nonattr
    { $$ = Lists.<TypeElement>newList($1); }
    | declspecs_nosc_ts_nosa_noea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_nosa_ea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_nosa_noea type_spec_reserved_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_nosa_ea type_spec_reserved_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_nosa_noea type_spec_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_nosa_ea type_spec_nonattr
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_nosc_ts_nosa_ea:
      type_spec_attr
    { $$ = Lists.<TypeElement>newList($1); }
    | declspecs_nosc_ts_nosa_noea eattributes
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_nosa_noea type_spec_reserved_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_nosa_ea type_spec_reserved_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_nosa_noea type_spec_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_nosa_ea type_spec_attr
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_nosc_ts_sa_noea:
      declspecs_nosc_ts_sa_noea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_sa_ea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_sa_noea type_spec_reserved_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_sa_ea type_spec_reserved_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_sa_noea type_spec_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_sa_ea type_spec_nonattr
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_nosc_ts_sa_ea:
      declspecs_nosc_ts_sa_noea eattributes
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_sa_noea type_spec_reserved_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_sa_ea type_spec_reserved_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_sa_noea type_spec_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_sa_ea type_spec_attr
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_sc_nots_nosa_noea:
      scspec
    { $$ = Lists.<TypeElement>newList($1); }
    | declspecs_sc_nots_nosa_noea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_nosa_ea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_nosa_noea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_nosa_ea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_nosa_noea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_nosa_ea scspec
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_sc_nots_nosa_ea:
      declspecs_sc_nots_nosa_noea eattributes
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_sc_nots_sa_noea:
      declspecs_sc_nots_sa_noea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_sa_ea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_sa_noea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_nots_sa_ea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_sa_noea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_sa_ea scspec
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_sc_nots_sa_ea:
      declspecs_sc_nots_sa_noea eattributes
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_sc_ts_nosa_noea:
      declspecs_sc_ts_nosa_noea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_nosa_ea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_nosa_noea type_spec_reserved_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_nosa_ea type_spec_reserved_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_nosa_noea type_spec_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_nosa_ea type_spec_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_nosa_noea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_nosa_ea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_nosa_noea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_nosa_ea scspec
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_sc_ts_nosa_ea:
      declspecs_sc_ts_nosa_noea eattributes
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_nosa_noea type_spec_reserved_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_nosa_ea type_spec_reserved_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_nosa_noea type_spec_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_nosa_ea type_spec_attr
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_sc_ts_sa_noea:
      declspecs_sc_ts_sa_noea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_sa_ea type_qual
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_sa_noea type_spec_reserved_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_sa_ea type_spec_reserved_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_sa_noea type_spec_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_sa_ea type_spec_nonattr
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_sa_noea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_nosc_ts_sa_ea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_sa_noea scspec
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_sa_ea scspec
    { $$ = Lists.chain($1, $2); }
    ;

declspecs_sc_ts_sa_ea:
      declspecs_sc_ts_sa_noea eattributes
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_sa_noea type_spec_reserved_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_ts_sa_ea type_spec_reserved_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_sa_noea type_spec_attr
    { $$ = Lists.chain($1, $2); }
    | declspecs_sc_nots_sa_ea type_spec_attr
    { $$ = Lists.chain($1, $2); }
    ;

/* Particular useful classes of declspecs.  */
declspecs_ts:
      declspecs_nosc_ts_nosa_noea
    { $$ = $1; }
    | declspecs_nosc_ts_nosa_ea
    { $$ = $1; }
    | declspecs_nosc_ts_sa_noea
    { $$ = $1; }
    | declspecs_nosc_ts_sa_ea
    { $$ = $1; }
    | declspecs_sc_ts_nosa_noea
    { $$ = $1; }
    | declspecs_sc_ts_nosa_ea
    { $$ = $1; }
    | declspecs_sc_ts_sa_noea
    { $$ = $1; }
    | declspecs_sc_ts_sa_ea
    { $$ = $1; }
    ;

declspecs_nots:
      declspecs_nosc_nots_nosa_noea
    { $$ = $1; }
    | declspecs_nosc_nots_nosa_ea
    { $$ = $1; }
    | declspecs_nosc_nots_sa_noea
    { $$ = $1; }
    | declspecs_nosc_nots_sa_ea
    { $$ = $1; }
    | declspecs_sc_nots_nosa_noea
    { $$ = $1; }
    | declspecs_sc_nots_nosa_ea
    { $$ = $1; }
    | declspecs_sc_nots_sa_noea
    { $$ = $1; }
    | declspecs_sc_nots_sa_ea
    { $$ = $1; }
    ;

declspecs_ts_nosa:
      declspecs_nosc_ts_nosa_noea
    { $$ = $1; }
    | declspecs_nosc_ts_nosa_ea
    { $$ = $1; }
    | declspecs_sc_ts_nosa_noea
    { $$ = $1; }
    | declspecs_sc_ts_nosa_ea
    { $$ = $1; }
    ;

declspecs_nots_nosa:
      declspecs_nosc_nots_nosa_noea
    { $$ = $1; }
    | declspecs_nosc_nots_nosa_ea
    { $$ = $1; }
    | declspecs_sc_nots_nosa_noea
    { $$ = $1; }
    | declspecs_sc_nots_nosa_ea
    { $$ = $1; }
    ;

declspecs_nosc_ts:
      declspecs_nosc_ts_nosa_noea
    { $$ = $1; }
    | declspecs_nosc_ts_nosa_ea
    { $$ = $1; }
    | declspecs_nosc_ts_sa_noea
    { $$ = $1; }
    | declspecs_nosc_ts_sa_ea
    { $$ = $1; }
    ;

declspecs_nosc_nots:
      declspecs_nosc_nots_nosa_noea
    { $$ = $1; }
    | declspecs_nosc_nots_nosa_ea
    { $$ = $1; }
    | declspecs_nosc_nots_sa_noea
    { $$ = $1; }
    | declspecs_nosc_nots_sa_ea
    { $$ = $1; }
    ;

declspecs_nosc:
      declspecs_nosc_ts
    { $$ = $1; }
    | declspecs_nosc_nots
    { $$ = $1; }
    ;

declspecs:
      declspecs_ts
    { $$ = $1; }
    | declspecs_nots
    { $$ = $1; }
    ;

/* A (possibly empty) sequence of type qualifiers and attributes.  */
maybe_type_quals_attrs:
      /* empty */
    { $$ = Lists.<TypeElement>newList(); }
    | declspecs_nosc_nots
    { $$ = $1; }
    ;

/* A type specifier (but not a type qualifier).
 Once we have seen one of these in a declaration,
 if a typedef name appears then it is being redeclared.

 The _reserved versions start with a reserved word and may appear anywhere
 in the declaration specifiers; the _nonreserved versions may only
 appear before any other type specifiers, and after that are (if names)
 being redeclared.

 FIXME: should the _nonreserved version be restricted to names being
 redeclared only?  The other entries there relate only the GNU extensions
 and Objective C, and are historically parsed thus, and don't make sense
 after other type specifiers, but it might be cleaner to count them as
 _reserved.

 _attr means: specifiers that either end with attributes,
 or are such that any following attributes would
 be parsed as part of the specifier.

 _nonattr: specifiers.  */

type_spec_nonattr:
      type_spec_reserved_nonattr
    { $$ = $1; }
    | type_spec_nonreserved_nonattr
    { $$ = $1; }
    ;

type_spec_attr:
      type_spec_reserved_attr
    { $$ = $1; }
    ;

type_spec_reserved_nonattr:
      type_spec
    { $$ = $1; }
    | structuse
    { $$ = $1; }
    ;

type_spec_reserved_attr:
      structdef
    { $$ = $1; }
    ;

type_spec_nonreserved_nonattr:
      TYPEDEF_NAME
    {
        final Typename typename = new Typename($1.getLocation(), $1.getValue());
        typename.setEndLocation($1.getEndLocation());
        $$ = typename;

    }
    | COMPONENTREF DOT identifier
    {
        final ComponentTyperef ref = new ComponentTyperef($1.getLocation(), $1.getValue(), $3.getValue());
        ref.setEndLocation($3.getEndLocation());
        $$ = ref;

    }
    | TYPEOF LPAREN expr RPAREN
    {
        final TypeofExpr typeof = new TypeofExpr($1.getLocation(), $3);
        typeof.setEndLocation($4.getEndLocation());
        $$ = typeof;
    }
    | TYPEOF LPAREN typename RPAREN
    {
        final TypeofType typeof = new TypeofType(null, $3);
        typeof.setEndLocation($4.getEndLocation());
        $$ = typeof;
    }
    ;
/* type_spec_nonreserved_attr does not exist.  */

initdecls:
      initdecls_
    { $$ = $1; }
    ;

notype_initdecls:
      notype_initdecls_
    { $$ = $1; }
    ;

initdecls_:
      initdcl
    { $$ = Lists.<Declaration>newList($1); }
    | initdecls_ COMMA maybe_resetattrs initdcl
    { $$ = Lists.<Declaration>chain($1, $4); }
    ;

notype_initdecls_:
      notype_initdcl
    { $$ = Lists.newList($1); }
    | notype_initdecls_ COMMA maybe_resetattrs initdcl
    { $$ = Lists.chain($1, $4); }
    ;

maybeasm:
      /* empty */
    {
        $$ = null;
    }
    | ASM_KEYWORD LPAREN string_chain RPAREN
    {
        // FIXME AsmStms
        $$ = new AsmStmt(null, null, null, null, null, null);
    }
    ;

initdcl:
      declarator maybeasm maybe_attribute EQ
    {
        /*
         * The reason for the strange assignment below is the fact that
         * to handle action inside right side of production, yacc implicitly
         * splits current production into two separate rules. The $$ symbol
         * probably cannot be used in anonymous production.
         */
        declareName($1, pstate.declspecs);
        final VariableDecl decl = Declarations.startDecl($1, Optional.fromNullable($2), pstate.declspecs,
                prefixAttr($3), true);
        $<VariableDecl>$ = decl;
    }
      init
    {
          /*
           * $<declaration>5 : The result of anonymous rule is the fifth
           * element in the right-hand side of production.
           */
          final VariableDecl decl = $<VariableDecl>5;
          $$ = Declarations.finishDecl(decl, Optional.of($6));
    }
/* Note how the declaration of the variable is in effect while its init is parsed! */
    | declarator maybeasm maybe_attribute
    {
        declareName($1, pstate.declspecs);
        final VariableDecl decl = Declarations.startDecl($1, Optional.fromNullable($2),  pstate.declspecs,
                prefixAttr($3), false);
        $$ = Declarations.finishDecl(decl, Optional.<Expression>absent());
    }
    ;

notype_initdcl:
      notype_declarator maybeasm maybe_attribute EQ
    {
        declareName($1, pstate.declspecs);
        final VariableDecl decl = Declarations.startDecl($1, Optional.fromNullable($2), pstate.declspecs,
                prefixAttr($3), true);
        $<VariableDecl>$ = decl;
    }
      init
    {
          final VariableDecl decl = $<VariableDecl>5;
          $$ = Declarations.finishDecl(decl, Optional.of($6));
    }
/* Note how the declaration of the variable is in effect while its init is parsed! */
    | notype_declarator maybeasm maybe_attribute
    {
        declareName($1, pstate.declspecs);
        VariableDecl decl = Declarations.startDecl($1, Optional.fromNullable($2),  pstate.declspecs, prefixAttr($3),
                false);
        $$ = Declarations.finishDecl(decl, Optional.<Expression>absent());
    }
    ;

maybe_attribute:
      /* empty */
    { $$ = Lists.<Attribute>newList(); }
    | attributes
    { $$ = $1; }
    ;

eattributes:
      attributes
    { $$ = Lists.<Attribute, TypeElement>convert($1); }
    ;

nesc_attributes:
      /* empty */
    { $$ = Lists.<Attribute>newList(); }
    | nesc_attributes nattrib
    { $$ = Lists.<Attribute>chain($1, $2); }
    ;

attributes:
      attribute
    {
        // NOTICE: Attribute returns a list of attributes.
        $$ = $1;
    }
    | attributes attribute
    { $$ = Lists.chain($1, $2); }
    ;

attribute:
      ATTRIBUTE LPAREN LPAREN attribute_list RPAREN RPAREN
    {
        // TODO: wrapper for such attributes
        $$ = $4;
    }
    | target_attribute
    { $$ = Lists.<Attribute>newList($1); }
    | nattrib
    { $$ = Lists.<Attribute>newList($1); }
    ;

target_attribute:
      TARGET_ATTRIBUTE0
    {
        final Word w = new Word($1.getLocation(), $1.getValue());
        w.setEndLocation($1.getEndLocation());
        final TargetAttribute attribute = new TargetAttribute($1.getLocation(), w, Lists.<Expression>newList());
        attribute.setEndLocation($1.getEndLocation());
        $$ = attribute;
    }
    | TARGET_ATTRIBUTE1 restricted_expr
    {
        final Word w = new Word($1.getLocation(), $1.getValue());
        w.setEndLocation($1.getEndLocation());
        final TargetAttribute attribute = new TargetAttribute($1.getLocation(), w, Lists.newList($2));
        attribute.setEndLocation($2.getEndLocation());
        $$ = attribute;
    }
    | AT restricted_expr
    {
        final Word w = new Word(Location.getDummyLocation(), "iar_at");
        w.setEndLocation(Location.getDummyLocation());
        final TargetAttribute attribute = new TargetAttribute($1.getLocation(), w, Lists.newList($2));
        attribute.setEndLocation($2.getEndLocation());
        $$ = attribute;
    }
    ;

restricted_expr:
      INTEGER_LITERAL
    {
        $$ = new LexicalCst($1.getLocation(), $1.getValue());
    }
    | FLOATING_POINT_LITERAL
    {
        $$ = new LexicalCst($1.getLocation(), $1.getValue());
    }
    | CHARACTER_LITERAL
    {
        $$ = new LexicalCst($1.getLocation(), $1.getValue());
    }
    | string
    { $$ = $1; }
    | LPAREN expr RPAREN
    { $$ = $2; }
    ;

attribute_list:
      attrib
    {
        /* NOTE: attrib can be null! */
        if ($1 == null) {
            $$ = Lists.<Attribute>newList();
        } else {
            $$ = Lists.<Attribute>newList($1);
        }
    }
    | attribute_list COMMA attrib
    {
        if ($3 == null) {
            $$ = $1;
        } else {
            $$ = Lists.chain($1, $3);
        }
    }
    ;

attrib:
      /* empty */
    { $$ = null; }
    | any_word
    {
        final GccAttribute attribute = new GccAttribute($1.getLocation(), $1, null);
        attribute.setEndLocation($1.getEndLocation());
        $$ = attribute;
    }
    | any_word LPAREN IDENTIFIER RPAREN
    {
        final Identifier id = new Identifier($3.getLocation(), $3.getValue());
        id.setEndLocation($3.getEndLocation());
        final GccAttribute attribute = new GccAttribute($1.getLocation(), $1, Lists.<Expression>newList(id));
        attribute.setEndLocation($4.getEndLocation());
        $$ = attribute;
    }
    | any_word LPAREN IDENTIFIER COMMA nonnull_exprlist RPAREN
    {
        final Identifier id = new Identifier($3.getLocation(), $3.getValue());
        id.setEndLocation($3.getEndLocation());
        final GccAttribute attribute = new GccAttribute($1.getLocation(), $1, Lists.<Expression>chain($5, id));
        attribute.setEndLocation($6.getEndLocation());
        $$ = attribute;
    }
    | any_word LPAREN exprlist RPAREN
    {
        final GccAttribute attribute = new GccAttribute($1.getLocation(), $1, $3);
        attribute.setEndLocation($4.getEndLocation());
        $$ = attribute;
    }
    ;

nattrib:
      AT nastart LPAREN initlist_maybe_comma RPAREN
    {
        $$ = NescAttributes.finishAttributeUse($1.getLocation(), $5.getEndLocation(), $2, $4);
    }
    | AT nastart error RPAREN
    {
        $$ = NescAttributes.finishAttributeUse($1.getLocation(), $4.getEndLocation(), $2,
                Lists.<Expression>newList(Expressions.makeErrorExpr()));
    }
    ;

nastart:
      idword
    { $$ = NescAttributes.startAttributeUse($1); }
    ;

/* This still leaves out most reserved keywords,
 shouldn't we include them?  */

any_word:
      idword
    { $$ = $1; }
    | scspec
    {
        final Word w = new Word($1.getLocation(), $1.getId().getName());
        w.setEndLocation($1.getEndLocation());
        $$ = w;
    }
    | type_spec
    {
        final Word w = new Word($1.getLocation(), $1.getId().getName());
        w.setEndLocation($1.getEndLocation());
        $$ = w;
    }
    | type_qual
    {
        final Word w = new Word($1.getLocation(), $1.getId().getName());
        w.setEndLocation($1.getEndLocation());
        $$ = w;
    }
    | SIGNAL
    {
        // FIXME: strange production
        final Word w = new Word($1.getLocation(), "signal");
        w.setEndLocation($1.getEndLocation());
        $$ = w;
    }
    ;

/* Initializers.  `init' is the entry point.  */

init:
      expr_no_commas
    {
        $$ = $1;
    }
    | LBRACE initlist_maybe_comma RBRACE
    {
        $$ = Init.makeInitList($1.getLocation(), $3.getEndLocation(), $2);
    }
    | error
    {
        $$ = Expressions.makeErrorExpr();
    }
    ;

/* `initlist_maybe_comma' is the guts of an initializer in braces.  */
initlist_maybe_comma:
      /* empty */
    { $$ = Lists.<Expression>newList(); }
    | initlist1 maybecomma
    { $$ = $1; }
    ;

initlist1:
      initelt
    { $$ = Lists.<Expression>newList($1); }
    | initlist1 COMMA initelt
    { $$ = Lists.<Expression>chain($1, $3); }
    ;

/* `initelt' is a single element of an initializer. It may use braces.  */
initelt:
      designator_list EQ initval
    {
        final Location startLocation = AstUtils.getStartLocation($1).get();
        $$ = Init.makeInitSpecific(startLocation, $3.getEndLocation(), $1, $3);
    }
    | designator initval
    {
        $$ = Init.makeInitSpecific($1.getLocation(), $2.getEndLocation(), $1, $2);
    }
    | identifier COLON initval
    {
        final Designator designator = Init.setInitLabel($1.getLocation(), $1.getEndLocation(), $1.getValue());
        $$ = Init.makeInitSpecific($1.getLocation(), $3.getEndLocation(), designator, $3);
    }
    | initval
    { $$ = $1; }
    ;

initval:
      LBRACE initlist_maybe_comma RBRACE
    {
        $$ = Init.makeInitList($1.getLocation(), $3.getEndLocation(), $2);
    }
    | expr_no_commas
    {
        $$ = $1;
    }
    | error
    {
        $$ = Expressions.makeErrorExpr();
    }
    ;

designator_list:
      designator
    { $$ = Lists.<Designator>newList($1); }
    | designator_list designator
    { $$ = Lists.<Designator>chain($1, $2); }
    ;

designator:
      DOT identifier
    {
        $$ = Init.setInitLabel($2.getLocation(), $2.getEndLocation(), $2.getValue());
    }
    /* These are for labeled elements.  The syntax for an array element
       initializer conflicts with the syntax for an Objective-C message,
       so don't include these productions in the Objective-C grammar.  */
    | LBRACK expr_no_commas ELLIPSIS expr_no_commas RBRACK
    {
        $$ = Init.setInitIndex($1.getLocation(), $5.getEndLocation(), $2, $4);
    }
    | LBRACK expr_no_commas RBRACK
    {
        $$ = Init.setInitIndex($1.getLocation(), $3.getEndLocation(), $2, null);
    }
    ;

nested_function:
      declarator maybeasm maybe_attribute
    {
        /* NOTICE: maybe asm can be null! */
        /* NOTICE: maybeasm is only here to avoid a s/r conflict */
        // TODO refuse_asm

        final FunctionDecl decl = Semantics.startFunction($1.getLocation(), pstate.declspecs, $1, $3, true);
        // TODO: detect syntax error (check !type_funcional())
        $<FunctionDecl>$ = decl;
    }
      old_style_parm_decls
    {
        $<FunctionDecl>$ = Semantics.setOldParams($<FunctionDecl>4, $5);
    }
    /*
     * This used to use compstmt_or_error. That caused a bug with input
     * `f(g) int g {}', where the use of YYERROR1 above caused an error which
     * then was handled by compstmt_or_error. There followed a repeated
     * execution of that same rule, which called YYERROR1 again, and so on.
     */
      compstmt
    {
        $$ = Semantics.finishFunction($<FunctionDecl>6, $7);
        //popLevel();   // FIXME: to pop or not to pop? test it.
    }
    ;

notype_nested_function:
      notype_declarator maybeasm maybe_attribute
    {
        /* NOTICE: maybe asm can be null! */
        /* NOTICE: maybeasm is only here to avoid a s/r conflict */
        // TODO: refuse_asm

        final FunctionDecl decl = Semantics.startFunction($1.getLocation(), pstate.declspecs, $1, $3, true);
        // TODO: detect syntax error (check !type_funcional())
        $<FunctionDecl>$ = decl;
    }
      old_style_parm_decls
    {
        $<FunctionDecl>$ = Semantics.setOldParams($<FunctionDecl>4, $5);
    }
    /*
     * This used to use compstmt_or_error. That caused a bug with input
     * `f(g) int g {}', where the use of YYERROR1 above caused an error which
     * then was handled by compstmt_or_error. There followed a repeated
     * execution of that same rule, which called YYERROR1 again, and so on.
     */
      compstmt
    {
        $$ = Semantics.finishFunction($<FunctionDecl>6, $7);
        //popLevel();   // FIXME: to pop or not to pop? test it.
    }
    ;

/*
 * Any kind of declarator (thus, all declarators allowed after an explicit
 * type_spec).
 */

declarator:
      after_type_declarator
    { $$ = $1; }
    | notype_declarator
    { $$ = $1; }
    ;

/* A declarator that is allowed only after an explicit type_spec.  */

after_type_declarator:
      after_type_declarator array_or_fn_declarator
    {
        // TODO make function for this (duplicated in 4 places)
        final Declarator declarator = Semantics.finishArrayOrFnDeclarator($1, $2);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            popLevel();
        }
        $$ = declarator;
    }
    | STAR maybe_type_quals_attrs after_type_declarator
    {
        $$ = Semantics.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    | LPAREN maybe_attribute after_type_declarator RPAREN
    {
        final QualifiedDeclarator decl = new QualifiedDeclarator($1.getLocation(), $3,
                Lists.<Attribute, TypeElement>convert($2));
        decl.setEndLocation($4.getEndLocation());
        $$ = decl;
    }
    | TYPEDEF_NAME
    {
        final IdentifierDeclarator decl = new IdentifierDeclarator($1.getLocation(), $1.getValue());
        decl.setEndLocation($1.getEndLocation());
        $$ = decl;
    }
    | TYPEDEF_NAME DOT identifier
    {
        $$ = NescModule.makeInterfaceRefDeclarator($1.getLocation(), $1.getValue(),
                $3.getLocation(), $3.getEndLocation(), $3.getValue());
    }
    ;

/*
 * Kinds of declarator that can appear in a parameter list in addition
 * to notype_declarator.  This is like after_type_declarator but does not
 * allow a typedef name in parentheses as an identifier (because it would
 * conflict with a function with that typedef as arg).
 */
parm_declarator:
      parm_declarator array_or_fn_declarator
    {
        final Declarator declarator = Semantics.finishArrayOrFnDeclarator($1, $2);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            popLevel();
        }
        $$ = declarator;
    }
    | STAR maybe_type_quals_attrs parm_declarator
    {
        $$ = Semantics.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    | TYPEDEF_NAME
    {
        final IdentifierDeclarator decl = new IdentifierDeclarator($1.getLocation(), $1.getValue());
        decl.setEndLocation($1.getEndLocation());
        $$ = decl;
    }
    ;


/*
 * A declarator allowed whether or not there has been an explicit type_spec.
 * These cannot redeclare a typedef-name.
 */

notype_declarator:
      notype_declarator array_or_fn_declarator
    {
        final Declarator declarator = Semantics.finishArrayOrFnDeclarator($1, $2);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            popLevel();
        }
        $$ = declarator;
    }
    | STAR maybe_type_quals_attrs notype_declarator
    {
        $$ = Semantics.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    | LPAREN maybe_attribute notype_declarator RPAREN
    {
        final QualifiedDeclarator decl = new QualifiedDeclarator($1.getLocation(), $3,
                Lists.<Attribute, TypeElement>convert($2));
        decl.setEndLocation($4.getEndLocation());
        $$ = decl;
    }
    | IDENTIFIER
    {
        final IdentifierDeclarator decl = new IdentifierDeclarator($1.getLocation(), $1.getValue());
        decl.setEndLocation($1.getEndLocation());
        $$ = decl;
    }
    | IDENTIFIER DOT identifier
    {
        $$ = NescModule.makeInterfaceRefDeclarator($1.getLocation(), $1.getValue(),
               $3.getLocation(), $3.getEndLocation(), $3.getValue());
    }
    ;

tag:
      identifier
    {
        final Word word = new Word($1.getLocation(), $1.getValue());
        word.setEndLocation($1.getEndLocation());
        $$ = word;
    }
    ;

// NOTICE: nesc_attributes are ignored!
structuse:
      structkind tag nesc_attributes
    {
        // TODO: make warning "attributes ignored"
        $$ = Semantics.makeXrefTag($1.getLocation(), $2.getEndLocation(), $1.getKind(), $2);
    }
    | ENUM tag nesc_attributes
    {
        // TODO: make warning "attributes ignored"
        $$ = Semantics.makeXrefTag($1.getLocation(), $2.getEndLocation(), StructKind.ENUM, $2);
    }
    ;

/*
 * TODO: not sure what happens with nesc_attributes when struct creation
 * is not split into two phases (startStruct(), finishStruct()).
 */

structdef:
      structkind tag nesc_attributes LBRACE component_decl_list RBRACE maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($6.getEndLocation(), $7);
        $$ = Semantics.makeStruct($1.getLocation(), endLocation, $1.getKind(), Optional.of($2), $5,
                Lists.<Attribute>chain($3, $7));
    }
    | STRUCT AT tag nesc_attributes LBRACE component_decl_list RBRACE maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($7.getEndLocation(), $8);
        $$ = Semantics.makeStruct($1.getLocation(), endLocation, StructKind.ATTRIBUTE, Optional.of($3), $6,
                Lists.<Attribute>chain($4, $8));
    }
    | structkind LBRACE component_decl_list RBRACE maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($4.getEndLocation(), $5);
        $$ = Semantics.makeStruct($1.getLocation(), endLocation, $1.getKind(), Optional.<Word>absent(), $3, $5);
    }
    | ENUM tag nesc_attributes LBRACE enumlist maybecomma_warn RBRACE maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($7.getEndLocation(), $8);
        $$ = Semantics.makeEnum($1.getLocation(), endLocation, Optional.of($2), $5, Lists.<Attribute>chain($3, $8));
    }
    | ENUM LBRACE enumlist maybecomma_warn RBRACE maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($5.getEndLocation(), $6);
        $$ = Semantics.makeEnum($1.getLocation(), endLocation, Optional.<Word>absent(), $3, $6);
    }
    ;

//FIXME
structkind:
      STRUCT
    {
        $$ = new ValueStructKind($1.getLocation(), $1.getEndLocation(), StructKind.STRUCT);
    }
    | UNION
    {
        $$ = new ValueStructKind($1.getLocation(), $1.getEndLocation(), StructKind.UNION);
    }
    | NX_STRUCT
    {
        $$ = new ValueStructKind($1.getLocation(), $1.getEndLocation(), StructKind.NX_STRUCT);
    }
    | NX_UNION
    {
        $$ = new ValueStructKind($1.getLocation(), $1.getEndLocation(), StructKind.NX_UNION);
    }
    ;

maybecomma:
      /* empty */
    | COMMA
    ;

// TODO: pedantic warn "comma at end of enumerator list"
maybecomma_warn:
      /* empty */
    | COMMA
    ;

component_decl_list:
      component_decl_list2
    { $$ = $1; }
    | component_decl_list2 component_decl
    {
        $$ = Lists.<Declaration>chain($1, $2);
        // TODO: pedantic warn "no semicolon at end of struct or union"
    }
    ;

component_decl_list2:
      /* empty */
    { $$ = Lists.<Declaration>newList(); }
    | component_decl_list2 component_decl SEMICOLON
    { $$ = Lists.<Declaration>chain($1, $2); }
    | component_decl_list2 SEMICOLON
    {
        $$ = $1;
        // TODO pedantic "extra semicolon in struct or union specified"
    }
    ;

/* There is a shift-reduce conflict here, because `components' may start
 * with a `typename'.  It happens that shifting (the default resolution)
 * does the right thing, because it treats the `typename' as part of
 * a `typed_type_specs'.

 * It is possible that this same technique would allow the distinction
 * between `notype_initdecls' and `initdecls' to be eliminated.
 * But I am being cautious and not trying it.
 */

component_decl:
      declspecs_nosc_ts setspecs components
    {
        final Location startLocation = AstUtils.getStartLocation($1, $3).get();
        final Location endLocation = AstUtils.getEndLocation($3).get();
        $$ = Declarations.makeDataDecl(startLocation, endLocation, $1, $3);
        popDeclspecStack();
    }
    | declspecs_nosc_ts setspecs
    {
        final Location startLocation = AstUtils.getStartLocation($1).get();
        final Location endLocation = AstUtils.getEndLocation($1).get();
        $$ = Declarations.makeDataDecl(startLocation, endLocation, $1, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | declspecs_nosc_nots setspecs components_notype
    {
        final Location startLocation = AstUtils.getStartLocation($1, $3).get();
        final Location endLocation = AstUtils.getEndLocation($3).get();
        $$ = Declarations.makeDataDecl(startLocation, endLocation, $1, $3);
        popDeclspecStack();
    }
    | declspecs_nosc_nots setspecs
    {
        final Location startLocation = AstUtils.getStartLocation($1).get();
        final Location endLocation = AstUtils.getEndLocation($1).get();
        $$ = Declarations.makeDataDecl(startLocation, endLocation, $1, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | error
    {
        $$ = Declarations.makeErrorDecl();
    }
    | extension component_decl
    {
        $$ = Declarations.makeExtensionDecl($1.getLocation(), $2.getEndLocation(), $2);
    }
    ;

components:
      component_declarator
    { $$ = Lists.<Declaration>newList($1); }
    | components COMMA maybe_resetattrs component_declarator
    { $$ = Lists.<Declaration>chain($1, $4); }
    ;

/*
 * It should be possible to use components after the COMMA, but gcc 3
 * isn't doing this.
 */
components_notype:
      component_notype_declarator
    { $$ = Lists.<Declaration>newList($1); }
    | components_notype COMMA maybe_resetattrs component_notype_declarator
    { $$ = Lists.<Declaration>chain($1, $4); }
    ;

component_declarator:
      declarator maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        $$ = Semantics.makeField($1.getLocation(), endLocation,
                Optional.of($1), Optional.<Expression>absent(), pstate.declspecs, prefixAttr($2));
    }
    | declarator COLON expr_no_commas maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($3.getEndLocation(), $4);
        $$ = Semantics.makeField($1.getLocation(), endLocation,
                Optional.of($1), Optional.of($3), pstate.declspecs, prefixAttr($4));
    }
    | COLON expr_no_commas maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($2.getEndLocation(), $3);
        $$ = Semantics.makeField($1.getLocation(), endLocation,
                Optional.<Declarator>absent(), Optional.of($2), pstate.declspecs, prefixAttr($3));
    }
    ;

component_notype_declarator:
      notype_declarator maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        $$ = Semantics.makeField($1.getLocation(), endLocation,
                Optional.of($1), Optional.<Expression>absent(), pstate.declspecs, prefixAttr($2));
    }
    | notype_declarator COLON expr_no_commas maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($3.getEndLocation(), $4);
        $$ = Semantics.makeField($1.getLocation(), endLocation,
                Optional.of($1), Optional.of($3), pstate.declspecs, prefixAttr($4));
    }
    | COLON expr_no_commas maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($2.getEndLocation(), $3);
        $$ = Semantics.makeField($1.getLocation(), endLocation,
                Optional.<Declarator>absent(), Optional.of($2), pstate.declspecs, prefixAttr($3));
    }
    ;

enumlist:
      enumerator
    { $$ = Lists.<Declaration>newList($1); }
    | enumlist COMMA enumerator
    { $$ = Lists.<Declaration>chain($1, $3); }
    | error
    { $$ = Lists.<Declaration>newList(); }
    ;


enumerator:
      identifier
    {
        $$ = Semantics.makeEnumerator($1.getLocation(), $1.getEndLocation(), $1.getValue(),
                Optional.<Expression>absent());
    }
    | identifier EQ expr_no_commas
    {
        $$ = Semantics.makeEnumerator($1.getLocation(), $3.getEndLocation(), $1.getValue(), Optional.of($3));
    }
    ;

// FIXME
typename:
      declspecs_nosc absdcl
    {
        /* NOTICE: absdcl may be null! */
        $$ = Semantics.makeType($1, Optional.<Declarator>fromNullable($2));
    }
    ;

absdcl:   /* an abstract declarator */
      /* empty */
    { $$ = null; }
    | absdcl1
    { $$ = $1; }
    ;

absdcl1:  /* a nonempty abstract declarator */
      absdcl1_ea
    { $$ = $1; }
    | absdcl1_noea
    { $$ = $1; }
    ;

absdcl1_noea:
      direct_absdcl1
    { $$ = $1; }
    | STAR maybe_type_quals_attrs absdcl1_noea
    {
        $$ = Semantics.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    ;

absdcl1_ea:
      STAR maybe_type_quals_attrs
    {
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        $$ = Semantics.makePointerDeclarator($1.getLocation(), endLocation, Optional.<Declarator>absent(), $2);
    }
    | STAR maybe_type_quals_attrs absdcl1_ea
    {
        $$ = Semantics.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    ;

direct_absdcl1:
      LPAREN maybe_attribute absdcl1 RPAREN
    {
        final QualifiedDeclarator decl = new QualifiedDeclarator($1.getLocation(), $3,
                Lists.<Attribute, TypeElement>convert($2));
        decl.setEndLocation($4.getEndLocation());
        $$ = decl;
    }
    | direct_absdcl1 array_or_absfn_declarator
    {
        final Declarator declarator = Semantics.finishArrayOrFnDeclarator($1, $2);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            popLevel();
        }
        $$ = declarator;
    }
    | array_or_absfn_declarator
    {
        final Declarator declarator = Semantics.finishArrayOrFnDeclarator(null, $1);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            popLevel();
        }
        $$ = declarator;
    }
    ;

array_or_fn_declarator:
      fn_declarator
    { $$ = $1; }
    | array_declarator
    { $$ = $1; }
    ;

array_or_absfn_declarator:
      absfn_declarator
    { $$ = $1; }
    | array_declarator
    { $$ = $1; }
    ;

// NOTICE: fn_quals do not even appear.
fn_declarator:
      parameters LPAREN parmlist_or_identifiers_1 fn_quals
    {
        final Location endLocation = AstUtils.getEndLocation($2.getEndLocation(), $3);
        final FunctionDeclarator decl = new FunctionDeclarator($2.getLocation(), null, $3, $1, $4);
        decl.setEndLocation(endLocation);
        $$ = decl;
    }
    | LPAREN parmlist_or_identifiers fn_quals
    {
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        final FunctionDeclarator decl = new FunctionDeclarator($1.getLocation(), null, $2,
                Lists.<Declaration>newList(), $3);
        decl.setEndLocation(endLocation);
        $$ = decl;
    }
    ;

absfn_declarator:
      LPAREN parmlist fn_quals
    {
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        final FunctionDeclarator decl = new FunctionDeclarator($1.getLocation(), null, $2,
                Lists.<Declaration>newList(), $3);
        decl.setEndLocation(endLocation);
        $$ = decl;
    }
    ;

array_declarator:
      LBRACK expr RBRACK
    {
        final ArrayDeclarator decl = new ArrayDeclarator($1.getLocation(), null, $2);
        decl.setEndLocation($3.getEndLocation());
        $$ = decl;
    }
    | LBRACK RBRACK
    {
        final ArrayDeclarator decl = new ArrayDeclarator($1.getLocation(), null, null);
        decl.setEndLocation($2.getEndLocation());
        $$ = decl;
    }
    ;

/*
 * At least one statement, the first of which parses without error.
 *
 * stmts is used only after decls, so an invalid first statement
 * is actually regarded as an invalid decl and part of the decls.
 */
stmts:
      stmt_or_labels
    {
        if ($1.getCounter() > 0) {
            final Statement lastLabel = $1.getStatements().getLast();
            final EmptyStmt empty = new EmptyStmt(lastLabel.getLocation());
            empty.setEndLocation(lastLabel.getEndLocation());
            Statements.chainWithLabels($1.getStatements(), Lists.<Statement>newList(lastLabel));
        }
        $$ = $1.getStatements();
    }
    ;

stmt_or_labels:
      stmt_or_label
    {
        $$ = new ValueStatements(Lists.<Statement>newList($1.getStatement()), $1.getCounter());
    }
    | stmt_or_labels stmt_or_label
    {
        $$ = new ValueStatements(
                Statements.chainWithLabels($1.getStatements(), Lists.<Statement>newList($2.getStatement())),
                $2.getCounter());
    }
    | stmt_or_labels errstmt
    {
        $$ = new ValueStatements(Lists.<Statement>newList(Statements.makeErrorStmt()), 0);
    }
    ;

xstmts:
      /* empty */
    { $$ = Lists.<Statement>newList(); }
    | stmts
    { $$ = $1; }
    ;

errstmt:
      error SEMICOLON
    ;

pushlevel:
      /* empty */
    {
        pushLevel(false);
    }
    ;

/*
 * Read zero or more forward-declarations for labels that nested functions can
 * jump to.
 */
maybe_label_decls:
      /* empty */
    { $$ = Lists.<IdLabel>newList(); }
    | label_decls
    { $$ = $1; }
    ;

label_decls:
      label_decl
    {
        // NOTICE: label_decl is a list.
        $$ = $1;
    }
    | label_decls label_decl
    { $$ = Lists.chain($1, $2); }
    ;

label_decl:
      LABEL identifiers_or_typenames SEMICOLON
    { $$ = $2; }
    ;

/*
 * This is the body of a function definition. It causes syntax errors
 * to ignore to the next openbrace.
 */
compstmt_or_error:
      compstmt
    { $$ = $1; }
    | error compstmt
    { $$ = $2; }
    ;

compstmt_start:
      LBRACE
    { $$ = $1; }
    ;

compstmt:
      compstmt_start pushlevel RBRACE
    {
        popLevel();
        final CompoundStmt stmt = new CompoundStmt($1.getLocation(), Lists.<IdLabel>newList(),
                Lists.<Declaration>newList(), Lists.<Statement>newList());
        stmt.setEndLocation($3.getEndLocation());
        $$ = stmt;
    }
    | compstmt_start pushlevel maybe_label_decls decls xstmts RBRACE
    {
        popLevel();
        final CompoundStmt stmt = new CompoundStmt($1.getLocation(), $3, $4, $5);
        stmt.setEndLocation($6.getEndLocation());
        $$ = stmt;
    }
    | compstmt_start pushlevel maybe_label_decls error RBRACE
    {
        popLevel();
        $$ = Statements.makeErrorStmt();
    }
    | compstmt_start pushlevel maybe_label_decls stmts RBRACE
    {
        popLevel();
        final CompoundStmt stmt = new CompoundStmt($1.getLocation(), $3, Lists.<Declaration>newList(), $4);
        stmt.setEndLocation($5.getEndLocation());
        $$ = stmt;
    }
    ;

/*
 * Value is number of statements counted as of the closeparen.
 */
simple_if:
      if_prefix labeled_stmt
    {
        final IfStmt stmt = new IfStmt($1.getLocation(), $1.getExpression(), $2, null);
        stmt.setEndLocation($2.getEndLocation());
        $$ = new ValueStatement(stmt, $1.getCounter());
    }
    | if_prefix error
    {
        $$ = new ValueStatement(Statements.makeErrorStmt(), $1.getCounter());
    }
    ;

if_prefix:
      IF LPAREN expr RPAREN
    {
        $$ = new ValueExpression($1.getLocation(), $4.getEndLocation(), $3, pstate.stmtCount);
    }
    ;

/*
 * This is a subroutine of stmt. It is used twice, once for valid DO statements
 * and once for catching errors in parsing the end test.
 */
do_stmt_start:
      DO
    {
        pstate.stmtCount++;
    }
      labeled_stmt WHILE
    {
        final DoWhileStmt stmt = new DoWhileStmt($1.getLocation(), null, $3);
        stmt.setEndLocation($4.getEndLocation());
        $$ = stmt;
    }
    ;

labeled_stmt:
      stmt
    { $$ = $1; }
    | label labeled_stmt
    {
        final LabeledStmt stmt = new LabeledStmt($1.getLocation(), $1, $2);
        stmt.setEndLocation($2.getEndLocation());
        $$ = stmt;
    }
    ;

stmt_or_label:
      stmt
    {
        $$ = new ValueStatement($1, 0);
    }
    | label
    {
        final LabeledStmt stmt = new LabeledStmt($1.getLocation(), $1, null);
        stmt.setEndLocation($1.getEndLocation());
        $$ = new ValueStatement(stmt, 1);
    }
    ;

atomic_stmt:
      ATOMIC stmt_or_error
    {
        final AtomicStmt atomicStmt = new AtomicStmt($1.getLocation(), $2);
        atomicStmt.setEndLocation($1.getEndLocation());
        $$ = atomicStmt;
    }
    ;

stmt_or_error:
      stmt
    { $$ = $1; }
    | error
    { $$ = Statements.makeErrorStmt(); }
    ;

/*
 * Parse a single real statement, not including any labels.
 */
stmt:
      compstmt
    {
        pstate.stmtCount++;
        $$ = $1;
    }
    | expr SEMICOLON
    {
        pstate.stmtCount++;
        final ExpressionStmt stmt = new ExpressionStmt($1.getLocation(), $1);
        stmt.setEndLocation($2.getEndLocation());
        $$ = stmt;
    }
    | simple_if ELSE
    {
        $1.setCounter(pstate.stmtCount);
    }
      labeled_stmt
    {
        if (pstate.stmtCount == $1.getCounter()) {
            warning($2.getLocation(), Optional.of($2.getEndLocation()), "empty body in an else-statement");
        }
        final Statement stmt = $1.getStatement();
        /*
         * NOTICE: Setting false statement.
         * Be careful, could be an error statement!
         */
        if ($$ instanceof IfStmt) {
            final IfStmt ifStmt = (IfStmt) stmt;
            ifStmt.setFalseStatement($4);
        }
        $$ = stmt;
    }
    | simple_if %prec IF
    {
        /*
         * This warning is here instead of in simple_if, because we
         * do not want a warning if an empty if is followed by an
         * else statement. Increment stmtCount so we don't
         * give a second error if this is a nested `if'.
         */
        if (pstate.stmtCount++ == $1.getCounter()) {
            warning($1.getStatement().getLocation(), Optional.of($1.getStatement().getEndLocation()),
                    "empty body in an if-statement");
        }
        $$ = $1.getStatement();
    }
    | simple_if ELSE error
    {
        $$ = Statements.makeErrorStmt();
    }
    | WHILE
    {
        pstate.stmtCount++;
    }
      LPAREN expr RPAREN labeled_stmt
    {
        final WhileStmt stmt = new WhileStmt($1.getLocation(), $4, $6);
        stmt.setEndLocation($6.getEndLocation());
        $$ = stmt;
    }
    | do_stmt_start LPAREN expr RPAREN SEMICOLON
    {
        $1.setCondition($3);
        $$ = $1;
    }
    | do_stmt_start error
    {
        $$ = Statements.makeErrorStmt();
    }
    | FOR LPAREN xexpr SEMICOLON
    {
        pstate.stmtCount++;
    }
      xexpr SEMICOLON xexpr RPAREN labeled_stmt
    {
        /* NOTICE: xexpr may be null. */
        final ForStmt stmt = new ForStmt($1.getLocation(), $3, $6, $8, $10);
        stmt.setEndLocation($10.getEndLocation());
        $$ = stmt;
    }
    | SWITCH LPAREN expr RPAREN
    {
        pstate.stmtCount++;
    }
      labeled_stmt
    {
        final SwitchStmt stmt = new SwitchStmt($1.getLocation(), $3, $6);
        stmt.setEndLocation($6.getEndLocation());
        $$ = stmt;
    }
    | BREAK SEMICOLON
    {
        pstate.stmtCount++;
        final BreakStmt stmt = new BreakStmt($1.getLocation());
        stmt.setEndLocation($2.getEndLocation());
        $$ = stmt;
    }
    | CONTINUE SEMICOLON
    {
        pstate.stmtCount++;
        final ContinueStmt stmt = new ContinueStmt($1.getLocation());
        stmt.setEndLocation($2.getEndLocation());
        $$ = stmt;
    }
    | RETURN SEMICOLON
    {
        pstate.stmtCount++;
        $$ = Statements.makeVoidReturn($1.getLocation(), $2.getEndLocation());
    }
    | RETURN expr SEMICOLON
    {
        pstate.stmtCount++;
        $$ = Statements.makeReturn($1.getLocation(), $3.getEndLocation(), $2);
    }
    | ASM_KEYWORD maybe_type_qual LPAREN expr RPAREN SEMICOLON
    {
        // TODO
        /* NOTICE: maybe_type_qual may be null */
        pstate.stmtCount++;
        $$ = new AsmStmt(null, $4, Lists.<AsmOperand>newList(),
                Lists.<AsmOperand>newList(), Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($2));
    }
    /* This is the case with just output operands.  */
    | ASM_KEYWORD maybe_type_qual LPAREN expr COLON asm_operands RPAREN SEMICOLON
    {
        // TODO
        /* NOTE: maybe_type_qual may be null */
        pstate.stmtCount++;
        $$ = new AsmStmt(null, $4, $6, Lists.<AsmOperand>newList(),
                Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($2));
    }
    /* This is the case with input operands as well.  */
    | ASM_KEYWORD maybe_type_qual LPAREN expr COLON asm_operands COLON asm_operands RPAREN SEMICOLON
    {
        // TODO
        /* NOTE: maybe_type_qual may be null */
        pstate.stmtCount++;
        $$ = new AsmStmt(null, $4, $6, $8, Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($2));
    }
    /* This is the case with clobbered registers as well.  */
    | ASM_KEYWORD maybe_type_qual LPAREN expr COLON asm_operands COLON asm_operands COLON asm_clobbers RPAREN SEMICOLON
    {
        // TODO
        /* NOTE: maybe_type_qual may be null */
        pstate.stmtCount++;
        $$ = new AsmStmt(null, $4, $6, $8, $10,
                Lists.<TypeElement>newListEmptyOnNull($2));
    }
    | GOTO id_label SEMICOLON
    {
        pstate.stmtCount++;
        final GotoStmt stmt = new GotoStmt($1.getLocation(), $2);
        stmt.setEndLocation($3.getEndLocation());
        $$ = stmt;
    }
    | GOTO STAR expr SEMICOLON
    {
        pstate.stmtCount++;
        final ComputedGotoStmt stmt = new ComputedGotoStmt($1.getLocation(), $3);
        stmt.setEndLocation($4.getEndLocation());
        $$ = stmt;
    }
    | atomic_stmt
    {
        $$ = $1;
    }
    | SEMICOLON
    {
        final EmptyStmt stmt = new EmptyStmt($1.getLocation());
        stmt.setEndLocation($1.getEndLocation());
        $$ = stmt;
    }
    ;

/*
 * Any kind of label, including jump labels and case labels.
 * ANSI C accepts labels only before statements, but we allow them
 * also at the end of a compound statement.
 */

label:
      CASE expr_no_commas COLON
    {
        final CaseLabel label = new CaseLabel($1.getLocation(), $2, null);
        label.setEndLocation($3.getEndLocation());
        $$ = label;
    }
    | CASE expr_no_commas ELLIPSIS expr_no_commas COLON
    {
        final CaseLabel label = new CaseLabel($1.getLocation(), $2, $4);
        label.setEndLocation($5.getEndLocation());
        $$ = label;
    }
    | DEFAULT COLON
    {
        final DefaultLabel label = new DefaultLabel($1.getLocation());
        label.setEndLocation($2.getEndLocation());
        $$ = label;
    }
    | id_label COLON
    {
        $$ = $1;
    }
    ;

/* Either a type-qualifier or nothing.  First thing in an `asm' statement.  */

maybe_type_qual:
      /* empty */
    { $$ = null; }
    | type_qual
    { $$ = $1; }
    ;

xexpr:
      /* empty */
    { $$ = null; }
    | expr
    { $$ = $1; }
    ;

/* These are the operands other than the first string and colon
 in  asm ("addextend %2,%1": "=dm" (x), "0" (y), "g" (*x))  */
// TODO
asm_operands:
      /* empty */
    { $$ = Lists.<AsmOperand>newList(); }
    | nonnull_asm_operands
    { $$ = $1; }
    ;

// TODO
nonnull_asm_operands:
      asm_operand
    { $$ = Lists.newList($1); }
    | nonnull_asm_operands COMMA asm_operand
    { $$ = Lists.chain($1, $3); }
    ;

// TODO
asm_operand:
      string_chain LPAREN expr RPAREN
    {
        $$ = new AsmOperand(null, null, $1, $3);
    }
    | LBRACK idword RBRACK string_chain LPAREN expr RPAREN
    {
        $$ = new AsmOperand(null, $2, $4, $6);
    }
    ;

asm_clobbers:
      string_chain
    { $$ = Lists.<StringAst>newList($1); }
    | asm_clobbers COMMA string_chain
    { $$ = Lists.<StringAst>chain($1, $3); }
    ;

/*
 * This is what appears inside the parens in a function declarator.
 * Its value is a list of ..._TYPE nodes.
 */
parmlist:
    {
        /*
         * NOTICE: A strange thing, an action, even empty, MUST be in here.
         * If this action were removed, erroneous parser would be produced.
         */
        pushLevel(true);
    }
      parmlist_1
    {
        $$ = $2;
        /* poplevel() is done when building the declarator */
    }
    ;

parmlist_1:
      parmlist_2 RPAREN
    { $$ = $1; }
    | parms SEMICOLON parmlist_1
    { $$ = Lists.<Declaration>chain($1, $3); }
    | error RPAREN
    { $$ = Lists.<Declaration>newList(Declarations.makeErrorDecl()); }
    ;

/*
 * This is what appears inside the parens in a function declarator.
 * Is value is represented in the format that grokdeclarator expects.
 */
parmlist_2:
      /* empty */
    { $$ = Lists.<Declaration>newList(); }
    | ELLIPSIS
    {
        $$ = Lists.<Declaration>newList(Declarations.makeErrorDecl());
		  /* Gcc used to allow this as an extension.  However, it does
		   * not work for all targets, and thus has been disabled.
		   * Also, since func (...) and func () are indistinguishable,
		   * it caused problems with the code in expand_builtin which
		   * tries to verify that BUILT_IN_NEXT_ARG is being used
		   * correctly.
		   */
    }
    | parms
    { $$ = $1; }
    | parms COMMA ELLIPSIS
    {
        final EllipsisDecl decl = new EllipsisDecl($3.getLocation());
        decl.setEndLocation($3.getEndLocation());
        $$ = Lists.<Declaration>chain($1, decl);
    }
    ;

parms:
      parm
    { $$ = Lists.<Declaration>newList($1); }
    | parms COMMA parm
    { $$ = Lists.<Declaration>chain($1, $3); }
    ;

/*
 * A single parameter declaration or parameter type name,
 * as found in a parmlist.
 */
parm:
      declspecs_ts xreferror parm_declarator maybe_attribute
    {
        $$ = Semantics.declareParameter(Optional.of($3), $1, $4);
    }
    | declspecs_ts xreferror notype_declarator maybe_attribute
    {
        $$ = Semantics.declareParameter(Optional.of($3), $1, $4);
    }
    | declspecs_ts xreferror absdcl
    {
        /* NOTE: absdcl may be null */
        $$ = Semantics.declareParameter(Optional.fromNullable($3), $1, Lists.<Attribute>newList());
    }
    | declspecs_ts xreferror absdcl1_noea attributes
    {
        $$ = Semantics.declareParameter(Optional.of($3), $1, $4);
    }
    | declspecs_nots xreferror notype_declarator maybe_attribute
    {
        $$ = Semantics.declareParameter(Optional.of($3), $1, $4);
    }
    | declspecs_nots xreferror absdcl
    {
        /* NOTICE: absdcl may be null. */
        $$ = Semantics.declareParameter(Optional.fromNullable($3), $1, Lists.<Attribute>newList());
    }
    | declspecs_nots xreferror absdcl1_noea attributes
    {
        $$ = Semantics.declareParameter(Optional.of($3), $1, $4);
    }
    ;

xreferror:
      /* empty */
    {
        // TODO
    }
    ;

/*
 * This is used in a function definition where either a parmlist or an
 * identifier list is ok.
 * Its value is a list of ..._TYPE nodes or a list of identifiers.
 */
parmlist_or_identifiers:
    {
        pushLevel(true);
    }
      parmlist_or_identifiers_1
    { $$ = $2; }
    ;

parmlist_or_identifiers_1:
      parmlist_1
    { $$ = $1; }
    | identifiers RPAREN
    { $$ = $1; }
    ;

/* A nonempty list of identifiers.  */
identifiers:
      old_parameter
    { $$ = Lists.<Declaration>newList($1); }
    | identifiers COMMA old_parameter
    { $$ = Lists.<Declaration>chain($1, $3); }
    ;

old_parameter:
      IDENTIFIER
    {
        $$ = Semantics.declareOldParameter($1.getLocation(), $1.getEndLocation(), $1.getValue());
    }
    ;

/*
 * A nonempty list of identifiers, including typenames.
 */
identifiers_or_typenames:
      id_label
    { $$ = Lists.<IdLabel>newList($1); }
    | identifiers_or_typenames COMMA id_label
    { $$ = Lists.<IdLabel>chain($1, $3); }
    ;

/*
 * A possibly empty list of function qualifiers (only one exists so far).
 */
fn_quals:
      /* empty */
    { $$ = Lists.<TypeElement>newList(); }
    | fn_qual
    { $$ = Lists.<TypeElement>newList($1); }
    ;

extension:
      EXTENSION
    {
        $$ = $1;
    }
    ;

/* FIXME : check if all specifiers were listed in productions below
(scspec, type_qual, fn_qual, type_spec)
*/
scspec:
      TYPEDEF
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.TYPEDEF);
        wasTypedef = true;
        LOG.trace("Setting wasTypedef to true.");
    }
    | EXTERN
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.EXTERN);
    }
    | STATIC
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.STATIC);
    }
    | AUTO
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.AUTO);
    }
    | REGISTER
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.REGISTER);
    }
    | COMMAND
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.COMMAND);
    }
    | EVENT
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.EVENT);
    }
    | ASYNC
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.ASYNC);
    }
    | TASK
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.TASK);
    }
    | NORACE
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.NORACE);
    }
    | DEFAULT
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.DEFAULT);
    }
    | INLINE
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.INLINE);
    }
    ;

type_qual:
      CONST
    {
        $$ = Semantics.makeQualifier($1.getLocation(), $1.getEndLocation(), RID.CONST);
    }
    | RESTRICT
    {
        $$ = Semantics.makeQualifier($1.getLocation(), $1.getEndLocation(), RID.RESTRICT);
    }
    | VOLATILE
    {
        $$ = Semantics.makeQualifier($1.getLocation(), $1.getEndLocation(), RID.VOLATILE);
    }
    ;

/*
 * FIXME: do you know any FN_QUAL? Note that inline is type_spec.
 */
fn_qual:
      FN_QUAL
    { $$ = null; }
    ;

type_spec:
      VOID
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.VOID);
    }
    | CHAR
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.CHAR);
    }
    | SHORT
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.SHORT);
    }
    | INT
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.INT);
    }
    | LONG
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.LONG);
    }
    | FLOAT
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.FLOAT);
    }
    | DOUBLE
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.DOUBLE);
    }
    | SIGNED
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.SIGNED);
    }
    | UNSIGNED
    {
        $$ = Semantics.makeRid($1.getLocation(), $1.getEndLocation(), RID.UNSIGNED);
    }
    ;

string_chain:
      STRING_LITERAL
    {
        final StringCst stringCst = new StringCst($1.getLocation(), $1.getValue());
        stringCst.setEndLocation($1.getEndLocation());
        final StringAst stringAst = new StringAst($1.getLocation(), Lists.<StringCst>newList(stringCst));
        stringAst.setEndLocation($1.getEndLocation());
        $$ = stringAst;
    }
    | string_chain STRING_LITERAL
    {
        final StringCst stringCst = new StringCst($2.getLocation(), $2.getValue());
        stringCst.setEndLocation($2.getEndLocation());

        /* Add string literal at the end of list, update end location. */
        $1.getStrings().addLast(stringCst);
        $1.setEndLocation($2.getEndLocation());
        $$ = $1;
    }
    ;

%code {

    private static final Logger LOG = Logger.getLogger(Parser.class);
    /**
     * Name of currently being parsed entity.
     */
    private String currentEntityName;
    /**
     * Currently being parsed file path.
     */
    private String filePath;
    /**
     * File type.
     */
    private FileType fileType;
    /**
     * Lexer.
     */
    private pl.edu.mimuw.nesc.lexer.Lexer lex;
    /**
     * Lexer wrapper.
     */
    private LexerWrapper lexer;
    /**
     * Symbol table. Enables to distinguish between IDENTIFIER and TYPENAME
     * token. Shared with LexerWrapper.
     */
    private SymbolTable symbolTable;
    private ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;
    private ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder;
    /**
     * Indicates whether parsing was successful.
     */
    private boolean errors;
    /**
     * Keeps data essential during parsing phase.
     */
    private ParserState pstate;
    /**
     * Indicates if there was a TYPEDEF keyword in current declaration.
     * This field was introduced only for proper parsing of
     * <code>COMPONENTREF DOT X</code> construction. To determine whether
     * general declaration is typedef declaration use special visitor
     * {@link TypeElementUtils#isTypedef(LinkedList<TypeElement>)}.
     */
    private boolean wasTypedef;
    /**
     *
     */
    private ParserListener parserListener;
    /**
     * The root of parsed AST (nesc entity).
     */
    private Node entityRoot;
    /**
     * The list of definitions located prior to the nesc entity definition.
     */
    private List<Declaration> extdefs;

    /**
     * Creates parser.
     *
     * @param filePath              currently being parsed file path
     * @param lex                   lexer
     * @param symbolTable           symbol table
     * @param fileType              fileType file type
     * @param tokensMultimapBuilder tokens multimap builder
     */
    public Parser(String filePath,
                  pl.edu.mimuw.nesc.lexer.Lexer lex,
                  SymbolTable symbolTable,
                  FileType fileType,
                  ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                  ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder) {
        Preconditions.checkNotNull(filePath, "file path cannot be null");
        Preconditions.checkNotNull(lex, "lexer cannot be null");
        Preconditions.checkNotNull(symbolTable, "symbol table cannot be null");
        Preconditions.checkNotNull(fileType, "file type cannot be null");
        Preconditions.checkNotNull(tokensMultimapBuilder, "tokens multimap builder cannot be null");
        Preconditions.checkNotNull(issuesMultimapBuilder, "issues multimap builder cannot be null");

        this.filePath = filePath;
        this.fileType = fileType;
        this.currentEntityName = Files.getNameWithoutExtension(filePath);
        this.symbolTable = symbolTable;
        this.tokensMultimapBuilder = tokensMultimapBuilder;
        this.issuesMultimapBuilder = issuesMultimapBuilder;
        this.lex = lex;
        this.lexer = new LexerWrapper(this.lex, this.symbolTable, this.tokensMultimapBuilder,
                this.issuesMultimapBuilder);
        this.yylexer = lexer;

        this.errors = false;
        this.pstate = new ParserState();

        switch (fileType) {
            case HEADER:
            case C:
                this.lexer.pushtoken(Symbol.builder().symbolCode(Lexer.DISPATCH_C).value("DISPATCH_C").build());
                break;
            case NESC:
                this.lexer.pushtoken(Symbol.builder().symbolCode(Lexer.DISPATCH_NESC).value("DISPATCH_NESC").build());
                break;
            default:
                throw new RuntimeException("not handled file type " + fileType);
        }

    }

    /**
     * Returns lexer.
     *
     * @return lexer
     */
    public pl.edu.mimuw.nesc.lexer.Lexer getLexer() {
        return this.lex;
    }

    public void setListener(ParserListener listener) {
        Preconditions.checkNotNull(listener, "listener cannot be null");
        this.parserListener = listener;
    }

    public void removeListener() {
        this.parserListener = null;
    }

    /**
     * Returns the root of nesc entity abstract syntax tree.
     *
     * @return root of nesc entity abstract syntax tree or
     * <code>Optional.absent()</code> if AST cannot be created or parsed
     * file is not a nesc file
     */
    public Optional<Node> getEntityRoot() {
        return Optional.fromNullable(this.entityRoot);
    }

    /**
     * Returns the list of definitions located prior to the nesc entity
     * definition.
     *
     * @return list of declarations
     */
    public List<Declaration> getExtdefs() {
        if (this.extdefs == null) {
            return new LinkedList<>();
        }
        return this.extdefs;
    }

    /**
     * Returns whether any errors occurred.
     *
     * @return <code>true</code> when errors occurred during parsing.
     */
    public boolean errors() {
        return errors;
    }

    /**
     * Adds new scope on the top of scopes stack.
     *
     * @param isParmLevel is parameter level
     */
    private void pushLevel(boolean isParmLevel) {
        LOG.trace("pushlevel");
        this.symbolTable.pushLevel(isParmLevel);
    }

    /**
     * Removes scope from the top of stack.
     */
    private void popLevel() {
        // XXX: see detect_bogus_env in semantics.c
        LOG.trace("poplevel");
        this.symbolTable.popLevel();
    }

    /**
     * Adds identifier as a type's name to current scope.
     *
     * @param identifier identifier
     */
    private void addTypename(String identifier) {
        this.symbolTable.add(identifier, Lexer.TYPEDEF_NAME);
    }

    /**
     * Adds identifier as a component's name to current scope.
     *
     * @param identifier identifier
     */
    private void addComponentRef(String identifier) {
        this.symbolTable.add(identifier, Lexer.COMPONENTREF);
    }

    /**
     * Adds identifier as a plain identifier (e.g. variable or function name) to
     * current scope.
     *
     * @param identifier identifier
     */
    private void addIdentifier(String identifier) {
        this.symbolTable.add(identifier, Lexer.IDENTIFIER);
    }

    /**
     *
     */
    private void popDeclspecStack() {
        LOG.trace("popDeclspecStack");
        pstate.popDeclspecStack();
    }

    /**
     *
     */
    private void pushDeclspecStack() {
        LOG.trace("pushDeclspecStack");
        pstate.pushDeclspecStack();
    }

    /**
     * Declares identifier as a type name or variable/function/array/etc
     * name depending on there was a TYPEDEF keyword in current declaration.
     *
     * @param declarator declarator
     * @param elements   type elements
     */
    private void declareName(Declarator declarator, LinkedList<TypeElement> elements) {
        final boolean isTypedef = TypeElementUtils.isTypedef(elements);
        final String name = DeclaratorUtils.getDeclaratorName(declarator);
        LOG.trace(format("Add %s %s;", (isTypedef ? "type" : "variable"), name));
        if (isTypedef) {
            addTypename(name);
        } else {
            addIdentifier(name);
        }

        if (this.symbolTable.isGlobalLevel() && this.parserListener != null) {
            this.parserListener.globalId(name, isTypedef ? Lexer.TYPEDEF_NAME : Lexer.IDENTIFIER);
        }
    }

    /**
     * Merges specified list of attributes with attributes kept in parser state.
     * The specified attributes will be placed after those from parser state.
     *
     * @param postAttrs the list of attributes
     * @return a merged list of specified attributes and attributes kept in
     * parser state
     */
    private LinkedList<Attribute> prefixAttr(LinkedList<Attribute> postAttrs) {
        return Lists.chain(pstate.attributes, postAttrs);
    }

    private void requireInterface(Word ifaceName) {
        if (this.parserListener != null
                && !this.parserListener.interfaceDependency(filePath, ifaceName.getName())) {
            final String message = format("cannot find interface %s definition file", ifaceName.getName());
            error(ifaceName.getLocation(), Optional.of(ifaceName.getEndLocation()), message);
        }
    }

    private void requireComponent(Word componentName) {
        if (this.parserListener != null
                && !this.parserListener.componentDependency(filePath, componentName.getName())) {
            final String message = format("cannot find component %s definition file", componentName.getName());
            error(componentName.getLocation(), Optional.of(componentName.getEndLocation()), message);
        }
    }

    private void extdefsFinish() {
        LOG.debug("extdefs finish");
        if (this.parserListener != null) {
            this.parserListener.extdefsFinished();
        }
    }

    private void warning(Location startLocation, Optional<Location> endLocation, String message) {
        final NescWarning warning = new NescWarning(startLocation, endLocation, message);
        this.issuesMultimapBuilder.put(startLocation.getLine(), warning);
        LOG.info("Warning at (" + startLocation.getLine() + ", " + startLocation.getColumn() + "): " + message);
    }

    private void error(Location startLocation, Optional<Location> endLocation, String message) {
        final NescError error = new NescError(startLocation, endLocation, message);
        this.issuesMultimapBuilder.put(startLocation.getLine(), error);
        LOG.info("Error at (" + startLocation.getLine() + ", " + startLocation.getColumn() + "): " + message);
    }

    /**
     * Lexer wrapper. Handles lookahead, location creation, setting yylval...
     */
    private class LexerWrapper implements Lexer {

        private final pl.edu.mimuw.nesc.lexer.Lexer lexer;
        /**
         * A queue of pre-read tokens to deal with the lookahead.
         */
        private LinkedList<Symbol> tokenQueue;
        /**
         * Symbol table. Enables to distinguish between IDENTIFIER and TYPENAME
         * token. Shared with LexerWrapper.
         */
        private SymbolTable symbolTable;
        private ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;
        private ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder;
        /**
         *
         */
        private Symbol symbol;
        /**
         * For testing purposes.
         */
        private TokenPrinter tokenPrinter;
        /**
         * Current symbol value.
         */
        private String value;

        public LexerWrapper(pl.edu.mimuw.nesc.lexer.Lexer lexer,
                            SymbolTable symbolTable,
                            ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                            ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder) {
            this.symbolTable = symbolTable;
            this.lexer = lexer;
            this.tokensMultimapBuilder = tokensMultimapBuilder;
            this.issuesMultimapBuilder = issuesMultimapBuilder;
            this.tokenQueue = new LinkedList<>();
            this.tokenPrinter = new TokenPrinter();
        }

        @Override
        public Object getLVal() {
            /*
             * NOTICE: Not to decrease performance, the same symbol that is
             * returned by preprocessor is used in parser.
             */
            return this.symbol;
        }

        @Override
        public int yylex() throws java.io.IOException {
            this.symbol = poptoken();

            /*
             * Here is done the distinction between TYPEDEF_NAME, COMPONENTREF and
             * plain IDENTIFIER.
             */
            if (symbol.getSymbolCode() == IDENTIFIER) {
                final String name = symbol.getValue();
                final int type = symbolTable.get(name);
                symbol.setSymbolCode(type);
            }

            /*
             * Some lookahead is done below.
             *
             * Detecting construct:
             *     COMPONENTREF . (IDENTIFIER | TYPENAME | MAGIC_STRING)
             *
             * when found, should be replaced by:
             *     COMPONENTREF . IDENTIFIER
             *
             * otherwise replace COMPONENTREF by IDENTIFIER
             */
            if (symbol.getSymbolCode() == COMPONENTREF) {
                final Symbol componentRefSymbol = symbol;

                /* Default to regular identifier. */
                componentRefSymbol.setSymbolCode(IDENTIFIER);

                final Symbol secondSymbol = poptoken();
                final int secondSymbolCode = secondSymbol.getSymbolCode();

                if (secondSymbolCode != DOT) {
                    pushtoken(secondSymbol);
                } else {
                    final Symbol thirdSymbol = poptoken();
                    final int thirdSymbolCode = thirdSymbol.getSymbolCode();

                    if (thirdSymbolCode == IDENTIFIER ||
                            thirdSymbolCode == TYPEDEF_NAME ||
                            thirdSymbolCode == MAGIC_STRING) {

                        /*
                         *
                         */
                        if (wasTypedef) {
                            componentRefSymbol.setSymbolCode(COMPONENTREF);
                            thirdSymbol.setSymbolCode(IDENTIFIER);
                        }
                    }
                    pushtoken(thirdSymbol);
                    pushtoken(secondSymbol);
                }
            }

            addSymbol(symbol);

            return symbol.getSymbolCode();
        }

        @Override
        public void yyerror(String msg) {
            Parser.this.errors = true;
            final Location startLocation = symbol.getLocation();
            final String message = format("%s in %s at line: %d, column: %d.", msg, startLocation.getFilePath(),
                    startLocation.getLine(), startLocation.getColumn());
            LOG.info(message);
            final NescError error = new NescError(symbol.getLocation(), Optional.of(symbol.getEndLocation()), msg);
            this.issuesMultimapBuilder.put(symbol.getLocation().getLine(), error);
        }

        /**
         * Returns the next token from the queue or gets the next token from lexer
         * if the queue is empty.
         *
         * @return next token
         * @throws IOException
         */
        public Symbol poptoken() throws IOException {
            if (tokenQueue.isEmpty())
                return this.lexer.nextToken();
            else
                return this.tokenQueue.removeFirst();
        }

        /**
         * Inserts the token at the beginning queue when the lookahead was done.
         *
         * @param symbol symbol
         */
        public void pushtoken(Symbol symbol) {
            this.tokenQueue.addFirst(symbol);
        }

        /**
         * Convert symbol into token. Only tokens carrying no semantic data
         * (ast nodes, symbol table references) are added into map in this
         * step.
         *
         * @param symbol symbol
         */
        private void addSymbol(Symbol symbol) {
            final Optional<? extends Token> tokenOptional = TokenFactory.of(symbol);
            if (tokenOptional.isPresent()) {
                final int line = symbol.getLocation().getLine();
                this.tokensMultimapBuilder.put(line, tokenOptional.get());
            }
        }

    }

}
;
%%
