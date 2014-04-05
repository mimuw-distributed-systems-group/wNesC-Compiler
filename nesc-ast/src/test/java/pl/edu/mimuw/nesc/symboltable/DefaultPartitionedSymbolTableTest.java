package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefaultPartitionedSymbolTableTest {

    private static final String VAR_A = "A";
    private static final String VAR_B = "B";

    private static final String FILE_1 = "file_1.nc";
    private static final String FILE_2 = "file_2.nc";

    private static final Location LOCATION_FILE1_9_5 = new Location(FILE_1, 9, 5);
    private static final Location LOCATION_FILE1_10_15 = new Location(FILE_1, 10, 15);
    private static final Location LOCATION_FILE2_10_15 = new Location(FILE_2, 10, 15);

    private DefaultPartitionedSymbolTable<ObjectDeclaration> partitionedSymbolTable;
    private Partition partition1;
    private Partition partition2;
    private ObjectDeclaration variableA_File1;
    private ObjectDeclaration variableB_File2;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        partitionedSymbolTable = new DefaultPartitionedSymbolTable<>();
        partition1 = new Partition(FILE_1);
        partition2 = new Partition(FILE_2);
        variableA_File1 = new VariableDeclaration(VAR_A, LOCATION_FILE1_10_15);
        variableB_File2 = new VariableDeclaration(VAR_B, LOCATION_FILE2_10_15);
    }

    @Test
    public void shouldNotRecreatePartition() {
        partitionedSymbolTable.createPartition(partition1);
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("partition " + partition1.getName() + " already exists");
        partitionedSymbolTable.createPartition(partition1);
    }

    @Test
    public void shouldAddDifferentVariablesToDifferentPartitions() {
        partitionedSymbolTable.createPartition(partition1);
        partitionedSymbolTable.createPartition(partition2);

        assertThat(partitionedSymbolTable.add(partition1, VAR_A, variableA_File1)).isTrue();
        assertThat(partitionedSymbolTable.add(partition2, VAR_B, variableB_File2)).isTrue();
        assertThat(partitionedSymbolTable.contains(VAR_A)).isTrue();
        assertThat(partitionedSymbolTable.contains(VAR_B)).isTrue();
        assertThat(partitionedSymbolTable.get(VAR_A)).isEqualTo(Optional.of(variableA_File1));
        assertThat(partitionedSymbolTable.get(VAR_B)).isEqualTo(Optional.of(variableB_File2));
    }

    @Test
    public void shouldNotAddTheSameNameVariableTwice() {
        partitionedSymbolTable.createPartition(partition1);
        final ObjectDeclaration newVariableA = new VariableDeclaration(VAR_A, LOCATION_FILE1_9_5);

        assertThat(partitionedSymbolTable.add(partition1, VAR_A, variableA_File1)).isTrue();
        assertThat(partitionedSymbolTable.add(partition1, VAR_A, newVariableA)).isFalse();
        assertThat(partitionedSymbolTable.contains(VAR_A)).isTrue();
        assertThat(partitionedSymbolTable.get(VAR_A)).isEqualTo(Optional.of(variableA_File1));
    }

    @Test
    public void shouldRemovePartition() {
        partitionedSymbolTable.createPartition(partition1);
        partitionedSymbolTable.createPartition(partition2);

        assertThat(partitionedSymbolTable.add(partition1, VAR_A, variableA_File1)).isTrue();
        assertThat(partitionedSymbolTable.add(partition2, VAR_B, variableB_File2)).isTrue();

        assertThat(partitionedSymbolTable.contains(VAR_A)).isTrue();
        assertThat(partitionedSymbolTable.contains(VAR_B)).isTrue();

        partitionedSymbolTable.removePartition(partition1);

        assertThat(partitionedSymbolTable.contains(VAR_A)).isFalse();
        assertThat(partitionedSymbolTable.contains(VAR_B)).isTrue();
    }

    @Test
    public void exceptionWhenRemovingNotExistingPartition() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("partition " + partition1.getName() + " does not exist");
        partitionedSymbolTable.removePartition(partition1);
    }

}
