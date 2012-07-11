package org.icgc.dcc.release.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.icgc.dcc.core.model.BaseEntity;
import org.icgc.dcc.core.model.HasName;
import org.icgc.dcc.dictionary.model.Dictionary;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Reference;

@Entity
public class Release extends BaseEntity implements HasName {

  protected String name;

  protected ReleaseState state;

  protected List<Submission> submissions = new ArrayList<Submission>();

  protected List<String> queue = new ArrayList<String>();

  protected Date releaseDate;

  @Reference
  protected Dictionary dictionary;

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

  public void addSubmission(Submission submission) {
    this.getSubmissions().add(submission);
  }

  public Dictionary getDictionary() {
    return this.dictionary;
  }

  public void setDictionary(Dictionary dictionary) {
    this.dictionary = dictionary;
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

  public String dequeue() {
    return this.getQueue().remove(0);
  }

  public void emptyQueue() {
    this.queue = new LinkedList<String>();
  }
}
