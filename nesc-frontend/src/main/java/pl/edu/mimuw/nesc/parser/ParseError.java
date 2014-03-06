package pl.edu.mimuw.nesc.parser;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * Represents parse error.
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class ParseError {

	final Location location;
	final String message;

	public ParseError(Location location, String message) {
		this.location = location;
		this.message = message;
	}

	public Location getLocation() {
		return location;
	}

	public String getMessage() {
		return message;
	}

}
