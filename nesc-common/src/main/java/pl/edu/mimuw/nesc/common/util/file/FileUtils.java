package pl.edu.mimuw.nesc.common.util.file;

import com.google.common.io.Files;
import pl.edu.mimuw.nesc.common.FileType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class provides set of common file operations.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class FileUtils {

    private static final String HEADER_FILE_EXTENSION = "h";
    private static final String C_FILE_EXTENSION = "c";
    private static final String NESC_FILE_EXTENSION = "nc";

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

    /**
     * Determines file type from file extension. If extension is not
     * one of .nc, .h or .c the file is treated as nesc file.
     *
     * @param filePath file path
     * @return file type
     */
    public static FileType fileTypeFromExtension(String filePath) {
        checkNotNull(filePath, "file path should not be null");

        final String extension = Files.getFileExtension(filePath);
        switch (extension) {
            case NESC_FILE_EXTENSION:
                return FileType.NESC;
            case HEADER_FILE_EXTENSION:
                return FileType.HEADER;
            case C_FILE_EXTENSION:
                return FileType.C;
            default:
                return FileType.NESC;
        }
    }

    private FileUtils() {
    }
}
