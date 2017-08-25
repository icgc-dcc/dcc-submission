package org.icgc.dcc.submission.ega.metadata.extractor.impl;

import com.google.common.base.Splitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.extractor.DataExtractor;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
  @Override
  public List<Pair<String, String>> extract(File file) {

    try( BufferedReader br = new BufferedReader(new FileReader(file)) ) {
      List<Pair<String, String>> buffer = new ArrayList<>();
      String line;
      while((line = br.readLine()) != null){
        List<String> fields = Splitter.on('\t').trimResults().omitEmptyStrings().splitToList(line);
        buffer.add(Pair.of(fields.get(0), fields.get(3)));
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
