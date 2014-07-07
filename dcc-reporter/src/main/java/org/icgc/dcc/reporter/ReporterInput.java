package org.icgc.dcc.reporter;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.util.Jackson.PRETTY_WRITTER;
import static org.icgc.dcc.reporter.Reporter.getHeadPipeName;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;

import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

public class ReporterInput {

  private final Table<String, FileType, Set<String>> data = HashBasedTable.create();

  /**
   * TODO: necessary?
   */
  public static ReporterInput from(Map<String, Map<FileType, List<Path>>> matchingFiles) {
    val inputData = new ReporterInput();
    for (val projectKey : matchingFiles.keySet()) {
      val projectFiles = matchingFiles.get(projectKey);
      for (val fileType : projectFiles.keySet()) {
        for (val path : projectFiles.get(fileType)) {
          inputData.addFile(projectKey, fileType, path.toUri().getPath()

              // TODO: improve when it's decided whether we go the NFS or HDFS route
              .replace("/hdfs/dcc", "")
              .replace("/nfs/dcc_secure/dcc/etl/icgc16/migration", "/icgc/dcc_root_dir/ICGC16/migration"));
        }
      }
    }
    return inputData;
  }

  public Set<String> getProjectKeys() {
    return ImmutableSet.copyOf(data.rowKeySet());
  }

  public Set<String> getMatchingFilePaths(String projectKey, FileType fileType) {
    return ImmutableSet.copyOf(firstNonNull(
        data.get(projectKey, fileType),
        ImmutableSet.<String> of()));
  }

  public boolean hasFeatureType(String projectKey, FeatureType featureType) {
    return !getMatchingFilePaths(projectKey, featureType.getDataTypePresenceIndicator()).isEmpty();
  }

  public Iterable<FeatureType> getFeatureTypesWithData(final String projectKey) {
    return filter(
        asList(FeatureType.values()),
        new Predicate<FeatureType>() {

          @Override
          public boolean apply(FeatureType featureType) {
            return hasFeatureType(projectKey, featureType);
          }

        });
  }

  public Map<String, String> getPipeNameToFilePath(
      final java.lang.String projectKey) {
    val pipeNameToFilePath = new ImmutableMap.Builder<String, String>();

    pipeNameToFilePath.putAll(getPipeNameToFilePath(projectKey, SPECIMEN_TYPE));
    pipeNameToFilePath.putAll(getPipeNameToFilePath(projectKey, SAMPLE_TYPE));

    for (val featureType : getFeatureTypesWithData(projectKey)) {
      pipeNameToFilePath.putAll(getPipeNameToFilePath(projectKey, featureType.getMetaFileType()));
      pipeNameToFilePath.putAll(getPipeNameToFilePath(projectKey, featureType.getPrimaryFileType()));
    }

    return pipeNameToFilePath.build();
  }

  private Map<String, String> getPipeNameToFilePath(final String projectKey, final FileType fileType) {
    val pipeNameToFilePath = new ImmutableMap.Builder<String, String>();
    int fileNumber = 0;
    for (val matchingFilePath : getMatchingFilePaths(projectKey, fileType)) {
      pipeNameToFilePath.put(
          getHeadPipeName(projectKey, fileType, fileNumber++),
          matchingFilePath);
    }

    return pipeNameToFilePath.build();
  }

  @Override
  @SneakyThrows
  public String toString() {
    return PRETTY_WRITTER.writeValueAsString(data);
  }

  private void addFile(String projectKey, FileType fileType, String path) {
    Set<String> paths = data.get(projectKey, fileType);
    if (paths == null) {
      paths = newLinkedHashSet();
      data.put(projectKey, fileType, paths);
    }
    paths.add(path);
  }

}
