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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Maps.uniqueIndex;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.dictionary.model.ValueType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;

/**
 * Translates validator object into simpler structures (temporary solution).
 */
public class Translator {

  private static final String CODELIST_RESTRICTION_TYPE = "codelist";

  private static final String CODELIST_NAME_PROPERTY = "name";

  public static Map<String, ValueType> translateFileSchema(FileSchema fileSchema) {
    ImmutableMap.Builder<String, ValueType> builder = new ImmutableMap.Builder<String, ValueType>();
    for(Field field : fileSchema.getFields()) {
      builder.put(field.getName(), field.getValueType());
    }
    return builder.build();
  }

  public static Table<String, String, String> translateCodeLists(FileSchema fileSchema, List<CodeList> codeLists) {
    // Create an index for efficient lookup
    ImmutableMap<String, CodeList> codeListByName = uniqueIndex(codeLists, new Function<CodeList, String>() {
      @Override
      public String apply(CodeList codeList) {
        return codeList.getName();
      }
    });

    // This has to be mutable since ImmutableTable is not serializable. This will cause errors downstream with cascading
    // and hadoop.
    Table<String, String, String> table = HashBasedTable.create();

    for(Field field : fileSchema.getFields()) {
      // Try to find a code list restriction
      Optional<Restriction> restriction = tryFind(field.getRestrictions(), new Predicate<Restriction>() {
        @Override
        public boolean apply(Restriction restriction) {
          return CODELIST_RESTRICTION_TYPE.equals(restriction.getType());
        }
      });

      if(restriction.isPresent()) {
        // Resolve code list
        String codeListName = restriction.get().getConfig().getString(CODELIST_NAME_PROPERTY);
        CodeList codeList = codeListByName.get(codeListName);
        checkNotNull(codeList, "Code list %s referenced by field %s is unknown", codeListName, field);

        // Add code list terms
        String fieldName = field.getName();
        for(Term term : codeList.getTerms()) {
          table.put(fieldName, term.getCode(), term.getValue());
        }
      }
    }

    return table;
  }

}
