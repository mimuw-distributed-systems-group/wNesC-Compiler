package pl.edu.mimuw.nesc.constexpr.value.factory;

import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;
import pl.edu.mimuw.nesc.constexpr.value.type.DoubleConstantType;
import pl.edu.mimuw.nesc.constexpr.value.type.FloatConstantType;
import pl.edu.mimuw.nesc.constexpr.value.type.SignedIntegerConstantType;
import pl.edu.mimuw.nesc.constexpr.value.type.UnsignedIntegerConstantType;
import pl.edu.mimuw.nesc.type.*;
import pl.edu.mimuw.nesc.typelayout.UniversalTypeLayoutCalculator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A class that produces constant types from types from the types
 * hierarchy:</p>
 * <code>pl.edu.mimuw.nesc.type</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ConstantTypeFactory {
    /**
     * ABI that will be used for producing types.
     */
    private final ABI abi;

    /**
     * Visitor that visits types and creates the instances of constant types.
     */
    private final TypeVisitor<ConstantType, Void> creatorVisitor = new CreatorVisitor();

    public ConstantTypeFactory(ABI abi) {
        checkNotNull(abi, "ABI cannot be null");
        this.abi = abi;
    }

    /**
     * Create a new constant type that corresponds to the given type from
     * <code>pl.edu.mimuw.nesc.type</code> hierarchy.
     *
     * @param type Type to reflect in the constant types hierarchy.
     * @return Constant type that corresponds to the given type (it is not
     *         necessarily new instance because some constant types are
     *         singletons).
     * @throws IllegalArgumentException The given type cannot be represented
     *                                  in the constant types hierarchy.
     */
    public ConstantType newConstantType(Type type) {
        checkNotNull(type, "type cannot be null");
        return type.accept(creatorVisitor, null);
    }

    public SignedIntegerConstantType newSignedIntegerType(int bitsCount) {
        return new SignedIntegerConstantType(bitsCount);
    }

    public SignedIntegerConstantType newSignedIntegerType(SignedIntegerType type) {
        return newSignedIntegerType(retrieveBitsCount(type));
    }

    public UnsignedIntegerConstantType newUnsignedIntegerType(int bitsCount) {
        return new UnsignedIntegerConstantType(bitsCount);
    }

    public UnsignedIntegerConstantType newUnsignedIntegerType(UnsignedIntegerType type) {
        return newUnsignedIntegerType(retrieveBitsCount(type));
    }

    private int retrieveBitsCount(Type type) {
        return new UniversalTypeLayoutCalculator(this.abi, type).calculate().getSize() * 8;
    }

    /**
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class CreatorVisitor implements TypeVisitor<ConstantType, Void> {

        @Override
        public ConstantType visit(CharType type, Void arg) {
            final int bitsCount = ConstantTypeFactory.this.retrieveBitsCount(type);

            return ConstantTypeFactory.this.abi.getChar().isSigned()
                    ? newSignedIntegerType(bitsCount)
                    : newUnsignedIntegerType(bitsCount);
        }

        @Override
        public ConstantType visit(SignedCharType type, Void arg) {
            return newSignedIntegerType(type);
        }

        @Override
        public ConstantType visit(ShortType type, Void arg) {
            return newSignedIntegerType(type);
        }

        @Override
        public ConstantType visit(IntType type, Void arg) {
            return newSignedIntegerType(type);
        }

        @Override
        public ConstantType visit(LongType type, Void arg) {
            return newSignedIntegerType(type);
        }

        @Override
        public ConstantType visit(LongLongType type, Void arg) {
            return newSignedIntegerType(type);
        }

        @Override
        public ConstantType visit(UnsignedCharType type, Void arg) {
            return newUnsignedIntegerType(type);
        }

        @Override
        public ConstantType visit(UnsignedShortType type, Void arg) {
            return newUnsignedIntegerType(type);
        }

        @Override
        public ConstantType visit(UnsignedIntType type, Void arg) {
            return newUnsignedIntegerType(type);
        }

        @Override
        public ConstantType visit(UnsignedLongType type, Void arg) {
            return newUnsignedIntegerType(type);
        }

        @Override
        public ConstantType visit(UnsignedLongLongType type, Void arg) {
            return newUnsignedIntegerType(type);
        }

        @Override
        public ConstantType visit(EnumeratedType type, Void arg) {
            // FIXME
            throw new UnsupportedOperationException("creating constant type for an enumerated type is currently unimplemented");
        }

        @Override
        public ConstantType visit(FloatType type, Void arg) {
            return FloatConstantType.getInstance();
        }

        @Override
        public ConstantType visit(DoubleType type, Void arg) {
            return DoubleConstantType.getInstance();
        }

        @Override
        public ConstantType visit(LongDoubleType type, Void arg) {
            return DoubleConstantType.getInstance();
        }

        @Override
        public ConstantType visit(VoidType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for type 'void'");
        }

        @Override
        public ConstantType visit(PointerType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for a pointer type");
        }

        @Override
        public ConstantType visit(ArrayType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for an array type");
        }

        @Override
        public ConstantType visit(FunctionType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for a function type");
        }

        @Override
        public ConstantType visit(StructureType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for a structure type");
        }

        @Override
        public ConstantType visit(UnionType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for a union type");
        }

        @Override
        public ConstantType visit(ExternalStructureType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for an external structure type");
        }

        @Override
        public ConstantType visit(ExternalUnionType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for an external union type");
        }

        @Override
        public ConstantType visit(UnknownType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for an unknown type");
        }

        @Override
        public ConstantType visit(UnknownArithmeticType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for an unknown arithmetic type");
        }

        @Override
        public ConstantType visit(UnknownIntegerType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for an unknown integer type");
        }

        @Override
        public ConstantType visit(TypeDefinitionType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for an artificial type");
        }

        @Override
        public ConstantType visit(InterfaceType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for an interface type");
        }

        @Override
        public ConstantType visit(ComponentType type, Void arg) {
            throw new IllegalArgumentException("cannot create constant type for a component type");
        }
    }
}
