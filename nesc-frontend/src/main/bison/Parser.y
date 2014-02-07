/* TODO: borrowed from..., licence, etc... */
%code imports {
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import pl.edu.mimuw.nesc.ast.*;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.common.SourceLanguage;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.lexer.TokenPrinter;
import pl.edu.mimuw.nesc.semantic.*;
import pl.edu.mimuw.nesc.semantic.nesc.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.semantic.Semantics.current;
}

%define package {pl.edu.mimuw.nesc.parser}
%define public
%define parser_class_name {Parser}

/*
 * Start non-terminal of the grammar.
 */
%start dispatch

/*
 * ========== Token definitions ==========
 *
 * %token <field> name_or_names
 * The value associated with token(s) of given name(s) is stored in yylval
 * variable in attribute named "field".
 */

/*
 * All identifiers that are not reserved words and are not declared typedefs
 * in the current block.
 */
%token <Value.IdToken> IDENTIFIER

/*
 * All identifiers that are declared typedefs in the current block. In some
 * contexts, they are treated just like IDENTIFIER, but they can also serve
 * as typespecs in declarations.
 */
%token <Value.IdToken> TYPEDEF_NAME

/*
 * An identifier that is declared as a component reference in the current
 * block, and which is going to be used to refer to a typedef from the
 * component via the component-ref DOT identifier syntax.
 */
%token <Value.IdToken> COMPONENTREF

/*
 * Reserved words that specify storage class.
 */
%token TYPEDEF EXTERN STATIC AUTO REGISTER COMMAND EVENT ASYNC TASK NORACE

/*
 * Reserved words that specify type.
 */
%token VOID CHAR SHORT INT LONG FLOAT DOUBLE SIGNED UNSIGNED COMPLEX

/*
 * Reserved words that qualify types/functions: "const" or "volatile",
 * "deletes".
 * FIXME: FN_QUAL, present in grammar but never pushed from lexer.
 */
%token CONST RESTRICT VOLATILE INLINE FN_QUAL

/*
 * Character or numeric constants. yylval is the node for the constant.
 * TODO: string or int/float/char ?
 */
%token <LexicalCst> INTEGER_LITERAL
%token <LexicalCst> FLOATING_POINT_LITERAL
%token <LexicalCst> CHARACTER_LITERAL
%token <String> STRING_LITERAL

/*
 * String constants in raw form.
 * TODO: What is MAGIC_STRING?
 */
%token <Value.IdToken> MAGIC_STRING

/*
 * All kind of parentheses, operators, etc.
 */
%token        LBRACK RBRACK
%token        LPAREN RPAREN
%token        LBRACE RBRACE
%token        COLON SEMICOLON DOT COMMA
%token        ARROW LEFT_ARROW AT QUESTION ELLIPSIS
%token        STAR DIV MOD PLUS MINUS AND XOR OR TILDE NOT LSHIFT RSHIFT ANDAND OROR
%token        LT GT LTEQ GTEQ EQEQ NOTEQ
%token        EQ MULEQ DIVEQ MODEQ PLUSEQ MINUSEQ LSHIFTEQ RSHIFTEQ ANDEQ XOREQ OREQ

/* the reserved words */
/* SCO include files test "ASM", so use something else. */
%token        SIZEOF ENUM IF ELSE WHILE DO FOR SWITCH CASE DEFAULT
%token        BREAK CONTINUE RETURN GOTO ASM_KEYWORD TYPEOF ALIGNOF
%token        ATTRIBUTE EXTENSION LABEL
%token        REALPART IMAGPART VA_ARG OFFSETOF

/* the dispatching (fake) tokens */
%token <Value.IToken> DISPATCH_C DISPATCH_NESC DISPATCH_PARM DISPATCH_TYPE

/* nesC reserved words */
%token <Value.IToken> ATOMIC USES INTERFACE COMPONENTS PROVIDES MODULE
%token <Value.IToken> INCLUDES CONFIGURATION AS IMPLEMENTATION CALL
%token <Value.IToken> SIGNAL POST GENERIC NEW
%token <Value.StructKindToken> NX_STRUCT NX_UNION STRUCT UNION
/* words reserved for nesC's future. Some may never be used... */
%token <Value.IToken> ABSTRACT COMPONENT EXTENDS
%token <Value.IdToken> TARGET_ATTRIBUTE0 TARGET_ATTRIBUTE1 TARGET_DEF

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
 * Define the type of value returned by each production (more precisely,
 * which attribute of yylval should store the returned value).
 * %type <field> name_or_names
 */
%type <AsmOperand> asm_operand
%type <LinkedList<AsmOperand>> asm_operands nonnull_asm_operands
%type <AsmStmt> maybeasm
%type <Attribute> nattrib
%type <LinkedList<Attribute>> maybe_attribute attribute
%type <LinkedList<Attribute>> attributes attribute_list nesc_attributes
%type <GccAttribute> attrib target_attribute
%type <NescAttribute> nastart
%type <Declaration> datadecl datadef decl extdef fndef fndef2
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
%type <Value.IdToken> identifier type_parm
%type <Value.IExpr> if_prefix
%type <Value.IStmts> stmt_or_labels
%type <Value.IStmt> simple_if stmt_or_label
%type <LeftUnaryOperation> unop
%type <Value.IToken> extension TILDE NOT compstmt_start LBRACE SEMICOLON
%type <Value.IToken> sizeof alignof
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
%type <LinkedList<String>> fieldlist
%type <Value.StructKindToken> structkind

%type <NescCallKind> callkind
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
%type <LinkedList<ComponentRef>> component_list cuses
%type <Connection> connection
%type <LinkedList<Declaration>> configuration_decl
%type <LinkedList<Declaration>> configuration_decls
%type <EndPoint> endpoint
%type <ParameterisedIdentifier> parameterised_identifier
%type <Implementation> iconfiguration imodule
%type <Boolean> generic
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
		// push new level regardless the interface is generic or not
	    pushLevel(false);
        NescSemantics.startNescEntity(SourceLanguage.INTERFACE, $3);
    }
      interface_parms nesc_attributes
    {
        Attributes.handleNescdeclAttributes($6, current.container);
    }
      LBRACE datadef_list RBRACE
    {
        Interface intf = new Interface(null, $6, $3, $9);
        intf.setCdecl(current.container);

        if (intf.getCdecl().isAbstract()) {
            Semantics.poplevel();
        }
        popLevel();

        $$ = intf;
    }
    ;

interface_parms:
      /* empty */
    {
        $$ = Lists.<Declaration>newList();
    }
    | LT interface_parm_list GT
    {
        NescDeclaration intf = current.container;
        intf.setParameters($2);
        Environment env = current.isEnvEmpty() ? null : current.peekEnv();
        intf.setParameterEnv(env);
        $$ = $2;

        // A new scope level is needed.
        Semantics.pushlevel(false);
        // The interface env counts as global.
        Environment env2 = current.peekEnv();
        env2.setGlobalLevel(true);
        /*
         * FIXME: in startNescEntity, at the top of current.env the
         * Environment of newly created NescDeclaration is pushed.
         * Why is it set in NescDeclaration once again?
         */
        // intf.setEnvironment(current.env.peek());
        intf.setAbstract(true);
    }
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
        final String paramName = $1.id.getData();
        addTypename(paramName);
        $$ = NescSemantics.declareTypeParameter(null, $1.id, $2, null);
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
        Semantics.pushlevel(true);
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
    	// FIXME: push/pop level, the same for configuration
        pushLevel(false);   // FIXME parmlevel
        NescSemantics.startNescEntity(SourceLanguage.COMPONENT, $3);
        current.container.setAbstract($1);
    }
      component_parms nesc_attributes
    {
        Attributes.handleNescdeclAttributes($6, current.container);
    }
      LBRACE requires_or_provides_list RBRACE imodule
    {
        // FIXME all_tasks
        LinkedList<Declaration> intfs =
                Lists.<Declaration>chain(Lists.<Declaration>newList(), $9);
        Component component = new Component(null, $6, $3, $1, $5, intfs, $11);
        $$ = component;
        popLevel();
    }
    ;

configuration:
      generic CONFIGURATION idword
    {
        pushLevel(false);   // FIXME parmlevel
        NescSemantics.startNescEntity(SourceLanguage.COMPONENT, $3);
        current.container.setAbstract($1);
        current.container.setConfiguration(true);
    }
      component_parms nesc_attributes
    {
        Attributes.handleNescdeclAttributes($6, current.container);
    }
      LBRACE requires_or_provides_list RBRACE iconfiguration
    {
        Component component = new Component(null, $6, $3, $1, $5, $9, $11);
        $$ = component;
        popLevel();
    }
    ;

binary_component:
      COMPONENT idword
    {
        NescSemantics.startNescEntity(SourceLanguage.COMPONENT, $2);
    }
      nesc_attributes
    {
        Attributes.handleNescdeclAttributes($4, current.container);
    }
      LBRACE requires_or_provides_list RBRACE
    {
        BinaryComponent dummy = new BinaryComponent(null,
                NescComponents.startImplementation());
        Component component = new Component(null, $4, $2, false, null, $7,
                dummy);
        $$ = component;
    }
    ;

generic:
      GENERIC
    { $$ = true; }
    | /* empty */
    { $$ = false; }
    ;

component_parms:
      /* empty */
    {
        if (current.container.isAbstract()) {
            //error("generic components require a parameter list");
        }
        $$ = Lists.<Declaration>newList();
    }
    | LPAREN template_parms RPAREN
    {
        NescDeclaration comp = current.container;
        if (!comp.isAbstract()) {
            //error("generic components require a parameter list");
        }
        comp.setParameters($2);
        comp.setParameterEnv(current.isEnvEmpty() ? null : current.peekEnv());
        $$ = $2;

        /*
         * Only generic components need a new scope.
         */
        pushLevel(false);    // FIXME: too late, you fool!
        Semantics.pushlevel(false);
        Environment env = current.peekEnv();
        env.setGlobalLevel(true);
        comp.setEnvironment(env);
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
        $$ = NescSemantics.declareTemplateParameter($3, $1, $4);
    }
    | declspecs_ts xreferror notype_declarator maybe_attribute
    {
        declareName($3, $1);
        $$ = NescSemantics.declareTemplateParameter($3, $1, $4);
    }
    | declspecs_nots xreferror notype_declarator maybe_attribute
    {
        declareName($3, $1);
        $$ = NescSemantics.declareTemplateParameter($3, $1, $4);
    }
    | declspecs_ts xreferror
    {
        // TODO: what we are supposed to do?
        $$ = NescSemantics.declareTemplateParameter(null, $1, null);
    }
    ;

requires_or_provides_list:
      requires_or_provides_list_
    {
        current.specSection = SemanticState.SpecSection.NORMAL;
        $$ = $1;
    }
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
    { current.specSection = SemanticState.SpecSection.NORMAL; }
      just_datadef
    {
        // TODO check if null
        $$ = Lists.<Declaration>newList($2);
    }
    ;

requires:
      USES
    { current.specSection = SemanticState.SpecSection.USES; }
      parameterised_interface_list
    {
        $$ = new RpInterface(null, true, $3);
    }
    ;

provides:
      PROVIDES
    { current.specSection = SemanticState.SpecSection.PROVIDES; }
      parameterised_interface_list
    {
        $$ = new RpInterface(null, false, $3);
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
        Environment env = current.isEnvEmpty() ? null : current.peekEnv();
        NescComponents.declareInterfaceRef($1, null, env, $2);
        $$ = $1;
    }
    | interface_ref parameters nesc_attributes SEMICOLON
    {
        $1.setGparms($2);
        popLevel();        // for parsing purposes
        Semantics.poplevel();
        Environment env = current.isEnvEmpty() ? null : current.peekEnv();
        NescComponents.declareInterfaceRef($1, $2, env, $3);
        $$ = $1;
    }
    ;

interface_ref:
      interface_type
    { $$ = $1; }
    | interface_type AS idword
    {
        $1.setWord2($3);
        $$ = $1;
    }
    ;

interface_type:
      INTERFACE idword
    {
        requireInterface($2.getCstring().getData());
        $$ = new InterfaceRef(null, $2, Lists.<Expression>newList(), null,
                Lists.<Declaration>newList(), Lists.<Attribute>newList(),
                null);
    }
    | INTERFACE idword
    {
        requireInterface($2.getCstring().getData());
    }
      LT typelist GT
    {
        $$ = new InterfaceRef(null, $2, $5, null, Lists.<Declaration>newList(),
                Lists.<Attribute>newList(), null);
    }
    ;

typelist:
      generic_type
    { $$ = Lists.<Expression>newList($1); }
    | typelist COMMA generic_type
    { $$ = Lists.<Expression>chain($1, $3); }
    ;

iconfiguration:
      IMPLEMENTATION
    { $<Environment>$ = NescComponents.startImplementation(); }
      LBRACE configuration_decls RBRACE
    {
        $$ = new Configuration(null, $<Environment>2, $4);
    }
    ;

cuses:
      COMPONENTS component_list SEMICOLON
    { $$ = $2; }
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
        requireComponent($1.getWord1().getCstring().getData());
        /* Put component alias into parser's symbol table. */
        final String componentAlias = $1.getWord1().getCstring().getData();
        addComponentRef(componentAlias);
        $$ = NescConfiguration.requireComponent($1, null);
    }
    | component_ref2 AS idword
    {
        requireComponent($1.getWord1().getCstring().getData());
        /* Put component alias into parser's symbol table. */
        final String componentAlias = $3.getCstring().getData();
        addComponentRef(componentAlias);
        $$ = NescConfiguration.requireComponent($1, $3);
    }
    ;

component_ref2:
      idword
    { $$ = new ComponentRef(null, $1, null, false,
            Lists.<Expression>newList()); }
    | NEW idword LPAREN generic_args RPAREN
    { $$ = new ComponentRef(null, $2, null, true, $4); }
    ;

generic_args:
      /* empty */
    { $$ = Lists.<Expression>newList(); }
    | generic_arglist
    { $$ = $1; }
    ;

generic_arglist:
      generic_arg
    { $$ = Lists.<Expression>newList(); }
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
    { $$ = Lists.<Declaration>newList($1); }
    | just_datadef
    // FIXME
    //{ $$ = Lists.<Declaration>newList($1); }
    { $$ = Lists.<Declaration>newList(); }
    | cuses
    { $$ = Lists.<ComponentRef, Declaration>convert($1); }
    ;

connection:
      endpoint EQ endpoint SEMICOLON
    { $$ = new EqConnection(null, $1, $3); }
    | endpoint ARROW endpoint SEMICOLON
    { $$ = new RpConnection(null, $3, $1); }
    | endpoint LEFT_ARROW endpoint SEMICOLON
    { $$ = new RpConnection(null, $1, $3); }
    ;

endpoint:
      endpoint DOT parameterised_identifier
    {
        $1.setIds(Lists.<ParameterisedIdentifier>chain($1.getIds(), $3));
        $$ = $1;
    }
    | parameterised_identifier
    { $$ = new EndPoint(null, Lists.<ParameterisedIdentifier>newList($1)); }
    ;

parameterised_identifier:
      idword
    { $$ = new ParameterisedIdentifier(null, $1, null); }
    | idword LBRACK nonnull_exprlist RBRACK
    { $$ = new ParameterisedIdentifier(null, $1, $3); }
    ;

imodule:
      IMPLEMENTATION
    {
        $<Environment>$ = NescComponents.startImplementation();
        // TODO all_tasks = null
    }
    LBRACE extdefs RBRACE
    { $$ = new Module(null, $<Environment>2, $4); }
    ;

/* the reason for the strange actions in this rule
is so that notype_initdecls when reached via datadef
can find a valid list of type and sc specs in $0. */
//FIXME: not sure if extdefs will work properly.
extdefs:
    {
        $<TypeElement>$ = null;
        $<LinkedList>$ = Lists.<TypeElement> newList();
        wasTypedef = false;
    }
    extdef
    {
        // TODO check if null
        $$ = Lists.<Declaration>newListEmptyOnNull($2);
     }
    | extdefs
    {
        $<TypeElement>$ = null;
        $<LinkedList>$ = Lists.<TypeElement> newList();
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
        AsmStmt asmStmt = new AsmStmt(null, $3, Lists.<AsmOperand>newList(),
                Lists.<AsmOperand>newList(), Lists.<StringAst>newList(),
                Lists.<TypeElement>newList());

        $$ = new AsmDecl(null, asmStmt);
    }
    | extension extdef
    {
        // FIXME extension
        $$ = Declarations.makeExtensionDecl(0, null, $2);
    }
    ;

datadef:
      setspecs notype_initdecls SEMICOLON
    {
        $$ = Declarations.makeDataDecl(Lists.<TypeElement>newList(), $2);
        popDeclspecStack();
    }
    | just_datadef
    { $$ = $1; }
    ;

just_datadef:
      declspecs_nots setspecs notype_initdecls SEMICOLON
    {
        $$ = Declarations.makeDataDecl($1, $3);
        popDeclspecStack();
    }
    | declspecs_ts setspecs initdecls SEMICOLON
    {
        $$ = Declarations.makeDataDecl($1, $3);
        popDeclspecStack();
    }
    | declspecs setspecs SEMICOLON
    {
        // TODO
        $$ = Declarations.makeDataDecl($1, null);
        popDeclspecStack();
    }
    | error SEMICOLON
    { $$ = Declarations.makeErrorDecl(); }
    | error RBRACE
    { $$ = Declarations.makeErrorDecl(); }
    | SEMICOLON
    {
        // FIXME: should not return null
        $$ = null;
    }
    | target_def
    ;

//TODO
target_def:
      TARGET_DEF identifier EQ expr SEMICOLON
    {
        $$ = null;
    }
    ;

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
        /* NOTE: maybe asm can be null! */
        /* maybeasm is only here to avoid a s/r conflict */
        // TODO refuse_asm($1);

        /* $0 refers to the declarator that precedes fndef2
         in fndef (we can't just save it in an action, as that
         causes s/r and r/r conflicts) */
        // TODO
    }
    old_style_parm_decls
    {
        Semantics.storeParmDecls($4);
    }
    compstmt_or_error
    {
          // TODO
          $$ = Semantics.finishFunction($6);
          //popLevel();
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
    { $$ = new IdLabel(null, $1.id); }
    ;

idword:
      identifier
    { $$ = new Word(null, $1.id); }
    ;

unop:
      AND
    { $$ = LeftUnaryOperation.ADDRESS_OF; }
    | MINUS
    { $$ = LeftUnaryOperation.UNARY_MINUS; }
    | PLUS
    { $$ = LeftUnaryOperation.UNARY_PLUS; }
    | PLUSPLUS
    { $$ = LeftUnaryOperation.PREINCREMENT; }
    | MINUSMINUS
    { $$ = LeftUnaryOperation.PREDECREMENT; }
    | TILDE
    { $$ = LeftUnaryOperation.BITNOT; }
    | NOT
    { $$ = LeftUnaryOperation.NOT; }
    | REALPART
    { $$ = LeftUnaryOperation.REALPART; }
    | IMAGPART
    { $$ = LeftUnaryOperation.IMAGPART; }
    ;

expr:
      nonnull_exprlist
    {
        /*
         * When there is only one element in the expression list, then we
         * return only this one.
         */
        if ($1.size() == 1)
            $$ = $1.get(0);
        else
            $$ = Expressions.makeComma(null, $1);
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
    // FIXME
    //{ $$ = Lists.newList($1); }
    { $$ = Lists.<Expression>newList(); }
    | nonnull_exprlist_ COMMA expr_no_commas
    // FIXME
    //{ $$ = Lists.chain($1, $3); }
    { $$ = Lists.<Expression>newList(); }
    ;

callkind:
      CALL
    { $$ = NescCallKind.COMMAND_CALL; }
    | SIGNAL
    { $$ = NescCallKind.EVENT_SIGNAL; }
    | POST
    { $$ = NescCallKind.POST_TASK; }
    ;

//TODO : set first argument (location).
unary_expr:
      primary
    { $$ = $1; }
    | callkind function_call
    {
        FunctionCall fc = $2;
        $$ = $2;
        fc.setCallKind($1);
        /*
         * TODO: check whether apropriate modifier is specified, i.e.
         * commands must be called, events must be signaled, etc.
         */
    }
    | STAR cast_expr
    {
        $$ = Expressions.makeDereference(null, $2);
    }
    /* __extension__ turns off -pedantic for following primary.  */
    | extension cast_expr
    {
        $$ = Expressions.makeExtensionExpr(null, $2);
    }
    | unop cast_expr
    {
        $$ = Expressions.makeUnary(null, $1, $2);
    }
    /* Refer to the address of a label as a pointer.  */
    | ANDAND id_label
    {
        $$ = Expressions.makeLabelAddress(null, $2);
        Statements.useLabel($2);
    }
    | sizeof unary_expr
    {
        $$ = Expressions.makeSizeofExpr(null, $2);
    }
    | sizeof LPAREN typename RPAREN
    {
        $$ = Expressions.makeSizeofType(null, $3);
    }
    | alignof unary_expr
    {
        $$ = Expressions.makeAlignofExpr(null, $2);
    }
    | alignof LPAREN typename RPAREN
    {
        $$ = Expressions.makeAlignofType(null, $3);
    }
    ;

sizeof:
      SIZEOF
    {
        // TODO
    }
    ;

alignof:
      ALIGNOF
    {
        // TODO
    }
    ;

//TODO : set first argument (location).
cast_expr:
      unary_expr
    | LPAREN typename RPAREN cast_expr
    {
        Expressions.makeCast(null, $2, $4);
    }
    | LPAREN typename RPAREN LBRACE initlist_maybe_comma RBRACE
    {
        // TODO : I have no idea what is happening here!
    }
    ;

//TODO : set first argument (location).
expr_no_commas:
      cast_expr
    | expr_no_commas PLUS expr_no_commas
    {
        $$ = Expressions.makePlus(null, $1, $3);
    }
    | expr_no_commas MINUS expr_no_commas
    {
        $$ = Expressions.makeMinus(null, $1, $3);
    }
    | expr_no_commas STAR expr_no_commas
    {
        $$ = Expressions.makeTimes(null, $1, $3);
    }
    | expr_no_commas DIV expr_no_commas
    {
        $$ = Expressions.makeDivide(null, $1, $3);
    }
    | expr_no_commas MOD expr_no_commas
    {
        $$ = Expressions.makeModulo(null, $1, $3);
    }
    | expr_no_commas LSHIFT expr_no_commas
    {
        $$ = Expressions.makeLshift(null, $1, $3);
    }
    | expr_no_commas RSHIFT expr_no_commas
    {
        $$ = Expressions.makeRshift(null, $1, $3);
    }
    | expr_no_commas LTEQ expr_no_commas
    {
        $$ = Expressions.makeLeq(null, $1, $3);
    }
    | expr_no_commas GTEQ expr_no_commas
    {
        $$ = Expressions.makeGeq(null, $1, $3);
    }
    | expr_no_commas LT expr_no_commas
    {
        $$ = Expressions.makeLt(null, $1, $3);
    }
    | expr_no_commas GT expr_no_commas
    {
        $$ = Expressions.makeGt(null, $1, $3);
    }
    | expr_no_commas EQEQ expr_no_commas
    {
        $$ = Expressions.makeEq(null, $1, $3);
    }
    | expr_no_commas NOTEQ expr_no_commas
    {
        $$ = Expressions.makeNe(null, $1, $3);
    }
    | expr_no_commas AND expr_no_commas
    {
        $$ = Expressions.makeBitand(null, $1, $3);
    }
    | expr_no_commas OR expr_no_commas
    {
        $$ = Expressions.makeBitor(null, $1, $3);
    }
    | expr_no_commas XOR expr_no_commas
    {
        $$ = Expressions.makeBitxor(null, $1, $3);
    }
    | expr_no_commas ANDAND expr_no_commas
    {
        $$ = Expressions.makeAndand(null, $1, $3);
    }
    | expr_no_commas OROR expr_no_commas
    {
        $$ = Expressions.makeOror(null, $1, $3);
    }
    | expr_no_commas QUESTION expr COLON expr_no_commas
    {
        $$ = Expressions.makeConditional(null, $1, $3, $5);
    }
    | expr_no_commas QUESTION COLON expr_no_commas
    {
        $$ = Expressions.makeConditional(null, $1, null, $4);
    }
    | expr_no_commas EQ expr_no_commas
    {
        $$ = Expressions.makeAssign(null, $1, $3);
    }
    | expr_no_commas MULEQ expr_no_commas
    {
        $$ = Expressions.makeTimesAssign(null, $1, $3);
    }
    | expr_no_commas DIVEQ expr_no_commas
    {
        $$ = Expressions.makeDivideAssign(null, $1, $3);
    }
    | expr_no_commas MODEQ expr_no_commas
    {
        $$ = Expressions.makeModuloAssign(null, $1, $3);
    }
    | expr_no_commas PLUSEQ expr_no_commas
    {
        $$ = Expressions.makePlusAssign(null, $1, $3);
    }
    | expr_no_commas MINUSEQ expr_no_commas
    {
        $$ = Expressions.makeMinusAssign(null, $1, $3);
    }
    | expr_no_commas LSHIFTEQ expr_no_commas
    {
        $$ = Expressions.makeLshiftAssign(null, $1, $3);
    }
    | expr_no_commas RSHIFTEQ expr_no_commas
    {
        $$ = Expressions.makeRshiftAssign(null, $1, $3);
    }
    | expr_no_commas ANDEQ expr_no_commas
    {
        $$ = Expressions.makeBitandAssign(null, $1, $3);
    }
    | expr_no_commas XOREQ expr_no_commas
    {
        $$ = Expressions.makeBitxorAssign(null, $1, $3);
    }
    | expr_no_commas OREQ expr_no_commas
    {
        $$ = Expressions.makeBitorAssign(null, $1, $3);
    }
    ;

//TODO : set first argument (location).
primary:
      IDENTIFIER
    {
        // FIXME first and third argument.
        $$ = Expressions.makeIdentifier(null, $1.id, true);
    }
    | INTEGER_LITERAL
    {
        $$ = $1;
    }
    | FLOATING_POINT_LITERAL
    {
        $$ = $1;
    }
    | CHARACTER_LITERAL
    {
        $$ = $1;
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
        // TODO
    }
    | LPAREN compstmt RPAREN
    {
        // TODO
    }
    | function_call
    {
    	// FIXME
    	$$ = Expressions.makeIdentifier(null, new CString("fixme"), true);
    }
    | VA_ARG LPAREN expr_no_commas COMMA typename RPAREN
    {
    	// FIXME
    	$$ = Expressions.makeIdentifier(null, new CString("fixme"), true);
    }
    | OFFSETOF LPAREN typename COMMA fieldlist RPAREN
    {
    	// FIXME
    	$$ = Expressions.makeIdentifier(null, new CString("fixme"), true);
    }
    | primary LBRACK nonnull_exprlist RBRACK
    {
        // XXX: index is an expression list!
        $$ = Expressions.makeArrayRef(null, $1, $3);
    }
    | primary DOT identifier
    {
        $$ = Expressions.makeFieldRef(null, $1, $3.id);
    }
    | primary ARROW identifier
    {
        Expression dereference = Expressions.makeDereference(null, $1);
        $$ = Expressions.makeFieldRef(null, dereference, $3.id);
    }
    | primary PLUSPLUS
    {
        $$ = Expressions.makePostincrement(null, $1);
    }
    | primary MINUSMINUS
    {
        $$ = Expressions.makePostdecrement(null, $1);
    }
    ;

fieldlist:
      identifier
    { $$ = Lists.<String>newList($1.id.getData()); }
    | fieldlist DOT identifier
    { $$ = Lists.chain($1, $3.id.getData()); }
    ;

//TODO : set first argument (location).
function_call:
      primary LPAREN exprlist RPAREN
    { $$ = Expressions.makeFunctionCall(null, $1, $3); }
    ;

string:
      string_chain
    { $$ = $1; }
    | MAGIC_STRING
    { $$ = Expressions.makeIdentifier(null, $1.id, false); }
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
        // TODO : set first argument (location).
        $$ = Lists.<Declaration>chain($1, new EllipsisDecl(null));
    }
    ;

/* The following are analogous to decls and decl
 except that they do not allow nested functions.
 They are used for old-style parm decls.  */
datadecls:
      datadecl
    { $$ = Lists.<Declaration>newList($1); }
    | datadecls datadecl
    { $$ = Lists.<Declaration>chain($1, $2); }
    ;

/* We don't allow prefix attributes here because they cause reduce/reduce
 conflicts: we can't know whether we're parsing a function decl with
 attribute suffix, or function defn with attribute prefix on first old
 style parm.  */
datadecl:
      declspecs_ts_nosa setspecs initdecls SEMICOLON
    {
        $$ = Declarations.makeDataDecl($1, $3);
        popDeclspecStack();
    }
    | declspecs_nots_nosa setspecs notype_initdecls SEMICOLON
    {
        $$ = Declarations.makeDataDecl($1, $3);
        popDeclspecStack();
    }
    | declspecs_ts_nosa setspecs SEMICOLON
    {
        $$ = Declarations.makeDataDecl($1, null);
        popDeclspecStack();
    }
    | declspecs_nots_nosa SEMICOLON
    {
        $$ = Declarations.makeDataDecl($1, null);
        popDeclspecStack();
    }
    ;

/* This combination which saves a lineno before a decl
 is the normal thing to use, rather than decl itself.
 This is to avoid shift/reduce conflicts in contexts
 where statement labels are allowed.  */
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

/* records the type and storage class specs to use for processing
 the declarators that follow.
 Maintains a stack of outer-level values of pstate.declspecs,
 for the sake of parm declarations nested in function declarators.  */
setspecs:
    /* empty */
    {
        if (this.debug) {
            System.out.print("setspecs");
        }
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
        if (this.debug) {
            System.out.print(" Setting wasTypedef (false) ");
        }
    }
    ;

/* Possibly attributes after a comma, which should be saved in
 pstate.attributes */
maybe_resetattrs:
      maybe_attribute
    {
        if ($1 == null) {
            pstate.attributes = Lists.<Attribute>newList();
        } else {
            pstate.attributes = $1;
        }
    }
    ;

decl:
      declspecs_ts setspecs initdecls SEMICOLON
    {
        $$ = Declarations.makeDataDecl($1, $3);
        popDeclspecStack();
    }
    | declspecs_nots setspecs notype_initdecls SEMICOLON
    {
        $$ = Declarations.makeDataDecl($1, $3);
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
        $$ = Declarations.makeDataDecl($1, null);
        popDeclspecStack();
    }
    | extension decl
    {
        //$$ = Declarations.makeExtensionDecl($1.i, null, $2);
        $$ = Declarations.makeExtensionDecl(0, null, $2);
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
        // FIXME 1. 2.
        $$ = new Typename(null, null);
    }
    | COMPONENTREF DOT identifier
    {
        // FIXME 1. 2. 3.
        $$ = new ComponentTyperef(null, null, null);
    }
    | TYPEOF LPAREN expr RPAREN
    {
        $$ = new TypeofExpr(null, $3);
    }
    | TYPEOF LPAREN typename RPAREN
    {
        $$ = new TypeofType(null, $3);
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
        // FIXME 1. 2.
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
        final VariableDecl decl = Declarations.startDecl($1, $2, pstate.declspecs,
                true, prefixAttr($3));
        // FIXME startInit
        Init.startInit(decl, null);
        $<VariableDecl>$ = decl;
    }
      init
    {
          Init.finishInit();
          /*
           * $<declaration>5 : The result of anonymous rule is the fifth
           * element in the right-hand side of production.
           */
          final VariableDecl decl = (VariableDecl) $<VariableDecl>5;
          $$ = Declarations.finishDecl(decl, $6);
    }
/* Note how the declaration of the variable is in effect while its init is parsed! */
    | declarator maybeasm maybe_attribute
    {
        declareName($1, pstate.declspecs);
        VariableDecl decl = Declarations.startDecl($1, $2,  pstate.declspecs,
                false, prefixAttr($3));
        $$ = Declarations.finishDecl(decl, null);
    }
    ;

notype_initdcl:
      notype_declarator maybeasm maybe_attribute EQ
    {
        declareName($1, pstate.declspecs);
        final VariableDecl decl = Declarations.startDecl($1, $2, pstate.declspecs,
                true, prefixAttr($3));
        // FIXME startInit
        Init.startInit(decl, null);
        $<VariableDecl>$ = decl;
    }
      init
    {
          Init.finishInit();
          final VariableDecl decl = (VariableDecl) $<VariableDecl>5;
          $$ = Declarations.finishDecl(decl, $6);
    }
/* Note how the declaration of the variable is in effect while its init is parsed! */
    | notype_declarator maybeasm maybe_attribute
    {
        declareName($1, pstate.declspecs);
        VariableDecl decl = Declarations.startDecl($1, $2,  pstate.declspecs,
                false, prefixAttr($3));
        $$ = Declarations.finishDecl(decl, null);
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
        // Note: Attribute returns a list of attributes.
        $$ = $1;
    }
    | attributes attribute
    { $$ = Lists.chain($1, $2); }
    ;

attribute:
      ATTRIBUTE LPAREN LPAREN attribute_list RPAREN RPAREN
    { $$ = $4; }
    | target_attribute
    //{ $$ = Lists.<Attribute>newList($1); }
    // FIXME
    { $$ = Lists.<Attribute>newList(); }
    | nattrib
    //{ $$ = Lists.<Attribute>newList($1); }
    // FIXME
    { $$ = Lists.<Attribute>newList(); }
    ;

target_attribute:
      TARGET_ATTRIBUTE0
    {
        // FIXME 1. 2.
        Word w = new Word(null, null);
        $$ = new TargetAttribute(null, w, null);
    }
    | TARGET_ATTRIBUTE1 restricted_expr
    {
        // FIXME 1. 2.
        Word w = new Word(null, null);
        $$ = new TargetAttribute(null, w, Lists.<Expression>newList($2));
    }
    | AT restricted_expr
    {
        // FIXME word w = new_word(pr, $2->location, str2cstring(pr, "iar_at"));
        Word w = new Word(null, null);
        $$ = new TargetAttribute(null, w, Lists.<Expression>newList($2));
    }
    ;

restricted_expr:
      INTEGER_LITERAL
    {
        $$ = $1;
    }
    | FLOATING_POINT_LITERAL
    {
        $$ = $1;
    }
    | CHARACTER_LITERAL
    {
        $$ = $1;
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
        $$ = Lists.<Attribute>newListEmptyOnNull($1);
    }
    | attribute_list COMMA attrib
    { $$ = Lists.chainNotNull($1, $3); }
    ;

attrib:
      /* empty */
    { $$ = null; }
    | any_word
    { $$ = new GccAttribute(null, $1, null); }
    | any_word LPAREN IDENTIFIER RPAREN
    {
        $$ = new GccAttribute(null, $1,
                Semantics.makeAttrArgs(null, $3.id, null));
    }
    | any_word LPAREN IDENTIFIER COMMA nonnull_exprlist RPAREN
    {
        $$ = new GccAttribute(null, $1,
                Semantics.makeAttrArgs(null, $3.id, $5));
    }
    | any_word LPAREN exprlist RPAREN
    {
        $$ = new GccAttribute(null, $1, $3);
    }
    ;

nattrib:
      AT nastart LPAREN initlist_maybe_comma RPAREN
    {
        $$ = NescAttributes.finishAttributeUse($2, $4);
    }
    | AT nastart error RPAREN
    {
        $$ = NescAttributes.finishAttributeUse($2,
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
        $$ = new Word(null, new CString($1.getId().getName()));
    }
    | type_spec
    {
        $$ = new Word(null, new CString($1.getId().getName()));
    }
    | type_qual
    {
        $$ = new Word(null, new CString($1.getId().getName()));
    }
    | SIGNAL
    {
        $$ = new Word(null, new CString("signal"));
    }
    ;

/* Initializers.  `init' is the entry point.  */

init:
      expr_no_commas
    {
        Init.simpleInit($1);
        $$ = $1;
    }
    | LBRACE
    {
        Init.reallyStartIncrementalInit(null);
    }
      initlist_maybe_comma RBRACE
    {
        $$ = Init.makeInitList(null, $3);
    }
    | error
    { $$ = Expressions.makeErrorExpr(); }
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

/* `initelt' is a single element of an initializer.
 It may use braces.  */
initelt:
      designator_list EQ initval
    {
        $$ = Init.makeInitSpecific($1, $3);
    }
    | designator initval
    {
        $$ = Init.makeInitSpecific($1, $2);
    }
    | identifier COLON
    {
        $<Designator>$ = Init.setInitLabel(null, $1.id);
    }
      initval
    {
        $$ = Init.makeInitSpecific($<Designator>3, $4);
    }
    | initval
    { $$ = $1; }
    ;

initval:
      LBRACE
    {

    }
      initlist_maybe_comma RBRACE
    {
          $$ = Init.makeInitList(null, $3);
    }
    | expr_no_commas
    {
        $$ = $1;
    }
    | error
    { $$ = Expressions.makeErrorExpr(); }
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
        $$ = Init.setInitLabel(null, $2.id);
    }
    /* These are for labeled elements.  The syntax for an array element
       initializer conflicts with the syntax for an Objective-C message,
       so don't include these productions in the Objective-C grammar.  */
    | LBRACK expr_no_commas ELLIPSIS expr_no_commas RBRACK
    {
        $$ = Init.setInitIndex(null, $2, $4);
    }
    | LBRACK expr_no_commas RBRACK
    {
        $$ = Init.setInitIndex(null, $2, null);
    }
    ;

nested_function:
      declarator maybeasm maybe_attribute
    {
        /* maybeasm is only here to avoid a s/r conflict */
        // TODO refuse_asm

        if (!Semantics.startFunction(pstate.declspecs, $1, $3, true)) {
            // TODO error
        }

    }
      old_style_parm_decls
    {
        /*
         * FIXME: according to original parser (c-parse.y) we have:
         * store_parm_decls(declaration_reverse($3));
         * Should be $5?
         */
        Semantics.storeParmDecls($5);
    }
/* This used to use compstmt_or_error.
 That caused a bug with input `f(g) int g {}',
 where the use of YYERROR1 above caused an error
 which then was handled by compstmt_or_error.
 There followed a repeated execution of that same rule,
 which called YYERROR1 again, and so on.  */
      compstmt
    {
        $$ = Semantics.finishFunction($7);
        //popLevel();
    }
    ;

notype_nested_function:
      notype_declarator maybeasm maybe_attribute
    {
        /* maybeasm is only here to avoid a s/r conflict */
        // TODO refuse_asm

        if (!Semantics.startFunction(pstate.declspecs, $1, $3, true)) {
            // TODO error
        }

    }
      old_style_parm_decls
    {
        /*
         * FIXME: according to original parser (c-parse.y) we have:
         * store_parm_decls(declaration_reverse($3));
         * Should be $5?
         */
        Semantics.storeParmDecls($5);
    }
/* This used to use compstmt_or_error.
 That caused a bug with input `f(g) int g {}',
 where the use of YYERROR1 above caused an error
 which then was handled by compstmt_or_error.
 There followed a repeated execution of that same rule,
 which called YYERROR1 again, and so on.  */
      compstmt
    {
        $$ = Semantics.finishFunction($7);
        //popLevel();
    }
    ;

/* Any kind of declarator (thus, all declarators allowed
 after an explicit type_spec).  */

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
    { $$ = Semantics.makePointerDeclarator(null, $3, $2); }
    | LPAREN maybe_attribute after_type_declarator RPAREN
    {
        $$ = new QualifiedDeclarator(null, $3,
                Lists.<Attribute, TypeElement>convert($2));
    }
    | TYPEDEF_NAME
    { $$ = new IdentifierDeclarator(null, $1.id); }
    | TYPEDEF_NAME DOT identifier
    { $$ = NescModule.makeInterfaceRefDeclarator(null, $1.id, $3.id); }
    ;

/* Kinds of declarator that can appear in a parameter list
 in addition to notype_declarator.  This is like after_type_declarator
 but does not allow a typedef name in parentheses as an identifier
 (because it would conflict with a function with that typedef as arg).  */
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
    { $$ = Semantics.makePointerDeclarator(null, $3, $2); }
    | TYPEDEF_NAME
    { $$ = new IdentifierDeclarator(null, $1.id); }
    ;


/* A declarator allowed whether or not there has been
 an explicit type_spec.  These cannot redeclare a typedef-name.  */

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
    { $$ = Semantics.makePointerDeclarator(null, $3, $2); }
    | LPAREN maybe_attribute notype_declarator RPAREN
    {
        $$ = new QualifiedDeclarator(null, $3,
                Lists.<Attribute, TypeElement>convert($2));
        }
    | IDENTIFIER
    { $$ = new IdentifierDeclarator(null, $1.id); }
    | IDENTIFIER DOT identifier
    { $$ = NescModule.makeInterfaceRefDeclarator(null, $1.id, $3.id); }
    ;

tag:
      identifier
    { $$ = new Word(null, $1.id); }
    ;

structuse:
      structkind tag nesc_attributes
    { $$ = Semantics.makeXrefTag(null, $1.kind, $2); }
    | ENUM tag nesc_attributes
    { $$ = Semantics.makeXrefTag(null, StructKind.ENUM, $2); }
    ;

structdef:
      structkind tag nesc_attributes LBRACE
    {
        $<TagRef>$ = Semantics.startStruct(null, $1.kind, $2);
    }
      component_decl_list RBRACE maybe_attribute
    {
        $$ = Semantics.finishStruct($<TagRef>5, $6, Lists.<Attribute>chain($3, $8));
    }
    | STRUCT AT tag nesc_attributes LBRACE
    {
        $<TagRef>$ = Semantics.startStruct(null, StructKind.ATTRIBUTE, $3);
    }
      component_decl_list RBRACE maybe_attribute
    {
        $$ = Semantics.finishStruct($<TagRef>6, $7, Lists.<Attribute>chain($4, $9));
    }
    | structkind LBRACE component_decl_list RBRACE maybe_attribute
    {
        TagRef tagRef = Semantics.startStruct(null, $1.kind, null);
        $$ = Semantics.finishStruct(tagRef, $3, $5);
    }
    | ENUM tag nesc_attributes LBRACE
    {
        $<EnumRef>$ = Semantics.startEnum(null, $2);
    }
      enumlist maybecomma_warn RBRACE maybe_attribute
    {
          $$ = Semantics.finishEnum($<EnumRef>5, $6, Lists.<Attribute>chain($3, $9));
    }
    | ENUM LBRACE
    {
        $<EnumRef>$ = Semantics.startEnum(null, null);
    }
      enumlist maybecomma_warn RBRACE maybe_attribute
    {
        $$ = Semantics.finishEnum($<EnumRef>3, $4, $7);
    }
    ;

//FIXME
structkind:
      STRUCT
    {
        final Value.StructKindToken token = new Value.StructKindToken();
        token.kind = StructKind.STRUCT;
        $$ = token;
    }
    | UNION
    {
        final Value.StructKindToken token = new Value.StructKindToken();
        token.kind = StructKind.UNION;
        $$ = token;
    }
    | NX_STRUCT
    {
        final Value.StructKindToken token = new Value.StructKindToken();
        token.kind = StructKind.NX_STRUCT;
        $$ = token;
    }
    | NX_UNION
    {
        final Value.StructKindToken token = new Value.StructKindToken();
        token.kind = StructKind.NX_UNION;
        $$ = token;
    }
    ;

maybecomma:
      /* empty */
    | COMMA
    ;

maybecomma_warn:
      /* empty */
    | COMMA
    ;

component_decl_list:
      component_decl_list2
    { $$ = $1; }
    | component_decl_list2 component_decl
    { $$ = Lists.<Declaration>chain($1, $2); }
    ;

component_decl_list2:
      /* empty */
    { $$ = Lists.<Declaration>newList(); }
    | component_decl_list2 component_decl SEMICOLON
    { $$ = Lists.<Declaration>chain($1, $2); }
    | component_decl_list2 SEMICOLON
    { $$ = $1; }
    ;

/* There is a shift-reduce conflict here, because `components' may
 start with a `typename'.  It happens that shifting (the default resolution)
 does the right thing, because it treats the `typename' as part of
 a `typed_type_specs'.

 It is possible that this same technique would allow the distinction
 between `notype_initdecls' and `initdecls' to be eliminated.
 But I am being cautious and not trying it.  */

component_decl:
      declspecs_nosc_ts setspecs components
    {
        $$ = Declarations.makeDataDecl($1, $3);
        popDeclspecStack();
    }
    | declspecs_nosc_ts setspecs
    {
        $$ = Declarations.makeDataDecl($1, null);
        popDeclspecStack();
    }
    | declspecs_nosc_nots setspecs components_notype
    {
        $$ = Declarations.makeDataDecl($1, $3);
        popDeclspecStack();
    }
    | declspecs_nosc_nots setspecs
    {
        $$ = Declarations.makeDataDecl($1, null);
        popDeclspecStack();
    }
    | error
    { $$ = Declarations.makeErrorDecl(); }
    | extension component_decl
    //{ $$ = Declarations.makeExtensionDecl($1.i, null, $2); }
    { $$ = Declarations.makeExtensionDecl(0, null, $2); }
    ;

components:
      component_declarator
    { $$ = Lists.<Declaration>newList($1); }
    | components COMMA maybe_resetattrs component_declarator
    { $$ = Lists.<Declaration>chain($1, $4); }
    ;

/* It should be possible to use components after the COMMA, but gcc 3
 isn't doing this */
components_notype:
      component_notype_declarator
    { $$ = Lists.<Declaration>newList($1); }
    | components_notype COMMA maybe_resetattrs component_notype_declarator
    { $$ = Lists.<Declaration>chain($1, $4); }
    ;

component_declarator:
      declarator maybe_attribute
    { $$ = Semantics.makeField($1, null, pstate.declspecs, prefixAttr($2)); }
    | declarator COLON expr_no_commas maybe_attribute
    { $$ = Semantics.makeField($1, $3, pstate.declspecs, prefixAttr($4)); }
    | COLON expr_no_commas maybe_attribute
    { $$ = Semantics.makeField(null, $2, pstate.declspecs, prefixAttr($3)); }
    ;

component_notype_declarator:
      notype_declarator maybe_attribute
    { $$ = Semantics.makeField($1, null, pstate.declspecs, prefixAttr($2)); }
    | notype_declarator COLON expr_no_commas maybe_attribute
    { $$ = Semantics.makeField($1, $3, pstate.declspecs, prefixAttr($4)); }
    | COLON expr_no_commas maybe_attribute
    { $$ = Semantics.makeField(null, $2, pstate.declspecs, prefixAttr($3)); }
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
    { $$ = Semantics.makeEnumerator(null, $1.id, null); }
    | identifier EQ expr_no_commas
    { $$ = Semantics.makeEnumerator(null, $1.id, $3); }
    ;

typename:
      declspecs_nosc
    { } // TODO
      absdcl
    {
        /* NOTE: absdcl may be null! */
        $$ = Semantics.makeType($1, $3);
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
    { $$ = Semantics.makePointerDeclarator(null, $3, $2); }
    ;

absdcl1_ea:
      STAR maybe_type_quals_attrs
    { $$ = Semantics.makePointerDeclarator(null, null, $2); }
    | STAR maybe_type_quals_attrs absdcl1_ea
    { $$ = Semantics.makePointerDeclarator(null, $3, $2); }
    ;

direct_absdcl1:
      LPAREN maybe_attribute absdcl1 RPAREN
    {
        $$ = new QualifiedDeclarator(null, $3,
                Lists.<Attribute, TypeElement>convert($2));
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

fn_declarator:
      parameters LPAREN parmlist_or_identifiers_1 fn_quals
    { $$ = new FunctionDeclarator(null, null, $3, $1, $4, null); }
    | LPAREN parmlist_or_identifiers fn_quals
    {
        $$ = new FunctionDeclarator(null, null, $2,
                Lists.<Declaration>newList(), $3, null);
    }
    ;

absfn_declarator:
      LPAREN parmlist fn_quals
    {
        $$ = new FunctionDeclarator(null, null, $2,
                Lists.<Declaration>newList(), $3, null);
    }
    ;

array_declarator:
      LBRACK expr RBRACK
    { $$ = new ArrayDeclarator(null, null, $2); }
    | LBRACK RBRACK
    { $$ = new ArrayDeclarator(null, null, null); }
    ;

/* at least one statement, the first of which parses without error.  */
/* stmts is used only after decls, so an invalid first statement
 is actually regarded as an invalid decl and part of the decls.  */

stmts:
      stmt_or_labels
    {
        if ($1.i > 0) {
            // TODO last_statement, chain_with_labels
        }
        $$ = $1.stmts;
    }
    ;

stmt_or_labels:
      stmt_or_label
    {
        final Value.IStmts stmts = new Value.IStmts();
        stmts.i = $1.i;
        stmts.stmts = Lists.<Statement>newList($1.stmt);
        $$ = stmts;
    }
    | stmt_or_labels stmt_or_label
    {
        final Value.IStmts stmts = new Value.IStmts();
        stmts.i = $2.i;
        stmts.stmts = Semantics.chainWithLabels($1.stmts, Lists.<Statement>newList($2.stmt));
        $$ = stmts;
    }
    | stmt_or_labels errstmt
    {
        final Value.IStmts stmts = new Value.IStmts();
        stmts.i = 0;
        stmts.stmts = Lists.<Statement>newList(Statements.makeErrorStmt());
        $$ = stmts;
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
        pushLevel(false);    // for parsing purposes
        Semantics.pushlevel(false);
    }
    ;

/* Read zero or more forward-declarations for labels
 that nested functions can jump to.  */
maybe_label_decls:
      /* empty */
    { $$ = Lists.<IdLabel>newList(); }
    | label_decls
    { $$ = $1; }
    ;

label_decls:
      label_decl
    {
        // NOTE: label_decl is a list.
        $$ = $1;
    }
    | label_decls label_decl
    { $$ = Lists.chain($1, $2); }
    ;

label_decl:
      LABEL identifiers_or_typenames SEMICOLON
    { $$ = $2; }
    ;

/* This is the body of a function definition.
 It causes syntax errors to ignore to the next openbrace.  */
compstmt_or_error:
      compstmt
    | error compstmt
    { $$ = $2; }
    ;

/*
* TODO: should return location of '{'.
* TODO: compstmt_count?
*/
compstmt_start:
      LBRACE
    ;

//FIXME
compstmt:
      compstmt_start pushlevel RBRACE
    {
        popLevel();        // for parsing purposes
        Semantics.poplevel();
        // FIXME last param is poplevel()
        $$ = new CompoundStmt(null, null, null, null, null);
    }
    | compstmt_start pushlevel maybe_label_decls decls xstmts RBRACE
    {
        popLevel();        // for parsing purposes
        Semantics.poplevel();
        // FIXME last param is poplevel()
        $$ = new CompoundStmt(null, $3, $4, $5, null);
    }
    | compstmt_start pushlevel maybe_label_decls error RBRACE
    {
        popLevel();        // for parsing purposes
        Semantics.poplevel();
        $$ = Statements.makeErrorStmt();
    }
    | compstmt_start pushlevel maybe_label_decls stmts RBRACE
    {
        popLevel();        // for parsing purposes
        Semantics.poplevel();
        // FIXME last param is poplevel()
        $$ = new CompoundStmt(null, $3, null, $4, null);
    }
    ;

/* Value is number of statements counted as of the closeparen.  */
simple_if:
      if_prefix labeled_stmt
    {
        final Value.IStmt stmt = new Value.IStmt();
        stmt.i = $1.i;
        stmt.stmt = new IfStmt(null, $1.expr, $2, null);
        $$ = stmt;
    }
    | if_prefix error
    {
        final Value.IStmt stmt = new Value.IStmt();
        stmt.i = $1.i;
        stmt.stmt = Statements.makeErrorStmt();
        $$ = stmt;
    }
    ;

if_prefix:
      IF LPAREN expr RPAREN
    {
        final Value.IExpr expr = new Value.IExpr();
        expr.i = pstate.stmtCount;
        expr.expr = $3;
        $$ = expr;
    }
    ;

/* This is a subroutine of stmt.
 It is used twice, once for valid DO statements
 and once for catching errors in parsing the end test.  */
do_stmt_start:
      DO
    {
        pstate.stmtCount++;
        $<ConditionalStmt>$ = new DoWhileStmt(null, null, null);
    }
      labeled_stmt WHILE
    {
        $<ConditionalStmt>2.setStmt($3);
        $$ = $<ConditionalStmt>2;
    }
    ;

labeled_stmt:
      stmt
    { $$ = $1; }
    | label labeled_stmt
    { $$ = new LabeledStmt(null, $1, $2); }
    ;

stmt_or_label:
      stmt
    {
        final Value.IStmt stmt = new Value.IStmt();
        stmt.i = 0;
        stmt.stmt = $1;
        $$ = stmt;
    }
    | label
    {
        final Value.IStmt stmt = new Value.IStmt();
        stmt.i = 1;
        stmt.stmt = new LabeledStmt(null, $1, null);
        $$ = stmt;
    }
    ;

atomic_stmt:
      ATOMIC stmt_or_error
    {
        final AtomicStmt atomicStmt = new AtomicStmt(null, $2);
        $$ = atomicStmt;
    }
    ;

stmt_or_error:
      stmt
    { $$ = $1; }
    | error
    { $$ = Statements.makeErrorStmt(); }
    ;

/* Parse a single real statement, not including any labels.  */
stmt:
      compstmt
    {
        pstate.stmtCount++;
        $$ = $1;
    }
    | expr SEMICOLON
    {
        pstate.stmtCount++;
        $$ = new ExpressionStmt(null, $1);
    }
    | simple_if ELSE
    {
        $1.i = pstate.stmtCount;
    }
      labeled_stmt
    {
        if (pstate.stmtCount == $1.i) {
            // TODO warning("empty body in an else-statement");
        }
        $$ = $1.stmt;
        // TODO
    }
    | simple_if %prec IF
    {
        /* This warning is here instead of in simple_if, because we
         do not want a warning if an empty if is followed by an
         else statement.  Increment stmt_count so we don't
         give a second error if this is a nested `if'.  */
        if (pstate.stmtCount++ == $1.i) {
            // TODO warning_with_location ($1.stmt->location,
           // "empty body in an if-statement");
        }
        $$ = $1.stmt;
    }
    | simple_if ELSE error
    {
        $$ = Statements.makeErrorStmt();
    }
    | WHILE
    { pstate.stmtCount++; }
      LPAREN expr RPAREN
    {
        // TODO
        $<ConditionalStmt>$ = new WhileStmt(null, $4, null);
        // TODO
    }
      labeled_stmt
    {
        $<ConditionalStmt>6.setStmt($7);
        $$ = $<ConditionalStmt>6;
        // TODO
    }
    | do_stmt_start LPAREN expr RPAREN SEMICOLON
    {
        $$ = $1;
        $1.setCondition($3);
        // TODO
    }
    | do_stmt_start error
    {
        $$ = Statements.makeErrorStmt();
        // TODO
    }
    | FOR LPAREN xexpr SEMICOLON
    {
        /* NOTE: xexpr may be null */
        pstate.stmtCount++;
    }
      xexpr SEMICOLON
    {
        /* NOTE: xexpr may be null */
        // TODO
    }
      xexpr RPAREN
    {
        /* NOTE: xexpr may be null */
        $<ForStmt>$ = new ForStmt(null, $3, $6, $9, null);
        // TODO
    }
      labeled_stmt
    {
        $<ForStmt>11.setStmt($12);
        $$ = $<ForStmt>11;
        // TODO
    }
    | SWITCH LPAREN expr RPAREN
    {
        pstate.stmtCount++;
        // TODO
        $<ConditionalStmt>$ = new SwitchStmt(null, $3, null);
        // TODO
    }
      labeled_stmt
    {
        $<ConditionalStmt>5.setStmt($6);
        $$ = $<ConditionalStmt>5;
        // TODO
    }
    | BREAK SEMICOLON
    {
        pstate.stmtCount++;
        $$ = new BreakStmt(null);
        // TODO
    }
    | CONTINUE SEMICOLON
    {
        pstate.stmtCount++;
        $$ = new ContinueStmt(null);
        // TODO
    }
    | RETURN SEMICOLON
    {
        pstate.stmtCount++;
        $$ = Statements.makeVoidReturn(null);
    }
    | RETURN expr SEMICOLON
    {
        pstate.stmtCount++;
        $$ = Statements.makeReturn(null, $2);
    }
    | ASM_KEYWORD maybe_type_qual LPAREN expr RPAREN SEMICOLON
    {
        /* NOTE: maybe_type_qual may be null */
        pstate.stmtCount++;
        $$ = new AsmStmt(null, $4, Lists.<AsmOperand>newList(),
                Lists.<AsmOperand>newList(), Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($2));
    }
    /* This is the case with just output operands.  */
    | ASM_KEYWORD maybe_type_qual LPAREN expr COLON asm_operands RPAREN SEMICOLON
    {
        /* NOTE: maybe_type_qual may be null */
        pstate.stmtCount++;
        $$ = new AsmStmt(null, $4, $6, Lists.<AsmOperand>newList(),
                Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($2));
    }
    /* This is the case with input operands as well.  */
    | ASM_KEYWORD maybe_type_qual LPAREN expr COLON asm_operands COLON asm_operands RPAREN SEMICOLON
    {
        /* NOTE: maybe_type_qual may be null */
        pstate.stmtCount++;
        $$ = new AsmStmt(null, $4, $6, $8, Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($2));
    }
    /* This is the case with clobbered registers as well.  */
    | ASM_KEYWORD maybe_type_qual LPAREN expr COLON asm_operands COLON asm_operands COLON asm_clobbers RPAREN SEMICOLON
    {
        /* NOTE: maybe_type_qual may be null */
        pstate.stmtCount++;
        $$ = new AsmStmt(null, $4, $6, $8, $10,
                Lists.<TypeElement>newListEmptyOnNull($2));
    }
    | GOTO id_label SEMICOLON
    {
        pstate.stmtCount++;
        $$ = new GotoStmt(null, $2);
        // TODO
    }
    | GOTO STAR expr SEMICOLON
    {
        // TODO
        pstate.stmtCount++;
        $$ = new ComputedGotoStmt(null, $3);
        // TODO
    }
    | atomic_stmt
    {
        pstate.stmtCount++;
        $$ = $1;
    }
    | SEMICOLON
    {
        $$ = new EmptyStmt(null);
    }
    ;

/* Any kind of label, including jump labels and case labels.
 ANSI C accepts labels only before statements, but we allow them
 also at the end of a compound statement.  */

label:
      CASE expr_no_commas COLON
    {
        $$ = new CaseLabel(null, $2, null);
        // TODO
    }
    | CASE expr_no_commas ELLIPSIS expr_no_commas COLON
    {
        $$ = new CaseLabel(null, $2, $4);
        // TODO
    }
    | DEFAULT COLON
    {
        $$ = new DefaultLabel(null);
        // TODO
    }
    | id_label COLON
    {
        $$ = $1;
        // TODO
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
asm_operands:
      /* empty */
    { $$ = Lists.<AsmOperand>newList(); }
    | nonnull_asm_operands
    { $$ = $1; }
    ;

nonnull_asm_operands:
      asm_operand
    { $$ = Lists.newList($1); }
    | nonnull_asm_operands COMMA asm_operand
    { $$ = Lists.chain($1, $3); }
    ;

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

/* This is what appears inside the parens in a function declarator.
 Its value is a list of ..._TYPE nodes.  */
parmlist:
    {
      /*
       * NOTE: A strange thing, an action, even empty, MUST be in here.
       * Actions inside production, but not at the end of it, is converted
       * implicitly to a new production.
       * If this action were removed, erroneous parser would be produced.
       */
        pushLevel(true);    // for parsing purposes
        Semantics.pushlevel(true);
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
    | parms SEMICOLON
    {
        // TODO
    }
      parmlist_1
    { $$ = Lists.<Declaration>chain($1, $4); }
    | error RPAREN
    { $$ = Lists.<Declaration>newList(Declarations.makeErrorDecl()); }
    ;

/* This is what appears inside the parens in a function declarator.
 Is value is represented in the format that grokdeclarator expects.  */
parmlist_2:
      /* empty */
    { $$ = Lists.<Declaration>newList(); }
    | ELLIPSIS
    {
        $$ = Lists.<Declaration>newList(Declarations.makeErrorDecl());
    }
    | parms
    { $$ = $1; }
    | parms COMMA ELLIPSIS
    { $$ = Lists.<Declaration>chain($1, new EllipsisDecl(null)); }
    ;

parms:
      parm
    { $$ = Lists.<Declaration>newList($1); }
    | parms COMMA parm
    { $$ = Lists.<Declaration>chain($1, $3); }
    ;

/* A single parameter declaration or parameter type name,
 as found in a parmlist.  */
parm:
      declspecs_ts xreferror parm_declarator maybe_attribute
    { $$ = Semantics.declareParameter($3, $1, $4); }
    | declspecs_ts xreferror notype_declarator maybe_attribute
    { $$ = Semantics.declareParameter($3, $1, $4); }
    | declspecs_ts xreferror absdcl
    {
        /* NOTE: absdcl may be null */
        $$ = Semantics.declareParameter($3, $1, null);
    }
    | declspecs_ts xreferror absdcl1_noea attributes
    { $$ = Semantics.declareParameter($3, $1, $4); }
    | declspecs_nots xreferror notype_declarator maybe_attribute
    { $$ = Semantics.declareParameter($3, $1, $4); }
    | declspecs_nots xreferror absdcl
    {
        /* NOTE: absdcl may be null */
        $$ = Semantics.declareParameter($3, $1, null);
    }
    | declspecs_nots xreferror absdcl1_noea attributes
    { $$ = Semantics.declareParameter($3, $1, $4); }
    ;

xreferror:
      /* empty */
    {
        // TODO
    }
    ;

/* This is used in a function definition
 where either a parmlist or an identifier list is ok.
 Its value is a list of ..._TYPE nodes or a list of identifiers.  */
parmlist_or_identifiers:
    {
        pushLevel(true);    // for parsing purposes
        Semantics.pushlevel(true);
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
    { $$ = Semantics.declareOldParameter(null, $1.id); }
    ;

/* A nonempty list of identifiers, including typenames.  */
identifiers_or_typenames:
      id_label
    {
        $$ = Lists.<IdLabel>newList($1);
        // TODO
    }
    | identifiers_or_typenames COMMA id_label
    {
        $$ = Lists.<IdLabel>chain($1, $3);
        // TODO
    }
    ;

/* A possibly empty list of function qualifiers (only one exists so far) */
fn_quals:
      /* empty */
    { $$ = Lists.<TypeElement>newList(); }
    | fn_qual
    { $$ = Lists.<TypeElement>newList($1); }
    ;

extension:
      EXTENSION
    {
        $$ = null;
    }
    ;

/* FIXME : check if all specifiers were listed in productions below
(scspec, type_qual, fn_qual, type_spec)
*/
scspec:
      TYPEDEF
    {
        $$ = new Rid(null, RID.TYPEDEF);
        wasTypedef = true;
        if (this.debug) {
            System.out.println(" Setting wasTypedef (true) ");
        }
    }
    | EXTERN
    { $$ = new Rid(null, RID.EXTERN); }
    | STATIC
    { $$ = new Rid(null, RID.STATIC); }
    | AUTO
    { $$ = new Rid(null, RID.AUTO); }
    | REGISTER
    { $$ = new Rid(null, RID.REGISTER); }
    | COMMAND
    { $$ = new Rid(null, RID.COMMAND); }
    | EVENT
    { $$ = new Rid(null, RID.EVENT); }
    | ASYNC
    { $$ = new Rid(null, RID.ASYNC); }
    | TASK
    { $$ = new Rid(null, RID.TASK); }
    | NORACE
    { $$ = new Rid(null, RID.NORACE); }
    | DEFAULT
    { $$ = new Rid(null, RID.DEFAULT); }
    | INLINE
    { $$ = new Rid(null, RID.INLINE); }
    ;

type_qual:
      CONST
    { $$ = new Qualifier(null, RID.CONST); }
    | RESTRICT
    { $$ = new Qualifier(null, RID.RESTRICT); }
    | VOLATILE
    { $$ = new Qualifier(null, RID.VOLATILE); }
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
    { $$ = new Rid(null, RID.VOID); }
    | CHAR
    { $$ = new Rid(null, RID.CHAR); }
    | SHORT
    { $$ = new Rid(null, RID.SHORT); }
    | INT
    { $$ = new Rid(null, RID.INT); }
    | LONG
    { $$ = new Rid(null, RID.LONG); }
    | FLOAT
    { $$ = new Rid(null, RID.FLOAT); }
    | DOUBLE
    { $$ = new Rid(null, RID.DOUBLE); }
    | SIGNED
    { $$ = new Rid(null, RID.SIGNED); }
    | UNSIGNED
    { $$ = new Rid(null, RID.UNSIGNED); }
    ;

string_chain:
      STRING_LITERAL
    {
        final StringCst str = new StringCst(null, new CString($1));
        $$ = new StringAst(null, Lists.<StringCst>newList(str), null);
    }
    | string_chain STRING_LITERAL
    {
        final StringCst str = new StringCst(null, new CString($2));
        final LinkedList<StringCst> list = $<StringAst>1.getStrings();
        $$ = new StringAst(null, Lists.<StringCst>chain(list, str), null);
    }
    ;

%code {
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
     * Indicates whether parsing was successful.
     */
    private boolean errors;
    /**
     * Keeps data essential during parsing phase.
     */
    private ParserState pstate;
    /**
     * Symbol table. Enables to distinguish between IDENTIFIER and TYPENAME
     * token. Shared with LexerWrapper.
     */
    private SymbolTable symbolTable;
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
     *
     */
    private boolean debug;

    /**
     * Creates parser.
     * @param filePath
     *            currently being parsed file path
     * @param lex
     *            lexer
     * @param symbolTable
     *            symbol table
     * @param fileType
     *            fileType file type
     */
    public Parser(String filePath,
                  pl.edu.mimuw.nesc.lexer.Lexer lex,
                  SymbolTable symbolTable,
                  FileType fileType) {
        Preconditions.checkNotNull(filePath, "file path cannot be null");
        Preconditions.checkNotNull(lex, "lexer cannot be null");
        Preconditions.checkNotNull(symbolTable, "symbol table cannot be null");
        Preconditions.checkNotNull(fileType, "file type cannot be null");

        this.filePath = filePath;
        this.fileType = fileType;
        this.currentEntityName = Files.getNameWithoutExtension(filePath);
        this.symbolTable = symbolTable;
        this.lex = lex;
        this.lexer = new LexerWrapper(lex, this.symbolTable);
        this.yylexer = lexer;

        this.errors = false;
        this.pstate = new ParserState();

        switch (fileType) {
            case HEADER:
            case C:
                this.lexer.pushtoken(new Symbol(Lexer.DISPATCH_C, 0, 0, null, null));
                break;
            case NESC:
                this.lexer.pushtoken(new Symbol(Lexer.DISPATCH_NESC, 0, 0, null, null));
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
     * @return root of nesc entity abstract syntax tree
     */
    public Node getEntityRoot() {
        return this.entityRoot;
    }

    /**
     * Returns the list of definitions located prior to the nesc entity
     * definition.
     *
     * @return list of declarations
     */
    public List<Declaration> getExtdefs() {
        if (this.extdefs == null) {
            return new LinkedList<Declaration>();
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
     * Sets whether parser should work in debug mode.
     *
     * @param debug
     *            debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Adds new scope on the top of scopes stack.
     *
     * @param isParmLevel is parameter level
     */
    private void pushLevel(boolean isParmLevel) {
        if (this.debug) {
            System.out.print(" pushlevel ");
        }
        this.symbolTable.pushLevel(isParmLevel);
    }

    /**
     * Removes scope from the top of stack.
     */
    private void popLevel() {
        // XXX: see detect_bogus_env in semantics.c
        if (this.debug) {
            System.out.print(" poplevel ");
        }
        this.symbolTable.popLevel();
    }

    /**
     * Adds identifier as a type's name to current scope.
     *
     * @param identifier
     *            identifier
     */
    private void addTypename(String identifier) {
        this.symbolTable.add(identifier, Lexer.TYPEDEF_NAME);
    }

    /**
     * Adds identifier as a component's name to current scope.
     *
     * @param identifier
     *            identifier
     */
    private void addComponentRef(String identifier) {
        this.symbolTable.add(identifier, Lexer.COMPONENTREF);
    }

    /**
     * Adds identifier as a plain identifier (e.g. variable or function name) to
     * current scope.
     *
     * @param identifier
     *            identifier
     */
    private void addIdentifier(String identifier) {
        this.symbolTable.add(identifier, Lexer.IDENTIFIER);
    }

    /**
     *
     */
    private void popDeclspecStack() {
        if (this.debug) {
            System.out.print(" popDeclspecStack ");
        }
        pstate.popDeclspecStack();
    }

    /**
     *
     */
    private void pushDeclspecStack() {
        if (this.debug) {
            System.out.print(" pushDeclspecStack ");
        }
        pstate.pushDeclspecStack();
    }

    /**
     * Declares identifier as a type name or variable/function/array/etc
     * name depending on there was a TYPEDEF keyword in current declaration.
     *
     * @param identifier
     *            identifier
     * @param elements
     *            type elements
     */
    private void declareName(Declarator declarator, LinkedList<TypeElement> elements) {
        final boolean isTypedef = TypeElementUtils.isTypedef(elements);
        final String name = DeclaratorUtils.getDeclaratorName(declarator);
        if (this.debug) {
            final String msg = format("Add %s %s;",
                    (isTypedef ? "type" : "variable"), name);
            System.out.println(msg);
        }
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
     * @param postAttrs
     *            the list of attributes
     * @return a merged list of specified attributes and attributes kept in
     *         parser state
     */
    private LinkedList<Attribute> prefixAttr(LinkedList<Attribute> postAttrs) {
        return Lists.<Attribute> chain(pstate.attributes, postAttrs);
    }

    private void requireInterface(String interfaceName) {
        if (this.parserListener != null) {
            this.parserListener.interfaceDependency(filePath, interfaceName);
        }
    }

    private void requireComponent(String componentName) {
        if (this.parserListener != null) {
            this.parserListener.componentDependency(filePath, componentName);
        }
    }

    private void extdefsFinish() {
        if (this.debug) {
            System.out.println("extdefs finish;");
        }
        if (this.parserListener != null) {
            this.parserListener.extdefsFinished();
        }
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
        /**
         *
         */
        private String file;
        /**
         * Line.
         */
        private int line;
        /**
         * Column.
         */
        private int column;
        /**
         * For testing purposes.
         */
        private TokenPrinter tokenPrinter;
        /**
         * Current symbol value.
         */
        private Object object;
        
        private int prevLine;

        public LexerWrapper(pl.edu.mimuw.nesc.lexer.Lexer lexer, SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
            this.lexer = lexer;
            this.tokenQueue = new LinkedList<Symbol>();
            this.tokenPrinter = new TokenPrinter();
        }

        @Override
        public Object getLVal() {
            return object;
        }

        @Override
        public int yylex() throws java.io.IOException {
            Symbol symbol = poptoken();

            /*
             * Here is done the distinction between TYPEDEF_NAME, COMPONENTREF and
             * plain IDENTIFIER.
             */
            if (symbol.getSymbolCode() == IDENTIFIER) {
                final Value.IdToken idToken = (Value.IdToken) symbol.getValue();
                final String name = idToken.id.getData();
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

            /*
             * Assign new values to variables.
             */
            this.file = symbol.getFile();
            this.line = symbol.getLine();
            this.column = symbol.getColumn();
            this.object = symbol.getValue();

            if (Parser.this.debug) {
                this.tokenPrinter.print(symbol.getSymbolCode(), symbol.getValue());
            }
            return symbol.getSymbolCode();
        }

        @Override
        public void yyerror(String msg) {
            Parser.this.errors = true;
            final String message = format("%s in %s at line: %d, column: %d.",
                    msg, file, line, column);
            System.out.println(message);
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
         * @param symbol
         *            token
         */
        public void pushtoken(Symbol symbol) {
            this.tokenQueue.addFirst(symbol);
        }

    }


    }
;
%%
