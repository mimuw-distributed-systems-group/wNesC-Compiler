package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import pl.edu.mimuw.nesc.declaration.Declaration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefaultSymbolTable<T extends Declaration> implements SymbolTable<T> {

    private final Optional<SymbolTable<T>> parent;
    private final Map<String, T> symbols;
    private final Set<String> fromCurrentFile;

    public DefaultSymbolTable() {
        this(Optional.<SymbolTable<T>>absent());
    }

    public DefaultSymbolTable(Optional<SymbolTable<T>> parent) {
        this.parent = parent;
        this.symbols = new HashMap<>();
        this.fromCurrentFile = new HashSet<>();
    }

    @Override
    public boolean add(String name, T item) {
        return add(name, item, true);
    }

    @Override
    public boolean add(String name, T item, boolean fromCurrentFile) {
        if (this.symbols.containsKey(name)) {
            return false;
        }
        this.symbols.put(name, item);
        if (fromCurrentFile) {
            this.fromCurrentFile.add(name);
        }
        return true;
    }

    @Override
    public Optional<? extends T> get(String name) {
        return get(name, false);
    }

    @Override
    public Optional<? extends T> get(String name, boolean onlyCurrentScope) {
        final T symbol = this.symbols.get(name);
        if (symbol != null) {
            return Optional.of(symbol);
        }
        if (onlyCurrentScope || !parent.isPresent()) {
            return Optional.absent();
        }
        return parent.get().get(name);
    }

    @Override
    public Set<Map.Entry<String, T>> getAll() {
        return this.symbols.entrySet();
    }

    @Override
    public Set<Map.Entry<String, T>> getAllFromFile() {
        final ImmutableSet.Builder<Map.Entry<String, T>> builder = ImmutableSet.builder();
        for (Map.Entry<String, T> entry : this.symbols.entrySet()) {
            if (this.fromCurrentFile.contains(entry.getKey())) {
                builder.add(entry);
            }
        }
        return builder.build();
    }

    @Override
    public boolean contains(String name) {
        return contains(name, false);
    }

    @Override
    public boolean contains(String name, boolean onlyCurrentScope) {
        return get(name, onlyCurrentScope).isPresent();
    }
}
