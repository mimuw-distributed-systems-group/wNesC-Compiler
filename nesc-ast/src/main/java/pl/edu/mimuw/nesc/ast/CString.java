package pl.edu.mimuw.nesc.ast;

public class CString {
	private final String data;

	public CString(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}

	public int getLength() {
		return data.length();
	}

}
