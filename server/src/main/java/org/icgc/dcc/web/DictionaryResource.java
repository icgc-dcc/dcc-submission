package org.icgc.dcc.web;

import static com.google.common.base.Preconditions.checkArgument;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.icgc.dcc.model.dictionary.Dictionaries;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.QDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.mongodb.MongoException.DuplicateKey;
import com.mongodb.WriteConcern;

@Path("dictionary")
public class DictionaryResource {

  private static final Logger log = LoggerFactory.getLogger(DictionaryResource.class);

  @Inject
  private Dictionaries dictionaries;

  @POST
  public Response addDictionary(Dictionary d) {
    checkArgument(d != null);
    try {
      dictionaries.datastore().save(d);
      return Response.created(UriBuilder.fromResource(DictionaryResource.class).path(d.version).build()).build();
    } catch(DuplicateKey e) {
      log.info("Duplicate key", e);
      return Response.status(Status.BAD_REQUEST).build();
    }
  }

  @DELETE
  public Response deleteDictionary() {
    Dictionary d = dictionaries.query().orderBy(QDictionary.dictionary.version.desc()).singleResult();
    if(d == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    dictionaries.datastore().delete(d, WriteConcern.SAFE);
    return Response.ok(d).build();
  }

  @GET
  public Response getCurrent() {
    Dictionary d = dictionaries.query().orderBy(QDictionary.dictionary.version.desc()).singleResult();
    if(d == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(d).build();
  }

  @GET
  @Path("{version}")
  public Response getIt(@PathParam("version") String version) {
    Dictionary d = dictionaries.where(QDictionary.dictionary.version.eq(version)).uniqueResult();
    if(d == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(d).build();
  }
}
