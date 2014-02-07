package pl.edu.mimuw.nesc.filesgraph.walker;

import pl.edu.mimuw.nesc.filesgraph.FilesGraph;
import pl.edu.mimuw.nesc.filesgraph.GraphFile;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DfsPostOrderWalker extends BaseWalker {

    private final Set<GraphFile> visited;

    public DfsPostOrderWalker(FilesGraph graph, NodeAction action) {
        super(graph, action);
        this.visited = new HashSet<>();
    }

    @Override
    public void walk(String startFile) {
        this.visited.clear();
        final GraphFile file = this.graph.getFile(startFile);
        visit(file);
    }

    private void visit(GraphFile currentFile) {
        if (visited.contains(currentFile)) {
            return;
        }
        visited.add(currentFile);

        for (GraphFile descendant : currentFile.getUses().values()) {
            visit(descendant);
        }

        action.run(currentFile);
    }


}
