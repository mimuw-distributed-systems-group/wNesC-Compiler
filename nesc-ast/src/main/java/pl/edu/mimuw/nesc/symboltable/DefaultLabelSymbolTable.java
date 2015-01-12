package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.label.LabelDeclaration;

/**
 * <p>Implementation of a default label symbol table.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class DefaultLabelSymbolTable<T extends LabelDeclaration> extends DefaultSymbolTable<T>
        implements LabelSymbolTable<T> {
    /**
     * Value indicating if this symbol table is a symbol table that corresponds
     * to a function body compound statement.
     */
    private final boolean isFunctionTopLevelTable;

    public static <T extends LabelDeclaration> DefaultLabelSymbolTable<T> newFunctionTopLevelTable(
            Optional<? extends LabelSymbolTable<T>> parent) {
        return new DefaultLabelSymbolTable<>(parent, true);
    }

    public static <T extends LabelDeclaration> DefaultLabelSymbolTable<T> newBlockTable(LabelSymbolTable<T> parent) {
        return new DefaultLabelSymbolTable<>(Optional.of(parent), false);
    }

    private DefaultLabelSymbolTable(Optional<? extends LabelSymbolTable<T>> parent, boolean isFunctionTopLevelTable) {
        super(Optional.<SymbolTable<T>>fromNullable(parent.orNull()));
        this.isFunctionTopLevelTable = isFunctionTopLevelTable;
    }

    @Override
    public boolean addFunctionScopeLabel(String name, T item) {
        if (!isFunctionTopLevelTable) {
            final LabelSymbolTable<T> parent = (LabelSymbolTable<T>) getParent().get();
            return parent.addFunctionScopeLabel(name, item);
        }

        return add(name, item);
    }

    @Override
    public Optional<? extends T> getFromCurrentFunction(String name) {
        final Optional<? extends T> symbol = get(name, true);

        if (symbol.isPresent()) {
            return symbol;
        } else if (!isFunctionTopLevelTable) {
            return ((LabelSymbolTable<T>) getParent().get()).getFromCurrentFunction(name);
        } else {
            return Optional.absent();
        }
    }

    @Override
    public Optional<? extends T> getLocalFromOuterFunctions(String name) {
        if (!isFunctionTopLevelTable) {
            return ((LabelSymbolTable<T>) getParent().get()).getLocalFromOuterFunctions(name);
        } else if (getParent().isPresent()) {
            final Optional<? extends T> symbol = getParent().get().get(name);
            return symbol.isPresent() && symbol.get().isLocal()
                    ? symbol
                    : Optional.<T>absent();
        } else {
            return Optional.absent();
        }
    }
}
