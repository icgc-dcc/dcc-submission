package org.icgc.dcc.submission.ega.metadata.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.icgc.dcc.submission.ega.metadata.config.EGAMetadataConfig;
import org.icgc.dcc.submission.ega.metadata.service.EGAMetadataService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

@Service
@RequiredArgsConstructor
public class EGAMetadataServiceImpl implements EGAMetadataService {

  @NonNull
  EGAMetadataConfig.EGAMetadataPostgresqlConfig config;

  @Override
  public List<ObjectNode> getData() {

    String jdbcUrl = "jdbc:postgresql://" + config.getHost() + "/" + config.getDatabase() + "?user=" + config.getUser() + "&password=" + config.getPassword();
    String sql = "select * from " + config.getViewName() + ";";
    JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(jdbcUrl));

    List<ObjectNode> data = new ArrayList<>();

    jdbcTemplate.query(sql, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {

        ObjectMapper mapper = new ObjectMapper();

        while(resultSet.next()){
          String sample_id = resultSet.getString(1);
          String file_id = resultSet.getString(2);
          ObjectNode node = mapper.createObjectNode();
          node.put("submitterSampleId", sample_id);
          node.put("fileId", file_id);
          data.add(node);
        }
      }
    });

    return data;
  }
}
