package org.icgc.dcc.reporter.cascading.subassembly.table1;

import lombok.val;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;

public class Table1 extends SubAssembly {

  public Table1(Pipe preComputationTable) {
    val preProcessed = new Table1Preprocessing(preComputationTable);
    setTails(new Merge(
        new Table1ClinicalProcessing(preProcessed),
        new Table1FeaturesProcessing(preProcessed)));
  }

}
