package org.icgc.dcc.validation.visitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.visitor.UniqueFieldsPlanningVisitor.UniqueFieldsPlanElement.CountBuffer;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

public class UniqueFieldsPlanningVisitorTest {

  @Test
  public void test_CountBuffer_identifiesNonUnique() {
    List<String> fields = new ArrayList<String>();
    fields.add("id");
    CountBuffer buffer = new CountBuffer(fields);
    Fields incoming = new Fields("id", "_state");

    // Note that CountBuffer expects grouped input, so the actual values will already be the same in each group
    TupleEntry[] tuples =
        new TupleEntry[] { new TupleEntry(incoming, new Tuple("1", new TupleState())), new TupleEntry(incoming,
            new Tuple("1", new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, incoming);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();
    TupleState state = (TupleState) t.getObject(1);
    assertTrue(state.isValid());

    t = iterator.next();
    state = (TupleState) t.getObject(1);
    assertFalse(state.isValid());
  }

  @Test
  public void test_CountBuffer_passesUnique() {
    List<String> fields = new ArrayList<String>();
    fields.add("id");
    CountBuffer buffer = new CountBuffer(fields);
    Fields incoming = new Fields("id", "_state");

    TupleEntry[] tuples = new TupleEntry[] { new TupleEntry(incoming, new Tuple("1", new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, incoming);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();
    TupleState state = (TupleState) t.getObject(1);
    assertTrue(state.isValid());
  }
}
