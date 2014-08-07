package org.icgc.dcc.reporter;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.core.util.Optionals.ABSENT_STRING_MAP;
import static org.icgc.dcc.core.util.Splitters.TAB;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.readSmallTextFile;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Optional;

@Slf4j
public class ReporterGatherer {

  public static ArrayNode getJsonTable1(
      @NonNull final FileSystem fileSystem,
      @NonNull final String outputDirPath,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    return getJsonTable(
        fileSystem, outputDirPath, releaseName, projectKey, OutputType.DONOR, ABSENT_STRING_MAP);
  }

  public static ArrayNode getJsonTable2(
      @NonNull final FileSystem fileSystem,
      @NonNull final String outputDirPath,
      @NonNull final String releaseName,
      @NonNull final String projectKey,
      @NonNull final Map<String, String> mapping) {
    return getJsonTable(
        fileSystem, outputDirPath, releaseName, projectKey, OutputType.SEQUENCING_STRATEGY, Optional.of(mapping));
  }

  private static ArrayNode getJsonTable(
      @NonNull final FileSystem fileSystem,
      @NonNull final String outputDirPath,
      @NonNull final String releaseName,
      @NonNull final String projectKey,
      @NonNull final OutputType outputType,
      @NonNull final Optional<Map<String, String>> mapping) {

    val outputFilePath = Reporter.getOutputFilePath(outputDirPath, outputType, releaseName, projectKey);
    val iterator = readSmallTextFile(fileSystem, new Path(outputFilePath)).iterator();
    val headerLine = iterator.next();
    val headers = getTsvHeaders(headerLine);
    log.info("Headers: '{}'", headers);
    val headerSize = headers.size();

    val documents = JsonNodeFactory.instance.arrayNode();
    while (iterator.hasNext()) {
      String line = iterator.next();
      if (!line.equals(headerLine)) {
        val values = newArrayList(TAB.split(line));
        checkState(headerSize == values.size());

        val node = JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < headerSize; i++) {
          node.put(getJsonHeader(headers.get(i), mapping), values.get(i));
        }
        documents.add(node);
      }
    }

    return documents;
  }

  private static List<String> getTsvHeaders(String headerLine) {
    return newArrayList(TAB.split(headerLine));
  }

  private static String getJsonHeader(String header, Optional<Map<String, String>> mapping) {
    return mapping.isPresent() ?
        tryTranslate(
            mapping.get(),
            header) :
        header;
  }

  private static String tryTranslate(
      @NonNull final Map<String, String> mapping,
      @NonNull final String code) {

    return mapping.containsKey(code) ?
        mapping.get(code) : code;
  }

}
