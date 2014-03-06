package pl.edu.mimuw.nesc.ast;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.common.SourceLanguage;

/**
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class NescDeclaration {

	private final SourceLanguage language;
	private final String name;
	private boolean isAbstract;
	private Environment environment;
	private LinkedList<Declaration> parameters;
	private Environment parameterEnv;

	/* For components */
	private boolean isConfiguration;

	public NescDeclaration(SourceLanguage language, String name,
			Environment parent) {
		this.language = language;
		this.name = name;
		this.environment = new Environment(environment, true, false);
	}

	public NescDeclaration(SourceLanguage language, String name) {
		this.language = language;
		this.name = name;
	}

	public SourceLanguage getLanguage() {
		return language;
	}

	public String getName() {
		return name;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public Environment getEnvironment() {
		return environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public LinkedList<Declaration> getParameters() {
		return parameters;
	}

	public void setParameters(LinkedList<Declaration> parameters) {
		this.parameters = parameters;
	}

	public Environment getParameterEnv() {
		return parameterEnv;
	}

	public void setParameterEnv(Environment parameterEnv) {
		this.parameterEnv = parameterEnv;
	}

	/* For components */
	public boolean isConfiguration() {
		return isConfiguration;
	}

	public void setConfiguration(boolean isConfiguration) {
		this.isConfiguration = isConfiguration;
	}

}
