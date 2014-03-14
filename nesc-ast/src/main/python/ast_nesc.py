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


# The common supertype of all AST nodes.
class Node(BasicASTNode):
    location = ReferenceField("Location")
    endLocation = ReferenceField("Location", constructor_variable=0)
    next = ReferenceField("Node", constructor_variable=0)
    parent = ReferenceField("Node", constructor_variable=0)
    instantiation = ReferenceField("Node", constructor_variable=0)


# The common type of all definitions.
class Declaration(BasicASTNode):
    superclass = Node


# The common type of all statements.
class Statement(BasicASTNode):
    superclass = Node
    # PARENT_LOOP
    # - for break and continue: the containing for/while/do-while/switch
    # statement they escape from.
    # - for for/while/do-while: the containing for/while/do-while/switch
    # statement.
    parentLoop = ReferenceField("Statement", constructor_variable=0)
    # CONTAINING_ATOMIC
    # - for return statement: their containing atomic statement
    # - for labels and looping statements, their containing atomic statement
    # (or NULL for none). Used to check that break, continue and goto do not
    # break in or out of an atomic statement.
    # (Note: for nested atomic statements, CONTAINING_ATOMIC will point to a
    # dangling node as we drop these nested statements from the AST).
    containingAtomic = ReferenceField("AtomicStmt", constructor_variable=0)
    # See section: Selected fields description.
    isAtomic = ReferenceField("AtomicType", constructor_variable=0)


# The common type of all expressions.
class Expression(BasicASTNode):
    superclass = Node
    # LVALUE is true if this expression can be used in a context requiring an
    # lvalue.
    lvalue = BoolField(constructor_variable=0)
    # SIDE_EFFECTS is true if the expression has side effects.
    sideEffects = BoolField(constructor_variable=0)
    # CST is non-null (and points to an appropriate constant) if this
    # expression is constant.
    cst = ReferenceField("KnownCst", constructor_variable=0)
    # BITFIELD is true if this lvalue is a bitfield.
    bitfield = BoolField(constructor_variable=0)
    # ISREGISTER is true if this lvalue is (declared to be) in a register.
    isRegister = BoolField(constructor_variable=0)
    #
    type = ReferenceField("Type", constructor_variable=0)
    # STATIC_ADDRESS is true for lvalues whose address is a constant
    # expression.
    staticAddress = ReferenceField("KnownCst", constructor_variable=0)
    # CONVERTED_TO_POINTER is true for expressions which default_conversion
    # indicates need converting to pointer type (note that these nodes did not
    # have their type changed).
    convertedToPointer = BoolField(constructor_variable=0)
    # CST_CHECKED is set to true once we've successfully checked this
    # expression's constantness, and associated constant value (used to avoid
    # duplicate error messages in repeated constant folding passes).
    cstChecked = BoolField(constructor_variable=0)
    # SPELLING saves the `spelling' (a user-friendly name) of expressions used
    # in initialisers.
    spelling = StringField(constructor_variable=0)
    # PARENS is TRUE if the expression is in parentheses
    parens = BoolField(constructor_variable=0)
    # IVALUE is a pointer to an ivalue (see init.h) holding the value of an
    # initialiser expression. On an init_list or in an expression used as
    # a simple initialiser (e.g., '3 + 2' in 'int x = 3 + 2'), this is the
    # value of the initialiser. Inside these initialisers, ivalue points into
    # the ivalue structure of the containing initialiser.
    ivalue = ReferenceField("IValue", constructor_variable=0)
    # CONTEXT is the usage context for this expression (see nesc-uses.h).
    context = ReferenceField("Context", constructor_variable=0)
    # See section: Selected fields description.
    isAtomic = ReferenceField("AtomicType", constructor_variable=0)


# A common super-type for all type-building elements (qualifiers, etc.).
class TypeElement(BasicASTNode):
    superclass = Node


# A common super-type for all declarator elements.
class Declarator(BasicASTNode):
    superclass = Node


# A common super-type for all labels.
class Label(BasicASTNode):
    superclass = Node
    # NEXT_LABEL points to the next case or default label of a switch (for case
    # or default labels only).
    nextLabel = ReferenceField("Label", constructor_variable=0)


#==============================================================================
#                                 Declarations
#==============================================================================

# Placeholder for erroneous declarations
class ErrorDecl(BasicASTNode):
    superclass = Declaration


# Asm statement STMT at the top level of a file (GCC).
class AsmDecl(BasicASTNode):
    superclass = Declaration
    asmStmt = ReferenceField("AsmStmt")


# The declaration MODIFIERS DECLS; DECLS is a list.
class DataDecl(BasicASTNode):
    superclass = Declaration
    modifiers = ReferenceListField("TypeElement")
    decls = ReferenceListField("Declaration")


# __extension__ DECL; (GCC)
class ExtensionDecl(BasicASTNode):
    superclass = Declaration
    decl = ReferenceField("Declaration")


# A pseudo-declaration to represent ... in a function argument list.
class EllipsisDecl(BasicASTNode):
    superclass = Declaration


# The enumeration element CSTRING = ARG1.
class Enumerator(BasicASTNode):
    superclass = Declaration
    # CSTRING is optional.
    name = StringField()
    arg1 = ReferenceField("Expression")
    ddecl = ReferenceField("DataDeclaration")


class OldIdentifierDecl(BasicASTNode):
    superclass = Declaration
    # CSTRING in an old-style parameter list.
    name = StringField()
    ddecl = ReferenceField("DataDeclaration")


# A function declaration with body STMT
class FunctionDecl(BasicASTNode):
    superclass = Declaration
    declarator = ReferenceField("Declarator")
    modifiers = ReferenceListField("TypeElement")
    attributes = ReferenceListField("Attribute")
    # OLD_PARMS is the old-style parameter declaration list.
    oldParms = ReferenceListField("Declaration")
    stmt = ReferenceField("Statement")
    parentFunction = ReferenceField("FunctionDecl")
    ddecl = ReferenceField("DataDeclaration")
    fdeclarator = ReferenceField("FunctionDeclarator", constructor_variable=0)
    declaredType = ReferenceField("Type", constructor_variable=0)
    undeclaredVariables = ReferenceField("Env", constructor_variable=0)
    baseLabels = ReferenceField("Env", constructor_variable=0)
    scopedLabels = ReferenceField("Env", constructor_variable=0)
    currentLoop = ReferenceField("Statement", constructor_variable=0)
    nlocals = IntField(constructor_variable=0)


# Used as the AST node for implicit declarations.
class ImplicitDecl(BasicASTNode):
    superclass = Declaration
    # IDENT points to the identifier node that implicitly declared the function.
    ident = ReferenceField("Identifier")


# Declaration of DECLARATOR ASM_STMT ATTRIBUTES = ARG1.
class VariableDecl(BasicASTNode):
    superclass = Declaration
    declarator = ReferenceField("Declarator")
    # ATTRIBUTES is a list. ASM_STMT is optional (GCC specific).
    attributes = ReferenceListField("Attribute")
    # ARG1 is an optional initialiser.
    arg1 = ReferenceField("Expression")
    asmStmt = ReferenceField("AsmStmt")
    # DDECL points to the declaration for this item.
    ddecl = ReferenceField("DataDeclaration", constructor_variable=0)
    # DECLARED_TYPE is the type in this declaration (which may be different
    # than that in DDECL->TYPE).
    declaredType = ReferenceField("Type", constructor_variable=0)
    # FORWARD is true for parameters that are forward declarations.
    forward = BoolField(constructor_variable=0)


# Declaration of field DECLARATOR ATTRIBUTES : ARG1.
class FieldDecl(BasicASTNode):
    superclass = Declaration
    declarator = ReferenceField("Declarator")
    # QUALIFIERS and ATTRIBUTEES are lists.
    attributes = ReferenceListField("Attribute")
    # ARG1 is an optional bitfield specifier.
    arg1 = ReferenceField("Expression")
    # TYPE_CHECKED is set to true once it has been checked that this field is
    # of network type (inside network structures).
    typeChecked = BoolField(constructor_variable=0)
    # FDECL is this field's declaration.
    fdecl = ReferenceField("FieldDeclaration", constructor_variable=0)


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
    ddecl = ReferenceField("DataDeclaration")


# typeof ARG1
class TypeofExpr(BasicASTNode):
    superclass = TypeElement
    arg1 = ReferenceField("Expression")


# typeof(ASTTYPE)
class TypeofType(BasicASTNode):
    superclass = TypeElement
    asttype = ReferenceField("AstType")


# base type for gcc and nesc attributes.
class Attribute(BasicASTNode):
    superclass = TypeElement
    word1 = ReferenceField("Word")


# The (gcc) attribute WORD1(ARGS).
class GccAttribute(BasicASTNode):
    superclass = Attribute
    # args can be empty, and may not be semantically valid.
    args = ReferenceListField("Expression")


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
# TDECL points to the internal declaration node for this type
class TagRef(BasicASTNode):
    superclass = TypeElement
    word1 = ReferenceField("Word")
    attributes = ReferenceListField("Attribute")
    fields = ReferenceListField("Declaration")
    defined = BoolField()
    tdecl = ReferenceField("TagDeclaration", constructor_variable=0)


# A struct
class StructRef(BasicASTNode):
    superclass = TagRef


# An attribute definition.
class AttributeRef(BasicASTNode):
    superclass = TagRef


# A union
class UnionRef(BasicASTNode):
    superclass = TagRef


# An enum
class EnumRef(BasicASTNode):
    superclass = TagRef


#==============================================================================
#                                 Declarators
#==============================================================================

#   The last DECLARATOR in a chain is:
#   NULL_NODE in absolute declarations
#   an identifier_decl otherwise

# A common supertype for function/pointer/array declarator which includes
# the nested DECLARATOR
class NestedDeclarator(BasicASTNode):
    superclass = Declarator
    declarator = ReferenceField("Declarator")


# Function declarator DECLARATOR(PARMS). PARMS is a list of declarations.
# ENV is the environment for parms
# GPARMS is the list of declarations of generic parameters (commands, events only)
# RETURN_TYPE (optional) contains an overridden return type from nesdoc
class FunctionDeclarator(BasicASTNode):
    superclass = NestedDeclarator
    parms = ReferenceListField("Declaration")
    gparms = ReferenceListField("Declaration")
    qualifiers = ReferenceListField("TypeElement")
    env = ReferenceField("Environment")
    returnType = ReferenceField("AstType", constructor_variable=0)


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
    arg1 = ReferenceField("Expression")


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


# { ID_LABELS DECLS STMTS }. The ID_LABELS are GCC-specific. ID_LABELS, DECLS,
# STMTS are lists
# ENV is the environment for the block
class CompoundStmt(BasicASTNode):
    superclass = Statement
    idLabels = ReferenceListField("IdLabel")
    decls = ReferenceListField("Declaration")
    stmts = ReferenceListField("Statement")
    env = ReferenceField("Environment")


# IF (CONDITION) STMT1 ELSE STMT2. STMT2 is optional
class IfStmt(BasicASTNode):
    superclass = Statement
    condition = ReferenceField("Expression")
    stmt1 = ReferenceField("Statement")
    stmt2 = ReferenceField("Statement")


# LABEL: STMT
class LabeledStmt(BasicASTNode):
    superclass = Statement
    label = ReferenceField("Label")
    stmt = ReferenceField("Statement")


# EXPR;
class ExpressionStmt(BasicASTNode):
    superclass = Statement
    arg1 = ReferenceField("Expression")


# Basic type for all conditional statements
class ConditionalStmt(BasicASTNode):
    superclass = Statement
    condition = ReferenceField("Expression")
    stmt = ReferenceField("Statement")


class WhileStmt(BasicASTNode):
    superclass = ConditionalStmt


class DoWhileStmt(BasicASTNode):
    superclass = ConditionalStmt


# SWITCH (CONDITION) STMT.
# NEXT_LABEL points to the switches first label
class SwitchStmt(BasicASTNode):
    superclass = ConditionalStmt
    nextLabel = ReferenceField("Label", constructor_variable=0)


# FOR (ARG1; ARG2; ARG3) STMT. ARG1, ARG2, ARG3 are optional
class ForStmt(BasicASTNode):
    superclass = Statement
    arg1 = ReferenceField("Expression")
    arg2 = ReferenceField("Expression")
    arg3 = ReferenceField("Expression")
    stmt = ReferenceField("Statement")


class BreakStmt(BasicASTNode):
    superclass = Statement


class ContinueStmt(BasicASTNode):
    superclass = Statement


# RETURN ARG1. ARG1 is optional
class ReturnStmt(BasicASTNode):
    superclass = Statement
    arg1 = ReferenceField("Expression")


class GotoStmt(BasicASTNode):
    superclass = Statement
    idLabel = ReferenceField("IdLabel")


# GOTO *ARG1 (GCC)
class ComputedGotoStmt(BasicASTNode):
    superclass = Statement
    arg1 = ReferenceField("Expression")


class EmptyStmt(BasicASTNode):
    superclass = Statement


#==============================================================================
#                                  Expressions
#==============================================================================

# Placeholder for erroneous expressions.
class ErrorExpr(BasicASTNode):
    superclass = Expression


class Unary(BasicASTNode):
    superclass = Expression
    argument = ReferenceField("Expression")


class Binary(BasicASTNode):
    superclass = Expression
    leftArgument = ReferenceField("Expression")
    rightArgument = ReferenceField("Expression")


# A comma separated list of expressions.
class Comma(BasicASTNode):
    superclass = Expression
    expressions = ReferenceListField("Expression")


class SizeofType(BasicASTNode):
    superclass = Expression
    asttype = ReferenceField("AstType")


class AlignofType(BasicASTNode):
    superclass = Expression
    asttype = ReferenceField("AstType")


class LabelAddress(BasicASTNode):
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
    superclass = Expression
    condition = ReferenceField("Expression")
    onTrueExp = ReferenceField("Expression")
    onFalseExp = ReferenceField("Expression")


class Identifier(BasicASTNode):
    superclass = Expression
    name = StringField()
    ddecl = ReferenceField("DataDeclaration", constructor_variable=0)


class CompoundExpr(BasicASTNode):
    superclass = Expression
    statement = ReferenceField("Statement")


# ARG1(ARGS). ARGS is a list of expressions
# If VA_ARG_CALL is non-null, this is actually a call to the pseudo-function
# __builtin_va_arg(args, va_arg_call) (where va_arg_call is a type). In 
# this case arg1 is a dummy identifier.
# CALL_KIND is one of normal_call, post_task, command_call or event_signal.
class FunctionCall(BasicASTNode):
    superclass = Expression
    function = ReferenceField("Expression")
    arguments = ReferenceListField("Expression")
    vaArgCall = ReferenceField("AstType")
    callKind = ReferenceField("NescCallKind")


# XXX: originally ArrayRef is BinaryExp
class ArrayRef(BasicASTNode):
    superclass = Expression
    array = ReferenceField("Expression")
    index = ReferenceListField("Expression")


class FieldRef(BasicASTNode):
    superclass = Unary
    name = StringField()
    fdecl = ReferenceField("FieldDeclaration", constructor_variable=0)


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
    temp1 = ReferenceField("DataDeclaration", constructor_variable=0)
    temp2 = ReferenceField("DataDeclaration", constructor_variable=0)


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
    temp1 = ReferenceField("DataDeclaration", constructor_variable=0)


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
    superclass = Expression
    arguments = ReferenceListField("Expression")


class InitSpecific(BasicASTNode):
    superclass = Expression
    designator = ReferenceListField("Designator")
    initExpr = ReferenceField("Expression")


class Designator(BasicASTNode):
    superclass = Node


class DesignateField(BasicASTNode):
    superclass = Designator
    name = StringField()


class DesignateIndex(BasicASTNode):
    superclass = Designator
    arg1 = ReferenceField("Expression")
    arg2 = ReferenceField("Expression")


#==============================================================================
#                                   Constants
#==============================================================================

# A constant represented as in its unparsed lexical form CSTRING. These
# appear in the AST.
class LexicalCst(BasicASTNode):
    superclass = Expression
    string = StringField()


# A single lexical string - a sequence of these gets concatenated to
# form a string. The source form of the constant can be found in
# CSTRING.
class StringCst(BasicASTNode):
    superclass = LexicalCst


# A list of STRINGS forming a single string constant.
# DDECL is the magic_string declaration for this string.
class StringAst(BasicASTNode):
    superclass = Expression
    strings = ReferenceListField("StringCst")
    ddecl = ReferenceField("DataDeclaration", constructor_variable=0)


#==============================================================================
#                                     Labels
#==============================================================================

class IdLabel(BasicASTNode):
    superclass = Label
    id = StringField()
    ldecl = ReferenceField("LabelDeclaration", constructor_variable=0)


class CaseLabel(BasicASTNode):
    superclass = Label
    arg1 = ReferenceField("Expression")
    arg2 = ReferenceField("Expression")


class DefaultLabel(BasicASTNode):
    superclass = Label


#==============================================================================
#                                 Miscellaneous
#==============================================================================

class Word(BasicASTNode):
    superclass = Node
    name = StringField()


class AsmOperand(BasicASTNode):
    superclass = Node
    word1 = ReferenceField("Word")
    string = ReferenceField("StringAst")
    arg1 = ReferenceField("Expression")


#==============================================================================
#                                nesc extensions
#==============================================================================

#==============================================================================
#                          The different kinds of files
#==============================================================================

class NescDecl(BasicASTNode):
    superclass = Declaration
    name = ReferenceField("Word")
    attributes = ReferenceListField("Attribute")
    cdecl = ReferenceField("NescDeclaration", constructor_variable=0)


class Interface(BasicASTNode):
    superclass = NescDecl
    parameters = ReferenceListField("Declaration")
    decls = ReferenceListField("Declaration")


class Component(BasicASTNode):
    superclass = NescDecl
    isAbstract = BoolField()
    parms = ReferenceListField("Declaration")
    decls = ReferenceListField("Declaration")
    implementation = ReferenceField("Implementation")


class Configuration(BasicASTNode):
    superclass = Component


class Module(BasicASTNode):
    superclass = Component


class BinaryComponent(BasicASTNode):
    superclass = Component


class Implementation(BasicASTNode):
    superclass = Node
    ienv = ReferenceField("Environment", constructor_variable=0)
    cdecl = ReferenceField("NescDeclaration", constructor_variable=0)


class ConfigurationImpl(BasicASTNode):
    superclass = Implementation
    decls = ReferenceListField("Declaration")


class ModuleImpl(BasicASTNode):
    superclass = Implementation
    decls = ReferenceListField("Declaration")


class BinaryComponentImpl(BasicASTNode):
    superclass = Implementation


# This node was not present in original compiler.
class ComponentsUses(BasicASTNode):
    superclass = Declaration
    components = ReferenceListField("ComponentRef")


#==============================================================================
#                           Component definition types
#==============================================================================

class RpInterface(BasicASTNode):
    superclass = Declaration
    declarations = ReferenceListField("Declaration")


class RequiresInterface(BasicASTNode):
    superclass = RpInterface


class ProvidesInterface(BasicASTNode):
    superclass = RpInterface


class InterfaceRef(BasicASTNode):
    superclass = Declaration
    name = ReferenceField("Word")
    arguments = ReferenceListField("Expression")
    alias = ReferenceField("Word")
    genericParameters = ReferenceListField("Declaration")
    attributes = ReferenceListField("Attribute")
    ddecl = ReferenceField("DataDeclaration", constructor_variable=0)


class ComponentRef(BasicASTNode):
    superclass = Declaration
    name = ReferenceField("Word")
    alias = ReferenceField("Word")
    isAbstract = BoolField()
    arguments = ReferenceListField("Expression")
    cdecl = ReferenceField("NescDeclaration", constructor_variable=0)


class Connection(BasicASTNode):
    superclass = Declaration
    endPoint1 = ReferenceField("EndPoint")
    endPoint2 = ReferenceField("EndPoint")


# requires/provides
class RpConnection(BasicASTNode):
    superclass = Connection


class EqConnection(BasicASTNode):
    superclass = Connection


class EndPoint(BasicASTNode):
    superclass = Node
    ids = ReferenceListField("ParameterisedIdentifier")


class ParameterisedIdentifier(BasicASTNode):
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
    parms = ReferenceListField("Declaration")


# ARG1[ARGS]. ARGS is a list of expressions, ARG1 is a generic function
class GenericCall(BasicASTNode):
    superclass = Expression
    arg1 = ReferenceField("Expression")
    args = ReferenceListField("Expression")


class InterfaceRefDeclarator(BasicASTNode):
    superclass = NestedDeclarator
    word1 = ReferenceField("Word")


class InterfaceDeref(BasicASTNode):
    superclass = Unary
    name = StringField()
    ddecl = ReferenceField("DataDeclaration")


class ComponentDeref(BasicASTNode):
    superclass = Unary
    name = StringField()
    ddecl = ReferenceField("DataDeclaration")


class ComponentTyperef(BasicASTNode):
    superclass = Typename
    name = StringField()


class AtomicStmt(BasicASTNode):
    superclass = Statement
    stmt = ReferenceField("Statement")


class NxStructRef(BasicASTNode):
    superclass = StructRef


class NxUnionRef(BasicASTNode):
    superclass = UnionRef


class NescAttribute(BasicASTNode):
    superclass = Attribute
    value = ReferenceField("Expression")
    tdecl = ReferenceField("TagDeclaration", constructor_variable=0)


# A target-specific extension represented internally as a gcc-style attribute.
class TargetAttribute(BasicASTNode):
    superclass = GccAttribute


#==============================================================================
#                      Types for the polymorphic extensions
#==============================================================================

class TypeParmDecl(BasicASTNode):
    superclass = Declaration
    name = StringField()
    attributes = ReferenceListField("Attribute")
    ddecl = ReferenceField("DataDeclaration", constructor_variable=0)


class TypeArgument(BasicASTNode):
    superclass = Expression
    asttype = ReferenceField("AstType")

#==============================================================================
#==============================================================================
if __name__ == "__main__":
    generate_code(DST_LANGUAGE.JAVA, "pl/edu/mimuw/nesc/ast/gen")
