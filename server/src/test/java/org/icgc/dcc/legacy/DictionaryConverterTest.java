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

/**
 * 
 */
public class DictionaryConverterTest {

  @Before
  public void setUp() {

  }

  @Test
  public void test_dictionaryConverter_compareJSON() throws IOException, XPathExpressionException,
      ParserConfigurationException, SAXException {

    DictionaryConverter dc = new DictionaryConverter();
    dc.readDictionary("src/test/resources/converter/source/");
    dc.saveToJSON("src/test/resources/dictionary.json");

    File testFile = new File("src/test/resources/dictionary.json");
    File refFile = new File("src/main/resources/dictionary.json");

    ObjectMapper mapper = new ObjectMapper();

    JsonNode testTree = mapper.readTree(FileUtils.readFileToString(testFile));
    JsonNode refTree = mapper.readTree(FileUtils.readFileToString(refFile));

    // check Dictionary fields
    assertEquals(refTree.get("name"), testTree.get("name"));
    assertEquals(refTree.get("version"), testTree.get("version"));
    assertEquals(refTree.get("state"), testTree.get("state"));

    JsonNode testFileSchemaList = testTree.get("files");
    JsonNode refFileSchemaList = refTree.get("files");

    // check FileSchema List Size
    assertEquals(refFileSchemaList.size(), testFileSchemaList.size());

    // check each FileSchema
    for(int i = 0; i < refFileSchemaList.size(); i++) {
      JsonNode refNode = refFileSchemaList.get(i);
      JsonNode testNode = this.findNode(testFileSchemaList, refNode.get("name"));

      assertEquals(refNode.get("name"), testNode.get("name"));
      assertEquals(refNode.get("label"), testNode.get("label"));
      assertEquals(refNode.get("pattern"), testNode.get("pattern"));
      assertEquals(refNode.get("role"), testNode.get("role"));
      assertEquals(refNode.get("uniqueFields"), testNode.get("uniqueFields"));
      assertEquals(refNode.get("fields"), testNode.get("fields"));
    }
  }

  private JsonNode findNode(JsonNode tree, JsonNode name) {
    for(int i = 0; i < tree.size(); i++) {
      if(tree.get(i).get("name").equals(name)) {
        return tree.get(i);
      }
    }
    return null;
  }

}
