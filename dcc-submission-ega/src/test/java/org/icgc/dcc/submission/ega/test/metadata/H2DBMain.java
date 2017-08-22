package org.icgc.dcc.submission.ega.test.metadata;

import org.apache.commons.io.FileUtils;
import org.h2.tools.Server;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class H2DBMain {

  public static void main(String[] args) {
    Server server = null;
    try {

      FileUtils.deleteDirectory(new File("/tmp/dcc/submission/ega/h2"));

      server = Server.createPgServer("-baseDir", "/tmp/dcc/submission/ega/h2");

      server.start();

      System.out.println(server.getPort());
      System.out.println(server.getURL());

      JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource("jdbc:postgresql://localhost:5435/ICGC_Metadata?user=sa&password="));
      jdbcTemplate.execute("create schema if not exists ega");

      jdbcTemplate.execute("create table if not exists ega.ega_sample_mapping (sample_id varchar(64), file_id varchar(64), primary key(sample_id, file_id));");

      int ret = jdbcTemplate.update("insert into ega.ega_sample_mapping values(?, ?);", "sample_id_1", "file_id_1");
      System.out.println(ret);

      List<Map<String, Object>> out = jdbcTemplate.queryForList("select * from ega.ega_sample_mapping;");
      System.out.println("result set is " + out.size());

      out.stream().forEach(map -> {
        Set<String> keys = map.keySet();
        keys.stream().forEach(key -> {
          System.out.println(key + ":" + map.get(key).toString());
        });
      });


    } catch (SQLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {

      if (server != null){
        server.stop();
        server.shutdown();
      }
    }
  }
}
