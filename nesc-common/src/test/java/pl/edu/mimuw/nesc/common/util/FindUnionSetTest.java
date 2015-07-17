package pl.edu.mimuw.nesc.common.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.*;

/**
 * Tests for the find-union set implementation.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class FindUnionSetTest {
    @Test
    public void testSmallSingletonUnion() {
        final ImmutableSet<Integer> universum = createUniversum(1, 20);

        // Create initial set
        final List<Set<Integer>> sets = new LinkedList<>();
        for (int element : universum) {
            final Set<Integer> newSet = new HashSet<>();
            newSet.add(element);
            sets.add(newSet);
        }

        final FindUnionSet<Integer> findUnion = new FindUnionSet<>(universum);
        assertEquivalent(findUnion, new HashSet<>(sets));

        // Perform the union operations
        final Iterator<Set<Integer>> setsIt = sets.iterator();
        final Set<Integer> first = setsIt.next();
        while (setsIt.hasNext()) {
            final Set<Integer> setToUnion = setsIt.next();
            findUnion.union(first.iterator().next(), setToUnion.iterator().next());
            first.addAll(setToUnion);
            setsIt.remove();
            assertEquivalent(findUnion, new HashSet<>(sets));
        }
    }

    @Test
    public void testMiddleRandom() {
        // Create a set with all elements
        final ImmutableSet<Integer> universum = createUniversum(1, 1000);

        // Create representation of find-union
        final List<Set<Integer>> sets = new ArrayList<>();
        for (int element : universum) {
            final Set<Integer> newSet = new HashSet<>();
            newSet.add(element);
            sets.add(newSet);
        }

        final FindUnionSet<Integer> findUnion = new FindUnionSet<>(universum);
        final Random generator = new Random();

        assertEquivalent(findUnion, new HashSet<>(sets));

        // Perform operations until all sets are merged
        while (sets.size() > 1) {
            final int firstSetIndex = generator.nextInt(sets.size());
            int secondIndexCandidate = generator.nextInt(sets.size() - 1);
            final int secondSetIndex = secondIndexCandidate != firstSetIndex
                    ? secondIndexCandidate
                    : sets.size() - 1;

            final Set<Integer> firstSet = sets.get(firstSetIndex);
            final Set<Integer> secondSet = sets.get(secondSetIndex);

            findUnion.union(firstSet.iterator().next(), secondSet.iterator().next());

            if (firstSet.size() > secondSet.size()) {
                firstSet.addAll(secondSet);
                sets.remove(secondSetIndex);
            } else {
                secondSet.addAll(firstSet);
                sets.remove(firstSetIndex);
            }

            assertEquivalent(findUnion, new HashSet<>(sets));
        }
    }

    private ImmutableSet<Integer> createUniversum(int startNumber, int endNumber) {
        checkArgument(startNumber <= endNumber, "start number must be less or equal than the end number");
        final ImmutableSet.Builder<Integer> universumBuilder = ImmutableSet.builder();
        for (int i = startNumber; i <= endNumber; ++i) {
            universumBuilder.add(i);
        }
        return universumBuilder.build();
    }

    private <T> void assertEquivalent(FindUnionSet<T> findUnion, Set<Set<T>> sets) {
        assertEquals(sets.size(), findUnion.size());

        // Create a representation of the sets in the find-union structure
        final ImmutableMultimap.Builder<T, T> findUnionSetsBuilder =
                ImmutableMultimap.builder();
        for (T element : findUnion.getAllElements()) {
            findUnionSetsBuilder.put(findUnion.find(element), element);
        }
        final ImmutableMultimap<T, T> findUnionSets = findUnionSetsBuilder.build();

        // Check representatives
        assertEquals(findUnionSets.keySet(), findUnion.getAllRepresentatives());

        // Check sets
        final Set<Set<T>> setsInFindUnion = new HashSet<>();
        for (T representative : findUnionSets.keySet()) {
            setsInFindUnion.add(new HashSet<>(findUnionSets.get(representative)));
        }
        assertEquals(sets, setsInFindUnion);
    }
}
