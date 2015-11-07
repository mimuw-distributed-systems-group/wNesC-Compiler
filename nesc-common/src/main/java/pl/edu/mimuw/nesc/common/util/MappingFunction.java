package pl.edu.mimuw.nesc.common.util;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Function that returns the value in the given map treating its argument as the
 * key for the map. If an argument is given that is not in the map of the
 * function given at its construction, an IllegalArgumentException object is
 * thrown.
 *
 * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
 */
public final class MappingFunction<T1, T2> implements Function<T1, T2> {
    private final ImmutableMap<T1, T2> map;

    public MappingFunction(Map<T1, T2> map) {
        checkNotNull(map, "map cannot be null");
        this.map = ImmutableMap.copyOf(map);
    }

    @Override
    public T2 apply(T1 valueToMap) {
        final Optional<T2> optResult = Optional.fromNullable(map.get(valueToMap));
        if (!optResult.isPresent()) {
            throw new IllegalArgumentException("no value for object " + valueToMap + " in the map");
        }
        return optResult.get();
    }
}
