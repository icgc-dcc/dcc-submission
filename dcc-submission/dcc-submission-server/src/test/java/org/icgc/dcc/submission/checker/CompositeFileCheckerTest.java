package org.icgc.dcc.submission.checker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class CompositeFileCheckerTest {

  @Mock
  private FileChecker baseChecker;

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
        ImmutableList.<FirstPassValidationError> of(new FirstPassValidationError(null, null, null, null)));
    assertTrue(!checker.check("anything").isEmpty());
    assertTrue(checker.check("anything").size() == 1);
  }

  @Test
  public void compositeValid() throws Exception {
    CompositeCheckerUnderTest checker1 = spy(new CompositeCheckerUnderTest(baseChecker));
    CompositeCheckerUnderTest checker2 = spy(new CompositeCheckerUnderTest(checker1));
    when(baseChecker.isValid()).thenReturn(true);
    assertTrue(checker2.isValid());

    verify(baseChecker, times(1)).isValid();
    verify(checker1, times(1)).isValid();
  }

  @Test
  public void checkFailFast() throws Exception {
    CompositeCheckerUnderTest checker = new CompositeCheckerUnderTest(baseChecker);
    when(baseChecker.isValid()).thenReturn(false);
    when(baseChecker.isFailFast()).thenReturn(true);

    CompositeCheckerUnderTest spy = spy(checker);
    checker.check(anyString());
    verify(spy, never()).performSelfCheck(anyString());
  }

  @Test
  public void compositeCheck() throws Exception {
    CompositeCheckerUnderTest checker1 = spy(new CompositeCheckerUnderTest(baseChecker, false));
    CompositeCheckerUnderTest checker2 = spy(new CompositeCheckerUnderTest(checker1, false));
    when(baseChecker.isValid()).thenReturn(false);
    when(baseChecker.isFailFast()).thenReturn(false);

    checker2.check(anyString());

    verify(checker1, times(1)).performSelfCheck(anyString());
    verify(checker2, times(1)).performSelfCheck(anyString());
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

    verify(spy1, never()).performSelfCheck(anyString());
    verify(spy2, never()).performSelfCheck(anyString());
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

    verify(checker1, times(1)).performSelfCheck(anyString());
    verify(checker2, never()).performSelfCheck(anyString());
  }

  private class CompositeCheckerUnderTest extends CompositeFileChecker {

    private final boolean failFast;

    public CompositeCheckerUnderTest(FileChecker nestedChecker, boolean failFast) {
      super(nestedChecker);
      this.failFast = failFast;
    }

    public CompositeCheckerUnderTest(FileChecker nestedChecker) {
      this(nestedChecker, false);
    }

    @Override
    public boolean isFailFast() {
      return failFast;
    }

    @Override
    public List<FirstPassValidationError> performSelfCheck(String filePathname) {
      return ImmutableList.of();
    }
  }
}
