/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

import java.io.File;
import java.util.List;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Relation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

/**
 * As suggested by Bob (temporary).
 */
public class Meta {

  static List<String> OPTIONALS = newArrayList("biomarker", "exposure", "family", "surgery", "therapy");

  @SneakyThrows
  public static void main(String[] args) {
    Dictionary dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(new File("/home/tony/tmp/dcc/0.7c/Dictionary.json"));

    for (val fileSchema : dictionary.getFiles()) {
      val name = fileSchema.getName();

      if (!OPTIONALS.contains(name)) {

        List<String> fieldNames = newArrayList(fileSchema.getFieldNames());
        List<String> uniqueFieldNames = fileSchema.getUniqueFields();
        if (!uniqueFieldNames.isEmpty()) {
          List<Integer> indices = indicesOf(uniqueFieldNames, fieldNames);

          System.out.println(format("public static final List<Integer> %s_PKS = newArrayList(%s);",
              name.toUpperCase(), formatIndices(indices)));

          System.out.println(format("public static final List<String> %s_PK_NAMES = newArrayList(%s);",
              name.toUpperCase(), formatNames(uniqueFieldNames)));

        }

        List<Relation> relations = fileSchema.getRelations();
        int size = relations.size();
        if (size == 0) {
          // :
        } else if (size == 1) {
          val relation = relations.get(0);
          val referencingFieldNames = relation.getFields();
          List<Integer> indices = indicesOf(referencingFieldNames, fieldNames);

          System.out.println(format("public static final List<Integer> %s_FKS = newArrayList(%s);",
              name.toUpperCase(), formatIndices(indices)));

          System.out.println(format("public static final List<String> %s_FK_NAMES = newArrayList(%s);",
              name.toUpperCase(), formatNames(referencingFieldNames)));

        } else {
          checkState(size == 2 && name.toLowerCase().endsWith("_m"), "%s, %s", size, name);

          val relation1 = relations.get(0);
          val referencingFieldNames1 = relation1.getFields();
          List<Integer> indices1 = indicesOf(referencingFieldNames1, fieldNames);

          System.out.println(format("public static final List<Integer> %s_FKS1 = newArrayList(%s);",
              name.toUpperCase(), formatIndices(indices1)));

          System.out.println(format("public static final List<String> %s_FK1_NAMES = newArrayList(%s);",
              name.toUpperCase(), formatNames(referencingFieldNames1)));

          val relation2 = relations.get(1);
          val referencingFieldNames2 = relation2.getFields();
          List<Integer> indices2 = indicesOf(referencingFieldNames2, fieldNames);

          System.out.println(format("public static final List<Integer> %s_FKS2 = newArrayList(%s);",
              name.toUpperCase(), formatIndices(indices2)));

          System.out.println(format("public static final List<String> %s_FK2_NAMES = newArrayList(%s);",
              name.toUpperCase(), formatNames(referencingFieldNames2)));

        }
      }
    }
  }

  private static String formatNames(List<String> uniqueFieldNames) {
    return uniqueFieldNames.toString().replace("[", "\"").replace("]", "\"").replace(", ", "\", \"");
  }

  private static String formatIndices(List<Integer> indices) {
    return indices.toString().replace("[", "").replace("]", "");
  }

  private static List<Integer> indicesOf(List<String> uniqueFields, List<String> fieldNames) {
    val builder = new ImmutableList.Builder<Integer>();
    for (val uniqueFieldName : uniqueFields) {
      builder.add(fieldNames.indexOf(uniqueFieldName));
    }
    return builder.build();
  }
}
