package pl.edu.mimuw.nesc.analysis.attributes;

import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.environment.Environment;

/**
 * Interface for a small analyzer. It it intended to analyze a single
 * attribute, e.g. '@C()'.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
interface AttributeSmallAnalyzer {
    /**
     * Performs all necessary actions for analysis of a single attribute,
     * e.g. generates semantic events or notifies the error helper about
     * detected errors.
     *
     * @param attributes All attributes applied to an entity.
     * @param declaration Entity that the attributes are applied to.
     * @param environment Environment of the declaration.
     */
    void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment);
}
