package pl.edu.mimuw.nesc.lexer;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
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
    private final boolean invalid;

	private Comment(Builder builder) {
		this.location = new Location(builder.file, builder.line, builder.column);
		this.body = builder.body;
		this.isC = builder.isC;
        this.invalid = builder.invalid;
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

    public boolean isInvalid() {
        return invalid;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("location", location)
                .add("body", body.substring(0, Math.min(body.length(), 100)))
                .add("isC", isC)
                .add("invalid", invalid)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location, body, isC, invalid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Comment other = (Comment) obj;
        return Objects.equal(this.location, other.location)
                && Objects.equal(this.body, other.body)
                && Objects.equal(this.isC, other.isC)
                && Objects.equal(this.invalid, other.invalid);
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
        private boolean invalid;

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

        public Builder invalid(boolean invalid) {
            this.invalid = invalid;
            return this;
        }

		public Comment build() {
			verify();
			return new Comment(this);
		}

		private void verify() {
			checkState(line >= 0, "line should be non-negative");
            checkState(column >= 0, "line should be non-negative");
            checkState(file != null, "file path should not be null");
            checkState(body != null, "body should no be null");
		}
	}

}
