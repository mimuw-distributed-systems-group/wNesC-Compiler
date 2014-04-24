package pl.edu.mimuw.nesc.environment;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.symboltable.Partition;
import pl.edu.mimuw.nesc.symboltable.PartitionedSymbolTableAdapter;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapts <code>PartitionedEnvironment</code> to <code>Environment</code>.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class PartitionedEnvironmentAdapter implements Environment {

    private final PartitionedEnvironment partitionedEnvironment;
    private final Map<String, Partition> visiblePartitions;
    private final SymbolTable<ObjectDeclaration> objects;
    private final SymbolTable<TagDeclaration> tags;
    private final List<Environment> enclosedEnvironments;

    public PartitionedEnvironmentAdapter(PartitionedEnvironment partitionedEnvironment,
                                         Partition currentPartition,
                                         Map<String, Partition> visiblePartitions) {
        this.partitionedEnvironment = partitionedEnvironment;
        this.visiblePartitions = visiblePartitions;
        this.objects = new PartitionedSymbolTableAdapter<>(this.partitionedEnvironment.getObjects(), currentPartition,
                visiblePartitions);
        this.tags = new PartitionedSymbolTableAdapter<>(this.partitionedEnvironment.getTags(), currentPartition,
                visiblePartitions);
        this.enclosedEnvironments = new ArrayList<>();
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
        throw new RuntimeException();
    }

    @Override
    public void setStartLocation(Location location) {
        throw new RuntimeException();
    }

    @Override
    public Optional<Location> getEndLocation() {
        throw new RuntimeException();
    }

    @Override
    public void setEndLocation(Location location) {
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

    @Override
    public void addEnclosedEnvironment(Environment environment) {
        this.enclosedEnvironments.add(environment);
    }

    @Override
    public List<Environment> getEnclosedEnvironments() {
        return this.enclosedEnvironments;
    }


    public Map<String, Partition> getVisiblePartitions() {
        return visiblePartitions;
    }
}
