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
package org.icgc.dcc;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.dictionary.model.ValueType;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

/**
 * Translates validator object into simpler structures (temporary solution).
 */
public class Translator {
  public static Map<String, ValueType> translateFileSchema(FileSchema fileSchema) {
    ImmutableMap.Builder<String, ValueType> builder = new ImmutableMap.Builder<String, ValueType>();
    for(Field field : fileSchema.getFields()) {
      builder.put(field.getName(), field.getValueType());
    }
    return builder.build();
  }

  public static Table<String, String, String> translateCodeLists(FileSchema fileSchema, List<CodeList> codeLists) {
    ImmutableMap<String, CodeList> index = Maps.uniqueIndex(codeLists, new Function<CodeList, String>() {
      @Override
      public String apply(CodeList codeList) {
        return codeList.getName();
      }
    });

    ImmutableTable.Builder<String, String, String> builder = ImmutableTable.builder();

    for(Field field : fileSchema.getFields()) {
      String fieldName = field.getName();
      CodeList codeList = index.get(fieldName);
      if(null != codeList) {
        for(Term term : codeList.getTerms()) {
          builder.put(fieldName, term.getValue(), term.getCode());
        }
      }
    }

    return builder.build();
  }

}
