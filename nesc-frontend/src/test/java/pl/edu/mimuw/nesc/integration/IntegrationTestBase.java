package pl.edu.mimuw.nesc.integration;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import pl.edu.mimuw.nesc.*;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Enumeration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class IntegrationTestBase {

    /**
     * Name of the main entity used in tests.
     */
    protected static final String MAIN_ENTITY_NAME = "C";

    /**
     * Function that changes a file to its path.
     */
    private static final Function<File, String> FILE_TO_PATH = new Function<File, String>() {
        @Override
        public String apply(File file) {
            checkNotNull(file, "file cannot be null");
            return file.getPath();
        }
    };

    protected Frontend frontend;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void turnOffLogging() {
        LogManager.getRootLogger().setLevel(Level.OFF);
        final Enumeration<Logger> loggersEnum = LogManager.getCurrentLoggers();

        while (loggersEnum.hasMoreElements()) {
            final Logger logger = loggersEnum.nextElement();
            logger.setLevel(Level.OFF);
        }
    }

    @Before
    public void setUp() throws Exception {
        frontend = NescFrontend.builder()
                .standalone(true)
                .build();
    }

    protected FileData getFileData(Frontend frontend, String resourcePath, String mainEntity, String testEntityPath)
            throws InvalidOptionsException, FileNotFoundException {
        final String filePath = getResourceDirectory(resourcePath);
        final String dirPath = getParent(filePath);
        final String[] args = new String[]{"-p", dirPath, "-m", mainEntity};
        final ContextRef contextRef = frontend.createContext(args);
        return frontend.update(contextRef, getResourceDirectory(testEntityPath)).getRootFileData().get();
    }

    protected ProjectData getProjectData(Frontend frontend, String dirPath, String mainEntity)
            throws InvalidOptionsException, FileNotFoundException {
        final String[] args = new String[] { "-p", dirPath, "-m", mainEntity };
        final ContextRef contextRef = frontend.createContext(args);
        return frontend.build(contextRef);
    }

    protected static String getResourceDirectory(String resourcePath) {
        return Thread.currentThread().getContextClassLoader()
                .getResource(resourcePath)
                .getPath();
    }

    protected static String[] getResourceDirectoryContents(String resourcePath,
            final Optional<String> dirNameRegex) {
        final String path = getResourceDirectory(resourcePath);
        final File dir = new File(path);
        assert dir.isDirectory() : "given path does not designate a directory";

        final File[] acceptedFiles = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (!dirNameRegex.isPresent() || file.getName().matches(dirNameRegex.get()))
                        && file.isDirectory();
            }
        });
        assert acceptedFiles != null : "cannot get contents of the directory";

        return FluentIterable.from(Arrays.asList(acceptedFiles))
                .transform(FILE_TO_PATH)
                .toArray(String.class);
    }

    protected String getParent(String filePath) {
        return new File(filePath).getParent();
    }
}
