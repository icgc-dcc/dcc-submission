package org.icgc.dcc.genes.extra;

import static org.apache.commons.lang.StringUtils.repeat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.xml.sax.SAXException;

import com.google.common.io.Resources;

@Slf4j
public class PathwayHierarchyParserTest {

  @Test
  public void testParse() throws MalformedURLException, IOException, SAXException, ParserConfigurationException {
    URL pathwayHierURL = Resources.getResource("pathway_hier.txt");
    log.info("Pathway URL {}", pathwayHierURL);

    val parser = new PathwayHierarchyParser();
    val results = parser.parse(pathwayHierURL);

    for (val dbId : results.keySet()) {
      log.info("{}", repeat("-", 80));
      log.info("Pathway dbId: '{}'", dbId);
      log.info("{}", repeat("-", 80));
      val set = results.get(dbId);

      int index = 1;
      for (val list : set) {
        log.info("{}.", index);
        for (int i = 0; i < list.size(); i++) {
          val segment = list.get(i);
          log.info("  {}{} - {}", repeat("  ", i), segment.getName(), segment.getHasDiagram());
        }

        index++;
      }
    }
  }

}
