package org.icgc.dcc.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    when(dictionaryService.getCodeList("dr__donor_sex")).thenReturn(codeList);
    when(dictionaryService.getCodeList("dr__donor_vital_status")).thenReturn(codeList);
    when(dictionaryService.getCodeList("dr__disease_status_last_followup")).thenReturn(codeList);
    when(dictionaryService.getCodeList("dr__donor_relapse_type")).thenReturn(codeList);

    when(codeList.getTerms()).thenReturn(termList);

    validationService = new ValidationService(dccFileSystem, projectService, planner);
    dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(
            new File(this.getClass().getResource("/dictionary.json").getFile()));

    // TODO: rm ValidationInternalIntegrityTest dir
  }

  @Test
  public void test_validate_valid() throws IOException {
    validate(validationService, dictionary, "/integration/validation/error/codelist");
  }

  private void validate(ValidationService validationService, Dictionary dictionary, String relative) throws IOException {
    String rootDirString = this.getClass().getResource(relative).getFile();
    String rootDirString2 = this.getClass().getResource(relative + "/" + ".validation").getFile();

    File rootDir = new File(rootDirString);
    File outputDir = new File(rootDirString2);

    FileSchemaDirectory fileSchemaDirectory = new LocalFileSchemaDirectory(rootDir);
    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir);

    Cascade cascade = validationService.planCascade(null, null, fileSchemaDirectory, cascadingStrategy, dictionary);
    Assert.assertEquals(1, cascade.getFlows().size());
    validationService.runCascade(cascade, null, null);

    String name = rootDirString + "/" + ".validation" + "/" + "donor.internal#errors.json";
    String content = FileUtils.readFileToString(new File(name));
    Assert.assertTrue(content.isEmpty());
  }
}
