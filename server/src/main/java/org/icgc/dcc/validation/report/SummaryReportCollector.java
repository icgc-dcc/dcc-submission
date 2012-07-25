package org.icgc.dcc.validation.report;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.PlanExecutionException;
import org.icgc.dcc.validation.report.BaseReportingPlanElement.FieldSummary;

public class SummaryReportCollector implements ReportCollector {

  private final BaseReportingPlanElement planElement;

  public SummaryReportCollector(BaseReportingPlanElement planElement) {
    this.planElement = planElement;
  }

  @Override
  public Outcome collect(CascadingStrategy strategy, SchemaReport report) {
    try {
      InputStream src =
          strategy.readReportTap(planElement.getFileSchema(), planElement.getFlowType(), this.planElement.getName());
      ObjectMapper mapper = new ObjectMapper();
      List<FieldReport> fieldReports = new ArrayList<FieldReport>();

      MappingIterator<FieldSummary> fieldSummary = mapper.reader().withType(FieldSummary.class).readValues(src);
      while(fieldSummary.hasNext()) {
        fieldReports.add(FieldReport.convert(fieldSummary.next()));
      }

      report.setFieldReports(fieldReports);
    } catch(Exception e) {
      throw new PlanExecutionException(e);
    }

    return Outcome.PASSED;
  }
}
