package pl.edu.mimuw.nesc.typelayout;

import com.google.common.base.Optional;
import java.util.Iterator;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.abi.Endianness;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.constexpr.ConstExprInterpreter;
import pl.edu.mimuw.nesc.constexpr.value.ConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.IntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.FieldTagDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.FieldElement;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.TreeElement;
import pl.edu.mimuw.nesc.type.ArithmeticType;
import pl.edu.mimuw.nesc.type.ArrayType;
import pl.edu.mimuw.nesc.type.ExternalStructureType;
import pl.edu.mimuw.nesc.type.FieldTagType;
import pl.edu.mimuw.nesc.type.StructureType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class responsible for computing layout of field tag types.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FieldTagTypeLayoutCalculator implements TypeLayoutCalculator {
    /**
     * Constant that contains the number of bits in a byte.
     */
    private static final int BITS_PER_BYTE = 8;

    private final ABI abi;
    private final FieldTagType<?> type;
    private final ConstExprInterpreter interpreter;
    private Optional<TypeLayout> calculatedLayout = Optional.absent();

    /**
     * Initializes this calculator to compute the layout of given field tag
     * type in given ABI.
     *
     * @param abi ABI that will be used.
     * @param type Type to determine the layout of.
     */
    public FieldTagTypeLayoutCalculator(ABI abi, FieldTagType<?> type) {
        checkNotNull(abi, "ABI cannot be null");
        checkNotNull(type, "type cannot be null");
        checkArgument(type.getDeclaration().getStructure().isPresent(),
                "calculating layout for an incomplete type");

        this.abi = abi;
        this.type = type;
        this.interpreter = new ConstExprInterpreter(abi);
    }

    @Override
    public TypeLayout calculate() {
        if (this.calculatedLayout.isPresent()) {
            return this.calculatedLayout.get();
        }

        if (!this.type.getDeclaration().hasLayout()) {
            _calculate();
            this.type.getDeclaration().setLayout(calculatedLayout.get().getSize(),
                    calculatedLayout.get().getAlignment());
        } else {
            this.calculatedLayout = Optional.of(new TypeLayout(this.type.getDeclaration().getSize(),
                    this.type.getDeclaration().getAlignment()));
        }

        return this.calculatedLayout.get();
    }

    private void _calculate() {
        int offsetInBits = 0;
        int sizeInBits = 0;
        int alignmentInBits = BITS_PER_BYTE;
        Optional<Endianness> previousEndianness = Optional.absent();
        final Iterator<TreeElement> structureIt = type.getDeclaration().getStructure().get().iterator();

        while (structureIt.hasNext()) {
            final TreeElement element = structureIt.next();
            final boolean last = !structureIt.hasNext();
            final ElementDispatchVisitor dispatchVisitor = new ElementDispatchVisitor(offsetInBits,
                    sizeInBits, alignmentInBits, previousEndianness, last);

            final TinyCalculator tinyCalculator = element.accept(dispatchVisitor, null);
            tinyCalculator.calculate();
            offsetInBits = tinyCalculator.getNewOffsetInBits();
            sizeInBits = tinyCalculator.getNewSizeInBits();
            alignmentInBits = tinyCalculator.getNewAlignmentInBits();
            previousEndianness = dispatchVisitor.getCurrentEndianness();
        }

        if (!this.type.isExternal()) {
            alignmentInBits = VariousUtils.lcm(alignmentInBits,
                    this.abi.getFieldTagType().getMinimumAlignment() * BITS_PER_BYTE);
        }
        sizeInBits = VariousUtils.alignNumber(sizeInBits, alignmentInBits);

        checkState(alignmentInBits % BITS_PER_BYTE == 0,
                "alignment of the field tag type is not a multiple of a byte");
        checkState(sizeInBits % BITS_PER_BYTE == 0,
                "size of the field tag type is not a multiple of a byte");

        this.calculatedLayout = Optional.of(new TypeLayout(sizeInBits / BITS_PER_BYTE,
                alignmentInBits / BITS_PER_BYTE));
    }

    private TinyCalculator calculateFieldElement(FieldDeclaration field,
                int startOffsetInBits, int startSizeInBits, int startAlignmentInBits,
                Optional<Endianness> previousEndianness, boolean isLast) {
        if (!field.isBitField()) {
            // Check if the field is a flexible array member
            if (field.getType().get().isArrayType()) {
                final ArrayType arrayType = (ArrayType) field.getType().get();
                if (!arrayType.isOfKnownSize() && isLast) {
                    final TypeLayout elementTypeLayout = new UniversalTypeLayoutCalculator(this.abi,
                            arrayType.getElementType()).calculate();
                    return new FlexibleArrayMemberTinyCalculator(startOffsetInBits, startSizeInBits,
                            startAlignmentInBits, elementTypeLayout, field);
                }
            }

            final TypeLayout fieldTypeLayout = new UniversalTypeLayoutCalculator(this.abi,
                    field.getType().get()).calculate();

            return new RegularFieldTinyCalculator(startOffsetInBits,
                    startSizeInBits, startAlignmentInBits, fieldTypeLayout, field);
        } else {
            final TypeLayout fieldTypeLayout = new UniversalTypeLayoutCalculator(this.abi,
                    field.getType().get()).calculate();

            final ConstantValue widthValue = interpreter.evaluate(field.getAstField().getBitfield().get());
            if (widthValue.getType().getType() != ConstantType.Type.UNSIGNED_INTEGER
                    && widthValue.getType().getType() != ConstantType.Type.SIGNED_INTEGER) {
                throw new RuntimeException("width of the bit-field is not an integer constant");
            }
            final int width = ((IntegerConstantValue<?>) widthValue).getValue().intValue();
            checkArgument(width >= 0, "the expression of the bit-field width evaluates to a negative number");

            if (width != 0) {
                return new NonZeroWidthBitFieldTinyCalculator(startOffsetInBits,
                        startSizeInBits, startAlignmentInBits, fieldTypeLayout,
                        field, width, previousEndianness);
            } else {
                return new ZeroWidthBitFieldTinyCalculator(startOffsetInBits,
                        startSizeInBits, startAlignmentInBits, fieldTypeLayout,
                        field);
            }
        }
    }

    private TinyCalculator calculateBlockElement(FieldTagDeclaration<?> tagDeclaration,
            int startOffsetInBits, int startSizeInBits, int startAlignmentInBits) {

        final TypeLayout anonymousTagLayout = new FieldTagTypeLayoutCalculator(this.abi,
                tagDeclaration.getType(false, false)).calculate();
        return new BlockElementTinyCalculator(startOffsetInBits, startSizeInBits,
                startAlignmentInBits, anonymousTagLayout, tagDeclaration);
    }

    /**
     * Visitor used for determining the method that will compute layout of the
     * visited element of a tag type.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class ElementDispatchVisitor implements TreeElement.Visitor<TinyCalculator, Void> {
        private final int startOffsetInBits, startSizeInBits, startAlignmentInBits;
        private final Optional<Endianness> previousEndianness;
        private final boolean last;
        private Optional<Endianness> currentEndianness;

        private ElementDispatchVisitor(int startOffsetInBits, int startSizeInBits,
                int startAlignmentInBits, Optional<Endianness> previousEndianness,
                boolean last) {
            this.startOffsetInBits = startOffsetInBits;
            this.startSizeInBits = startSizeInBits;
            this.startAlignmentInBits = startAlignmentInBits;
            this.previousEndianness = previousEndianness;
            this.last = last;
        }

        @Override
        public TinyCalculator visit(FieldElement fieldElement, Void arg) {
            if (fieldElement.getFieldDeclaration().isBitField()
                    && FieldTagTypeLayoutCalculator.this.type.isExternal()) {
                final ArithmeticType bitFieldType = (ArithmeticType) fieldElement
                        .getFieldDeclaration().getType().get();
                this.currentEndianness = Optional.of(bitFieldType.getExternalScheme().get().getEndianness());
            } else {
                this.currentEndianness = Optional.absent();
            }

            return calculateFieldElement(fieldElement.getFieldDeclaration(),
                    this.startOffsetInBits, this.startSizeInBits,
                    this.startAlignmentInBits, previousEndianness, last);
        }

        @Override
        public TinyCalculator visit(BlockElement blockElement, Void arg) {
            this.currentEndianness = Optional.absent();
            return calculateBlockElement(blockElement.getDeclaration(),
                    this.startOffsetInBits, this.startSizeInBits,
                    this.startAlignmentInBits);
        }

        private Optional<Endianness> getCurrentEndianness() {
            return this.currentEndianness;
        }
    };

    /**
     * Interface of a layout calculator for a single field.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface TinyCalculator {
        void calculate();
        int getNewOffsetInBits();
        int getNewSizeInBits();
        int getNewAlignmentInBits();
    }

    /**
     * Skeletal implementation of a tiny calculator.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private abstract class AbstractTinyCalculator implements TinyCalculator {
        protected final int startOffsetInBits;
        protected final int startSizeInBits;
        protected final int startAlignmentInBits;
        protected final TypeLayout fieldTypeLayout;

        private Optional<Integer> newOffsetInBits;
        private Optional<Integer> newSizeInBits;
        private Optional<Integer> newAlignmentInBits;

        private AbstractTinyCalculator(int startOffsetInBits, int startSizeInBits,
                int startAlignmentInBits, TypeLayout fieldTypeLayout) {

            this.startOffsetInBits = startOffsetInBits;
            this.startSizeInBits = startSizeInBits;
            this.startAlignmentInBits = startAlignmentInBits;
            this.fieldTypeLayout = fieldTypeLayout;

            this.newOffsetInBits = Optional.absent();
            this.newSizeInBits = Optional.absent();
            this.newAlignmentInBits = Optional.absent();
        }

        protected void align(int alignmentInBits) {
            this.newOffsetInBits = Optional.of(VariousUtils.alignNumber(this.startOffsetInBits,
                    alignmentInBits));
        }

        protected void update(int fieldSizeInBits, int fieldAlignmentInBits) {
            if (FieldTagTypeLayoutCalculator.this.type instanceof StructureType
                    || FieldTagTypeLayoutCalculator.this.type instanceof ExternalStructureType) {
                this.newOffsetInBits = Optional.of(newOffsetInBits.or(startOffsetInBits) + fieldSizeInBits);
                this.newSizeInBits = this.newOffsetInBits;
            } else {
                this.newOffsetInBits = Optional.of(0);
                this.newSizeInBits = Optional.of(Math.max(this.startSizeInBits, fieldSizeInBits));
            }

            this.newAlignmentInBits = Optional.of(VariousUtils.lcm(this.startAlignmentInBits,
                    fieldAlignmentInBits));
        }

        @Override
        public final int getNewOffsetInBits() {
            checkState(newOffsetInBits.isPresent(), "the new offset has not been set");
            return newOffsetInBits.get();
        }

        @Override
        public final int getNewSizeInBits() {
            checkState(newSizeInBits.isPresent(), "the new size has not been set");
            return newSizeInBits.get();
        }

        @Override
        public final int getNewAlignmentInBits() {
            checkState(newAlignmentInBits.isPresent(), "the new alignment has not been set");
            return newAlignmentInBits.get();
        }
    }

    private abstract class FieldElementTinyCalculator extends AbstractTinyCalculator {
        protected final FieldDeclaration declaration;

        private FieldElementTinyCalculator(int startOffsetInBits, int startSizeInBits,
                int startAlignmentInBits, TypeLayout fieldTypeLayout,
                FieldDeclaration declaration) {
            super(startOffsetInBits, startSizeInBits, startAlignmentInBits, fieldTypeLayout);
            this.declaration = declaration;
        }
    }

    private final class RegularFieldTinyCalculator extends FieldElementTinyCalculator {
        private RegularFieldTinyCalculator(int startOffsetInBits, int startSizeInBits,
                    int startAlignmentInBits, TypeLayout fieldTypeLayout,
                    FieldDeclaration declaration) {

            super(startOffsetInBits, startSizeInBits, startAlignmentInBits,
                    fieldTypeLayout, declaration);
        }

        @Override
        public void calculate() {
            final int sizeInBits = fieldTypeLayout.getSize() * BITS_PER_BYTE;
            final int alignmentInBits = fieldTypeLayout.getAlignment() * BITS_PER_BYTE;

            align(alignmentInBits);
            declaration.setLayout(getNewOffsetInBits(), sizeInBits, alignmentInBits);
            update(sizeInBits, alignmentInBits);
        }
    }

    private final class ZeroWidthBitFieldTinyCalculator extends FieldElementTinyCalculator {
        private ZeroWidthBitFieldTinyCalculator(int startOffsetInBits, int startSizeInBits,
                    int startAlignmentInBits, TypeLayout fieldTypeLayout,
                    FieldDeclaration declaration) {

            super(startOffsetInBits, startSizeInBits, startAlignmentInBits,
                    fieldTypeLayout, declaration);
        }

        @Override
        public void calculate() {
            if (FieldTagTypeLayoutCalculator.this.abi.getFieldTagType().bitFieldTypeMatters()
                    || FieldTagTypeLayoutCalculator.this.type.isExternal()) {
                final int alignmentInBits = fieldTypeLayout.getAlignment() * BITS_PER_BYTE;
                align(alignmentInBits);
                declaration.setLayout(getNewOffsetInBits(), 0, alignmentInBits);
                update(0, 1);
            } else {
                final int alignmentInBits = FieldTagTypeLayoutCalculator.this.abi.getFieldTagType().getEmptyBitFieldAlignmentInBits();
                align(alignmentInBits);
                declaration.setLayout(getNewOffsetInBits(), 0, alignmentInBits);
                update(0, alignmentInBits);
            }
        }
    }

    private final class NonZeroWidthBitFieldTinyCalculator extends FieldElementTinyCalculator {
        private final int bitFieldWidth;

        /**
         * Endianness of the previous bit-field (it the type the layout is
         * calculated for is external and the previous field was a bit-field).
         */
        private final Optional<Endianness> previousEndianness;

        private NonZeroWidthBitFieldTinyCalculator(int startOffsetInBits, int startSizeInBits,
                    int startAlignmentInBits, TypeLayout fieldTypeLayout,
                    FieldDeclaration declaration, int bitFieldWidth,
                    Optional<Endianness> previousEndianness) {

            super(startOffsetInBits, startSizeInBits, startAlignmentInBits,
                    fieldTypeLayout, declaration);
            this.bitFieldWidth = bitFieldWidth;
            this.previousEndianness = previousEndianness;
        }

        @Override
        public void calculate() {
            final int alignmentInBits = fieldTypeLayout.getAlignment() * BITS_PER_BYTE;
            final int sizeInBits = fieldTypeLayout.getSize() * BITS_PER_BYTE;

            if (FieldTagTypeLayoutCalculator.this.abi.getFieldTagType().bitFieldTypeMatters()
                    && !FieldTagTypeLayoutCalculator.this.type.isExternal()) {
                /* The bit-field is placed in the next storage unit if it could not
                   be read fully by reading a value of the bit-field type from
                   a properly aligned address for the bit-field type if it was
                   placed in the current storage unit. */
                final int firstBlockWithoutBitField = (this.startOffsetInBits + bitFieldWidth + alignmentInBits - 1)
                        / alignmentInBits;
                final int firstBlockWithBitField = this.startOffsetInBits / alignmentInBits;
                final int bitFieldBlocksCount = firstBlockWithoutBitField - firstBlockWithBitField;
                final int typeBlocksCount = sizeInBits / alignmentInBits;

                if (bitFieldBlocksCount > typeBlocksCount) {
                    align(alignmentInBits);
                }
                declaration.setLayout(getNewOffsetInBits(), bitFieldWidth, alignmentInBits);
                update(bitFieldWidth, alignmentInBits);
            } else {
                if (FieldTagTypeLayoutCalculator.this.type.isExternal()) {
                    final ArithmeticType fieldType = (ArithmeticType) declaration.getType().get();
                    final Endianness currentEndianness = fieldType.getExternalScheme().get().getEndianness();

                    if (!previousEndianness.isPresent() || previousEndianness.get() != currentEndianness) {
                        align(BITS_PER_BYTE);
                    }
                }

                // The bit-field is not aligned
                declaration.setLayout(startOffsetInBits, bitFieldWidth, BITS_PER_BYTE);
                update(bitFieldWidth, BITS_PER_BYTE);
            }
        }
    }

    /**
     * Calculator for an anonymous structure or an anonymous union.
     */
    private final class BlockElementTinyCalculator extends AbstractTinyCalculator {
        /**
         * Declaration of an anonymous structure or an anonymous union.
         */
        private final FieldTagDeclaration<?> declaration;

        private BlockElementTinyCalculator(int startOffsetInBits, int startSizeInBits,
                    int startAlignmentInBits, TypeLayout fieldTypeLayout,
                    FieldTagDeclaration<?> declaration) {

            super(startOffsetInBits, startSizeInBits, startAlignmentInBits, fieldTypeLayout);
            this.declaration = declaration;
        }

        @Override
        public void calculate() {
            align(declaration.getAlignment() * BITS_PER_BYTE);
            final int nestedFieldsOffset = getNewOffsetInBits();

            for (TreeElement nestedElement : declaration.getStructure().get()) {
                for (FieldDeclaration nestedField : nestedElement) {
                    nestedField.increaseOffset(nestedFieldsOffset);
                }
            }

            update(declaration.getSize() * BITS_PER_BYTE,
                    declaration.getAlignment() * BITS_PER_BYTE);
        }
    }

    private final class FlexibleArrayMemberTinyCalculator extends FieldElementTinyCalculator {
        private FlexibleArrayMemberTinyCalculator(int startOffsetInBits, int startSizeInBits,
                    int startAlignmentInBits, TypeLayout arrayElementTypeLayout,
                    FieldDeclaration declaration) {
            super(startOffsetInBits, startSizeInBits, startAlignmentInBits, arrayElementTypeLayout, declaration);
        }

        @Override
        public void calculate() {
            final int alignmentInBits = fieldTypeLayout.getAlignment() * BITS_PER_BYTE;
            final int structAlignmentInBits = VariousUtils.lcm(alignmentInBits, startAlignmentInBits);

            align(structAlignmentInBits);
            declaration.setLayout(getNewOffsetInBits(), 0, structAlignmentInBits);
            update(0, structAlignmentInBits);
        }
    }
}
