/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.validation.primary.visitor;

import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.validation.cascading.TupleStates.keepInvalidTuplesFilter;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.STATE_FIELD;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.PlanExecutionException;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.ReportingPlanElement;
import org.icgc.dcc.submission.validation.primary.report.ReportCollector;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

public class ErrorReportingPlanningVisitor extends ReportingPlanningVisitor {

  public ErrorReportingPlanningVisitor(@NonNull String projectKey, SubmissionPlatformStrategy platform,
      @NonNull FlowType type) {
    super(projectKey, platform, type);
  }

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);

    collectPlanElement(new ErrorsPlanElement(
        fileSchema.getName(),
        getCurrentFileName(),
        getFlowType()));
  }

  static class ErrorsPlanElement implements ReportingPlanElement {

    /**
     * Configuration.
     */
    @Getter
    private final String fileSchemaName;
    private final String fileName;
    private final FlowType flowType;

    public ErrorsPlanElement(@NonNull String fileSchemaName, @NonNull String fileName, @NonNull FlowType flowType) {
      this.fileSchemaName = fileSchemaName;
      this.fileName = fileName;
      this.flowType = flowType;
    }

    @Override
    public String getElementName() {
      return "errors";
    }

    @Override
    public String describe() {
      return String.format("%s-errors", fileName);
    }

    @Override
    public Pipe report(Pipe pipe) {
      return new Retain(new Each(pipe, keepInvalidTuplesFilter()), STATE_FIELD);
    }

    public FlowType getFlowType() {
      return this.flowType;
    }

    @Override
    public ReportCollector getCollector() {
      return new ErrorReportCollector(fileName);
    }

    @RequiredArgsConstructor
    class ErrorReportCollector implements ReportCollector {

      /**
       * Configuration.
       */
      private final String fileName;

      @Override
      public void collect(SubmissionPlatformStrategy platform, ReportContext context) {
        try {
          @Cleanup
          val reportInputStream = getReportInputStream(platform);
          val tupleStates = getTupleStates(reportInputStream);

          while (tupleStates.hasNext()) {
            val tupleState = tupleStates.next();
            if (tupleState.isInvalid()) {
              for (val errorTuple : tupleState.getErrors()) {
                context.reportError(
                    error()
                        .fileName(fileName)
                        .fieldNames(errorTuple.getColumnNames())
                        .type(errorTuple.getType())
                        .number(errorTuple.getNumber())
                        .lineNumber(errorTuple.getLine())
                        .value(errorTuple.getValue())
                        .params(errorTuple.getParameters())
                        .build());
              }
            }
          }

          context.reportLineNumbers(platform.getFile(fileName));
        } catch (FileNotFoundException fnfe) {
          // There were no errors
        } catch (Exception e) {
          throw new PlanExecutionException("Error collecting validation errors for file " + fileName, e);
        }
      }

      @SneakyThrows
      private InputStream getReportInputStream(SubmissionPlatformStrategy strategy) {
        return strategy.readReportTap(fileName, getFlowType(), getElementName());
      }

      @SneakyThrows
      private MappingIterator<TupleState> getTupleStates(InputStream reportInputStream) {
        val reader = new ObjectMapper().reader().forType(TupleState.class);

        return reader.readValues(reportInputStream);
      }
    }

  }

}
