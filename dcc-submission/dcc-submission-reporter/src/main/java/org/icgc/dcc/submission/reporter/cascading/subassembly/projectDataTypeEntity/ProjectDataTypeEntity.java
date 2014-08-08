package org.icgc.dcc.submission.reporter.cascading.subassembly.projectDataTypeEntity;

import lombok.val;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;

public class ProjectDataTypeEntity extends SubAssembly {

  public ProjectDataTypeEntity(Pipe preComputationTable) {
    val preProcessed = new ProjectDataTypeEntityPreprocessing(preComputationTable);
    setTails(new Merge(
        new ProjectDataTypeEntityClinicalProcessing(preProcessed),
        new ProjectDataTypeEntityFeaturesProcessing(preProcessed)));
  }

}
