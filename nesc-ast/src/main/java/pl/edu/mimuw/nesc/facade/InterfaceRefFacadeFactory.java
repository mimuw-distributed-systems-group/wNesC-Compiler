package pl.edu.mimuw.nesc.facade;

import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.Interface;
import pl.edu.mimuw.nesc.ast.gen.TypeArgument;
import pl.edu.mimuw.nesc.ast.gen.TypeParmDecl;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;

import com.google.common.base.Optional;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

/**
 * A factory that is responsible for producing interface reference facades of
 * appropriate classes.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InterfaceRefFacadeFactory {
    /**
     * Logger that will handle debug messages.
     */
    private static final Logger LOG = Logger.getLogger(InterfaceRefFacadeFactory.class);

    /**
     * Interface reference that the facade will be built for.
     */
    private InterfaceRefDeclaration interfaceRef;

    /**
     * Get the new factory that will built an interface reference facade.
     *
     * @return Newly created interface reference facade factory.
     */
    public static InterfaceRefFacadeFactory newInstance() {
        return new InterfaceRefFacadeFactory();
    }

    /**
     * Private constructor to force using the factory method.
     */
    private InterfaceRefFacadeFactory() {
    }

    /**
     * Set the interface reference declaration that represents the reference
     * that the facade will be built for.
     *
     * @param declaration Declaration to set.
     * @return <code>this</code>
     */
    public InterfaceRefFacadeFactory setInterfaceRefDeclaration(InterfaceRefDeclaration declaration) {
        this.interfaceRef = declaration;
        return this;
    }

    /**
     * <p>Create a new instance of an interface reference facade for the
     * interface reference that has been previously set. The algorithm of
     * choosing the class that is instantiated is as follows:</p>
     *
     * <ol>
     *     <li>If the interface declaration is absent in the interface reference
     *     declaration object, an <code>InvalidInterfaceRefFacade</code> object
     *     is created.</li>
     *
     *     <li>Otherwise, if the count of provided generic parameters differs
     *     from the count of generic parameters from the interface definition,
     *     an <code>InvalidInterfaceRefFacade</code> object is created.</li>
     *
     *     <li>Otherwise, if there is a generic parameter in the interface
     *     definition that is not represented by a {@link TypeParmDecl} object
     *     (e.g. it is represented by an
     *     {@link pl.edu.mimuw.nesc.ast.gen.ErrorDecl ErrorDecl} object), an
     *     <code>InvalidInterfaceRefFacade</code> object is created.</li>
     *
     *     <li>Otherwise, if there is a type provided for a generic parameter
     *     that is not represented by a {@link TypeArgument} object, an
     *     <code>InvalidInterfaceRefFacade</code> object is created.</li>
     *
     *     <li>Otherwise, if there are two generic parameters in the interface
     *     definition that have the same name, an
     *     <code>InvalidInterfaceRefFacade</code> object is created.</li>
     *
     *     <li>Otherwise, a <code>GoodInterfaceRefFacade</code> object is
     *     created.</li>
     * </ol>
     *
     * @return A newly created instance of <code>InterfaceRefFacade</code>.
     * @throws IllegalStateException The interface reference declaration object
     *                               has not been set yet.
     * @see GoodInterfaceRefFacade
     * @see InvalidInterfaceRefFacade
     */
    public InterfaceRefFacade newInterfaceRefFacade() {
        checkState(interfaceRef != null, "interface reference declaration object has not been set");

        if (LOG.isTraceEnabled()) {
            LOG.trace("Creating an interface reference facade for '" + interfaceRef.getName()
                    + "' in file " + interfaceRef.getLocation().getFilePath());
        }

        if (!interfaceRef.getIfaceDeclaration().isPresent()) {
            return new InvalidInterfaceRefFacade(interfaceRef);
        }

        final Interface iface = interfaceRef.getIfaceDeclaration().get().getAstInterface();
        final Optional<LinkedList<Expression>> providedGenericParams = interfaceRef.getAstInterfaceRef().getArguments();
        final GoodInterfaceRefFacade.Builder goodFacadeBuilder = GoodInterfaceRefFacade.builder();

        /* Add information about the provided and needed generic parameters
           to the builder simultaneously checking the correctness. */
        if (!iface.getParameters().isPresent() && providedGenericParams.isPresent()
                || iface.getParameters().isPresent() && !providedGenericParams.isPresent()) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid interface reference facade for '" + interfaceRef.getName() + "' in file "
                            + interfaceRef.getLocation().getFilePath()
                            + " because of inconsistency in the presence of generic parameters");
            }
            return new InvalidInterfaceRefFacade(interfaceRef);

        } else if (iface.getParameters().isPresent()) {
            if (!addParametersData(goodFacadeBuilder, iface.getParameters().get().iterator(),
                    providedGenericParams.get().iterator())) {
                return new InvalidInterfaceRefFacade(interfaceRef);
            }
        }

        // Add remaining information to the facade builder
        goodFacadeBuilder.bodyEnvironment(iface.getDeclarationEnvironment())
                .ifaceRefDeclaration(interfaceRef);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Good interface reference facade for '" + interfaceRef.getName() + "' in file "
                    + interfaceRef.getLocation().getFilePath());
        }

        return goodFacadeBuilder.build();
    }

    private boolean addParametersData(GoodInterfaceRefFacade.Builder builder, Iterator<Declaration> declsIt,
            Iterator<Expression> destTypesIt) {
        final Set<String> paramsNames = new HashSet<>();
        boolean invalidCounts = false;

        while (declsIt.hasNext()) {

            if (!destTypesIt.hasNext()) {
                invalidCounts = true;
                break;
            }

            final Declaration decl = declsIt.next();
            final Expression destType = destTypesIt.next();

            if (!(decl instanceof TypeParmDecl) || !(destType instanceof TypeArgument)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid interface reference facade for '" + interfaceRef.getName() + "' in file "
                        + interfaceRef.getLocation().getFilePath() + " because of classes of generic parameters encountered in AST: "
                        + decl.getClass().getCanonicalName() + " and " + destType.getClass().getCanonicalName());
                }
                return false;
            }

            final TypeParmDecl typeParamDecl = (TypeParmDecl) decl;
            final TypeArgument typeArgument = (TypeArgument) destType;

            if (!paramsNames.add(typeParamDecl.getName())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid interface reference facade for '" + interfaceRef.getName() + "' in file "
                            + interfaceRef.getLocation().getFilePath() + " because two generic parameters have name '"
                            + typeParamDecl.getName() + "' in the interface definition");
                }
                return false;
            }

            builder.addParameterName(typeParamDecl.getName());
            builder.addInstantiationType(typeArgument.getAsttype().getType());
        }

        invalidCounts = invalidCounts || destTypesIt.hasNext();

        if (invalidCounts && LOG.isDebugEnabled()) {
            LOG.debug("Invalid interface reference facade for '" + interfaceRef.getName() + "' in file "
                    + interfaceRef.getLocation().getFilePath() + " because counts of provided and needed generic parameters differ");
        }

        return !invalidCounts;
    }
}
