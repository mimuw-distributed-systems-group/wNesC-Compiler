package pl.edu.mimuw.nesc.compilation;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pl.edu.mimuw.nesc.ContextRef;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.Frontend;
import pl.edu.mimuw.nesc.NescFrontend;
import pl.edu.mimuw.nesc.ProjectData;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExprTransformer;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.Module;
import pl.edu.mimuw.nesc.ast.gen.ModuleImpl;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;
import pl.edu.mimuw.nesc.atomic.AtomicBlockData;
import pl.edu.mimuw.nesc.atomic.AtomicTransformer;
import pl.edu.mimuw.nesc.basicreduce.BasicReduceExecutor;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.connect.ConnectExecutor;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.finalanalysis.FinalAnalyzer;
import pl.edu.mimuw.nesc.finalreduce.ExternalExprBlockData;
import pl.edu.mimuw.nesc.finalreduce.ExternalExprTransformer;
import pl.edu.mimuw.nesc.finalreduce.FinalTransformer;
import pl.edu.mimuw.nesc.finalreduce.OffsetofTransformation;
import pl.edu.mimuw.nesc.fold.FoldExecutor;
import pl.edu.mimuw.nesc.instantiation.CyclePresentException;
import pl.edu.mimuw.nesc.instantiation.InstantiateExecutor;
import pl.edu.mimuw.nesc.intermediate.TraversingIntermediateGenerator;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.optimization.AtomicOptimizer;
import pl.edu.mimuw.nesc.optimization.DeclarationsCleaner;
import pl.edu.mimuw.nesc.optimization.LinkageOptimizer;
import pl.edu.mimuw.nesc.optimization.TaskOptimizationChecker;
import pl.edu.mimuw.nesc.optimization.TaskOptimizer;
import pl.edu.mimuw.nesc.optimization.UnexpectedWiringException;
import pl.edu.mimuw.nesc.option.OptionsProvider;
import pl.edu.mimuw.nesc.problem.NescError;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.NescIssueComparator;
import pl.edu.mimuw.nesc.problem.NescWarning;
import pl.edu.mimuw.nesc.problem.issue.InstantiationCycleError;
import pl.edu.mimuw.nesc.problem.issue.Issue;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;
import pl.edu.mimuw.nesc.wiresgraph.WiresGraph;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * <p>Class responsible for performing all compilation steps that transform
 * a NesC program to an equivalent C program. The C program is represented by
 * C declarations and definitions in proper order.</p>
 *
 * <p>A single compilation executor recognizes a fixed set of target attributes
 * that is specified by arguments for the constructor. When the no-argument
 * constructor is used, then the compilation executor will not recognize any
 * target attributes.</p>
 *
 * <p>If an error is detected in the code the compilation that is performed by
 * the executor is aborted by throwing {@link ErroneousIssueException}.
 * Moreover, currently, when an error happens the whole Java virtual machine
 * can be terminated by the frontend (see
 * {@link pl.edu.mimuw.nesc.NescFrontend}).</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CompilationExecutor {
    /**
     * The frontend instance that will be used by this executor.
     */
    private final Frontend frontend;

    /**
     * The listener that will be notified about events generated during the
     * compilation.
     */
    private Optional<CompilationListener> listener;

    /**
     * Visitor that notifies the listener about visited issues.
     */
    private final NescIssue.Visitor<Void, Void> issueNotifier = new NescIssue.Visitor<Void, Void>() {
        @Override
        public Void visit(NescError error, Void arg) {
            if (listener.isPresent()) {
                listener.get().error(error);
            }
            return null;
        }

        @Override
        public Void visit(NescWarning warning, Void arg) {
            if (listener.isPresent()) {
                listener.get().warning(warning);
            }
            return null;
        }
    };

    /**
     * Initialize this compilation executor not to recognize any target
     * attributes.
     */
    public CompilationExecutor() {
        this(Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    /**
     * Initialize this compilation executor to recognize given target
     * attributes.
     *
     * @param targetAttributes0 Iterable with no-parameter target attributes
     *                          that will be recognized by the frontend used by
     *                          this executor.
     * @param targetAttributes1 Iterable with one-parameter target attributes
     *                          that will be recognized by this executor.
     */
    public CompilationExecutor(Iterable<String> targetAttributes0, Iterable<String> targetAttributes1) {
        this.frontend = NescFrontend.builder()
                .standalone(true)
                .addTargetAttributes0(targetAttributes0)
                .addTargetAttributes1(targetAttributes1)
                .build();
        this.listener = Optional.absent();
    }

    /**
     * Set the listener that will be notified about compilation events.
     * Listener that has been previously set will not be notified about
     * events.
     *
     * @param listener Listener to set.
     */
    public void setListener(CompilationListener listener) {
        checkNotNull(listener, "listener cannot be null");
        this.listener = Optional.of(listener);
    }

    /**
     * Performs the compilation of the program specified by options that are
     * provided by given options provider.
     *
     * @param provider Provider of the frontend options.
     * @return Result of the compilation that contains the list of
     *         C declarations and definitions.
     * @throws ErroneousIssueException An error in the program is detected.
     */
    public CompilationResult compile(OptionsProvider provider)
            throws ErroneousIssueException, InvalidOptionsException {
        return compile(new ProvidedParamsContextCreator(provider));
    }

    /**
     * Performs the compilation of the program specified by given arguments to
     * the frontend.
     *
     * @param frontendArgs Parameters to the NesC frontend that specify the
     *                     program to compile.
     * @return Result of the compilation that contains the list of
     *         C declarations and definitions.
     * @throws ErroneousIssueException An error in the program is detected.
     */
    public CompilationResult compile(String[] frontendArgs)
            throws ErroneousIssueException, InvalidOptionsException {
        return compile(new RawParamsContextCreator(frontendArgs));
    }

    private CompilationResult compile(ContextCreator contextCreator)
            throws InvalidOptionsException, ErroneousIssueException {

        final ProjectData projectData = load(contextCreator);
        handleIssues(projectData);
        final Optional<Configuration> taskWiringConf = basicReduce(projectData);
        collectUniqueNames(projectData);
        final Set<Component> instantiatedComponents = instantiate(projectData, taskWiringConf);
        fold(projectData, taskWiringConf, instantiatedComponents);
        performFinalAnalysis(projectData, taskWiringConf, instantiatedComponents);
        final WiresGraph wiring = connect(projectData, taskWiringConf, instantiatedComponents);
        final ImmutableMap<String, String> combiningFunsAfterMangling =
                stripCombiningFunsMangling(projectData);
        final Multimap<String, FunctionDecl> intermediateFuns = generateIntermediateFuns(wiring,
                combiningFunsAfterMangling, projectData.getNameMangler());
        finalReduce(projectData, taskWiringConf, instantiatedComponents, wiring);
        final ImmutableList<Declaration> finalCode = generate(projectData, instantiatedComponents,
                intermediateFuns.values());
        final ReferencesGraph refsGraph = buildReferencesGraph(finalCode);
        final ImmutableList<Declaration> cleanedCode = optimize(projectData,
                wiring, finalCode, refsGraph, AtomicSpecification.DEFAULT_SPECIFICATION);
        reduceAtomic(projectData, cleanedCode);

        return new CompilationResult(cleanedCode, projectData.getNameMangler(),
                refsGraph, projectData.getOutputFile(), projectData.getExternalVariables(),
                projectData.getExternalVariablesFile());
    }

    /**
     * Executes the load phase of the compilation. All files are parsed and
     * analyzed.
     *
     * @return Data about loaded project.
     */
    private ProjectData load(ContextCreator contextCreator) throws InvalidOptionsException{
        final ProjectData projectData = frontend.build(contextCreator.createContext());
        if (projectData.getIssues().isEmpty()) {
            projectData.getNameMangler().addForbiddenNames(projectData.getGlobalNames().values());
        }
        return projectData;
    }

    /**
     * Executes the basic reduce phase of compilation.
     *
     * @param projectData Data about loaded project.
     * @return Configuration that wires created task interfaces from non-generic
     *         modules (if it is necessary).
     */
    private Optional<Configuration> basicReduce(ProjectData projectData) {
        final BasicReduceExecutor.Builder executorBuilder = BasicReduceExecutor.builder()
                .nameMangler(projectData.getNameMangler())
                .schedulerSpecification(projectData.getSchedulerSpecification().get())
                .putGlobalNames(projectData.getGlobalNames());

        // Add all declarations from the whole project

        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent()) {
                executorBuilder.addNode(fileData.getEntityRoot().get());
            }
            executorBuilder.addNodes(fileData.getExtdefs());
        }

        return executorBuilder.build().reduce();
    }

    /**
     * Perform collecting unique names in the module table of modules that are
     * not generic.
     *
     * @param projectData Data about loaded project.
     */
    private void collectUniqueNames(ProjectData projectData) {
        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent() && fileData.getEntityRoot().get() instanceof Module) {

                final Module module = (Module) fileData.getEntityRoot().get();
                if (!module.getIsAbstract()) {
                    module.getModuleTable().collectUniqueNames((ModuleImpl) module.getImplementation());
                }
            }
        }
    }

    /**
     * Perform the instantiation stage of compilation. If a cycle in creation of
     * components is detected, then the compiler exits with error. Otherwise,
     * the compilation continues.
     *
     * @param projectData Data about loaded project.
     * @param taskWiringConf Configuration that wires task interfaces from
     *                       non-generic modules.
     * @return Set with instantiated components.
     */
    private Set<Component> instantiate(ProjectData projectData, Optional<Configuration> taskWiringConf)
                throws ErroneousIssueException {
        final InstantiateExecutor.Builder executorBuilder = InstantiateExecutor.builder()
                .nameMangler(projectData.getNameMangler());

        // Add all components and interfaces

        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent()) {
                executorBuilder.addNescDeclaration((NescDecl) fileData.getEntityRoot().get());
            }
        }

        if (taskWiringConf.isPresent()) {
            executorBuilder.addNescDeclaration(taskWiringConf.get());
        }

        try {
            return executorBuilder.build().instantiate();
        } catch (CyclePresentException e) {
            final Issue issue = new InstantiationCycleError(e.getMessage());
            new NescError(Optional.<Location>absent(), Optional.<Location>absent(),
                        Optional.of(issue.getCode()), issue.generateDescription())
                    .accept(issueNotifier, null);
            throw new ErroneousIssueException();
        }
    }

    /**
     * Fold calls to constant functions.
     *
     * @param projectData Data about loaded project.
     * @param taskWiringConf Configuration that wires task interfaces from
     *                       non-generic components.
     * @param instantiatedComponents Instantiated components.
     */
    private void fold(ProjectData projectData, Optional<Configuration> taskWiringConf,
            Set<Component> instantiatedComponents) {
        final FoldExecutor.Builder executorBuilder = FoldExecutor.builder()
                .addNodes(instantiatedComponents);

        // Add nodes with constant functions to fold

        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent()) {
                if (!(fileData.getEntityRoot().get() instanceof Component)) {
                    // Generic interfaces are folded exactly once
                    executorBuilder.addNode(fileData.getEntityRoot().get());
                } else {
                    final Component component = (Component) fileData.getEntityRoot().get();
                    if (!component.getIsAbstract()) {
                        executorBuilder.addNode(component);
                    }
                }
            }
            executorBuilder.addNodes(fileData.getExtdefs());
        }

        if (taskWiringConf.isPresent()) {
            executorBuilder.addNode(taskWiringConf.get());
        }

        // Fold calls to constant functions

        executorBuilder.build().fold();
    }

    /**
     * Runs the final analysis for constraints that can only be fully checked
     * after instantiation of components. If errors are detected, the
     * compilation is terminated with error messages.
     *
     * @param projectData Data about loaded project.
     * @param taskWiringConf Configuration that wires task interfaces from
     *                       non-generic components.
     * @param instantiatedComponents Set with components that have been
     *                               instantiated.
     */
    private void performFinalAnalysis(ProjectData projectData, Optional<Configuration> taskWiringConf,
            Set<Component> instantiatedComponents) throws ErroneousIssueException {
        final FinalAnalyzer finalAnalyzer = new FinalAnalyzer(projectData.getABI());
        final TaskOptimizationChecker.Builder taskOptCheckerBuilder =
                TaskOptimizationChecker.builder(projectData.getSchedulerSpecification().get());

        // Analyze all declarations except interfaces and generic components

        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent()
                    && fileData.getEntityRoot().get() instanceof Component) {
                final Component component = (Component) fileData.getEntityRoot().get();
                if (!component.getIsAbstract()) {
                    finalAnalyzer.analyze(component);
                    taskOptCheckerBuilder.addDeclaration(component);
                }
            }

            for (Declaration extDeclaration : fileData.getExtdefs()) {
                finalAnalyzer.analyze(extDeclaration);
            }
            taskOptCheckerBuilder.addDeclarations(fileData.getExtdefs());
        }

        for (Component component : instantiatedComponents) {
            finalAnalyzer.analyze(component);
        }
        taskOptCheckerBuilder.addDeclarations(instantiatedComponents);

        if (taskWiringConf.isPresent()) {
            finalAnalyzer.analyze(taskWiringConf.get());
            taskOptCheckerBuilder.addDeclaration(taskWiringConf.get());
        }

        // Perform the analysis and handle issues

        final List<NescIssue> issues = new ArrayList<>(finalAnalyzer.getIssues().values());

        if (projectData.getOptimizeTasks()) {
            final Optional<NescWarning> warning = taskOptCheckerBuilder.build().check();
            if (warning.isPresent()) {
                projectData.setOptimizeTasks(false);
                issues.add(new NescWarning(warning.get().getStartLocation(), warning.get().getEndLocation(),
                        format("tasks optimization disabled because %s", warning.get().getMessage())));
            }
        }

        handleIssues(issues);
    }

    /**
     * Create the wiring graph.
     *
     * @param projectData Data about loaded project.
     * @param taskWiringConf Configuration that wires task interfaces from
     *                       non-generic components.
     * @param instantiatedComponents Set with components that have been
     *                               instantiated.
     * @return The created graph.
     */
    private WiresGraph connect(ProjectData projectData, Optional<Configuration> taskWiringConf,
            Set<Component> instantiatedComponents) {
        final ConnectExecutor.Builder executorBuilder = ConnectExecutor.builder(
                projectData.getNameMangler(), projectData.getABI())
                .addNescDeclarations(instantiatedComponents);

        // Add all necessary nodes

        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent()) {
                if (!(fileData.getEntityRoot().get() instanceof Component)) {
                    executorBuilder.addNescDeclaration((NescDecl) fileData.getEntityRoot().get());
                } else {
                    final Component component = (Component) fileData.getEntityRoot().get();
                    if (!component.getIsAbstract()) {
                        executorBuilder.addNescDeclaration(component);
                    }
                }
            }
        }

        if (taskWiringConf.isPresent()) {
            executorBuilder.addNescDeclaration(taskWiringConf.get());
        }

        // Create the graph

        return executorBuilder.build().connect();
    }

    /**
     * Get the map of combining functions with type definitions with their final
     * unique names as keys.
     *
     * @param projectData Data about the project.
     * @return Map with final unique names of type definitions as keys.
     */
    private ImmutableMap<String, String> stripCombiningFunsMangling(ProjectData projectData) {
        final ImmutableMap<String, String> globalNames = projectData.getGlobalNames();
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        for (Map.Entry<String, String> combiner : projectData.getCombiningFunctions().entrySet()) {
            final String finalTypedefUniqueName = globalNames.containsKey(combiner.getKey())
                    ? globalNames.get(combiner.getKey())
                    : combiner.getKey();
            builder.put(finalTypedefUniqueName, combiner.getValue());
        }

        return builder.build();
    }

    /**
     * Perform the generation of all necessary intermediate functions.
     *
     * @return Multimap with intermediate functions.
     */
    private Multimap<String, FunctionDecl> generateIntermediateFuns(WiresGraph graph,
            ImmutableMap<String, String> combiningFuns, NameMangler nameMangler) {
        return new TraversingIntermediateGenerator(graph, combiningFuns, nameMangler).generate();
    }

    /**
     * Perform the final reduce, e.g. reduce 'call' and 'signal' expressions and
     * remove NesC attributes.
     */
    private void finalReduce(ProjectData projectData, Optional<Configuration> taskWiringConf,
            Set<Component> instantiatedComponents, WiresGraph graph) {
        final FinalTransformer transformer = new FinalTransformer(graph, projectData.getABI());
        final ExternalExprTransformer externalTransformer = new ExternalExprTransformer(
                projectData.getNameMangler());
        final ExternalExprBlockData blockData = new ExternalExprBlockData();
        final ExprTransformer<Void> offsetofTransformer = new ExprTransformer<>(
                new OffsetofTransformation(projectData.getABI()));

        for (Component component : instantiatedComponents) {
            component.traverse(transformer, Optional.<String>absent());
            component.traverse(externalTransformer, blockData);
            component.traverse(offsetofTransformer, null);
        }

        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent()
                    && fileData.getEntityRoot().get() instanceof Component) {
                final Component component = (Component) fileData.getEntityRoot().get();
                if (!component.getIsAbstract()) {
                    component.traverse(transformer, Optional.<String>absent());
                    component.traverse(externalTransformer, blockData);
                    component.traverse(offsetofTransformer, null);
                }
            }
            for (Declaration declaration : fileData.getExtdefs()) {
                declaration.traverse(transformer, Optional.<String>absent());
                declaration.traverse(externalTransformer, blockData);
                declaration.traverse(offsetofTransformer, null);
            }
        }

        if (taskWiringConf.isPresent()) {
            taskWiringConf.get().traverse(transformer, Optional.<String>absent());
            taskWiringConf.get().traverse(externalTransformer, blockData);
            taskWiringConf.get().traverse(offsetofTransformer, null);
        }
    }

    /**
     * Generate a final list with all C declarations that constitute the NesC
     * program.
     */
    private ImmutableList<Declaration> generate(
            ProjectData projectData,
            Set<Component> instantiatedComponents,
            Collection<FunctionDecl> intermediateFuns
    ) {
        final String mainConfigName = ((NescDecl) projectData.getRootFileData().get().getEntityRoot().get())
                .getName().getName();
        return DefaultCodeGenerator.builder(mainConfigName)
                .addOriginalData(projectData.getFileDatas().values())
                .addIntermediateFunctions(intermediateFuns)
                .addInstantiatedComponents(instantiatedComponents)
                .addDefaultIncludeFiles(projectData.getDefaultIncludeFiles())
                .build()
                .generate();
    }

    /**
     * Create the graph of references between given top-level declarations.
     *
     * @param topLevelDecls List with top-level declarations.
     * @return The built references graph.
     */
    private ReferencesGraph buildReferencesGraph(ImmutableList<Declaration> topLevelDecls) {
        return ReferencesGraph.builder()
                .addDeclarations(topLevelDecls)
                .build();
    }

    /**
     * Get a list of declarations after filtering it by removing unnecessary
     * declarations and after performing other optimizations.
     */
    private ImmutableList<Declaration> optimize(ProjectData projectData, WiresGraph wiresGraph,
                ImmutableList<Declaration> declarations, ReferencesGraph refsGraph,
                AtomicSpecification atomicSpecification) {
        final ImmutableList<Declaration> afterCleaning = DeclarationsCleaner.builder(refsGraph)
                .addDeclarations(declarations)
                .addPreservedObject(atomicSpecification.getTypename())
                .addPreservedObject(atomicSpecification.getStartFunctionName())
                .addPreservedObject(atomicSpecification.getEndFunctionName())
                .build()
                .clean();
        final ImmutableList<Declaration> afterLinkageOptimization =
                new LinkageOptimizer(projectData.getNameMangler())
                        .optimize(afterCleaning);
        final ImmutableList<Declaration> afterTaskOptimization = performTasksOptimization(
                projectData, wiresGraph, afterLinkageOptimization, refsGraph);

        if (projectData.getOptimizeAtomic()) {
            new AtomicOptimizer(afterTaskOptimization, refsGraph).optimize();
        }

        return afterTaskOptimization;
    }

    private ImmutableList<Declaration> performTasksOptimization(ProjectData projectData,
            WiresGraph wiresGraph, ImmutableList<Declaration> declarations,
            ReferencesGraph refsGraph) {
        if (projectData.getOptimizeTasks()) {
            try {
                return new TaskOptimizer(declarations, wiresGraph, refsGraph,
                        projectData.getSchedulerSpecification().get())
                        .optimize();
            } catch (UnexpectedWiringException e) {
                new NescWarning(Optional.<Location>absent(), Optional.<Location>absent(),
                            "tasks optimization disabled because " + e.getMessage())
                        .accept(issueNotifier, null);
                return declarations;
            }
        } else {
            return declarations;
        }
    }

    private void reduceAtomic(ProjectData projectData, ImmutableList<Declaration> declarations) {
        final AtomicTransformer atomicTransformer = new AtomicTransformer(
                AtomicSpecification.DEFAULT_SPECIFICATION, projectData.getNameMangler());
        final AtomicBlockData initialData = AtomicBlockData.newInitialData();

        for (Declaration declaration : declarations) {
            declaration.traverse(atomicTransformer, initialData);
        }
    }

    /**
     * Checks if there are some issues in the given project. If some errors are
     * present, the listener is notified about them and the compilation is
     * terminated by throwing {@link ErroneousIssueException}. Otherwise, the
     * listener is notified about warnings and compilation continues.
     *
     * @param projectData Project data with potential issues.
     */
    private void handleIssues(ProjectData projectData) throws ErroneousIssueException {
        final List<NescIssue> issues = new ArrayList<>();

        // Collect all issues in the project

        issues.addAll(projectData.getIssues());

        for (FileData fileData : projectData.getFileDatas().values()) {
            issues.addAll(fileData.getIssues().values());
        }

        handleIssues(issues);
    }

    /**
     * Notify the listener about given issues and terminate the compilation if
     * at least one of them is an error.
     *
     * @param issues List with issues to handle.
     */
    private void handleIssues(List<NescIssue> issues) throws ErroneousIssueException {
        // Sort issues
        Collections.sort(issues, new NescIssueComparator());

        // Notify about issues and exit if an error is present

        boolean errorPresent = false;
        for (NescIssue issue : issues) {
            issue.accept(issueNotifier, null);

            if (issue instanceof NescError) {
                errorPresent = true;
            }
        }

        if (errorPresent) {
            throw new ErroneousIssueException();
        }
    }

    /**
     * Interface for creating the context for the frontend.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface ContextCreator {
        /**
         * Create the context for the frontend.
         *
         * @return Newly created frontend context.
         * @throws InvalidOptionsException One of the options for the frontend
         *                                 is invalid.
         */
        ContextRef createContext() throws InvalidOptionsException;
    }

    /**
     * Object that supplies frontend context by using raw parameters that has
     * not been yet parsed.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class RawParamsContextCreator implements ContextCreator {
        /**
         * The parameters for the frontend.
         */
        private final String[] parameters;

        private RawParamsContextCreator(String[] args) {
            checkNotNull(args, "parameters cannot be null");
            this.parameters = args;
        }

        @Override
        public ContextRef createContext() throws InvalidOptionsException {
            return CompilationExecutor.this.frontend.createContext(parameters);
        }
    }

    /**
     * Object that will create a context using provided options.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class ProvidedParamsContextCreator implements ContextCreator {
        /**
         * Object that will provide the options for the frontend.
         */
        private final OptionsProvider provider;

        private ProvidedParamsContextCreator(OptionsProvider provider) {
            checkNotNull(provider, "options provider cannot be null");
            this.provider = provider;
        }

        @Override
        public ContextRef createContext() throws InvalidOptionsException {
            return CompilationExecutor.this.frontend.createContext(provider);
        }
    }
}
