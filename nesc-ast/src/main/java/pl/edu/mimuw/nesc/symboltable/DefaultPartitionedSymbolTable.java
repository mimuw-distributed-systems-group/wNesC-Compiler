package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import pl.edu.mimuw.nesc.declaration.Declaration;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefaultPartitionedSymbolTable<T extends Declaration> implements PartitionedSymbolTable<T> {

    private final Map<String, T> symbols;
    private final Set<Partition> partitions;
    private final Multimap<String, String> partitionedSymbols;

    public DefaultPartitionedSymbolTable() {
        this.symbols = new HashMap<>();
        this.partitions = new HashSet<>();
        this.partitionedSymbols = HashMultimap.create();
    }

    @Override
    public boolean add(Partition partition, String name, T item) {
        final String partitionName = partition.getName();
        checkState(this.partitions.contains(partition), "unknown partition " + partitionName);
        checkNotNull(item, "item cannot be null");
        if (this.symbols.containsKey(name)) {
            return false;
        }

        item.setPartition(partition);
        this.symbols.put(name, item);
        this.partitionedSymbols.put(partitionName, name);
        return true;
    }

    @Override
    public Optional<? extends T> get(String name) {
        return Optional.fromNullable(this.symbols.get(name));
    }

    @Override
    public Set<Map.Entry<String, T>> getAll() {
        return this.symbols.entrySet();
    }

    @Override
    public boolean contains(String name) {
        return this.symbols.containsKey(name);
    }

    @Override
    public void createPartition(Partition partition) {
        checkState(!this.partitions.contains(partition), "partition " + partition.getName() + " already exists");
        this.partitions.add(partition);
    }

    @Override
    public void removePartition(Partition partition) {
        final String partitionName = partition.getName();
        //checkState(this.partitions.contains(partition), "partition " + partitionName + " does not exist");
        this.partitions.remove(partition);
        final Collection<String> toRemove = this.partitionedSymbols.removeAll(partitionName);
        for (String itemName : toRemove) {
            this.symbols.remove(itemName);
        }
    }
}
