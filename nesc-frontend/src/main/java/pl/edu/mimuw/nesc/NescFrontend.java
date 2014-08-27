package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.common.NesCFileType;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.filesgraph.FilesGraph;
import pl.edu.mimuw.nesc.filesgraph.GraphFile;
import pl.edu.mimuw.nesc.filesgraph.visitor.DefaultFileGraphVisitor;
import pl.edu.mimuw.nesc.load.FileCache;
import pl.edu.mimuw.nesc.load.LoadExecutor;
import pl.edu.mimuw.nesc.option.OptionsHolder;
import pl.edu.mimuw.nesc.option.OptionsParser;
import pl.edu.mimuw.nesc.problem.NescError;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Frontend implementation for nesc language.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescFrontend implements Frontend {

    private static final Logger LOG = Logger.getLogger(NescFrontend.class);

    public static Builder builder() {
        return new Builder();
    }

    private final boolean isStandalone;
    private final Map<ContextRef, FrontendContext> contextsMap;

    private NescFrontend(Builder builder) {
        this.isStandalone = builder.isStandalone;
        this.contextsMap = new HashMap<>();
    }

    @Override
    public ContextRef createContext(String[] args) throws InvalidOptionsException {
        checkNotNull(args, "arguments cannot be null");
        LOG.info("Create context; " + Arrays.toString(args));

        final ContextRef contextRef = new ContextRef();
        final FrontendContext context = createContextFromOptions(args);
        this.contextsMap.put(contextRef, context);
        return contextRef;
    }

    @Override
    public void deleteContext(ContextRef contextRef) {
        LOG.info("Delete context; " + contextRef);
        checkNotNull(contextRef, "context reference cannot be null");
        this.contextsMap.remove(contextRef);
    }

    @Override
    public void updateSettings(ContextRef contextRef, String[] args) throws InvalidOptionsException {
        checkNotNull(args, "arguments cannot be null");
        LOG.info("Update settings; " + Arrays.toString(args));

        final OptionsParser optionsParser = new OptionsParser();
        try {
            final OptionsHolder options = optionsParser.parse(args);
            final FrontendContext context = getContext(contextRef);
            context.updateOptions(options);
        } catch (ParseException e) {
            final String msg = e.getMessage();
            throw new InvalidOptionsException(msg);
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public ProjectData build(ContextRef contextRef) {
        checkNotNull(contextRef, "context reference cannot be null");
        LOG.info("Rebuild; contextRef=" + contextRef);

        final FrontendContext context = getContext(contextRef).basicCopy();
        setContext(context, contextRef);

        final ProjectData.Builder projectDataBuilder;
        final Optional<String> startFile = getStartFile(context);

        parseFilesIncludedByDefault(context);
        context.setWasInitialBuild(true);

        if (startFile.isPresent()) {
            projectDataBuilder = _update(contextRef, startFile.get());
        } else {
            final String msg = format("Cannot find main configuration '%s'", context.getOptions().getEntryEntity());
            LOG.error(msg);
            final NescError error = new NescError(Optional.<Location>absent(), Optional.<Location>absent(), msg);
            projectDataBuilder = ProjectData.builder();
            projectDataBuilder.addIssue(error);
        }

        projectDataBuilder.addIssues(context.getIssues());
        return projectDataBuilder.build();
    }

    @Override
    public ProjectData update(ContextRef contextRef, String filePath) {
        checkNotNull(contextRef, "context reference cannot be null");
        checkNotNull(filePath, "file path cannot be null");
        final FrontendContext context = getContext(contextRef);

        if (context.wasInitialBuild()) {
            return _update(contextRef, filePath).build();
        } else {
            final ProjectData projectData = build(contextRef);
            context.setWasInitialBuild(true);
            return ProjectData.builder()
                    .addFileDatas(projectData.getFileDatas().values())
                    .addRootFileData(projectData.getFileDatas().get(filePath))
                    .addIssues(projectData.getIssues())
                    .build();
        }
    }

    private ProjectData.Builder _update(ContextRef contextRef, String filePath) {
        LOG.info("Update; contextRef=" + contextRef + "; filePath=" + filePath);

        final FrontendContext context = getContext(contextRef);
        clearDirtyFileCache(context, filePath);

        try {
            // FIXME: what if we would like to edit file included by default?
            List<FileCache> fileCacheList = new LoadExecutor(context).parse(filePath, false);
            final List<FileData> fileDatas = Lists.newArrayList();
            for (FileCache cache : fileCacheList) {
                if (cache.isRoot()) {
                    fileDatas.add(new FileData(cache));
                }
            }
            return ProjectData.builder()
                    .addFileDatas(fileDatas)
                    .addRootFileData(fileDatas.get(0));
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
        return ProjectData.builder();
    }

    private Optional<String> getStartFile(FrontendContext context) {
        final String entryFileName = context.getOptions().getEntryEntity();
        return context.getPathsResolver().getEntityFile(entryFileName);
    }

    private FrontendContext getContext(ContextRef contextRef) {
        final FrontendContext context = this.contextsMap.get(contextRef);
        if (context == null) {
            throw new IllegalArgumentException("unknown context reference");
        }
        return context;
    }

    private void setContext(FrontendContext context, ContextRef contextRef) {
        this.contextsMap.put(contextRef, context);
    }

    /**
     * Creates context from given options.
     *
     * @param args unstructured options
     * @return context
     * @throws InvalidOptionsException thrown when context options cannot be
     *                                 parsed successfully
     */
    private FrontendContext createContextFromOptions(String[] args) throws InvalidOptionsException {
        final OptionsParser optionsParser = new OptionsParser();
        final FrontendContext result;
        try {
            final OptionsHolder options = optionsParser.parse(args);
            result = new FrontendContext(options, this.isStandalone);
            return result;
        } catch (ParseException e) {
            final String msg = e.getMessage();
            if (this.isStandalone) {
                System.out.println(msg);
                optionsParser.printHelp();
                System.exit(1);
                return null;
            } else {
                throw new InvalidOptionsException(msg);
            }
        } catch (IOException e) {
            if (this.isStandalone) {
                e.printStackTrace();
                System.exit(1);
                return null;
            } else {
                final String msg = "Cannot find options.properties file.";
                throw new IllegalStateException(msg);
            }
        }
    }

    private List<FileData> parseFilesIncludedByDefault(FrontendContext context) {
        final List<FileData> result = new ArrayList<>();
        final List<String> defaultIncludes = context.getDefaultIncludeFiles();

        context.resetDefaultMacros();
        context.resetDefaultSymbols();
        
        for (String filePath : defaultIncludes) {
            try {
                final List<FileCache> fileCacheList = new LoadExecutor(context).parse(filePath, true);
                for (FileCache cache : fileCacheList) {
                    if (cache.isRoot()) {
                        result.add(new FileData(cache));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Error during parsing default files.", e);
                final String msg = format("Could not find file included by default; path = %s.", filePath);
                final NescError error = new NescError(Optional.<Location>absent(), Optional.<Location>absent(), msg);
                context.getIssues().add(error);
            }
        }
        return result;
    }

    private void clearDirtyFileCache(FrontendContext context, String modifiedFile) {
        final FilesGraph filesGraph = context.getFilesGraph();
        final GraphFile file = filesGraph.getFile(modifiedFile);
        if (file == null) {
            return;
        }
        final DirtyFileVisitor visitor = new DirtyFileVisitor(filesGraph.getFile(modifiedFile), false);
        visitor.start();
        final Set<GraphFile> dirtyFiles = visitor.getDirtyFiles();
        for (GraphFile graphFile : dirtyFiles) {
            context.getCache().remove(graphFile.getFilePath());
        }
    }

    private static class DirtyFileVisitor extends DefaultFileGraphVisitor {

        private final Set<GraphFile> dirtyFiles;

        public DirtyFileVisitor(GraphFile startNode, boolean outgoing) {
            super(startNode, outgoing);
            this.dirtyFiles = new HashSet<>();
        }

        public Set<GraphFile> getDirtyFiles() {
            return dirtyFiles;
        }

        @Override
        public void start() {
            super.start();
            dirtyFiles.remove(startNode);
        }

        @Override
        protected boolean action(GraphFile file) {
            dirtyFiles.add(file);
            return file.getNesCFileType() == NesCFileType.NONE;
        }
    }

    /**
     * Nesc frontend builder.
     */
    public static final class Builder {

        private boolean isStandalone;

        public Builder() {
            this.isStandalone = false;
        }

        /**
         * Sets whether the frontend is standalone or used as a library.
         * By default is not standalone.
         *
         * @param standalone is standalone
         * @return builder
         */
        public Builder standalone(boolean standalone) {
            this.isStandalone = standalone;
            return this;
        }

        public NescFrontend build() {
            verify();
            return new NescFrontend(this);
        }

        private void verify() {
        }
    }
}
