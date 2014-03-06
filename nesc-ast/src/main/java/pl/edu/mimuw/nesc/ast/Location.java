package pl.edu.mimuw.nesc.ast;

/**
 * Represents exact location of token or language construct in source file.
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public final class Location {

	private final String filename;
	private final int line;
	private final int column;

	public Location(String filename, int line, int column) {
		this.filename = filename;
		this.line = line;
		this.column = column;
	}

	public String getFilename() {
		return filename;
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

    @Override
    public String toString() {
        return "Location{" +
                "filename='" + filename + '\'' +
                ", line=" + line +
                ", column=" + column +
                '}';
    }
}
