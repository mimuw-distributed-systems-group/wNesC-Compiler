package pl.edu.mimuw.nesc.ast;


import com.google.common.base.Objects;

/**
 * Represents exact location of token or language construct in source file.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class Location {

    private static final Location DUMMY_LOCATION = new Location("", -1, -1);

    /**
     * Dummy location which indicates that:
     * <ul>
     * <li>token is not present in source file</li>
     * <li>language construct is empty or does not exist corresponding
     * tokens in source file</li>
     * </ul>
     *
     * @return dummy location
     */
    public static Location getDummyLocation() {
        return DUMMY_LOCATION;
    }

    private final String filePath;
    private final int line;
    private final int column;

    public Location(String filePath, int line, int column) {
        this.filePath = filePath;
        this.line = line;
        this.column = column;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("line", line)
                .add("column", column)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(line, column);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Location other = (Location) obj;
        return Objects.equal(this.line, other.line) && Objects.equal(this.column, other.column);
    }
}
