package pl.edu.mimuw.nesc.filesgraph.walker;

import pl.edu.mimuw.nesc.filesgraph.GraphFile;

/**
 * Contains code that should be executed when visiting each node of files
 * graph.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface NodeAction {

    /**
     * Called when a new node is visited.
     *
     * @param graphFile graph file
     */
    void run(GraphFile graphFile);
}
