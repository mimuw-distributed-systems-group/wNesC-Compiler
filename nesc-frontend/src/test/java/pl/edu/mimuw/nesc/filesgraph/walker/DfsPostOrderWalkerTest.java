package pl.edu.mimuw.nesc.filesgraph.walker;

import org.junit.Test;
import pl.edu.mimuw.nesc.filesgraph.FilesGraph;
import pl.edu.mimuw.nesc.filesgraph.GraphFile;

import static org.junit.Assert.assertEquals;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DfsPostOrderWalkerTest {

    private static final String FILE_1_NAME = "file1";
    private static final String FILE_2_NAME = "file2";
    private static final String FILE_3_NAME = "file3";
    private static final String FILE_4_NAME = "file4";

    @Test
    public void performWalkOnSmallTree() {
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
        graph.addEdge(file1, file4);
        graph.addEdge(file2, file3);

        // 1
        final TestAction action1 = new TestAction(new GraphFile[]{file3, file2, file4, file1});
        final FilesGraphWalker walker1 = FilesGraphWalkerFactory.ofDfsPostOrderWalker(graph, action1);
        walker1.walk(FILE_1_NAME);
        action1.finished();

        // 2
        final TestAction action2 = new TestAction(new GraphFile[]{file3, file2});
        final FilesGraphWalker walker2 = FilesGraphWalkerFactory.ofDfsPostOrderWalker(graph, action2);
        walker2.walk(FILE_2_NAME);
        action2.finished();

        // 3
        final TestAction action3 = new TestAction(new GraphFile[]{file3});
        final FilesGraphWalker walker3 = FilesGraphWalkerFactory.ofDfsPostOrderWalker(graph, action3);
        walker3.walk(FILE_3_NAME);
        action3.finished();

        // 4
        final TestAction action4 = new TestAction(new GraphFile[]{file4});
        final FilesGraphWalker walker4 = FilesGraphWalkerFactory.ofDfsPostOrderWalker(graph, action4);
        walker4.walk(FILE_4_NAME);
        action4.finished();
    }

    private static class TestAction implements NodeAction {

        private final GraphFile[] order;
        private int counter;

        TestAction(GraphFile[] order) {
            this.order = order;
            this.counter = 0;
        }

        @Override
        public void run(GraphFile graphFile) {
            assertEquals(order[counter], graphFile);
            counter++;
        }

        public void finished() {
            assertEquals(counter, order.length);
        }
    }

}
