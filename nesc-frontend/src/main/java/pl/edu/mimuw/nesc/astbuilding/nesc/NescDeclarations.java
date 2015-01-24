package pl.edu.mimuw.nesc.astbuilding.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.analysis.AttributeAnalyzer;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.TypeDefinitionType;
import pl.edu.mimuw.nesc.type.UnknownType;
import pl.edu.mimuw.nesc.type.UnknownTypeFactory;
import pl.edu.mimuw.nesc.astutil.Interval;
import pl.edu.mimuw.nesc.astbuilding.AstBuildingBase;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.nesc.ComponentDeclaration;
import pl.edu.mimuw.nesc.declaration.nesc.InterfaceDeclaration;
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
import static pl.edu.mimuw.nesc.analysis.NescAnalysis.checkComponentInstantiation;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.*;
import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.resolveType;
import static pl.edu.mimuw.nesc.astutil.AstUtils.getEndLocation;
import static pl.edu.mimuw.nesc.astutil.AstUtils.getStartLocation;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NescDeclarations extends AstBuildingBase {

    public NescDeclarations(NescEntityEnvironment nescEnvironment,
                            ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                            ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                            SemanticListener semanticListener, AttributeAnalyzer attributeAnalyzer) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder,
                semanticListener, attributeAnalyzer);
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
        final String uniqueName = semanticListener.nameManglingRequired(name);

        final TypeParmDecl decl = new TypeParmDecl(startLocation, name, attributes);
        decl.setEndLocation(endLocation);
        decl.setIsNestedInNescEntity(true);
        decl.setUniqueName(uniqueName);

        final UnknownType denotedType = UnknownTypeFactory.newInstance()
                .setName(name)
                .addAttributes(attributes)
                .newUnknownType();

        final TypenameDeclaration symbol = TypenameDeclaration.builder()
                .isGenericParameter(true)
                .uniqueName(uniqueName)
                .denotedType(denotedType)
                .name(name)
                .startLocation(startLocation)
                .build();

        if (!environment.getObjects().add(name, symbol)) {
            errorHelper.error(startLocation, Optional.of(endLocation),
                    format("duplicate parameter name '%s' in parameter list", name));
        }
        decl.setDeclaration(symbol);
        attributeAnalyzer.analyzeAttributes(attributes, symbol, environment);

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
                                            ComponentRef componentRef, Optional<Word> alias) {
        componentRef.setEndLocation(endLocation);
        componentRef.setAlias(alias);

        final Word componentName = componentRef.getName();
        Optional<? extends NescDeclaration> component = nescEnvironment.get(componentName.getName());
        if (component.isPresent() && component.get() instanceof InterfaceDeclaration) {
            component = Optional.absent();
            errorHelper.error(componentName.getLocation(), Optional.of(componentName.getEndLocation()),
                    format("expected component, but got an interface '%s'", componentName.getName()));
        }
        final String refName = alias.isPresent() ? alias.get().getName() : componentRef.getName().getName();

        final ComponentRefDeclaration symbol = ComponentRefDeclaration.builder()
                .componentName(componentName)
                .withFacade(true)
                .astNode(componentRef)
                .nescDeclaration((ComponentDeclaration) component.orNull())
                .name(refName)
                .startLocation(startLocation)
                .build();

        if (!environment.getObjects().add(refName, symbol)) {
            errorHelper.error(startLocation, Optional.of(endLocation), format("redeclaration of '%s'", refName));
        }
        componentRef.setDeclaration(symbol);

        checkComponentInstantiation(symbol, errorHelper);

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

        // Count the number of type elements that are not attributes
        int specifiersCount = elements.size();
        for (TypeElement typeElement : elements) {
            if (typeElement instanceof Attribute) {
                --specifiersCount;
            }
        }

        // Check the non-type specifiers and emit errors and warnings
        final SpecifiersSet specifiers = new SpecifiersSet(elements, errorHelper);
        checkGenericParameterSpecifiers(specifiers, specifiersCount,
                Interval.of(getStartLocation(elements).get(), endLocation),
                errorHelper);

        if (declarator.isPresent()) {
            final boolean isTypedef = specifiers.contains(NonTypeSpecifier.TYPEDEF);
            final String name = DeclaratorUtils.getDeclaratorName(declarator.get()).get();
            final ObjectDeclaration.Builder<? extends ObjectDeclaration> builder;
            final String uniqueName = DeclaratorUtils.mangleDeclaratorName(declarator.get(),
                    manglingFunction).get();

            if (isTypedef) {
                final UnknownType denotedType = UnknownTypeFactory.newInstance()
                        .setName(name)
                        .addAttributes(elements)
                        .addAttributes(attributes)
                        .newUnknownType();

                builder = TypenameDeclaration.builder().isGenericParameter(true)
                            .denotedType(denotedType)
                            .uniqueName(uniqueName);
                variableDecl.setType(Optional.<Type>of(TypeDefinitionType.getInstance()));
            } else {
                variableDecl.setType(resolveType(environment, elements, declarator,
                        errorHelper, startLocation, endLocation, semanticListener,
                        attributeAnalyzer));
                builder = VariableDeclaration.builder().isGenericParameter(true)
                            .uniqueName(uniqueName)
                            .type(variableDecl.getType().orNull());
            }

            final ObjectDeclaration declaration = builder
                    .name(name)
                    .startLocation(startLocation)
                    .build();

            if (!environment.getObjects().add(name, declaration)) {
                errorHelper.error(startLocation, Optional.of(endLocation), format("redeclaration of '%s'", name));
            }
            variableDecl.setDeclaration(declaration);
            attributeAnalyzer.analyzeAttributes(AstUtils.joinAttributes(elements, attributes), declaration, environment);
        }

        final DataDecl dataDecl = new DataDecl(startLocation, elements, Lists.<Declaration>newList(variableDecl));
        dataDecl.setEndLocation(endLocation);

        // TODO add to tokens

        return dataDecl;
    }
}
