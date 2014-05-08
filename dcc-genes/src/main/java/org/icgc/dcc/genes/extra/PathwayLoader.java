/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.genes.extra;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringEscapeUtils;
import org.icgc.dcc.core.model.FieldNames;
import org.icgc.dcc.genes.cli.MongoClientURIConverter;
import org.jongo.Jongo;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * Create Pathway collection in mongodb, and subsequently embed pathway information into the gene-collection
 */
@Slf4j
@AllArgsConstructor
public class PathwayLoader {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String PATHWAY_COLLECTION_NAME = "Pathway";
  private static final String GENE_COLLECTION_NAME = "Gene";
  private static final String HOMO_SAPIEN = "Homo sapiens";
  private static final String[] CSV_HEADER = {
      FieldNames.PATHWAY_UNIPROT_ID,
      FieldNames.PATHWAY_REACTOME_ID,
      FieldNames.PATHWAY_URL,
      FieldNames.PATHWAY_NAME,
      FieldNames.PATHWAY_EVIDENCE_CODE,
      FieldNames.PATHWAY_SPECIES };

  private final Splitter TAB_SPLITTER = Splitter.on('\t');

  /**
   * Configuration.
   */
  private final MongoClientURI mongoUri;

  @SneakyThrows
  private Map<String, String> reactome2hasDiagram(URL hierarchyFile) {
    Map<String, String> result = newHashMap();

    val inputStream = hierarchyFile.openStream();
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
    processHasDiagram(doc.getDocumentElement(), result);
    return result;
  }

  private void processHasDiagram(Node node, Map<String, String> result) {
    val children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      val child = children.item(i);
      if (child.getNodeName().equals("Pathway")) {
        val name = child.getAttributes().getNamedItem("displayName");
        val hasDiagram = child.getAttributes().getNamedItem("hasDiagram");

        if (hasDiagram != null && hasDiagram.getNodeValue().equals("true")) {
          result.put(name.getNodeValue(), hasDiagram.getNodeValue());
        } else {
          result.put(name.getNodeValue(), "false");
        }
      }
      processHasDiagram(child, result);
    }
  }

  @SneakyThrows
  private BiMap<String, String> reactomeName2reactomeId(Reader summationReader) {
    BiMap<String, String> result = HashBiMap.create();

    @Cleanup
    BufferedReader bufferedReader = new BufferedReader(summationReader);
    String line = null;
    while ((line = bufferedReader.readLine()) != null) {
      List<String> tokens = TAB_SPLITTER.splitToList(line);
      String reactomeId = tokens.get(0);
      String reactomeName = tokens.get(1);
      reactomeName = StringEscapeUtils.unescapeHtml4(reactomeName);
      result.put(reactomeName, reactomeId);
    }
    return result;
  }

  @SneakyThrows
  private void parsePathwaySummation(URL file, Map<String, PathwayMeta> meta) {
    log.info("Parsing reactome summations");

    @Cleanup
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.openStream(), "UTF-8"));
    String line = null;
    int counter = 0;
    while ((line = bufferedReader.readLine()) != null) {
      List<String> tokens = TAB_SPLITTER.splitToList(line);
      PathwayMeta m = meta.get(tokens.get(0));
      if (m == null) {
        m = new PathwayMeta();
      }

      m.setPathwayId(tokens.get(0));
      m.setName(tokens.get(1));
      m.setSummation(tokens.get(2));
      meta.put(tokens.get(0), m);
      counter++;
    }
    log.info("Processed {} summation entries", counter);
  }

  @SneakyThrows
  private static void parseUniprot2Reactome(URL file, Map<String, PathwayMeta> meta) {
    log.info("Parsing uniprot2reactome");
    @Cleanup
    val csvReader = new CsvMapReader(new InputStreamReader(file.openStream(), "UTF-8"), CsvPreference.TAB_PREFERENCE);
    Map<String, String> map = null;
    while (null != (map = csvReader.read(CSV_HEADER))) {
      if (!map.get(FieldNames.PATHWAY_SPECIES).equals(HOMO_SAPIEN)) continue;

      val uniprot = map.get(FieldNames.PATHWAY_UNIPROT_ID);
      val pathwayID = map.get(FieldNames.PATHWAY_REACTOME_ID);
      val pathwayURL = map.get(FieldNames.PATHWAY_URL);
      val evidenceCode = map.get(FieldNames.PATHWAY_EVIDENCE_CODE);

      PathwayMeta m = meta.get(pathwayID);
      if (m == null) {
        m = new PathwayMeta();
      }
      m.setPathwayId(pathwayID);
      m.setEvidenceCode(evidenceCode);
      m.getUniProts().add(uniprot);
      m.setUrl(pathwayURL);
      meta.put(pathwayID, m);
    }
    log.info("Done uniprot2reactome");
  }

  @Data
  public static class PathwayMeta {

    public PathwayMeta() {
      uniProts = newHashSet();
    }

    String pathwayId;
    String species;
    String evidenceCode;
    String url;
    String name;
    String summation;
    Set<String> uniProts;
  }

  @SneakyThrows
  public void run(URL uniprotFile, URL summationFile, URL hierarchyFile) {

    // Build up a meta lookup table
    Map<String, PathwayMeta> meta = newHashMap();
    parsePathwaySummation(summationFile, meta);
    parseUniprot2Reactome(uniprotFile, meta);

    val reactomeName2reactomeId = reactomeName2reactomeId(new InputStreamReader(summationFile.openStream(), "UTF-8"));
    val hierParser = new PathwayHierarchyParser();
    val hier = hierParser.parse(hierarchyFile);
    val reactome2hasDiagram = reactome2hasDiagram(hierarchyFile);

    // 0) Annotate names with reactomeIds
    for (val reactomeName : hier.keySet()) {
      val set = hier.get(reactomeName);
      for (val list : set) {
        for (int i = 0; i < list.size(); i++) {
          String lookupStr = list.get(i).getName();
          lookupStr = StringEscapeUtils.unescapeHtml4(lookupStr);
          if (reactomeName2reactomeId.get(lookupStr) == null) {
            throw new Exception("Cannot find reactome id for :" + lookupStr);
          }
          list.get(i).setReactomeId(reactomeName2reactomeId.get(lookupStr));
        }
      }
    }

    // 1) Loop over reactome objects
    // 2) Get hierarchy by name
    // 3) Reverse hierarchy list to aggregate genes (reactomeId->uniprot)
    // 4) Annotate additional gene information for hierarchies
    for (val reactomeId : meta.keySet()) {
      val pathwayMeta = meta.get(reactomeId);

      val reactomeName = pathwayMeta.getName();
      val reactomeHierList = hier.get(reactomeName);

      for (val list : reactomeHierList) {
        // Base leaf node
        Set<String> aggregatedUniprot = newHashSet();
        aggregatedUniprot.addAll(pathwayMeta.getUniProts());

        for (int i = list.size() - 1; i >= 0; i--) {
          val segment = list.get(i);
          val segmentId = segment.getReactomeId();
          val segmentMeta = meta.get(segmentId);

          // Update aggregation
          aggregatedUniprot.addAll(segmentMeta.getUniProts());
          for (val uniprot : aggregatedUniprot) {
            segmentMeta.getUniProts().add(uniprot);
          }
        }
      }
    }

    // 5) Save data to mongodb (genes and pathway)
    @Cleanup
    val mongo = new MongoClient(mongoUri);
    val db = mongo.getDB(mongoUri.getDatabase());
    val jongo = new Jongo(db);

    // Start fresh
    val pathwayCollection = jongo.getCollection(PATHWAY_COLLECTION_NAME);
    pathwayCollection.drop();

    val geneCollection = jongo.getCollection(GENE_COLLECTION_NAME);
    geneCollection.ensureIndex("{external_db_ids.uniprotkb_swissprot:1}");
    geneCollection.update("{}").multi().with("{$unset: {pathways:''}}");
    geneCollection.update("{}").multi().with("{$unset: {reactome_pathways:''}}"); // Clean up old versions

    // Save genes
    log.info("Adding pathway to gene collection");
    for (val reactomeId : meta.keySet()) {
      val reactomeName = reactomeName2reactomeId.inverse().get(reactomeId);
      val pathwayMeta = meta.get(reactomeId);

      val uniprots = pathwayMeta.getUniProts();
      for (val uniprot : uniprots) {
        geneCollection
            .update("{external_db_ids.uniprotkb_swissprot:'" + uniprot + "'}")
            .multi()
            .with("{$push: { pathways:{pathway_id:#, pathway_name:#, source:#}}}",
                reactomeId, reactomeName, "Reactome");
      }
    }

    // Save reactome document
    log.info("Saving pathway documents");
    for (val reactomeId : meta.keySet()) {
      val pathwayMeta = meta.get(reactomeId);

      val reactomeName = reactomeName2reactomeId.inverse().get(reactomeId);
      val reactomeHier = hier.get(reactomeName);

      ObjectNode pathwayNode = JsonNodeFactory.instance.objectNode();
      val parentPathways = PathwayLoader.MAPPER.valueToTree(reactomeHier);

      long geneCount = geneCollection.count("{pathways.pathway_id:#}", reactomeId);

      pathwayNode.put(FieldNames.PATHWAY_REACTOME_ID, reactomeId);
      pathwayNode.put(FieldNames.PATHWAY_SOURCE, "Reactome");
      pathwayNode.put(FieldNames.PATHWAY_PARENT_PATHWAYS, parentPathways);
      pathwayNode.put(FieldNames.PATHWAY_NAME, pathwayMeta.getName());
      pathwayNode.put(FieldNames.PATHWAY_SUMMATION, pathwayMeta.getSummation());
      pathwayNode.put(FieldNames.PATHWAY_SPECIES, HOMO_SAPIEN);
      pathwayNode.put(FieldNames.PATHWAY_EVIDENCE_CODE, pathwayMeta.getEvidenceCode());
      pathwayNode.put(FieldNames.PATHWAY_GENE_COUNT, geneCount);
      pathwayNode.put(FieldNames.PATHWAY_HAS_DIAGRAM, reactome2hasDiagram.get(reactomeName));

      // Construct link_out array
      // Get the first node up the hierarchy chain that has a working diagram
      val reactomeHierList = hier.get(reactomeName);
      for (val list : reactomeHierList) {
        if (reactome2hasDiagram.get(reactomeName).equals("true")) {
          pathwayNode.withArray("link_out").add("#DIAGRAM=" + reactomeId + "&ID=" + reactomeId);
        } else {
          for (int i = list.size() - 1; i >= 0; i--) {
            val segment = list.get(i);
            if (segment.getHasDiagram().equals("true")) {
              pathwayNode.withArray("link_out").add("#DIAGRAM=" + segment.getReactomeId() + "&ID=" + reactomeId);
              break;
            }
          }
        }
      }

      pathwayCollection.save(pathwayNode);
    }
  }

  public static void main(String[] args) throws MalformedURLException {
    String mongoURI = args[0];
    String summationURI = args[1];
    String uniprotURI = args[2];
    String hierFileURI = args[3];

    val converter = new MongoClientURIConverter();
    val pathwayLoader = new PathwayLoader(converter.convert(mongoURI));
    pathwayLoader.run(new URL(uniprotURI), new URL(summationURI), new URL(hierFileURI));
  }

}
