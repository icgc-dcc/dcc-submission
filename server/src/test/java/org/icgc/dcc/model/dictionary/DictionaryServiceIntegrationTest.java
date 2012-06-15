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
package org.icgc.dcc.model.dictionary;

import java.util.List;

import org.icgc.dcc.service.HasRelease;
import org.icgc.dcc.service.ReleaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

/**
 * TODO: remove - just to test quickly
 */
// @RunWith(GuiceJUnitRunner.class)
// @GuiceModules({ ConfigModule.class, CoreModule.class, HttpModule.class, JerseyModule.class, ModelModule.class })
public class DictionaryServiceIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(DictionaryServiceIntegrationTest.class);

  private DictionaryService dictionaryService;

  private ReleaseService releaseService;

  @Inject
  public void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }

  @Inject
  public void setReleaseService(ReleaseService releaseService) {
    this.releaseService = releaseService;
  }

  // @Test
  public void test_dictionary_typicalWorkflow() {

    List<HasRelease> releaseList = releaseService.list();
    Function<HasRelease, String> function2 = new Function<HasRelease, String>() {
      @Override
      public String apply(HasRelease release) {
        return release.getRelease().getName();
      }
    };
    Iterable<String> dictionaryStringList2 = Iterables.transform(releaseList, function2);
    log.info("" + dictionaryStringList2);

    Function<Dictionary, String> function = new Function<Dictionary, String>() {
      @Override
      public String apply(Dictionary dictionary) {
        return toDisplayString(dictionary);
      }
    };
    log.info("" + Iterables.transform(dictionaryService.list(), function));

    Dictionary dictionary = dictionaryService.getFromVersion("vd1");
    log.info(dictionary.getVersion() + " - " + dictionary.getState());

    dictionaryService.close(dictionary);
    log.info("" + dictionaryService.getFromVersion("vd1").getState());

    Dictionary dictionary2 = new Dictionary();
    dictionary2.setVersion("vd3");
    dictionary2.setState(DictionaryState.OPENED);
    dictionaryService.update(dictionary2);

    log.info("" + Iterables.transform(dictionaryService.list(), function));

    Dictionary clone = dictionaryService.clone("vd8", "vd10");
    log.info(clone.getVersion() + "-" + clone.getState());
    log.info("" + Iterables.transform(dictionaryService.list(), function));

    clone.close();
    clone.getFiles().get(0).setLabel("balbal");
    dictionaryService.update(clone);
    log.info("" + Iterables.transform(dictionaryService.list(), function));
  }

  public static String toDisplayString(Dictionary dictionary) {
    Function<FileSchema, String> function = new Function<FileSchema, String>() {
      @Override
      public String apply(FileSchema fileSchema) {
        return toDisplayString(fileSchema);
      }
    };
    return dictionary.getVersion() + "-" + dictionary.getState() + "-"
        + Iterables.transform(dictionary.getFiles(), function);
  }

  public static String toDisplayString(FileSchema fileSchema) {
    return fileSchema.getName() + "-" + fileSchema.getLabel();
  }
}
