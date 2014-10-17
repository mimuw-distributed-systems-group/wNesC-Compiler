package pl.edu.mimuw.nesc.ast.type;

/**
 * A visitor whose purpose is to be a base class for convenient creation of
 * visitors that don't have to visit all types (it is not an error if a type is
 * skipped). All methods in this class return <code>null</code> and can be
 * overridden if necessary.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class NullTypeVisitor<R, A> implements TypeVisitor<R, A> {
    @Override
    public R visit(ArrayType type, A arg) {
        return null;
    }

    @Override
    public R visit(CharType type, A arg) {
        return null;
    }

    @Override
    public R visit(ComponentType type, A arg) {
        return null;
    }

    @Override
    public R visit(DoubleType type, A arg) {
        return null;
    }

    @Override
    public R visit(EnumeratedType type, A arg) {
        return null;
    }

    @Override
    public R visit(ExternalStructureType type, A arg) {
        return null;
    }

    @Override
    public R visit(ExternalUnionType type, A arg) {
        return null;
    }

    @Override
    public R visit(FloatType type, A arg) {
        return null;
    }

    @Override
    public R visit(FunctionType type, A arg) {
        return null;
    }

    @Override
    public R visit(InterfaceType type, A arg) {
        return null;
    }

    @Override
    public R visit(IntType type, A arg) {
        return null;
    }

    @Override
    public R visit(LongDoubleType type, A arg) {
        return null;
    }

    @Override
    public R visit(LongLongType type, A arg) {
        return null;
    }

    @Override
    public R visit(LongType type, A arg) {
        return null;
    }

    @Override
    public R visit(PointerType type, A arg) {
        return null;
    }

    @Override
    public R visit(ShortType type, A arg) {
        return null;
    }

    @Override
    public R visit(SignedCharType type, A arg) {
        return null;
    }

    @Override
    public R visit(StructureType type, A arg) {
        return null;
    }

    @Override
    public R visit(TypeDefinitionType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnionType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnsignedCharType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnsignedIntType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnsignedLongLongType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnsignedLongType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnsignedShortType type, A arg) {
        return null;
    }

    @Override
    public R visit(VoidType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnknownType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnknownArithmeticType type, A arg) {
        return null;
    }

    @Override
    public R visit(UnknownIntegerType type, A arg) {
        return null;
    }
}
