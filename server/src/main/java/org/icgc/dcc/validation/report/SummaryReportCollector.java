package org.icgc.dcc.validation.report;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.PlanExecutionException;
import org.icgc.dcc.validation.report.BaseReportingPlanElement.FieldSummary;

import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;

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

      String SummaryFile = CharStreams.toString(new InputStreamReader(src, "UTF-8"));
      if(!SummaryFile.isEmpty()) {
        SummaryFile = SummaryFile.substring(1, SummaryFile.length() - 1);
        Iterable<String> summaries = Splitter.on("}{").split(SummaryFile);
        Iterator<String> summaryIterator = summaries.iterator();

        while(summaryIterator.hasNext()) {
          String summary = summaryIterator.next();
          summary = "{" + summary + "}";
          FieldSummary fieldSummary = mapper.readValue(summary, FieldSummary.class);
          fieldReports.add(FieldReport.convert(fieldSummary));
        }
      }

      report.setFieldReports(fieldReports);
    } catch(Exception e) {
      throw new PlanExecutionException(e);
    }

    return Outcome.PASSED;
  }
}
