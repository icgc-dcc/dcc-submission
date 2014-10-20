package org.icgc.dcc.submission.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

public class FieldNameComparatorTest {

  @Test
  public void testCompare() {
    val fieldNames = ImmutableList.of("c", "b", "a");
    val comparator = new FieldNameComparator(fieldNames);

    val sortedMap = ImmutableSortedMap.<String, Object> orderedBy(comparator)
        .put("a", 1)
        .put("c", 3)
        .put("b", 2)
        .build();

    assertThat(sortedMap.keySet()).containsExactly("c", "b", "a");
  }

}
