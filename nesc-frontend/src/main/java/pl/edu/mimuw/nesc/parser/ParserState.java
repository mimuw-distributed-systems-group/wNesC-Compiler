package pl.edu.mimuw.nesc.parser;

import com.google.common.base.Optional;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.common.util.list.Lists;

import java.util.LinkedList;
import java.util.Stack;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
class ParserState {

    /*
     * We need to keep declspecs (modifiers, qualifiers, modifiers,
     * attributes, etc.) in the context, since they are separated from the
     * declared name by a construct called 'declarator'.
     *
     * Example:
     *    static int __attribute__(foo)     i = 1, j = i, k = 123;
     *    |---------------------------|     |---|  |---|  |-----|
     *            declspecs_ts                   3 x initdcl
     *
     * This may correspond to the grammar rules:
     *     decl:    declspecs_ts setspecs initdecls SEMICOLON
     *     initdcl: declarator maybeasm maybe_attribute EQ init
     *            | declarator maybeasm maybe_attribute
     *            ;
     *
     * We need to know declspecs while parsing declarator, to be able to put
     * the declared name into the symbol table, just after the declarator is
     * finished.
     *
     * Moreover, one declaration may be nested in another. For example,
     * the declaration of a struct contains declarations of fields.
     * In consequence, we need to keep a stack of declspecs.
     */

    private class StackItem {
        private final LinkedList<Attribute> attributes;
        private final TypeElementsAssociation declspecs;

        public StackItem(LinkedList<Attribute> attributes,
                         TypeElementsAssociation declspecs) {
            this.attributes = attributes;
            this.declspecs = declspecs;
        }

        public StackItem() {
            this.attributes = Lists.newList();
            this.declspecs = new TypeElementsAssociation(Lists.<TypeElement>newList());
        }

        public LinkedList<Attribute> getAttributes() {
            return attributes;
        }

        public TypeElementsAssociation getDeclspecs() {
            return declspecs;
        }

    }

    private final Stack<StackItem> stack;
    public LinkedList<Attribute> attributes;
    public TypeElementsAssociation declspecs;
    public int stmtCount;
    public boolean insideUsesProvides = false;
    public boolean newFunctionScope = false;
    public Optional<Set<String>> atomicStmtLabelsNames = Optional.absent();

    public ParserState() {
        this.stack = new Stack<>();
        this.stack.push(new StackItem());
    }

    public void popDeclspecStack() {
        final StackItem previous = this.stack.pop();
        this.attributes = previous.getAttributes();
        this.declspecs = previous.getDeclspecs();
    }

    public void pushDeclspecStack() {
        final StackItem current = new StackItem(this.attributes, this.declspecs);
        this.stack.push(current);
        this.attributes = Lists.newList();
        this.declspecs = new TypeElementsAssociation(Lists.<TypeElement>newList());
    }
}
