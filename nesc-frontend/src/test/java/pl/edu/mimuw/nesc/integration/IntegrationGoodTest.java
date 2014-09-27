package pl.edu.mimuw.nesc.integration;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.ProjectData;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;
import static org.junit.runners.Parameterized.Parameters;

/**
 * <p>Tests for correct programs. Each test is associated with a test program
 * that should be located in a subdirectory of directory:
 *
 * <code>nesc-frontend/src/test/resources/integration/programs/good</code>
 *
 * The name of the subdirectory shall start with <code>good</code> and contain
 * at least one character after this word. All tests are detected automatically
 * so to add a test one must only add the directory with the program.</p>
 * <p>A test is passed if and only if all files in a program are correctly
 * parsed and analyzed and have no issues (this means in particular, they shall
 * generate no warnings).</p>
 * <p>A program for a good test shall be correct and valid. The main
 * configuration shall have the name that is the value of static variable
 * {@link IntegrationTestBase#MAIN_ENTITY_NAME}.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
@RunWith(Parameterized.class)
public class IntegrationGoodTest extends IntegrationTestBase {

    private static final String TESTDIR_REGEX = "^good.+$";
    private final String absolutePath;

    @Parameters
    public static List<String[]> testData() {
        final String[] directories = getResourceDirectoryContents("integration/programs/good",
                Optional.of(TESTDIR_REGEX));

        final List<String[]> result = new ArrayList<>();
        for (String directory : directories) {
            result.add(new String[] { directory });
        }

        return result;
    }

    public IntegrationGoodTest(String absolutePath) {
        checkNotNull(absolutePath, "absolute path cannot be null");
        this.absolutePath = absolutePath;
    }

    @Test
    public void loadsCorrectly() throws FileNotFoundException, InvalidOptionsException {
        final ProjectData projectData = getProjectData(frontend, absolutePath, MAIN_ENTITY_NAME);

        assertTrue("issues present for a good project", projectData.getIssues().isEmpty());
        assertTrue("root file data absent for a good project",
                projectData.getRootFileData().isPresent());

        for (FileData fileData : projectData.getFileDatas().values()) {
            assertTrue("issues present for a file from a good project",
                    fileData.getIssues().isEmpty());
            assertTrue("root node absent for a file from a good project",
                    fileData.getEntityRoot().isPresent());
        }
    }
}
