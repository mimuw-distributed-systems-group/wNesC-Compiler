package pl.edu.mimuw.nesc.common.util;

import java.util.Arrays;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for {@link IntervalTree} class.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class IntervalTreeTest {
    @Test
    public void testSmallTreeFixed() {
        final int[] elements = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0 };
        final IntervalTree<Integer> tree = new IntervalTree<>(Integer.class,
                new IntegerSumIntervalTreeOperation(), 10);

        assertEquals(16, tree.size());
        assertIntervalsEquivalent(elements, tree);

        for (int i = 0; i < elements.length; ++i) {
            elements[i] = i + 1;
            tree.set(i, i + 1);
            assertIntervalsEquivalent(elements, tree);
        }

        for (int i = 0; i < elements.length; ++i) {
            elements[i] = 0;
            tree.set(i, 0);
            assertIntervalsEquivalent(elements, tree);
        }
    }

    @Test
    public void testMiddleTreeRandom() {
        final Random generator = new Random();
        final int[] elements = new int[128];
        Arrays.fill(elements, 0);
        final IntervalTree<Integer> tree = new IntervalTree<>(Integer.class,
                new IntegerSumIntervalTreeOperation(), 128);

        assertEquals(128, tree.size());
        assertIntervalsEquivalent(elements, tree);

        // Make 8192 random 'set' operations
        for (int i = 0; i < 512; ++i) {
            final int elementIndex = generator.nextInt(elements.length);
            final int value = generator.nextInt();
            elements[elementIndex] = value;
            tree.set(elementIndex, value);
            assertIntervalsEquivalent(elements, tree);
        }
    }

    @Test
    public void testSingleton() {
        final IntervalTree<Integer> tree = new IntervalTree<>(Integer.class,
                new IntegerSumIntervalTreeOperation(), 1);
        assertEquals(0, (int) tree.compute(0, 1));
        tree.set(0, 10);
        assertEquals(10, (int) tree.compute(0, 1));
        tree.set(0, 20);
        assertEquals(20, (int) tree.compute(0, 1));
        tree.set(0, 0);
        assertEquals(0, (int) tree.compute(0, 1));
    }

    private void assertIntervalsEquivalent(int[] elements, IntervalTree<Integer> tree) {
        for (int i = 0; i < elements.length; ++i) {
            int sum = 0;
            for (int j = i; j < elements.length; ++j) {
                sum += elements[j];
                assertEquals(sum, (int) tree.compute(i, j + 1));
            }
        }
    }
}
