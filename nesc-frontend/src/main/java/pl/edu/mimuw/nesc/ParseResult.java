package pl.edu.mimuw.nesc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the result of parsing process.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ParseResult {

    private final Map<String, FileResult> files;

    // TODO: extend attributes list

    /**
     * Creates new ParseResult instance.
     */
    public ParseResult() {
        this.files = new HashMap<>();
    }

    /**
     * Adds result of parsing of new file.
     *
     * @param fileResult object containing result of single file parsing
     */
    public void addFile(FileResult fileResult) {
        checkNotNull(fileResult, "file result cannot be null");
        final String fileName = fileResult.getFileName();
        checkState(!this.files.containsKey(fileName), "duplicated entry for file " + fileName);
        this.files.put(fileName, fileResult);
    }

    /**
     * Returns object containing result of parsing of specified file.
     *
     * @param file file path
     * @return object containing result of parsing of specified file
     */
    public FileResult getFile(String file) {
        checkNotNull(file, "file cannot be null");
        return this.files.get(file);
    }

    /**
     * Returns map of results of parsing of all files.
     *
     * @return map of results
     */
    public Map<String, FileResult> getFilesMap() {
        return this.files;
    }

    @Override
    public String toString() {
        return "{ ParseResult; {files=" + files + "}}";
    }

}
