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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.model.dictionary.CodeList;
import org.icgc.dcc.model.dictionary.Term;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

/**
 * 
 */
public class CodeListConverter {
  private final List<CodeList> codec = new ArrayList<CodeList>();

  public void saveToJSON(String fileName) throws JsonGenerationException, JsonMappingException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), codec);
  }

  public void readCodec(String folder) throws IOException {

    File tsvFolder = new File(folder);
    File[] tsvFiles = tsvFolder.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".tsv");
      }

    });

    for(File tsvFile : tsvFiles) {
      this.codec.add(this.readCodeList(tsvFile));
    }
  }

  private CodeList readCodeList(File tsvFile) throws IOException {
    CodeList codeList = new CodeList();
    String codeListName = FilenameUtils.removeExtension(tsvFile.getName());
    codeList.setName(codeListName);

    String codeListText = Files.toString(tsvFile, Charsets.UTF_8);

    Iterable<String> lines = Splitter.on('\n').trimResults().omitEmptyStrings().split(codeListText);

    List<Term> terms = new ArrayList<Term>();
    for(String line : lines) {
      terms.add(this.readTerm(line));
    }
    codeList.setTerms(terms);

    return codeList;
  }

  private Term readTerm(String line) {
    Term term = new Term();

    Iterable<String> values = Splitter.on('\t').trimResults().omitEmptyStrings().split(line);

    Iterator<String> iterator = values.iterator();

    String code = iterator.next();
    String value = iterator.next();

    term.setCode(code);
    term.setValue(value);

    return term;
  }
}
