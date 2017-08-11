package org.icgc.dcc.submission.ega.test.metadata;

import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.repo.EGAMetadataRepo;
import org.icgc.dcc.submission.ega.metadata.repo.impl.EGAMetadataRepoPostgres;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import rx.Observable;

import java.util.*;

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

public class EGAMetadataRepoPostgresTest extends EGAMetadataResourcesProvider {

  @Test
  public void test_persist() {

    EGAMetadataConfig.EGAMetadataPostgresqlConfig config = new EGAMetadataConfig.EGAMetadataPostgresqlConfig();
    config.setHost("localhost:5435");
    config.setDatabase("ICGC_metadata");
    config.setUser("sa");
    config.setPassword("");
    config.setViewName("view_ega_sample_mapping");

    EGAMetadataRepo repo = new EGAMetadataRepoPostgres(config);

    List<Pair<String, String>> data = new ArrayList<>();
    Set<String> keySet = new HashSet<>();
    Set<String> valueSet = new HashSet<>();

    for(int i = 0;i<100;i++){
      data.add(Pair.of("sample_id_" + i, "file_id_"+i));
      data.add(Pair.of("sample_id_" + i, "file_id_"+i + "" + i));
      keySet.add("sample_id_" +i);
      valueSet.add("file_id_" + i);
      valueSet.add("file_id_" + i + "" + i);
    }

    repo.persist(Observable.just(data));

    JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://localhost:5435/ICGC_metadata?user=sa&password="));

    List<Map<String, Object>> ret = jdbcTemplate.queryForList("select * from view_ega_sample_mapping;");

    Assert.assertEquals(200, ret.size());


    ret.stream().forEach(map -> {

      Assert.assertTrue( keySet.contains(map.get("sample_id")) );
      Assert.assertTrue( valueSet.contains(map.get("file_id")) );

    });


  }
}
