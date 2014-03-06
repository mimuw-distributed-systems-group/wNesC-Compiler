package pl.edu.mimuw.nesc.preprocessor.directive;


import com.google.common.collect.ImmutableMap;

import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base class for preprocessor directive.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class PreprocessorDirective {

    protected final String sourceFile;
    protected final boolean activeBlock;
    protected final LineRange lineRange;
    protected final Map<Integer, Integer> linesLengths;
    protected final TokenLocation hashLocation;
    // TODO: list of errors in directive

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected PreprocessorDirective(Builder<? extends PreprocessorDirective> builder) {
        this.activeBlock = builder.activeBlock;
        this.lineRange = builder.lineRange;
        this.linesLengths = builder.linesLengths;
        this.hashLocation = builder.hashLocation;
        this.sourceFile = builder.sourceFile;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public boolean isActiveBlock() {
        return activeBlock;
    }

    public LineRange getLineRange() {
        return lineRange;
    }

    public Map<Integer, Integer> getLinesLengths() {
        return linesLengths;
    }

    public TokenLocation getHashLocation() {
        return hashLocation;
    }

    /**
     * Makes possible the visitor to access fields of concrete preprocessor
     * directive instance.
     *
     * @param visitor visitor
     * @param arg     argument
     * @param <R>     returned value's class
     * @param <A>     argument's class
     * @return depends on visitor
     */
    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    /**
     * Preprocessor directives visitor.
     *
     * @param <R> returned value's class
     * @param <A> argument's class
     */
    public interface Visitor<R, A> {
        R visit(IncludeDirective include, A arg);

        R visit(DefineDirective define, A arg);

        R visit(UndefDirective undef, A arg);

        R visit(IfDirective _if, A arg);

        R visit(IfdefDirective ifdef, A arg);

        R visit(IfndefDirective ifndef, A arg);

        R visit(ElseDirective _else, A arg);

        R visit(ElifDirective elif, A arg);

        R visit(EndifDirective endif, A arg);

        R visit(ErrorDirective error, A arg);

        R visit(WarningDirective warning, A arg);

        R visit(PragmaDirective pragma, A arg);

        R visit(LineDirective line, A arg);

        R visit(UnknownDirective unknown, A arg);
    }

    /**
     * Represents a range of line numbers. Both range ends are inclusive.
     */
    public static final class LineRange {

        private final int start;
        private final int end;

        LineRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public boolean isMultiline() {
            return (start < end);
        }
    }

    /**
     * Token location.
     */
    public static final class TokenLocation {

        private final int line;
        private final int column;
        private final int length;

        TokenLocation(int line, int column, int length) {
            checkState(line > 0, "line must be a positive number; actual " + line);
            checkState(column > 0, "column must be a positive number; actual " + column);
            checkState(length > 0, "length must be a positive number; actual " + length);

            this.line = line;
            this.column = column;
            this.length = length;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public int getLength() {
            return length;
        }
    }

    /**
     * Base builder class for preprocessor directives.
     *
     * @param <T> concrete preprocessor directive class
     */
    public static abstract class Builder<T extends PreprocessorDirective> {

        protected String sourceFile;
        protected boolean activeBlock;
        protected int lineRangeStart;
        protected int lineRangeEnd;
        protected LineRange lineRange;
        protected ImmutableMap.Builder<Integer, Integer> linesLengthsBuilder;
        protected Map<Integer, Integer> linesLengths;
        protected TokenLocation hashLocation;
        protected TokenLocation keywordLocation;

        public Builder() {
            this.lineRangeStart = -1;
            this.lineRangeEnd = -1;
            this.linesLengthsBuilder = ImmutableMap.builder();
        }

        public Builder<T> sourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public Builder<T> activeBlock(boolean activeBlock) {
            this.activeBlock = activeBlock;
            return this;
        }

        /**
         * Adds pair (line number, line length). Duplicated keys are not
         * allowed, and will cause build to fail.
         *
         * @param lineNumber line number
         * @param lineLength line length
         * @return builder
         */
        public Builder<T> addLine(int lineNumber, int lineLength) {
            linesLengthsBuilder.put(lineNumber, lineLength);
            return this;
        }

        /**
         * Adds pairs (line number, line length). Duplicated keys are not
         * allowed, and will cause build to fail.
         *
         * @param linesMap map of pairs (line number, line length)
         * @return builder
         */
        public Builder<T> addLines(Map<Integer, Integer> linesMap) {
            linesLengthsBuilder.putAll(linesMap);
            return this;
        }

        /**
         * Sets location of hash #.
         *
         * @param line   line
         * @param column column
         * @return builder
         */
        public Builder<T> hashLocation(int line, int column) {
            this.hashLocation = new TokenLocation(line, column, 1);
            return this;
        }

        /**
         * Sets preprocessor directive name (keyword) location.
         *
         * @param line   line
         * @param column column
         * @param length length
         * @return builder
         */
        public Builder<T> keywordLocation(int line, int column, int length) {
            this.keywordLocation = new TokenLocation(line, column, length);
            return this;
        }

        /**
         * Creates preprocessor directive instance.
         *
         * @return preprocessor directive instance
         */
        public final T build() {
            prepare();
            verify();
            return getInstance();
        }

        /**
         * Builds all unfinished attributes before verification and creating
         * the final object.
         */
        protected void prepare() {
            this.linesLengths = linesLengthsBuilder.build();
            setLineRange();
        }

        /**
         * Checks correctness of builder parameters. Should be overridden in
         * subclass. <code>super.verify()</code> should be called first.
         */
        protected void verify() {
            checkState(sourceFile != null, "sourceFile must be set");
            checkState(!linesLengths.isEmpty(), "at least one line must be specified");
            checkState(this.lineRangeStart > 0, "line number must be a positive number; actual " + this.lineRangeStart);
            checkState(this.lineRangeEnd > 0, "line number must be a positive number; actual " + this.lineRangeEnd);
            checkState(this.lineRangeStart <= this.lineRangeEnd, "start must be less or equal to end; "
                    + this.lineRangeStart + ", " + this.lineRangeEnd);
        }

        /**
         * Returns an object that is currently being built.
         *
         * @return final object instance
         */
        protected abstract T getInstance();

        private void setLineRange() {
            final Iterator<Integer> it = this.linesLengths.keySet().iterator();

            final Integer first = it.next();
            this.lineRangeStart = first;
            this.lineRangeEnd = first;

            while (it.hasNext()) {
                final int lineNum = it.next();
                this.lineRangeStart = Math.min(this.lineRangeStart, lineNum);
                this.lineRangeEnd = Math.max(this.lineRangeEnd, lineNum);
            }
            this.lineRange = new LineRange(this.lineRangeStart, this.lineRangeEnd);
        }
    }

}
