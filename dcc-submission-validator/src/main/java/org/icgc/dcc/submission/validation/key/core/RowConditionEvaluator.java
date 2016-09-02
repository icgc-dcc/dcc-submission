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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.mvel2.MVEL;
import org.mvel2.compiler.ExecutableStatement;

import com.google.common.collect.ImmutableMap;

public class RowConditionEvaluator {

  private final ExecutableStatement statement;
  private final List<String> fieldNames;

  public RowConditionEvaluator(@NonNull String script, @NonNull List<String> fieldNames) {
    this.statement = (ExecutableStatement) MVEL.compileExpression(script);
    this.fieldNames = fieldNames;
  }

  public boolean evaluate(@NonNull List<String> row) {
    val result = MVEL.executeExpression(statement, prepareInput(row));
    checkState(result instanceof Boolean, "Failed to execute script. Result: %s", result);

    return (Boolean) result;
  }

  private Map<String, Object> prepareInput(List<String> row) {
    checkState(fieldNames.size() == row.size(), "Failed verify script for row. \nExpected fields:%s. \nRow: %s",
        fieldNames, row);
    val input = ImmutableMap.<String, Object> builder();
    for (int i = 0; i < fieldNames.size(); i++) {
      input.put(fieldNames.get(i), row.get(i));
    }

    return input.build();
  }

}
