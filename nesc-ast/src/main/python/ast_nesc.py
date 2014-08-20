from ast_core import *

#==============================================================================
#                             
#============================================================================== 

# ===== Naming convention =====
#
# All class names are borrowed from nodetypes.def and adjusted to Java naming
# convention. Class names start with capital letter.
#
# Examples:
#  node -> Node
#  tag_ref -> TagRef
#  asttype -> AstType
#
# Sometimes field or class names collide with language keywords.
#
#    abstract -> isAbstract
#    String -> StringAst
#

#==============================================================================
#                             Selected fields description
#==============================================================================

# ISATOMIC 
#    ATOMIC_ANY if the statement does not involve any shared variable accesses
#    ATOMIC_SINGLE if the statement involves a single access to a shared 
#    variable, and that access is guaranteed to be atomic (e.g., a single byte)
#    NOT_ATOMIC otherwise

#==============================================================================
#                                    Base types
#==============================================================================


class Node(BasicASTNode):
    """ A common superclass of all AST nodes. """
    location = ReferenceField("Location", visitable=False)
    endLocation = ReferenceField("Location", constructor_variable=0, visitable=False)
    next = ReferenceField("Node", constructor_variable=0, visitable=False)
    parent = ReferenceField("Node", constructor_variable=0, visitable=False)
    instantiation = ReferenceField("Node", constructor_variable=0, visitable=False)


class Declaration(BasicASTNode):
    """ A common superclass of all definitions. """
    superclass = Node


# TODO: statements probably should have pointer to the next statement
class Statement(BasicASTNode):
    """ A common superclass of all statements. """
    superclass = Node
    # PARENT_LOOP
    # - for break and continue: the containing for/while/do-while/switch
    # statement they escape from.
    # - for for/while/do-while: the containing for/while/do-while/switch
    # statement.
    parentLoop = ReferenceField("Statement", constructor_variable=0, visitable=False)
    # CONTAINING_ATOMIC
    # - for return statement: their containing atomic statement
    # - for labels and looping statements, their containing atomic statement
    # (or NULL for none). Used to check that break, continue and goto do not
    # break in or out of an atomic statement.
    # (Note: for nested atomic statements, CONTAINING_ATOMIC will point to a
    # dangling node as we drop these nested statements from the AST).
    containingAtomic = ReferenceField("AtomicStmt", constructor_variable=0, visitable=False)
    # See section: Selected fields description.
    isAtomic = ReferenceField("AtomicType", constructor_variable=0, visitable=False)


class Expression(BasicASTNode):
    """ A common superclass of all expressions. """
    superclass = Node
    # LVALUE is true if this expression can be used in a context requiring an
    # lvalue.
    lvalue = BoolField(constructor_variable=0, visitable=False)
    # SIDE_EFFECTS is true if the expression has side effects.
    sideEffects = BoolField(constructor_variable=0, visitable=False)
    # CST is non-null (and points to an appropriate constant) if this
    # expression is constant.
    cst = ReferenceField("KnownCst", constructor_variable=0, visitable=False)
    # BITFIELD is true if this lvalue is a bitfield.
    bitfield = BoolField(constructor_variable=0, visitable=False)
    # ISREGISTER is true if this lvalue is (declared to be) in a register.
    isRegister = BoolField(constructor_variable=0, visitable=False)
    #
    type = ReferenceField("Type", constructor_variable=0, visitable=False)
    # STATIC_ADDRESS is true for lvalues whose address is a constant
    # expression.
    staticAddress = ReferenceField("KnownCst", constructor_variable=0, visitable=False)
    # CONVERTED_TO_POINTER is true for expressions which default_conversion
    # indicates need converting to pointer type (note that these nodes did not
    # have their type changed).
    convertedToPointer = BoolField(constructor_variable=0, visitable=False)
    # CST_CHECKED is set to true once we've successfully checked this
    # expression's constantness, and associated constant value (used to avoid
    # duplicate error messages in repeated constant folding passes).
    cstChecked = BoolField(constructor_variable=0, visitable=False)
    # SPELLING saves the `spelling' (a user-friendly name) of expressions used
    # in initialisers.
    spelling = StringField(constructor_variable=0, visitable=False)
    # PARENS is TRUE if the expression is in parentheses
    parens = BoolField(constructor_variable=0, visitable=False)
    # IVALUE is a pointer to an ivalue (see init.h) holding the value of an
    # initialiser expression. On an init_list or in an expression used as
    # a simple initialiser (e.g., '3 + 2' in 'int x = 3 + 2'), this is the
    # value of the initialiser. Inside these initialisers, ivalue points into
    # the ivalue structure of the containing initialiser.
    ivalue = ReferenceField("IValue", constructor_variable=0, visitable=False)
    # CONTEXT is the usage context for this expression (see nesc-uses.h).
    context = ReferenceField("Context", constructor_variable=0, visitable=False)
    # See section: Selected fields description.
    isAtomic = ReferenceField("AtomicType", constructor_variable=0, visitable=False)


class TypeElement(BasicASTNode):
    """ A common superclass for all type-building elements (qualifiers, modifiers, attributes, etc.). """
    superclass = Node


class Declarator(BasicASTNode):
    """ A common superclass for all declarators. """
    superclass = Node


class Label(BasicASTNode):
    """ A common superclass for all labels. """
    superclass = Node
    # NEXT_LABEL points to the next case or default label of a switch (for case
    # or default labels only).
    nextLabel = ReferenceField("Label", constructor_variable=0, visitable=False)


#==============================================================================
#                                 Declarations
#==============================================================================

class ErrorDecl(BasicASTNode):
    """ Placeholder for erroneous declarations. """
    superclass = Declaration


# Asm statement STMT at the top level of a file (GCC).
class AsmDecl(BasicASTNode):
    superclass = Declaration
    asmStmt = ReferenceField("AsmStmt")


# The declaration MODIFIERS DECLS; DECLS is a list.
class DataDecl(BasicASTNode):
    superclass = Declaration
    modifiers = ReferenceListField("TypeElement")
    declarations = ReferenceListField("Declaration")


class ExtensionDecl(BasicASTNode):
    """
    GCC uses the __extension__ attribute when using the -ansi flag to avoid warnings in headers with GCC extensions.
    This is mostly used in glibc with function declarations using long long.
    """
    superclass = Declaration
    declaration = ReferenceField("Declaration")


class EllipsisDecl(BasicASTNode):
    """ # A pseudo-declaration to represent ... in a function argument list. """
    superclass = Declaration


class Enumerator(BasicASTNode):
    """ The enumeration element. """
    superclass = Declaration
    # name is optional.
    name = StringField()
    value = ReferenceField("Expression")
    declaration = ReferenceField("ConstantDeclaration", constructor_variable=0, visitable=False)


class OldIdentifierDecl(BasicASTNode):
    """
    Parameter declaration in old-style way (K&R style).

    <pre>
        void f(i, c, fp)
        int i;
        char c;
        float *fp;
        { ... }
    </pre>
    """
    superclass = Declaration
    # CSTRING in an old-style parameter list.
    name = StringField()
    declaration = ReferenceField("VariableDeclaration", constructor_variable=0, visitable=False)


class FunctionDecl(BasicASTNode):
    """
    <p>A function declaration.</p>
    <p><code>declaration</code> can be function declaration or interface
    reference.</p>
    """
    superclass = Declaration
    declarator = ReferenceField("Declarator")
    modifiers = ReferenceListField("TypeElement")
    attributes = ReferenceListField("Attribute")
    # OLD_PARMS is the old-style parameter declaration list.
    oldParms = ReferenceListField("Declaration", constructor_variable=0)
    body = ReferenceField("Statement")
    isNested = BoolField(visitable=False)
    declaration = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)

    # FIXME refactor attributes below
    parentFunction = ReferenceField("FunctionDecl", constructor_variable=0, visitable=False)
    fdeclarator = ReferenceField("FunctionDeclarator", constructor_variable=0, visitable=False)
    declaredType = ReferenceField("Type", constructor_variable=0, visitable=False)
    #undeclaredVariables = ReferenceField("Env", constructor_variable=0, visitable=False)
    #baseLabels = ReferenceField("Env", constructor_variable=0, visitable=False)
    #scopedLabels = ReferenceField("Env", constructor_variable=0, visitable=False)
    currentLoop = ReferenceField("Statement", constructor_variable=0, visitable=False)
    nlocals = IntField(constructor_variable=0, visitable=False)


# Used as the AST node for implicit declarations.
class ImplicitDecl(BasicASTNode):
    superclass = Declaration
    # IDENT points to the identifier node that implicitly declared the function.
    ident = ReferenceField("Identifier")


class VariableDecl(BasicASTNode):
    """
    <p>Declaration of the following syntax:
    <code>declarator asm_stmt attributes [= initializer]</code>.</p>
    <p>The name of the node is misleading, it corresponds not only to
    variable declaration, but also typedef declaration or
    function forward declaration.<p>
    """
    superclass = Declaration
    declarator = ReferenceField("Declarator", optional=True)
    attributes = ReferenceListField("Attribute")
    initializer = ReferenceField("Expression", constructor_variable=0, optional=True)
    asmStmt = ReferenceField("AsmStmt", optional=True)
    declaration = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)

    # FIXME: refactor attributes below
    # DECLARED_TYPE is the type in this declaration (which may be different
    # than that in DDECL->TYPE).
    #declaredType = ReferenceField("Type", constructor_variable=0, visitable=False)
    # FORWARD is true for parameters that are forward declarations.
    forward = BoolField(constructor_variable=0, visitable=False)


# Declaration of field DECLARATOR ATTRIBUTES : ARG1.
class FieldDecl(BasicASTNode):
    superclass = Declaration
    declarator = ReferenceField("Declarator")
    # QUALIFIERS and ATTRIBUTEES are lists.
    attributes = ReferenceListField("Attribute")
    # ARG1 is an optional bitfield specifier.
    bitfield = ReferenceField("Expression")
    declaration = ReferenceField("FieldDeclaration", constructor_variable=0, visitable=False)

    # FIXME: refactor attributes below
    # TYPE_CHECKED is set to true once it has been checked that this field is
    # of network type (inside network structures).
    typeChecked = BoolField(constructor_variable=0, visitable=False)


#==============================================================================
#                           Types and type elements
#==============================================================================

# The source-level type QUALIFIERS DECLARATOR.
class AstType(BasicASTNode):
    superclass = Node
    declarator = ReferenceField("Declarator")
    qualifiers = ReferenceListField("TypeElement")
    type = ReferenceField("Type", constructor_variable=0)


# typedef-type with declaration DDECL. The name is ddecl->name.
class Typename(BasicASTNode):
    superclass = TypeElement
    name = StringField()
    declaration = ReferenceField("TypenameDeclaration", constructor_variable=0, visitable=False)


# typeof ARG1
class TypeofExpr(BasicASTNode):
    superclass = TypeElement
    expression = ReferenceField("Expression")


# typeof(ASTTYPE)
class TypeofType(BasicASTNode):
    superclass = TypeElement
    asttype = ReferenceField("AstType")


# base type for gcc and nesc attributes.
class Attribute(BasicASTNode):
    superclass = TypeElement
    name = ReferenceField("Word")


# The (gcc) attribute WORD1(ARGS).
class GccAttribute(BasicASTNode):
    superclass = Attribute
    # args can be empty, and may not be semantically valid.
    arguments = ReferenceListField("Expression")


# Storage class specifier, type specifier or type qualifier ID (see RID_xxx)
class Rid(BasicASTNode):
    superclass = TypeElement
    id = ReferenceField("RID")


# Type or function qualifier ID (see qualifiers.h and type_quals in types.h)
class Qualifier(BasicASTNode):
    superclass = TypeElement
    id = ReferenceField("RID")


# struct/union/enum WORD1 { FIELDS; }  ATTRIBUTES
# ATTRIBUTES and FIELDS are lists.
# ATTRIBUTES is GCC specific. WORD1 is optional.
# DEFINED is TRUE if this declaration defines the struct/union/enum.
# DEFINED == FALSE => FIELDS == NULL
class TagRef(BasicASTNode):
    superclass = TypeElement
    name = ReferenceField("Word")
    attributes = ReferenceListField("Attribute")
    fields = ReferenceListField("Declaration")
    isDefined = BoolField(visitable=False)


# A struct
class StructRef(BasicASTNode):
    superclass = TagRef


# An attribute definition.
# FIXME what does this node represents?
class AttributeRef(BasicASTNode):
    superclass = TagRef
    declaration = ReferenceField("AttributeDeclaration", constructor_variable=0, visitable=False)


# A union
class UnionRef(BasicASTNode):
    superclass = TagRef
    declaration = ReferenceField("UnionDeclaration", constructor_variable=0, visitable=False)


# An enum
class EnumRef(BasicASTNode):
    superclass = TagRef
    declaration = ReferenceField("EnumDeclaration", constructor_variable=0, visitable=False)


#==============================================================================
#                                 Declarators
#==============================================================================

#   The last DECLARATOR in a chain is:
#   NULL_NODE in absolute declarations
#   an identifier_decl otherwise

# A common supertype for function/pointer/array declarator which includes
# the nested declarator.
class NestedDeclarator(BasicASTNode):
    superclass = Declarator
    # Refers to declarator that precedes e.g. function/array parentheses
    # or follows pointer asterisk.
    declarator = ReferenceField("Declarator")


# Function declarator DECLARATOR(PARMS). PARMS is a list of declarations.
# ENV is the environment for parms
# GPARMS is the list of declarations of generic parameters (commands, events only)
# RETURN_TYPE (optional) contains an overridden return type from nesdoc
class FunctionDeclarator(BasicASTNode):
    superclass = NestedDeclarator
    parameters = ReferenceListField("Declaration")
    genericParameters = ReferenceListField("Declaration", optional=True)
    qualifiers = ReferenceListField("TypeElement")
    environment = ReferenceField("Environment", constructor_variable=0, visitable=False)
    #returnType = ReferenceField("AstType", constructor_variable=0, visitable=False)


# Pointer declarator *DECLARATOR
class PointerDeclarator(BasicASTNode):
    superclass = NestedDeclarator


# Declarator MODIFIERS DECLARATOR. The MODIFIERS are qualifiers
# or attributes.
# Note: MODIFIERS is never NULL
class QualifiedDeclarator(BasicASTNode):
    superclass = NestedDeclarator
    modifiers = ReferenceListField("TypeElement")


# Array declarator DECLARATOR[ARG1]. ARG1 is optional.
class ArrayDeclarator(BasicASTNode):
    superclass = NestedDeclarator
    index = ReferenceField("Expression")


# Declaration of CSTRING
class IdentifierDeclarator(BasicASTNode):
    superclass = Declarator
    name = StringField()


#==============================================================================
#                                  Statements
#==============================================================================

# Placeholder for erroneous statements.
class ErrorStmt(BasicASTNode):
    superclass = Statement


# The statement asm QUALIFIERS (ARG1 : ASM_OPERANDS1 : ASM_OPERANDS2 : ASM_CLOBBERS)
# where ASM_OPERANDS1, ASM_OPERANDS2, QUALIFIERS are optional, ASM_CLOBBERS is a list (GCC)
class AsmStmt(BasicASTNode):
    superclass = Statement
    arg1 = ReferenceField("Expression")
    asmOperands1 = ReferenceListField("AsmOperand")
    asmOperands2 = ReferenceListField("AsmOperand")
    asmClobbers = ReferenceListField("StringAst")
    qualifiers = ReferenceListField("TypeElement")


class CompoundStmt(BasicASTNode):
    """
    <p>Represents a block with its own environment.</p>
    <p>It can be body of function or statement.</p>
    """
    superclass = Statement
    idLabels = ReferenceListField("IdLabel")
    declarations = ReferenceListField("Declaration")
    statements = ReferenceListField("Statement")
    environment = ReferenceField("Environment", constructor_variable=0, visitable=False)


# IF (CONDITION) STMT1 ELSE STMT2. STMT2 is optional
class IfStmt(BasicASTNode):
    superclass = Statement
    condition = ReferenceField("Expression")
    trueStatement = ReferenceField("Statement")
    falseStatement = ReferenceField("Statement")


# LABEL: STMT
class LabeledStmt(BasicASTNode):
    superclass = Statement
    label = ReferenceField("Label")
    statement = ReferenceField("Statement")


# EXPR;
class ExpressionStmt(BasicASTNode):
    superclass = Statement
    expression = ReferenceField("Expression")


# Basic type for all conditional statements
class ConditionalStmt(BasicASTNode):
    superclass = Statement
    condition = ReferenceField("Expression")
    statement = ReferenceField("Statement")


class WhileStmt(BasicASTNode):
    superclass = ConditionalStmt


class DoWhileStmt(BasicASTNode):
    superclass = ConditionalStmt


# SWITCH (CONDITION) STMT.
# NEXT_LABEL points to the switches first label
class SwitchStmt(BasicASTNode):
    superclass = ConditionalStmt
    nextLabel = ReferenceField("Label", constructor_variable=0, visitable=False)


# FOR (ARG1; ARG2; ARG3) STMT. ARG1, ARG2, ARG3 are optional
class ForStmt(BasicASTNode):
    superclass = Statement
    initExpression = ReferenceField("Expression")
    conditionExpression = ReferenceField("Expression")
    incrementExpression = ReferenceField("Expression")
    statement = ReferenceField("Statement")


class BreakStmt(BasicASTNode):
    superclass = Statement


class ContinueStmt(BasicASTNode):
    superclass = Statement


# RETURN ARG1. ARG1 is optional
class ReturnStmt(BasicASTNode):
    superclass = Statement
    value = ReferenceField("Expression")


class GotoStmt(BasicASTNode):
    superclass = Statement
    idLabel = ReferenceField("IdLabel")


# GOTO *ARG1 (GCC)
class ComputedGotoStmt(BasicASTNode):
    superclass = Statement
    address = ReferenceField("Expression")


class EmptyStmt(BasicASTNode):
    superclass = Statement


#==============================================================================
#                                  Expressions
#==============================================================================

class ErrorExpr(BasicASTNode):
    """ Placeholder for erroneous expressions. """
    superclass = Expression


class Unary(BasicASTNode):
    """ Base class of unary expressions. """
    superclass = Expression
    argument = ReferenceField("Expression")


class Binary(BasicASTNode):
    """ Base class of binary expressions. """
    superclass = Expression
    leftArgument = ReferenceField("Expression")
    rightArgument = ReferenceField("Expression")


class Comma(BasicASTNode):
    """ A comma separated list of expressions. """
    superclass = Expression
    expressions = ReferenceListField("Expression")


class SizeofType(BasicASTNode):
    """ Calculates the number of bytes of storage the expression occupies. """
    superclass = Expression
    asttype = ReferenceField("AstType")


class AlignofType(BasicASTNode):
    """
    Allows to inquire about how an object is aligned, or the minimum alignment usually required by a type.
    """
    superclass = Expression
    asttype = ReferenceField("AstType")


class LabelAddress(BasicASTNode):
    """
    <p>
    You can get the address of a label defined in the current function (or a containing function) with the unary
    operator ‘&&’. The value has type void *. This value is a constant and can be used wherever a constant of that type
    is valid. For example:

    <pre>
        void *ptr;
        ptr = &&foo;
    </pre>

    To use these values, you need to be able to jump to one. This is done with the computed goto statement, goto *exp;.
    For example,

    <pre>
        goto *ptr;
    </pre>
    </p>
    <p>gcc.gnu.org/onlinedocs/gcc/Labels-as-Values.html</p>
    """
    superclass = Expression
    idLabel = ReferenceField("IdLabel")


class Cast(BasicASTNode):
    superclass = Unary
    asttype = ReferenceField("AstType")


class CastList(BasicASTNode):
    superclass = Expression
    asttype = ReferenceField("AstType")
    initExpr = ReferenceField("Expression")


class Conditional(BasicASTNode):
    """
    Conditional expression. The middle operand in a conditional expression may be omitted. Then if the first operand
    is nonzero, its value is the value of the conditional expression.
    """
    superclass = Expression
    condition = ReferenceField("Expression")
    onTrueExp = ReferenceField("Expression")
    onFalseExp = ReferenceField("Expression")


class Identifier(BasicASTNode):
    superclass = Expression
    name = StringField()
    declaration = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)


class CompoundExpr(BasicASTNode):
    """
    <p>A compound statement enclosed in parentheses may appear as an expression in GNU C. This allows you to use loops,
    switches, and local variables within an expression.</p>
    <p>http://gcc.gnu.org/onlinedocs/gcc/Statement-Exprs.html</p>
    """
    superclass = Expression
    statement = ReferenceField("Statement")


class FunctionCall(BasicASTNode):
    """
    <p>Function call.</p>
    <p>If vaArgCall is present, this is actually a call to the pseudo-function __builtin_va_arg(arguments, vaArgCall)
    where vaArgCall is a type. In this case function is a dummy identifier.</p>
    """
    superclass = Expression
    function = ReferenceField("Expression")
    arguments = ReferenceListField("Expression")
    vaArgCall = ReferenceField("AstType")
    callKind = ReferenceField("NescCallKind")


class ArrayRef(BasicASTNode):
    """
    <p>Array reference</p>
    <p>NOTICE: originally this node was extending BinaryExpression.</p>
    """
    superclass = Expression
    array = ReferenceField("Expression")
    index = ReferenceListField("Expression")


class FieldRef(BasicASTNode):
    superclass = Unary
    fieldName = StringField()
    declaration = ReferenceField("FieldDeclaration", constructor_variable=0, visitable=False)


class Dereference(BasicASTNode):
    superclass = Unary


class ExtensionExpr(BasicASTNode):
    superclass = Unary


class SizeofExpr(BasicASTNode):
    superclass = Unary


class AlignofExpr(BasicASTNode):
    superclass = Unary


class Realpart(BasicASTNode):
    superclass = Unary


class Imagpart(BasicASTNode):
    superclass = Unary


class AddressOf(BasicASTNode):
    superclass = Unary


class UnaryMinus(BasicASTNode):
    superclass = Unary


class UnaryPlus(BasicASTNode):
    superclass = Unary


class Conjugate(BasicASTNode):
    superclass = Unary


class Bitnot(BasicASTNode):
    superclass = Unary


class Not(BasicASTNode):
    superclass = Unary


class Increment(BasicASTNode):
    superclass = Unary
    # FIXME
    #temp1 = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)
    #temp2 = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)


class Preincrement(BasicASTNode):
    superclass = Increment


class Predecrement(BasicASTNode):
    superclass = Increment


class Postincrement(BasicASTNode):
    superclass = Increment


class Postdecrement(BasicASTNode):
    superclass = Increment


class Plus(BasicASTNode):
    superclass = Binary


class Minus(BasicASTNode):
    superclass = Binary


class Times(BasicASTNode):
    superclass = Binary


class Divide(BasicASTNode):
    superclass = Binary


class Modulo(BasicASTNode):
    superclass = Binary


class Lshift(BasicASTNode):
    superclass = Binary


class Rshift(BasicASTNode):
    superclass = Binary


class Comparison(BasicASTNode):
    superclass = Binary


class Leq(BasicASTNode):
    superclass = Comparison


class Geq(BasicASTNode):
    superclass = Comparison


class Lt(BasicASTNode):
    superclass = Comparison


class Gt(BasicASTNode):
    superclass = Comparison


class Eq(BasicASTNode):
    superclass = Comparison


class Ne(BasicASTNode):
    superclass = Comparison


class Bitand(BasicASTNode):
    superclass = Binary


class Bitor(BasicASTNode):
    superclass = Binary


class Bitxor(BasicASTNode):
    superclass = Binary


class Andand(BasicASTNode):
    superclass = Binary


class Oror(BasicASTNode):
    superclass = Binary


class Assignment(BasicASTNode):
    superclass = Binary
    # FIXME
    #temp1 = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)


class Assign(BasicASTNode):
    superclass = Assignment


class PlusAssign(BasicASTNode):
    superclass = Assignment


class MinusAssign(BasicASTNode):
    superclass = Assignment


class TimesAssign(BasicASTNode):
    superclass = Assignment


class DivideAssign(BasicASTNode):
    superclass = Assignment


class ModuloAssign(BasicASTNode):
    superclass = Assignment


class LshiftAssign(BasicASTNode):
    superclass = Assignment


class RshiftAssign(BasicASTNode):
    superclass = Assignment


class BitandAssign(BasicASTNode):
    superclass = Assignment


class BitorAssign(BasicASTNode):
    superclass = Assignment


class BitxorAssign(BasicASTNode):
    superclass = Assignment


class InitList(BasicASTNode):
    """
    <p>Initializer of list of elements.</p>
    <p>http://gcc.gnu.org/onlinedocs/gcc/Designated-Inits.html</p>

    <pre>
        int a[6] = { 0, 0, 15, 0, 29, 0 };
    </pre>
    """
    superclass = Expression
    arguments = ReferenceListField("Expression")


class InitSpecific(BasicASTNode):
    """
    <p>Initializer of specific elements.</p>
    <p>http://gcc.gnu.org/onlinedocs/gcc/Designated-Inits.html</p>

    <pre>
        int a[6] = { [4] = 29, [2] = 15 };
    </pre>
    """
    superclass = Expression
    designator = ReferenceListField("Designator")
    initExpr = ReferenceField("Expression")


class Designator(BasicASTNode):
    """ Base class for designated initializers. """
    superclass = Node


class DesignateField(BasicASTNode):
    """
    <p>In a structure initializer, one can specify the name of a field to initialize.</p>
    <p>http://gcc.gnu.org/onlinedocs/gcc/Designated-Inits.html</p>

    <p>
    For example, given the following structure:
    <pre>
        struct point { int x, y; };
    </pre>
    the following initialization is possible:
    <pre>
        struct point p = { .y = yvalue, .x = xvalue };
    </pre>
    </p>
    """
    superclass = Designator
    name = StringField()


class DesignateIndex(BasicASTNode):
    """
    <p>GNU extension allows to initialize a range of elements to the same value.</p>
    <p>http://gcc.gnu.org/onlinedocs/gcc/Designated-Inits.html</p>

    <pre>
    int widths[] = { [0 ... 9] = 1, [10 ... 99] = 2, [100] = 3 };
    </pre>
    """
    superclass = Designator
    first = ReferenceField("Expression")
    last = ReferenceField("Expression")


#==============================================================================
#                                   Constants
#==============================================================================

class LexicalCst(BasicASTNode):
    """ A constant kept in unparsed form. """
    superclass = Expression
    string = StringField()


class StringCst(BasicASTNode):
    """ A single lexical string. """
    superclass = LexicalCst


class StringAst(BasicASTNode):
    """ A list of StringCst nodes forming a single string constant. """
    superclass = Expression
    strings = ReferenceListField("StringCst")
    # FIXME: does it need declaration field?
    # declaration = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)


#==============================================================================
#                                     Labels
#==============================================================================

class IdLabel(BasicASTNode):
    superclass = Label
    id = StringField()
    declaration = ReferenceField("LabelDeclaration", constructor_variable=0, visitable=False)


class CaseLabel(BasicASTNode):
    """
    <p>C extensions allows to use in switch statement case ranges:</p>
    <pre>
        case low ... high:
    </pre>

    <p>NOTICE: Be careful: Write spaces around the ..., for otherwise it may be parsed wrong when you use it with
    integer values.</p>
    """
    superclass = Label
    low = ReferenceField("Expression")
    high = ReferenceField("Expression")


class DefaultLabel(BasicASTNode):
    """ Default label is switch-case construct. """
    superclass = Label


#==============================================================================
#                                 Miscellaneous
#==============================================================================

class Word(BasicASTNode):
    superclass = Node
    name = StringField()


# TODO: description
class AsmOperand(BasicASTNode):
    superclass = Node
    word1 = ReferenceField("Word", optional=True)
    string = ReferenceField("StringAst")
    arg1 = ReferenceField("Expression")


#==============================================================================
#                                nesc extensions
#==============================================================================

#==============================================================================
#                          The different kinds of files
#==============================================================================

class NescDecl(BasicASTNode):
    """ Base class for nesc component or interface declaration. """
    superclass = Declaration
    name = ReferenceField("Word")
    attributes = ReferenceListField("Attribute")


class Interface(BasicASTNode):
    """ Interface definition. """
    superclass = NescDecl
    parameters = ReferenceListField("Declaration", optional=True)
    declarations = ReferenceListField("Declaration")
    declaration = ReferenceField("InterfaceDeclaration", constructor_variable=0, visitable=False)


class Component(BasicASTNode):
    """ Base class for nesc component. """
    superclass = NescDecl
    isAbstract = BoolField()
    parameters = ReferenceListField("Declaration", optional=True)
    declarations = ReferenceListField("Declaration")
    implementation = ReferenceField("Implementation")
    parameterEnvironment = ReferenceField("Environment", constructor_variable=0, visitable=False)
    specificationEnvironment = ReferenceField("Environment", constructor_variable=0, visitable=False)


class Configuration(BasicASTNode):
    superclass = Component
    declaration = ReferenceField("ConfigurationDeclaration", constructor_variable=0, visitable=False)


class Module(BasicASTNode):
    superclass = Component
    declaration = ReferenceField("ModuleDeclaration", constructor_variable=0, visitable=False)


class BinaryComponent(BasicASTNode):
    superclass = Component


class Implementation(BasicASTNode):
    superclass = Node
    environment = ReferenceField("Environment", constructor_variable=0, visitable=False)
    #cdecl = ReferenceField("NescDeclaration", constructor_variable=0, visitable=False)


class ConfigurationImpl(BasicASTNode):
    superclass = Implementation
    declarations = ReferenceListField("Declaration")


class ModuleImpl(BasicASTNode):
    superclass = Implementation
    declarations = ReferenceListField("Declaration")


class BinaryComponentImpl(BasicASTNode):
    superclass = Implementation


class ComponentsUses(BasicASTNode):
    """
    <p>This node was not present in original compiler.</p>

    <pre>
        configuration NullAppC{}
            implementation {
            components MainC, NullC; # <- components uses
            MainC.Boot <- NullC;
        }
    </p>
    """
    superclass = Declaration
    components = ReferenceListField("ComponentRef")


#==============================================================================
#                           Component definition types
#==============================================================================

class RpInterface(BasicASTNode):
    superclass = Declaration
    declarations = ReferenceListField("Declaration")


class RequiresInterface(BasicASTNode):
    """ List of required interfaces by component. """
    superclass = RpInterface


class ProvidesInterface(BasicASTNode):
    """ List of interfaces provided by component. """
    superclass = RpInterface


class InterfaceRef(BasicASTNode):
    """
    <p>Interface reference in component specification.</p>
    <p>'uses'/'provides' 'interface' NAME '<'ARGUMENTS'>' '['GENERIC_PARAMETERS']' 'as' ALIAS ATTRIBUTES;</p>

    <pre>
        configuration All {
            provides interface A as ProvidedA1;
            provides interface A as ProvidedA2;
            provides interface A as ProvidedA3;
            uses interface A as UsedA1;
        }
    </pre>
    """
    superclass = Declaration
    name = ReferenceField("Word")
    arguments = ReferenceListField("Expression", optional=True)
    alias = ReferenceField("Word", constructor_variable=False, optional=True)
    genericParameters = ReferenceListField("Declaration", constructor_variable=False, optional=True)
    attributes = ReferenceListField("Attribute", constructor_variable=False)
    declaration = ReferenceField("InterfaceRefDeclaration", constructor_variable=False, visitable=False)


class ComponentRef(BasicASTNode):
    superclass = Declaration
    name = ReferenceField("Word")
    alias = ReferenceField("Word", constructor_variable=0, optional=True)
    isAbstract = BoolField()
    arguments = ReferenceListField("Expression")
    declaration = ReferenceField("ComponentRefDeclaration", constructor_variable=0, visitable=False)


class Connection(BasicASTNode):
    """ Wiring statement connecting two endpoints. """
    superclass = Declaration
    endPoint1 = ReferenceField("EndPoint")
    endPoint2 = ReferenceField("EndPoint")


class RpConnection(BasicASTNode):
    """ Link wire. """
    superclass = Connection


class EqConnection(BasicASTNode):
    """ Equate wire. """
    superclass = Connection


class EndPoint(BasicASTNode):
    superclass = Node
    ids = ReferenceListField("ParameterisedIdentifier")


class ParameterisedIdentifier(BasicASTNode):
    """
    Endpoint can be parameterised.

    <pre>
        Receive = ActiveMessageC.Receive[amId];
    </pre>
    """
    superclass = Node
    name = ReferenceField("Word")
    arguments = ReferenceListField("Expression")


#==============================================================================
#                  Types for extensions to the regular C syntax
#==============================================================================

# parameterised declaration DECLARATOR [ PARMS ].
class GenericDeclarator(BasicASTNode):
    superclass = Declarator
    declarator = ReferenceField("Declarator")
    parameters = ReferenceListField("Declaration")


class GenericCall(BasicASTNode):
    """
    <p>Parameterised interface function call.</p>
    <p>name: name or alias of interface</p>

    <pre>
        err = call AMSend.send[amId](dest, msg, len);
    </pre>
    """
    superclass = Expression
    name = ReferenceField("Expression")
    arguments = ReferenceListField("Expression")


class InterfaceRefDeclarator(BasicASTNode):
    """
    <p>Declarator for interface command/event.</p>
    <p>name: name or alias of interface</p>

    <pre>
        command error_t Send.send[uint8_t clientId](message_t* msg, uint8_t len) { ...
    </pre>
    """
    superclass = NestedDeclarator
    name = ReferenceField("Word")


class InterfaceDeref(BasicASTNode):
    """
    Invocation of interface command.

    <pre>
        am_id_t amId = call AMPacket.type(msg);
    </pre>
    """
    superclass = Unary
    methodName = StringField()
    # FIXME: does it need declaration reference?
    #declaration = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)


class ComponentDeref(BasicASTNode):
    """
    Reference to component's field.

    <pre>
        module M {
            enum { one = 1 };
            ...
        } ...

        configuration C {}
        implementation {
            components M as First;
            enum { two = First.one * 2 };
            ...
        }
    </pre>
    """
    superclass = Unary
    fieldName = StringField()
    # FIXME: does it need declaration reference?
    #declaration = ReferenceField("ObjectDeclaration", constructor_variable=0, visitable=False)


class ComponentTyperef(BasicASTNode):
    """
    Reference to a typedef in component.

    <pre>
        module M {
            typedef int my_t;
            ...
        } ...

        configuration C {}
        implementation {
            components M as First;
            typedef First.my_t conf_C_t;
            ...
        }
    </pre>
    """
    superclass = Typename
    typeName = StringField()


class AtomicStmt(BasicASTNode):
    superclass = Statement
    statement = ReferenceField("Statement")


class NxStructRef(BasicASTNode):
    superclass = StructRef


class NxUnionRef(BasicASTNode):
    superclass = UnionRef


class NescAttribute(BasicASTNode):
    """ NesC attribute decorated with @. """
    superclass = Attribute
    value = ReferenceField("Expression")
    declaration = ReferenceField("AttributeDeclaration", constructor_variable=0, visitable=False)


class TargetAttribute(BasicASTNode):
    """ A target-specific extension represented internally as a gcc-style attribute. """
    superclass = GccAttribute


#==============================================================================
#                      Types for the polymorphic extensions
#==============================================================================

class TypeParmDecl(BasicASTNode):
    """
    Type parameter declaration in generic interface declaration.

    <pre>
        interface Queue<t> { ... }
        interface Timer<precision_tag> { ... }
    </pre>
    """
    superclass = Declaration
    name = StringField()
    attributes = ReferenceListField("Attribute")
    declaration = ReferenceField("TypenameDeclaration", constructor_variable=0, visitable=False)


class TypeArgument(BasicASTNode):
    """
    Type parameter in generic components. For example:

    <pre>
        components new QueueC(_srf_queue_entry_t*, QUEUE_SIZE) as SendQueue;
    </pre>
    """
    superclass = Expression
    asttype = ReferenceField("AstType")

#==============================================================================
#==============================================================================
if __name__ == "__main__":
    generate_code(DST_LANGUAGE.JAVA, "pl/edu/mimuw/nesc/ast/gen")
