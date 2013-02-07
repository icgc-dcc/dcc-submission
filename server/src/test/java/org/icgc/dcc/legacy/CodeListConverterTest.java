/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class CodeListConverterTest {

  private static final Logger log = LoggerFactory.getLogger(CodeListConverterTest.class);

  private static final String INPUT_DIR = "src/test/resources/converter/codec";

  private static final String CURRENT_CODELISTS = "src/main/resources/codeList.json";

  private static final String NEW_CODELISTS = "target/codeList.json";

  @Test
  public void test_codeListConverter_compareJSON() throws IOException {

    CodeListConverter clc = new CodeListConverter();
    log.info("current: " + CURRENT_CODELISTS);
    log.info("new: " + NEW_CODELISTS);
    clc.readCodec(INPUT_DIR);
    clc.saveToJSON(NEW_CODELISTS);

    File testFile = new File(NEW_CODELISTS);
    File refFile = new File(CURRENT_CODELISTS);

    ObjectMapper mapper = new ObjectMapper();

    JsonNode testTree = mapper.readTree(FileUtils.readFileToString(testFile));
    JsonNode refTree = mapper.readTree(FileUtils.readFileToString(refFile));

    assertEquals(refTree.size(), testTree.size());

    for(int i = 0; i < refTree.size(); i++) {
      JsonNode refCodeList = refTree.get(i);
      JsonNode testCodeList = this.findNode(testTree, refCodeList.get("name"));

      assertTrue(testCodeList != null);

      assertEquals(refCodeList.get("name"), testCodeList.get("name"));
      assertEquals(refCodeList.get("label"), testCodeList.get("label"));

      this.test_compare_termNodes(refCodeList.get("terms"), testCodeList.get("terms"));
    }

  }

  private void test_compare_termNodes(JsonNode refTerms, JsonNode testTerms) {
    assertEquals(refTerms.size(), testTerms.size());

    // check each individual terms
    for(int j = 0; j < refTerms.size(); j++) {
      JsonNode refTerm = refTerms.get(j);
      JsonNode testTerm = this.findTermNode(testTerms, refTerm.get("code"));

      assertTrue(testTerm != null);

      assertEquals(refTerm.get("code"), testTerm.get("code"));
      assertEquals(refTerm.get("value"), testTerm.get("value"));
      assertEquals(refTerm.get("uri"), testTerm.get("uri"));
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

  private JsonNode findTermNode(JsonNode tree, JsonNode code) {
    for(int i = 0; i < tree.size(); i++) {
      if(tree.get(i).get("code").equals(code)) {
        return tree.get(i);
      }
    }
    return null;
  }
}
