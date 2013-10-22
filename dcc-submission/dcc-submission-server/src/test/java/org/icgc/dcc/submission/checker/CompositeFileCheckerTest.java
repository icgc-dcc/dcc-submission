package org.icgc.dcc.submission.checker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class CompositeFileCheckerTest {

  private FileChecker baseChecker;

  @Before
  public void setup() {
    baseChecker = mock(FileChecker.class);
  }

  @Test
  public void valid() throws Exception {
    CompositeCheckerUnderTest checker = new CompositeCheckerUnderTest(baseChecker);
    when(baseChecker.isValid()).thenReturn(true);
    assertTrue(checker.isValid());
  }

  @Test
  public void notvalid() throws Exception {
    CompositeCheckerUnderTest checker = new CompositeCheckerUnderTest(baseChecker);
    when(baseChecker.isValid()).thenReturn(false);
    assertFalse(checker.isValid());
  }

  @Test
  public void testCheckEmpty() throws Exception {
    CompositeCheckerUnderTest checker = new CompositeCheckerUnderTest(baseChecker);
    when(baseChecker.check(anyString())).thenReturn(ImmutableList.<FirstPassValidationError> of());
    assertTrue(checker.check("anything").isEmpty());
  }

  @Test
  public void testCheckNotEmpty() throws Exception {
    CompositeCheckerUnderTest checker = new CompositeCheckerUnderTest(baseChecker);
    when(baseChecker.check(anyString())).thenReturn(
        ImmutableList.<FirstPassValidationError> of(new FirstPassValidationError(null, null, null)));
    assertTrue(!checker.check("anything").isEmpty());
    assertTrue(checker.check("anything").size() == 1);
  }

  @Test
  public void compositeValid() throws Exception {
    CompositeCheckerUnderTest checker1 = spy(new CompositeCheckerUnderTest(baseChecker));
    CompositeCheckerUnderTest checker2 = spy(new CompositeCheckerUnderTest(checker1));
    when(baseChecker.isValid()).thenReturn(true);
    assertTrue(checker2.isValid());

    verify(baseChecker, atLeastOnce()).isValid();
    verify(checker1, atLeastOnce()).isValid();
  }

  @Test
  public void checkFailFast() throws Exception {
    CompositeCheckerUnderTest checker = new CompositeCheckerUnderTest(baseChecker);
    when(baseChecker.isValid()).thenReturn(false);
    when(baseChecker.isFailFast()).thenReturn(true);

    CompositeCheckerUnderTest spy = spy(checker);
    checker.check(anyString());
    verify(spy, never()).selfCheck(anyString());
  }

  @Test
  public void compositeCheck() throws Exception {
    CompositeCheckerUnderTest checker1 = spy(new CompositeCheckerUnderTest(baseChecker, false));
    CompositeCheckerUnderTest checker2 = spy(new CompositeCheckerUnderTest(checker1, false));
    when(baseChecker.isValid()).thenReturn(false);
    when(baseChecker.isFailFast()).thenReturn(false);

    checker2.check(anyString());

    verify(checker1, atLeastOnce()).selfCheck(anyString());
    verify(checker2, atLeastOnce()).selfCheck(anyString());
  }

  @Test
  public void compositeCheckFast() throws Exception {
    CompositeCheckerUnderTest checker1 = new CompositeCheckerUnderTest(baseChecker);
    CompositeCheckerUnderTest checker2 = new CompositeCheckerUnderTest(checker1);
    when(baseChecker.isValid()).thenReturn(false);
    when(baseChecker.isFailFast()).thenReturn(true);

    CompositeCheckerUnderTest spy1 = spy(checker1);
    CompositeCheckerUnderTest spy2 = spy(checker2);

    assertFalse(checker2.isValid());

    verify(spy1, never()).selfCheck(anyString());
    verify(spy2, never()).selfCheck(anyString());
  }

  @Test
  public void compositeCheckFaster() throws Exception {
    CompositeCheckerUnderTest checker1 = spy(new CompositeCheckerUnderTest(baseChecker));
    when(baseChecker.isValid()).thenReturn(false);
    when(baseChecker.isFailFast()).thenReturn(false);

    when(checker1.isFailFast()).thenReturn(true);
    CompositeCheckerUnderTest checker2 = spy(new CompositeCheckerUnderTest(checker1));
    when(checker2.isFailFast()).thenReturn(false);

    checker2.check(anyString());

    verify(checker1, atLeastOnce()).selfCheck(anyString());
    verify(checker2, never()).selfCheck(anyString());
  }

  private class CompositeCheckerUnderTest extends CompositeFileChecker {

    private final boolean isFailFast;

    public CompositeCheckerUnderTest(FileChecker nestedChecker, boolean isFailFast) {
      super(nestedChecker);
      this.isFailFast = isFailFast;
    }

    public CompositeCheckerUnderTest(FileChecker nestedChecker) {
      this(nestedChecker, false);
    }

    @Override
    public boolean isFailFast() {
      return isFailFast;
    }

    @Override
    public List<FirstPassValidationError> selfCheck(String filePathname) {
      return ImmutableList.of();
    }
  }
}
