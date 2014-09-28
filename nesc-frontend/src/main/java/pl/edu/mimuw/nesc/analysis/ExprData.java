package pl.edu.mimuw.nesc.analysis;

import pl.edu.mimuw.nesc.ast.type.Type;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class whose objects contain results of analysis of an expression.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ExprData {
    /**
     * The type of the expression.
     */
    private Type type;

    /**
     * <code>true</code> if and only if the analyzed expression is an
     * lvalue.
     */
    private boolean isLvalue;

    /**
     * <code>true</code> if and only if the analyzed expression designates
     * a bit-field.
     */
    private boolean isBitField;

    /**
     * <code>true</code> if and only if the analyzed expression is a null
     * pointer constant.
     */
    private boolean isNullPointerConstant;

    /**
     * Get the builder for an expression data object.
     *
     * @return Newly created builder that will build an expression data object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize this object with information from the builder.
     *
     * @param builder Builder with the information about expression.
     */
    private ExprData(Builder builder) {
        this.type = builder.type;
        this.isLvalue = builder.isLvalue;
        this.isBitField = builder.isBitField;
        this.isNullPointerConstant = builder.isNullPointerConstant;
    }

    /**
     * Get the type of the analyzed expression.
     *
     * @return Type of the expression. Never null.
     */
    public Type getType() {
        return type;
    }

    /**
     * Check if the expression is an lvalue.
     *
     * @return <code>true</code> if and only if the analyzed expression
     *         designates an lvalue.
     */
    public boolean isLvalue() {
        return isLvalue;
    }

    /**
     * Check if the expression designates a bit-field.
     *
     * @return <code>true</code> if and only if the analyzed expression
     *         designates a bit-field.
     */
    public boolean isBitField() {
        return isBitField;
    }

    /**
     * Check if the expression is a null pointer constant.
     *
     * @return <code>true</code> if and only if the analyzed expression is
     *         a null pointer constant.
     */
    public boolean isNullPointerConstant() {
        return isNullPointerConstant;
    }

    /**
     * Changes the type contained in this data object to the type that is the
     * result of decaying it.
     *
     * @return <code>this</code>
     */
    ExprData decayType() {
        type = type.decay();
        return this;
    }

    /**
     * <p>Performs the operation of lvalue conversion. If the data in this
     * object depicts an lvalue that does not have array type, the data are
     * changed in the following way:</p>
     * <ul>
     *     <li>the flag about being lvalue is cleared</li>
     *     <li>the type is changed to the unqualified version of the same
     *     type</li>
     * </ul>
     * <p>Otherwise, this method has no side effects.</p>
     *
     * @return <code>this</code>
     */
    ExprData lvalueConversion() {
        if (isLvalue && !type.isArrayType()) {
            type = type.removeQualifiers();
            isLvalue = false;
        }

        return this;
    }

    /**
     * Builder for an expression data.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        private Type type;
        private boolean isLvalue;
        private boolean isBitField;
        private boolean isNullPointerConstant;

        /**
         * Restrict the permissions for instantiating a builder.
         */
        private Builder() {
        }

        /**
         * Set the type of the expression data object that will be created.
         *
         * @param type The type to set.
         * @return <code>this</code>
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Set if the expression is an lvalue.
         *
         * @param isLvalue Value to set.
         * @return <code>this</code>
         */
        public Builder isLvalue(boolean isLvalue) {
            this.isLvalue = isLvalue;
            return this;
        }

        /**
         * Set if the expression designates a bit-field.
         *
         * @param isBitField Value to set.
         * @return <code>this</code>
         */
        public Builder isBitField(boolean isBitField) {
            this.isBitField = isBitField;
            return this;
        }

        /**
         * Set if the expression is a null pointer constant.
         *
         * @param isNullPointerConstant Value to set.
         * @return <code>this</code>
         */
        public Builder isNullPointerConstant(boolean isNullPointerConstant) {
            this.isNullPointerConstant = isNullPointerConstant;
            return this;
        }

        private void validate() {
            checkNotNull(type, "type cannot be null");
        }

        public ExprData build() {
            validate();
            return new ExprData(this);
        }
    }
}
