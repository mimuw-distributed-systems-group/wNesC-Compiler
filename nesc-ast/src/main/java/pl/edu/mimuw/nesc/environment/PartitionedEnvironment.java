package pl.edu.mimuw.nesc.environment;

import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.symboltable.DefaultPartitionedSymbolTable;
import pl.edu.mimuw.nesc.symboltable.Partition;
import pl.edu.mimuw.nesc.symboltable.PartitionedSymbolTable;

/**
 * <p>Represents environment that is divided into partitions. Should be used
 * only as global environment. Each file has a corresponding partition
 * in <code>PartitionedEnvironment</code>.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class PartitionedEnvironment {

    private final PartitionedSymbolTable<ObjectDeclaration> objects;
    private final PartitionedSymbolTable<TagDeclaration> tags;

    public PartitionedEnvironment() {
        this.objects = new DefaultPartitionedSymbolTable<>();
        this.tags = new DefaultPartitionedSymbolTable<>();
    }

    public PartitionedSymbolTable<ObjectDeclaration> getObjects() {
        return objects;
    }

    public PartitionedSymbolTable<TagDeclaration> getTags() {
        return tags;
    }

    public void createPartition(Partition partition) {
        this.objects.createPartition(partition);
        this.tags.createPartition(partition);
    }

    public void removePartition(Partition partition) {
        this.objects.removePartition(partition);
        this.tags.removePartition(partition);
    }
}
