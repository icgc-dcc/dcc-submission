package org.icgc.dcc.release.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.icgc.dcc.core.model.BaseEntity;
import org.icgc.dcc.core.model.HasName;
import org.icgc.dcc.release.ReleaseException;

import com.google.code.morphia.annotations.Entity;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

@Entity
public class Release extends BaseEntity implements HasName {

  protected String name;

  protected ReleaseState state;

  protected List<Submission> submissions = new ArrayList<Submission>();

  protected List<String> queue = new ArrayList<String>();

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

  public List<String> getQueue() {
    return queue;
  }

  public void enqueue(String projectKey) {
    List<String> queue = this.getQueue();
    if(projectKey != null && !projectKey.isEmpty() && !queue.contains(projectKey)) {
      queue.add(projectKey);
    }
  }

  public void enqueue(List<String> projectKeys) {
    for(String projectKey : projectKeys) {
      this.enqueue(projectKey);
    }
  }

  public boolean removeFromQueue(List<String> projectKeys) {
    return this.queue.removeAll(projectKeys);
  }

  public Optional<String> nextInQueue() {
    return this.queue != null && this.queue.isEmpty() == false ? Optional.<String> of(this.queue.get(0)) : Optional
        .<String> absent();
  }

  public Optional<String> dequeue() {
    return this.queue != null && this.queue.isEmpty() == false ? Optional.<String> of(this.queue.remove(0)) : Optional
        .<String> absent();
  }

  public void emptyQueue() {
    this.queue = new LinkedList<String>();
  }
}
