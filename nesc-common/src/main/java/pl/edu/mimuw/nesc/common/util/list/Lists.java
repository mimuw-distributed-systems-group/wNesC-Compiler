package pl.edu.mimuw.nesc.common.util.list;

import java.util.Collections;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>
 * This class provides common operations on lists.
 * </p>
 * <p>
 * Generic methods could be used as follows:
 * <code>Lists.&lt;Expression&gt;newList()</code>. When the type parameter T can
 * be inferred from passed parameters, we can simply write:
 * <code>Lists.newList(item)</code>.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Lists {
    /**
     * Creates an empty list.
     *
     * @return an empty list
     */
    public static <T> LinkedList<T> newList() {
        return new LinkedList<>();
    }

    /**
     * Creates a list with specified element.
     *
     * @param item element
     * @return a list with specified element
     * @throws NullPointerException item is null
     */
    public static <T> LinkedList<T> newList(T item) {
        checkNotNull(item, "item should not be null");

        LinkedList<T> result = new LinkedList<>();
        result.add(item);
        return result;
    }

    /**
     * Creates a list with specified element. When parameter is
     * <code>null</code> an empty list is returned.
     *
     * @param item element
     * @return a list with specified element
     */
    public static <T> LinkedList<T> newListEmptyOnNull(T item) {
        LinkedList<T> result = new LinkedList<>();
        if (item != null) {
            result.add(item);
        }
        return result;
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param list the list which specified element should be added to
     * @param item the element to add
     * @return the list with added element
     * @throws NullPointerException either list or item is null
     */
    public static <T> LinkedList<T> addFirst(LinkedList<T> list, T item) {
        checkNotNull(list, "list should not be null");
        checkNotNull(item, "item should not be null");

        list.addFirst(item);
        return list;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param list the list which specified element should be added to
     * @param item element to be appended to this list
     * @return the list with appended element
     * @throws NullPointerException either list or item is null
     */
    public static <T> LinkedList<T> chain(LinkedList<T> list, T item) {
        // FIXME: neither list or item should not be null
        if (list == null) {
            return newListEmptyOnNull(item);
        } else if (item == null) {
            return list;
        }
        list.add(item);
        return list;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's Iterator.
     *
     * @param list1 the first list
     * @param list2 the second list
     * @return merged lists
     * @throws NullPointerException any of specified lists is null
     */
    public static <T> LinkedList<T> chain(LinkedList<T> list1, LinkedList<T> list2) {
        checkNotNull(list1, "list1 should not be null");
        checkNotNull(list2, "list2 should not be null");

        if (list1.isEmpty())
            return list2;
        if (list2.isEmpty())
            return list1;

        list1.addAll(list2);
        return list1;
    }

    /**
     * Reverses the order of the elements in the specified list.
     *
     * @param list the list whose elements are to be reversed
     * @return the list with reversed the order of elements
     * @throws NullPointerException list is null
     */
    public static <T> LinkedList<T> reverse(LinkedList<T> list) {
        checkNotNull(list, "list should not be null");

        Collections.reverse(list);
        return list;
    }

    /**
     * Converts a list of objects of certain subclass to a list of superclass
     * elements.
     *
     * @param list list to be converted
     * @return a list of superclass elements
     */
    public static <S extends T, T> LinkedList<T> convert(LinkedList<S> list) {
        LinkedList<T> result = new LinkedList<T>(list);
        return result;
    }

}
