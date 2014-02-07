package pl.edu.mimuw.nesc.filesgraph.walker;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface FilesGraphWalker {

    /**
     * Perform walk on graph nodes starting from specified file.
     *
     * @param startFile start file's name
     */
    void walk(String startFile);

}
