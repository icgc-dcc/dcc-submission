package uk.ac.ebi.mydas.examples;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import uk.ac.ebi.mydas.model.DasComponentFeature;
import uk.ac.ebi.mydas.model.DasFeature;
import uk.ac.ebi.mydas.model.DasFeatureOrientation;
import uk.ac.ebi.mydas.model.DasMethod;
import uk.ac.ebi.mydas.model.DasPhase;
import uk.ac.ebi.mydas.model.DasType;
import uk.ac.ebi.mydas.exceptions.DataSourceException;
import uk.ac.ebi.mydas.extendedmodel.DasUnknownFeatureSegment;

public class VariantParser {
	
	private static final Logger log = LoggerFactory.getLogger(ElasticSearchParser.class);
	
	private final TransportClient es; 
	private final ObjectReader reader;
	private ArrayList<DasType> types;
    private DasType ssmType;
    private DasType parentType;
    private DasMethod method;
    private DasMethod methodNonPositional;
    private String segment;
    private int start;
    private int end;
	
	public VariantParser() throws DataSourceException{
		es = new TransportClient().addTransportAddress(new InetSocketTransportAddress(
			      "localhost", 9300));
		reader = new ObjectMapper().reader();
		types = new ArrayList<DasType>();
        ssmType= new DasType("Somatic Variant", null,null,null);
        parentType = new DasType("Somatic Variant",null, "SO:0001777", "Somatic Variant");
        types.add(ssmType);
        types.add(parentType);
        segment = null;//Just initialized
        start = -2;//-2 stands for just initialized
        end = -2;//-2 stands for just initialized
        method = new DasMethod("combinatorial analysis used in automatic assertion","combinatorial analysis used in automatic assertion","ECO:0000213");
        methodNonPositional = new DasMethod("N/A", null, null);
	}
	
	public DasAnnotatedSegment parseBySegmentId(String segmentID, int start, int stop) throws JsonProcessingException, IOException, DataSourceException{
		//log.info("Parsing...");
		QueryBuilder qb = null;
		//QueryBuilder qb1 = null;
		SearchResponse esresponse = null;
		
		//The following three variables are to be used in the featureID command
		this.segment = segmentID;
		this.start= start;
		this.end = stop;
		
		if(start==-1&&stop==-1){
			qb = termQuery("chr", segmentID);
			
			esresponse = es.prepareSearch("varianttest").setTypes("variant")
			        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			        .setQuery(qb)
			        .setFrom(0).setSize(1000) //Way to include all results?
			        .execute()
			        .actionGet();
		}
		else{
			qb = filteredQuery(
					termQuery("chr", segmentID),
					rangeFilter("start").from(start).to(stop).includeLower(true).includeUpper(true));
			/*qb1 = filteredQuery(
					termQuery("chromosome", segmentID),
					rangeFilter("stop").from(start).to(stop).includeLower(true).includeUpper(true));
			*/
			esresponse = es.prepareSearch("varianttest").setTypes("variant")
			        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			        .setQuery(qb)
			        .setFrom(0).setSize(1000) //Way to include all results
			        .execute()
			        .actionGet();
		}
		
		DasAnnotatedSegment segment = null;
		
		//The following gets the length of the chromosome being queried
		if(start==-1&&stop==-1){
			Scanner chromSizes = new Scanner(new File("chromSizes.txt"));
			while(chromSizes.hasNextLine()){
				String line = chromSizes.nextLine();
				if(segmentID.length()==1){
					if(line.substring(3, 4).equals(segmentID)){
						segment = new DasAnnotatedSegment(segmentID,1,Integer.parseInt(line.substring(5).trim()),"1.0",segmentID, new ArrayList<DasFeature>());
					}
				}
				else if(line.substring(3, 5).equals(segmentID)){
					segment = new DasAnnotatedSegment(segmentID,1,Integer.parseInt(line.substring(6).trim()),"1.0",segmentID, new ArrayList<DasFeature>());
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
				
				ArrayList<String> notes = new ArrayList<String>();
				notes.add("Projects");
				ArrayList<String> projectID = new ArrayList<String>();
				
				for (JsonNode ssms : response.path("ssm")){	
					projectID.add(ssms.path("project_key").asText());
				}
				
				int totalNumberOfPatients = 0;
				int numberOfPatients = 0;
				for(int i=0;i<projectID.size();i++){
					numberOfPatients = 1;
					for(int j=i+1;j<projectID.size();j++){
						if(projectID.get(i).equals(projectID.get(j))){
							projectID.remove(j);
							numberOfPatients++;
						}
					}
					notes.add(projectID.get(i)+" ("+numberOfPatients+")");
					totalNumberOfPatients += numberOfPatients;
				}
				DasFeature parent = this.getFeature(response.path("snv_id").asText(),0, 0,parentType,null,null,null,segment);
				DasFeature child = this.getFeature(response.path("snv_id").asText()+":child", response.path("start").asInt(), response.path("end").asInt(), ssmType, (double)(totalNumberOfPatients),parent, notes, segment);
				parent.getParts().add(child.getFeatureId());
			}
			
					
		}
		return segment;
	}
	
	public Collection<DasAnnotatedSegment> parseByFeatureId(Collection<String> FeatureId) throws JsonProcessingException, IOException, DataSourceException{
		//log.info("Parsing...");
		
		SearchResponse esresponse = null;
		QueryBuilder qb = null;
		
		//Collection<DasAnnotatedSegment> segments=new ArrayList<DasAnnotatedSegment>();
		if(this.start==-1&&this.end==-1){
			qb = termQuery("chr", this.segment);
			
			esresponse = es.prepareSearch("varianttest").setTypes("variant")
			        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			        .setQuery(qb)
			        .setFrom(0).setSize(100000) //Way to include all results?
			        .execute()
			        .actionGet();
		}
		else{
			qb = filteredQuery(
					termQuery("chr", this.segment),
					rangeFilter("start").from(this.start).to(this.end).includeLower(true).includeUpper(true));
			
			esresponse = es.prepareSearch("varianttest").setTypes("variant")
			        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
			        .setQuery(qb)
			        .setFrom(0).setSize(100000) //Way to include all results
			        .execute()
			        .actionGet();
		}
		Object [] featureID = FeatureId.toArray();
		FeatureId.clear();
		for (SearchHit hit : esresponse.hits()) {
			String json = hit.getSourceAsString();
			JsonNode response = reader.readTree(json);
			//DasAnnotatedSegment segment = this.getSegment(segments, response.path("chr").asText());
			
			//ArrayList<String> notes = new ArrayList<String>();
			//notes.add("Projects");
			
			//ArrayList<String> projectID = new ArrayList<String>();
			
			boolean projectInSNV = false;
			/*
			 * Iterates through all the ssms in a the current SNV hit. If any of the projectID's passed in through the 'FeatureId' collection argument
			 * is contributing to that SSM, then projectInSnv is set to true.
			 */
			for(JsonNode ssms:response.path("ssm")){
				//projectID.add(ssms.path("project_key").asText());//This is used as a counter for the numberofpatients contributing to a given project
				
				for(int x=0; x<featureID.length;x++){
					if(projectInSNV){
						break;
					}
					else if((ssms.path("project_key").asText().trim()).equals(featureID[x].toString())){
						FeatureId.add(response.path("snv_id").asText());
						FeatureId.add(response.path("snv_id").asText()+":child");
						projectInSNV = true;
						break;
					}
				}
				
			}
			//If any one of the projectID's passed in through the FeatureId collection are part of an SNV then the following is executed
			
			/*if(projectInSNV){
				FeatureId.add(response.path("snv_id").asText());
				/*int totalNumberOfPatients = 0;
				int numberOfPatients = 0;
				for(int y=0;y<projectID.size();y++){
					numberOfPatients = 1;
					for(int z=y+1;z<projectID.size();z++){
						if(projectID.get(y).equals(projectID.get(z))){
							projectID.remove(z);
							numberOfPatients++;
						}
					}
					notes.add(projectID.get(y)+" ("+numberOfPatients+")");
					totalNumberOfPatients+=numberOfPatients;
				}
				DasFeature parent = this.getFeature(response.path("snv_id").asText()+":parent",0, 0,parentType,null,null,null,segment);
				DasFeature child = this.getFeature(response.path("snv_id").asText(), response.path("start").asInt(), response.path("end").asInt(), ssmType, (double)(totalNumberOfPatients),parent, notes, segment);
				parent.getParts().add(child.getFeatureId());
			}*/
		}
		
		
		/*for(DasAnnotatedSegment segment:segments){
			for(DasFeature feature:segment.getFeatures()){
				FeatureId.add(feature.getFeatureId());
			}
		}*/
		return null;
	}

    private DasFeature getFeature(String featureId, int start, int stop, DasType type, Double score,DasFeature parent, ArrayList<String> notes, DasAnnotatedSegment segment) throws DataSourceException {
		DasFeatureOrientation orientation= DasFeatureOrientation.ORIENTATION_NOT_APPLICABLE;
		DasPhase phase= DasPhase.PHASE_NOT_APPLICABLE;
		
		HashSet parents=new HashSet();
		
		if(parent!=null){
			parents.add(parent.getFeatureId());
		}
		
		DasFeature featureOfSegment = null;
		if(start==0){
			featureOfSegment=new DasFeature(featureId,null,type, methodNonPositional, start, stop , null,orientation,phase, notes, null, null, parents, new ArrayList<String>());
		}
		else{
			featureOfSegment=new DasFeature(featureId,featureId,type, method, start, stop , score,orientation,phase, notes, null, null, parents, new ArrayList<String>());
		}
		segment.getFeatures().add(featureOfSegment);
		return featureOfSegment;
	}

    /*private DasFeature getFeatureWithId(String featureId, DasAnnotatedSegment segment){
        for(DasFeature feature:segment.getFeatures()){
            if(feature.getFeatureId().equals(featureId)){
                return feature;
            }
        }
        return null;
    }*/
    
    private DasAnnotatedSegment getSegment(Collection<DasAnnotatedSegment> segments,String segmentId) throws DataSourceException, NumberFormatException, FileNotFoundException {
        if(!segments.isEmpty())
	    	for (DasAnnotatedSegment segment:segments)
	                if (segment.getSegmentId().equals(segmentId))
	                        return segment;
        Integer length = this.getSegmentLength(segmentId);
        DasAnnotatedSegment newSegment = new DasAnnotatedSegment(segmentId,1,length,"1.0",segmentId, new ArrayList<DasFeature>());
        segments.add(newSegment);
        return newSegment;
    }
    
    private Integer getSegmentLength(String segmentId) throws NumberFormatException, DataSourceException, FileNotFoundException{
    	int length = 1;
    	Scanner chromSizes = new Scanner(new File("chromSizes.txt"));
		while(chromSizes.hasNextLine()){
			String line = chromSizes.nextLine();
			if(line.substring(3, 5).equals("12")){
				length = Integer.parseInt(line.substring(6));
			}
		}
		chromSizes.close();
		return length;
    }
    public ArrayList<DasType> getTypes() {
        return types;
    }
    
    public Integer getTotalCountForType(String typeId){
    	//Complete if needed
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
