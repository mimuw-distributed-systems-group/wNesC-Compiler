package pl.edu.mimuw.nesc.parser;

import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.common.util.list.Lists;

import java.util.LinkedList;
import java.util.Stack;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
class ParserState {

    private class StackItem {

        private final LinkedList<Attribute> attributes;
        private final LinkedList<TypeElement> declspecs;
        private final boolean wasTypedef;

        public StackItem(LinkedList<Attribute> attributes,
                         LinkedList<TypeElement> declspecs, boolean wasTypedef) {
            this.attributes = attributes;
            this.declspecs = declspecs;
            this.wasTypedef = wasTypedef;
        }

        public StackItem() {
            this.attributes = Lists.newList();
            this.declspecs = Lists.newList();
            this.wasTypedef = false;
        }

        public LinkedList<Attribute> getAttributes() {
            return attributes;
        }

        public LinkedList<TypeElement> getDeclspecs() {
            return declspecs;
        }

        public boolean wasTypedef() {
            return wasTypedef;
        }

    }

    private final Stack<StackItem> stack;
    public LinkedList<Attribute> attributes;
    public LinkedList<TypeElement> declspecs;
    public boolean wasTypedef;
    public int stmtCount;

    public ParserState() {
        this.stack = new Stack<>();
        this.stack.push(new StackItem());
    }

    public void popDeclspecStack() {
        final StackItem previous = this.stack.pop();
        this.attributes = previous.getAttributes();
        this.declspecs = previous.getDeclspecs();
        this.wasTypedef = previous.wasTypedef();
    }

    public void pushDeclspecStack() {
        final StackItem current = new StackItem(this.attributes,
                this.declspecs, wasTypedef);
        this.stack.push(current);
        this.attributes = Lists.newList();
        this.declspecs = Lists.newList();
        this.wasTypedef = false;
    }

}
