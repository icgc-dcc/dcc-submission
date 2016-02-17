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
package org.icgc.dcc.submission.generator.utils;

import static com.google.common.io.Resources.getResource;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.CodeList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.ImmutableList;

/**
 * Initializes and stores {@link CodeList}s.
 */
@Slf4j
public class CodeLists {

  /**
   * Constants.
   */
  private static final ObjectReader READER = new ObjectMapper().reader(CodeList.class);
  private static final String DEFAULT_CODELISTS_PATH = "org/icgc/dcc/resources/CodeList.json";
  private static final URL DEFAULT_CODELISTS_URL = getResource(DEFAULT_CODELISTS_PATH);

  private final List<CodeList> codeLists;

  @SneakyThrows
  public CodeLists(File codeListFile) {
    log.info("Initializing codelist from file: {}", codeListFile.getAbsolutePath());
    this.codeLists = resolveCodeLists(codeListFile.toURI().toURL());
  }

  public CodeLists() {
    log.info("Initializing codelist from URL: {}", DEFAULT_CODELISTS_URL);
    this.codeLists = resolveCodeLists(DEFAULT_CODELISTS_URL);
  }

  @SneakyThrows
  private List<CodeList> resolveCodeLists(URL url) {
    Iterator<CodeList> values = READER.readValues(url);
    return ImmutableList.copyOf(values);
  }

  public List<CodeList> getCodeLists() {
    return codeLists;
  }

}
