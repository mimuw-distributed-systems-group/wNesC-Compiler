package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.option.OptionsHolder;
import pl.edu.mimuw.nesc.option.OptionsParser;
import pl.edu.mimuw.nesc.problem.NescError;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final FrontendContext context = createContextWorker(args);
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
    public ProjectData rebuild(ContextRef contextRef) {
        checkNotNull(contextRef, "context reference cannot be null");
        LOG.info("Rebuild; contextRef=" + contextRef);

        final FrontendContext context = getContext(contextRef).basicCopy();
        setContext(context, contextRef);

        final ProjectData.Builder projectDataBuilder = ProjectData.builder();
        final Optional<String> startFile = getStartFile(context);

        parseFilesIncludedByDefault(context);

        if (startFile.isPresent()) {
            update(contextRef, startFile.get());
        } else {
            final String msg = format("Cannot find main configuration '%s'", context.getOptions().getEntryEntity());
            LOG.error(msg);
            final NescError error = new NescError(Optional.<Location>absent(), Optional.<Location>absent(), msg);
            projectDataBuilder.addIssue(error);
        }

        projectDataBuilder.addIssues(context.getIssues());
        return projectDataBuilder.build();
    }

    @Override
    public FileData update(ContextRef contextRef, String filePath) {
        // TODO: update on one file may update more than one file (e.g. when
        // a new file is included or new NesC entity is used. Return list of
        // FileDatas?
        checkNotNull(contextRef, "context reference cannot be null");
        checkNotNull(filePath, "file path cannot be null");
        LOG.info("Update; contextRef=" + contextRef + "; filePath=" + filePath);

        final FrontendContext context = getContext(contextRef);

        try {
            // FIXME: what if we would like to edit file included by default?
            new ParseExecutor(context).parse(filePath, false);
            /*
             * File data is created only when necessary (to avoid creating
             * costly objects that might not be used).
             */
            return FileData.convertFrom(context.getCache().get(filePath));
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
        // FIXME FileData should be always returned or proper exception
        // should be thrown.
        return null;
    }

    @Override
    public Iterable<String> getKeywords() {
        return Keywords.KEYWORDS;
    }

    @Override
    public Iterable<String> getCoreKeywords() {
        return Keywords.CORE_KEYWORDS;
    }

    @Override
    public Iterable<String> getExtensionKeywords() {
        return Keywords.EXTENSION_KEYWORDS;
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
     * Creates context worker from given options.
     *
     * @param args unstructured options
     * @return context worker
     * @throws InvalidOptionsException thrown when context options cannot be
     *                                 parsed successfully
     */
    private FrontendContext createContextWorker(String[] args) throws InvalidOptionsException {
        final OptionsParser optionsParser = new OptionsParser();
        final FrontendContext result;
        try {
            final OptionsHolder options = optionsParser.parse(args);
            result = new FrontendContext(options);
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

    private void parseFilesIncludedByDefault(FrontendContext context) {
        final List<String> defaultIncludes = context.getDefaultIncludeFiles();

        for (String filePath : defaultIncludes) {
            try {
                new ParseExecutor(context).parse(filePath, true);
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Error during parsing default files.", e);
                final String msg = format("Could not find file included by default; path = %s.", filePath);
                final NescError error = new NescError(Optional.<Location>absent(), Optional.<Location>absent(), msg);
                context.getIssues().add(error);
            }
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
         * Sets whether frontend is standalone or used as a library.
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
