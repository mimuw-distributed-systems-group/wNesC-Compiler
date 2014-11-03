package pl.edu.mimuw.nesc.facade.component;

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.TypeArgument;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.ast.type.UnknownType;
import pl.edu.mimuw.nesc.declaration.nesc.ComponentDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.facade.Substitution;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>Factory responsible for building appropriate facades for component
 * references.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ComponentRefFacadeFactory {
    /**
     * The logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(ComponentRefFacadeFactory.class);

    /**
     * Object that represents the component reference for the built facade.
     */
    private ComponentRefDeclaration declaration;

    /**
     * Get a factory that will build a component reference facade.
     *
     * @return Newly created factory.
     */
    public static ComponentRefFacadeFactory newInstance() {
        return new ComponentRefFacadeFactory();
    }

    /**
     * Private constructor to force using the factory static method.
     */
    private ComponentRefFacadeFactory() {
    }

    /**
     * Set the declaration that represents the component reference that the
     * facade will be built for.
     *
     * @param declaration Declaration to set.
     * @return <code>this</code>
     */
    public ComponentRefFacadeFactory setDeclaration(ComponentRefDeclaration declaration) {
        this.declaration = declaration;
        return this;
    }

    /**
     * <p>Create a new instance of a component reference facade built for the
     * reference represented by the declaration that has been previously set.
     * </p>
     *
     * @return Newly created component reference facade.
     * @throws IllegalStateException The component reference declaration object
     *                               has not been set yet.
     */
    public ComponentRefFacade newComponentRefFacade() {
        checkState(declaration != null, "component reference declaration object has not been set yet");

        if (!declaration.getComponentDeclaration().isPresent() ||
                !(declaration.getComponentDeclaration().get() instanceof ComponentDeclaration)) {
            logInvalidFacade("erroneous component declaration or reference");
            return new InvalidComponentRefFacade(declaration);
        }

        final ComponentDeclaration nescDeclaration =
                (ComponentDeclaration) declaration.getComponentDeclaration().get();

        final Optional<LinkedList<Declaration>> expectedParameters = nescDeclaration.getGenericParameters();
        final LinkedList<Expression> providedParameters = declaration.getAstComponentRef().getArguments();
        final Substitution.Builder substBuilder = Substitution.builder();

        // Create the substitution if the component is generic
        if (!expectedParameters.isPresent() && !providedParameters.isEmpty()) {
            logInvalidFacade("unexpected values of parameters");
            return new InvalidComponentRefFacade(declaration);
        } else if (expectedParameters.isPresent()) {
            if (expectedParameters.get().size() != providedParameters.size()) {
                logInvalidFacade("invalid number of parameters");
                return new InvalidComponentRefFacade(declaration);
            }

            if (!addTypeParameters(substBuilder, expectedParameters.get().iterator(),
                    providedParameters.iterator())) {
                logInvalidFacade("invalid values of parameters");
                return new InvalidComponentRefFacade(declaration);
            }
        }

        if (LOG.isDebugEnabled()) {
            final Location location = declaration.getAstComponentRef().getLocation();
            LOG.debug(format("Good component reference facade for '%s' in %s:%d:%d", declaration.getName(),
                    location.getFilePath(), location.getLine(), location.getColumn()));
        }

        return GoodComponentRefFacade.builder()
                .declaration(declaration)
                .specificationEnvironment(nescDeclaration.getSpecificationEnvironment())
                .substitution(substBuilder.build())
                .build();
    }

    private boolean addTypeParameters(Substitution.Builder substBuilder, Iterator<Declaration> expectedIt,
            Iterator<Expression> providedIt) {
        final Set<String> usedNames = new HashSet<>();

        while (expectedIt.hasNext()) {
            final Declaration paramDecl = expectedIt.next();
            final Expression paramValue = providedIt.next();

            if (!(paramDecl instanceof DataDecl)) {
                return false;
            }

            final DataDecl expectedDecl = (DataDecl) paramDecl;
            checkState(expectedDecl.getDeclarations().size() == 1, "unexpected count of inner declarations in a generic parameter declaration %s",
                    expectedDecl.getDeclarations().size());
            checkState(expectedDecl.getDeclarations().getFirst() instanceof VariableDecl, "unexpected class of the inner declaration of a generic parameter '%s'",
                    expectedDecl.getDeclarations().getFirst().getClass());
            final VariableDecl expectedInnerDecl = (VariableDecl) expectedDecl.getDeclarations().getFirst();

            final ObjectDeclaration expectedDeclaration = expectedInnerDecl.getDeclaration();
            if (expectedDeclaration == null) {
                return false;
            } else if (expectedDeclaration.getKind() != ObjectKind.TYPENAME) {
                continue;
            }

            final TypenameDeclaration typeDeclaration = (TypenameDeclaration) expectedDeclaration;
            checkState(typeDeclaration.getDenotedType().isPresent(),
                    "denoted type of a generic type parameter unexpectedly absent");
            checkState(typeDeclaration.getDenotedType().get() instanceof UnknownType,
                    "unexpected class of the denoted type of a generic type parameter '%s'",
                    typeDeclaration.getDenotedType().get().getClass());
            final UnknownType paramType = (UnknownType) typeDeclaration.getDenotedType().get();

            if (!usedNames.add(paramType.getName())) {
                return false;
            } else if (!(paramValue instanceof TypeArgument)) {
                return false;
            }

            final TypeArgument providedType = (TypeArgument) paramValue;
            substBuilder.addMapping(paramType.getName(), providedType.getAsttype().getType());
        }

        return true;
    }

    private void logInvalidFacade(String reason) {
        if (LOG.isDebugEnabled()) {
            final Location location = declaration.getAstComponentRef().getLocation();
            LOG.debug(format("Invalid component reference facade for '%s', reason: %s, at: %s:%d:%d",
                    declaration.getName(), reason, location.getFilePath(), location.getLine(),
                    location.getColumn()));
        }
    }
}
