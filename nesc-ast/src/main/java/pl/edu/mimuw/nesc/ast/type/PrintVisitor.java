package pl.edu.mimuw.nesc.ast.type;

import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import com.google.common.base.Optional;
import java.util.List;

/**
 * Visitor that creates the textual representation of a type. The second
 * argument of the visit methods shall be <code>true</code> if and only if the
 * type that is visited is an argument for a pointer type visited right before.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class PrintVisitor implements TypeVisitor<Void, Boolean> {
    /**
     * Constants used to create the textual representation of a type.
     */
    private static final String QUALIFIER_CONST = "const";
    private static final String QUALIFIER_VOLATILE = "volatile";
    private static final String QUALIFIER_RESTRICT = "restrict";
    private static final String TYPE_CHAR = "char";
    private static final String TYPE_SIGNED_CHAR = "signed char";
    private static final String TYPE_SHORT = "short";
    private static final String TYPE_INT = "int";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_LONG_LONG = "long long";
    private static final String TYPE_UNSIGNED_CHAR = "unsigned char";
    private static final String TYPE_UNSIGNED_SHORT = "unsigned short";
    private static final String TYPE_UNSIGNED_INT = "unsigned int";
    private static final String TYPE_UNSIGNED_LONG = "unsigned long";
    private static final String TYPE_UNSIGNED_LONG_LONG = "unsigned long long";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_DOUBLE = "double";
    private static final String TYPE_LONG_DOUBLE = "long double";
    private static final String TYPE_VOID = "void";
    private static final String TYPE_ERROR = "<error>";
    private static final String TAG_ENUMERATED_TYPE = "enum";
    private static final String TAG_STRUCTURE = "struct";
    private static final String TAG_UNION = "union";
    private static final String TAG_EXTERNAL_STRUCTURE = "nx_struct";
    private static final String TAG_EXTERNAL_UNION = "nx_union";
    private static final String NESC_INTERFACE = "interface";
    private static final String NESC_COMPONENT = "component";
    private static final String TYPE_DEFINITION = "typename";
    private static final char SYMBOL_ASTERISK = '*';
    private static final char SYMBOL_LBRACKET = '[';
    private static final char SYMBOL_RBRACKET = ']';
    private static final char SYMBOL_LPAR = '(';
    private static final char SYMBOL_RPAR = ')';
    private static final char SYMBOL_LANGLE = '<';
    private static final char SYMBOL_RANGLE = '>';
    private static final String SYMBOL_ELLIPSIS = "...";

    /**
     * String builder that accumulates the textual representation of a type
     * that is built.
     */
    private final StringBuilder builder = new StringBuilder();

    /**
     * @return The textual representation of the type that has been visited.
     */
    final String get() {
        return builder.toString().trim();
    }

    @Override
    public Void visit(ArrayType type, Boolean pointerTo) {
        if (pointerTo) {
            lift();
        }

        builder.append(SYMBOL_LBRACKET);
        builder.append(SYMBOL_RBRACKET);

        return type.getElementType().accept(this, false);
    }

    @Override
    public Void visit(CharType type, Boolean pointerTo) {
        return prependTypename(TYPE_CHAR, type);
    }

    @Override
    public Void visit(ComponentType type, Boolean pointerTo) {
        builder.append(NESC_COMPONENT);
        builder.append(' ');
        builder.append(type.getComponentName());
        return null;
    }

    @Override
    public Void visit(DoubleType type, Boolean pointerTo) {
        return prependTypename(TYPE_DOUBLE, type);
    }

    @Override
    public Void visit(EnumeratedType type, Boolean pointerTo) {
        return prependTypename(getTagTypename(TAG_ENUMERATED_TYPE, type.getEnumDeclaration()), type);
    }

    @Override
    public Void visit(ExternalStructureType type, Boolean pointerTo) {
        return prependTypename(getTagTypename(TAG_EXTERNAL_STRUCTURE, type.getDeclaration()), type);
    }

    @Override
    public Void visit(ExternalUnionType type, Boolean pointerTo) {
        return prependTypename(getTagTypename(TAG_EXTERNAL_UNION, type.getDeclaration()), type);
    }

    @Override
    public Void visit(FloatType type, Boolean pointerTo) {
        return prependTypename(TYPE_FLOAT, type);
    }

    @Override
    public Void visit(FunctionType type, Boolean pointerTo) {
        if (pointerTo) {
            lift();
        }

        builder.append(SYMBOL_LPAR);
        appendTypesList(type.getArgumentsTypes(), type.getVariableArguments());
        builder.append(SYMBOL_RPAR);
        type.getReturnType().accept(this, false);

        return null;
    }

    @Override
    public Void visit(InterfaceType type, Boolean pointerTo) {
        builder.append(NESC_INTERFACE);
        builder.append(' ');
        builder.append(type.getInterfaceName());

        final Optional<List<Optional<Type>>> maybeTypeParams = type.getTypeParameters();
        if (maybeTypeParams.isPresent()) {
            builder.append(SYMBOL_LANGLE);
            appendTypesList(maybeTypeParams.get(), false);
            builder.append(SYMBOL_RANGLE);
        }

        return null;
    }

    @Override
    public Void visit(IntType type, Boolean pointerTo) {
        return prependTypename(TYPE_INT, type);
    }

    @Override
    public Void visit(LongDoubleType type, Boolean pointerTo) {
        return prependTypename(TYPE_LONG_DOUBLE, type);
    }

    @Override
    public Void visit(LongLongType type, Boolean pointerTo) {
        return prependTypename(TYPE_LONG_LONG, type);
    }

    @Override
    public Void visit(LongType type, Boolean pointerTo) {
        return prependTypename(TYPE_LONG, type);
    }

    @Override
    public Void visit(PointerType type, Boolean pointerTo) {
        if (type.isRestrictQualified()) {
            prepend(QUALIFIER_RESTRICT + " ");
        }

        prependQualifiers(type);
        prepend(SYMBOL_ASTERISK);
        type.getReferencedType().accept(this, true);

        return null;
    }

    @Override
    public Void visit(ShortType type, Boolean pointerTo) {
        return prependTypename(TYPE_SHORT, type);
    }

    @Override
    public Void visit(SignedCharType type, Boolean pointerTo) {
        return prependTypename(TYPE_SIGNED_CHAR, type);
    }

    @Override
    public Void visit(StructureType type, Boolean pointerTo) {
        return prependTypename(getTagTypename(TAG_STRUCTURE, type.getDeclaration()), type);
    }

    @Override
    public Void visit(TypeDefinitionType type, Boolean pointerTo) {
        prepend(TYPE_DEFINITION);
        return null;
    }

    @Override
    public Void visit(UnionType type, Boolean pointerTo) {
        return prependTypename(getTagTypename(TAG_UNION, type.getDeclaration()), type);
    }

    @Override
    public Void visit(UnsignedCharType type, Boolean pointerTo) {
        return prependTypename(TYPE_UNSIGNED_CHAR, type);
    }

    @Override
    public Void visit(UnsignedIntType type, Boolean pointerTo) {
        return prependTypename(TYPE_UNSIGNED_INT, type);
    }

    @Override
    public Void visit(UnsignedLongLongType type, Boolean pointerTo) {
        return prependTypename(TYPE_UNSIGNED_LONG_LONG, type);
    }

    @Override
    public Void visit(UnsignedLongType type, Boolean pointerTo) {
        return prependTypename(TYPE_UNSIGNED_LONG, type);
    }

    @Override
    public Void visit(UnsignedShortType type, Boolean pointerTo) {
        return prependTypename(TYPE_UNSIGNED_SHORT, type);
    }

    @Override
    public Void visit(VoidType type, Boolean pointerTo) {
        return prependTypename(TYPE_VOID, type);
    }

    @Override
    public Void visit(UnknownType type, Boolean pointerTo) {
        return prependTypename(type.getName(), type);
    }

    @Override
    public Void visit(UnknownArithmeticType type, Boolean pointerTo) {
        return prependTypename(type.getName(), type);
    }

    @Override
    public Void visit(UnknownIntegerType type, Boolean pointerTo) {
        return prependTypename(type.getName(), type);
    }

    private <A> void prepend(A value) {
        builder.insert(0, value);
    }

    private void prependQualifiers(Type type) {
        if (type.isVolatileQualified()) {
            prepend(QUALIFIER_VOLATILE + " ");
        }
        if (type.isConstQualified()) {
            prepend(QUALIFIER_CONST + " ");
        }
    }

    private Void prependTypename(String typename, Type type) {
        prepend(typename + " ");
        prependQualifiers(type);
        return null;
    }

    private void appendTypesList(List<Optional<Type>> types, boolean ellipsisPresent) {
        boolean first = true;

        for (Optional<Type> type : types) {
            if (!first) {
                builder.append(", ");
            }

            first = false;
            final String toAppend =    type.isPresent()
                                     ? type.get().toString()
                                     : TYPE_ERROR;
            builder.append(toAppend);
        }

        if (ellipsisPresent) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(SYMBOL_ELLIPSIS);
        }
    }

    private void lift() {
        prepend(SYMBOL_LPAR);
        builder.append(SYMBOL_RPAR);
    }

    private static String getTagTypename(String tagKeyword, TagDeclaration tagDecl) {
        final StringBuilder builder = new StringBuilder();
        builder.append(tagKeyword);
        builder.append(' ');

        // Append the name of the tag
        final Optional<String> maybeName = tagDecl.getName();
        if (maybeName.isPresent()) {
            builder.append(maybeName.get());
        } else {
            builder.append(SYMBOL_LANGLE);
            builder.append("anonymous ");
            builder.append(tagKeyword);
            builder.append(SYMBOL_RANGLE);
        }

        return builder.toString();
    }
}
