package org.icgc.dcc.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.junit.Test;
import org.junit.runner.RunWith;

import cascading.cascade.Cascade;

import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ValidationTestModule.class })
public class ValidationInternalIntegrityTest {

  @Inject
  private DictionaryService dictionaries;

  @Inject
  private Planner planner;

  @Test
  public void test_validation() throws JsonProcessingException, IOException {
    /*
     * Set<RestrictionType> restrictionTypes = new HashSet<RestrictionType>(); restrictionTypes.add(new
     * DiscreteValuesRestriction.Type()); restrictionTypes.add(new RangeFieldRestriction.Type());
     * restrictionTypes.add(new RequiredRestriction.Type()); restrictionTypes.add(new CodeListRestriction.Type());
     * 
     * Planner planner = new DefaultPlanner(restrictionTypes);
     */
    DccFileSystem dccFileSystem = mock(DccFileSystem.class);
    ProjectService projectService = mock(ProjectService.class);

    CodeList codeList = mock(CodeList.class);
    List<Term> termList = new ArrayList<Term>();

    when(dictionaries.getCodeList("dr__donor_sex")).thenReturn(codeList);
    when(codeList.getTerms()).thenReturn(termList);

    ValidationService validationService = new ValidationService(dccFileSystem, projectService, planner, dictionaries);

    Dictionary dictionary =
        new ObjectMapper()
            .reader(Dictionary.class)
            .readValue(
                new File(
                    "/Users/jguberman/Documents/workspace/data-submission/server/src/test/resources/ValidationInternalIntegrityTest/dictionary.json"));

    File rootDir =
        new File(
            "/Users/jguberman/Documents/workspace/data-submission/server/src/test/resources/ValidationInternalIntegrityTest");
    File outputDir =
        new File(
            "/Users/jguberman/Documents/workspace/data-submission/server/src/test/resources/ValidationInternalIntegrityTest/Results");

    FileSchemaDirectory fileSchemaDirectory = new LocalFileSchemaDirectory(rootDir);
    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir);

    Cascade cascade = validationService.planCascade(null, null, fileSchemaDirectory, cascadingStrategy, dictionary);
    validationService.runCascade(cascade, null, null);
  }
}
