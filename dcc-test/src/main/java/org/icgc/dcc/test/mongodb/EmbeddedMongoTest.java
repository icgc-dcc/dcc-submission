package org.icgc.dcc.test.mongodb;

import lombok.val;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class EmbeddedMongoTest {

  @Mock
  Statement statement;
  @Mock
  Description description;

  @Test
  public void testApply() throws Throwable {
    for (int i = 0; i < 100; i++) {
      val embeddedMongo = new EmbeddedMongo();
      embeddedMongo.apply(statement, description).evaluate();
    }
  }

}
