package pl.edu.mimuw.nesc.integration;

import org.junit.Before;
import pl.edu.mimuw.nesc.ContextRef;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.Frontend;
import pl.edu.mimuw.nesc.NescFrontend;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;

import java.io.File;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class IntegrationTestBase {

    protected Frontend frontend;

    @Before
    public void setUp() throws Exception {
        frontend = NescFrontend.builder()
                .standalone(false)
                .build();
    }

    protected FileData getFileData(Frontend frontend, String resourcePath, String mainEntity, String testEntityPath)
            throws InvalidOptionsException {
        final String filePath = getResourceDirectory(resourcePath);
        final String dirPath = getParent(filePath);
        final String[] args = new String[]{"-p", dirPath, "-m", mainEntity};
        final ContextRef contextRef = frontend.createContext(args);
        return frontend.update(contextRef, getResourceDirectory(testEntityPath));
    }

    protected String getResourceDirectory(String resourcePath) {
        return Thread.currentThread().getContextClassLoader()
                .getResource(resourcePath)
                .getPath();
    }

    protected String getParent(String filePath) {
        return new File(filePath).getParent();
    }
}
