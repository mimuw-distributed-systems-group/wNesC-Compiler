package pl.edu.mimuw.nesc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the result of parsing process for entire project.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ProjectData {

    // TODO: make it immutable for clients
    // TODO: what should this class contain?

    private final Map<String, FileData> files;

    // TODO: extend attributes list

    /**
     * Creates new ProjectData instance.
     */
    public ProjectData() {
        this.files = new HashMap<>();
    }

    /**
     * Adds result of parsing of new file.
     *
     * @param fileData object containing result of single file parsing
     */
    public void addFile(FileData fileData) {
        checkNotNull(fileData, "file result cannot be null");
        final String fileName = fileData.getFilePath();
        checkState(!this.files.containsKey(fileName), "duplicated entry for file " + fileName);
        this.files.put(fileName, fileData);
    }

    /**
     * Returns object containing result of parsing of specified file.
     *
     * @param file file path
     * @return object containing result of parsing of specified file
     */
    public FileData getFile(String file) {
        checkNotNull(file, "file cannot be null");
        return this.files.get(file);
    }

    /**
     * Returns map of results of parsing of all files.
     *
     * @return map of results
     */
    public Map<String, FileData> getFilesMap() {
        return this.files;
    }

    @Override
    public String toString() {
        return "{ ProjectData; {files=" + files + "}}";
    }

}
