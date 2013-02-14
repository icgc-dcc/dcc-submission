package uk.ac.ebi.mydas.examples;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.mydas.model.DasAnnotatedSegment;
import uk.ac.ebi.mydas.model.DasFeature;
import uk.ac.ebi.mydas.model.DasFeatureOrientation;
import uk.ac.ebi.mydas.model.DasMethod;
import uk.ac.ebi.mydas.model.DasPhase;
import uk.ac.ebi.mydas.model.DasType;
import uk.ac.ebi.mydas.exceptions.DataSourceException;
import uk.ac.ebi.mydas.extendedmodel.DasUnknownFeatureSegment;

public class ElasticSearchParser {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticSearchParser.class);
	
	private final TransportClient es; 
	private final ObjectReader reader;
	private ArrayList<DasType> types;
    private DasType geneType,transcriptType,exonType;
    private DasMethod method;
	
	public ElasticSearchParser() throws DataSourceException{
		es = new TransportClient().addTransportAddress(new InetSocketTransportAddress(
			      "localhost", 9300));
		reader = new ObjectMapper().reader();
		types = new ArrayList<DasType>();
		geneType= new DasType("Gene", null, "SO:0000704", "Gene");
        transcriptType= new DasType("Transcript", null, "SO:0000673", "Transcript");
        exonType= new DasType("Exon", null, "SO:0000147", "Exon");
        types.add(geneType);
        types.add(transcriptType);
        types.add(exonType);
        method = new DasMethod("combinatorial analysis used in automatic assertion","combinatorial analysis used in automatic assertion","ECO:0000213");
	}
	
	public DasAnnotatedSegment parseBySegmentId(String segmentID, int start, int stop) throws JsonProcessingException, IOException, DataSourceException{
		//log.info("Parsing...");
		QueryBuilder qb = null;
		//QueryBuilder qb1 = null;
		SearchResponse esresponse = null;
		
		if(start==-1&&stop==-1){
			qb = termQuery("chromosome", segmentID);
			
			esresponse = es.prepareSearch("genetest").setTypes("gene")
			        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			        .setQuery(qb)
			        .setFrom(0).setSize(100) //Way to include all results?
			        .execute()
			        .actionGet();
		}
		else{
			qb = filteredQuery(
					termQuery("chromosome", segmentID),
					rangeFilter("start").from(start).to(stop).includeLower(true).includeUpper(true));
			/*qb1 = filteredQuery(
					termQuery("chromosome", segmentID),
					rangeFilter("stop").from(start).to(stop).includeLower(true).includeUpper(true));
			*/
			esresponse = es.prepareSearch("genetest").setTypes("gene")
			        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			        .setQuery(qb)
			        .setFrom(0).setSize(100) //Way to include all results
			        .execute()
			        .actionGet();
		}
		
		DasAnnotatedSegment segment = null;
		
		//Read from chromSizes.txt file to retrieve length of segment
		if(start==-1&&stop==-1){
			Scanner chromSizes = new Scanner(new File("chromSizes.txt"));
			while(chromSizes.hasNextLine()){
				String line = chromSizes.nextLine();
				if(segmentID.length()==1){
					if(line.substring(3, 4).equals(segmentID)){
						segment = new DasAnnotatedSegment(segmentID,1,Integer.parseInt(line.substring(5).trim()),"1.0",segmentID, new ArrayList<DasFeature>());
					}
				}
				else{
					if(line.substring(3, 5).equals(segmentID)){
						segment = new DasAnnotatedSegment(segmentID,1,Integer.parseInt(line.substring(6).trim()),"1.0",segmentID, new ArrayList<DasFeature>());
					}
				}
			}
			chromSizes.close();
		}
		else{
			segment = new DasAnnotatedSegment(segmentID,start, stop,"1.0",segmentID, new ArrayList<DasFeature>());
		}
		
		if(esresponse.hits().totalHits()==0){
			segment = new DasUnknownFeatureSegment(segmentID);
		}
		else{
			for (SearchHit hit : esresponse.hits()) {
				String json = hit.getSourceAsString();
				JsonNode response = reader.readTree(json);
			
				
				ArrayList<String> geneNotes = new ArrayList<String>();
				geneNotes.add(response.path("symbol").asText());
				geneNotes.add(response.path("name").asText());
				int geneOrientation = response.path("strand").asInt();
				DasFeature gene = this.getFeature(response.path("id").asText(), response.path("start").asInt(), response.path("end").asInt(), geneType, null, geneOrientation, geneNotes,segment);

					
				int x = 1;//Controls exonID since there isn't one
				for (JsonNode trans : response.path("transcripts")){
					
					int transcriptStart = 1000000000;
					int transcriptEnd = 0;
					
					//Used to get start and end of transcript since it is not explicitly stated in the transcript domain
					for (JsonNode exons : trans.path("exons")){
						int begin = exons.path("start").asInt();
						int end = exons.path("end").asInt();
						if(begin<transcriptStart){
							transcriptStart=begin;
						}
						if(end>transcriptEnd){
							transcriptEnd=end;
						}
					}
					
					DasFeature transcript = this.getFeature(trans.path("id").asText(), transcriptStart, transcriptEnd, transcriptType, gene,0, null, segment);				
					
					for (JsonNode exons : trans.path("exons")){
						DasFeature exon = this.getFeature((trans.path("id").asText()+"."+x), exons.path("start").asInt(), exons.path("end").asInt(), exonType, transcript,0, null, segment);
						transcript.getParts().add(exon.getFeatureId());
						x++;
					}
		
					gene.getParts().add(transcript.getFeatureId());
					x=1;//Sets it back to 1 for the new transcript
				}	
			}
		}
		return segment;
	}
	
    private DasFeature getFeature(String featureId, int start, int stop, DasType type, DasFeature parent, int orientation, ArrayList<String> notes, DasAnnotatedSegment segment) throws DataSourceException {
		//Orientations (plural) is the actual orientation, and orientation is the parameter passed in
		DasFeatureOrientation orientations = null;
		if(orientation==1){
			orientations = DasFeatureOrientation.ORIENTATION_SENSE_STRAND;
		}
		else if(orientation==-1){
			orientations= DasFeatureOrientation.ORIENTATION_ANTISENSE_STRAND;
		}
		else{
			orientations = DasFeatureOrientation.ORIENTATION_NOT_APPLICABLE;
		}
		
		
		DasPhase phase= DasPhase.PHASE_NOT_APPLICABLE;
		Double score=null;
		
		HashSet parents=new HashSet();
		
		if(parent!=null){
			parents.add(parent.getFeatureId());
		}
		DasFeature featureOfSegment=new DasFeature(featureId,featureId,type, method, start, stop , score,orientations,phase, notes, null, null, parents, new ArrayList<String>());
		segment.getFeatures().add(featureOfSegment);
		
		return featureOfSegment;
	}

    /*No point in implementing this because we're not saving the DAS model in memory and every transcript/gene/exon has a unique ID.
     * private DasFeature getFeatureWithId(String featureId, DasAnnotatedSegment segment){
        for(DasFeature feature:segment.getFeatures()){
            if(feature.getFeatureId().equals(featureId)){
                return feature;
            }
        }
        return null;
    }*/
    
    public ArrayList<DasType> getTypes() {
        return types;
    }
    
    public Integer getTotalCountForType(String typeId){
    	//Complete this part if need for it is found
    	return null;
    }
    
    public void close(){
		es.close();
	}
    
    /*For testing in JUnit
    public JsonNode parsing() throws JsonProcessingException, IOException{
    	QueryBuilder qb = termQuery("chromosome", "12");
		
    	SearchResponse esresponse = es.prepareSearch("genetest").setTypes("gene")
		        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
		        .setQuery(qb)
		        .setFrom(0).setSize(60)
		        .execute()
		        .actionGet();
	
		//DasAnnotatedSegment segment = null;
		for (SearchHit hit : esresponse.hits()) {
			String json = hit.getSourceAsString();
			JsonNode response = reader.readTree(json);
			return response;
		
	    }
    return null;
    }*/
}
