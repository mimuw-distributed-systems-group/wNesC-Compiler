package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.problem.NescIssue;

import static com.google.common.base.Preconditions.checkState;

/**
 * Contains the result of parsing process for entire project.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ProjectData {

    public static Builder builder() {
        return new Builder();
    }

    private final ImmutableMap<String, FileData> fileDatas;
    private final ImmutableList<String> defaultIncludeFiles;
    private final ImmutableMap<String, String> globalNames;
    private final ImmutableMap<String, String> combiningFunctions;
    private final Optional<FileData> rootFileData;
    private final ImmutableList<NescIssue> issues;
    private final NameMangler nameMangler;
    private final Optional<SchedulerSpecification> schedulerSpecification;
    private final AtomicSpecification atomicSpecification;
    private final String outputFile;
    private final ABI abi;
    private final SetMultimap<Optional<String>, String> externalVariables;
    private final Optional<String> externalVariablesFile;
    private final boolean optimizeAtomic;
    private boolean optimizeTasks;

    private ProjectData(Builder builder) {
        builder.buildMaps();

        this.fileDatas = builder.fileDatas;
        this.defaultIncludeFiles = builder.defaultIncludedFilesBuilder.build();
        this.globalNames = builder.globalNames;
        this.combiningFunctions = builder.combiningFunctions;
        this.rootFileData = Optional.fromNullable(builder.rootFileData);
        this.issues = builder.issueListBuilder.build();
        this.nameMangler = builder.nameMangler;
        this.schedulerSpecification = builder.schedulerSpecification;
        this.atomicSpecification = builder.atomicSpecification;
        this.outputFile = builder.outputFile;
        this.abi = builder.abi;
        this.externalVariables = builder.externalVariables;
        this.externalVariablesFile = builder.externalVariablesFile;
        this.optimizeAtomic = builder.optimizeAtomic;
        this.optimizeTasks = builder.optimizeTasks;
    }

    public ImmutableMap<String, FileData> getFileDatas() {
        return fileDatas;
    }

    /**
     * Get the list with names of files that are included by default for this
     * project. They are not necessarily present in the map of file datas of
     * this object.
     *
     * @return List with paths of files included by default.
     */
    public ImmutableList<String> getDefaultIncludeFiles() {
        return defaultIncludeFiles;
    }

    /**
     * Map with unique names of entities that are to be located in the global
     * scope mapped to their normal names as they appear in the code. The names
     * are for the entire project.
     *
     * @return Mapping of unique names of global entities to their normal names.
     */
    public ImmutableMap<String, String> getGlobalNames() {
        return globalNames;
    }

    /**
     * Get the map with unique names of type definitions as keys and names of
     * combining functions associated with them as values. The map contains
     * information about combining functions from the entire project.
     *
     * @return Mapping of unique names of typedefs to names of combining
     *         functions associated with them.
     */
    public ImmutableMap<String, String> getCombiningFunctions() {
        return combiningFunctions;
    }

    public Optional<FileData> getRootFileData() {
        return rootFileData;
    }

    public ImmutableList<NescIssue> getIssues() {
        return issues;
    }

    /**
     * <p>Get name mangler that has been used to mangle names in the NesC
     * application and can be used to generate new unique names for it.
     * If the frontend instance used to create this project data processes
     * files, then the state of the mangler can change.</p>
     *
     * <p>If this project contains some errors, then the returned value may be
     * <code>null</code>.</p>
     *
     * @return Name mangler for further unique names generation.
     */
    public NameMangler getNameMangler() {
        return nameMangler;
    }

    /**
     * <p>Get the specification of the scheduler to use for this NesC project.
     * It is absent if the scheduler has not been set by an appropriate option
     * to the compiler.</p>
     *
     * <p>If this project contains some errors, the returned scheduler may be
     * absent even if specified by a compiler option.</p>
     *
     * @return Specification of the scheduler to use for this NesC application.
     */
    public Optional<SchedulerSpecification> getSchedulerSpecification() {
        return schedulerSpecification;
    }

    /**
     * <p>Get the name of the output C file to be generated by the compiler.</p>
     *
     * @return Name of the output file to generate.
     */
    public String getOutputFile() {
        return outputFile;
    }

    /**
     * <p>Get ABI of the project.</p>
     *
     * @return ABI used in this project.
     */
    public ABI getABI() {
        return abi;
    }

    /**
     * <p>Get a multimap with names of external variables as values. Variables
     * with a specific key can only appear inside the implementation scope of
     * a module with the name from the key. If it is absent, the variable can
     * appear in the global scope. The returned set multimap is unmodifiable and
     * its entries are returned by the iterator in the same order as they were
     * given in the compiler option.</p>
     *
     * @return Map with names of external variables.
     */
    public SetMultimap<Optional<String>, String> getExternalVariables() {
        return externalVariables;
    }

    /**
     * <p>Get the name of the external variables file to create. The file
     * contains unique names of external variables that appeared in the
     * application in the same order as in the external variables option.</p>
     *
     * @return Name of the external variables file to create. If it is absent,
     *         no external variables file should be created.
     */
    public Optional<String> getExternalVariablesFile() {
        return externalVariablesFile;
    }

    /**
     * <p>Check if the atomic optimization should be performed for this project.
     * </p>
     *
     * @return <code>true</code> if and only if the atomic optimization should
     *         be performed for this project.
     */
    public boolean getOptimizeAtomic() {
        return optimizeAtomic;
    }

    /**
     * <p>Check if the tasks optimization should be performed for this project.
     * </p>
     *
     * @return <code>true</code> if and only if the tasks optimization should be
     *         performed for this project.
     */
    public boolean getOptimizeTasks() {
        return optimizeTasks;
    }

    /**
     * <p>Set the value indicating if the tasks optimization should be performed
     * to the given value.</p>
     */
    public void setOptimizeTasks(boolean value) {
        this.optimizeTasks = value;
    }

    /**
     * <p>Get the atomic specification for this project.</p>
     *
     * @return The atomic specification.
     */
    public AtomicSpecification getAtomicSpecification() {
        return atomicSpecification;
    }

    /**
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder {

        private ImmutableMap.Builder<String, FileData> fileDataBuilder;
        private ImmutableList.Builder<String> defaultIncludedFilesBuilder;
        private ImmutableList.Builder<NescIssue> issueListBuilder;
        private FileData rootFileData;

        private ImmutableMap<String, FileData> fileDatas;
        private ImmutableMap<String, String> globalNames;
        private ImmutableMap<String, String> combiningFunctions;

        private NameMangler nameMangler;
        private Optional<SchedulerSpecification> schedulerSpecification = Optional.absent();
        private AtomicSpecification atomicSpecification;
        private SetMultimap<Optional<String>, String> externalVariables;
        private String outputFile;
        private ABI abi;
        private Optional<String> externalVariablesFile = Optional.absent();

        private boolean optimizeAtomic;
        private boolean optimizeTasks;

        public Builder() {
            this.fileDataBuilder = ImmutableMap.builder();
            this.defaultIncludedFilesBuilder = ImmutableList.builder();
            this.issueListBuilder = ImmutableList.builder();
        }

        public Builder addRootFileData(FileData fileData) {
            this.rootFileData = fileData;
            return this;
        }

        public Builder addFileDatas(Collection<FileData> fileDatas) {
            for (FileData data : fileDatas) {
                this.fileDataBuilder.put(data.getFilePath(), data);
            }
            return this;
        }

        public Builder addDefaultIncludeFiles(List<String> filePaths) {
            this.defaultIncludedFilesBuilder.addAll(filePaths);
            return this;
        }

        public Builder addIssue(NescIssue issue) {
            this.issueListBuilder.add(issue);
            return this;
        }

        public Builder addIssues(Collection<NescIssue> issues) {
            this.issueListBuilder.addAll(issues);
            return this;
        }

        public Builder externalVariables(SetMultimap<Optional<String>, String> externalVariables) {
            this.externalVariables = externalVariables;
            return this;
        }

        public Builder nameMangler(NameMangler nameMangler) {
            this.nameMangler = nameMangler;
            return this;
        }

        public Builder schedulerSpecification(SchedulerSpecification schedulerSpec) {
            this.schedulerSpecification = Optional.fromNullable(schedulerSpec);
            return this;
        }

        public Builder atomicSpecification(AtomicSpecification atomicSpec) {
            this.atomicSpecification = atomicSpec;
            return this;
        }

        public Builder outputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public Builder abi(ABI abi) {
            this.abi = abi;
            return this;
        }

        public Builder externalVariablesFile(String externalVariablesFile) {
            this.externalVariablesFile = Optional.fromNullable(externalVariablesFile);
            return this;
        }

        public Builder optimizeAtomic(boolean optimizeAtomic) {
            this.optimizeAtomic = optimizeAtomic;
            return this;
        }

        public Builder optimizeTasks(boolean optimizeTasks) {
            this.optimizeTasks = optimizeTasks;
            return this;
        }

        public ProjectData build() {
            verify();
            return new ProjectData(this);
        }

        private void verify() {
        }

        private void buildMaps() {
            this.fileDatas = fileDataBuilder.build();

            final Map<String, String> globalNames = new HashMap<>();
            final ImmutableMap.Builder<String, String> combiningFunctionsBuilder = ImmutableMap.builder();

            for (FileData fileData : this.fileDatas.values()) {
                combiningFunctionsBuilder.putAll(fileData.getCombiningFunctions());

                for (Map.Entry<String, String> globalNameEntry : fileData.getGlobalNames().entrySet()) {
                    if (!globalNames.containsKey(globalNameEntry.getKey())) {
                        globalNames.put(globalNameEntry.getKey(), globalNameEntry.getValue());
                    } else {
                        checkState(globalNames.get(globalNameEntry.getKey()).equals(globalNameEntry.getValue()),
                                "different names have been mangled to the same unique name '%s'",
                                globalNameEntry.getKey());
                    }
                }
            }

            this.globalNames = ImmutableMap.copyOf(globalNames);
            this.combiningFunctions = combiningFunctionsBuilder.build();
        }
    }
}
