package pl.edu.mimuw.nesc.filesgraph;

import pl.edu.mimuw.nesc.common.FileType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>Each node of graph represents a single source file. Each one keeps
 * both outgoing and ingoing edges.</p>
 * <p/>
 *
 * <p>Node dependencies should be executed via graph instance
 * {@link FilesGraph}</p>
 *
 * <p>The order of used files is preserved.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class GraphFile {

    private final String filePath;
    private FileType fileType;
    private final Map<String, GraphFile> uses;
    private final Map<String, GraphFile> isUsedBy;

    public GraphFile(String filePath) {
        this(filePath, null);
    }

    public GraphFile(String filePath, FileType fileType) {
        this.filePath = filePath;
        this.fileType = fileType;
        this.uses = new LinkedHashMap<>();
        this.isUsedBy = new HashMap<>();
    }

    public String getFilePath() {
        return filePath;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public Map<String, GraphFile> getUses() {
        return uses;
    }

    public Map<String, GraphFile> getIsUsedBy() {
        return isUsedBy;
    }

    void addUsedFile(GraphFile graphFile) {
        this.uses.put(graphFile.getFilePath(), graphFile);
    }

    void removeUsedFile(String fileName) {
        this.uses.remove(fileName);
    }

    void addFileUser(GraphFile graphFile) {
        this.isUsedBy.put(graphFile.getFilePath(), graphFile);
    }

    void removeFileUser(String fileName) {
        this.isUsedBy.remove(fileName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphFile graphFile = (GraphFile) o;

        if (!filePath.equals(graphFile.filePath)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }
}
