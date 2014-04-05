package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.Declaration;

import java.util.Map;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class PartitionedSymbolTableAdapter<T extends Declaration> implements SymbolTable<T> {

    private final PartitionedSymbolTable<T> partitionedSymbolTable;
    private final Partition currentPartition;
    private final Map<String, Partition> visiblePartitions;

    public PartitionedSymbolTableAdapter(PartitionedSymbolTable<T> partitionedSymbolTable,
                                         Partition currentPartition,
                                         Map<String, Partition> visiblePartitions) {
        this.partitionedSymbolTable = partitionedSymbolTable;
        this.currentPartition = currentPartition;
        this.visiblePartitions = visiblePartitions;
        this.partitionedSymbolTable.createPartition(currentPartition);
    }

    @Override
    public boolean add(String name, T item) {
        return this.partitionedSymbolTable.add(currentPartition, name, item);
    }

    @Override
    public Optional<? extends T> get(String name) {
        return getIfVisible(name);
    }

    @Override
    public boolean contains(String name) {
        return getIfVisible(name).isPresent();
    }

    public void removePartition() {
        this.partitionedSymbolTable.removePartition(currentPartition);
    }

    public void addVisiblePartition(Partition partition) {
        this.visiblePartitions.put(partition.getName(), partition);
    }

    private Optional<? extends T> getIfVisible(String name) {
        final Optional<? extends T> symbol = this.partitionedSymbolTable.get(name);
        /* If symbol in unknown, return absent. */
        if (!symbol.isPresent()) {
            return symbol;
        }
        final Partition symbolPartition = symbol.get().getPartition();
        /* If symbol's partition is not visible in current file, return absent. */
        if (!this.visiblePartitions.containsKey(symbolPartition.getName())) {
            return Optional.absent();
        }
        /* Symbol is visible. */
        return symbol;
    }
}
