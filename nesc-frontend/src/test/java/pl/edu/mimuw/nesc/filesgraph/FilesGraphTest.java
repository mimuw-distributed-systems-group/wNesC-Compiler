package pl.edu.mimuw.nesc.filesgraph;

import static org.junit.Assert.*;

import com.google.common.collect.Iterables;
import org.junit.Test;


/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class FilesGraphTest {

    private static final String FILE_1_NAME = "file1";
    private static final String FILE_2_NAME = "file2";
    private static final String FILE_3_NAME = "file3";
    private static final String FILE_4_NAME = "file4";

    @Test
    public void addSeveralNodes() {
        final FilesGraph graph = new FilesGraph();

        final GraphFile file1 = new GraphFile(FILE_1_NAME);
        graph.addFile(file1);
        final GraphFile file2 = new GraphFile(FILE_2_NAME);
        graph.addFile(file2);
        final GraphFile file3 = new GraphFile(FILE_3_NAME);
        graph.addFile(file3);
        final GraphFile file4 = new GraphFile(FILE_4_NAME);
        graph.addFile(file4);

        graph.addEdge(file1, file2);
        graph.addEdge(file1, file3);
        graph.addEdge(file2, file3);
        graph.addEdge(file2, file4);
        graph.addEdge(file3, file4);

        // 1
        assertEquals(2, file1.getUses().size());
        assertTrue(Iterables.contains(file1.getUses().values(), file2));
        assertTrue(Iterables.contains(file1.getUses().values(), file3));

        assertEquals(0, file1.getIsUsedBy().size());

        // 2
        assertEquals(2, file2.getUses().size());
        assertTrue(Iterables.contains(file2.getUses().values(), file3));
        assertTrue(Iterables.contains(file2.getUses().values(), file4));

        assertEquals(1, file2.getIsUsedBy().size());
        assertTrue(Iterables.contains(file2.getIsUsedBy().values(), file1));

        // 3
        assertEquals(1, file3.getUses().size());
        assertTrue(Iterables.contains(file3.getUses().values(), file4));

        assertEquals(2, file3.getIsUsedBy().size());
        assertTrue(Iterables.contains(file3.getIsUsedBy().values(), file1));
        assertTrue(Iterables.contains(file3.getIsUsedBy().values(), file2));

        // 4
        assertEquals(0, file4.getUses().size());

        assertEquals(2, file4.getIsUsedBy().size());
        assertTrue(Iterables.contains(file4.getIsUsedBy().values(), file2));
        assertTrue(Iterables.contains(file4.getIsUsedBy().values(), file3));
    }

    @Test
    public void removeOneNode() {
        final FilesGraph graph = new FilesGraph();

        final GraphFile file1 = new GraphFile(FILE_1_NAME);
        graph.addFile(file1);
        final GraphFile file2 = new GraphFile(FILE_2_NAME);
        graph.addFile(file2);
        final GraphFile file3 = new GraphFile(FILE_3_NAME);
        graph.addFile(file3);
        final GraphFile file4 = new GraphFile(FILE_4_NAME);
        graph.addFile(file4);

        graph.addEdge(file1, file2);
        graph.addEdge(file1, file3);
        graph.addEdge(file2, file3);
        graph.addEdge(file2, file4);
        graph.addEdge(file3, file4);

        graph.removeFile(file3);

        // 1
        assertEquals(1, file1.getUses().size());
        assertTrue(Iterables.contains(file1.getUses().values(), file2));

        assertEquals(0, file1.getIsUsedBy().size());

        // 2
        assertEquals(1, file2.getUses().size());
        assertTrue(Iterables.contains(file2.getUses().values(), file4));

        assertEquals(1, file2.getIsUsedBy().size());
        assertTrue(Iterables.contains(file2.getIsUsedBy().values(), file1));

        // 3
        assertNull(graph.getFile(FILE_3_NAME));

        // 4
        assertEquals(0, file4.getUses().size());

        assertEquals(1, file4.getIsUsedBy().size());
        assertTrue(Iterables.contains(file4.getIsUsedBy().values(), file2));
    }

}
