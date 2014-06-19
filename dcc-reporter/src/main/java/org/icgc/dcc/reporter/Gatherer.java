package org.icgc.dcc.reporter;

import static com.google.common.base.Charsets.UTF_8;
import static org.icgc.dcc.core.util.Joiners.INDENT;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.core.util.Splitters.TAB;
import static org.icgc.dcc.reporter.Reporter.OUTPUT_FILE;

import java.io.File;
import java.util.List;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.commons.lang.StringUtils;
import org.icgc.dcc.core.model.DataType.DataTypes;
import org.icgc.dcc.reporter.presentation.DataTypeCountsReportTable;

import com.google.common.io.Files;

public class Gatherer {

  private static final String FUSE_MOUTPOINT_PREFIX = "/hdfs/dcc";
  private static final String PART_FILE = "part-00000";

  public static DataTypeCountsReportTable getTable(Set<String> projectKeys) {
    val table = new DataTypeCountsReportTable(projectKeys);
    populateTable(table, OutputType.DONOR);
    populateTable(table, OutputType.SPECIMEN);
    populateTable(table, OutputType.SAMPLE);
    populateTable(table, OutputType.OBSERVATION);

    // OutputType.SEQUENCING_STRATEGY.name(); // N/A for sequencing strategy
    val lines = readLines(OutputType.SEQUENCING_STRATEGY);

    System.out.println(StringUtils.repeat("=", 75));
    System.out.println(INDENT.join(OutputType.SEQUENCING_STRATEGY, INDENT.join(lines)));
    System.out.println();

    return table;
  }

  private static void populateTable(DataTypeCountsReportTable table, OutputType output) {
    val lines = readLines(output);

    System.out.println(StringUtils.repeat("=", 75));
    System.out.println(INDENT.join(output, INDENT.join(lines)));
    System.out.println();

    for (int i = 1; i < lines.size(); i++) { // Skip header
      val fields = TAB.split(lines.get(i));
      val iterator = fields.iterator();
      val projectId = iterator.next();
      val type = DataTypes.from(iterator.next()); // TODO: explain (can't use actual enum until here)
      val count = Long.valueOf(iterator.next());

      table.updateCount(output, projectId, type, count);
    }
  }

  @SneakyThrows
  private static List<String> readLines(OutputType output) {
    String outputFilePath = Reporter.getOutputFilePath(output);
    if (!Main.isLocal()) {
      outputFilePath = PATH.join(
          FUSE_MOUTPOINT_PREFIX,
          outputFilePath,
          PART_FILE);
    }
    return Files.readLines(
        new File(outputFilePath),
        UTF_8);
  }

  @SneakyThrows
  public static void writeCsvFile(DataTypeCountsReportTable table) {
    Files.write(
        table.getCsvRepresentation().getBytes(),
        new File(OUTPUT_FILE));
  }

}
