package org.icgc.dcc.submission.server.repository;

import static java.lang.String.format;

import org.icgc.dcc.common.test.mongodb.EmbeddedMongo;
import org.junit.Rule;

public abstract class AbstractRepositoryTest {

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

  protected String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-submission-server.ReleaseRepository", embeddedMongo.getPort());
  }

}
