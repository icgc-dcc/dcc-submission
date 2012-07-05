package org.icgc.dcc.dictionary.visitor;

public class DictionaryIndexException extends RuntimeException {
  public DictionaryIndexException(Exception e) {
    super(e);
  }

  public DictionaryIndexException(String message) {
    super(message);
  }

  public DictionaryIndexException(String message, Exception e) {
    super(message, e);
  }
}
