package org.icgc.dcc.validation;

import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.function.FieldCountFunction;
import org.icgc.dcc.validation.function.FieldsValueTypeFunction;

import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.tuple.Fields;

public class FileSchemaPipeBuilder extends SubAssembly {

  FileSchemaPipeBuilder(Pipe head, FileSchema schema) {
    Pipe tail = new Each(head, Fields.ALL, new FieldCountFunction(schema), Fields.RESULTS);
    tail = new Each(tail, Fields.ALL, new FieldsValueTypeFunction(schema), Fields.RESULTS);
    setTails(tail);
  }
}
