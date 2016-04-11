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
package org.icgc.dcc.submission.loader.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.icgc.dcc.common.core.meta.Resolver.DictionaryResolver;
import org.icgc.dcc.common.core.meta.RestfulCodeListsResolver;
import org.icgc.dcc.common.core.meta.RestfulDictionaryResolver;
import org.icgc.dcc.submission.loader.cli.ClientOptions;
import org.icgc.dcc.submission.loader.db.orientdb.OrientdbDatabseService;
import org.icgc.dcc.submission.loader.db.orientdb.OrientdbDocumentLinker;
import org.icgc.dcc.submission.loader.db.postgres.PostgresDatabaseService;
import org.icgc.dcc.submission.loader.io.LoadFilesResolver;
import org.icgc.dcc.submission.loader.meta.CodeListValuesDecoder;
import org.icgc.dcc.submission.loader.meta.CodeListsService;
import org.icgc.dcc.submission.loader.meta.ReleaseResolver;
import org.icgc.dcc.submission.loader.meta.SubmissionMetadataService;
import org.icgc.dcc.submission.loader.meta.TypeDefGraph;
import org.postgresql.ds.PGPoolingDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor(access = PRIVATE)
public final class DependencyFactory implements Closeable {

  private static DependencyFactory instance;

  @Getter(lazy = true)
  private final FileSystem fileSystem = createFileSystem();
  @Getter(lazy = true)
  private final OPartitionedDatabasePool dbPool = createDbPool();
  @Getter(lazy = true)
  private final DictionaryResolver dictionaryResolver = createDictionaryResolver();
  @Getter(lazy = true)
  private final CodeListsService codeListsService = createCodeListsService();
  @Getter(lazy = true)
  private final Configuration hadoopConfig = createConfiguration();
  @Getter(lazy = true)
  private final CompressionCodecFactory compressionCodecFactory = createCompressionCodecFactory();
  @Getter(lazy = true)
  private final ExecutorService executor = createExecutor();
  @Getter(lazy = true)
  private final PGPoolingDataSource dataSource = createDataSource();
  @Getter(lazy = true)
  private final ReleaseResolver releaseResolver = createReleaseResolver();
  private final Map<String, SubmissionMetadataService> releaseSubmissionService = Maps.newHashMap();

  private final ClientOptions options;

  @Override
  @SneakyThrows
  public void close() throws IOException {
    getFileSystem().close();
    getExecutor().shutdown();
    closeDbPool();
  }

  public CompletionService<Void> createCompletionService() {
    return new ExecutorCompletionService<Void>(getExecutor());
  }

  public OrientdbDocumentLinker createDocumentLinker(ODatabaseDocumentTx db, String release) {
    return new OrientdbDocumentLinker(createSubmissionMetadataService(release), db);
  }

  public OrientdbDatabseService getDatabaseService(ODatabaseDocumentTx db, String release) {
    return new OrientdbDatabseService(db, createSubmissionMetadataService(release));
  }

  public CodeListValuesDecoder createCodeListValuesDecoder(@NonNull String release, @NonNull String fileType) {
    val submissionSystem = createSubmissionMetadataService(release);
    val fieldNamecodeListName = submissionSystem.getFieldNameCodeListName(fileType);
    val fieldCodeLists = createFieldCodeLists(fieldNamecodeListName);

    return new CodeListValuesDecoder(fieldCodeLists);
  }

  private Map<String, Map<String, String>> createFieldCodeLists(Map<String, String> fieldNamecodeListName) {
    val codeListsService = getCodeListsService();

    return fieldNamecodeListName.entrySet().stream()
        .collect(toImmutableMap(e -> e.getKey(), e -> codeListsService.getCodeLists(e.getValue())));
  }

  public static void initialize(ClientOptions options) {
    instance = new DependencyFactory(options);
  }

  public static DependencyFactory getInstance() {
    checkNotNull(instance, "DependencyFactory is not initialized");

    return instance;
  }

  public static LoadFilesResolver createLoadFilesResolver(@NonNull String submissionDirPath, String release) {
    val dependencyFactory = getInstance();
    val fs = dependencyFactory.getFileSystem();
    val submissionService = dependencyFactory.createSubmissionMetadataService(release);
    val submissionDir = new Path(submissionDirPath);

    return new LoadFilesResolver(release, submissionDir, fs, submissionService);
  }

  public static ODatabaseDocumentTx connect() {
    return DependencyFactory.getInstance().getDbPool().acquire();
  }

  private ReleaseResolver createReleaseResolver() {
    return new ReleaseResolver(options.submissionUrl, options.submissionUser, options.submissionPassword);
  }

  private ExecutorService createExecutor() {
    return Executors.newFixedThreadPool(options.nThreads);
  }

  public PostgresDatabaseService createPostgresDatabaseService(@NonNull String release) {
    val submissionService = createSubmissionMetadataService(release);
    val graph = new TypeDefGraph(submissionService.getFileTypes());

    return new PostgresDatabaseService(submissionService, new JdbcTemplate(getDataSource()), graph);
  }

  private PGPoolingDataSource createDataSource() {
    val dataSource = new PGPoolingDataSource();
    dataSource.setServerName(options.dbHost);

    if (options.dbPort != null) {
      dataSource.setPortNumber(Integer.parseInt(options.dbPort));
    }

    dataSource.setDatabaseName(options.dbName);

    if (options.dbUser != null) {
      dataSource.setUser(options.dbUser);
    }

    if (options.dbPassword != null) {
      dataSource.setPassword(options.dbPassword);
    }

    return dataSource;
  }

  private CompressionCodecFactory createCompressionCodecFactory() {
    return new CompressionCodecFactory(getHadoopConfig());
  }

  private SubmissionMetadataService createSubmissionMetadataService(String release) {
    SubmissionMetadataService submissionService = releaseSubmissionService.get(release);
    if (submissionService == null) {
      val dictVersion = getReleaseResolver().getDictionaryVersion(release);
      submissionService = new SubmissionMetadataService(getDictionaryResolver(), dictVersion);
      releaseSubmissionService.put(release, submissionService);
    }

    return submissionService;
  }

  @SneakyThrows
  private FileSystem createFileSystem() {
    return FileSystem.get(getHadoopConfig());
  }

  private OPartitionedDatabasePool createDbPool() {
    val url = createOrientdbUrl();

    return new OPartitionedDatabasePool(url, options.dbUser, options.dbPassword);
  }

  private String createOrientdbUrl() {
    val url = new StringBuilder();
    url.append(options.dbHost);
    if (options.dbName != null) {
      url.append("/" + options.dbName);
    }

    return url.toString();
  }

  private DictionaryResolver createDictionaryResolver() {
    return new RestfulDictionaryResolver(options.submissionUrl + "/ws");
  }

  private CodeListsService createCodeListsService() {
    return new CodeListsService(new RestfulCodeListsResolver(options.submissionUrl + "/ws"));
  }

  private Configuration createConfiguration() {
    val configuration = new Configuration();
    configuration.set("fs.defaultFS", options.fsUrl);

    return configuration;
  }

  private void closeDbPool() {
    switch (options.dbType) {
    case ORIENTDB:
      getDbPool().close();
      break;
    case POSTGRES:
      getDataSource().close();
      break;
    default:
      throw new IllegalArgumentException(format("Unsupported database %s", options.dbType));
    }
  }

}
