package org.icgc.dcc.submission.reporter;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.submission.reporter.Reporter.getHeadPipeName;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.core.util.Functions2;

import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

@ToString
public class ReporterInput {

  private final Table<String, FileType, Set<String>> data = HashBasedTable.create();

  public static ReporterInput from(Map<String, Map<FileType, List<Path>>> matchingFiles) {
    val inputData = new ReporterInput();
    for (val projectKey : matchingFiles.keySet()) {
      val projectFiles = matchingFiles.get(projectKey);
      for (val fileType : projectFiles.keySet()) {
        for (val path : projectFiles.get(fileType)) {
          inputData.addFile(projectKey, fileType, path.toUri().getPath());
        }
      }
    }

    return inputData;
  }

  public Set<String> getProjectKeys() {
    return ImmutableSet.copyOf(data.rowKeySet());
  }

  public Set<String> getMatchingFilePaths(
      @NonNull final String projectKey,
      @NonNull final FileType fileType) {
    return ImmutableSet.copyOf(firstNonNull(
        getMatchingFilePaths(projectKey).get(fileType),
        ImmutableSet.<String> of()));
  }

  public Table<String, FileType, Integer> getMatchingFilePathCounts() {
    return Tables.transformValues(
        data,
        Functions2.<String> size());
  }

  public Map<FileType, Set<String>> getMatchingFilePaths(@NonNull final String projectKey) {
    return ImmutableMap.copyOf(firstNonNull(
        data.row(projectKey),
        ImmutableMap.<FileType, Set<String>> of()));
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

  public Map<String, String> getPipeNameToFilePath(@NonNull final String projectKey) {
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

  private void addFile(String projectKey, FileType fileType, String path) {
    Set<String> paths = data.get(projectKey, fileType);
    if (paths == null) {
      paths = newLinkedHashSet();
      data.put(projectKey, fileType, paths);
    }

    paths.add(path);
  }

}
