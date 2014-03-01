package pl.edu.mimuw.nesc.parser;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * <p>
 * Symbol is passed from lexer to parser.
 * </p>
 * <p>
 * Contains all necessary information: code of symbol, value associated with
 * token, number of line and column.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Symbol {

    public static Builder builder() {
        return new Builder();
    }

    private int symbolCode;
    private final Location startLocation;
    private final Location endLocation;
    private final String value;

    private Symbol(Builder builder) {
        this.symbolCode = builder.symbolCode;
        this.value = builder.value;
        this.startLocation = new Location(builder.file, builder.line, builder.column);
        this.endLocation = new Location(builder.file, builder.endLine, builder.endColumn);
    }

    public int getSymbolCode() {
        return symbolCode;
    }

    public void setSymbolCode(int symbolCode) {
        this.symbolCode = symbolCode;
    }

    public Location getLocation() {
        return startLocation;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("symbolCode", symbolCode)
                .add("startLocation", startLocation)
                .add("endLocation", endLocation)
                .add("value", value)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(symbolCode, startLocation, endLocation, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Symbol other = (Symbol) obj;
        return Objects.equal(this.symbolCode, other.symbolCode)
                && Objects.equal(this.startLocation, other.startLocation)
                && Objects.equal(this.endLocation, other.endLocation)
                && Objects.equal(this.value, other.value);
    }

    /**
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    public static class Builder {

        private int symbolCode;
        private int line;
        private int column;
        private int endLine;
        private int endColumn;
        private String file;
        private String value;

        public Builder symbolCode(int symbolCode) {
            this.symbolCode = symbolCode;
            return this;
        }

        public Builder line(int line) {
            this.line = line;
            return this;
        }

        public Builder column(int column) {
            this.column = column;
            return this;
        }

        public Builder endLine(int line) {
            this.endLine = line;
            return this;
        }

        public Builder endColumn(int column) {
            this.endColumn = column;
            return this;
        }

        public Builder file(String file) {
            this.file = file;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Symbol build() {
            // TODO: verify?
            return new Symbol(this);
        }

    }
}
