package pl.edu.mimuw.nesc;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.problem.NescIssue;

import java.util.Collection;

/**
 * Contains the result of parsing process for entire project.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ProjectData {

    public static Builder builder() {
        return new Builder();
    }

    private final ImmutableList<NescIssue> issues;

    private ProjectData(Builder builder) {
        this.issues = builder.issues;
    }

    public ImmutableList<NescIssue> getIssues() {
        return issues;
    }

    /**
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    public static class Builder {

        private ImmutableList<NescIssue> issues;

        public Builder() {
        }

        public Builder addIssues(Collection<NescIssue> issues) {
            this.issues = ImmutableList.copyOf(issues);
            return this;
        }

        public ProjectData build() {
            verify();
            return new ProjectData(this);
        }

        private void verify() {
        }
    }
}
