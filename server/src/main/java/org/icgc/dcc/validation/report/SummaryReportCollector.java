package org.icgc.dcc.validation.report;

import java.io.InputStream;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.PlanExecutionException;

public class SummaryReportCollector implements ReportCollector {

  private final FileSchema fileSchema;

  private final FlowType flowType;

  public SummaryReportCollector(FileSchema fileSchema, FlowType flowType) {
    this.fileSchema = fileSchema;
    this.flowType = flowType;
  }

  @Override
  public Outcome collect(CascadingStrategy strategy, SchemaReport report) {
    try {
      InputStream src = strategy.readReportTap(fileSchema, flowType, report.getName());
      ObjectMapper mapper = new ObjectMapper();
      List<FieldReport> fieldReports =
          mapper.readValue(src, mapper.getTypeFactory().constructCollectionType(List.class, FieldReport.class));
      report.getFieldReports().addAll(fieldReports);
    } catch(Exception e) {
      throw new PlanExecutionException(e);
    }

    return Outcome.PASSED;
  }
}
