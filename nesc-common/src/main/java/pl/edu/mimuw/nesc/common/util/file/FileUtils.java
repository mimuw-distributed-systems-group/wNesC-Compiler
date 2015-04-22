package pl.edu.mimuw.nesc.common.util.file;

import com.google.common.io.Files;
import java.io.File;
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
     * Returns the file name without its file extension or path.
     *
     * @param filePath file path
     * @return the file name without its pah or extension
     */
    public static String getFileNameWithoutExtension(String filePath) {
        checkNotNull(filePath, "file path should not be null");

        return Files.getNameWithoutExtension(filePath);
    }

    /**
     * Returns the prefix of the given path without the extension of the target
     * file.
     *
     * @param fullPath Path of a file.
     * @return The given path but without the extension of the target file.
     */
    public static String getPathPrefixWithoutExtension(String fullPath) {
        checkNotNull(fullPath, "path should not be null");

        final int lastDotIndex = fullPath.lastIndexOf('.');
        final int lastSlashIndex = fullPath.lastIndexOf(File.separatorChar);

        if (lastDotIndex == -1 || lastDotIndex == lastSlashIndex + 1
                || lastDotIndex == fullPath.length() - 1
                || lastDotIndex < lastSlashIndex) {
            return fullPath;
        } else {
            return fullPath.substring(0, lastDotIndex);
        }
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

    /**
     * Returns normalized path. Removes redundant occurrences of
     * <code>../</code>, <code>./</code>, etc when possible.
     *
     * @param filePath path to be normalized
     * @return normalized path
     */
    public static String normalizePath(String filePath) {
        return Files.simplifyPath(filePath);
    }

    private FileUtils() {
    }
}
