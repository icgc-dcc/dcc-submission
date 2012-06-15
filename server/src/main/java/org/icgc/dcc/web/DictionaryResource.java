package org.icgc.dcc.web;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.icgc.dcc.model.ResponseTimestamper;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.DictionaryService;
import org.icgc.dcc.model.dictionary.DictionaryState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("dictionaries")
public class DictionaryResource {

  private static final Logger log = LoggerFactory.getLogger(DictionaryResource.class);

  @Inject
  private DictionaryService dictionaries;

  @POST
  public Response addDictionary(Dictionary d) {
    checkArgument(d != null);
    if(this.dictionaries.list().isEmpty() == false) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    this.dictionaries.add(d);

    return Response.created(UriBuilder.fromResource(DictionaryResource.class).path(d.getVersion()).build()).build();
  }

  @GET
  public Response getDictionaries() {
    List<Dictionary> dictionaries = this.dictionaries.list();
    if(dictionaries == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(dictionaries).build();
  }

  @GET
  @Path("{version}")
  public Response getDictionary(@PathParam("version") String version) {
    Dictionary d = this.dictionaries.getFromVersion(version);
    if(d == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return ResponseTimestamper.ok(d).build();
  }

  @PUT
  @Path("{version}")
  public Response updateDictionary(@PathParam("version") String version, Dictionary newDictionary, @Context Request req) {
    Dictionary oldDictionary = this.dictionaries.getFromVersion(version);
    if(oldDictionary == null) {
      return Response.status(Status.NOT_FOUND).build();
    } else if(oldDictionary.getState() != DictionaryState.OPENED || newDictionary.getVersion().equals(version) == false) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    ResponseTimestamper.evaluate(req, oldDictionary);
    this.dictionaries.update(newDictionary);

    return ResponseTimestamper.ok(newDictionary).build();
  }
}
