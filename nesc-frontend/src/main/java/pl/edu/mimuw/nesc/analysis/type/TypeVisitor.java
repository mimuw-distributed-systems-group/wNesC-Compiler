package pl.edu.mimuw.nesc.analysis.type;

/**
 * Interface that follows the Visitor design pattern for the types class
 * hierarchy. It contains methods only for classes that are not abstract.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface TypeVisitor<R, A> {
    R visit(ArrayType type, A arg);
    R visit(CharType type, A arg);
    R visit(ComponentType type, A arg);
    R visit(DoubleType type, A arg);
    R visit(EnumeratedType type, A arg);
    R visit(ExternalStructureType type, A arg);
    R visit(ExternalUnionType type, A arg);
    R visit(FloatType type, A arg);
    R visit(FunctionType type, A arg);
    R visit(InterfaceType type, A arg);
    R visit(IntType type, A arg);
    R visit(LongDoubleType type, A arg);
    R visit(LongLongType type, A arg);
    R visit(LongType type, A arg);
    R visit(PointerType type, A arg);
    R visit(ShortType type, A arg);
    R visit(SignedCharType type, A arg);
    R visit(StructureType type, A arg);
    R visit(UnionType type, A arg);
    R visit(UnsignedCharType type, A arg);
    R visit(UnsignedIntType type, A arg);
    R visit(UnsignedLongLongType type, A arg);
    R visit(UnsignedLongType type, A arg);
    R visit(UnsignedShortType type, A arg);
    R visit(VoidType type, A arg);
}
