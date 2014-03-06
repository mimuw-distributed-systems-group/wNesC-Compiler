package pl.edu.mimuw.nesc.ast.use;

import pl.edu.mimuw.nesc.ast.Context;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;

public class IdUse extends Use {

	private DataDeclaration id;

	public IdUse(Location location, DataDeclaration fn, Context context) {
		super(location, fn, context);
	}

	public DataDeclaration getId() {
		return id;
	}

	public void setId(DataDeclaration id) {
		this.id = id;
	}

}
