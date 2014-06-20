package org.icgc.dcc.reporter.presentation;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Sets.newTreeSet;
import static org.apache.commons.lang.WordUtils.capitalize;
import static org.icgc.dcc.core.util.Joiners.COMMA;
import static org.icgc.dcc.core.util.Joiners.NEWLINE;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.DataType.DataTypes;
import org.icgc.dcc.reporter.OutputType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

@Slf4j
public class DataTypeCountsReportTable implements CsvRepresentable {

  private static final String CLINICAL_TYPE_DISPLAY_NAME = "Clinical";

  private final Table<String, DataType, DataTypeCountsReportRow> table = HashBasedTable
      .<String, DataType, DataTypeCountsReportRow> create();

  public DataTypeCountsReportTable(Set<String> projectKeys) {
    for (val projectKey : projectKeys) {
      for (val dataType : filter(
          DataTypes.values(),
          new Predicate<DataType>() {

            @Override
            public boolean apply(DataType dataType) {
              return !isOptionalClinicalType(dataType);
            }

          })) {
        table.put(projectKey, dataType, DataTypeCountsReportRow.getEmptyInstance());
      }
    }
  }

  public void updateCount(OutputType output, String projectId, DataType dataType, long count) {
    log.info("TODO: {}, {}, {}", new Object[] { projectId, dataType, table.rowKeySet() });
    val dataTypeCountsReportRow = table.get(projectId, dataType);
    switch (output) {
    case DONOR:
      dataTypeCountsReportRow.setDonorCount(count);
      break;
    case SPECIMEN:
      dataTypeCountsReportRow.setSpecimenCount(count);
      break;
    case SAMPLE:
      dataTypeCountsReportRow.setSampleCount(count);
      break;
    case OBSERVATION:
      val optionalObservationCount = dataType.isClinicalType() ? Optional.<Long> absent() : Optional.of(count);
      dataTypeCountsReportRow.setObservationCount(optionalObservationCount);
      break;
    default:
      checkState(false, "TODO");
      break;
    }
  }

  @Override
  public String getCsvRepresentation() {
    val lines = Lists.newArrayList();

    // Add header line
    lines.add(DataTypeCountsReportRow.getCsvHeaderLine());

    // Add data lines
    for (val projectId : newTreeSet(table.rowKeySet())) {
      val row = table.row(projectId);

      Set<DataType> keySet = new TreeSet<DataType>(new Comparator<DataType>() {

        @Override
        public int compare(DataType dataType1, DataType dataType2) {
          checkState(!dataType1.isClinicalType() || !dataType1.asClinicalType().isOptionalClinicalType(), "TODO");
          checkState(!dataType2.isClinicalType() || !dataType2.asClinicalType().isOptionalClinicalType(), "TODO");
          if (dataType1.isClinicalType()) {
            return -1;
          } else if (dataType2.isClinicalType()) {
            return 1;
          } else {
            return dataType1.name().compareTo(dataType2.name());
          }
        }

      });

      keySet.addAll(row.keySet());

      for (val dataType : keySet) {
        checkState(!isOptionalClinicalType(dataType), "TODO");
        lines.add(
            COMMA.join(
                projectId,
                dataType.isClinicalType() ?
                    CLINICAL_TYPE_DISPLAY_NAME :
                    dataType.getTypeName().toUpperCase(),
                row.get(dataType)
                    .getCsvRepresentation()));
      }
    }
    lines.add(""); // Add a trailing newline

    return NEWLINE.join(lines);
  }

  private boolean isOptionalClinicalType(DataType dataType) {
    return dataType.isClinicalType()
        && dataType.asClinicalType().isOptionalClinicalType();
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  @lombok.Data
  static class DataTypeCountsReportRow implements CsvRepresentable {

    private static final String NOT_APPLICABLE = "N/A";

    enum HeaderField {
      PROJECT, DATA_TYPE, DONORS, SPECIMENS, SAMPLES, OBSERVATIONS;

      private String getDisplayName() {
        return capitalize(name().replace('_', ' ').toLowerCase());
      }

      public static List<String> getDisplayNames() {
        val displayNames = new ImmutableList.Builder<String>();
        for (val header : values()) {
          displayNames.add(header.getDisplayName());
        }
        return displayNames.build();
      }

    }

    private long donorCount;
    private long specimenCount;
    private long sampleCount;
    private Optional<Long> observationCount = Optional.absent(); // TODO: remove

    public static DataTypeCountsReportRow getEmptyInstance() {
      return new DataTypeCountsReportRow();
    }

    public static String getCsvHeaderLine() {
      return COMMA.join(HeaderField.getDisplayNames());
    }

    @Override
    public String getCsvRepresentation() {
      return COMMA.join(
          donorCount,
          specimenCount,
          sampleCount,
          observationCount.isPresent() ?
              observationCount.get() :
              NOT_APPLICABLE);
    }

  }
}