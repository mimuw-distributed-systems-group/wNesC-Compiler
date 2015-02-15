package pl.edu.mimuw.nesc.finalreduce;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Iterator;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.ArrayDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.FieldDecl;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Rid;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.predicates.ExternalBaseAttributePredicate;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.typelayout.UniversalTypeLayoutCalculator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transformation for external base types.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ExternalBaseTransformer {
    /**
     * Name of the field that will be created for external base types.
     */
    private static final String NAME_BASE_FIELD = "nxdata";

    /**
     * Declaration that will be transformed.
     */
    private final DataDecl dataDecl;

    /**
     * ABI of the project.
     */
    private final ABI abi;

    /**
     * Predicate used for checking if the declaration is an external base type
     * declaration.
     */
    private final ExternalBaseAttributePredicate externalBasePredicate;

    public static String getBaseFieldName() {
        return NAME_BASE_FIELD;
    }

    ExternalBaseTransformer(DataDecl dataDecl, ABI abi) {
        checkNotNull(dataDecl, "data declaration cannot be null");
        checkNotNull(abi, "ABI cannot be null");

        this.dataDecl = dataDecl;
        this.abi = abi;
        this.externalBasePredicate = new ExternalBaseAttributePredicate();
    }

    /**
     * Performs the transformation of the data declaration given at construction
     * if it is necessary.
     */
    public void transform() {
        if (!removeExternalBaseAttributes(dataDecl)) {
            return;
        } else if (dataDecl.getDeclarations().size() != 1) {
            throw new RuntimeException("outer declaration of an external base type contains "
                    + dataDecl.getDeclarations().size() + " inner declaration(s)");
        }

        removeTypeElements();
        appendTypedefModifier();
        appendStructureDefinition();
    }

    /**
     * @return <code>true</code> if and only if the given declaration is an
     *         external base type declaration.
     */
    private boolean removeExternalBaseAttributes(DataDecl dataDecl) {
        final Iterator<TypeElement> typeElementsIt = dataDecl.getModifiers().iterator();
        boolean nxBaseDeclaration = false;

        while (typeElementsIt.hasNext()) {
            final TypeElement typeElement = typeElementsIt.next();

            if (typeElement instanceof Attribute
                    && externalBasePredicate.apply((Attribute) typeElement)) {
                typeElementsIt.remove();
                nxBaseDeclaration = true;
            }
        }

        for (Declaration innerDecl : dataDecl.getDeclarations()) {
            final Collection<Attribute> attributes;

            if (innerDecl instanceof VariableDecl) {
                attributes = ((VariableDecl) innerDecl).getAttributes();
            } else if (innerDecl instanceof FieldDecl) {
                attributes = ((FieldDecl) innerDecl).getAttributes();
            } else {
                throw new RuntimeException("unexpected inner declaration class '"
                        + innerDecl.getClass().getCanonicalName() + "'");
            }

            nxBaseDeclaration = externalBasePredicate.remove(attributes)
                    || nxBaseDeclaration;
        }

        return nxBaseDeclaration;
    }

    private void removeTypeElements() {
        dataDecl.getModifiers().clear();
    }

    private void appendTypedefModifier() {
        dataDecl.getModifiers().add(new Rid(Location.getDummyLocation(), RID.TYPEDEF));
    }

    private void appendStructureDefinition() {
        // Determine the size of the type
        final VariableDecl nxBaseTypeDeclaration = (VariableDecl) dataDecl.getDeclarations().getFirst();
        final TypenameDeclaration declarationObj = (TypenameDeclaration) nxBaseTypeDeclaration.getDeclaration();
        final int arraySize = new UniversalTypeLayoutCalculator(this.abi, declarationObj.getDenotedType().get())
                .calculate().getSize();

        // Create and append the structure definition
        dataDecl.getModifiers().add(newNxBaseStructure(getBaseFieldName(), arraySize));
    }

    private StructRef newNxBaseStructure(String fieldName, int arraySize) {
        // Identifier declarator

        final IdentifierDeclarator identDecl = new IdentifierDeclarator(Location.getDummyLocation(),
                fieldName);
        identDecl.setUniqueName(Optional.<String>absent());

        // Array declarator

        final ArrayDeclarator arrayDecl = new ArrayDeclarator(Location.getDummyLocation(),
                Optional.<Declarator>of(identDecl), Optional.of(AstUtils.newIntegerConstant(arraySize)));

        // Field declaration

        final FieldDecl fieldDecl = new FieldDecl(Location.getDummyLocation(),
                Optional.<Declarator>of(arrayDecl), Lists.<Attribute>newList(),
                Optional.<Expression>absent());

        // Data declaration

        final DataDecl dataDecl = new DataDecl(Location.getDummyLocation(),
                AstUtils.newRidsList(RID.UNSIGNED, RID.CHAR),
                Lists.<Declaration>newList(fieldDecl));

        // Structure definition

        return new StructRef(
                Location.getDummyLocation(),
                Lists.<Attribute>newList(),
                Lists.<Declaration>newList(dataDecl),
                null,  // unnamed structure
                StructSemantics.DEFINITION
        );
    }
}
