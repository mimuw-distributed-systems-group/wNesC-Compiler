package pl.edu.mimuw.nesc.type;

import pl.edu.mimuw.nesc.ast.gen.AstType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A class that represents an artificial type. An artificial type is a type
 * that is not a type of an object in the C type system. Artificial types are as
 * follows:</p>
 * <ul>
 *     <li>type of a type definition</li>
 *     <li>type of an interface reference</li>
 *     <li>type of a component reference</li>
 * </ul>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class ArtificialType implements Type {
    @Override
    public final boolean isArithmetic() {
        return false;
    }

    @Override
    public final boolean isIntegerType() {
        return false;
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isFloatingType() {
        return false;
    }

    @Override
    public final boolean isRealType() {
        return false;
    }

    @Override
    public final boolean isGeneralizedRealType() {
        return false;
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isGeneralizedScalarType() {
        return false;
    }

    @Override
    public final boolean isVoid() {
        return false;
    }

    @Override
    public final boolean isDerivedType() {
        return false;
    }

    @Override
    public final boolean isFieldTagType() {
        return false;
    }

    @Override
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final boolean isArrayType() {
        return false;
    }

    @Override
    public final boolean isObjectType() {
        return false;
    }

    @Override
    public final boolean isFunctionType() {
        return false;
    }

    @Override
    public final boolean isUnknownType() {
        return false;
    }

    @Override
    public final boolean isUnknownArithmeticType() {
        return false;
    }

    @Override
    public final boolean isUnknownIntegerType() {
        return false;
    }

    @Override
    public final boolean isGeneralizedArithmeticType() {
        return false;
    }

    @Override
    public final boolean isGeneralizedIntegerType() {
        return false;
    }

    @Override
    public final boolean isModifiable() {
        return false;
    }

    @Override
    public final boolean isConstQualified() {
        return false;
    }

    @Override
    public final boolean isVolatileQualified() {
        return false;
    }

    @Override
    public final boolean hasAllQualifiers(Type otherType) {
        checkNotNull(otherType, "the other type cannot be null");
        return false;
    }

    @Override
    public final boolean isComplete() {
        return false;
    }

    @Override
    public final boolean isExternal() {
        return false;
    }

    @Override
    public final boolean isExternalBaseType() {
        return false;
    }

    @Override
    public final boolean maybeExternal() {
        return false;
    }

    @Override
    public final boolean isArtificialType() {
        return true;
    }

    @Override
    public final Type addQualifiers(boolean constQualified,
                                    boolean volatileQualified, boolean restrictQualified) {
        throw new UnsupportedOperationException("adding qualifiers to an artificial type");
    }

    @Override
    public final Type addQualifiers(Type otherType) {
        checkNotNull(otherType, "the other type cannot be null");
        throw new UnsupportedOperationException("adding qualifiers to an artificial type");
    }

    @Override
    public final Type removeQualifiers(boolean removeConst,
                                       boolean removeVolatile, boolean removeRestrict) {
        throw new UnsupportedOperationException("removing qualifiers from an artificial type");
    }

    @Override
    public final Type removeQualifiers() {
        return removeQualifiers(true, true, true);
    }

    @Override
    public boolean isCompatibleWith(Type type) {
        throw new UnsupportedOperationException("checking the compatibility with an artificial type");
    }

    @Override
    public final Type promote() {
        throw new UnsupportedOperationException("promoting an artificial type");
    }

    @Override
    public final Type decay() {
        throw new UnsupportedOperationException("decaying an artificial type");
    }

    @Override
    public final String toString() {
        final PrintVisitor visitor = new PrintVisitor();
        accept(visitor, false);
        return visitor.get();
    }

    @Override
    public final AstType toAstType() {
        throw new UnsupportedOperationException("cannot get an AST node representing an artificial type");
    }
}
