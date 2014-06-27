package org.icgc.dcc.reporter;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.core.util.Splitters.TAB;

import java.io.File;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.core.util.Jackson;
import org.icgc.dcc.reporter.presentation.DataTypeCountsReportTable;

import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;

public class Gatherer {

  private static final String FUSE_MOUTPOINT_PREFIX = "/hdfs/dcc";
  private static final String PART_FILE = "part-00000";

  public static DataTypeCountsReportTable getTable(Set<String> projectKeys) {
    val outputFilePath = getOuputFilePath(OutputType.SEQUENCING_STRATEGY);
    val headerLine = readFirstLine(outputFilePath);
    val headers = newArrayList(TAB.split(headerLine));
    val headerSize = headers.size();

    System.out.println(readRemainingLines(outputFilePath));

    val documents = new BasicDBList();
    for (val line : readRemainingLines(outputFilePath)) {
      val values = newArrayList(TAB.split(line));
      checkState(headerSize == values.size());

      val builder = new BasicDBObjectBuilder();
      for (int i = 0; i < headerSize; i++) {
        builder.add(
            headers.get(i),
            values.get(i));
      }
      documents.add(builder.get());
    }
    System.out.println(Jackson.toJsonPrettyString(documents.toString()));

    return null;
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
