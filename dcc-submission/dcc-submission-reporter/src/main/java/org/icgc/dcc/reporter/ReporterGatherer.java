package org.icgc.dcc.reporter;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static org.icgc.dcc.core.util.Optionals.ABSENT_STRING_MAP;
import static org.icgc.dcc.core.util.Splitters.TAB;

import java.io.File;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.io.Files;

public class ReporterGatherer {

  public static ArrayNode getJsonTable1(
      @NonNull final String outputDirPath,
      @NonNull final String projectKey) {

    return getJsonTable(outputDirPath, projectKey, OutputType.DONOR, ABSENT_STRING_MAP);
  }

  public static ArrayNode getJsonTable2(
      @NonNull final String outputDirPath,
      @NonNull final String projectKey,
      @NonNull final Map<String, String> mapping) {

    return getJsonTable(outputDirPath, projectKey, OutputType.SEQUENCING_STRATEGY, Optional.of(mapping));
  }

  private static ArrayNode getJsonTable(
      @NonNull final String outputDirPath,
      @NonNull final String projectKey,
      @NonNull final OutputType outputType,
      @NonNull final Optional<Map<String, String>> mapping) {
    val outputFilePath = Reporter.getOutputFilePath(outputDirPath, outputType, projectKey);
    val headerLine = readFirstLine(outputFilePath);
    val headers = newArrayList(TAB.split(headerLine));
    val headerSize = headers.size();

    val documents = JsonNodeFactory.instance.arrayNode();
    for (val line : readRemainingLines(outputFilePath)) {
      val values = newArrayList(TAB.split(line));
      checkState(headerSize == values.size());

      val node = JsonNodeFactory.instance.objectNode();
      for (int i = 0; i < headerSize; i++) {
        node.put(getHeader(headers.get(i), mapping), values.get(i));
      }
      documents.add(node);
    }

    return documents;
  }

  private static String getHeader(String header, Optional<Map<String, String>> mapping) {
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

  @SneakyThrows
  private static String readFirstLine(String filePath) {
    return Files.readFirstLine(
        new File(filePath),
        UTF_8);
  }

  @SneakyThrows
  private static Iterable<String> readRemainingLines(String filePath) {
    return filter(readLines(
        new File(filePath),
        UTF_8),
        new Predicate<String>() {

          int lineNumber;

          @Override
          public boolean apply(String line) {
            return lineNumber++ != 0;
          }

        });
  }

}
