package pl.edu.mimuw.nesc.ast.use;

import pl.edu.mimuw.nesc.ast.Context;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;

public class Use {

	private Location location;
	/**
	 * Function containing use.
	 */
	private DataDeclaration fn;
	private Context context;

	public Use(Location location, DataDeclaration fn, Context context) {
		this.location = location;
		this.fn = fn;
		this.context = context;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public DataDeclaration getFn() {
		return fn;
	}

	public void setFn(DataDeclaration fn) {
		this.fn = fn;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

}
