package org.icgc.dcc.release.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.icgc.dcc.core.model.BaseEntity;
import org.icgc.dcc.core.model.HasName;
import org.icgc.dcc.release.ReleaseException;

import com.google.code.morphia.annotations.Entity;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Entity
public class Release extends BaseEntity implements HasName {

  protected String name;

  protected ReleaseState state;

  protected List<Submission> submissions = Lists.newArrayList();

  protected List<QueuedProject> queue = Lists.newArrayList();

  protected Date releaseDate;

  protected String dictionaryVersion;

  public Release() {
    super();
    this.setState(ReleaseState.OPENED);
  }

  public Release(String name) {
    super();
    this.setName(name);
    this.setState(ReleaseState.OPENED);
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ReleaseState getState() {
    return state;
  }

  public void setState(ReleaseState state) {
    this.state = state;
  }

  public List<Submission> getSubmissions() {
    return submissions;
  }

  @JsonIgnore
  public Iterable<String> getProjectKeys() {
    return Iterables.transform(getSubmissions(), new Function<Submission, String>() {
      @Override
      public String apply(Submission input) {
        return input.getProjectKey();
      }
    });
  }

  public Submission getSubmission(final String projectKey) {
    checkArgument(projectKey != null);

    Optional<Submission> foundSubmission = Iterables.tryFind(this.submissions, new Predicate<Submission>() {
      @Override
      public boolean apply(Submission submission) {
        return submission.getProjectKey().equals(projectKey);
      }
    });

    if(foundSubmission.isPresent()) {
      return foundSubmission.get();
    } else {
      throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
          this.name));
    }
  }

  public void addSubmission(Submission submission) {
    this.getSubmissions().add(submission);
  }

  public String getDictionaryVersion() {
    return this.dictionaryVersion;
  }

  public void setDictionaryVersion(String dictionaryVersion) {
    this.dictionaryVersion = dictionaryVersion;
  }

  public Date getReleaseDate() {
    return releaseDate != null ? new Date(releaseDate.getTime()) : null;
  }

  public void setReleaseDate() {
    this.releaseDate = new Date();
  }

  public List<String> getQueuedProjectKeys() {
    List<String> projectKeys = Lists.newArrayList();
    for(QueuedProject qp : this.getQueue()) {
      projectKeys.add(qp.getProjectKey());
    }
    return projectKeys;
  }

  public List<QueuedProject> getQueue() {
    return queue;
  }

  public void enqueue(QueuedProject project) {
    if(project.getProjectKey() != null && !project.getProjectKey().isEmpty() && !this.queue.contains(project)) {
      this.queue.add(project);
    }
  }

  public void enqueue(List<String> projectKeys, Set<String> userNames) {
    for(String projectKey : projectKeys) {
      this.enqueue(new QueuedProject(projectKey, userNames));
    }
  }

  public boolean removeFromQueue(List<String> projectKeys) {
    return this.queue.removeAll(projectKeys);
  }

  public Optional<QueuedProject> nextInQueue() {
    return this.queue != null && this.queue.isEmpty() == false ? Optional.<QueuedProject> of(this.queue.get(0)) : Optional
        .<QueuedProject> absent();
  }

  public Optional<QueuedProject> dequeue() {
    return this.queue != null && this.queue.isEmpty() == false ? Optional.<QueuedProject> of(this.queue.remove(0)) : Optional
        .<QueuedProject> absent();
  }

  public void emptyQueue() {
    this.queue = Lists.newArrayList();
  }
}
