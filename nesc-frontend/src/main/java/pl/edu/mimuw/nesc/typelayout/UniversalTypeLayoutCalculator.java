package pl.edu.mimuw.nesc.typelayout;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.constexpr.ConstExprInterpreter;
import pl.edu.mimuw.nesc.constexpr.Interpreter;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.IntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;
import pl.edu.mimuw.nesc.type.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class responsible for determining information about types. It allows
 * getting information about size of a type or its alignment.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class UniversalTypeLayoutCalculator implements TypeLayoutCalculator {
    /**
     * ABI with necessary information about types.
     */
    private final ABI abi;

    /**
     * Type that the layout will be computed of.
     */
    private final Type type;

    /**
     * Interpreter for constant expressions.
     */
    private final Interpreter interpreter;

    /**
     * The calculated layout.
     */
    private volatile Optional<TypeLayout> calculatedLayout = Optional.absent();

    /**
     * Initializes this object to calculate the layout of the given type in
     * given ABI.
     */
    public UniversalTypeLayoutCalculator(ABI abi, Type type) {
        checkNotNull(abi, "ABI cannot be null");
        checkNotNull(type, "type cannot be null");

        this.abi = abi;
        this.type = type;
        this.interpreter = new ConstExprInterpreter(abi);
    }

    @Override
    public TypeLayout calculate() {
        if (this.calculatedLayout.isPresent()) {
            return this.calculatedLayout.get();
        }

        this.calculatedLayout = Optional.of(this.type.accept(new CalculatorVisitor(), null));
        return this.calculatedLayout.get();
    }

    /**
     * The visitor that actually computes the layout.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class CalculatorVisitor implements TypeVisitor<TypeLayout, Void> {
        @Override
        public TypeLayout visit(CharType type, Void arg) {
            // size and alignment of 'char' type are always 1
            return new TypeLayout(1, 1);
        }

        @Override
        public TypeLayout visit(SignedCharType type, Void arg) {
            // size and alignment of 'signed char' type are always 1
            return new TypeLayout(1, 1);
        }

        @Override
        public TypeLayout visit(UnsignedCharType type, Void arg) {
            // size and alignment of 'unsigned char' type are always 1
            return new TypeLayout(1, 1);
        }

        @Override
        public TypeLayout visit(ShortType type, Void arg) {
            return computeShortLayout();
        }

        @Override
        public TypeLayout visit(UnsignedShortType type, Void arg) {
            return computeShortLayout();
        }

        @Override
        public TypeLayout visit(IntType type, Void arg) {
            return computeIntLayout();
        }

        @Override
        public TypeLayout visit(UnsignedIntType type, Void arg) {
            return computeIntLayout();
        }

        @Override
        public TypeLayout visit(LongType type, Void arg) {
            return computeLongLayout();
        }

        @Override
        public TypeLayout visit(UnsignedLongType type, Void arg) {
            return computeLongLayout();
        }

        @Override
        public TypeLayout visit(LongLongType type, Void arg) {
            return computeLongLongLayout();
        }

        @Override
        public TypeLayout visit(UnsignedLongLongType type, Void arg) {
            return computeLongLongLayout();
        }

        @Override
        public TypeLayout visit(EnumeratedType type, Void arg) {
            return new EnumeratedTypeLayoutCalculator(UniversalTypeLayoutCalculator.this.abi, type)
                    .calculate();
        }

        @Override
        public TypeLayout visit(FloatType type, Void arg) {
            return new TypeLayout(abi.getFloat().getSize(), abi.getFloat().getAlignment());
        }

        @Override
        public TypeLayout visit(DoubleType type, Void arg) {
            return new TypeLayout(abi.getDouble().getSize(), abi.getDouble().getAlignment());
        }

        @Override
        public TypeLayout visit(LongDoubleType type, Void arg) {
            return new TypeLayout(abi.getLongDouble().getSize(), abi.getLongDouble().getAlignment());
        }

        @Override
        public TypeLayout visit(VoidType type, Void arg) {
            throw new RuntimeException("calculating layout for 'void' type");
        }
        @Override
        public TypeLayout visit(ArrayType type, Void arg) {
            // FIXME compilation error if array size < 0

            checkArgument(type.getSize().isPresent(),
                    "cannot compute the layout of an incomplete array type");

            // Compute the size of the array
            final ConstantValue arraySizeValue = interpreter.evaluate(type.getSize().get());
            checkState(arraySizeValue.getType().getType() == ConstantType.Type.SIGNED_INTEGER
                        || arraySizeValue.getType().getType() == ConstantType.Type.UNSIGNED_INTEGER,
                        "array size expression evaluated to non-integer type '" + arraySizeValue.getType().getType() + "'");
            final IntegerConstantValue<?> arraySize = (IntegerConstantValue<?>) arraySizeValue;

            // Zero size arrays are allowed to support the GCC extension
            checkState(arraySize.getValue().signum() >= 0, "size of an array cannot be negative: %s",
                    arraySize.getValue());

            // Compute the layout of the element type
            final TypeLayout elementTypeLayout = new UniversalTypeLayoutCalculator(UniversalTypeLayoutCalculator.this.abi,
                    type.getElementType()).calculate();

            return new TypeLayout(arraySize.getValue().intValue() * elementTypeLayout.getSize(),
                    elementTypeLayout.getAlignment());
        }

        @Override
        public TypeLayout visit(PointerType type, Void arg) {
            return new TypeLayout(abi.getPointerType().getSize(), abi.getPointerType().getAlignment());
        }

        @Override
        public TypeLayout visit(FunctionType type, Void arg) {
            throw new RuntimeException("calculating layout for a function type");
        }

        @Override
        public TypeLayout visit(StructureType type, Void arg) {
            return computeFieldTagLayout(type);
        }

        @Override
        public TypeLayout visit(UnionType type, Void arg) {
            return computeFieldTagLayout(type);
        }

        @Override
        public TypeLayout visit(ExternalStructureType type, Void arg) {
            return computeFieldTagLayout(type);
        }

        @Override
        public TypeLayout visit(ExternalUnionType type, Void arg) {
            return computeFieldTagLayout(type);
        }

        @Override
        public TypeLayout visit(UnknownType type, Void arg) {
            throw new RuntimeException("calculating layout of an unknown type");
        }

        @Override
        public TypeLayout visit(UnknownArithmeticType type, Void arg) {
            throw new RuntimeException("calculating layout of an unknown type");
        }

        @Override
        public TypeLayout visit(UnknownIntegerType type, Void arg) {
            throw new RuntimeException("calculating layout of an unknown type");
        }

        @Override
        public TypeLayout visit(TypeDefinitionType type, Void arg) {
            throw new RuntimeException("calculating layout for a type definition");
        }

        @Override
        public TypeLayout visit(InterfaceType type, Void arg) {
            throw new RuntimeException("calculating layout for an interface type");
        }

        @Override
        public TypeLayout visit(ComponentType type, Void arg) {
            throw new RuntimeException("calculating layout for a component type");
        }

        private TypeLayout computeShortLayout() {
            return new TypeLayout(abi.getShort().getSize(), abi.getShort().getAlignment());
        }

        private TypeLayout computeIntLayout() {
            return new TypeLayout(abi.getInt().getSize(), abi.getInt().getAlignment());
        }

        private TypeLayout computeLongLayout() {
            return new TypeLayout(abi.getLong().getSize(), abi.getLong().getAlignment());
        }

        private TypeLayout computeLongLongLayout() {
            return new TypeLayout(abi.getLongLong().getSize(), abi.getLongLong().getAlignment());
        }

        private TypeLayout computeFieldTagLayout(FieldTagType<?> fieldTagType) {
            return new FieldTagTypeLayoutCalculator(UniversalTypeLayoutCalculator.this.abi, fieldTagType)
                    .calculate();
        }
    }
}
