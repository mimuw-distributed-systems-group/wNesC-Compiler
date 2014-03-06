package pl.edu.mimuw.nesc.common.util.file;

import com.google.common.io.Files;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class provides set of common file operations.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class FileUtils {

    private static final String HEADER_FILE_EXTENSION = "h";
    private static final String C_FILE_EXTENSION = "c";

    /**
     * Checks if specified file is a header file.
     *
     * @param filePath file path
     * @return <code>true</code> if specified file is a header file
     */
    public static boolean isHeaderFile(String filePath) {
        checkNotNull(filePath, "file path should not be null");

        final String extension = Files.getFileExtension(filePath);
        return HEADER_FILE_EXTENSION.equals(extension);
    }

    /**
     * Checks if specified file is a c file.
     *
     * @param filePath file path
     * @return <code>true</code> if specified file is a c file
     */
    public static boolean isCFile(String filePath) {
        checkNotNull(filePath, "file path should not be null");

        final String extension = Files.getFileExtension(filePath);
        return C_FILE_EXTENSION.equals(extension);
    }

    private FileUtils() {
    }
}
