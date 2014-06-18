package org.icgc.dcc.reporter;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.util.Jackson.PRETTY_WRITTER;
import static org.icgc.dcc.reporter.Reporter.getHeadPipeName;

import java.io.File;
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

public class InputData {

  private final Table<String, FileType, Set<String>> data = HashBasedTable.create();

  // private final Map<String, Map<FileType, Set<String>>> d = newLinkedHashMap();

  /**
   * TODO: necessary?
   */
  public static InputData from(Map<String, Map<FileType, List<Path>>> matchingFiles) {
    val inputData = new InputData();
    for (val projectKey : matchingFiles.keySet()) {
      val projectFiles = matchingFiles.get(projectKey);
      for (val fileType : projectFiles.keySet()) {
        for (val path : projectFiles.get(fileType)) {
          inputData.addFile(projectKey, fileType, path.toUri().getPath()

              // TODO: improve when it's decided whether we go the NFS or HDFS route
              .replace("/hdfs/dcc", "")
              .replace("/nfs/dcc_secure/dcc/etl/icgc16/migration", "/tmp/migration"));
        }
      }
    }
    return inputData;
  }

  public Set<String> getProjectKeys() {
    return ImmutableSet.copyOf(data.rowKeySet());
    // return ImmutableSet.copyOf(d.keySet());
  }

  public Set<String> getMatchingFilePaths(String projectKey, FileType fileType) {
    return ImmutableSet.copyOf(firstNonNull(
        data.get(projectKey, fileType),
        // d.get(projectKey).get(fileType),
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

  public Map<String, String> getPipeNameToFilePath() {
    val pipeNameToFilePath = new ImmutableMap.Builder<String, String>();
    for (val projectKey : getProjectKeys()) {
      pipeNameToFilePath.putAll(getPipeNameToFilePath(projectKey, SPECIMEN_TYPE));
      pipeNameToFilePath.putAll(getPipeNameToFilePath(projectKey, SAMPLE_TYPE));

      for (val featureType : getFeatureTypesWithData(projectKey)) {
        pipeNameToFilePath.putAll(getPipeNameToFilePath(projectKey, featureType.getMetaFileType()));
        pipeNameToFilePath.putAll(getPipeNameToFilePath(projectKey, featureType.getPrimaryFileType()));
      }
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
    // return PRETTY_WRITTER.writeValueAsString(d);
  }

  private void addFile(String projectKey, FileType fileType, String path) {
    Set<String> paths = data.get(projectKey, fileType);
    if (paths == null) {
      paths = newLinkedHashSet();
      data.put(projectKey, fileType, paths);
    }
    paths.add(path);
    // Map<FileType, Set<String>> m = d.get(projectKey);
    // if (m == null) {
    // m = new LinkedHashMap<FileType, Set<String>>();
    // d.put(projectKey, m);
    // }
    // Set<String> paths = m.get(fileType);
    // if (paths == null) {
    // paths = newLinkedHashSet();
    // m.put(fileType, paths);
    // }
    // paths.add(path);
  }

  public static InputData getDummy() {
    InputData data;

    val dir = "/home/tony/Desktop/reports/1";
    val dir2 = "/home/tony/Desktop/reports/2";

    data = new InputData();
    data.addFile("project.1", FileType.SPECIMEN_TYPE, new File(dir, "specimen.txt").getPath());
    data.addFile("project.1", FileType.SAMPLE_TYPE, new File(dir, "sample.txt").getPath());
    data.addFile("project.1", FileType.CNSM_M_TYPE, new File(dir, "cnsm_m.txt").getPath());
    data.addFile("project.1", FileType.CNSM_P_TYPE, new File(dir, "cnsm_p.txt").getPath());
    data.addFile("project.1", FileType.CNSM_S_TYPE, new File(dir, "cnsm_s.txt").getPath());

    data.addFile("project.1", FileType.EXP_ARRAY_M_TYPE, new File(dir, "exp_array_m.txt").getPath());
    data.addFile("project.1", FileType.EXP_ARRAY_P_TYPE, new File(dir, "exp_array_p.txt").getPath());
    data.addFile("project.1", FileType.EXP_SEQ_M_TYPE, new File(dir, "exp_seq_m.txt").getPath());
    data.addFile("project.1", FileType.EXP_SEQ_P_TYPE, new File(dir, "exp_seq_p.txt").getPath());
    data.addFile("project.1", FileType.JCN_M_TYPE, new File(dir, "jcn_m.txt").getPath());
    data.addFile("project.1", FileType.JCN_P_TYPE, new File(dir, "jcn_p.txt").getPath());
    data.addFile("project.1", FileType.METH_ARRAY_M_TYPE, new File(dir, "meth_array_m.txt").getPath());
    data.addFile("project.1", FileType.METH_ARRAY_P_TYPE, new File(dir, "meth_array_p.txt").getPath());
    data.addFile("project.1", FileType.METH_SEQ_M_TYPE, new File(dir, "meth_seq_m.txt").getPath());
    data.addFile("project.1", FileType.METH_SEQ_P_TYPE, new File(dir, "meth_seq_p.txt").getPath());
    data.addFile("project.1", FileType.MIRNA_SEQ_M_TYPE, new File(dir, "mirna_seq_m.txt").getPath());
    data.addFile("project.1", FileType.MIRNA_SEQ_P_TYPE, new File(dir, "mirna_seq_p.txt").getPath());
    data.addFile("project.1", FileType.PEXP_M_TYPE, new File(dir, "pexp_m.txt").getPath());
    data.addFile("project.1", FileType.PEXP_P_TYPE, new File(dir, "pexp_p.txt").getPath());
    data.addFile("project.1", FileType.SGV_M_TYPE, new File(dir, "sgv_m.txt").getPath());
    data.addFile("project.1", FileType.SGV_P_TYPE, new File(dir, "sgv_p.txt").getPath());
    data.addFile("project.1", FileType.SSM_M_TYPE, new File(dir, "ssm_m.1.txt").getPath());
    data.addFile("project.1", FileType.SSM_M_TYPE, new File(dir, "ssm_m.2.txt").getPath());
    data.addFile("project.1", FileType.SSM_M_TYPE, new File(dir, "ssm_m.3.txt").getPath());
    data.addFile("project.1", FileType.SSM_P_TYPE, new File(dir, "ssm_p.1.txt").getPath());
    data.addFile("project.1", FileType.SSM_P_TYPE, new File(dir, "ssm_p.2.txt").getPath());
    data.addFile("project.1", FileType.SSM_P_TYPE, new File(dir, "ssm_p.3.txt").getPath());
    data.addFile("project.1", FileType.STSM_M_TYPE, new File(dir, "stsm_m.txt").getPath());
    data.addFile("project.1", FileType.STSM_P_TYPE, new File(dir, "stsm_p.txt").getPath());
    data.addFile("project.1", FileType.STSM_S_TYPE, new File(dir, "stsm_s.txt").getPath());

    data.addFile("project.2", FileType.SPECIMEN_TYPE, new File(dir2, "specimen.txt").getPath());
    data.addFile("project.2", FileType.SAMPLE_TYPE, new File(dir2, "sample.txt").getPath());
    data.addFile("project.2", FileType.CNSM_M_TYPE, new File(dir2, "cnsm_m.txt").getPath());
    data.addFile("project.2", FileType.CNSM_P_TYPE, new File(dir2, "cnsm_p.txt").getPath());
    data.addFile("project.2", FileType.CNSM_S_TYPE, new File(dir2, "cnsm_s.txt").getPath());
    // data.addFile("project.2", FileType.EXP_ARRAY_M_TYPE, new File(dir2, "exp_array_m.txt").getPath());
    // data.addFile("project.2", FileType.EXP_ARRAY_P_TYPE, new File(dir2, "exp_array_p.txt").getPath());
    data.addFile("project.2", FileType.EXP_SEQ_M_TYPE, new File(dir2, "exp_seq_m.txt").getPath());
    data.addFile("project.2", FileType.EXP_SEQ_P_TYPE, new File(dir2, "exp_seq_p.txt").getPath());
    data.addFile("project.2", FileType.JCN_M_TYPE, new File(dir2, "jcn_m.txt").getPath());
    data.addFile("project.2", FileType.JCN_P_TYPE, new File(dir2, "jcn_p.txt").getPath());
    // data.addFile("project.2", FileType.METH_ARRAY_M_TYPE, new File(dir2, "meth_array_m.txt").getPath());
    // data.addFile("project.2", FileType.METH_ARRAY_P_TYPE, new File(dir2, "meth_array_p.txt").getPath());
    data.addFile("project.2", FileType.METH_SEQ_M_TYPE, new File(dir2, "meth_seq_m.txt").getPath());
    data.addFile("project.2", FileType.METH_SEQ_P_TYPE, new File(dir2, "meth_seq_p.txt").getPath());
    data.addFile("project.2", FileType.MIRNA_SEQ_M_TYPE, new File(dir2, "mirna_seq_m.txt").getPath());
    data.addFile("project.2", FileType.MIRNA_SEQ_P_TYPE, new File(dir2, "mirna_seq_p.txt").getPath());
    // data.addFile("project.2", FileType.PEXP_M_TYPE, new File(dir2, "pexp_m.txt").getPath());
    // data.addFile("project.2", FileType.PEXP_P_TYPE, new File(dir2, "pexp_p.txt").getPath());
    data.addFile("project.2", FileType.SGV_M_TYPE, new File(dir2, "sgv_m.txt").getPath());
    data.addFile("project.2", FileType.SGV_P_TYPE, new File(dir2, "sgv_p.txt").getPath());
    data.addFile("project.2", FileType.SSM_M_TYPE, new File(dir2, "ssm_m.1.txt").getPath());
    data.addFile("project.2", FileType.SSM_M_TYPE, new File(dir2, "ssm_m.2.txt").getPath());
    data.addFile("project.2", FileType.SSM_M_TYPE, new File(dir2, "ssm_m.3.txt").getPath());
    data.addFile("project.2", FileType.SSM_P_TYPE, new File(dir2, "ssm_p.1.txt").getPath());
    data.addFile("project.2", FileType.SSM_P_TYPE, new File(dir2, "ssm_p.2.txt").getPath());
    data.addFile("project.2", FileType.SSM_P_TYPE, new File(dir2, "ssm_p.3.txt").getPath());
    data.addFile("project.2", FileType.STSM_M_TYPE, new File(dir2, "stsm_m.txt").getPath());
    data.addFile("project.2", FileType.STSM_P_TYPE, new File(dir2, "stsm_p.txt").getPath());
    data.addFile("project.2", FileType.STSM_S_TYPE, new File(dir2, "stsm_s.txt").getPath());

    return data;
  }
}
