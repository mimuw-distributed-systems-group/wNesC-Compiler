package pl.edu.mimuw.nesc.finalreduce;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.abi.Endianness;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FieldDecl;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.NxStructRef;
import pl.edu.mimuw.nesc.ast.gen.NxUnionRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.predicates.AttributePredicates;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.StructDeclaration;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.type.IntegerType;
import pl.edu.mimuw.nesc.typelayout.FieldTagTypeLayoutCalculator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Transformer responsible for changing external structures to support
 * bit-fields inside of them.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ExternalStructureTransformer {
    /**
     * Unmangled name of the filler field to use.
     */
    private static final String FILLER_FIELD_NAME = "__nx__filler";

    /**
     * Reference to the structure that will be transformed.
     */
    private final NxStructRef nxStructRef;

    /**
     * Declaration object for the transformed structure.
     */
    private final StructDeclaration declaration;

    /**
     * ABI of the application.
     */
    private final ABI abi;

    /**
     * Name mangler used for mangling names for the filler fields.
     */
    private final NameMangler nameMangler;

    ExternalStructureTransformer(NxStructRef nxStructRef, ABI abi, NameMangler nameMangler) {
        checkNotNull(nxStructRef, "reference to external structure cannot be null");
        checkNotNull(abi, "ABI cannot be null");
        checkNotNull(nameMangler, "name mangler cannot be null");

        this.nxStructRef = nxStructRef;
        this.declaration = nxStructRef.getDeclaration();
        this.abi = abi;
        this.nameMangler = nameMangler;
    }

    /**
     * Performs the transformation of the external structure that is packing
     * bit-fields to filler fields.
     */
    public void transform() {
        if (nxStructRef.getSemantics() == null) {
            throw new IllegalStateException("an external structure with null semantics");
        } else if (nxStructRef.getSemantics() != StructSemantics.DEFINITION
                || declaration.isTransformed()) {
            return;
        }

        calculateLayout();
        separateFields();
        packBitFields();
        addPackedAttribute();
        raiseTransformedFlag();
    }

    private void calculateLayout() {
        new FieldTagTypeLayoutCalculator(abi, declaration.getType(false, false))
                .calculate();
    }

    private void separateFields() {
        final ListIterator<Declaration> astOuterDeclIt = nxStructRef.getFields().listIterator();

        while (astOuterDeclIt.hasNext()) {
            final DataDecl dataDecl = (DataDecl) astOuterDeclIt.next();
            boolean first = true;

            if (dataDecl.getDeclarations().size() < 2) {
                continue;
            }

            for (Declaration astInnerDecl : dataDecl.getDeclarations()) {
                if (first) {
                    dataDecl.setDeclarations(Lists.newList(astInnerDecl));
                    first = false;
                } else {
                    final DataDecl newDataDecl = new DataDecl(
                            Location.getDummyLocation(),
                            AstUtils.deepCopyNodes(dataDecl.getModifiers(), true, Optional.<Map<Node, Node>>absent()),
                            Lists.newList(astInnerDecl)
                    );
                    astOuterDeclIt.add(newDataDecl);
                }
            }
        }
    }

    private void packBitFields() {
        final ListIterator<Declaration> astDeclIt = nxStructRef.getFields().listIterator();
        final BitFieldSequenceData bitFieldsData = new BitFieldSequenceData();

        while (astDeclIt.hasNext()) {
            final DataDecl dataDecl = (DataDecl) astDeclIt.next();

            if (!dataDecl.getDeclarations().isEmpty()) {
                final FieldDecl fieldDecl = (FieldDecl) dataDecl.getDeclarations().getFirst();
                final FieldDeclaration fieldDeclaration = fieldDecl.getDeclaration();

                if (!fieldDeclaration.isBitField() && bitFieldsData.isNonEmpty()) {
                    insertFillerField(astDeclIt, bitFieldsData.getTotalSizeInBits(), true);
                    bitFieldsData.reset();
                } else if (fieldDeclaration.isBitField()) {
                    final IntegerType bitFieldType = (IntegerType) fieldDeclaration.getType().get();
                    final Endianness fieldEndianness = bitFieldType.getExternalScheme().get().getEndianness();
                    astDeclIt.remove();

                    if (!bitFieldsData.hasSameEndianness(fieldEndianness)) {
                        bitFieldsData.alignToByte();
                    }

                    bitFieldsData.update(fieldDeclaration.getSizeInBits(), fieldEndianness);
                }
            } else {
                if (bitFieldsData.isNonEmpty()) {
                    insertFillerField(astDeclIt, bitFieldsData.getTotalSizeInBits(), true);
                    bitFieldsData.reset();
                }
                recursiveTransform(dataDecl.getModifiers());
            }
        }

        if (bitFieldsData.isNonEmpty()) {
            insertFillerField(astDeclIt, bitFieldsData.getTotalSizeInBits(), false);
        }
    }

    private void addPackedAttribute() {
        if (!Iterables.any(nxStructRef.getAttributes(), AttributePredicates.getPackedPredicate())) {
            nxStructRef.getAttributes().add(AstUtils.newPackedAttribute());
        }
    }

    private void raiseTransformedFlag() {
        declaration.transformed();
    }

    private void insertFillerField(ListIterator<Declaration> astDeclIt, int fillerSizeInBits,
                boolean stepBackwards) {
        if (stepBackwards) {
            astDeclIt.previous();
        }

        final String fillerFieldName = nameMangler.mangle(FILLER_FIELD_NAME);
        final int fillerSize = VariousUtils.alignNumber(fillerSizeInBits, 8) / 8;
        astDeclIt.add(AstUtils.newFillerField(fillerFieldName, fillerSize));

        if (stepBackwards) {
            astDeclIt.next();
        }
    }

    private void recursiveTransform(List<? extends TypeElement> typeElements) {
        for (TypeElement typeElement : typeElements) {
            if (typeElement instanceof NxStructRef) {
                new ExternalStructureTransformer((NxStructRef) typeElement, abi, nameMangler)
                        .transform();
            } else if (typeElement instanceof NxUnionRef) {
                new ExternalUnionTransformer((NxUnionRef) typeElement, abi, nameMangler)
                        .transform();
            }
        }
    }
}
