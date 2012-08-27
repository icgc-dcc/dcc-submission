package org.icgc.dcc.data.web;

import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.bson.types.ObjectId;
import org.icgc.dcc.data.DataService;
import org.icgc.dcc.data.model.AffectsGene;
import org.icgc.dcc.data.model.Gene;
import org.icgc.dcc.data.model.Observation;
import org.icgc.dcc.data.model.QObservation;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;
import com.sun.jersey.api.NotFoundException;

@Path("/observations")
public class ObservationsResource {

  @Inject
  private DataService s;

  @GET
  public Iterator<Observation> observations() {
    return query(QObservation.observation).iterate();
  }

  @GET
  @Path("/{id}")
  public Observation observation(@PathParam("id") String id) {
    Observation o = s.datastore().get(Observation.class, new ObjectId(id));
    if(o == null) {
      throw new NotFoundException();
    }
    return o;
  }

  @GET
  @Path("/{id}/genes")
  public Iterable<Gene> observationGenes(@PathParam("id") String id) {
    return Iterables.transform(observation(id).affectedGenes, new Function<AffectsGene, Gene>() {

      @Override
      public Gene apply(AffectsGene input) {
        return s.datastore().get(Gene.class, input.geneId);
      }
    });
  }

  private <T> MorphiaQuery<T> query(EntityPath<T> path) {
    return new MorphiaQuery<T>(s.morphia(), s.datastore(), path);
  }
}