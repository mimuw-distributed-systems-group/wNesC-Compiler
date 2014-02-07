package pl.edu.mimuw.nesc;

import com.google.common.io.Files;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>
 * Holds information about search paths of current project.
 * </p>
 * <p>
 * Provides methods for retrieving full file path containing the nesc entities
 * definitions.
 * </p>
 * <p>
 * <h2>Search order</h2>
 * TODO
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class PathsResolver {

    private final String projectPath;
    private final List<String> sourcePaths;
    private final List<String> quoteIncludePaths;
    private final List<String> searchOrder;

    private PathsResolver(Builder builder) {
        this.projectPath = builder.projectPath;
        this.sourcePaths = builder.sourcePaths;
        this.quoteIncludePaths = builder.quoteIncludePaths;
        this.searchOrder = new ArrayList<>(sourcePaths.size() + 1);
        this.searchOrder.add(projectPath);
        this.searchOrder.addAll(sourcePaths);
    }

    /**
     * Returns a new builder instance.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns project main directory path.
     *
     * @return main directory path
     */
    public String getProjectPath() {
        return projectPath;
    }

    /**
     * Returns a list of directories paths where header files and nesc entities
     * definitions should be searched.
     *
     * @return list of directories paths
     */
    public List<String> getSourcePaths() {
        return sourcePaths;
    }

    /**
     * Return a list of directories paths where header files requested with
     * <code>#include "file"</code> should be searched.
     *
     * @return list of directories path
     */
    public List<String> getQuoteIncludePaths() {
        return quoteIncludePaths;
    }

    /**
     * Returns ordered search paths.
     *
     * @return ordered search paths
     */
    public List<String> getSearchOrder() {
        return searchOrder;
    }

    /**
     * Returns file containing the definition of configuration, module or
     * interface of specified name.
     *
     * @param name configuration's, module's or interface's name
     * @return path to file containing entity definition or <code>null</code>
     * when no file matches given entity name
     */
    public String getEntityFile(String name) {
        checkNotNull(name, "entity name cannot be null");

        for (String searchPath : this.searchOrder) {
            final File directory = new File(searchPath);

            File[] files = directory.listFiles();

            // FIXME : handle this warning
            if (files == null) {
                //System.err.println("Directory files array is null; " + searchPath);
                continue;
            }

            for (File child : files) {
                if (!child.isFile()) {
                    continue;
                }
                final String childName = Files.getNameWithoutExtension(child.getName());
                final String extension = Files.getFileExtension(child.getName());
                if (name.equals(childName) && "nc".equals(extension)) {
                    return child.getPath();
                }
            }
        }
        return null;
    }

    /**
     * Builder.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    public static class Builder {

        private String projectPath;
        private List<String> sourcePaths;
        private List<String> quoteIncludePaths;

        public Builder() {
        }

        public Builder projectPath(String projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public Builder sourcePaths(List<String> sourcePaths) {
            this.sourcePaths = sourcePaths;
            return this;
        }

        public Builder quoteIncludePaths(List<String> quoteIncludePaths) {
            this.quoteIncludePaths = quoteIncludePaths;
            return this;
        }

        public PathsResolver build() {
            checkNotNull(projectPath);
            checkNotNull(sourcePaths);
            checkNotNull(quoteIncludePaths);
            return new PathsResolver(this);
        }

    }

}
