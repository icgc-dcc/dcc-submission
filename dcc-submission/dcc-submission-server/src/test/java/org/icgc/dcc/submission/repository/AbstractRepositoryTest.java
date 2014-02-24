package org.icgc.dcc.submission.repository;

import org.icgc.dcc.test.mongodb.EmbeddedMongo;
import org.junit.Rule;

public abstract class AbstractRepositoryTest {

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

}
