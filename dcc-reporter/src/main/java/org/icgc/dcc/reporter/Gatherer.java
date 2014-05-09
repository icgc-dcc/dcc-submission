package org.icgc.dcc.reporter;

import static com.google.common.base.Charsets.UTF_8;
import static org.icgc.dcc.core.util.Splitters.TAB;
import static org.icgc.dcc.reporter.Reporter.OUTPUT_FILE;

import java.io.File;
import java.util.List;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.core.model.DataType.DataTypes;
import org.icgc.dcc.reporter.presentation.DataTypeCountsReportTable;

import com.google.common.io.Files;

public class Gatherer {

  public static DataTypeCountsReportTable getTable(Set<String> projectKeys) {
    val table = new DataTypeCountsReportTable(projectKeys);
    populateTable(table, OutputType.DONOR);
    populateTable(table, OutputType.SPECIMEN);
    populateTable(table, OutputType.SAMPLE);
    populateTable(table, OutputType.OBSERVATION);
    OutputType.SEQUENCING_STRATEGY.name(); // N/A for sequencing strategy
    return table;
  }

  private static void populateTable(DataTypeCountsReportTable table, OutputType output) {
    val lines = readLines(output);
    for (int i = 1; i < lines.size(); i++) { // Skip header
      val fields = TAB.split(lines.get(i));
      val iterator = fields.iterator();
      val projectId = iterator.next();
      val type = DataTypes.valueOf(iterator.next());
      val count = Long.valueOf(iterator.next());

      table.updateCount(output, projectId, type, count);
    }
  }

  @SneakyThrows
  private static List<String> readLines(OutputType output) {
    return Files.readLines(
        new File(Reporter.getOutputFilePath(output)),
        UTF_8);
  }

  @SneakyThrows
  public static void writeCsvFile(DataTypeCountsReportTable table) {
    Files.write(
        table.getCsvRepresentation().getBytes(),
        new File(OUTPUT_FILE));
  }

}
