package org.icgc.dcc.reporter;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static org.icgc.dcc.core.util.Splitters.TAB;
import static org.icgc.dcc.core.util.Strings2.EMPTY_STRING;

import java.io.File;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.util.Jackson;
import org.icgc.dcc.core.util.Separators;
import org.icgc.dcc.reporter.presentation.DataTypeCountsReportTable;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.io.Files;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObjectBuilder;

@Slf4j
public class ReporterGatherer {

  private static final Optional<Map<String, String>> ABSENT_MAPPING = Optional.<Map<String, String>> absent();

  public static String getTsvTable1(
      @NonNull final Set<String> projectKeys) {

    val sb = new StringBuilder();
    boolean firstProject = true;
    for (val projectKey : projectKeys) {
      sb.append(getTsvTable(OutputType.DONOR, projectKey, ABSENT_MAPPING, firstProject));
      firstProject = false;
    }

    return sb.toString();
  }

  public static String getTsvTable2(
      @NonNull final Set<String> projectKeys,
      @NonNull final Map<String, String> mapping) {

    val sb = new StringBuilder();
    boolean firstProject = true;
    for (val projectKey : projectKeys) {
      sb.append(getTsvTable(OutputType.SEQUENCING_STRATEGY, projectKey, Optional.of(mapping), firstProject));
      firstProject = false;
    }

    return sb.toString();
  }

  private static String getTsvTable(
      OutputType outputType,
      final java.lang.String projectKey,
      Optional<Map<String, String>> mapping,
      boolean printHeader) {
    val outputFilePath = Reporter.getOuputFileFusePath(outputType, projectKey);
    val headerLine = readFirstLine(outputFilePath);
    val sb = new StringBuilder();
    if (printHeader) {
      val headers = newArrayList(TAB.split(headerLine));
      val headerSize = headers.size();
      for (int i = 0; i < headerSize; i++) {
        sb.append(i == 0 ?
            EMPTY_STRING : Separators.TAB);
        sb.append(
            getHeader(
                headers.get(i),
                mapping));
      }
      sb.append(Separators.NEWLINE);
    }
    for (val line : readRemainingLines(outputFilePath)) {
      sb.append(line + Separators.NEWLINE);
    }

    return sb.toString();
  }

  public static ArrayNode getJsonTable1(
      @NonNull final String projectKey) {

    return getJsonTable(projectKey, OutputType.DONOR, ABSENT_MAPPING);
  }

  public static ArrayNode getJsonTable2(
      @NonNull final String projectKey,
      @NonNull final Map<String, String> mapping) {

    return getJsonTable(projectKey, OutputType.SEQUENCING_STRATEGY, Optional.of(mapping));
  }

  private static ArrayNode getJsonTable(final String projectKey, OutputType outputType, Optional<Map<String, String>> mapping) {
    val outputFilePath = Reporter.getOuputFileFusePath(outputType, projectKey);
    val headerLine = readFirstLine(outputFilePath);
    val headers = newArrayList(TAB.split(headerLine));
    val headerSize = headers.size();

    val documents = JsonNodeFactory.instance.arrayNode();
    for (val line : readRemainingLines(outputFilePath)) {
      val values = newArrayList(TAB.split(line));
      checkState(headerSize == values.size());

      //val builder = new BasicDBObjectBuilder();
      val node = JsonNodeFactory.instance.objectNode();
      for (int i = 0; i < headerSize; i++) {
    	  node.put(getHeader(headers.get(i), mapping), values.get(i));
      }
      documents.add(node);
    }
    log.info("Content for '{}': '{}'", projectKey, Jackson.formatPrettyJson(documents));
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

  @SneakyThrows
  public static void writeCsvFile(DataTypeCountsReportTable table) {
    Files.write(
        table.getCsvRepresentation().getBytes(),
        new File("TODO"));
  }

}
