package org.icgc.dcc.submission.server.repository;

import org.icgc.dcc.common.test.mongodb.EmbeddedMongo;
import org.junit.Rule;

public abstract class AbstractRepositoryTest {

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

}
