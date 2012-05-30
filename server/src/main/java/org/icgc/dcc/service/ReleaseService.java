package org.icgc.dcc.service;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;

import com.google.code.morphia.Datastore;
import com.google.inject.Inject;
import com.mongodb.Mongo;

public class ReleaseService {

  @Inject
  private Mongo mongo;

  @Inject
  private Datastore datastore;

  public ReleaseState nextReleaseState() throws IllegalReleaseStateException {
    return getNextRelease().getRelease().getState();
  }

  public NextRelease getNextRelease() throws IllegalReleaseStateException {
    // release should be getting from mongodb
    Release nextRelease = new Release();
    return new NextRelease(nextRelease);
  }

  public OpenedRelease getOpenedRelease() throws IllegalReleaseStateException {
    // release should be getting from mongodb
    Release openedRelease = new Release();
    return new OpenedRelease(openedRelease);
  }

  public ClosedRelease getClosedRelease() throws IllegalReleaseStateException {
    // release should be getting from mongodb
    Release closedRelease = new Release();
    return new ClosedRelease(closedRelease);
  }

  public Iterable<CompletedRelease> getCompletedReleases() throws IllegalReleaseStateException {
    List<CompletedRelease> completedReleases = new ArrayList<CompletedRelease>();
    List<Release> releases = this.datastore.find(Release.class, "state", ReleaseState.COMPLETED).asList();
    for(Release release : releases) {
      completedReleases.add(new CompletedRelease(release));
    }
    return completedReleases;
  }

  public Iterable<HasRelease> list() {
    List<HasRelease> list = new ArrayList<HasRelease>();
    List<Release> releases = this.datastore.find(Release.class).asList();
    for(Release release : releases) {
      list.add(new HasRelease(release));
    }
    return list;
  }
}
