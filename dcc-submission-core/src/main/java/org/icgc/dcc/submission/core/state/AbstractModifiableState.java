package org.icgc.dcc.submission.core.state;

import static org.icgc.dcc.submission.fs.SubmissionFileEventType.FILE_RENAMED;

import java.util.List;

import org.icgc.dcc.common.core.model.ClinicalType;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.submission.fs.SubmissionFileEvent;
import org.icgc.dcc.submission.fs.SubmissionFileRenamedEvent;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.val;

/**
 * A state that allows file system modifications of the associated submission.
 */
public abstract class AbstractModifiableState extends AbstractClosePreservingState {

  @Override
  public final boolean isReadOnly() {
    // Not read-only
    return false;
  }

  @Override
  public void modifyFile(@NonNull StateContext context, @NonNull SubmissionFileEvent event) {
    // Current submission data state
    val report = context.getReport();
    val submissionFiles = context.getSubmissionFiles();

    // Refresh
    report.refreshFiles(submissionFiles);

    // Reset modified data type's reports
    val dataTypes = resolveModifiedDataTypes(event);
    if (!dataTypes.isEmpty()) {
      // DCC-4964:
      // If there was a core clinical file modified, we must reset all files. This is because, for example, a sample may
      // have been removed leading to invalid experimental files.
      val resetAll = dataTypes.contains(ClinicalType.CLINICAL_CORE_TYPE);

      if (resetAll) {
        report.resetDataTypes();
      } else {
        report.resetDataTypes(dataTypes);
      }
    }

    // Transition based on report
    val nextState = getReportedNextState(report);
    context.setState(nextState);
  }

  private List<DataType> resolveModifiedDataTypes(@NonNull SubmissionFileEvent event) {
    val dataTypes = ImmutableList.<DataType> builder();

    // Add if managed by dictionary
    val fileDataType = event.getFile().getDataType();
    if (fileDataType.isPresent()) {
      dataTypes.add(fileDataType.get());
    }

    // Two files involved. Add if managed by dictionary
    if (event.getType() == FILE_RENAMED) {
      val renameEvent = (SubmissionFileRenamedEvent) event;
      val newFile = renameEvent.getNewFile();

      val newFileDataType = newFile.getDataType();
      if (newFileDataType.isPresent()) {
        dataTypes.add(newFileDataType.get());
      }
    }

    return dataTypes.build();
  }

}
