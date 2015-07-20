package pl.edu.mimuw.nesc.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the navigable inverse map.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class NavigableInverseMapTest {
    @Test
    public void testSmallFixed() {
        final Map<Integer, Integer> map = new HashMap<>();
        final NavigableInverseMap<Integer, Integer> inverseMap = new NavigableInverseMap<>();
        int value = 0;
        int increment = 1;

        for (int key = 0; key < 100; ++key) {
            map.put(key, value);
            inverseMap.put(key, value);

            assertEquivalent(map, inverseMap);
            assertIntegral(inverseMap);

            if (key % 3 == 0) {
                value += increment;
                ++increment;
            }
        }

        for (int key = 0; key < 100; ++key) {
            final int valueMap = map.remove(key);
            final int valueInverseMap = inverseMap.remove(key);
            assertEquals(valueMap, valueInverseMap);
            assertEquivalent(map, inverseMap);
            assertIntegral(inverseMap);
        }

        assertTrue(inverseMap.isEmpty());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMiddleRandom() {
        final Map<Integer, Integer> map = new HashMap<>();
        final NavigableInverseMap<Integer, Integer> inverseMap = new NavigableInverseMap<>();
        final Random generator = new Random();

        for (int i = 0; i < 100000; ++i) {
            final Integer prevValueMap, prevValueInverseMap;

            switch (generator.nextInt(2)) {
                case 0: {
                    // Put a new mapping
                    final int key = generator.nextInt(30);
                    final int value = generator.nextInt(10);
                    prevValueMap = map.put(key, value);
                    prevValueInverseMap = inverseMap.put(key, value);
                    break;
                }
                case 1: {
                    // Remove a mapping
                    final int key = generator.nextInt(30);
                    prevValueMap = map.remove(key);
                    prevValueInverseMap = inverseMap.remove(key);
                    break;
                }
                default:
                    throw new RuntimeException("unexpected value");
            }


            assertEquals(prevValueMap, prevValueInverseMap);
            assertEquivalent(map, inverseMap);
            assertIntegral(inverseMap);
        }

        map.clear();
        inverseMap.clear();

        assertTrue(map.isEmpty());
        assertTrue(inverseMap.isEmpty());
    }

    private <K, V> void assertEquivalent(Map<K, V> expected, NavigableInverseMap<K, V> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        assertTrue(expected.values().containsAll(actual.values()));
        assertTrue(actual.values().containsAll(expected.values()));
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        assertEquals(expected.entrySet(), actual.entrySet());

        // Check 'containsKey'
        for (K key : expected.keySet()) {
            assertTrue(actual.containsKey(key));
        }

        // Check 'containsValue'
        for (V value : expected.values()) {
            assertTrue(actual.containsValue(value));
        }

        // Check 'equals'
        assertEquals(expected, actual);
    }

    private <K, V> void assertIntegral(NavigableInverseMap<K, V> map) {
        final NavigableMap<V, NavigableSet<K>> expectedInverse = new TreeMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!expectedInverse.containsKey(entry.getValue())) {
                expectedInverse.put(entry.getValue(), new TreeSet<K>());
            }
            expectedInverse.get(entry.getValue()).add(entry.getKey());
        }

        assertEquals(expectedInverse, map.inverseMap());
    }
}
