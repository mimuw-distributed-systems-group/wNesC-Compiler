package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.symboltable.Partition;
import pl.edu.mimuw.nesc.symboltable.PartitionedSymbolTableAdapter;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import java.util.Map;

/**
 * Adapts <code>PartitionedEnvironment</code> to <code>Environment</code>.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class PartitionedEnvironmentAdapter implements Environment {

    private final PartitionedEnvironment partitionedEnvironment;
    private final SymbolTable<ObjectDeclaration> objects;
    private final SymbolTable<TagDeclaration> tags;

    public PartitionedEnvironmentAdapter(PartitionedEnvironment partitionedEnvironment,
                                         Partition currentPartition,
                                         Map<String, Partition> visiblePartitions) {
        this.partitionedEnvironment = partitionedEnvironment;
        this.objects = new PartitionedSymbolTableAdapter<>(this.partitionedEnvironment.getObjects(), currentPartition,
                visiblePartitions);
        this.tags = new PartitionedSymbolTableAdapter<>(this.partitionedEnvironment.getTags(), currentPartition,
                visiblePartitions);
    }

    @Override
    public Optional<Environment> getParent() {
        return Optional.absent();
    }

    @Override
    public SymbolTable<ObjectDeclaration> getObjects() {
        return objects;
    }

    @Override
    public SymbolTable<TagDeclaration> getTags() {
        return tags;
    }

    @Override
    public Optional<Location> getStartLocation() {
        return Optional.absent();
    }

    @Override
    public void setStartLocation(Location location) {
        throw new RuntimeException();
    }

    @Override
    public Optional<Location> getEndLocation() {
        return Optional.absent();
    }

    @Override
    public void setEndLocation(Location endLocation) {
        throw new RuntimeException();
    }

    @Override
    public ScopeType getScopeType() {
        return ScopeType.GLOBAL;
    }

    @Override
    public void setScopeType(ScopeType type) {
        throw new RuntimeException();
    }
}
