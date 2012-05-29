package org.icgc.dcc.service;

import java.util.ArrayList;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;

public class ReleaseService {

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

  public Iterable<CompletedRelease> getCompletedReleases() {
    Iterable<CompletedRelease> completedReleases = new ArrayList<CompletedRelease>();
    return completedReleases;
  }

  public Iterable<HasRelease> list() {
    Iterable<HasRelease> list = new ArrayList<HasRelease>();
    return list;
  }
}
