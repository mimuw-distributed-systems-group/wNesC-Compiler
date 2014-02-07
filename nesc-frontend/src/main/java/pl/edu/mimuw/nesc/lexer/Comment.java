package pl.edu.mimuw.nesc.lexer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * Represents comment, both a multiple line and a single line one.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 *
 */
public final class Comment {

	public static Builder builder() {
		return new Builder();
	}

	private final Location location;
	private final String body;
	private final boolean isC;

	private Comment(Builder builder) {
		this.location = new Location(builder.file, builder.line, builder.column);
		this.body = builder.body;
		this.isC = builder.isC;
	}

	public Location getLocation() {
		return location;
	}

	public String getBody() {
		return body;
	}

	public boolean isC() {
		return isC;
	}

    @Override
    public String toString() {
        return "Comment{" +
                "location=" + location +
                ", body='" + body.substring(0, Math.min(body.length(), 100)) + '\'' +
                ", isC=" + isC +
                '}';
    }

    /**
     * Comment builder.
     */
    public static final class Builder {

		private int line;
		private int column;
		private String file;
		private String body;
		private boolean isC;

		public Builder() {
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

		public Builder body(String body) {
			this.body = body;
			return this;
		}

		public Builder isC(boolean isC) {
			this.isC = isC;
			return this;
		}

		public Comment build() {
			verify();
			return new Comment(this);
		}

		private void verify() {
			checkArgument(line >= 0, "line should be non-negative");
			checkArgument(column >= 0, "line should be non-negative");
			checkNotNull(file, "file path should not be null");
			checkNotNull(body, "body should no be null");
		}
	}

}
