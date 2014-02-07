package pl.edu.mimuw.nesc.filesgraph.walker;

import pl.edu.mimuw.nesc.filesgraph.FilesGraph;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class FilesGraphWalkerFactory {

    /**
     * Creates files graph walker that performs walk using DFS algorithm and
     * which executes action in post-order (when all of node's children were
     * visited).
     *
     * @param graph  graph
     * @param action action
     * @return files graph walker
     */
    public static FilesGraphWalker ofDfsPostOrderWalker(FilesGraph graph, NodeAction action) {
        return new DfsPostOrderWalker(graph, action);
    }

}
