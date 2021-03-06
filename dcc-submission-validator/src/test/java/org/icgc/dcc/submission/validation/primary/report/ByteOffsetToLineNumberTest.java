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
package org.icgc.dcc.submission.validation.primary.report;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Files.getFileExtension;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ByteOffsetToLineNumberTest {

  /**
   * Test configuration.
   */
  private static final String TEST_DIR = "src/test/resources/fixtures/validation/line-numbers";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  FileSystem fileSystem;

  @Before
  @SneakyThrows
  public void setUp() {
    fileSystem = FileSystem.getLocal(new Configuration());

    ByteOffsetToLineNumber.fileSystem = fileSystem;
  }

  @Test
  @SneakyThrows
  public void testConvert() {
    for (val file : new File(TEST_DIR).listFiles()) {
      if (file.getName().endsWith("bz2")) {
        // Skip this file as it is for the other test only.
        continue;
      }

      log.info("Processing '{}'", file.getName());
      val expected = getMapping(file);
      val offsets = expected.keySet();
      log.info("Expected: {}", expected);

      Path path = new Path(tmp.newFile(file.getName()).getAbsolutePath());
      fileSystem.copyFromLocalFile(new Path(file.toURI()), path);

      // Exercise
      val actual = ByteOffsetToLineNumber.convert(path, offsets, false);

      assertThat(actual).isEqualTo(expected);
    }
  }

  /**
   * See https://jira.oicr.on.ca/browse/DCC-4752
   */
  @Test(expected = IllegalStateException.class)
  @SneakyThrows
  public void testConvertBadBzip() {
    val offsets = createPart0And1Offsets();
    val file = new File(TEST_DIR, "ssm_p.20160408.txt.bz2");
    Path path = new Path(tmp.newFile(file.getName()).getAbsolutePath());
    fileSystem.copyFromLocalFile(new Path(file.toURI()), path);

    // Exercise. This shouldn't throw, but it does due to how bz2 is working?!?
    ByteOffsetToLineNumber.convert(path, offsets, false);
  }

  @SneakyThrows
  private static Map<Long, Long> getMapping(File file) {
    val mapping = ImmutableMap.<Long, Long> builder();
    long offset = 0;
    long lineNumber = 1;

    for (val b : getBytes(file)) {
      offset++;
      if ((char) b == '\n') {
        lineNumber++;

        log.info("line {} = {} byte offset", lineNumber, offset);
        mapping.put(offset, lineNumber);
      }
    }

    return mapping.build();
  }

  @SneakyThrows
  private static byte[] getBytes(File file) {
    @Cleanup
    val input = new FileInputStream(file);
    byte[] bytes = toByteArray(isGzip(file) ? new GZIPInputStream(input) : input);

    return bytes;
  }

  private static boolean isGzip(File file) {
    return getFileExtension(file.getName()).equals("gz");
  }

  /**
   * Transcribed from https://jira.oicr.on.ca/secure/attachment/34954/line-number-error.tar.gz
   */
  @SuppressWarnings("unused")
  private static Collection<Long> createPart1Offsets() {
    val offsets = Lists.<Long> newArrayList();

    offsets.add(25505L);
    offsets.add(56952L);
    offsets.add(106004L);
    offsets.add(115834L);
    offsets.add(129244L);
    offsets.add(136866L);
    offsets.add(141208L);
    offsets.add(143082L);
    offsets.add(150062L);
    offsets.add(173127L);
    offsets.add(182919L);
    offsets.add(189380L);
    offsets.add(192801L);
    offsets.add(194469L);
    offsets.add(201442L);
    offsets.add(209361L);
    offsets.add(211665L);
    offsets.add(238821L);
    offsets.add(259593L);
    offsets.add(265001L);
    offsets.add(269884L);
    offsets.add(280080L);
    offsets.add(287008L);
    offsets.add(298404L);
    offsets.add(390996L);
    offsets.add(770093L);
    offsets.add(785765L);
    offsets.add(788673L);
    offsets.add(820956L);
    offsets.add(839066L);
    offsets.add(856819L);
    offsets.add(882134L);
    offsets.add(907293L);
    offsets.add(909631L);
    offsets.add(914207L);
    offsets.add(1030163L);
    offsets.add(1067625L);
    offsets.add(1156490L);
    offsets.add(1253430L);
    offsets.add(1272910L);
    offsets.add(1290921L);
    offsets.add(1305805L);
    offsets.add(1321846L);
    offsets.add(1332596L);
    offsets.add(1341534L);
    offsets.add(1368396L);
    offsets.add(1377431L);
    offsets.add(1389276L);
    offsets.add(1513574L);
    offsets.add(1660601L);
    offsets.add(1668233L);
    offsets.add(1678165L);
    offsets.add(1682612L);
    offsets.add(1693414L);
    offsets.add(1703362L);
    offsets.add(1719703L);
    offsets.add(1743658L);
    offsets.add(1752120L);
    offsets.add(1755638L);
    offsets.add(1762659L);
    offsets.add(1774957L);
    offsets.add(1793799L);
    offsets.add(1825759L);
    offsets.add(1833332L);
    offsets.add(1845029L);
    offsets.add(1851812L);
    offsets.add(1861358L);
    offsets.add(1883720L);
    offsets.add(1891775L);
    offsets.add(1908917L);
    offsets.add(1912684L);
    offsets.add(1922562L);
    offsets.add(1938530L);
    offsets.add(1947817L);
    offsets.add(4195568L);
    offsets.add(4989694L);
    offsets.add(4999350L);
    offsets.add(5146111L);
    offsets.add(5148841L);
    offsets.add(5165861L);
    offsets.add(5175575L);
    offsets.add(5199838L);
    offsets.add(5215055L);
    offsets.add(5291941L);
    offsets.add(5458000L);
    offsets.add(5477882L);
    offsets.add(5483074L);
    offsets.add(5489347L);
    offsets.add(5511693L);
    offsets.add(5885542L);
    offsets.add(5898984L);
    offsets.add(5906140L);
    offsets.add(5930882L);
    offsets.add(5957953L);
    offsets.add(5960300L);
    offsets.add(5989816L);
    offsets.add(6011645L);
    offsets.add(6039183L);
    offsets.add(6143253L);
    offsets.add(6323716L);
    offsets.add(6664290L);
    offsets.add(6858421L);
    offsets.add(6870730L);
    offsets.add(6872233L);
    offsets.add(6919148L);
    offsets.add(6923940L);
    offsets.add(6932298L);
    offsets.add(6949949L);
    offsets.add(6957107L);
    offsets.add(6962597L);
    offsets.add(7010143L);
    offsets.add(7016450L);
    offsets.add(7023008L);
    offsets.add(7029448L);
    offsets.add(7041739L);
    offsets.add(7053204L);
    offsets.add(7060912L);
    offsets.add(7076083L);
    offsets.add(7174364L);
    offsets.add(7410954L);
    offsets.add(7421533L);
    offsets.add(7442157L);
    offsets.add(7455662L);
    offsets.add(7465468L);
    offsets.add(7473631L);
    offsets.add(7493406L);
    offsets.add(7512183L);
    offsets.add(7520029L);
    offsets.add(7535974L);
    offsets.add(7546220L);
    offsets.add(7549004L);
    offsets.add(7560058L);
    offsets.add(8237514L);
    offsets.add(8380882L);
    offsets.add(8388049L);
    offsets.add(8448372L);
    offsets.add(8708392L);
    offsets.add(8796745L);
    offsets.add(8973827L);
    offsets.add(9215264L);
    offsets.add(9225954L);
    offsets.add(9237204L);
    offsets.add(9272529L);
    offsets.add(9301048L);
    offsets.add(9322968L);
    offsets.add(9429988L);
    offsets.add(9452819L);
    offsets.add(9494992L);
    offsets.add(9506510L);
    offsets.add(9509970L);
    offsets.add(9678846L);
    offsets.add(9724498L);
    offsets.add(10039040L);
    offsets.add(10074576L);
    offsets.add(10180476L);
    offsets.add(10193312L);
    offsets.add(13168400L);
    offsets.add(13170456L);
    offsets.add(13202531L);
    offsets.add(13231262L);
    offsets.add(13491981L);
    offsets.add(13648981L);
    offsets.add(13962440L);
    offsets.add(13974742L);
    offsets.add(14631561L);
    offsets.add(14632730L);
    offsets.add(14687601L);
    offsets.add(14884670L);
    offsets.add(14886885L);
    offsets.add(14893944L);
    offsets.add(14927112L);
    offsets.add(15559969L);
    offsets.add(15687543L);
    offsets.add(15755151L);
    offsets.add(15769372L);
    offsets.add(16933180L);
    offsets.add(17107412L);
    offsets.add(17118135L);
    offsets.add(17128265L);
    offsets.add(17140614L);
    offsets.add(17143795L);
    offsets.add(17161313L);
    offsets.add(17204067L);
    offsets.add(17220367L);
    offsets.add(17238568L);
    offsets.add(17245593L);
    offsets.add(17257934L);
    offsets.add(17261599L);
    offsets.add(17265746L);
    offsets.add(17280121L);
    offsets.add(17283128L);
    offsets.add(17289457L);
    offsets.add(17306474L);
    offsets.add(17309896L);
    offsets.add(17315469L);
    offsets.add(17331980L);
    offsets.add(17335123L);
    offsets.add(17341551L);
    offsets.add(17345483L);
    offsets.add(17348071L);
    offsets.add(17351032L);
    offsets.add(17356376L);
    offsets.add(17358798L);
    offsets.add(17364950L);
    offsets.add(17370159L);
    offsets.add(17373763L);
    offsets.add(17375460L);
    offsets.add(17382260L);
    offsets.add(17386663L);
    offsets.add(17391935L);
    offsets.add(17407647L);
    offsets.add(17411955L);
    offsets.add(17432001L);
    offsets.add(17441853L);
    offsets.add(17459326L);
    offsets.add(17497379L);
    offsets.add(17514804L);
    offsets.add(17530223L);
    offsets.add(17539941L);
    offsets.add(17541827L);
    offsets.add(17543515L);
    offsets.add(17561082L);
    offsets.add(17579693L);
    offsets.add(17622588L);
    offsets.add(17643266L);
    offsets.add(17647329L);
    offsets.add(17655799L);
    offsets.add(17658815L);
    offsets.add(17668949L);
    offsets.add(17695193L);
    offsets.add(17699945L);
    offsets.add(17708266L);
    offsets.add(17713240L);
    offsets.add(17717851L);
    offsets.add(17738838L);
    offsets.add(17741442L);
    offsets.add(17747028L);
    offsets.add(17750976L);
    offsets.add(17754620L);
    offsets.add(17763293L);
    offsets.add(17782898L);
    offsets.add(17812724L);
    offsets.add(17816272L);
    offsets.add(17824144L);
    offsets.add(17833544L);
    offsets.add(17849107L);
    offsets.add(17852681L);
    offsets.add(17869123L);
    offsets.add(17894804L);
    offsets.add(17902523L);
    offsets.add(17904338L);
    offsets.add(17919416L);
    offsets.add(17930939L);
    offsets.add(17932426L);
    offsets.add(18049413L);
    offsets.add(18118363L);
    offsets.add(18652242L);
    offsets.add(18681439L);
    offsets.add(18685085L);
    offsets.add(18695983L);
    offsets.add(18706225L);
    offsets.add(18720171L);
    offsets.add(18739378L);
    offsets.add(18749949L);
    offsets.add(18813422L);
    offsets.add(18839185L);
    offsets.add(18985452L);
    offsets.add(18991305L);
    offsets.add(19101744L);
    offsets.add(19551284L);
    offsets.add(19570303L);
    offsets.add(19677342L);
    offsets.add(19798058L);
    offsets.add(19991177L);
    offsets.add(19995965L);
    offsets.add(20156208L);
    offsets.add(20214621L);
    offsets.add(20263800L);
    offsets.add(20269966L);
    offsets.add(20423726L);
    offsets.add(20455434L);
    offsets.add(20462104L);
    offsets.add(20516374L);
    offsets.add(20521107L);
    offsets.add(20523749L);
    offsets.add(20550270L);
    offsets.add(20576038L);

    return offsets;
  }

  private static Collection<Long> createPart0And1Offsets() {
    val offsets = Lists.<Long> newArrayList();
    offsets.add(25505L);
    offsets.add(56952L);
    offsets.add(106004L);
    offsets.add(115834L);
    offsets.add(129244L);
    offsets.add(136866L);
    offsets.add(141208L);
    offsets.add(143082L);
    offsets.add(150062L);
    offsets.add(173127L);
    offsets.add(182919L);
    offsets.add(189380L);
    offsets.add(192801L);
    offsets.add(194469L);
    offsets.add(201442L);
    offsets.add(209361L);
    offsets.add(211665L);
    offsets.add(238821L);
    offsets.add(259593L);
    offsets.add(265001L);
    offsets.add(269884L);
    offsets.add(280080L);
    offsets.add(287008L);
    offsets.add(298404L);
    offsets.add(390996L);
    offsets.add(770093L);
    offsets.add(785765L);
    offsets.add(788673L);
    offsets.add(820956L);
    offsets.add(839066L);
    offsets.add(856819L);
    offsets.add(882134L);
    offsets.add(907293L);
    offsets.add(909631L);
    offsets.add(914207L);
    offsets.add(1030163L);
    offsets.add(1067625L);
    offsets.add(1156490L);
    offsets.add(1253430L);
    offsets.add(1272910L);
    offsets.add(1290921L);
    offsets.add(1305805L);
    offsets.add(1321846L);
    offsets.add(1332596L);
    offsets.add(1341534L);
    offsets.add(1368396L);
    offsets.add(1377431L);
    offsets.add(1389276L);
    offsets.add(1513574L);
    offsets.add(1660601L);
    offsets.add(1668233L);
    offsets.add(1678165L);
    offsets.add(1682612L);
    offsets.add(1693414L);
    offsets.add(1703362L);
    offsets.add(1719703L);
    offsets.add(1743658L);
    offsets.add(1752120L);
    offsets.add(1755638L);
    offsets.add(1762659L);
    offsets.add(1774957L);
    offsets.add(1793799L);
    offsets.add(1825759L);
    offsets.add(1833332L);
    offsets.add(1845029L);
    offsets.add(1851812L);
    offsets.add(1861358L);
    offsets.add(1883720L);
    offsets.add(1891775L);
    offsets.add(1908917L);
    offsets.add(1912684L);
    offsets.add(1922562L);
    offsets.add(1938530L);
    offsets.add(1947817L);
    offsets.add(4195568L);
    offsets.add(4989694L);
    offsets.add(4999350L);
    offsets.add(5146111L);
    offsets.add(5148841L);
    offsets.add(5165861L);
    offsets.add(5175575L);
    offsets.add(5199838L);
    offsets.add(5215055L);
    offsets.add(5291941L);
    offsets.add(5458000L);
    offsets.add(5477882L);
    offsets.add(5483074L);
    offsets.add(5489347L);
    offsets.add(5511693L);
    offsets.add(5885542L);
    offsets.add(5898984L);
    offsets.add(5906140L);
    offsets.add(5930882L);
    offsets.add(5957953L);
    offsets.add(5960300L);
    offsets.add(5989816L);
    offsets.add(6011645L);
    offsets.add(6039183L);
    offsets.add(6143253L);
    offsets.add(6323716L);
    offsets.add(6664290L);
    offsets.add(6858421L);
    offsets.add(6870730L);
    offsets.add(6872233L);
    offsets.add(6919148L);
    offsets.add(6923940L);
    offsets.add(6932298L);
    offsets.add(6949949L);
    offsets.add(6957107L);
    offsets.add(6962597L);
    offsets.add(7010143L);
    offsets.add(7016450L);
    offsets.add(7023008L);
    offsets.add(7029448L);
    offsets.add(7041739L);
    offsets.add(7053204L);
    offsets.add(7060912L);
    offsets.add(7076083L);
    offsets.add(7174364L);
    offsets.add(7410954L);
    offsets.add(7421533L);
    offsets.add(7442157L);
    offsets.add(7455662L);
    offsets.add(7465468L);
    offsets.add(7473631L);
    offsets.add(7493406L);
    offsets.add(7512183L);
    offsets.add(7520029L);
    offsets.add(7535974L);
    offsets.add(7546220L);
    offsets.add(7549004L);
    offsets.add(7560058L);
    offsets.add(8237514L);
    offsets.add(8380882L);
    offsets.add(8388049L);
    offsets.add(8448372L);
    offsets.add(8708392L);
    offsets.add(8796745L);
    offsets.add(8973827L);
    offsets.add(9215264L);
    offsets.add(9225954L);
    offsets.add(9237204L);
    offsets.add(9272529L);
    offsets.add(9301048L);
    offsets.add(9322968L);
    offsets.add(9429988L);
    offsets.add(9452819L);
    offsets.add(9494992L);
    offsets.add(9506510L);
    offsets.add(9509970L);
    offsets.add(9678846L);
    offsets.add(9724498L);
    offsets.add(10039040L);
    offsets.add(10074576L);
    offsets.add(10180476L);
    offsets.add(10193312L);
    offsets.add(13168400L);
    offsets.add(13170456L);
    offsets.add(13202531L);
    offsets.add(13231262L);
    offsets.add(13491981L);
    offsets.add(13648981L);
    offsets.add(13962440L);
    offsets.add(13974742L);
    offsets.add(14631561L);
    offsets.add(14632730L);
    offsets.add(14687601L);
    offsets.add(14884670L);
    offsets.add(14886885L);
    offsets.add(14893944L);
    offsets.add(14927112L);
    offsets.add(15559969L);
    offsets.add(15687543L);
    offsets.add(15755151L);
    offsets.add(15769372L);
    offsets.add(16933180L);
    offsets.add(17107412L);
    offsets.add(17118135L);
    offsets.add(17128265L);
    offsets.add(17140614L);
    offsets.add(17143795L);
    offsets.add(17161313L);
    offsets.add(17204067L);
    offsets.add(17220367L);
    offsets.add(17238568L);
    offsets.add(17245593L);
    offsets.add(17257934L);
    offsets.add(17261599L);
    offsets.add(17265746L);
    offsets.add(17280121L);
    offsets.add(17283128L);
    offsets.add(17289457L);
    offsets.add(17306474L);
    offsets.add(17309896L);
    offsets.add(17315469L);
    offsets.add(17331980L);
    offsets.add(17335123L);
    offsets.add(17341551L);
    offsets.add(17345483L);
    offsets.add(17348071L);
    offsets.add(17351032L);
    offsets.add(17356376L);
    offsets.add(17358798L);
    offsets.add(17364950L);
    offsets.add(17370159L);
    offsets.add(17373763L);
    offsets.add(17375460L);
    offsets.add(17382260L);
    offsets.add(17386663L);
    offsets.add(17391935L);
    offsets.add(17407647L);
    offsets.add(17411955L);
    offsets.add(17432001L);
    offsets.add(17441853L);
    offsets.add(17459326L);
    offsets.add(17497379L);
    offsets.add(17514804L);
    offsets.add(17530223L);
    offsets.add(17539941L);
    offsets.add(17541827L);
    offsets.add(17543515L);
    offsets.add(17561082L);
    offsets.add(17579693L);
    offsets.add(17622588L);
    offsets.add(17643266L);
    offsets.add(17647329L);
    offsets.add(17655799L);
    offsets.add(17658815L);
    offsets.add(17668949L);
    offsets.add(17695193L);
    offsets.add(17699945L);
    offsets.add(17708266L);
    offsets.add(17713240L);
    offsets.add(17717851L);
    offsets.add(17738838L);
    offsets.add(17741442L);
    offsets.add(17747028L);
    offsets.add(17750976L);
    offsets.add(17754620L);
    offsets.add(17763293L);
    offsets.add(17782898L);
    offsets.add(17812724L);
    offsets.add(17816272L);
    offsets.add(17824144L);
    offsets.add(17833544L);
    offsets.add(17849107L);
    offsets.add(17852681L);
    offsets.add(17869123L);
    offsets.add(17894804L);
    offsets.add(17902523L);
    offsets.add(17904338L);
    offsets.add(17919416L);
    offsets.add(17930939L);
    offsets.add(17932426L);
    offsets.add(18049413L);
    offsets.add(18118363L);
    offsets.add(18652242L);
    offsets.add(18681439L);
    offsets.add(18685085L);
    offsets.add(18695983L);
    offsets.add(18706225L);
    offsets.add(18720171L);
    offsets.add(18739378L);
    offsets.add(18749949L);
    offsets.add(18813422L);
    offsets.add(18839185L);
    offsets.add(18985452L);
    offsets.add(18991305L);
    offsets.add(19101744L);
    offsets.add(19551284L);
    offsets.add(19570303L);
    offsets.add(19677342L);
    offsets.add(19798058L);
    offsets.add(19991177L);
    offsets.add(19995965L);
    offsets.add(20156208L);
    offsets.add(20214621L);
    offsets.add(20263800L);
    offsets.add(20269966L);
    offsets.add(20423726L);
    offsets.add(20455434L);
    offsets.add(20462104L);
    offsets.add(20516374L);
    offsets.add(20521107L);
    offsets.add(20523749L);
    offsets.add(20550270L);
    offsets.add(20576038L);
    offsets.add(2171770L);
    offsets.add(2348183L);
    offsets.add(2478864L);
    offsets.add(2521299L);
    offsets.add(2545750L);
    offsets.add(2561803L);
    offsets.add(2639035L);
    offsets.add(2657513L);
    offsets.add(2687264L);
    offsets.add(2697640L);
    offsets.add(2703387L);
    offsets.add(2732833L);
    offsets.add(2758692L);
    offsets.add(2760587L);
    offsets.add(2806469L);
    offsets.add(2892030L);
    offsets.add(2894889L);
    offsets.add(2914922L);
    offsets.add(2917060L);
    offsets.add(2954580L);
    offsets.add(2969066L);
    offsets.add(2983667L);
    offsets.add(2998370L);
    offsets.add(3023940L);
    offsets.add(3040252L);
    offsets.add(3064421L);
    offsets.add(3084439L);
    offsets.add(3097929L);
    offsets.add(3101604L);
    offsets.add(3120916L);
    offsets.add(3129791L);
    offsets.add(3132396L);
    offsets.add(3136496L);
    offsets.add(3143828L);
    offsets.add(3151570L);
    offsets.add(3160623L);
    offsets.add(3174364L);
    offsets.add(3191816L);
    offsets.add(3203093L);
    offsets.add(3205761L);
    offsets.add(3508786L);
    offsets.add(3528033L);
    offsets.add(3543147L);
    offsets.add(3547409L);
    offsets.add(3549997L);
    offsets.add(3579481L);
    offsets.add(3596993L);
    offsets.add(3606188L);
    offsets.add(10887862L);
    offsets.add(10898910L);
    offsets.add(10918149L);
    offsets.add(10922126L);
    offsets.add(10944213L);
    offsets.add(10959318L);
    offsets.add(10965365L);
    offsets.add(10971029L);
    offsets.add(10979750L);
    offsets.add(10981658L);
    offsets.add(10991722L);
    offsets.add(11013517L);
    offsets.add(11018689L);
    offsets.add(11038027L);
    offsets.add(11070441L);
    offsets.add(11088281L);
    offsets.add(11127466L);
    offsets.add(11158283L);
    offsets.add(11175778L);
    offsets.add(11188766L);
    offsets.add(11193329L);
    offsets.add(11202627L);
    offsets.add(11215656L);
    offsets.add(11230641L);
    offsets.add(17307864L);
    offsets.add(17314317L);
    offsets.add(17352447L);
    offsets.add(21021739L);
    offsets.add(21399201L);
    offsets.add(21413090L);
    offsets.add(21416095L);
    offsets.add(21454188L);
    offsets.add(21459460L);
    offsets.add(21467118L);
    offsets.add(21476461L);
    offsets.add(21480055L);
    offsets.add(21483046L);
    offsets.add(21518337L);
    offsets.add(21538548L);
    offsets.add(21543733L);
    offsets.add(21552309L);
    offsets.add(21558546L);
    offsets.add(21575077L);
    offsets.add(21584258L);
    offsets.add(21596760L);
    offsets.add(21603631L);
    offsets.add(22128151L);
    offsets.add(22144423L);
    offsets.add(22160170L);
    offsets.add(22222924L);
    offsets.add(22240158L);
    offsets.add(22266773L);
    offsets.add(22345017L);
    offsets.add(22363556L);
    offsets.add(22378546L);
    offsets.add(22387520L);
    offsets.add(22396474L);
    offsets.add(22416187L);
    offsets.add(22428138L);
    offsets.add(22513308L);
    offsets.add(22519391L);
    offsets.add(22524247L);
    offsets.add(22534396L);
    offsets.add(22538060L);
    offsets.add(22542577L);
    offsets.add(22569268L);
    offsets.add(22582397L);
    offsets.add(22592561L);
    offsets.add(22602920L);
    offsets.add(22626211L);
    offsets.add(22632420L);
    offsets.add(22681527L);
    offsets.add(22745412L);
    offsets.add(22751507L);
    offsets.add(22762091L);
    offsets.add(22768660L);
    offsets.add(22799940L);
    offsets.add(22814144L);
    offsets.add(22823710L);
    offsets.add(22834860L);
    offsets.add(22844514L);
    offsets.add(22849537L);
    offsets.add(22859979L);
    offsets.add(22862276L);
    offsets.add(23007243L);
    offsets.add(23026732L);
    offsets.add(23155045L);
    offsets.add(23191850L);
    offsets.add(23230463L);
    offsets.add(23295830L);
    offsets.add(23308233L);
    offsets.add(23698226L);
    offsets.add(23823775L);
    offsets.add(23863293L);
    offsets.add(23975571L);
    offsets.add(24023973L);
    offsets.add(24054828L);
    offsets.add(24059520L);
    offsets.add(24068374L);
    offsets.add(24083980L);
    offsets.add(24090672L);
    offsets.add(24104044L);
    offsets.add(24176074L);
    offsets.add(24190495L);
    offsets.add(24202029L);
    offsets.add(24229927L);
    offsets.add(24236287L);
    offsets.add(24250881L);
    offsets.add(24275467L);
    offsets.add(24284314L);
    offsets.add(24299494L);
    offsets.add(24339094L);
    offsets.add(24359878L);
    offsets.add(24372967L);
    offsets.add(24427631L);
    offsets.add(24480767L);
    offsets.add(24610162L);
    offsets.add(24620209L);
    offsets.add(24624421L);
    offsets.add(24643574L);
    offsets.add(24690581L);
    offsets.add(24696064L);
    offsets.add(24699500L);
    offsets.add(24713344L);
    offsets.add(24798064L);
    offsets.add(24823906L);
    offsets.add(24841345L);
    offsets.add(24856859L);
    offsets.add(24892271L);
    offsets.add(24964974L);
    offsets.add(25006141L);
    offsets.add(25037391L);
    offsets.add(25086066L);
    offsets.add(25090400L);
    offsets.add(25095759L);
    offsets.add(25149021L);
    offsets.add(25162230L);
    offsets.add(25241983L);
    offsets.add(25263975L);
    offsets.add(25340256L);
    offsets.add(25405453L);
    offsets.add(25412887L);
    offsets.add(25431798L);
    offsets.add(25439759L);
    offsets.add(25460254L);
    offsets.add(25522462L);
    offsets.add(25603379L);
    offsets.add(25610925L);
    offsets.add(25614672L);
    offsets.add(25632443L);
    offsets.add(25711426L);
    offsets.add(25728946L);
    offsets.add(25734873L);
    offsets.add(25749827L);
    offsets.add(25838709L);
    offsets.add(25895902L);
    offsets.add(25935368L);
    offsets.add(25957073L);
    offsets.add(26020578L);
    offsets.add(26030374L);
    offsets.add(26042236L);
    offsets.add(26060292L);
    offsets.add(26076674L);
    offsets.add(26085892L);
    offsets.add(26103356L);
    offsets.add(26124505L);
    offsets.add(26127895L);
    return offsets;
  }

}
