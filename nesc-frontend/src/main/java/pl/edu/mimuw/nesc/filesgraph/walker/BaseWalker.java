package pl.edu.mimuw.nesc.filesgraph.walker;

import pl.edu.mimuw.nesc.filesgraph.FilesGraph;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
abstract class BaseWalker implements FilesGraphWalker {

    protected final FilesGraph graph;
    protected final NodeAction action;

    public BaseWalker(FilesGraph graph, NodeAction action) {
        checkNotNull(graph, "graph cannot be null");
        checkNotNull(action, "action cannot be null");

        this.graph = graph;
        this.action = action;
    }

}
