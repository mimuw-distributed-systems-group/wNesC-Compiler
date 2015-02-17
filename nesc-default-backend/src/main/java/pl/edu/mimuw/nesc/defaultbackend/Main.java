package pl.edu.mimuw.nesc.defaultbackend;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.file.Paths;
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
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.Module;
import pl.edu.mimuw.nesc.ast.gen.ModuleImpl;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;
import pl.edu.mimuw.nesc.astwriting.WriteSettings;
import pl.edu.mimuw.nesc.basicreduce.BasicReduceExecutor;
import pl.edu.mimuw.nesc.connect.ConnectExecutor;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.finalanalysis.FinalAnalyzer;
import pl.edu.mimuw.nesc.finalreduce.ExternalExprBlockData;
import pl.edu.mimuw.nesc.finalreduce.ExternalExprTransformer;
import pl.edu.mimuw.nesc.fold.FoldExecutor;
import pl.edu.mimuw.nesc.instantiation.CyclePresentException;
import pl.edu.mimuw.nesc.instantiation.InstantiateExecutor;
import pl.edu.mimuw.nesc.finalreduce.FinalTransformer;
import pl.edu.mimuw.nesc.intermediate.SimpleIntermediateGenerator;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.problem.NescError;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.NescIssueComparator;
import pl.edu.mimuw.nesc.problem.NescWarning;
import pl.edu.mimuw.nesc.problem.issue.InstantiationCycleError;
import pl.edu.mimuw.nesc.problem.issue.Issue;
import pl.edu.mimuw.nesc.wiresgraph.WiresGraph;

/**
 * <p>Class with <code>main</code> method that allows usage of the compiler. It
 * performs all steps of the compilation of a NesC program in the default
 * backend.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Main {
    /**
     * String used as the separator for tokens of location.
     */
    private static final String SEPARATOR = ":";

    /**
     * Error code returned if the compilation succeeds.
     */
    private static final int STATUS_SUCCESS = 0;

    /**
     * Error code returned if the compilation fails.
     */
    private static final int STATUS_ERROR = 1;

    /**
     * Frontend instance used by the compiler.
     */
    private final Frontend frontend;

    /**
     * Context of the frontend used by the compiler.
     */
    private final ContextRef context;

    public static void main(String[] args) {
        try {
            new Main(args).compile();
            System.exit(STATUS_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error occurred: " + e.getMessage());
            System.exit(STATUS_ERROR);
        }
    }

    private Main(String[] args) throws InvalidOptionsException {
        this.frontend = NescFrontend.builder()
                .standalone(true)
                .build();
        this.context = this.frontend.createContext(args);
    }

    /**
     * Performs the whole compilation process. This method should be called
     * exactly once per instance of this class.
     */
    private void compile() {
        final ProjectData projectData = load();
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
        writeCode(projectData, finalCode);
    }

    /**
     * Executes the load phase of the compilation. All files are parsed and
     * analyzed.
     *
     * @return Data about loaded project.
     */
    private ProjectData load() {
        final ProjectData projectData = this.frontend.build(context);
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
    private Set<Component> instantiate(ProjectData projectData, Optional<Configuration> taskWiringConf) {
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
        } catch(CyclePresentException e) {
            final Issue issue = new InstantiationCycleError(e.getMessage());
            final NescIssue nescIssue = new NescError(Optional.<Location>absent(),
                    Optional.<Location>absent(), Optional.of(issue.getCode()),
                    issue.generateDescription());
            nescIssue.accept(new IssuePrinter(), null);
            System.exit(STATUS_ERROR);

            // unreachable code only to silence the compilation error
            throw new RuntimeException("this shall never be thrown");
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
                Set<Component> instantiatedComponents) {
        final FinalAnalyzer finalAnalyzer = new FinalAnalyzer(projectData.getABI());

        // Analyze all declarations except interfaces and generic components

        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent()
                    && fileData.getEntityRoot().get() instanceof Component) {
                final Component component = (Component) fileData.getEntityRoot().get();
                if (!component.getIsAbstract()) {
                    finalAnalyzer.analyze(component);
                }
            }

            for (Declaration extDeclaration : fileData.getExtdefs()) {
                finalAnalyzer.analyze(extDeclaration);
            }
        }

        for (Component component : instantiatedComponents) {
            finalAnalyzer.analyze(component);
        }

        if (taskWiringConf.isPresent()) {
            finalAnalyzer.analyze(taskWiringConf.get());
        }

        // Handle the issues

        final List<NescIssue> issues = new ArrayList<>(finalAnalyzer.getIssues().values());
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
        return new SimpleIntermediateGenerator(graph, combiningFuns, nameMangler).generate();
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

        for (Component component : instantiatedComponents) {
            component.traverse(transformer, Optional.<String>absent());
            component.traverse(externalTransformer, blockData);
        }

        for (FileData fileData : projectData.getFileDatas().values()) {
            if (fileData.getEntityRoot().isPresent()
                    && fileData.getEntityRoot().get() instanceof Component) {
                final Component component = (Component) fileData.getEntityRoot().get();
                if (!component.getIsAbstract()) {
                    component.traverse(transformer, Optional.<String>absent());
                    component.traverse(externalTransformer, blockData);
                }
            }
            for (Declaration declaration : fileData.getExtdefs()) {
                declaration.traverse(transformer, Optional.<String>absent());
                declaration.traverse(externalTransformer, blockData);
            }
        }

        if (taskWiringConf.isPresent()) {
            taskWiringConf.get().traverse(transformer, Optional.<String>absent());
            taskWiringConf.get().traverse(externalTransformer, blockData);
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
     * Write the generated code to the file.
     *
     * @param finalCode All declarations to write in proper order.
     */
    private void writeCode(ProjectData projectData, ImmutableList<Declaration> finalCode) {
        final WriteSettings writeSettings = WriteSettings.builder()
                .charset("UTF-8")
                .indentWithSpaces(3)
                .nameMode(WriteSettings.NameMode.USE_UNIQUE_NAMES)
                .uniqueMode(WriteSettings.UniqueMode.OUTPUT_VALUES)
                .build();

        try (ASTWriter writer = new ASTWriter(projectData.getOutputFile(), writeSettings)) {
            writer.write(finalCode);
        } catch(IOException e) {
            System.err.println("Cannot write the code to the file: " + e.getMessage());
            System.exit(STATUS_ERROR);
        }
    }

    /**
     * Checks if there are some issues in the given project. If some errors are
     * present, they are printed on stderr and the compiler exits with the error
     * status. Otherwise, warnings are printed and compilation continues.
     *
     * @param projectData Project data with potential issues.
     */
    private void handleIssues(ProjectData projectData) {
        final List<NescIssue> issues = new ArrayList<>();

        // Collect all issues in the project

        issues.addAll(projectData.getIssues());

        for (FileData fileData : projectData.getFileDatas().values()) {
            issues.addAll(fileData.getIssues().values());
        }

        handleIssues(issues);
    }

    /**
     * Print given issues to stderr and terminate the compilation if at
     * least one of them is an error.
     *
     * @param issues List with issues to handle.
     */
    private void handleIssues(List<NescIssue> issues) {
        // Sort issues
        Collections.sort(issues, new NescIssueComparator());

        // Print issues

        final IssuePrinter issuePrinter = new IssuePrinter();
        for (NescIssue issue : issues) {
            issue.accept(issuePrinter, null);
        }

        if (issuePrinter.errorPresent) {
            System.exit(STATUS_ERROR);
        }
    }

    /**
     * Visitor that prints issues it visits on stderr. Each printed issue is
     * ended by a newline character.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class IssuePrinter implements NescIssue.Visitor<Void, Void> {
        private boolean errorPresent = false;

        @Override
        public Void visit(NescWarning warning, Void arg) {
            printIssue("warning", warning);
            return null;
        }

        @Override
        public Void visit(NescError error, Void arg) {
            errorPresent = true;
            printIssue("error", error);
            return null;
        }

        private void printIssue(String issueType, NescIssue issue) {
            // Start location

            final Optional<Location> startLocation = issue.getStartLocation();
            if (startLocation.isPresent()) {
                System.err.print(Paths.get(startLocation.get().getFilePath()).getFileName());
                System.err.print(SEPARATOR);
                System.err.print(startLocation.get().getLine());
                System.err.print(SEPARATOR);
                System.err.print(startLocation.get().getColumn());
                System.err.print(SEPARATOR);
                System.err.print(" ");
            }

            // Type of the issue

            System.err.print(issueType);

            if (issue.getCode().isPresent()) {
                System.err.print(" ");
                System.err.print(issue.getCode().get().asString());
            }

            System.err.print(SEPARATOR);
            System.err.print(" ");

            // Message

            System.err.println(issue.getMessage());
        }
    }
}
