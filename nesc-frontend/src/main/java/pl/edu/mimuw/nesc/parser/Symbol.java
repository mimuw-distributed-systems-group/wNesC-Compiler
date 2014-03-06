package pl.edu.mimuw.nesc.parser;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.LexicalCst;

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
 *
 */
public class Symbol {

	/**
	 *
	 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
	 *
	 */
	public static class Builder {

		private int symbolCode;
		private int line;
		private int column;
		private String file;
		private Object value;

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

		public Builder file(String file) {
			this.file = file;
			return this;
		}

		public Builder value(Object value) {
			this.value = value;
			return this;
		}

		public Symbol build() {
			return new Symbol(this);
		}

	}

	public static Builder builder() {
		return new Builder();
	}

	private int symbolCode;
	private final int line;
	private final int column;
	private final String file;
	private final Object value;

	/**
	 * Creates new symbol.
	 *
	 * @param symbolCode
	 *            symbol code defined in {@link Parser}
	 * @param line
	 *            line
	 * @param column
	 *            Column
	 * @param file
	 *            source file
	 * @param value
	 *            value associated with token
	 */
	public Symbol(int symbolCode, int line, int column, String file, Object value) {
		this.symbolCode = symbolCode;
		this.line = line;
		this.column = column;
		this.file = file;
		this.value = value;
	}

	private Symbol(Builder builder) {
		this.symbolCode = builder.symbolCode;
		this.line = builder.line;
		this.column = builder.column;
		this.file = builder.file;
		this.value = builder.value;
	}

	/**
	 * Returns symbol code.
	 *
	 * @return symbol code
	 */
	public int getSymbolCode() {
		return symbolCode;
	}

	/**
	 * Sets symbol code.
	 *
	 * @param symbolCode
	 *            new symbol code
	 */
	void setSymbolCode(int symbolCode) {
		this.symbolCode = symbolCode;
	}

	/**
	 * Returns line number.
	 *
	 * @return line number
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Returns column number.
	 *
	 * @return column number
	 */
	public int getColumn() {
		return column;
	}

	/**
	 * Returns file path which current token comes from.
	 *
	 * @return file path
	 */
	public String getFile() {
		return file;
	}

	/**
	 * Returns value associated with token.
	 *
	 * @return value
	 */
	public Object getValue() {
		return value;
	}

	public Location getLocation() {
		return new Location(file, line, column);
	}

	@Override
	public String toString() {
		return "#" + symbolCode;
	}

	public String print() {
		Object v = value;
		if (value instanceof LexicalCst) {
			v = ((LexicalCst) value).getCstring().getData();
		}
		return "{" + symbolCode + " at (" + file + ", " + line + ", " + column + ") + [" + v + "]};";
	}

}
