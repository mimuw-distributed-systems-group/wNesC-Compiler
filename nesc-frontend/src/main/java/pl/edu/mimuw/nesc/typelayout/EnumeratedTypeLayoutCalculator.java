package pl.edu.mimuw.nesc.typelayout;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.constexpr.ConstExprInterpreter;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.IntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.EnumDeclaration;
import pl.edu.mimuw.nesc.type.EnumeratedType;
import pl.edu.mimuw.nesc.type.IntType;
import pl.edu.mimuw.nesc.type.IntegerType;
import pl.edu.mimuw.nesc.type.LongLongType;
import pl.edu.mimuw.nesc.type.LongType;
import pl.edu.mimuw.nesc.type.ShortType;
import pl.edu.mimuw.nesc.type.SignedCharType;
import pl.edu.mimuw.nesc.type.SignedIntegerType;
import pl.edu.mimuw.nesc.type.UnsignedCharType;
import pl.edu.mimuw.nesc.type.UnsignedIntType;
import pl.edu.mimuw.nesc.type.UnsignedIntegerType;
import pl.edu.mimuw.nesc.type.UnsignedLongLongType;
import pl.edu.mimuw.nesc.type.UnsignedLongType;
import pl.edu.mimuw.nesc.type.UnsignedShortType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EnumeratedTypeLayoutCalculator implements TypeLayoutCalculator {
    /**
     * Type whose layout will be calculated.
     */
    private final EnumeratedType type;

    /**
     * ABI with information necessary fo calculation of the layout.
     */
    private final ABI abi;

    /**
     * Interpreter used for evaluation of constant expressions.
     */
    private final ConstExprInterpreter interpreter;

    /**
     * The result of calculating layout.
     */
    private Optional<TypeLayout> layout = Optional.absent();

    /**
     * Minimum and maximum values of the computed enumeration constants.
     */
    private BigInteger minimumValue;
    private BigInteger maximumValue;

    /**
     * Ranges and types for determining the perfect compatible type.
     */
    private ImmutableList<Range<BigInteger>> ranges;
    private IntegerType[] types;

    /**
     * Type with the smallest size that is capable of representing all constants
     * of the enumerated type.
     */
    private IntegerType perfectCompatibleType;

    public EnumeratedTypeLayoutCalculator(ABI abi, EnumeratedType type) {
        checkNotNull(abi, "ABI cannot be null");
        checkNotNull(type, "the enumerated type cannot be null");
        this.abi = abi;
        this.type = type;
        this.interpreter = new ConstExprInterpreter(abi);
    }

    @Override
    public TypeLayout calculate() {
        final EnumDeclaration declaration = type.getEnumDeclaration();

        if (layout.isPresent()) {
            return layout.get();
        } else if (declaration.hasLayout()) {
            layout = Optional.of(new TypeLayout(declaration.getSize(), declaration.getAlignment()));
            return layout.get();
        }

        computeConstantsValues();
        determineConstantsRange();
        prepareForTypeDetermination();
        determinePerfectCompatibleType();
        determineCompatibleType();

        layout = Optional.of(new UniversalTypeLayoutCalculator(abi, declaration.getCompatibleType()).calculate());
        declaration.setLayout(layout.get().getSize(), layout.get().getAlignment());

        return layout.get();
    }

    private void computeConstantsValues() {
        final EnumDeclaration declaration = type.getEnumDeclaration();
        BigInteger precedingValue = BigInteger.valueOf(-1L);

        for (ConstantDeclaration constant : declaration.getEnumerators().get()) {
            if (constant.getEnumerator().getValue().isPresent()) {
                final ConstantValue value = interpreter.evaluate(constant.getEnumerator().getValue().get());
                if (value.getType().getType() != ConstantType.Type.SIGNED_INTEGER
                        && value.getType().getType() != ConstantType.Type.UNSIGNED_INTEGER) {
                    throw new RuntimeException("value of enumeration constant does not evaluate to an integer");
                }
                final BigInteger valueBigInt = ((IntegerConstantValue<?>) value).getValue();
                constant.setValue(valueBigInt);
            } else {
                constant.setValue(precedingValue.add(BigInteger.ONE));
            }

            precedingValue = constant.getValue().get();
        }
    }

    private void determineConstantsRange() {
        final EnumDeclaration declaration = type.getEnumDeclaration();
        final ConstantDeclaration firstConstant = declaration.getEnumerators().get().get(0);

        // Initialize the range
        minimumValue = firstConstant.getValue().get();
        maximumValue = minimumValue;

        // Determine the exact range
        for (ConstantDeclaration constant : declaration.getEnumerators().get()) {
            minimumValue = minimumValue.min(constant.getValue().get());
            maximumValue = maximumValue.max(constant.getValue().get());
        }
    }

    private void prepareForTypeDetermination() {
        if (minimumValue.signum() < 0) {
            prepareSignedIntegerTypes();
        } else {
            prepareUnsignedIntegerTypes();
        }
    }

    private void prepareSignedIntegerTypes() {
        ranges = ImmutableList.of(
            abi.getChar().getSignedRange(),
            abi.getShort().getSignedRange(),
            abi.getInt().getSignedRange(),
            abi.getLong().getSignedRange(),
            abi.getLongLong().getSignedRange()
        );
        types = new SignedIntegerType[] {
            new SignedCharType(),
            new ShortType(),
            new IntType(),
            new LongType(),
            new LongLongType()
        };
    }

    private void prepareUnsignedIntegerTypes() {
        ranges = ImmutableList.of(
            abi.getChar().getUnsignedRange(),
            abi.getShort().getUnsignedRange(),
            abi.getInt().getUnsignedRange(),
            abi.getLong().getUnsignedRange(),
            abi.getLongLong().getUnsignedRange()
        );
        types = new UnsignedIntegerType[] {
            new UnsignedCharType(),
            new UnsignedShortType(),
            new UnsignedIntType(),
            new UnsignedLongType(),
            new UnsignedLongLongType()
        };
    }

    private void determinePerfectCompatibleType() {
        for (int i = 0; i < ranges.size(); ++i) {
            final Range<BigInteger> range = ranges.get(i);

            if (range.contains(minimumValue) && range.contains(maximumValue)) {
                perfectCompatibleType = types[i];
                return;
            }
        }

        throw new RuntimeException("no type can represent all enumeration constants");
    }

    private void determineCompatibleType() {
        final EnumDeclaration declaration = type.getEnumDeclaration();
        final Range<BigInteger> intRange = abi.getInt().getSignedRange();

        final IntegerType compatibleType = intRange.contains(minimumValue)
                    && intRange.contains(maximumValue)
                ? new IntType()
                : perfectCompatibleType;

        declaration.setCompatibleType(compatibleType);
    }
}
