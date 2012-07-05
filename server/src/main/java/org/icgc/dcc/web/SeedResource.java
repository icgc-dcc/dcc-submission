package org.icgc.dcc.web;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.release.model.Release;

import com.google.code.morphia.Datastore;
import com.google.inject.Inject;

@Path("seed")
public class SeedResource {

  @Inject
  private Datastore datastore;

  @Inject
  private DictionaryService dictionaryService;

  @POST
  @Path("users")
  public Response seedUsers(User[] users) {
    this.datastore.save(users);
    return Response.status(Status.CREATED).build();
  }

  @POST
  @Path("projects")
  public Response seedProjects(Project[] projects) {
    this.datastore.save(projects);
    return Response.status(Status.CREATED).build();
  }

  @POST
  @Path("releases")
  public Response seedReleases(Release[] releases) {
    for(Release release : releases) {
      Dictionary jsonDictionary = release.getDictionary();
      Dictionary dbDictionary = dictionaryService.getFromVersion(jsonDictionary.getVersion());
      if(dbDictionary != null) {
        release.setDictionary(dbDictionary);
      } else {
        this.datastore.save(release.getDictionary());
      }
    }
    this.datastore.save(releases);
    return Response.status(Status.CREATED).build();
  }

  @POST
  @Path("dictionaries")
  public Response seedDictionaries(Dictionary[] dictionaries) {
    this.datastore.save(dictionaries);
    return Response.status(Status.CREATED).build();
  }
}
