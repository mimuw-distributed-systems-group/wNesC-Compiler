package pl.edu.mimuw.nesc.names.mangling;

import java.util.Collection;

/**
 * <p>Interface with operations that a name mangler should support.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface NameMangler {
    /**
     * Mangles the given name to a unique name that differs from all previously
     * returned values by this method and {@link NameMangler#remangle} and all
     * forbidden names.
     *
     * @param name Name to mangle.
     * @return Name after mangling. Names returned by every call to this method
     *         and {@link NameMangler#remangle} of the same instance are unique.
     * @throws NullPointerException Name is <code>null</code>.
     * @throws IllegalArgumentException Name is an empty string.
     */
    String mangle(String name);

    /**
     * Takes a mangled name and mangles it again to a different name. All names
     * returned by this method and {@link NameMangler#mangle} are unique. It is
     * also guaranteed that a name that is currently forbidden will not be
     * returned by this method.
     *
     * @param mangledName Name that has been returned by a call to this method
     *                    or {@link NameMangler#mangle} and shall be mangled
     *                    again.
     * @return Name after mangling again.
     * @throws NullPointerException Mangled name is <code>null</code>.
     * @throws IllegalArgumentException Name is an empty string or is not
     *                                  a name that has been previously
     *                                  returned by this method or
     *                                  {@link NameMangler#mangle}.
     * @throws UnsupportedOperationException This method is not supported by
     *                                       this mangler.
     */
    String remangle(String mangledName);

    /**
     * Adds a forbidden name to the mangler. A forbidden name is a name that
     * will never be returned by {@link NameMangler#mangle} method.
     *
     * @param name Name to deny.
     * @return <code>true</code> if and only if the given name has not been
     *         already forbidden for this mangler.
     * @throws NullPointerException Name is <code>null</code>.
     * @throws IllegalArgumentException Name is an empty string.
     */
    boolean addForbiddenName(String name);

    /**
     * Adds all names from the given collection as forbidden – none of them
     * will be returned by {@link NameMangler#remangle} or
     * {@link NameMangler#mangle} after call to this method.
     *
     * @param names Names to forbid.
     * @throws NullPointerException The argument is <code>null</code>.
     * @throws IllegalArgumentException One of the elements of the collection
     *                                  is an empty string or <code>null</code>.
     */
    void addForbiddenNames(Collection<String> names);
}
