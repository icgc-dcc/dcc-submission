package org.icgc.dcc.release.model;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.filesystem.SubmissionFile;

public class DetailedSubmission extends Submission {
  private String projectName;

  private List<SubmissionFile> submissionFiles;

  public DetailedSubmission() {
    super();
  }

  public DetailedSubmission(Submission submission) {
    super();
    this.projectKey = submission.projectKey;
    this.state = submission.state;
    this.report = submission.report;
    this.lastUpdated = submission.lastUpdated;
    this.submissionFiles = new ArrayList<SubmissionFile>();
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public List<SubmissionFile> getSubmissionFiles() {
    return submissionFiles;
  }

  public void setSubmissionFiles(List<SubmissionFile> submissionFiles) {
    this.submissionFiles = submissionFiles;
  }
}
