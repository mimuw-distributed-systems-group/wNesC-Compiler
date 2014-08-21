package pl.edu.mimuw.nesc.filesgraph.visitor;

import pl.edu.mimuw.nesc.filesgraph.GraphFile;

import java.util.Map;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class DefaultFileGraphVisitor {

    protected final GraphFile startNode;
    protected final boolean outgoing;

    public DefaultFileGraphVisitor(GraphFile startNode, boolean outgoing) {
        this.startNode = startNode;
        this.outgoing = outgoing;
    }

    public void start() {
        visit(startNode);
    }

    private void visit(GraphFile file) {
        if (!action(file)) {
            return;
        }

        final Map<String, GraphFile> toVisit;
        if (outgoing) {
            toVisit = file.getUses();
        } else {
            toVisit = file.getIsUsedBy();
        }

        for (GraphFile child : toVisit.values()) {
            visit(child);
        }
    }

    protected abstract boolean action(GraphFile file);
}
