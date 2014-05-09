package org.icgc.dcc.reporter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.transformValues;
import static org.icgc.dcc.reporter.Reporter.getOutputFilePath;

import java.util.Map;

import lombok.val;

import org.icgc.dcc.hadoop.cascading.Taps;

import cascading.tap.Tap;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

public class Connector {

  /**
   * See {@link Taps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  public static Map<String, Tap> getInputTaps(InputData inputData) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getRawInputTaps(

        // get pipe to path map for the project/file type combination
        inputData.getPipeNameToFilePath()),

        Taps.RAW_CASTER);
  }

  /**
   * See {@link Taps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  public static Map<String, Tap> getOutputTaps(Iterable<String> tailNames) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getRawOutputTaps(tailNames),

        Taps.RAW_CASTER);
  }

  private static Map<String, Tap<?, ?, ?>> getRawInputTaps(Map<String, String> m) {

    System.out.println("@ " + m.toString().replace(",", "\n"));

    val d = transformValues(
        m,
        new Function<String, Tap<?, ?, ?>>() {

          @Override
          public Tap<?, ?, ?> apply(String filePath) {
            return Taps.getTsvFileWithHeader(filePath);
          }

        });

    System.out.println("+ " + d.toString().replace(",", "\n"));
    return d;
  }

  private static Map<String, Tap<?, ?, ?>> getRawOutputTaps(Iterable<String> tailNames) {
    val m = new ImmutableMap.Builder<String, Tap<?, ?, ?>>();
    val iterator = newArrayList(tailNames).iterator();
    for (val outputType : OutputType.values()) {
      m.put(
          iterator.next(), // TODO: explain...
          Taps.getTsvFileWithHeader(
              getOutputFilePath(outputType)));
    }
    System.out.println(m.build());
    return m.build();
  }

}
