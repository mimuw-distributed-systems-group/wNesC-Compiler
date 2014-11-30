package pl.edu.mimuw.nesc.astbuilding.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.astbuilding.AstBuildingBase;
import pl.edu.mimuw.nesc.ast.util.DeclaratorUtils;
import pl.edu.mimuw.nesc.declaration.nesc.ConfigurationDeclaration;
import pl.edu.mimuw.nesc.declaration.nesc.InterfaceDeclaration;
import pl.edu.mimuw.nesc.declaration.nesc.ModuleDeclaration;
import pl.edu.mimuw.nesc.declaration.nesc.NescDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.facade.component.specification.ConfigurationTable;
import pl.edu.mimuw.nesc.facade.component.specification.ImplementationElement;
import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;
import pl.edu.mimuw.nesc.facade.component.specification.WiringElement;
import pl.edu.mimuw.nesc.facade.iface.InterfaceRefFacadeFactory;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.MissingImplementationElementError;
import pl.edu.mimuw.nesc.problem.issue.MissingWiringError;
import pl.edu.mimuw.nesc.token.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.analysis.NescAnalysis.checkEqConnection;
import static pl.edu.mimuw.nesc.analysis.NescAnalysis.checkInterfaceInstantiation;
import static pl.edu.mimuw.nesc.analysis.NescAnalysis.checkRpConnection;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.checkInstanceParametersSpecifiers;
import static pl.edu.mimuw.nesc.ast.util.AstUtils.makeWord;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NescComponents extends AstBuildingBase {

    /**
     * Object that allows logging events that occur in objects of this class.
     */
    private static final Logger LOG = Logger.getLogger(NescComponents.class);

    public NescComponents(NescEntityEnvironment nescEnvironment,
                          ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                          ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder);
    }

    public Interface startInterface(Environment environment, Location startLocation, Word name) {
        final Interface iface = new Interface(startLocation, null, name, null, null);

        final InterfaceDeclaration declaration = new InterfaceDeclaration(name.getName(), name.getLocation());
        declaration.setAstInterface(iface);
        /* Set parameter environment before parameters are parsed. */
        iface.setParameterEnvironment(environment);
        iface.setDeclaration(declaration);

        if (!nescEnvironment.add(name.getName(), declaration)) {
            errorHelper.error(name.getLocation(), Optional.of(name.getEndLocation()),
                    format("redefinition of '%s'", name.getName()));
        }

        return iface;
    }

    public Module startModule(Environment environment, Location startLocation, Word name, boolean isGeneric) {
        final Module module = new Module(startLocation, null, name, null, null, isGeneric, null);
        /* Set parameter environment before parsing parameters. */
        module.setParameterEnvironment(environment);

        final ModuleDeclaration moduleDeclaration = new ModuleDeclaration(name.getName(), name.getLocation());
        moduleDeclaration.setAstModule(module);
        module.setDeclaration(moduleDeclaration);

        if (!nescEnvironment.add(name.getName(), moduleDeclaration)) {
            errorHelper.error(name.getLocation(), Optional.of(name.getEndLocation()),
                    format("redefinition of '%s'", name.getName()));
        }

        return module;
    }

    public Configuration startConfiguration(Environment environment, Location startLocation, Word name,
                                            boolean isGeneric) {
        final Configuration configuration = new Configuration(startLocation, null, name, null, null, isGeneric, null);
        /* Set parameter environment before parsing parameters. */
        configuration.setParameterEnvironment(environment);

        final ConfigurationDeclaration configurationDeclaration = new ConfigurationDeclaration(name.getName(),
                name.getLocation());
        configurationDeclaration.setAstConfiguration(configuration);
        configuration.setDeclaration(configurationDeclaration);

        if (!nescEnvironment.add(name.getName(), configurationDeclaration)) {
            errorHelper.error(name.getLocation(), Optional.of(name.getEndLocation()),
                    format("redefinition of '%s'", name.getName()));
        }

        return configuration;
    }

    public void handleInterfaceParametersAttributes(Environment environment, Interface iface,
                                                    Optional<LinkedList<Declaration>> parameters,
                                                    LinkedList<Attribute> attributes) {
        iface.setParameters(parameters);
        iface.setAttributes(attributes);
        /* Set declaration environment before specification is parsed. */
        iface.setDeclarationEnvironment(environment);
    }

    public void handleComponentParametersAttributes(Environment environment, Component component,
                                                    Optional<LinkedList<Declaration>> parameters,
                                                    LinkedList<Attribute> attributes) {
        component.setParameters(parameters);
        component.setAttributes(attributes);
        /* Set specification environment before specification is parsed. */
        component.setSpecificationEnvironment(environment);
    }

    public void handleComponentSpecification(Component component, LinkedList<Declaration> specification) {
        component.setDeclarations(specification);

        final SpecificationVisitor specificationVisitor = new SpecificationVisitor();
        specificationVisitor.visitDeclarations(component);
    }

    /**
     * Method that is called after
     * {@link NescComponents#handleComponentSpecification}.
     */
    public ModuleTable handleModuleSpecification(Module module, LinkedList<Declaration> specification) {
        final ModuleTable result = ModuleTable.builder()
                .addDeclarations(specification)
                .build();

        module.getDeclaration().setModuleTable(result);
        return result;
    }

    /**
     * This method is called after {@link NescComponents#handleComponentSpecification}.
     */
    public ConfigurationTable handleConfigurationSpecification(Configuration configuration,
            LinkedList<Declaration> specification) {

        final ConfigurationTable result = ConfigurationTable.builder()
                .addDeclarations(specification)
                .build();

        configuration.getDeclaration().setConfigurationTable(result);
        return result;
    }

    public void finishInterface(Interface iface, Location endLocation, LinkedList<Declaration> declarations) {
        iface.setDeclarations(declarations);
        iface.setEndLocation(endLocation);

         /* Check interface declarations. */
        final InterfaceBodyVisitor bodyVisitor = new InterfaceBodyVisitor();
        bodyVisitor.visitDeclarations(iface.getDeclarations());
    }

    public void finishComponent(Component component, Implementation implementation) {
        component.setImplementation(implementation);
        component.setEndLocation(implementation.getEndLocation());
    }

    /**
     * Method is called right after {@link NescComponents#finishComponent}.
     */
    public void finishModule(Module module, Location endLocation) {
        final ImmutableSet<Map.Entry<String, ImplementationElement>> elements =
                module.getDeclaration().getModuleTable().getAll();

        // Check if all required commands and events are implemented
        for (Map.Entry<String, ImplementationElement> implEntry : elements) {
            final String name = implEntry.getKey();
            final ImplementationElement implElement = implEntry.getValue();

            if (implElement.isProvided() && !implElement.isImplemented()) {
                final ErroneousIssue error = new MissingImplementationElementError(implElement.getKind(),
                        name, implElement.getInterfaceName());
                errorHelper.error(module.getLocation(), endLocation, error);
            }
        }
    }

    /**
     * Method is called right after {@link NescComponents#finishComponent}.
     */
    public void finishConfiguration(Configuration configuration, Location endLocation) {
        final ConfigurationImpl implementation = (ConfigurationImpl) configuration.getImplementation();
        final Environment implEnvironment = implementation.getEnvironment();
        final ConfigurationTable table = configuration.getDeclaration().getConfigurationTable();

        // Check wiring
        for (Declaration declaration : implementation.getDeclarations()) {
            if (declaration instanceof RpConnection) {
                checkRpConnection((RpConnection) declaration, implEnvironment, errorHelper);
            } else if (declaration instanceof EqConnection) {
                checkEqConnection((EqConnection) declaration, implEnvironment, table, errorHelper);
            }
        }

        // Check if all external specification elements are wired
        for (Map.Entry<String, WiringElement> configurationEntry : table.getAll()) {
            final String elementName = configurationEntry.getKey();
            final WiringElement wiringElement = configurationEntry.getValue();

            if (!wiringElement.isWired()) {
                final ErroneousIssue error = new MissingWiringError(elementName, wiringElement.getKind());
                errorHelper.error(configuration.getLocation(), endLocation, error);
            }
        }
    }

    public void declareInterfaceRef(Environment environment, InterfaceRef ifaceRef,
                                    Optional<LinkedList<Declaration>> genericParameters,
                                    LinkedList<Attribute> attributes) {
        checkInstanceParametersSpecifiers(genericParameters, errorHelper);

        ifaceRef.setGenericParameters(genericParameters);
        ifaceRef.setAttributes(attributes);

        final String ifaceName = ifaceRef.getName().getName();
        final String refName;
        final Location refLocation;
        final Location refEndLocation;
        if (ifaceRef.getAlias().isPresent()) {
            refName = ifaceRef.getAlias().get().getName();
            refLocation = ifaceRef.getAlias().get().getLocation();
            refEndLocation = ifaceRef.getAlias().get().getEndLocation();
        } else {
            refName = ifaceRef.getName().getName();
            refLocation = ifaceRef.getName().getLocation();
            refEndLocation = ifaceRef.getName().getEndLocation();
        }

        List<Optional<Type>> resolvedParams = null;
        if (ifaceRef.getArguments().isPresent()) {
            resolvedParams = new ArrayList<>();
            for (Expression expr : ifaceRef.getArguments().get()) {
                if (!(expr instanceof TypeArgument)) {
                    throw new RuntimeException(format("unexpected class '%s' as a type argument in interface reference", expr.getClass().getCanonicalName()));
                }
                resolvedParams.add(expr.getType());
            }
        }
        final Optional<List<Optional<Type>>> maybeParams = Optional.fromNullable(resolvedParams);

        final InterfaceRefDeclaration declaration = InterfaceRefDeclaration.builder()
                .interfaceName(ifaceName)
                .typeArguments(maybeParams)
                .astNode(ifaceRef)
                .name(refName)
                .startLocation(refLocation)
                .build();
        if (!environment.getObjects().add(refName, declaration)) {
            errorHelper.error(refLocation, Optional.of(refEndLocation), format("redefinition of '%s'", refName));
        }
        ifaceRef.setDeclaration(declaration);
    }

    public InterfaceRefDeclarator makeInterfaceRefDeclarator(Environment environment, Location ifaceStartLocation,
            String ifaceName, Location funcNameStartLocation, Location funcNameEndLocation, String functionName) {

        final IdentifierDeclarator id = new IdentifierDeclarator(funcNameStartLocation, functionName);
        id.setIsNestedInNescEntity(environment.isEnclosedInNescEntity());
        id.setEndLocation(funcNameEndLocation);

        final InterfaceRefDeclarator declarator = new InterfaceRefDeclarator(ifaceStartLocation,
                Optional.<Declarator>of(id), makeWord(ifaceStartLocation, funcNameEndLocation, ifaceName));
        declarator.setEndLocation(funcNameEndLocation);
        return declarator;
    }

    public RpConnection makeRpConnection(EndPoint from, EndPoint to, Location startLocation,
            Location endLocation, Environment environment) {
        final RpConnection connection = new RpConnection(startLocation, from, to);
        connection.setEndLocation(endLocation);
        return connection;
    }

    public EqConnection makeEqConnection(EndPoint left, EndPoint right, Location startLocation,
            Location endLocation, Environment environment) {
        final EqConnection connection = new EqConnection(startLocation, left, right);
        connection.setEndLocation(endLocation);
        return connection;
    }

    private final class SpecificationVisitor extends ExceptionVisitor<Void, Component> {

        /**
         * Set with names of analyzed objects to ignore analysis of declarations
         * that are redeclarations.
         */
        private final Set<String> analyzedNames = new HashSet<>();

        public void visitDeclarations(Component component) {
            for (Declaration declaration : component.getDeclarations()) {
                if (declaration == null) {
                    continue;
                }
                declaration.accept(this, component);
            }
        }

        public Void visitDataDecl(DataDecl declaration, Component component) {
            // TODO
            return null;
        }

        public Void visitErrorDecl(ErrorDecl declaration, Component component) {
            /* ignore */
            return null;
        }

        public Void visitEmptyDecl(EmptyDecl declaration, Component component) {
            /* ignore, additional semicolon */
            return null;
        }

        public Void visitRequiresInterface(RequiresInterface elem, Component component) {
            final RequiresProvidesVisitor rpVisitor = new RequiresProvidesVisitor(component, false, analyzedNames);
            rpVisitor.visitDeclarations(elem.getDeclarations());
            return null;
        }

        public Void visitProvidesInterface(ProvidesInterface elem, Component component) {
            final RequiresProvidesVisitor rpVisitor = new RequiresProvidesVisitor(component, true, analyzedNames);
            rpVisitor.visitDeclarations(elem.getDeclarations());
            return null;
        }

        // TODO: bare event/command

        /*
         * Occurrences of other declarations should be reported as errors.
         */

    }

    private final class RequiresProvidesVisitor extends ExceptionVisitor<Void, Void> {

        private final Component component;
        private final boolean provides;
        private final Set<String> analyzedNames;

        private RequiresProvidesVisitor(Component component, boolean provides,
                Set<String> analyzedNames) {
            this.component = component;
            this.provides = provides;
            this.analyzedNames = analyzedNames;
        }

        public void visitDeclarations(LinkedList<Declaration> declarations) {
            for (Declaration declaration : declarations) {
                if (declaration == null) {
                    continue;
                }
                declaration.accept(this, null);
            }
        }

        @Override
        public Void visitErrorDecl(ErrorDecl errorDecl, Void arg) {
            // ignore
            return null;
        }

        @Override
        public Void visitEmptyDecl(EmptyDecl declaration, Void arg) {
            /* ignore, additional semicolon */
            return null;
        }

        @Override
        public Void visitInterfaceRef(InterfaceRef ref, Void arg) {
            final String name = ref.getAlias().isPresent() ? ref.getAlias().get().getName() : ref.getName().getName();
            // Ignore the reference if it is a redeclaration
            if (!analyzedNames.add(name)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(format("Ignoring interface reference '%s' at %s:%d:%d", name, ref.getLocation().getFilePath(),
                            ref.getLocation().getLine(), ref.getLocation().getColumn()));
                }
                return null;
            }
            final InterfaceRefDeclaration refDeclaration =
                    (InterfaceRefDeclaration) component.getSpecificationEnvironment().getObjects().get(name).get();
            final InterfaceRefFacadeFactory facadeFactory = InterfaceRefFacadeFactory.newInstance();

            facadeFactory.setInterfaceRefDeclaration(refDeclaration);

            final Word ifaceName = ref.getName();
            final Optional<? extends NescDeclaration> declaration = nescEnvironment.get(ifaceName.getName());
            if (!declaration.isPresent()) {
                errorHelper.error(ifaceName.getLocation(), Optional.of(ifaceName.getEndLocation()),
                        format("unknown interface '%s'", ifaceName.getName()));
                refDeclaration.setIfaceDeclaration(Optional.<InterfaceDeclaration>absent());
                refDeclaration.setFacade(facadeFactory.newInterfaceRefFacade());
                return null;
            }
            if (!(declaration.get() instanceof InterfaceDeclaration)) {
                errorHelper.error(ifaceName.getLocation(), Optional.of(ifaceName.getEndLocation()),
                        format("'%s' is not interface", ifaceName.getName()));
                refDeclaration.setIfaceDeclaration(Optional.<InterfaceDeclaration>absent());
                refDeclaration.setFacade(facadeFactory.newInterfaceRefFacade());
                return null;
            }
            final InterfaceDeclaration ifaceDeclaration = (InterfaceDeclaration) declaration.get();

            refDeclaration.setProvides(provides);
            refDeclaration.setIfaceDeclaration(Optional.of(ifaceDeclaration));
            refDeclaration.setFacade(facadeFactory.newInterfaceRefFacade());

            checkInterfaceInstantiation(refDeclaration, errorHelper);

            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl dataDecl, Void arg) {
            // TODO: bare event/command
            // TODO: typedef, tagged type

            // Set the provides value for bare commands and events
            for (Declaration innerDecl : dataDecl.getDeclarations()) {
                if (!(innerDecl instanceof VariableDecl)) {
                    continue;
                }

                final VariableDecl varDecl = (VariableDecl) innerDecl;
                checkState(varDecl.getDeclaration() != null, "the declaration object is absent for a bare command or event AST node");
                if (varDecl.getDeclaration().getKind() != ObjectKind.FUNCTION) {
                    continue;
                }

                final FunctionDeclaration funDecl = (FunctionDeclaration) varDecl.getDeclaration();

                // Ignore this declaration if it is a redeclaration
                if (!analyzedNames.add(funDecl.getName())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(format("Ignoring function '%s' at %s:%d:%d", funDecl.getName(),
                                varDecl.getLocation().getFilePath(), varDecl.getLocation().getLine(),
                                varDecl.getLocation().getColumn()));
                    }
                    continue;
                }

                funDecl.setProvided(provides);
            }

            return null;
        }

    }

    private final class InterfaceBodyVisitor extends ExceptionVisitor<Void, Void> {

        public void visitDeclarations(LinkedList<Declaration> declarations) {
            for (Declaration declaration : declarations) {
                if (declaration == null) {
                    /* null for empty statement and target def. */
                    continue;
                }
                declaration.accept(this, null);
            }
        }

        public Void visitErrorDecl(ErrorDecl declaration, Void arg) {
            /* ignore */
            return null;
        }

        @Override
        public Void visitEmptyDecl(EmptyDecl declaration, Void arg) {
            /* ignore, additional semicolon */
            return null;
        }

        public Void visitDataDecl(DataDecl declaration, Void arg) {
            final InterfaceDeclarationVisitor visitor = new InterfaceDeclarationVisitor(declaration.getModifiers());
            visitor.visitDeclarations(declaration.getDeclarations());
            return null;
        }
    }

    private final class InterfaceDeclarationVisitor extends ExceptionVisitor<Void, Void> {

        private final LinkedList<TypeElement> modifiers;

        private InterfaceDeclarationVisitor(LinkedList<TypeElement> modifiers) {
            this.modifiers = modifiers;
        }

        /*
         * Environment already contains declarations.
         */

        public void visitDeclarations(LinkedList<Declaration> declarations) {
            for (Declaration declaration : declarations) {
                if (declaration == null) {
                    continue;
                }
                declaration.accept(this, null);
            }
        }

        public Void visitErrorDecl(ErrorDecl declaration, Void arg) {
            /* ignore */
            return null;
        }

        @Override
        public Void visitEmptyDecl(EmptyDecl declaration, Void arg) {
            /* ignore, additional semicolon */
            return null;
        }

        public Void visitVariableDecl(VariableDecl declaration, Void arg) {
            final Declarator declarator = declaration.getDeclarator().get();
            try {
                @SuppressWarnings("UnusedDeclaration")
                final FunctionDeclarator funDeclarator = DeclaratorUtils.getFunctionDeclarator(
                        declaration.getDeclarator().get());
            } catch (Exception e) {
                errorHelper.error(declarator.getLocation(), Optional.of(declarator.getEndLocation()),
                        format("only commands and events can be defined in interfaces"));
                return null;
            }
            final FunctionDeclaration funDeclaration = (FunctionDeclaration) declaration.getDeclaration();

            final FunctionDeclaration.FunctionType functionType = getFunctionType(modifiers);
            funDeclaration.setFunctionType(functionType);
            return null;
        }

        // FIXME: remove (almost the same code in TypeElementUtils)
        private FunctionDeclaration.FunctionType getFunctionType(LinkedList<TypeElement> qualifiers) {
            // FIXME: temporary solution, this kind of information should be
            // kept in type object
            for (TypeElement element : qualifiers) {
                if (element instanceof Rid) {
                    final Rid rid = (Rid) element;
                    if (rid.getId() == RID.COMMAND) {
                        return FunctionDeclaration.FunctionType.COMMAND;
                    }
                    if (rid.getId() == RID.EVENT) {
                        return FunctionDeclaration.FunctionType.EVENT;
                    }
                }
            }
            return FunctionDeclaration.FunctionType.NORMAL;
        }

        // TODO: all other declarations should be reported as errors
    }

}
