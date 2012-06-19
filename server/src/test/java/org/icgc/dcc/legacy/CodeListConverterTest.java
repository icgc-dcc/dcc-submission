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

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

/**
 * 
 */
public class CodeListConverterTest {

  @Test
  public void test_codeListConverter_compareJSON() throws IOException {

    CodeListConverter clc = new CodeListConverter();
    clc.readCodec("src/test/resources/converter/codec");
    clc.saveToJSON("src/test/resources/codeList.json");

    File testFile = new File("src/test/resources/codeList.json");
    File refFile = new File("src/main/resources/codeList.json");

    ObjectMapper mapper = new ObjectMapper();

    JsonNode testTree = mapper.readTree(FileUtils.readFileToString(testFile));
    JsonNode refTree = mapper.readTree(FileUtils.readFileToString(refFile));

    assertEquals(refTree.size(), testTree.size());

    for(int i = 0; i < refTree.size(); i++) {
      JsonNode testCodeList = testTree.get(i);
      JsonNode refCodeList = refTree.get(i);

      assertEquals(refCodeList.get("name"), testCodeList.get("name"));
      assertEquals(refCodeList.get("label"), testCodeList.get("label"));
      assertEquals(refCodeList.get("terms"), testCodeList.get("terms"));
    }

  }

}
