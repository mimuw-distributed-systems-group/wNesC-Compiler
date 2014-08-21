package pl.edu.mimuw.nesc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

    private final ImmutableMap<String, FileData> fileDatas;
    private final ImmutableList<FileData> modifiedFileDatas;
    private final ImmutableList<NescIssue> issues;

    private ProjectData(Builder builder) {
        this.fileDatas = builder.fileDataBuilder.build();
        this.modifiedFileDatas = builder.modifiedFileDatasBuilder.build();
        this.issues = builder.issueListBuilder.build();
    }

    public ImmutableMap<String, FileData> getFileDatas() {
        return fileDatas;
    }

    public ImmutableList<FileData> getModifiedFileDatas() {
        return modifiedFileDatas;
    }

    public ImmutableList<NescIssue> getIssues() {
        return issues;
    }

    /**
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    public static class Builder {

        private ImmutableMap.Builder<String, FileData> fileDataBuilder;
        private ImmutableList.Builder<FileData> modifiedFileDatasBuilder;
        private ImmutableList.Builder<NescIssue> issueListBuilder;

        public Builder() {
            this.fileDataBuilder = ImmutableMap.builder();
            this.modifiedFileDatasBuilder = ImmutableList.builder();
            this.issueListBuilder = ImmutableList.builder();
        }

        public Builder addFileData(FileData fileData) {
            this.fileDataBuilder.put(fileData.getFilePath(), fileData);
            return this;
        }

        public Builder addModifiedFileData(FileData fileData) {
            this.modifiedFileDatasBuilder.add(fileData);
            return this;
        }

        public Builder addFileDatas(Collection<FileData> fileDatas) {
            for (FileData data : fileDatas) {
                this.fileDataBuilder.put(data.getFilePath(), data);
            }
            return this;
        }

        public Builder addModifiedFileDatas(Collection<FileData> fileData) {
            this.modifiedFileDatasBuilder.addAll(fileData);
            return this;
        }

        public Builder addIssue(NescIssue issue) {
            this.issueListBuilder.add(issue);
            return this;
        }

        public Builder addIssues(Collection<NescIssue> issues) {
            this.issueListBuilder.addAll(issues);
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
