package pl.edu.mimuw.nesc.finalreduce;

import com.google.common.collect.Iterables;
import java.util.Iterator;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FieldDecl;
import pl.edu.mimuw.nesc.ast.gen.NxStructRef;
import pl.edu.mimuw.nesc.ast.gen.NxUnionRef;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.predicates.PackedAttributePredicate;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.FieldElement;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.TreeElement;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.typelayout.FieldTagTypeLayoutCalculator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class responsible for handling bit-fields in external unions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ExternalUnionTransformer {
    /**
     * Unmangled name of filler fields added to external tags.
     */
    private static final String FILLER_FIELD_NAME = "__nx__filler";

    /**
     * Reference to the union that will be transformed.
     */
    private final NxUnionRef nxUnionRef;

    /**
     * Declaration object from the union reference.
     */
    private final UnionDeclaration declaration;

    /**
     * ABI of the project.
     */
    private final ABI abi;

    /**
     * Instance of the element visitor that will be used.
     */
    private final NxUnionElementVisitor elementVisitor;

    /**
     * Name mangler that will be used to create names for the filler fields.
     */
    private final NameMangler nameMangler;

    ExternalUnionTransformer(NxUnionRef nxUnionRef, ABI abi, NameMangler nameMangler) {
        checkNotNull(nxUnionRef, "reference to external union cannot be null");
        checkNotNull(abi, "ABI cannot be null");
        checkNotNull(nameMangler, "name mangler cannot be null");

        this.nxUnionRef = nxUnionRef;
        this.declaration = nxUnionRef.getDeclaration();
        this.abi = abi;
        this.elementVisitor = new NxUnionElementVisitor();
        this.nameMangler = nameMangler;
    }

    /**
     * Performs the transformation of external union and all nested anonymous
     * unions and structures.
     */
    public void transform() {
        if (nxUnionRef.getSemantics() == null) {
            throw new IllegalStateException("semantics in a tag reference is null");
        } else if (nxUnionRef.getSemantics() != StructSemantics.DEFINITION
                || declaration.isTransformed()) {
            return;
        }

        calculateLayout();
        computeMaximumFieldsSizes();
        removeBitFields();
        addFillerField();
        addPackedAttribute();
        raiseTransformedFlag();
    }

    private void calculateLayout() {
        /* Ensure that the layout of each field is present in declaration
           objects. */
        new FieldTagTypeLayoutCalculator(this.abi, this.declaration.getType(false, false))
                .calculate();
    }

    private void computeMaximumFieldsSizes() {
        /* Compute maximum sizes of bit-fields and the other fields and
           recursively transform anonymous structures and unions. */
        for (TreeElement element : declaration.getStructure().get()) {
            element.accept(elementVisitor, null);
        }
    }

    private void removeBitFields() {
        final Iterator<Declaration> astOuterDeclIt = nxUnionRef.getFields().iterator();

        while (astOuterDeclIt.hasNext()) {
            final DataDecl astOuterDecl = (DataDecl) astOuterDeclIt.next();
            final Iterator<Declaration> astInnerDeclIt =astOuterDecl.getDeclarations().iterator();
            boolean elementsRemoved = false;

            while (astInnerDeclIt.hasNext()) {
                final FieldDecl astInnerDecl = (FieldDecl) astInnerDeclIt.next();
                if (astInnerDecl.getDeclaration().isBitField()) {
                    astInnerDeclIt.remove();
                    elementsRemoved = true;
                }
            }

            if (astOuterDecl.getDeclarations().isEmpty() && elementsRemoved) {
                astOuterDeclIt.remove();
            }
        }
    }

    private void addFillerField() {
        // Add a filler field if is it necessary
        if (elementVisitor.maxBitFieldSizeInBits > elementVisitor.maxNormalFieldSizeInBits) {
            final String fieldName = nameMangler.mangle(FILLER_FIELD_NAME);
            final int fieldSize = VariousUtils.alignNumber(elementVisitor.maxBitFieldSizeInBits, 8) / 8;
            nxUnionRef.getFields().add(AstUtils.newFillerField(fieldName, fieldSize));
        }
    }

    private void addPackedAttribute() {
        if (!Iterables.any(nxUnionRef.getAttributes(), new PackedAttributePredicate())) {
            nxUnionRef.getAttributes().add(AstUtils.newPackedAttribute());
        }
    }

    private void raiseTransformedFlag() {
        declaration.transformed();
    }

    /**
     * Visitor that facilities transformation of external union definitions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class NxUnionElementVisitor implements TreeElement.Visitor<Void, Void> {
        private int maxBitFieldSizeInBits = 0;
        private int maxNormalFieldSizeInBits = 0;

        @Override
        public Void visit(FieldElement fieldElement, Void arg) {
            final FieldDeclaration declaration = fieldElement.getFieldDeclaration();

            if (declaration.isBitField()) {
                maxBitFieldSizeInBits = Math.max(maxBitFieldSizeInBits, declaration.getSizeInBits());
            } else {
                maxNormalFieldSizeInBits = Math.max(maxNormalFieldSizeInBits, declaration.getSizeInBits());
            }

            return null;
        }

        @Override
        public Void visit(BlockElement blockElement, Void arg) {
            maxNormalFieldSizeInBits = Math.max(maxNormalFieldSizeInBits,
                    blockElement.getDeclaration().getSize() * 8);

            final TagRef anonymousTag = blockElement.getDeclaration().getAstNode();

            switch (blockElement.getType()) {
                case EXTERNAL_STRUCTURE:
                    new ExternalStructureTransformer((NxStructRef) anonymousTag, abi, nameMangler)
                            .transform();
                    break;
                case EXTERNAL_UNION:
                    new ExternalUnionTransformer((NxUnionRef) anonymousTag, abi, nameMangler)
                            .transform();
                    break;
                default:
                    throw new IllegalStateException("unexpected block type in an external union '"
                            + blockElement.getType() + "'");
            }

            return null;
        }
    }
}
