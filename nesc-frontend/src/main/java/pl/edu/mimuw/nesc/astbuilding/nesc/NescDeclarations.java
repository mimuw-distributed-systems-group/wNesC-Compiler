package pl.edu.mimuw.nesc.astbuilding.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astbuilding.AstBuildingBase;
import pl.edu.mimuw.nesc.astbuilding.DeclaratorUtils;
import pl.edu.mimuw.nesc.astbuilding.TypeElementUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.nesc.NescDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

import java.util.LinkedList;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.AstUtils.getEndLocation;
import static pl.edu.mimuw.nesc.ast.AstUtils.getStartLocation;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescDeclarations extends AstBuildingBase {

    public NescDeclarations(NescEntityEnvironment nescEnvironment,
                            ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                            ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder);
    }

    /**
     * Declares type parameter in parameterised interface definition.
     *
     * @param environment   current environment
     * @param startLocation start location of declaration
     * @param endLocation   end location of declaration
     * @param name          type name
     * @param attributes    attributes
     */
    public TypeParmDecl declareTypeParameter(Environment environment, Location startLocation, Location endLocation,
                                             String name, LinkedList<Attribute> attributes) {
        final TypeParmDecl decl = new TypeParmDecl(startLocation, name, attributes);
        decl.setEndLocation(endLocation);

        final TypenameDeclaration symbol = new TypenameDeclaration(name, startLocation);
        if (!environment.getObjects().add(name, symbol)) {
            errorHelper.error(startLocation, Optional.of(endLocation),
                    format("duplicate parameter name '%s' in parameter list", name));
        }
        decl.setDeclaration(symbol);

        // TODO add to tokens

        return decl;
    }

    /**
     * Declares usage of component with given alias.
     *
     * @param environment   current environment
     * @param startLocation start location of declaration
     * @param endLocation   end location of declaration
     * @param componentRef  component reference
     * @param alias         optional alias
     */
    public ComponentRef declareComponentRef(Environment environment, Location startLocation, Location endLocation,
                                            ComponentRef componentRef, Optional<String> alias) {
        componentRef.setEndLocation(endLocation);

        final Word componentName = componentRef.getName();
        final Optional<? extends NescDeclaration> component = nescEnvironment.get(componentName.getName());
        final String refName = alias.isPresent() ? alias.get() : componentRef.getName().getName();

        final ComponentRefDeclaration symbol = new ComponentRefDeclaration(refName, componentName, startLocation);
        symbol.setAstComponentRef(componentRef);
        symbol.setComponentDeclaration(component);

        if (!environment.getObjects().add(refName, symbol)) {
            errorHelper.error(startLocation, Optional.of(endLocation), format("redeclaration of '%s'", refName));
        }
        componentRef.setDeclaration(symbol);

        // TODO add to tokens

        return componentRef;
    }

    /**
     * Declares parameter of generic component.
     *
     * @param environment current environment
     * @param declarator  declarator
     * @param elements    modifiers
     * @param attributes  attributes
     * @return declaration of parameter
     */
    public DataDecl declareTemplateParameter(Environment environment, Optional<Declarator> declarator,
                                             LinkedList<TypeElement> elements, LinkedList<Attribute> attributes) {
        /* Either declarator or elements is present. */
        final Location startLocation = declarator.isPresent()
                ? declarator.get().getLocation()
                : getStartLocation(elements).get();
        final Location endLocation = declarator.isPresent()
                ? getEndLocation(declarator.get().getEndLocation(), elements, attributes)
                : getEndLocation(startLocation, elements, attributes); // elements is not empty, $1 will not be used


        final VariableDecl variableDecl = new VariableDecl(startLocation, declarator, attributes,
                Optional.<AsmStmt>absent());
        variableDecl.setInitializer(Optional.<Expression>absent());
        variableDecl.setEndLocation(endLocation);

        if (declarator.isPresent()) {
            final boolean isTypedef = TypeElementUtils.isTypedef(elements);
            final String name = DeclaratorUtils.getDeclaratorName(declarator.get());
            final ObjectDeclaration declaration;
            if (isTypedef) {
                declaration = new TypenameDeclaration(name, startLocation);
            } else {
                declaration = new VariableDeclaration(name, startLocation);
            }
            if (!environment.getObjects().add(name, declaration)) {
                errorHelper.error(startLocation, Optional.of(endLocation), format("redeclaration of '%s'", name));
            }
            variableDecl.setDeclaration(declaration);
        }

        final DataDecl dataDecl = new DataDecl(startLocation, elements, Lists.<Declaration>newList(variableDecl));
        dataDecl.setEndLocation(endLocation);

        // TODO add to tokens

        return dataDecl;
    }
}
