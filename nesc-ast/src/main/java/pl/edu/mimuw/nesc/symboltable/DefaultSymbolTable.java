package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.Declaration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefaultSymbolTable<T extends Declaration> implements SymbolTable<T> {

    private final Optional<SymbolTable<T>> parent;
    private final Map<String, T> symbols;

    public DefaultSymbolTable() {
        this(Optional.<SymbolTable<T>>absent());
    }

    public DefaultSymbolTable(Optional<SymbolTable<T>> parent) {
        this.parent = parent;
        this.symbols = new HashMap<>();
    }

    @Override
    public boolean add(String name, T item) {
        if (this.symbols.containsKey(name)) {
            return false;
        }
        this.symbols.put(name, item);
        return true;
    }

    @Override
    public Optional<? extends T> get(String name) {
        final T symbol = this.symbols.get(name);
        if (symbol != null) {
            return Optional.of(symbol);
        }
        if (!parent.isPresent()) {
            return Optional.absent();
        }
        return parent.get().get(name);
    }

    @Override
    public boolean contains(String name) {
        return get(name).isPresent();
    }
}
