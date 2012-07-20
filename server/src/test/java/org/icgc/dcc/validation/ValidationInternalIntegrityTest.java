package org.icgc.dcc.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.GuiceJUnitRunner;
import org.icgc.dcc.filesystem.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.validation.service.ValidationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import cascading.cascade.Cascade;

import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ValidationTestModule.class })
public class ValidationInternalIntegrityTest {

  @Inject
  private DictionaryService dictionaryService;

  @Inject
  private Planner planner;

  private ValidationService validationService;

  private Dictionary dictionary;

  @Before
  public void setUp() throws JsonProcessingException, IOException {
    DccFileSystem dccFileSystem = mock(DccFileSystem.class);
    ProjectService projectService = mock(ProjectService.class);

    CodeList codeList1 = mock(CodeList.class);
    CodeList codeList2 = mock(CodeList.class);
    CodeList codeList3 = mock(CodeList.class);
    CodeList codeList4 = mock(CodeList.class);

    List<Term> termList1 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    List<Term> termList2 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    List<Term> termList3 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null),
            new Term("4", "dummy", null), new Term("5", "dummy", null));
    List<Term> termList4 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null));

    when(dictionaryService.getCodeList("dr__donor_sex")).thenReturn(codeList1);
    when(dictionaryService.getCodeList("dr__donor_vital_status")).thenReturn(codeList2);
    when(dictionaryService.getCodeList("dr__disease_status_last_followup")).thenReturn(codeList3);
    when(dictionaryService.getCodeList("dr__donor_relapse_type")).thenReturn(codeList4);

    when(codeList1.getTerms()).thenReturn(termList1);
    when(codeList2.getTerms()).thenReturn(termList2);
    when(codeList3.getTerms()).thenReturn(termList3);
    when(codeList4.getTerms()).thenReturn(termList4);

    validationService = new ValidationService(dccFileSystem, projectService, planner);
    dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(
            new File(this.getClass().getResource("/dictionary.json").getFile()));

    // TODO: rm ValidationInternalIntegrityTest dir
  }

  @Test
  public void test_validate_valid() throws IOException {
    validate(validationService, dictionary, "/integration/validation/internal");
  }

  private void validate(ValidationService validationService, Dictionary dictionary, String relative) throws IOException {
    String rootDirString = this.getClass().getResource(relative).getFile();
    String outputDirString = rootDirString + "/" + ".validation";
    String errorFileString = outputDirString + "/" + "donor.internal#errors.json";
    ;
    File errorFile = new File(errorFileString);
    errorFile.delete();
    Assert.assertFalse(errorFileString, errorFile.exists());

    File rootDir = new File(rootDirString);
    File outputDir = new File(outputDirString);

    FileSchemaDirectory fileSchemaDirectory = new LocalFileSchemaDirectory(rootDir);
    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir);

    Cascade cascade = validationService.planCascade(null, null, fileSchemaDirectory, cascadingStrategy, dictionary);
    Assert.assertEquals(1, cascade.getFlows().size());
    validationService.runCascade(cascade, null, null);

    Assert.assertTrue(errorFileString, errorFile.exists());
    String content = FileUtils.readFileToString(errorFile);
    Assert.assertTrue(content, content.isEmpty());
  }
}
