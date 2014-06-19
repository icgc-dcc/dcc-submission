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
  public static Map<String, Tap> getRawInputTaps(InputData inputData) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getInputTaps(

        // get pipe to path map for the project/file type combination
        inputData.getPipeNameToFilePath()),

        Taps.RAW_CASTER);
  }

  /**
   * See {@link Taps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  public static Map<String, Tap> getRawOutputTaps(Iterable<String> tailNames) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getOutputTaps(tailNames),

        Taps.RAW_CASTER);
  }

  private static Map<String, Tap<?, ?, ?>> getInputTaps(Map<String, String> pipeNameToFilePath) {
    return transformValues(
        pipeNameToFilePath,
        new Function<String, Tap<?, ?, ?>>() {

          @Override
          public Tap<?, ?, ?> apply(final String path) {
            return Main.isLocal() ?
                Taps.getDecompressingLocalTsvWithHeader(path) :
                Taps.getCompressingHadoopTsvWithHeader(path);
          }

        });
  }

  private static Map<String, Tap<?, ?, ?>> getOutputTaps(Iterable<String> tailNames) {
    val rawOutputTaps = new ImmutableMap.Builder<String, Tap<?, ?, ?>>();
    val iterator = newArrayList(tailNames).iterator();
    for (val outputType : OutputType.values()) {
      val outputFilePath = getOutputFilePath(outputType);
      rawOutputTaps.put(
          iterator.next(), // TODO: explain...
          Main.isLocal() ?
              Taps.getNoCompressionLocalTsvWithHeader(outputFilePath) :
              Taps.getNoCompressionHadoopTsvFileWithHeader(outputFilePath));
    }

    return rawOutputTaps.build();
  }

}
