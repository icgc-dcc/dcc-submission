package org.icgc.dcc.validation.restriction;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.icgc.dcc.model.dictionary.CodeList;
import org.icgc.dcc.model.dictionary.DictionaryService;
import org.icgc.dcc.model.dictionary.Term;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.restriction.CodeListRestriction.InCodeListFunction;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

public class CodeListRestrictionTest extends CascadingTestCase {

  private DictionaryService mockDictionaries;

  private static final String FIELDNAME = "code";

  private static final String CODELISTNAME = "TestList";

  private static final String CODE1 = "0";

  private static final String VALUE1 = "X";

  public void setup_CodeListRestriction() {
    this.mockDictionaries = mock(DictionaryService.class);
    CodeList codeList = new CodeList(CODELISTNAME);
    Term term1 = new Term(CODE1, VALUE1, "");
    codeList.addTerm(term1);

    when(this.mockDictionaries.getCodeList(anyString())).thenReturn(codeList);
  }

  @Test
  public void test_CodeListRestriction() {
    setup_CodeListRestriction();
    CodeListRestriction restriction = new CodeListRestriction(FIELDNAME, CODELISTNAME, this.mockDictionaries);

    assertEquals(String.format("codeList[%s:%s]", FIELDNAME, CODELISTNAME), restriction.describe());
  }

  public void setup_InCodeListFunction() {

  }

  @Test
  public void test_InCodeListFunction_codeInCodeList() {

    TupleState state = testRig_InCodeListFunction(CODE1);
    assertTrue(state.isValid());
  }

  @Test
  public void test_InCodeListFunction_codeNotInCodeList() {
    TupleState state = testRig_InCodeListFunction("1");

    assertFalse(state.isValid());
  }

  @Test
  public void test_InCodeListFunction_numericCodeInCodeList() {

    TupleState state = testRig_InCodeListFunction(new Integer(0));
    assertTrue(state.isValid());
  }

  @Test
  public void test_InCodeListFunction_numericCodeNotInCodeList() {
    TupleState state = testRig_InCodeListFunction(new Integer(1));

    assertFalse(state.isValid());
  }

  @Test
  public void test_InCodeListFunction_valueInCodeList() {
    TupleState state = testRig_InCodeListFunction(VALUE1);

    assertTrue(state.isValid());
  }

  @Test
  public void test_InCodeListFunction_valueNotInCodeList() {
    TupleState state = testRig_InCodeListFunction("Y");

    assertFalse(state.isValid());
  }

  @Test
  public void test_InCodeListFunction_emptyValue() {
    TupleState state = testRig_InCodeListFunction("");

    assertFalse(state.isValid());
  }

  @Test
  public void test_InCodeListFunction_nullValue() {
    TupleState state = testRig_InCodeListFunction(null);

    assertFalse(state.isValid());
  }

  private TupleState testRig_InCodeListFunction(Object tupleValue) {
    Set<String> codes = new HashSet<String>();
    codes.add(CODE1);
    Set<String> values = new HashSet<String>();
    values.add(VALUE1);

    InCodeListFunction function = new InCodeListFunction(CODELISTNAME, codes, values);

    Fields incoming = new Fields(FIELDNAME, "_state");

    TupleEntry[] tuples = new TupleEntry[] { new TupleEntry(incoming, new Tuple(tupleValue, new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeFunction(function, tuples, incoming);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();

    assertEquals(tupleValue, t.getObject(0));
    TupleState state = (TupleState) t.getObject(1);
    return state;
  }
}
