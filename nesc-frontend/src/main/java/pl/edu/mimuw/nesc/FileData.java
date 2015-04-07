package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.load.FileCache;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

import java.util.List;

/**
 * Contains the results of parsing of single nesC source file.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class FileData {

    private final String filePath;
    private final FileType fileType;
    private final Optional<Node> entityRoot;
    private final List<Declaration> extdefs;
    private final List<Comment> comments;
    private final Map<String, String> globalNames;
    private final Map<String, String> combiningFunctions;
    private final List<PreprocessorDirective> preprocessorDirectives;
    private final Multimap<Integer, Token> tokens;
    private final Multimap<Integer, NescIssue> issues;
    private final Environment environment;

    // TODO: extends attributes list (e.g. macros...).

    public FileData(FileCache cache) {
        this.filePath = cache.getFilePath();
        this.fileType = cache.getFileType();
        this.entityRoot = cache.getEntityRoot();
        this.extdefs = cache.getExtdefs();
        this.comments = cache.getComments();
        this.globalNames = cache.getGlobalNames();
        this.combiningFunctions = cache.getCombiningFunctions();
        this.preprocessorDirectives = cache.getPreprocessorDirectives();
        this.tokens = cache.getTokens();
        this.issues = cache.getIssues();
        this.environment = cache.getEnvironment();
    }

    public String getFilePath() {
        return filePath;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Optional<Node> getEntityRoot() {
        return entityRoot;
    }

    public List<Declaration> getExtdefs() {
        return extdefs;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public Map<String, String> getGlobalNames() {
        return globalNames;
    }

    public Map<String, String> getCombiningFunctions() {
        return combiningFunctions;
    }

    public List<PreprocessorDirective> getPreprocessorDirectives() {
        return preprocessorDirectives;
    }

    public Multimap<Integer, Token> getTokens() {
        return tokens;
    }

    public Multimap<Integer, NescIssue> getIssues() {
        return issues;
    }

    public Environment getEnvironment() {
        return environment;
    }
}
