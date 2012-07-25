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

import java.util.List;
import java.util.Map;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.SummaryType;
import org.icgc.dcc.validation.ReportingPlanElement;

import cascading.tuple.Fields;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

abstract class BaseReportingPlanElement implements ReportingPlanElement {

  static final String FIELD = "field";

  static final String VALUE = "value";

  static final String REPORT = "report";

  static final Fields FIELD_FIELDS = new Fields(FIELD);

  static final Fields VALUE_FIELDS = new Fields(VALUE);

  static final Fields FIELD_VALUE_FIELDS = new Fields(FIELD, VALUE);

  static final Fields REPORT_FIELDS = new Fields(REPORT);

  protected final FileSchema fileSchema;

  protected final SummaryType summaryType;

  protected final List<Field> fields;

  protected BaseReportingPlanElement(FileSchema fileSchema, List<Field> fields, SummaryType summaryType) {
    this.fileSchema = fileSchema;
    this.fields = fields;
    this.summaryType = summaryType;
  }

  @Override
  public String getName() {
    return this.summaryType.getDescription();
  }

  @Override
  public String describe() {
    return String.format("%s%s", summaryType.getDescription(), fields);
  }

  protected String buildSubPipeName(String prefix) {
    return fileSchema.getName() + "_" + prefix + "_" + "pipe";
  }

  public static class FieldSummary {// TODO: use FieldReport instead?

    public String field;

    public long populated;

    public long nulls;

    public Map<String, Object> summary = Maps.newLinkedHashMap();

    @Override
    public String toString() { // for testing only for now
      return Objects.toStringHelper(FieldSummary.class).add("field", field).add("populated", populated)
          .add("nulls", nulls).add("summary", summary).toString();
    }
  }
}