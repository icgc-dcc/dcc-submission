package org.icgc.dcc.model;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.model.dictionary.Dictionary;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Reference;

@Entity
public class Release extends BaseEntity implements HasName {

  protected String name;

  protected ReleaseState state;

  protected List<Submission> submissions = new ArrayList<Submission>();

  protected List<String> projectKeys = new ArrayList<String>();

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

  public ReleaseState getState() {
    return state;
  }

  public void setState(ReleaseState state) {
    this.state = state;
  }

  public List<Submission> getSubmissions() {
    return submissions;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getProjectKeys() {
    return projectKeys;
  }

  public void enqueue(String projectKey) {
    this.projectKeys.add(projectKey);
  }

  public void enqueue(List<String> newProjectKeys) {
    this.projectKeys.addAll(newProjectKeys);
  }

  public void emptyQueue() {
    this.projectKeys = new ArrayList<String>();
  }

  public Dictionary getDictionary() {
    return this.dictionary;
  }

  public void setDictionary(Dictionary dictionary) {
    this.dictionary = dictionary;
  }
}
