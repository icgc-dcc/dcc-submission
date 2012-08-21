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
package org.icgc.dcc.legacy;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * 
 */
public class DictionaryConverterTest {

  private static final String CONVERSION_INPUT_FOLDER = "src/test/resources/converter/source/";

  private static final String CURRENT_DICTIONARY = "src/main/resources/dictionary.json";

  private static final String TEMPORARY_DICTIONARY = CURRENT_DICTIONARY + ".tmp";

  private static final String SECOND_DICTIONARY = "src/test/resources/integrationtest/secondDictionary.json";

  @Before
  public void setUp() {

  }

  @Test
  public void test_dictionaryConverter_compareJSON() throws IOException, XPathExpressionException,
      ParserConfigurationException, SAXException {

    DictionaryConverter dc = new DictionaryConverter();
    dc.readDictionary(CONVERSION_INPUT_FOLDER);
    dc.saveToJSON(TEMPORARY_DICTIONARY);

    File testFile = new File(TEMPORARY_DICTIONARY);
    File refFile = new File(CURRENT_DICTIONARY);

    ObjectMapper mapper = new ObjectMapper();

    JsonNode testTree = mapper.readTree(FileUtils.readFileToString(testFile));
    JsonNode refTree = mapper.readTree(FileUtils.readFileToString(refFile));

    // check Dictionary fields
    assertEquals(refTree.get("name"), testTree.get("name"));
    assertEquals(refTree.get("version"), testTree.get("version"));
    assertEquals(refTree.get("state"), testTree.get("state"));

    this.test_compare_fileSchema(refTree.get("files"), testTree.get("files"));
    updateFilesInProject(CURRENT_DICTIONARY, TEMPORARY_DICTIONARY, SECOND_DICTIONARY);
  }

  private void updateFilesInProject(String currentDictionary, String temporaryDictionary, String destination)
      throws IOException {
    Files.move(new File(temporaryDictionary), new File(currentDictionary));
    String content = Files.toString(new File(currentDictionary), Charsets.UTF_8);
    content = updateSecondDictionaryContent(content);
    Files.write(content.getBytes(), new File(destination));
  }

  private String updateSecondDictionaryContent(String content) {
    return content.replace("\"0.6c\"", "\"0.6d\""); // very basic for now
  }

  private void test_compare_fileSchema(JsonNode refFileSchemas, JsonNode testFileSchemas) {
    // check FileSchema List Size
    assertEquals(refFileSchemas.size(), testFileSchemas.size());
    // check each FileSchema
    for(int i = 0; i < refFileSchemas.size(); i++) {
      JsonNode refNode = refFileSchemas.get(i);
      JsonNode testNode = this.findNode(testFileSchemas, refNode.get("name"));

      assertEquals(refNode.get("name"), testNode.get("name"));
      assertEquals(refNode.get("label"), testNode.get("label"));
      assertEquals(refNode.get("pattern"), testNode.get("pattern"));
      assertEquals(refNode.get("role"), testNode.get("role"));
      assertEquals(refNode.get("uniqueFields"), testNode.get("uniqueFields"));
      this.test_compare_field(refNode.get("fields"), testNode.get("fields"));
      this.test_compare_relation(refNode.get("relation"), testNode.get("relation"));

    }
  }

  private void test_compare_field(JsonNode refFields, JsonNode testFields) {
    assertEquals(refFields.size(), testFields.size());

    for(int i = 0; i < refFields.size(); i++) {
      JsonNode refField = refFields.get(i);
      JsonNode testField = this.findNode(testFields, refField.get("name"));

      assertEquals(refField.get("name"), testField.get("name"));
      assertEquals(refField.get("label"), testField.get("label"));
      assertEquals(refField.get("valueType"), testField.get("valueType"));

      this.test_compare_restriction(refField.get("restrictions"), testField.get("restrictions"));
    }
  }

  private void test_compare_restriction(JsonNode refRestrictions, JsonNode testRestrictions) {
    assertEquals(refRestrictions.size(), testRestrictions.size());

    for(int i = 0; i < refRestrictions.size(); i++) {
      JsonNode refRestriction = refRestrictions.get(i);
      JsonNode testRestriction = this.findRestrictionNode(testRestrictions, refRestriction.get("type"));

      assertEquals(refRestriction.get("type"), testRestriction.get("type"));
      assertEquals(refRestriction.get("config"), testRestriction.get("config"));
    }
  }

  private void test_compare_relation(JsonNode refRelation, JsonNode testRelation) {
    assertEquals(refRelation.get("fields"), testRelation.get("fields"));
    assertEquals(refRelation.get("other"), testRelation.get("other"));
    assertEquals(refRelation.get("allowOrphan"), testRelation.get("allowOrphan"));
    assertEquals(refRelation.get("joinType"), testRelation.get("joinType"));
    assertEquals(refRelation.get("otherFields"), testRelation.get("otherFields"));
  }

  private JsonNode findNode(JsonNode tree, JsonNode name) {
    for(int i = 0; i < tree.size(); i++) {
      if(tree.get(i).get("name").equals(name)) {
        return tree.get(i);
      }
    }
    return null;
  }

  private JsonNode findRestrictionNode(JsonNode tree, JsonNode type) {
    for(int i = 0; i < tree.size(); i++) {
      if(tree.get(i).get("type").equals(type)) {
        return tree.get(i);
      }
    }
    return null;
  }
}
