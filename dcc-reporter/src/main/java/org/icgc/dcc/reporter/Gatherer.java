package org.icgc.dcc.reporter;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static org.icgc.dcc.core.util.Jackson.toJsonPrettyString;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.core.util.Splitters.TAB;

import java.io.File;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.reporter.presentation.DataTypeCountsReportTable;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;

@Slf4j
public class Gatherer {

  private static final String FUSE_MOUTPOINT_PREFIX = "/hdfs/dcc";
  private static final String PART_FILE = "part-00000";

  public static DataTypeCountsReportTable getTable(
      @NonNull final Set<String> projectKeys,
      @NonNull final Map<String, String> mapping) {

    val outputFilePath = getOuputFilePath(OutputType.SEQUENCING_STRATEGY);
    val headerLine = readFirstLine(outputFilePath);
    val headers = newArrayList(TAB.split(headerLine));
    val headerSize = headers.size();

    val documents = new BasicDBList();
    for (val line : readRemainingLines(outputFilePath)) {
      val values = newArrayList(TAB.split(line));
      checkState(headerSize == values.size());

      val builder = new BasicDBObjectBuilder();
      for (int i = 0; i < headerSize; i++) {
        builder.add(
            tryTranslate(
                mapping,
                headers.get(i)),
            values.get(i));
      }
      documents.add(builder.get());
    }
    log.info("Content: '{}'", toJsonPrettyString(documents.toString()));
    return null;
  }

  private static String tryTranslate(
      @NonNull final Map<String, String> mapping,
      @NonNull final String code) {

    return mapping.containsKey(code) ?
        mapping.get(code) : code;
  }

  @SneakyThrows
  private static String readFirstLine(String filePath) {
    return Files.readFirstLine(
        new File(filePath),
        UTF_8);
  }

  @SneakyThrows
  private static Iterable<String> readRemainingLines(String filePath) {
    return filter(readLines(
        new File(filePath),
        UTF_8),
        new Predicate<String>() {

          int lineNumber;

          @Override
          public boolean apply(String line) {
            return lineNumber++ != 0;
          }

        });
  }

  private static String getOuputFilePath(OutputType output) {
    String outputFilePath = Reporter.getOutputFilePath(output);
    if (!Main.isLocal()) {
      outputFilePath = PATH.join(
          FUSE_MOUTPOINT_PREFIX,
          outputFilePath,
          PART_FILE);
    }
    return outputFilePath;
  }

  @SneakyThrows
  public static void writeCsvFile(DataTypeCountsReportTable table) {
    Files.write(
        table.getCsvRepresentation().getBytes(),
        new File("TODO"));
  }

}
