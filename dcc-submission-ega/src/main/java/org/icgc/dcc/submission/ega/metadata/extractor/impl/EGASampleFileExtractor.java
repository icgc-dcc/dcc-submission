package org.icgc.dcc.submission.ega.metadata.extractor.impl;

import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.extractor.BadFormattedDataLogger;
import org.icgc.dcc.submission.ega.metadata.extractor.DataExtractor;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright (c) 2017 The Ontario Institute for Cancer Research. All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * <p>
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
@Slf4j
public class EGASampleFileExtractor implements DataExtractor<Pair<String, String>> {

  private BadFormattedDataLogger badFormattedDataLogger;

  private Pattern pattern4Filename = Pattern.compile(".+/(EGA.\\d+)/.+");

  public EGASampleFileExtractor(){
    badFormattedDataLogger = null;
  }

  public EGASampleFileExtractor(BadFormattedDataLogger logger) {
    this.badFormattedDataLogger = logger;
  }

  @Override
  public List<Pair<String, String>> extract(File file) {

    Matcher matcher = pattern4Filename.matcher(file.getAbsolutePath());
    matcher.matches();
    String filename = matcher.group(1);

    log.info("Extracting the sample data out of file: " + filename + "/.../Sample_File.map");
    try( BufferedReader br = new BufferedReader(new FileReader(file)) ) {
      List<Pair<String, String>> buffer = new ArrayList<>();
      List<BadFormattedDataLogger.BadFormattedData> badData = new ArrayList<>();
      String line;
      int lineNo = -1;
      while((line = br.readLine()) != null){
        lineNo++;
        List<String> fields = Splitter.on('\t').trimResults().omitEmptyStrings().splitToList(line);
        if(fields.size() == 4)
          buffer.add(Pair.of(fields.get(0), fields.get(3)));
        else {
          if(badFormattedDataLogger != null) {
            badData.add(new BadFormattedDataLogger.BadFormattedData(
                filename,
                line,
                lineNo,
                LocalDateTime.now(ZoneId.of("America/Toronto")).atZone(ZoneId.of("America/Toronto")).toEpochSecond()
            ));
          }
        }
      }
      if(!badData.isEmpty()){
        badFormattedDataLogger.log(badData);
      }
      return buffer;

    } catch (FileNotFoundException e) {
      log.warn(e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      log.warn(e.getMessage());
      e.printStackTrace();
    }

    return Collections.<Pair<String, String>>emptyList();
  }
  
}
