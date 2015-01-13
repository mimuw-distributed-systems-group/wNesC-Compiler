/* TODO: borrowed from..., licence, etc... */

%code imports {
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.io.Files;
import org.apache.log4j.Logger;

import pl.edu.mimuw.nesc.*;
import pl.edu.mimuw.nesc.analysis.AttributeAnalyzer;
import pl.edu.mimuw.nesc.analysis.ExpressionsAnalysis;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.ast.*;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.ExpressionUtils;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.common.NesCFileType;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.nesc.*;
import pl.edu.mimuw.nesc.declaration.object.*;
import pl.edu.mimuw.nesc.environment.*;
import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;
import pl.edu.mimuw.nesc.problem.*;
import pl.edu.mimuw.nesc.parser.value.*;
import pl.edu.mimuw.nesc.astbuilding.*;
import pl.edu.mimuw.nesc.astbuilding.nesc.*;
import pl.edu.mimuw.nesc.token.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
%token <Symbol> INVALID_NUMBER_LITERAL "invalid number literal"
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
%type <FunctionCall> function_call nesc_call task_call
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
%type <LinkedList<Word>> fieldlist
%type <ValueStructKind> structkind

%type <ValueCallKind> iface_callkind
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
%type <TypeElementsAssociation> setspecs

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
 * NOTICE: DISPATCH_X tokens are fake. They were created to avoid
 * conflicts. The selection of a particular production depends on
 * the extension of source file (.nc, .c, etc.).
 */

dispatch:
      DISPATCH_NESC interface
    { entityRoot = $2; }
    | DISPATCH_NESC component
    { entityRoot = $2; }
    | DISPATCH_C extdefs
    {
        entityRoot = null;
        this.extdefs = $extdefs;
    }
    | DISPATCH_C
    { entityRoot = null; }
    | DISPATCH_PARM parm
    { entityRoot = $2; }
    | DISPATCH_PARM error
    { entityRoot = declarations.makeErrorDecl(); }
    | DISPATCH_TYPE typename
    { entityRoot = $2; }
    | DISPATCH_TYPE error
    { entityRoot = null; }
    ;

/*
* What is ncheader and includes?
* includes is deprecated and components can be preceded by
* arbitrary C declarations and macros. As a result, #include behaves in a more
* comprehensible fashion. For details on includes, see Section 9 of the
* nesC 1.1 reference manual.
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
      ncheader INTERFACE[keyword] idword[name]
    {
        pushLevel();
        environment.setScopeType(ScopeType.INTERFACE_PARAMETER);
        environment.setStartLocation($name.getEndLocation());
        entityStarted($name.getName());
        parserListener.nescEntityRecognized(NesCFileType.INTERFACE);
        final Interface iface = nescComponents.startInterface(environment, $keyword.getLocation(), $name);
        $<Interface>$ = iface;
    }
      interface_parms[params] nesc_attributes[attrs] LBRACE[lbrace]
    {
        pushLevel();
        environment.setEnclosedInGenericNescEntity($params != null && !$params.isEmpty());
        environment.setScopeType(ScopeType.INTERFACE);
        environment.setStartLocation($lbrace.getLocation());
        final Interface iface = $<Interface>4;
        nescComponents.handleInterfaceParametersAttributes(environment, iface, Optional.fromNullable($params), $attrs);
        $<Interface>$ = iface;
    }
      datadef_list[defs] RBRACE[rbrace]
    {
        final Interface iface = $<Interface>8;
        nescComponents.finishInterface(iface, $rbrace.getEndLocation(), $defs);

        /* Close interface scope. */
        environment.setEndLocation($rbrace.getEndLocation());
        popLevel();
        /* Close interface parameter scope. */
        environment.setEndLocation($rbrace.getEndLocation());
        popLevel();

        $$ = iface;
    }
    ;

interface_parms:
      /* empty */
    { $$ = null; }
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
      type_parm[parm] nesc_attributes[attrs]
    {
        final String paramName = $parm.getValue();
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        final TypeParmDecl decl = nescDeclarations.declareTypeParameter(environment, $parm.getLocation(),
                endLocation, paramName, $attrs);
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
      LBRACK[lbrack]
    {
        /*
         * Scope of parameters for:
         *  - parameterised interface in uses/provides section (in this
         *    case, scope type must be redefined).
         *    "uses interface Iface[int id];"
         *                 ------->        <-----
         *  - parameterised event/command
         *    "command Iface.foo[int id](int bar, char baz) { ... }"
         *      params env ---->              inner env --->      <<-- (inner, params)
         * poplevel is done by user of this production. In the second case
         * the scope of interface parameters and event/command parameters
         * is continuation of current scope.
         */
        pushLevel();
        environment.setScopeType(ScopeType.FUNCTION_PARAMETER);
        environment.setStartLocation($lbrack.getLocation());
    }
      parameters1[params]
    { $$ = $params; }
    ;

/*
 * NOTICE: update end location if current environment is not
 * environment for interface parameters in specification section.
 */
parameters1:
      parms RBRACK[rbrack]
    {
        environment.setEndLocation($rbrack.getEndLocation());
        $$ = $1;
    }
    | error RBRACK[rbrack]
    {
        environment.setEndLocation($rbrack.getEndLocation());
        $$ = Lists.<Declaration>newList(declarations.makeErrorDecl());
    }
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
      generic[isGeneric] MODULE[keyword] idword[name]
    {
        pushLevel();
        environment.setEnclosedInGenericNescEntity($isGeneric.getValue());
        environment.setScopeType(ScopeType.COMPONENT_PARAMETER);
        environment.setStartLocation($name.getEndLocation());
        entityStarted($name.getName());
        parserListener.nescEntityRecognized(NesCFileType.MODULE);
        final Location startLocation = $isGeneric.getValue()
                ? $isGeneric.getLocation()
                : $keyword.getLocation();
        final Module module = nescComponents.startModule(environment, startLocation, $name,
                $isGeneric.getValue());
        $<Module>$ = module;
    }
      component_parms[params] nesc_attributes[attrs] LBRACE[lbrace]
    {
        pushLevel();
        environment.setScopeType(ScopeType.SPECIFICATION);
        environment.setStartLocation($lbrace.getLocation());
        final Module module = $<Module>4;
        nescComponents.handleComponentParametersAttributes(environment, module, Optional.fromNullable($params), $attrs);
        $<Module>$ = module;
    }
      requires_or_provides_list[specification]
    {
        final Module module = $<Module>8;
        nescComponents.handleComponentSpecification(module, $specification);
        final ModuleTable moduleTable = nescComponents.handleModuleSpecification(module, $specification);
        declarations.setModuleTable(moduleTable);
        $<Module>$ = module;
    }
      RBRACE[rbrace] imodule[impl]
    {
        final Module module = $<Module>10;
        nescComponents.finishComponent(module, $impl);
        nescComponents.finishModule(module, $name.getEndLocation());

        // implementation scope handled in imodule
        // specification scope
        environment.setEndLocation($impl.getEndLocation());
        popLevel();
        // component parameter scope
        environment.setEndLocation($impl.getEndLocation());
        popLevel();

        $$ = module;
    }
    ;

configuration:
      generic[isGeneric] CONFIGURATION[keyword] idword[name]
    {
        pushLevel();
        environment.setEnclosedInGenericNescEntity($isGeneric.getValue());
        environment.setScopeType(ScopeType.COMPONENT_PARAMETER);
        environment.setStartLocation($name.getEndLocation());
        entityStarted($name.getName());
        parserListener.nescEntityRecognized(NesCFileType.CONFIGURATION);
        final Location startLocation = $isGeneric.getValue()
                ? $isGeneric.getLocation()
                : $keyword.getLocation();
        final Configuration configuration = nescComponents.startConfiguration(environment, startLocation,
                $name, $isGeneric.getValue());
        $<Configuration>$ = configuration;
    }
      component_parms[params] nesc_attributes[attrs] LBRACE[lbrace]
    {
        pushLevel();
        environment.setScopeType(ScopeType.SPECIFICATION);
        environment.setStartLocation($lbrace.getLocation());
        final Configuration configuration = $<Configuration>4;
        nescComponents.handleComponentParametersAttributes(environment, configuration, Optional.fromNullable($params),
                $attrs);
        $<Configuration>$ = configuration;
    }
      requires_or_provides_list[specification]
    {
        final Configuration configuration = $<Configuration>8;
        nescComponents.handleComponentSpecification(configuration, $specification);
        nescComponents.handleConfigurationSpecification(configuration, $specification);
        $<Configuration>$ = configuration;
    }
      RBRACE[rbrace] iconfiguration[impl]
    {
        final Configuration configuration = $<Configuration>10;
        nescComponents.finishComponent(configuration, $impl);
        nescComponents.finishConfiguration(configuration, $name.getEndLocation());

        // implementation scope handled in iconfiguration
        // specification scope
        environment.setEndLocation($impl.getEndLocation());
        popLevel();
        // component parameter scope
        environment.setEndLocation($impl.getEndLocation());
        popLevel();

        $$ = configuration;
    }
    ;

binary_component:
      COMPONENT idword nesc_attributes LBRACE requires_or_provides_list RBRACE
    {
        entityStarted($2.getName());
        final BinaryComponentImpl dummy = new BinaryComponentImpl($1.getLocation());
        final BinaryComponent component = new BinaryComponent($1.getLocation(), $3, $2, $5, dummy, false,
                Optional.<LinkedList<Declaration>>absent());
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
        $$ = null;
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
        $$ = nescDeclarations.declareTemplateParameter(environment, Optional.of($3), $1, $4);
    }
    | declspecs_ts xreferror notype_declarator maybe_attribute
    {
        $$ = nescDeclarations.declareTemplateParameter(environment, Optional.of($3), $1, $4);
    }
    | declspecs_nots xreferror notype_declarator maybe_attribute
    {
        $$ = nescDeclarations.declareTemplateParameter(environment, Optional.of($3), $1, $4);
    }
    | declspecs_ts xreferror
    {
        $$ = nescDeclarations.declareTemplateParameter(environment, Optional.<Declarator>absent(), $1,
                Lists.<Attribute>newList());
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
        $$ = Lists.<Declaration>newListEmptyOnNull($1);
    }
    ;

requires:
      USES[keyword]
    {
        pstate.insideUsesProvides = true;
    }
      parameterised_interface_list[ifaceList]
    {
        pstate.insideUsesProvides = false;
        final RequiresInterface requires = new RequiresInterface($keyword.getLocation(), $ifaceList);
        // list maybe empty (erroneous but possible case)
        final Location endLocation = AstUtils.getEndLocation($keyword.getEndLocation(), AstUtils.getEndLocation($ifaceList));
        requires.setEndLocation(endLocation);
        $$ = requires;
    }
    ;

provides:
      PROVIDES[keyword]
    {
        pstate.insideUsesProvides = true;
    }
      parameterised_interface_list[ifaceList]
    {
        pstate.insideUsesProvides = false;
        final ProvidesInterface provides = new ProvidesInterface($keyword.getLocation(), $ifaceList);
        // list maybe empty (erroneous but possible case)
        final Location endLocation = AstUtils.getEndLocation($keyword.getEndLocation(), AstUtils.getEndLocation($ifaceList));
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
    | interface_ref[ifaceRef] nesc_attributes[attrs] SEMICOLON[semi]
    {
        nescComponents.declareInterfaceRef(environment, $ifaceRef, Optional.<LinkedList<Declaration>>absent(), $attrs);
        $ifaceRef.setEndLocation($semi.getEndLocation());
        $$ = $ifaceRef;
    }
    | interface_ref[ifaceRef] parameters[params] nesc_attributes[attrs] SEMICOLON[semi]
    {
        // NOTICE: corresponding pushLevel() called in parameters
        // we are in interface reference parameters scope
        environment.setScopeType(ScopeType.OTHER);
        popLevel();
        nescComponents.declareInterfaceRef(environment, $ifaceRef, Optional.of($params), $attrs);
        $ifaceRef.setEndLocation($semi.getEndLocation());
        $$ = $ifaceRef;
    }
    ;

interface_ref:
      interface_type[ifaceRef]
    {
        $ifaceRef.setAlias(Optional.<Word>absent());
        $$ = $ifaceRef;
    }
    | interface_type[ifaceRef] AS idword[alias]
    {
        $ifaceRef.setAlias(Optional.of($alias));
        $ifaceRef.setEndLocation($alias.getEndLocation()); // maybe updated in higher productions
        $$ = $ifaceRef;
    }
    ;

interface_type:
      INTERFACE[keyword] idword[name]
    {
        requireInterface($name);
        final InterfaceRef ifaceRef = new InterfaceRef($keyword.getLocation(), $name,
                Optional.<LinkedList<Expression>>absent());
        ifaceRef.setEndLocation($name.getEndLocation());   // maybe updated in higher productions
        $$ = ifaceRef;
    }
    | INTERFACE[keyword] idword[name]
    {
        requireInterface($name);
    }
      LT typelist[args] GT[gt]
    {
        final InterfaceRef ifaceRef = new InterfaceRef($keyword.getLocation(), $name, Optional.of($args));
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
      IMPLEMENTATION[keyword]
    {
        pushLevel();
        environment.setScopeType(ScopeType.CONFIGURATION_IMPLEMENTATION);
        environment.setStartLocation($keyword.getLocation());
    }
      LBRACE[lbrace] configuration_decls[decls] RBRACE[rbrace]
    {
        final ConfigurationImpl impl = new ConfigurationImpl($keyword.getLocation(), $decls);
        impl.setEndLocation($rbrace.getEndLocation());
        impl.setEnvironment(environment);

        environment.setEndLocation($rbrace.getEndLocation());
        popLevel();

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
      component_ref2[ref]
    {
        requireComponent($ref.getName());
        /* Put component's name into parser's symbol table. */
        nescDeclarations.declareComponentRef(environment, $ref.getLocation(), $ref.getEndLocation(),
                $ref, Optional.<Word>absent());
        $$ = $ref;
    }
    | component_ref2[ref] AS idword[alias]
    {
        requireComponent($ref.getName());
        /* Put component's alias into parser's symbol table. */
        nescDeclarations.declareComponentRef(environment, $alias.getLocation(), $alias.getEndLocation(),
                $ref, Optional.of($alias));
        $$ = $ref;
    }
    ;

component_ref2:
      idword
    {
        final ComponentRef ref = new ComponentRef($1.getLocation(), $1, false, Lists.<Expression>newList());
        ref.setEndLocation($1.getEndLocation());   // maybe updated in higher productions
        $$ = ref;
    }
    | NEW idword LPAREN generic_args RPAREN
    {
        final ComponentRef ref = new ComponentRef($1.getLocation(), $2, true, $4);
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
        analyzeExpression(Optional.of($expr_no_commas));
        Expressions.defaultConversionForAssignment($1);
        $$ = $1;
    }
    | generic_type
    { $$ = $1; }
    ;

generic_type:
      typename
    { $$ = NescExpressions.makeTypeArgument($1); }
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
    {
        $$ = nescComponents.makeEqConnection($1, $3, $1.getLocation(), $4.getLocation(), environment);
    }
    | endpoint ARROW endpoint SEMICOLON
    {
        $$ = nescComponents.makeRpConnection($1, $3, $1.getLocation(), $4.getLocation(), environment);
    }
    | endpoint LEFT_ARROW endpoint SEMICOLON
    {
        $$ = nescComponents.makeRpConnection($3, $1, $1.getLocation(), $4.getLocation(), environment);
    }
    ;

endpoint:
      endpoint DOT parameterised_identifier
    {
        $1.setIds(Lists.<ParameterisedIdentifier>chain($1.getIds(), $3));
        $1.setEndLocation($parameterised_identifier.getEndLocation());
        $$ = $1;
    }
    | parameterised_identifier
    {
        final EndPoint result = new EndPoint($1.getLocation(), Lists.<ParameterisedIdentifier>newList($1));
        result.setEndLocation($parameterised_identifier.getEndLocation());
        $$ = result;
    }
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
        analyzeExpressions($3);
        final ParameterisedIdentifier id = new ParameterisedIdentifier($1.getLocation(), $1, $3);
        id.setEndLocation($4.getEndLocation());
        $$ = id;
    }
    ;

imodule:
      IMPLEMENTATION[keyword]
    {
        pushLevel();
        environment.setScopeType(ScopeType.MODULE_IMPLEMENTATION);
        environment.setStartLocation($keyword.getLocation());
    }
      LBRACE[lbrace] extdefs[defs] RBRACE[rbrace]
    {
        final ModuleImpl impl = new ModuleImpl($keyword.getLocation(), $defs);
        impl.setEndLocation($rbrace.getEndLocation());

        environment.setEndLocation($rbrace.getEndLocation());
        impl.setEnvironment(environment);
        popLevel();

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
    }
    extdef[def]
    {
        if ($def == null) {
            $$ = Lists.<Declaration>newList();
        } else {
            $$ = Lists.<Declaration>newList($def);
        }
    }
    | extdefs[defs]
    {
        $<TypeElement>$ = null;
        $<LinkedList>$ = Lists.<TypeElement>newList();
    }
    extdef[def]
    {
        if ($def == null) {
            $$ = $defs;
        } else {
            $$ = Lists.<Declaration>chain($defs, $def);
        }
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
        analyzeExpression(Optional.of($expr));
        final AsmStmt asmStmt = new AsmStmt($1.getLocation(), $3, Lists.<AsmOperand>newList(),
                Lists.<AsmOperand>newList(), Lists.<StringAst>newList(), Lists.<TypeElement>newList());
        asmStmt.setEndLocation($5.getEndLocation());
        final AsmDecl asmDecl = new AsmDecl($1.getLocation(), asmStmt);
        asmDecl.setEndLocation($5.getEndLocation());
        $$ = asmDecl;
    }
    | extension extdef
    {
        $$ = declarations.makeExtensionDecl($1.getLocation(), $2.getEndLocation(), $2);
        // pedantic = 0
    }
    ;

datadef:
      setspecs notype_initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($2).get();
        $$ = declarations.makeDataDecl(environment, startLocation, $3.getEndLocation(), $1, $2);
        popDeclspecStack();
    }
    | just_datadef
    { $$ = $1; }
    ;

just_datadef:
      declspecs_nots setspecs notype_initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = declarations.makeDataDecl(environment, startLocation, $4.getEndLocation(), $2, $3);
        popDeclspecStack();
    }
    | declspecs_ts setspecs initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = declarations.makeDataDecl(environment, startLocation, $4.getEndLocation(), $2, $3);
        popDeclspecStack();
    }
    | declspecs setspecs SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($3.getLocation(), $1);
        $$ = declarations.makeDataDecl(environment, startLocation, $3.getEndLocation(), $2, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | error SEMICOLON
    {
        $$ = declarations.makeErrorDecl();
    }
    | error RBRACE
    {
        $$ = declarations.makeErrorDecl();
    }
    | SEMICOLON
    {
        /* Redundant semicolon after declaration. */
        final EmptyDecl empty = new EmptyDecl($1.getLocation());
        empty.setEndLocation($1.getEndLocation());
        $$ = empty;
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
      maybeasm[asm] maybe_attribute[attrs]
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
        final Location startLocation = declarator.getLocation();
        final Optional<FunctionDecl> decl = declarations.startFunction(environment, startLocation, pstate.declspecs.getTypeElements(),
                declarator, $attrs, true);
        if (!decl.isPresent()) {
            error(declarator.getLocation(), Optional.of(declarator.getEndLocation()),
                "syntax error, expected function definition");

            $<FunctionDecl>$ = null;
        } else {
            /* Push parameters environment. There is no need to change its parent. */
            final Environment paramEnv = DeclaratorUtils.getFunctionDeclarator(declarator).getEnvironment();
            paramEnv.setScopeType(ScopeType.FUNCTION_PARAMETER);
            /*
             * Start location is already set. End location is equal to the end
             * location of parameter list in declarator.
             * Eventually the end of parameter environment should be equal to
             * the end of body environment. It is set on poplevel, but in case
             * function body contains syntax errors, we set absent end location
             * to indicate that the end of scope is unknown (if it does not
             * contain syntax errors, the null end location will be overwriten
             * on poplevel as it was mentioned above).
             */
            paramEnv.setEndLocation(null);
            pushLevel(paramEnv);

            $<FunctionDecl>$ = decl.get();
        }
    }
    old_style_parm_decls[parms]
    {
        if ($<FunctionDecl>3 == null) {
            $<FunctionDecl>$ = null;
        } else {
            $<FunctionDecl>$ = declarations.setOldParams($<FunctionDecl>3, $parms);
        }
        pstate.newFunctionScope = true;
    }
    compstmt_or_error[body]
    {
        pstate.newFunctionScope = false;
        pstate.labelsNamesStack.pop();

        if ($<FunctionDecl>5 == null) {
            $$ = null;
        } else {
            $$ = declarations.finishFunction($<FunctionDecl>5, $body, false);
            /*
             * Parameter environment, has the same start and end location
             * as body environment.
             */
            environment.setStartLocation($body.getLocation());
            environment.setEndLocation($body.getEndLocation());
            popLevel();
        }

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

iface_callkind:
      CALL
    { $$ = new ValueCallKind($1.getLocation(), $1.getEndLocation(), NescCallKind.COMMAND_CALL); }
    | SIGNAL
    { $$ = new ValueCallKind($1.getLocation(), $1.getEndLocation(), NescCallKind.EVENT_SIGNAL); }
    ;

unary_expr:
      primary
    { $$ = $1; }
    | nesc_call
    { $$ = $1; }
    | task_call
    { $$ = $1; }
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
        $2.setIsColonTerminated(false);
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
        final InitList initList = initializers.makeInitList(environment, $4.getLocation(), $6.getEndLocation(), $5);
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
        $$ = Expressions.makeConditional($1, Optional.<Expression>of($3), $5);
    }
    | expr_no_commas QUESTION COLON expr_no_commas
    {
        $$ = Expressions.makeConditional($1, Optional.<Expression>absent(), $4);
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
        $$ = Expressions.makeIdentifier($1.getLocation(), $1.getEndLocation(), $1.getValue());
    }
    | INTEGER_LITERAL
    {
        $$ = Expressions.makeIntegerCst($1.getValue(), $1.getLocation(), $1.getEndLocation());
    }
    | FLOATING_POINT_LITERAL
    {
        final FloatingCst cst = new FloatingCst($1.getLocation(), $1.getValue());
        cst.setEndLocation($1.getEndLocation());
        $$ = cst;
    }
    | INVALID_NUMBER_LITERAL
    {
        $$ = Expressions.makeErrorExpr();
    }
    | CHARACTER_LITERAL
    {
        $$ = Expressions.makeCharacterCst($1.getValue(), $1.getLocation(), $1.getEndLocation());
    }
    | string
    {
        $$ = $1;
    }
    | LPAREN expr RPAREN
    {
        final int parenthesesCount = Optional.fromNullable($expr.getParenthesesCount()).or(0) + 1;
        $expr.setParenthesesCount(parenthesesCount);
        $$ = $expr;
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
        $$ = Expressions.makeOffsetof($1.getLocation(), $6.getEndLocation(), $3, $5);
    }
    | primary LBRACK nonnull_exprlist RBRACK
    {
        /* NOTICE: The ambiguity between array reference and generic call is
         * resolved by introducing a new non-terminal nesc_call. */
        $$ = Expressions.makeArrayRef($1.getLocation(), $4.getEndLocation(), $1, $3);
    }
    | primary[exp] DOT identifier[id]
    {
        // NOTICE: ambiguity: field reference or component dereference?
        final boolean isComponentDeref;
        if ($exp instanceof Identifier) {
            final Identifier id = (Identifier) $exp;
            final Optional<? extends ObjectDeclaration> symbol = environment.getObjects().get(id.getName());
            isComponentDeref = symbol.isPresent() && (symbol.get().getKind() == ObjectKind.COMPONENT);
        } else {
            isComponentDeref = false;
        }
        if (isComponentDeref) {
            final Word fieldWord = makeWord($id.getLocation(), $id.getEndLocation(), $id.getValue());
            $$ = NescExpressions.makeComponentDeref((Identifier) $exp, fieldWord);
        } else {
            $$ = Expressions.makeFieldRef($exp.getLocation(), $id.getEndLocation(), $exp, $id.getValue());
        }
    }
    | primary ARROW identifier
    {
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
    {
        final Word w = new Word($1.getLocation(), $1.getValue());
        $$ = Lists.<Word>newList(w);
    }
    | fieldlist DOT identifier
    {
        final Word w = new Word($3.getLocation(), $3.getValue());
        $$ = Lists.chain($1, w);
    }
    ;

function_call:
      primary LPAREN exprlist RPAREN
    { $$ = Expressions.makeFunctionCall(environment, $1.getLocation(), $4.getEndLocation(), $1, $3); }
    ;

nesc_call:
      /* bare command/event */
      iface_callkind[kind] identifier[name] LPAREN exprlist[params] RPAREN[rparen]
    {
        final Identifier id = Expressions.makeIdentifier($name.getLocation(), $name.getEndLocation(), $name.getValue());
        $$ = Expressions.makeFunctionCall(environment, $kind.getLocation(), $rparen.getEndLocation(), id, $params,
                $kind.getCallKind());
    }
    /* bare command/event */
    | iface_callkind[kind] identifier[name] LBRACK[lbrack] nonnull_exprlist[generic_params] RBRACK[rbrack] LPAREN exprlist[params] RPAREN[rparen]
    {
        final Identifier id = Expressions.makeIdentifier($name.getLocation(), $name.getEndLocation(), $name.getValue());
        final GenericCall genericCall = NescExpressions.makeGenericCall($lbrack.getLocation(), $rbrack.getEndLocation(),
                id, $generic_params);
        $$ = Expressions.makeFunctionCall(environment, $kind.getLocation(), $rparen.getEndLocation(), genericCall, $params,
                $kind.getCallKind());
    }
    | iface_callkind[kind] identifier[iface] DOT identifier[name] LPAREN exprlist[params] RPAREN[rparen]
    {
        final Identifier ifaceId = Expressions.makeIdentifier($iface.getLocation(), $iface.getEndLocation(),
                $iface.getValue());
        final Word methodWord = makeWord($name.getLocation(), $name.getEndLocation(), $name.getValue());
        final InterfaceDeref ifaceDeref = NescExpressions.makeInterfaceDeref(ifaceId, methodWord);
        $$ = Expressions.makeFunctionCall(environment, $kind.getLocation(), $rparen.getEndLocation(), ifaceDeref, $params,
                $kind.getCallKind());
    }
    | iface_callkind[kind] identifier[iface] DOT identifier[name] LBRACK[lbrack] nonnull_exprlist[generic_params] RBRACK[rbrack] LPAREN exprlist[params] RPAREN[rparen]
    {
        final Identifier ifaceId = Expressions.makeIdentifier($iface.getLocation(), $iface.getEndLocation(),
                $iface.getValue());
        final Word methodWord = makeWord($name.getLocation(), $name.getEndLocation(), $name.getValue());
        final InterfaceDeref ifaceDeref = NescExpressions.makeInterfaceDeref(ifaceId, methodWord);
        final GenericCall genericCall = NescExpressions.makeGenericCall($lbrack.getLocation(), $rbrack.getEndLocation(),
                ifaceDeref, $generic_params);
        $$ = Expressions.makeFunctionCall(environment, $kind.getLocation(), $rparen.getEndLocation(), genericCall, $params,
                $kind.getCallKind());
    }
    ;

task_call:
      POST[post] function_call[function]
    {
        $function.setLocation($post.getLocation());
        $function.setCallKind(NescCallKind.POST_TASK);
        $$ = $function;
    }
    ;

string:
      string_chain
    { $$ = $1; }
    | MAGIC_STRING
    { $$ = Expressions.makeIdentifier($1.getLocation(), $1.getEndLocation(), $1.getValue()); }
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
        $$ = declarations.makeDataDecl(environment, startLocation, $4.getEndLocation(), $2, $3);
        popDeclspecStack();
    }
    | declspecs_nots_nosa setspecs notype_initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = declarations.makeDataDecl(environment, startLocation, $4.getEndLocation(), $2, $3);
        popDeclspecStack();
    }
    | declspecs_ts_nosa setspecs SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($3.getLocation(), $1);
        $$ = declarations.makeDataDecl(environment, startLocation, $3.getEndLocation(), $2, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | declspecs_nots_nosa SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($2.getLocation(), $1);
        $$ = declarations.makeDataDecl(environment, startLocation, $2.getEndLocation(), new TypeElementsAssociation($1),
              Lists.<Declaration>newList());
        popDeclspecStack();
    }
    ;

/*
 * This combination which saves a lineno before a decl is the normal thing to
 * use, rather than decl itself. This is to avoid shift/reduce conflicts in
 * contexts where statement labels are allowed.
 */
/*
 * NOTICE: decl may be null! (When syntax error occurs in nested_function).
 */
decls:
      decl[declaration]
    {
        $$ = Lists.<Declaration>newListEmptyOnNull($declaration);
    }
    | errstmt
    { $$ = Lists.<Declaration>newList(declarations.makeErrorDecl()); }
    | decls[decl_list] decl[declaration]
    {
        if ($declaration == null) {
            $$ = $decl_list;
        } else {
            $$ = Lists.<Declaration>chain($decl_list, $declaration);
        }
    }
    | decl errstmt
    { $$ = Lists.<Declaration>newList(declarations.makeErrorDecl()); }
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
            pstate.declspecs = new TypeElementsAssociation(Lists.<TypeElement>newList());
        } else {
            // FIXME: ugly workaround for $<LinkedList<TypeElement>>0
            // bison does not handle <<>>
            pstate.declspecs = new TypeElementsAssociation(list);
        }
        pstate.attributes = Lists.<Attribute>newList();
        // TODO: check why we are not making $<telements$ an empty list

        $$ = pstate.declspecs;
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
        $$ = declarations.makeDataDecl(environment, startLocation, $4.getEndLocation(), $2, $3);
        popDeclspecStack();
    }
    | declspecs_nots setspecs notype_initdecls SEMICOLON
    {
        final Location startLocation = AstUtils.getStartLocation($4.getLocation(), $1, $3);
        $$ = declarations.makeDataDecl(environment, startLocation, $4.getEndLocation(), $2, $3);
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
        $$ = declarations.makeDataDecl(environment, startLocation, $3.getEndLocation(), $2, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | extension decl
    {
        $$ = declarations.makeExtensionDecl($1.getLocation(), $2.getEndLocation(), $2);
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
        final TypeofType typeof = new TypeofType($1.getLocation(), $3);
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
        final AsmStmt asmStmt = new AsmStmt($1.getLocation(), $3, Lists.<AsmOperand>newList(),
                Lists.<AsmOperand>newList(), Lists.<StringAst>newList(), Lists.<TypeElement>newList());
                asmStmt.setEndLocation($4.getEndLocation());
        $$ = asmStmt;
    }
    ;

/*
 * FIXME: initdcl is not only variable declaration but can be also a
 * function prototype declaration, e.g. int max(int, int).
 */
initdcl:
      declarator maybeasm[asm] maybe_attribute[attrs] EQ
    {
        final VariableDecl decl = declarations.startDecl(environment, $declarator, Optional.fromNullable($asm),
                pstate.declspecs, prefixAttr($attrs), true, pstate.insideUsesProvides);
        $<VariableDecl>$ = decl;
    }
      init
    {
          final VariableDecl decl = $<VariableDecl>5;
          $$ = declarations.finishDecl(decl, environment, Optional.of($init));
    }
/* Note how the declaration of the variable is in effect while its init is parsed! */
    | declarator maybeasm[asm] maybe_attribute[attrs]
    {
        final VariableDecl decl = declarations.startDecl(environment, $declarator, Optional.fromNullable($asm),
                pstate.declspecs, prefixAttr($attrs), false, pstate.insideUsesProvides);
        $$ = declarations.finishDecl(decl, environment, Optional.<Expression>absent());
    }
    ;

notype_initdcl:
      notype_declarator[declarator] maybeasm[asm] maybe_attribute[attrs] EQ
    {
        final VariableDecl decl = declarations.startDecl(environment, $declarator, Optional.fromNullable($asm),
                pstate.declspecs, prefixAttr($attrs), true, pstate.insideUsesProvides);
        $<VariableDecl>$ = decl;
    }
      init
    {
          final VariableDecl decl = $<VariableDecl>5;
          $$ = declarations.finishDecl(decl, environment, Optional.of($init));
    }
/* Note how the declaration of the variable is in effect while its init is parsed! */
    | notype_declarator[declarator] maybeasm[asm] maybe_attribute[attrs]
    {
        VariableDecl decl = declarations.startDecl(environment, $declarator, Optional.fromNullable($asm),
                pstate.declspecs, prefixAttr($attrs), false, pstate.insideUsesProvides);
        $$ = declarations.finishDecl(decl, environment, Optional.<Expression>absent());
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
        final TargetAttribute attribute = new TargetAttribute($1.getLocation(), w, Optional.<LinkedList<Expression>>absent());
        attribute.setEndLocation($1.getEndLocation());
        $$ = attribute;
    }
    | TARGET_ATTRIBUTE1 restricted_expr
    {
        final Word w = new Word($1.getLocation(), $1.getValue());
        w.setEndLocation($1.getEndLocation());
        final TargetAttribute attribute = new TargetAttribute($1.getLocation(), w, Optional.of(Lists.newList($2)));
        attribute.setEndLocation($2.getEndLocation());
        $$ = attribute;
    }
    | AT restricted_expr
    {
        final Word w = new Word(Location.getDummyLocation(), "iar_at");
        w.setEndLocation(Location.getDummyLocation());
        final TargetAttribute attribute = new TargetAttribute($1.getLocation(), w, Optional.of(Lists.newList($2)));
        attribute.setEndLocation($2.getEndLocation());
        $$ = attribute;
    }
    ;

restricted_expr:
      INTEGER_LITERAL
    {
        $$ = Expressions.makeIntegerCst($1.getValue(), $1.getLocation(), $1.getEndLocation());
    }
    | FLOATING_POINT_LITERAL
    {
        final FloatingCst cst = new FloatingCst($1.getLocation(), $1.getValue());
        cst.setEndLocation($1.getEndLocation());
        $$ = cst;
    }
    | CHARACTER_LITERAL
    {
        $$ = Expressions.makeCharacterCst($1.getValue(), $1.getLocation(), $1.getEndLocation());
    }
    | INVALID_NUMBER_LITERAL
    {
        $$ = Expressions.makeErrorExpr();
    }
    | LPAREN expr RPAREN
    {
        final int parenthesesCount = Optional.fromNullable($expr.getParenthesesCount()).or(0) + 1;
        $expr.setParenthesesCount(parenthesesCount);
        $$ = $expr;
    }
    | string
    { $$ = $1; }
    ;

attribute_list:
      attrib
    {
        /* NOTE: attrib can be null! */
        $$ = Lists.<Attribute>newListEmptyOnNull($1);
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
        final GccAttribute attribute = new GccAttribute($1.getLocation(), $1, Optional.<LinkedList<Expression>>absent());
        attribute.setEndLocation($1.getEndLocation());
        $$ = attribute;
    }
    | any_word LPAREN IDENTIFIER RPAREN
    {
        final Identifier id = new Identifier($3.getLocation(), $3.getValue());
        id.setEndLocation($3.getEndLocation());
        id.setUniqueName(Optional.<String>absent());
        final GccAttribute attribute = new GccAttribute($1.getLocation(), $1, Optional.of(Lists.<Expression>newList(id)));
        attribute.setEndLocation($4.getEndLocation());
        $$ = attribute;
    }
    | any_word LPAREN IDENTIFIER COMMA nonnull_exprlist RPAREN
    {
        final Identifier id = new Identifier($3.getLocation(), $3.getValue());
        id.setEndLocation($3.getEndLocation());
        final LinkedList<Expression> arguments = Lists.<Expression>chain($5, id);
        ExpressionUtils.setUniqueNameDeep(arguments, Optional.<String>absent());
        final GccAttribute attribute = new GccAttribute($1.getLocation(), $1, Optional.of(arguments));
        attribute.setEndLocation($6.getEndLocation());
        $$ = attribute;
    }
    | any_word LPAREN exprlist RPAREN
    {
        ExpressionUtils.setUniqueNameDeep($3, Optional.<String>absent());
        final GccAttribute attribute = new GccAttribute($1.getLocation(), $1, Optional.of($3));
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
        $$ = initializers.makeInitList(environment, $1.getLocation(), $3.getEndLocation(), $2);
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
        $$ = initializers.makeInitSpecific(environment, startLocation, $3.getEndLocation(), $1, $3);
    }
    | designator initval
    {
        $$ = initializers.makeInitSpecific(environment, $1.getLocation(), $2.getEndLocation(), $1, $2);
    }
    | identifier COLON initval
    {
        final Designator designator = initializers.setInitLabel($1.getLocation(), $1.getEndLocation(), $1.getValue());
        $$ = initializers.makeInitSpecific(environment, $1.getLocation(), $3.getEndLocation(), designator, $3);
    }
    | initval
    { $$ = $1; }
    ;

initval:
      LBRACE initlist_maybe_comma RBRACE
    {
        $$ = initializers.makeInitList(environment, $1.getLocation(), $3.getEndLocation(), $2);
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
        $$ = initializers.setInitLabel($2.getLocation(), $2.getEndLocation(), $2.getValue());
    }
    /* These are for labeled elements.  The syntax for an array element
       initializer conflicts with the syntax for an Objective-C message,
       so don't include these productions in the Objective-C grammar.  */
    | LBRACK expr_no_commas ELLIPSIS expr_no_commas RBRACK
    {
        $$ = initializers.setInitIndex(environment, $1.getLocation(), $5.getEndLocation(), $2, Optional.<Expression>of($4));
    }
    | LBRACK expr_no_commas RBRACK
    {
        $$ = initializers.setInitIndex(environment, $1.getLocation(), $3.getEndLocation(), $2, Optional.<Expression>absent());
    }
    ;

nested_function:
      declarator maybeasm[asm] maybe_attribute[attrs]
    {
        /* NOTICE: maybe asm can be null! */
        /* NOTICE: maybeasm is only here to avoid a s/r conflict */
        // TODO refuse_asm

        final Optional<FunctionDecl> decl = declarations.startFunction(environment, $declarator.getLocation(),
                pstate.declspecs.getTypeElements(), $declarator, $attrs, true);
        if (!decl.isPresent()) {
            error($declarator.getLocation(), Optional.of($declarator.getEndLocation()),
                    "syntax error, expected function definition");
            $<FunctionDecl>$ = null;
        } else {
            /* Push parameters environment. There is no need to change its parent. */
            final Environment paramEnv = DeclaratorUtils.getFunctionDeclarator($declarator).getEnvironment();
            paramEnv.setScopeType(ScopeType.FUNCTION_PARAMETER);
            /* See coresponding code in fndef2. */
            paramEnv.setEndLocation(null);
            pushLevel(paramEnv);
            $<FunctionDecl>$ = decl.get();
        }
    }
      old_style_parm_decls[parms]
    {
        if ($<FunctionDecl>4 == null) {
            $<FunctionDecl>$ = null;
        } else {
            $<FunctionDecl>$ = declarations.setOldParams($<FunctionDecl>4, $parms);
        }
        pstate.newFunctionScope = true;
    }
    /*
     * This used to use compstmt_or_error. That caused a bug with input
     * `f(g) int g {}', where the use of YYERROR1 above caused an error which
     * then was handled by compstmt_or_error. There followed a repeated
     * execution of that same rule, which called YYERROR1 again, and so on.
     */
      compstmt[body]
    {
        pstate.newFunctionScope = false;
        if ($<FunctionDecl>6 == null) {
            $$ = null;
        } else {
            $$ = declarations.finishFunction($<FunctionDecl>6, $body, true);
        }
        /*
         * Parameter environment, has the same start and end location
         * as body environment.
         */
        environment.setStartLocation($body.getLocation());
        environment.setEndLocation($body.getEndLocation());
        pstate.labelsNamesStack.pop();
        popLevel();
    }
    ;

notype_nested_function:
      notype_declarator[declarator] maybeasm[asm] maybe_attribute[attrs]
    {
        /* NOTICE: maybe asm can be null! */
        /* NOTICE: maybeasm is only here to avoid a s/r conflict */
        // TODO: refuse_asm

        final Optional<FunctionDecl> decl = declarations.startFunction(environment, $declarator.getLocation(),
                pstate.declspecs.getTypeElements(), $declarator, $attrs, true);
        if (!decl.isPresent()) {
            error($declarator.getLocation(), Optional.of($declarator.getEndLocation()),
                    "syntax error, expected function definition");
            $<FunctionDecl>$ = null;
        } else {
            /* Push parameters environment. There is no need to change its parent. */
            final Environment paramEnv = DeclaratorUtils.getFunctionDeclarator($declarator).getEnvironment();
            paramEnv.setScopeType(ScopeType.FUNCTION_PARAMETER);
            /* See coresponding code in fndef2. */
            paramEnv.setEndLocation(null);
            pushLevel(paramEnv);
            $<FunctionDecl>$ = decl.get();
        }
    }
      old_style_parm_decls[parms]
    {
        if ($<FunctionDecl>4 == null) {
            $<FunctionDecl>$ = null;
        } else {
            $<FunctionDecl>$ = declarations.setOldParams($<FunctionDecl>4, $parms);
        }
        pstate.newFunctionScope = true;
    }
    /*
     * This used to use compstmt_or_error. That caused a bug with input
     * `f(g) int g {}', where the use of YYERROR1 above caused an error which
     * then was handled by compstmt_or_error. There followed a repeated
     * execution of that same rule, which called YYERROR1 again, and so on.
     */
      compstmt[body]
    {
        pstate.newFunctionScope = false;
        if ($<FunctionDecl>6 == null) {
            $$ = null;
        } else {
            $$ = declarations.finishFunction($<FunctionDecl>6, $body, true);
        }
        /*
         * Parameter environment, has the same start and end location
         * as body environment.
         */
        environment.setStartLocation($body.getLocation());
        environment.setEndLocation($body.getEndLocation());
        pstate.labelsNamesStack.pop();
        popLevel();
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
        final Declarator declarator = declarations.finishArrayOrFnDeclarator(Optional.<Declarator>of($1), $2);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            final FunctionDeclarator funDeclarator = (FunctionDeclarator) declarator;
            funDeclarator.setEnvironment(environment);
            environment.setEndLocation($2.getEndLocation());
            popLevel();
        }
        $$ = declarator;
    }
    | STAR maybe_type_quals_attrs after_type_declarator
    {
        $$ = declarations.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    | LPAREN maybe_attribute after_type_declarator RPAREN
    {
        final QualifiedDeclarator decl = new QualifiedDeclarator($1.getLocation(), Optional.<Declarator>of($3),
                Lists.<Attribute, TypeElement>convert($2));
        decl.setEndLocation($4.getEndLocation());
        $$ = decl;
    }
    | TYPEDEF_NAME
    {
        final IdentifierDeclarator decl = new IdentifierDeclarator($1.getLocation(), $1.getValue());
        decl.setIsNestedInNescEntity(environment.isEnclosedInNescEntity());
        decl.setEndLocation($1.getEndLocation());
        $$ = decl;
    }
    | TYPEDEF_NAME DOT identifier
    {
        $$ = nescComponents.makeInterfaceRefDeclarator(environment, $1.getLocation(), $1.getValue(),
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
        final Declarator declarator = declarations.finishArrayOrFnDeclarator(Optional.<Declarator>of($1), $2);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            final FunctionDeclarator funDeclarator = (FunctionDeclarator) declarator;
            funDeclarator.setEnvironment(environment);
            environment.setEndLocation($2.getEndLocation());
            popLevel();
        }
        $$ = declarator;
    }
    | STAR maybe_type_quals_attrs parm_declarator
    {
        $$ = declarations.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    | TYPEDEF_NAME
    {
        final IdentifierDeclarator decl = new IdentifierDeclarator($1.getLocation(), $1.getValue());
        decl.setIsNestedInNescEntity(environment.isEnclosedInNescEntity());
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
        final Declarator declarator = declarations.finishArrayOrFnDeclarator(Optional.<Declarator>of($1), $2);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            final FunctionDeclarator funDeclarator = (FunctionDeclarator) declarator;
            funDeclarator.setEnvironment(environment);
            environment.setEndLocation($2.getEndLocation());
            popLevel();
        }
        $$ = declarator;
    }
    | STAR maybe_type_quals_attrs notype_declarator
    {
        $$ = declarations.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    | LPAREN maybe_attribute notype_declarator RPAREN
    {
        final QualifiedDeclarator decl = new QualifiedDeclarator($1.getLocation(), Optional.of($3),
                Lists.<Attribute, TypeElement>convert($2));
        decl.setEndLocation($4.getEndLocation());
        $$ = decl;
    }
    | IDENTIFIER
    {
        final IdentifierDeclarator decl = new IdentifierDeclarator($1.getLocation(), $1.getValue());
        decl.setIsNestedInNescEntity(environment.isEnclosedInNescEntity());
        decl.setEndLocation($1.getEndLocation());
        $$ = decl;
    }
    | IDENTIFIER DOT identifier
    {
        $$ = nescComponents.makeInterfaceRefDeclarator(environment, $1.getLocation(), $1.getValue(),
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
        $$ = declarations.makeXrefTag(environment, $1.getLocation(), $2.getEndLocation(), $1.getKind(), $2);
    }
    | ENUM tag nesc_attributes
    {
        // TODO: make warning "attributes ignored"
        $$ = declarations.makeXrefTag(environment, $1.getLocation(), $2.getEndLocation(), StructKind.ENUM, $2);
    }
    ;

/*
 * TODO: not sure what happens with nesc_attributes when struct creation
 * is not split into two phases (startStruct(), finishStruct()).
 */

structdef:
      structkind tag nesc_attributes LBRACE
    {
        $<TagRef>$ = declarations.startNamedStruct(environment, $structkind.getLocation(), $tag.getEndLocation(),
                $structkind.getKind(), $tag);
    }
      [tagRef] component_decl_list RBRACE maybe_attribute
    {
        final TagRef tagRef = $<TagRef>tagRef;
        final Location endLocation = AstUtils.getEndLocation($RBRACE.getEndLocation(), $maybe_attribute);
        declarations.finishNamedTagDefinition(tagRef, environment, endLocation, $component_decl_list,
                Lists.<Attribute>chain($nesc_attributes, $maybe_attribute));
        $$ = tagRef;
    }
    | STRUCT AT tag nesc_attributes LBRACE
    {
        $<TagRef>$ = declarations.startNamedStruct(environment, $STRUCT.getLocation(), $tag.getEndLocation(),
                StructKind.ATTRIBUTE, $tag);
    }
      [tagRef] component_decl_list RBRACE maybe_attribute
    {
        final TagRef tagRef = $<TagRef>tagRef;
        final Location endLocation = AstUtils.getEndLocation($RBRACE.getEndLocation(), $maybe_attribute);
        declarations.finishNamedTagDefinition(tagRef, environment, endLocation, $component_decl_list,
                Lists.<Attribute>chain($nesc_attributes, $maybe_attribute));
        $$ = tagRef;
    }
    | structkind LBRACE component_decl_list RBRACE maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($4.getEndLocation(), $5);
        $$ = declarations.makeStruct(environment, $1.getLocation(), endLocation, $1.getKind(), Optional.<Word>absent(), $3, $5);
    }
    | ENUM tag nesc_attributes LBRACE
    {
        $<TagRef>$ = declarations.startNamedEnum(environment, $ENUM.getLocation(), $tag.getEndLocation(), $tag);
    }
      [tagRef] enumlist maybecomma_warn RBRACE maybe_attribute
    {
        final TagRef tagRef = $<TagRef>tagRef;
        final Location endLocation = AstUtils.getEndLocation($RBRACE.getEndLocation(), $maybe_attribute);
        declarations.finishNamedTagDefinition(tagRef, environment, endLocation, $enumlist,
                Lists.<Attribute>chain($nesc_attributes, $maybe_attribute));
        $$ = tagRef;
    }
    | ENUM LBRACE enumlist maybecomma_warn RBRACE maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($5.getEndLocation(), $6);
        $$ = declarations.makeEnum(environment, $1.getLocation(), endLocation, Optional.<Word>absent(), $3, $6);
    }
    ;

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
        $$ = declarations.makeDataDecl(environment, startLocation, endLocation, $2, $3);
        popDeclspecStack();
    }
    | declspecs_nosc_ts setspecs
    {
        final Location startLocation = AstUtils.getStartLocation($1).get();
        final Location endLocation = AstUtils.getEndLocation($1).get();
        $$ = declarations.makeDataDecl(environment, startLocation, endLocation, $2, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | declspecs_nosc_nots setspecs components_notype
    {
        final Location startLocation = AstUtils.getStartLocation($1, $3).get();
        final Location endLocation = AstUtils.getEndLocation($3).get();
        $$ = declarations.makeDataDecl(environment, startLocation, endLocation, $2, $3);
        popDeclspecStack();
    }
    | declspecs_nosc_nots setspecs
    {
        final Location startLocation = AstUtils.getStartLocation($1).get();
        final Location endLocation = AstUtils.getEndLocation($1).get();
        $$ = declarations.makeDataDecl(environment, startLocation, endLocation, $2, Lists.<Declaration>newList());
        popDeclspecStack();
    }
    | error
    {
        $$ = declarations.makeErrorDecl();
    }
    | extension component_decl
    {
        $$ = declarations.makeExtensionDecl($1.getLocation(), $2.getEndLocation(), $2);
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
        $$ = declarations.makeField(environment, $1.getLocation(), endLocation,
                Optional.of($1), Optional.<Expression>absent(), pstate.declspecs, prefixAttr($2));
    }
    | declarator COLON expr_no_commas maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($3.getEndLocation(), $4);
        $$ = declarations.makeField(environment, $1.getLocation(), endLocation,
                Optional.of($1), Optional.of($3), pstate.declspecs, prefixAttr($4));
    }
    | COLON expr_no_commas maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($2.getEndLocation(), $3);
        $$ = declarations.makeField(environment, $1.getLocation(), endLocation,
                Optional.<Declarator>absent(), Optional.of($2), pstate.declspecs, prefixAttr($3));
    }
    ;

component_notype_declarator:
      notype_declarator maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        $$ = declarations.makeField(environment, $1.getLocation(), endLocation,
                Optional.of($1), Optional.<Expression>absent(), pstate.declspecs, prefixAttr($2));
    }
    | notype_declarator COLON expr_no_commas maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($3.getEndLocation(), $4);
        $$ = declarations.makeField(environment, $1.getLocation(), endLocation,
                Optional.of($1), Optional.of($3), pstate.declspecs, prefixAttr($4));
    }
    | COLON expr_no_commas maybe_attribute
    {
        final Location endLocation = AstUtils.getEndLocation($2.getEndLocation(), $3);
        $$ = declarations.makeField(environment, $1.getLocation(), endLocation,
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
        $$ = declarations.makeEnumerator(environment, $1.getLocation(), $1.getEndLocation(), $1.getValue(),
                Optional.<Expression>absent());
    }
    | identifier EQ expr_no_commas
    {
        $$ = declarations.makeEnumerator(environment, $1.getLocation(), $3.getEndLocation(), $1.getValue(),
                Optional.of($3));
    }
    ;

typename:
      declspecs_nosc absdcl
    {
        /* NOTICE: absdcl may be null! */
        $$ = declarations.makeType(environment, $1, Optional.<Declarator>fromNullable($2));
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
        $$ = declarations.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    ;

absdcl1_ea:
      STAR maybe_type_quals_attrs
    {
        final Location endLocation = AstUtils.getEndLocation($1.getEndLocation(), $2);
        $$ = declarations.makePointerDeclarator($1.getLocation(), endLocation, Optional.<Declarator>absent(), $2);
    }
    | STAR maybe_type_quals_attrs absdcl1_ea
    {
        $$ = declarations.makePointerDeclarator($1.getLocation(), $3.getEndLocation(), Optional.of($3), $2);
    }
    ;

direct_absdcl1:
      LPAREN maybe_attribute absdcl1 RPAREN
    {
        final QualifiedDeclarator decl = new QualifiedDeclarator($1.getLocation(), Optional.<Declarator>of($3),
                Lists.<Attribute, TypeElement>convert($2));
        decl.setEndLocation($4.getEndLocation());
        $$ = decl;
    }
    | direct_absdcl1 array_or_absfn_declarator
    {
        final Declarator declarator = declarations.finishArrayOrFnDeclarator(Optional.<Declarator>of($1), $2);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            final FunctionDeclarator funDeclarator = (FunctionDeclarator) declarator;
            funDeclarator.setEnvironment(environment);
            environment.setEndLocation($2.getEndLocation());
            popLevel();
        }
        $$ = declarator;
    }
    | array_or_absfn_declarator
    {
        final Declarator declarator = declarations.finishArrayOrFnDeclarator(Optional.<Declarator>absent(), $1);
        if (DeclaratorUtils.isFunctionDeclarator(declarator)) {
            final FunctionDeclarator funDeclarator = (FunctionDeclarator) declarator;
            funDeclarator.setEnvironment(environment);
            environment.setEndLocation($1.getEndLocation());
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
      parameters[generic_params] LPAREN[lparen] parmlist_or_identifiers_1[params] fn_quals[quals]
    {
        /* pushLevel in parameters, popLevel when final declarator is build */
        final Location startLocation = AstUtils.getStartLocation($lparen.getLocation(), $generic_params);
        final Location endLocation = AstUtils.getEndLocation($lparen.getEndLocation(), $params);
        final Optional<LinkedList<Declaration>> gparams = $generic_params.isEmpty()
                 ? Optional.<LinkedList<Declaration>>absent() : Optional.of($generic_params);
        final FunctionDeclarator decl = new FunctionDeclarator(startLocation, Optional.<Declarator>absent(), $params,
                gparams, $quals);
        decl.setEndLocation(endLocation);
        $$ = decl;
    }
    | LPAREN[lparen] parmlist_or_identifiers[params] fn_quals[quals]
    {
        /* pushLevel parmlist_or_identifiers, popLevel when final declarator is build */
        final Location endLocation = AstUtils.getEndLocation($lparen.getEndLocation(), $params);
        final FunctionDeclarator decl = new FunctionDeclarator($lparen.getLocation(), Optional.<Declarator>absent(),
                $params, Optional.<LinkedList<Declaration>>absent(), $quals);
        decl.setEndLocation(endLocation);
        $$ = decl;
    }
    ;

absfn_declarator:
      LPAREN[lparen] parmlist[params] fn_quals[quals]
    {
        final Location endLocation = AstUtils.getEndLocation($lparen.getEndLocation(), $params);
        final FunctionDeclarator decl = new FunctionDeclarator($lparen.getLocation(), Optional.<Declarator>absent(),
                $params, Optional.<LinkedList<Declaration>>absent(), $quals);
        decl.setEndLocation(endLocation);
        $$ = decl;
    }
    ;

array_declarator:
      LBRACK expr RBRACK
    {
        final ArrayDeclarator decl = new ArrayDeclarator($1.getLocation(), Optional.<Declarator>absent(),
                Optional.of($2));
        decl.setEndLocation($3.getEndLocation());
        $$ = decl;
    }
    | LBRACK RBRACK
    {
        final ArrayDeclarator decl = new ArrayDeclarator($1.getLocation(), Optional.<Declarator>absent(),
                Optional.<Expression>absent());
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
            statements.chainWithLabels($1.getStatements(), Lists.<Statement>newList(lastLabel));
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
                statements.chainWithLabels($1.getStatements(), Lists.<Statement>newList($2.getStatement())),
                $2.getCounter());
    }
    | stmt_or_labels errstmt
    {
        final LinkedList<Statement> stmts = $1.getStatements();
        stmts.add(statements.makeErrorStmt());
        $$ = new ValueStatements(Lists.chain(stmts, statements.makeErrorStmt()), 0);
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

/*
 * Push level is used only in compstmt.
 *
 * We need to set scope type and start location as soon as possible in case
 * some syntax error occurs to avoid losing data useful in content assist
 * (such as scope type and start location).
 */
pushlevel:
      /* empty */
    {
        if (pstate.newFunctionScope) {
            pushFunctionTopLevel();
            pstate.newFunctionScope = false;
            pstate.labelsNamesStack.push(Optional.<Set<String>>absent());
        } else {
            pushLevel();
        }

        environment.setScopeType(ScopeType.COMPOUND);
        /* pushlevel is always preceded by compstmt_start (LPAREN) */
        environment.setStartLocation($<Symbol>0.getLocation());
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
        final CompoundStmt stmt = new CompoundStmt($1.getLocation(), Lists.<IdLabel>newList(),
                Lists.<Declaration>newList(), Lists.<Statement>newList());
        stmt.setEnvironment(environment);
        stmt.setEndLocation($3.getEndLocation());

        environment.setEndLocation($3.getEndLocation());
        popLevel();

        $$ = stmt;
    }
    | compstmt_start pushlevel maybe_label_decls decls xstmts RBRACE
    {
        final CompoundStmt stmt = new CompoundStmt($1.getLocation(), $3, $4, $5);
        stmt.setEnvironment(environment);
        stmt.setEndLocation($6.getEndLocation());

        environment.setEndLocation($6.getEndLocation());
        popLevel();

        $$ = stmt;
    }
    | compstmt_start pushlevel maybe_label_decls error RBRACE
    {
        environment.setEndLocation($5.getEndLocation());
        popLevel();

        $$ = statements.makeErrorStmt();
    }
    | compstmt_start pushlevel maybe_label_decls stmts RBRACE
    {
        final CompoundStmt stmt = new CompoundStmt($1.getLocation(), $3, Lists.<Declaration>newList(), $4);
        stmt.setEnvironment(environment);
        stmt.setEndLocation($5.getEndLocation());

        environment.setEndLocation($5.getEndLocation());
        popLevel();

        $$ = stmt;
    }
    ;

/*
 * Value is number of statements counted as of the closeparen.
 */
simple_if:
      if_prefix labeled_stmt
    {
        final IfStmt stmt = new IfStmt($1.getLocation(), $1.getExpression(), $2, Optional.<Statement>absent());
        stmt.setEndLocation($2.getEndLocation());
        $$ = new ValueStatement(stmt, $1.getCounter());
    }
    | if_prefix error
    {
        $$ = new ValueStatement(statements.makeErrorStmt(), $1.getCounter());
    }
    ;

if_prefix:
      IF LPAREN expr RPAREN
    {
        analyzeExpression(Optional.of($expr));
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
        final DoWhileStmt stmt = new DoWhileStmt($1.getLocation(), null, $3);   // expression will be set later
        stmt.setEndLocation($4.getEndLocation());
        $$ = stmt;
    }
    ;

labeled_stmt:
      stmt
    { $$ = $1; }
    | label labeled_stmt
    {
        final LabeledStmt stmt = new LabeledStmt($1.getLocation(), $1, Optional.of($2));
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
        final LabeledStmt stmt = new LabeledStmt($1.getLocation(), $1, Optional.<Statement>absent());
        stmt.setEndLocation($1.getEndLocation());
        $$ = new ValueStatement(stmt, 1);
    }
    ;

atomic_stmt:
      ATOMIC[atomic]
    {
        $<Boolean>$ = !pstate.labelsNamesStack.isEmpty() && pstate.labelsNamesStack.peek().isPresent();
        if (!pstate.labelsNamesStack.isEmpty() && !pstate.labelsNamesStack.peek().isPresent()) {
            pstate.labelsNamesStack.pop();
            pstate.labelsNamesStack.push(Optional.<Set<String>>of(new HashSet<String>()));
        }
    }
      [isNestedAtomic] stmt_or_error[stmt]
    {
        final Optional<Set<String>> declaredLabelsNames;

        if (pstate.labelsNamesStack.isEmpty() || $<Boolean>isNestedAtomic) {
            declaredLabelsNames = Optional.absent();
        } else {
            declaredLabelsNames = pstate.labelsNamesStack.pop();
            pstate.labelsNamesStack.push(Optional.<Set<String>>absent());
        }

        final AtomicStmt atomicStmt = new AtomicStmt($atomic.getLocation(), $stmt, declaredLabelsNames);
        atomicStmt.setEndLocation($1.getEndLocation());
        $$ = atomicStmt;
    }
    ;

stmt_or_error:
      stmt
    { $$ = $1; }
    | error
    { $$ = statements.makeErrorStmt(); }
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
        analyzeExpression(Optional.of($expr));
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
        if (stmt instanceof IfStmt) {
            final IfStmt ifStmt = (IfStmt) stmt;
            ifStmt.setFalseStatement(Optional.<Statement>of($4));
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
        $$ = statements.makeErrorStmt();
    }
    | WHILE
    {
        pstate.stmtCount++;
    }
      LPAREN expr RPAREN labeled_stmt
    {
        analyzeExpression(Optional.of($expr));
        final WhileStmt stmt = new WhileStmt($1.getLocation(), $4, $6);
        stmt.setEndLocation($6.getEndLocation());
        $$ = stmt;
    }
    | do_stmt_start LPAREN expr RPAREN SEMICOLON
    {
        analyzeExpression(Optional.of($expr));
        $1.setCondition($3);
        $$ = $1;
    }
    | do_stmt_start error
    {
        $$ = statements.makeErrorStmt();
    }
    | FOR LPAREN xexpr[einit] SEMICOLON
    {
        analyzeExpression(Optional.fromNullable($einit));
        pstate.stmtCount++;
    }
      xexpr[econdition] SEMICOLON xexpr[eincrement] RPAREN labeled_stmt
    {
        /* NOTICE: xexpr may be null. */
        analyzeExpression(Optional.fromNullable($econdition));
        analyzeExpression(Optional.fromNullable($eincrement));
        final ForStmt stmt = new ForStmt($1.getLocation(), Optional.fromNullable($einit),
                Optional.fromNullable($econdition), Optional.fromNullable($eincrement), $labeled_stmt);
        stmt.setEndLocation($10.getEndLocation());
        $$ = stmt;
    }
    | SWITCH LPAREN expr RPAREN
    {
        analyzeExpression(Optional.of($expr));
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
        $$ = statements.makeVoidReturn($1.getLocation(), $2.getEndLocation());
    }
    | RETURN expr SEMICOLON
    {
        pstate.stmtCount++;
        $$ = statements.makeReturn(environment, $1.getLocation(), $3.getEndLocation(), $2);
    }
    | ASM_KEYWORD[asm] maybe_type_qual[quals] LPAREN expr RPAREN SEMICOLON[semi]
    {
        /* NOTICE: maybe_type_qual may be null */
        analyzeExpression(Optional.of($expr));
        pstate.stmtCount++;
        final AsmStmt asmStmt = new AsmStmt($asm.getLocation(), $expr, Lists.<AsmOperand>newList(),
                Lists.<AsmOperand>newList(), Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($quals));
        asmStmt.setEndLocation($semi.getEndLocation());
        $$ = asmStmt;
    }
    /* This is the case with just output operands.  */
    | ASM_KEYWORD[asm] maybe_type_qual[quals] LPAREN expr COLON asm_operands[operands] RPAREN SEMICOLON[semi]
    {
        /* NOTE: maybe_type_qual may be null */
        analyzeExpression(Optional.of($expr));
        pstate.stmtCount++;
        final AsmStmt asmStmt = new AsmStmt($asm.getLocation(), $expr, $operands, Lists.<AsmOperand>newList(),
                Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($quals));
        asmStmt.setEndLocation($semi.getEndLocation());
        $$ = asmStmt;
    }
    /* This is the case with input operands as well.  */
    | ASM_KEYWORD[asm] maybe_type_qual[quals] LPAREN expr COLON asm_operands[operands1] COLON asm_operands[operands2] RPAREN SEMICOLON[semi]
    {
        /* NOTE: maybe_type_qual may be null */
        analyzeExpression(Optional.of($expr));
        pstate.stmtCount++;
        final AsmStmt asmStmt = new AsmStmt($asm.getLocation(), $expr, $operands1, $operands2, Lists.<StringAst>newList(),
                Lists.<TypeElement>newListEmptyOnNull($quals));
        asmStmt.setEndLocation($semi.getEndLocation());
        $$ = asmStmt;
    }
    /* This is the case with clobbered registers as well.  */
    | ASM_KEYWORD[asm] maybe_type_qual[quals] LPAREN expr COLON asm_operands[operands1] COLON asm_operands[operands2] COLON asm_clobbers[clobbers] RPAREN SEMICOLON[semi]
    {
        /* NOTE: maybe_type_qual may be null */
        analyzeExpression(Optional.of($expr));
        pstate.stmtCount++;
        final AsmStmt asmStmt = new AsmStmt($asm.getLocation(), $expr, $operands1, $operands2, $clobbers,
                Lists.<TypeElement>newListEmptyOnNull($quals));
        asmStmt.setEndLocation($semi.getEndLocation());
        $$ = asmStmt;
    }
    | GOTO id_label SEMICOLON
    {
        $2.setIsColonTerminated(false);
        pstate.stmtCount++;
        final GotoStmt stmt = new GotoStmt($1.getLocation(), $2);
        stmt.setEndLocation($3.getEndLocation());
        $$ = stmt;
    }
    | GOTO STAR expr SEMICOLON
    {
        analyzeExpression(Optional.of($expr));
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
        analyzeExpression(Optional.of($expr_no_commas));
        final CaseLabel label = new CaseLabel($1.getLocation(), $2, Optional.<Expression>absent());
        label.setEndLocation($3.getEndLocation());
        $$ = label;
    }
    | CASE expr_no_commas[leftexpr] ELLIPSIS expr_no_commas[rightexpr] COLON
    {
        analyzeExpression(Optional.of($leftexpr));
        analyzeExpression(Optional.of($rightexpr));
        final CaseLabel label = new CaseLabel($1.getLocation(), $leftexpr, Optional.<Expression>of($rightexpr));
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
        addAtomicStmtLabelName($1.getId());
        final boolean isInsideAtomic = !pstate.labelsNamesStack.isEmpty()
                && pstate.labelsNamesStack.peek().isPresent();
        labels.defineLabel(environment, $1, isInsideAtomic);
        $1.setIsColonTerminated(true);
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
      string_chain[str] LPAREN expr RPAREN[rparen]
    {
        analyzeExpression(Optional.of($expr));
        final AsmOperand operand = new AsmOperand($str.getLocation(), Optional.<Word>absent(), $str, $expr);
        operand.setEndLocation($rparen.getEndLocation());
        $$ = operand;
    }
    | LBRACK[lbrack] idword RBRACK string_chain[str] LPAREN expr RPAREN[rparen]
    {
        analyzeExpression(Optional.of($expr));
        final AsmOperand operand = new AsmOperand($lbrack.getLocation(), Optional.of($idword), $str, $expr);
        operand.setEndLocation($rparen.getEndLocation());
        $$ = operand;
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
        /*
         * Scope of function parameters. See also comment for
         * non-terminal parameters.
         */
        pushLevel();
        environment.setScopeType(ScopeType.FUNCTION_PARAMETER);
        /* parmlist is always preceded by LPAREN */
        environment.setStartLocation($<Symbol>0.getLocation());
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
    { $$ = Lists.<Declaration>newList(declarations.makeErrorDecl()); }
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
        $$ = Lists.<Declaration>newList(declarations.makeErrorDecl());
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
        $$ = declarations.declareParameter(environment, Optional.of($3), $1, $4);
    }
    | declspecs_ts xreferror notype_declarator maybe_attribute
    {
        $$ = declarations.declareParameter(environment, Optional.of($3), $1, $4);
    }
    | declspecs_ts xreferror absdcl
    {
        /* NOTE: absdcl may be null */
        $$ = declarations.declareParameter(environment, Optional.fromNullable($3), $1, Lists.<Attribute>newList());
    }
    | declspecs_ts xreferror absdcl1_noea attributes
    {
        $$ = declarations.declareParameter(environment, Optional.of($3), $1, $4);
    }
    | declspecs_nots xreferror notype_declarator maybe_attribute
    {
        $$ = declarations.declareParameter(environment, Optional.of($3), $1, $4);
    }
    | declspecs_nots xreferror absdcl
    {
        /* NOTICE: absdcl may be null. */
        $$ = declarations.declareParameter(environment, Optional.fromNullable($3), $1, Lists.<Attribute>newList());
    }
    | declspecs_nots xreferror absdcl1_noea attributes
    {
        $$ = declarations.declareParameter(environment, Optional.of($3), $1, $4);
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
        /*
         * Scope of function parameters. See also comment for
         * non-terminal parameters.
         */
        pushLevel();
        environment.setScopeType(ScopeType.FUNCTION_PARAMETER);
        /* parmlist is always preceded by LPAREN */
        environment.setStartLocation($<Symbol>0.getLocation());
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
        $$ = declarations.declareOldParameter(environment, $1.getLocation(), $1.getEndLocation(), $1.getValue());
    }
    ;

/*
 * A nonempty list of identifiers, including typenames.
 */
identifiers_or_typenames:
      id_label
    {
        addAtomicStmtLabelName($1.getId());
        final boolean isInsideAtomic = !pstate.labelsNamesStack.isEmpty()
                && pstate.labelsNamesStack.peek().isPresent();
        labels.declareLocalLabel(environment, $1, isInsideAtomic);
        $1.setIsColonTerminated(false);
        $$ = Lists.<IdLabel>newList($1);
    }
    | identifiers_or_typenames COMMA id_label
    {
        addAtomicStmtLabelName($3.getId());
        final boolean isInsideAtomic = !pstate.labelsNamesStack.isEmpty()
                && pstate.labelsNamesStack.peek().isPresent();
        labels.declareLocalLabel(environment, $3, isInsideAtomic);
        $3.setIsColonTerminated(false);
        $$ = Lists.<IdLabel>chain($1, $3);
    }
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
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.TYPEDEF);
    }
    | EXTERN
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.EXTERN);
    }
    | STATIC
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.STATIC);
    }
    | AUTO
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.AUTO);
    }
    | REGISTER
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.REGISTER);
    }
    | COMMAND
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.COMMAND);
    }
    | EVENT
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.EVENT);
    }
    | ASYNC
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.ASYNC);
    }
    | TASK
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.TASK);
    }
    | NORACE
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.NORACE);
    }
    | DEFAULT
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.DEFAULT);
    }
    | INLINE
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.INLINE);
    }
    ;

type_qual:
      CONST
    {
        $$ = declarations.makeQualifier($1.getLocation(), $1.getEndLocation(), RID.CONST);
    }
    | RESTRICT
    {
        $$ = declarations.makeQualifier($1.getLocation(), $1.getEndLocation(), RID.RESTRICT);
    }
    | VOLATILE
    {
        $$ = declarations.makeQualifier($1.getLocation(), $1.getEndLocation(), RID.VOLATILE);
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
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.VOID);
    }
    | CHAR
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.CHAR);
    }
    | SHORT
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.SHORT);
    }
    | INT
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.INT);
    }
    | LONG
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.LONG);
    }
    | FLOAT
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.FLOAT);
    }
    | DOUBLE
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.DOUBLE);
    }
    | SIGNED
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.SIGNED);
    }
    | UNSIGNED
    {
        $$ = declarations.makeRid($1.getLocation(), $1.getEndLocation(), RID.UNSIGNED);
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
    private FrontendContext context;
    private Environment environment;
    private NescEntityEnvironment nescEnvironment;
    private ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;
    private ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder;
    private final Map<String, String> globalNames = new HashMap<>();
    private final Map<String, String> combiningFunctions = new HashMap<>();
    /**
     * Indicates whether parsing was successful.
     */
    private boolean errors;
    /**
     * Keeps data essential during parsing phase.
     */
    private ParserState pstate;
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

    private ErrorHelper errorHelper;

    private Declarations declarations;
    private Initializers initializers;
    private Statements statements;
    private Labels labels;
    private NescDeclarations nescDeclarations;
    private NescComponents nescComponents;
    
    private final SemanticListener semanticListener = new SemanticListener() {
        @Override
        public void globalName(String uniqueName, String name) {
            if (!globalNames.containsKey(uniqueName)) {
                globalNames.put(uniqueName, name);
            }
        }

        @Override
        public String nameManglingRequired(String unmangledName) {
            return context.getNameMangler().mangle(unmangledName);
        }

        @Override
        public void combiningFunction(String typedefUniqueName, String functionName) {
            combiningFunctions.put(typedefUniqueName, functionName);
        }
    };

    /**
     * Creates parser.
     *
     * @param filePath              currently being parsed file path
     * @param lex                   lexer
     * @param context               context
     * @param environment           global environment
     * @param nescEnvironment       nesc environment
     * @param fileType              fileType file type
     * @param tokensMultimapBuilder tokens multimap builder
     * @param issuesMultimapBuilder issues multimap builder
     */
    public Parser(String filePath,
                  pl.edu.mimuw.nesc.lexer.Lexer lex,
                  FrontendContext context,
                  Environment environment,
                  NescEntityEnvironment nescEnvironment,
                  FileType fileType,
                  ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                  ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder) {
        Preconditions.checkNotNull(filePath, "file path cannot be null");
        Preconditions.checkNotNull(lex, "lexer cannot be null");
        Preconditions.checkNotNull(context, "context cannot be null");
        Preconditions.checkNotNull(environment, "environment cannot be null");
        Preconditions.checkNotNull(nescEnvironment, "nesc environment cannot be null");
        Preconditions.checkNotNull(fileType, "file type cannot be null");
        Preconditions.checkNotNull(tokensMultimapBuilder, "tokens multimap builder cannot be null");
        Preconditions.checkNotNull(issuesMultimapBuilder, "issues multimap builder cannot be null");

        this.filePath = filePath;
        this.fileType = fileType;
        this.currentEntityName = Files.getNameWithoutExtension(filePath);
        this.context = context;
        this.environment = environment;
        this.nescEnvironment = nescEnvironment;
        this.tokensMultimapBuilder = tokensMultimapBuilder;
        this.issuesMultimapBuilder = issuesMultimapBuilder;
        this.lex = lex;
        this.lexer = new LexerWrapper(this.lex, this.tokensMultimapBuilder, this.issuesMultimapBuilder);
        this.yylexer = lexer;

        this.errors = false;
        this.pstate = new ParserState();

        this.errorHelper = new ErrorHelper(this.issuesMultimapBuilder);
        final AttributeAnalyzer attributeAnalyzer = new AttributeAnalyzer(semanticListener, errorHelper);

        this.declarations = new Declarations(this.nescEnvironment, this.issuesMultimapBuilder,
                this.tokensMultimapBuilder, semanticListener, attributeAnalyzer);
        this.initializers = new Initializers(this.nescEnvironment, this.issuesMultimapBuilder,
                this.tokensMultimapBuilder, semanticListener, attributeAnalyzer);
        this.statements = new Statements(this.nescEnvironment, this.issuesMultimapBuilder,
                this.tokensMultimapBuilder, semanticListener, attributeAnalyzer);
        this.labels = new Labels(this.nescEnvironment, this.issuesMultimapBuilder,
                this.tokensMultimapBuilder, semanticListener, attributeAnalyzer);
        this.nescDeclarations = new NescDeclarations(this.nescEnvironment, this.issuesMultimapBuilder,
                this.tokensMultimapBuilder, semanticListener, attributeAnalyzer);
        this.nescComponents = new NescComponents(this.nescEnvironment, this.issuesMultimapBuilder,
                this.tokensMultimapBuilder, semanticListener, attributeAnalyzer);

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
     * Get the unique names of objects that will appear in the global scope
     * mapped to their unmangled names from the code.
     *
     * @return map with unique names of all global C entities (values are thier
     *         normal names)
     */
    public Map<String, String> getGlobalNames() {
        return this.globalNames;
    }

    /**
     * Get the map with detected combining functions.
     *
     * @return Map with unique names of type definitions as keys and names of
     *         combining functions associated with them as values.
     */
    public Map<String, String> getCombiningFunctions() {
        return this.combiningFunctions;
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
     * Called when an entity starts being parsed.
     *
     * @param entityName the name of the new entity
     */
    public void entityStarted(String entityName) {
        context.getFileToComponent().put(filePath, entityName);
    }

    /**
     * Adds new scope on the top of scopes stack.
     */
    private void pushLevel() {
        pushLevel(new DefaultEnvironment(this.environment));
    }

    private void pushFunctionTopLevel() {
        pushLevel(new DefaultEnvironment(this.environment, true));
    }

    private void pushLevel(Environment env) {
        //System.out.println("PUSHLEVEL");
        LOG.trace("pushlevel");
        this.environment = env;
    }

    /**
     * Removes scope from the top of stack.
     */
    private void popLevel() {
        //System.out.println("POPLEVEL");
        // XXX: see detect_bogus_env in semantics.c
        LOG.trace("poplevel");
        this.environment = this.environment.getParent().get();
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
                && !this.parserListener.interfaceDependency(filePath, ifaceName.getName(), ifaceName.getLocation())) {
            final String message = format("Cannot find interface %s.", ifaceName.getName());
            error(ifaceName.getLocation(), Optional.of(ifaceName.getEndLocation()), message);
        }
    }

    private void requireComponent(Word componentName) {
        if (this.parserListener != null
                && !this.parserListener.componentDependency(filePath, componentName.getName(),
                        componentName.getLocation())) {
            final String message = format("Cannot find component %s.", componentName.getName());
            error(componentName.getLocation(), Optional.of(componentName.getEndLocation()), message);
        }
    }

    private void extdefsFinish() {
        LOG.debug("extdefs finish");
        if (this.parserListener != null) {
            this.parserListener.extdefsFinished();
        }
    }

    private Word makeWord(Location location, Location endLocation, String name) {
        final Word result = new Word(location, name);
        result.setEndLocation(endLocation);
        return result;
    }

    private void warning(Location startLocation, Optional<Location> endLocation, String message) {
        this.errorHelper.warning(startLocation, endLocation, message);
    }


    private void error(Location startLocation, Optional<Location> endLocation, String message) {
        this.errorHelper.error(startLocation, endLocation, message);
    }

    private void analyzeExpression(Optional<Expression> expr) {
        if (expr.isPresent()) {
            ExpressionsAnalysis.analyze(expr.get(), environment, errorHelper);
        }
    }

    private void analyzeExpressions(List<? extends Expression> exprs) {
        for (Expression expr : exprs) {
            ExpressionsAnalysis.analyze(expr, environment, errorHelper);
        }
    }

    private void addAtomicStmtLabelName(String name) {
        if (!pstate.labelsNamesStack.isEmpty() && pstate.labelsNamesStack.peek().isPresent()) {
            pstate.labelsNamesStack.peek().get().add(name);
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
        private ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;
        private ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder;
        /**
         *
         */
        private Symbol symbol;
        /**
         * Current symbol value.
         */
        private String value;

        private IdentifierTypeVisitor identifierTypeVisitor;

        public LexerWrapper(pl.edu.mimuw.nesc.lexer.Lexer lexer,
                            ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                            ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder) {
            this.lexer = lexer;
            this.tokensMultimapBuilder = tokensMultimapBuilder;
            this.issuesMultimapBuilder = issuesMultimapBuilder;
            this.tokenQueue = new LinkedList<>();
            this.identifierTypeVisitor = new IdentifierTypeVisitor();
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
                final Optional<? extends ObjectDeclaration> declaration =
                        Parser.this.environment.getObjects().get(name);
                final int type = declaration.isPresent()
                        ? declaration.get().accept(identifierTypeVisitor, null)
                        : IDENTIFIER;
                symbol.setSymbolCode(type);
            }

            /*
             * Some lookahead is done below.
             *
             * We need to handle the following case:
             *
             *      module M {
             *          typedef int t;
             *          enum { MAGIC = 54 };
             *      } ...
             *
             *      configuration C { }
             *      implementation {
             *          components M as Someone;
             *          typedef Someone.t Ct;
             *          enum { GREATERMAGIC = Someone.MAGIC + 1 };
             *      }
             *
             * There is an ambiguity, since 'X.X' may appear in two contexts:
             * in a wiring declaration or in a C-like declaration.
             *
             * Detecting construct:
             *     COMPONENTREF . (IDENTIFIER | TYPENAME | MAGIC_STRING)
             *
             * when found, should be replaced by:
             *     COMPONENTREF . IDENTIFIER
             *
             * otherwise replace COMPONENTREF by IDENTIFIER.
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
                        if (isTypedef(componentRefSymbol, thirdSymbol)) {
                            componentRefSymbol.setSymbolCode(COMPONENTREF);
                            thirdSymbol.setSymbolCode(IDENTIFIER);
                        }
                    }
                    pushtoken(thirdSymbol);
                    pushtoken(secondSymbol);
                }
            }
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
    }

    private boolean isTypedef(Symbol componentRefSymbol, Symbol thirdSymbol) {
        /* Get the original name of the referenced component. */
        final Optional<? extends ObjectDeclaration> objectDecl =
                Parser.this.environment.getObjects().get(componentRefSymbol.getValue());
        if (!objectDecl.isPresent() || objectDecl.get().getKind() != ObjectKind.COMPONENT) {
            return false;
        }

        /* Check if there exists a component with given name. */
        final Optional<? extends NescDeclaration> entityDecl =
                ((ComponentRefDeclaration) objectDecl.get()).getComponentDeclaration();
        if (!entityDecl.isPresent()) {
            return false;
        }

        /* Check if the component contains a field with given name. */
        final ComponentDeclaration componentDecl = (ComponentDeclaration) entityDecl.get();
        if (componentDecl.getSpecificationEnvironment() == null) {
            return false;
        }
        final Optional<? extends ObjectDeclaration> fieldDecl =
                componentDecl.getSpecificationEnvironment().getObjects().get(thirdSymbol.getValue());
        return fieldDecl.isPresent() && fieldDecl.get().getKind() == ObjectKind.TYPENAME;
    }

}
;
%%
