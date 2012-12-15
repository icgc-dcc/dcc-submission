/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation.report;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.PlanExecutionException;
import org.icgc.dcc.validation.ReportingFlowPlanningVisitor;
import org.icgc.dcc.validation.ReportingPlanElement;
import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.TupleStates;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;

import com.google.common.io.Closeables;

public class ErrorPlanningVisitor extends ReportingFlowPlanningVisitor {

  public ErrorPlanningVisitor(FlowType type) {
    super(type);
  }

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);
    collect(new ErrorsPlanElement(fileSchema, this.getFlow()));
  }

  static class ErrorsPlanElement implements ReportingPlanElement {

    private final FileSchema fileSchema;

    private final FlowType flowType;

    public ErrorsPlanElement(FileSchema fileSchema, FlowType flowType) {
      this.fileSchema = fileSchema;
      this.flowType = flowType;
    }

    @Override
    public String getName() {
      return "errors";
    }

    @Override
    public String describe() {
      return String.format("errors");
    }

    @Override
    public Pipe report(Pipe pipe) {
      return new Retain(new Each(pipe, TupleStates.keepInvalidTuplesFilter()), ValidationFields.STATE_FIELD);
    }

    public FileSchema getFileSchema() {
      return this.fileSchema;
    }

    public FlowType getFlowType() {
      return this.flowType;
    }

    @Override
    public ReportCollector getCollector() {
      return new ErrorReportCollector();
    }

    class ErrorReportCollector implements ReportCollector {

      private final Map<ValidationErrorCode, ValidationErrorReport> errorMap =
          new HashMap<ValidationErrorCode, ValidationErrorReport>();

      public ErrorReportCollector() {
      }

      @Override
      public Outcome collect(CascadingStrategy strategy, SchemaReport report) {
        InputStream src = null;
        try {
          src = strategy.readReportTap(getFileSchema(), getFlowType(), getName());

          report.setName(strategy.path(getFileSchema()).getName());

          ObjectMapper mapper = new ObjectMapper();

          Outcome outcome = Outcome.PASSED;
          MappingIterator<TupleState> tupleStates = mapper.reader().withType(TupleState.class).readValues(src);
          while(tupleStates.hasNext()) {
            TupleState tupleState = tupleStates.next();
            if(tupleState.isInvalid()) {
              outcome = Outcome.FAILED;
              for(TupleState.TupleError error : tupleState.getErrors()) {
                if(errorMap.containsKey(error.getCode()) == true) {
                  ValidationErrorReport errorReport = errorMap.get(error.getCode());
                  errorReport.updateReport(error);
                } else {
                  errorMap.put(error.getCode(), new ValidationErrorReport(error));
                }
              }
            }
          }
          for(ValidationErrorReport e : errorMap.values()) {
            e.updateLineNumbers(strategy.path(getFileSchema()));
            report.errors.add(e);
          }
          return outcome;
        } catch(FileNotFoundException fnfe) {
          return Outcome.PASSED;
        } catch(IOException e) {
          throw new PlanExecutionException(e);
        } finally {
          Closeables.closeQuietly(src);
        }
      }
    }
  }
}
