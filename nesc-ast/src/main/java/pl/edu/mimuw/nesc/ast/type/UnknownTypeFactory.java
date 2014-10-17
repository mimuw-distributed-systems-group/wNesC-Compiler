package pl.edu.mimuw.nesc.ast.type;

import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import java.util.LinkedList;
import java.util.List;

/**
 * A factory that is responsible for producing objects of a proper subclass of
 * <code>UnknownType</code> class (or that class itself).
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnknownTypeFactory {
    /**
     * Names of attributes that can be applied to a type parameter.
     */
    private static final String ATTRIBUTE_NUMBER = "number";
    private static final String ATTRIBUTE_INTEGER = "integer";

    /**
     * Name of the type that will be created.
     */
    private String name;

    /**
     * Attributes that were applied to the name of the type in a generic
     * parameter declaration (either a component or an interface parameter).
     */
    private final LinkedList<NescAttribute> attributes = new LinkedList<>();

    /**
     * Get a new instance of the unknown type factory.
     *
     * @return Newly created instance of the unknown type factory.
     */
    public static UnknownTypeFactory newInstance() {
        return new UnknownTypeFactory();
    }

    /**
     * Private constructor to enforce using factory method
     * {@link UnknownTypeFactory#newInstance newInstance}.
     */
    private UnknownTypeFactory() {
    }

    /**
     * Set the name that will be used for the unknown type to create.
     *
     * @param name Name to set.
     * @return <code>this</code>
     */
    public UnknownTypeFactory setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * <p>Add attributes that were applied for the type. The argument is
     * a list type elements because currently the attributes are not
     * correctly extracted from them. Actually, it shall be a list of
     * attributes.</p>
     * <p>Only objects that represent NesC attributes are used from the
     * given list.</p>
     *
     * @param typeElements List with type elements.
     * @return <code>this</code>
     */
    public UnknownTypeFactory addAttributes(List<? extends TypeElement> typeElements) {
        for (TypeElement typeElement : typeElements) {
            if (typeElement instanceof NescAttribute) {
                attributes.add((NescAttribute) typeElement);
            }
        }

        return this;
    }

    /**
     * <p>Create an unknown type that reflects the information provided to this
     * factory. It is created in the following way:</p>
     * <ol>
     *     <li>If <code>@integer()</code> attribute has been added, then an
     *     instance of <code>UnknownIntegerType</code> is created.</li>
     *     <li>Otherwise, if <code>@number()</code> attribute has been added,
     *     then an instance of <code>UnknownArithmeticType</code> is
     *     created.</li>
     *     <li>Otherwise, an instance of <code>UnknownType</code> is created.
     *     </li>
     * </ol>
     * <p>The returned type is neither const-qualified nor volatile-qualified.
     * </p>
     *
     * @return A newly created instance of an unknown type.
     */
    public UnknownType newUnknownType() {
        boolean numberAttribute = false;

        for (NescAttribute attr : attributes) {
            final String attrName = attr.getName().getName();

            if (ATTRIBUTE_INTEGER.equals(attrName)) {
                return new UnknownIntegerType(name);
            } else if (ATTRIBUTE_NUMBER.equals(attrName)) {
                numberAttribute = true;
            }
        }

        return numberAttribute
                ? new UnknownArithmeticType(name)
                : new UnknownType(name);
    }
}
