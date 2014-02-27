package pl.edu.mimuw.nesc.filesgraph;

import static com.google.common.base.Preconditions.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * <p>Graph of file dependencies.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class FilesGraph {

    private final Map<String, GraphFile> files;

    public FilesGraph() {
        this.files = new HashMap<>();
    }

    public GraphFile getFile(String filePath) {
        return this.files.get(filePath);
    }

    public boolean containsFile(String filePath) {
        return this.files.containsKey(filePath);
    }

    public void addFile(GraphFile graphFile) {
        this.files.put(graphFile.getFilePath(), graphFile);
    }

    public void removeFile(String fileName) {
        final GraphFile graphFile = this.files.get(fileName);
        if (graphFile == null) {
            return;
        }
        removeFile(graphFile);
    }

    public void removeFile(GraphFile graphFile) {
        this.files.remove(graphFile.getFilePath());

        for (GraphFile user : graphFile.getIsUsedBy().values()) {
            user.removeUsedFile(graphFile.getFilePath());
        }

        for (GraphFile use : graphFile.getUses().values()) {
            use.removeFileUser(graphFile.getFilePath());
        }
    }

    public void removeOutgoingDependencies(String fileName) {
        final GraphFile graphFile = this.files.get(fileName);
        if (graphFile == null) {
            // TODO
        }
        removeOutgoingDependencies(graphFile);
    }

    public void removeOutgoingDependencies(GraphFile graphFile) {
        for (GraphFile user : graphFile.getIsUsedBy().values()) {
            user.removeUsedFile(graphFile.getFilePath());
        }
    }

    public void addEdge(String from, String to) {
        checkState(this.files.containsKey(from), "unknown file from " + from);
        checkState(this.files.containsKey(to), "unknown file to " + to);
        addEdge(this.files.get(from), this.files.get(to));
    }

    public void addEdge(GraphFile from, String to) {
        checkState(this.files.containsKey(to), "unknown file to " + to);
        addEdge(from, this.files.get(to));
    }

    public void addEdge(String from, GraphFile to) {
        checkState(this.files.containsKey(from), "unknown file from " + from);
        addEdge(this.files.get(from), to);
    }

    public void addEdge(GraphFile from, GraphFile to) {
        from.addUsedFile(to);
        to.addFileUser(from);
    }

}
