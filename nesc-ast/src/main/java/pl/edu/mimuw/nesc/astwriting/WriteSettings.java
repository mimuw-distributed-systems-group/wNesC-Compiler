package pl.edu.mimuw.nesc.astwriting;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class that allows configuring the way of writing the code.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class WriteSettings {
    /**
     * <p>Object that represents the default settings:</p>
     * <ul>
     *     <li>normal (not unique) names will be used</li>
     *     <li>calls to constant-functions will not be expanded</li>
     *     <li>three spaces constitute a single indentation step</li>
     *     <li>platform default character set will be used</li>
     * </ul>
     */
    public static final WriteSettings DEFAULT_SETTINGS = WriteSettings.builder()
            .nameMode(NameMode.USE_NORMAL_NAMES)
            .uniqueMode(UniqueMode.OUTPUT_CALLS)
            .charset(Charset.defaultCharset().name())
            .indentWithSpaces(3)
            .build();

    private final NameMode nameMode;
    private final UniqueMode uniqueMode;
    private final IndentationType indentationType;
    private final int indentationSize;
    private final Charset charset;

    /**
     * Get a builder that will create a writing settings object.
     *
     * @return Newly created builder with settings set to default values.
     */
    public static Builder builder() {
        return new Builder();
    }

    private WriteSettings(Builder builder) {
        this.nameMode = builder.nameMode;
        this.uniqueMode = builder.uniqueMode;
        this.indentationType = builder.indentationType;
        this.indentationSize = builder.indentationSize;
        this.charset = Charset.forName(builder.charsetName);
    }

    /**
     * Get the name mode as set in this object.
     *
     * @return Name mode in these settings.
     */
    public NameMode getNameMode() {
        return nameMode;
    }

    /**
     * Get the mode of writing NesC constant functions.
     *
     * @return Mode specifying how to write NesC constant functions in this
     *         settings object.
     */
    public UniqueMode getUniqueMode() {
        return uniqueMode;
    }

    /**
     * Get the indentation type set in this settings object.
     *
     * @return Indentation type contained in this settings object.
     */
    public IndentationType getIndentationType() {
        return indentationType;
    }

    /**
     * Get the number indicating count of repetitions of the character for the
     * selected indentation type that create a single indentation.
     *
     * @return The count of repetitions of the indentation character that
     *         constitute a single indentation.
     */
    public int getIndentationSize() {
        return indentationSize;
    }

    /**
     * Get the charset used for writing the code.
     *
     * @return The charset contained in this settings object.
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Builder for the settings of writing.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder {
        private NameMode nameMode = NameMode.USE_NORMAL_NAMES;
        private UniqueMode uniqueMode = UniqueMode.OUTPUT_CALLS;
        private IndentationType indentationType = IndentationType.SPACES;
        private int indentationSize = 3;
        private String charsetName = "UTF-8";

        private Builder() {
        }

        /**
         * Set the names mode that the settings object will contain.
         *
         * @param nameMode The names mode to set.
         * @return <code>this</code>
         */
        public Builder nameMode(NameMode nameMode) {
            this.nameMode = nameMode;
            return this;
        }

        /**
         * Set the mode of writing NesC constant functions.
         *
         * @param uniqueMode The constant functions mode to set.
         * @return <code>this</code>
         */
        public Builder uniqueMode(UniqueMode uniqueMode) {
            this.uniqueMode = uniqueMode;
            return this;
        }

        /**
         * Set the type of indentation to spaces. A single indentation will
         * consist of 'spacesCount' spaces.
         *
         * @param spacesCount Count of spaces used for a single indentation.
         * @return <code>this</code>
         */
        public Builder indentWithSpaces(int spacesCount) {
            this.indentationType = IndentationType.SPACES;
            this.indentationSize = spacesCount;
            return this;
        }

        /**
         * Set the type of indentation to tabs. A single indentation will
         * consist of one tabs.
         *
         * @return <code>this</code>
         */
        public Builder indentWithTabs() {
            this.indentationType = IndentationType.TABS;
            this.indentationSize = 1;
            return this;
        }

        /**
         * Set the character set used for writing.
         *
         * @param charsetName Name of the character set.
         * @return <code>this</code>
         */
        public Builder charset(String charsetName) {
            this.charsetName = charsetName;
            return this;
        }

        private void validate() {
            checkState(nameMode != null, "names mode cannot be set to null");
            checkState(uniqueMode != null, "constant functions mode cannot be set to null");

            try {
                Charset.forName(charsetName);
            } catch (UnsupportedCharsetException e) {
                throw new IllegalStateException("'" + charsetName
                        + "' is not valid name of a character set");
            }
        }

        public WriteSettings build() {
            validate();
            return new WriteSettings(this);
        }
    }

    /**
     * Enumeration type that specifies which names will be used when writing
     * them.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum NameMode {
        USE_UNIQUE_NAMES,
        USE_NORMAL_NAMES,
    }

    /**
     * Enumeration type that specifies how to write calls to NesC constant
     * functions.
     */
    public enum UniqueMode {
        OUTPUT_CALLS,
        OUTPUT_VALUES,
    }

    /**
     * Indentation type to use when writing code.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum IndentationType {
        TABS,
        SPACES,
    }
}
