package org.icgc.dcc.validation.report;

import java.util.List;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Embedded
public class SchemaReport {

  protected String name;

  protected List<FieldReport> fieldReports;

  protected List<ValidationErrorReport> errors;

  public SchemaReport() {
    this.fieldReports = Lists.newArrayList();
    this.errors = Lists.newArrayList();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<FieldReport> getFieldReports() {
    return ImmutableList.copyOf(this.fieldReports);
  }

  public List<ValidationErrorReport> getErrors() {
    return ImmutableList.copyOf(this.errors);
  }

  public void addError(ValidationErrorReport error) {
    this.errors.add(error);
  }

  public void addErrors(List<ValidationErrorReport> errors) {
    this.errors.addAll(errors);
  }

  public Optional<FieldReport> getFieldReport(final String field) {
    return Iterables.tryFind(fieldReports, new Predicate<FieldReport>() {

      @Override
      public boolean apply(FieldReport input) {
        return input.getName().equals(field);
      }
    });
  }

  public void addFieldReport(FieldReport fieldReport) {
    this.fieldReports.add(fieldReport);
  }

  public void addFieldReports(List<FieldReport> fieldReports) {
    this.fieldReports.addAll(fieldReports);
  }
}
