package uk.ac.ebi.mydas.examples;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletContext;

import com.fasterxml.jackson.core.JsonProcessingException;

import uk.ac.ebi.mydas.configuration.DataSourceConfiguration;
import uk.ac.ebi.mydas.configuration.PropertyType;
import uk.ac.ebi.mydas.datasource.RangeHandlingAnnotationDataSource;
import uk.ac.ebi.mydas.exceptions.BadReferenceObjectException;
import uk.ac.ebi.mydas.exceptions.CoordinateErrorException;
import uk.ac.ebi.mydas.exceptions.DataSourceException;
import uk.ac.ebi.mydas.exceptions.UnimplementedFeatureException;
import uk.ac.ebi.mydas.model.DasAnnotatedSegment;
import uk.ac.ebi.mydas.model.DasType;
import uk.ac.ebi.mydas.model.DasEntryPoint;
import uk.ac.ebi.mydas.model.Range;


public class VariantDataSource implements RangeHandlingAnnotationDataSource{
        //CacheManager cacheManager = null;
        ServletContext svCon;
        Map<String, PropertyType> globalParameters;
        DataSourceConfiguration config;
        VariantParser elasticSearch;
        
        public void init(ServletContext servletContext, Map<String, PropertyType> globalParameters, DataSourceConfiguration dataSourceConfig)throws DataSourceException {
	        this.svCon = servletContext;
	        this.globalParameters = globalParameters;
	        this.config = dataSourceConfig;
	        elasticSearch=new VariantParser();
        }
        
        public void destroy() {
            elasticSearch.close();
        }
        
        public DasAnnotatedSegment getFeatures(String segmentId, int start, int stop, Integer maxbins) throws BadReferenceObjectException, CoordinateErrorException, DataSourceException {
        	try {
				return elasticSearch.parseBySegmentId(segmentId, start, stop);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	return null;
        }
        
        public DasAnnotatedSegment getFeatures(String segmentId, Integer maxbins) throws BadReferenceObjectException, DataSourceException {
        	try {
				return elasticSearch.parseBySegmentId(segmentId, -1, -1);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	return null;
        }
        
        public Collection<DasAnnotatedSegment> getFeatures(Collection<String> featureIdCollection, Integer maxbins)throws /*UnimplementedFeatureException,*/ DataSourceException {
        	//throw new UnimplementedFeatureException("Not implemented");
        	try {
        		return elasticSearch.parseByFeatureId(featureIdCollection);
        	}catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	return null;
        }
        
	public Collection<DasType> getTypes() throws DataSourceException {
	            return elasticSearch.getTypes();
	}

    public Integer getTotalCountForType(DasType type)throws DataSourceException {
    			return elasticSearch.getTotalCountForType(type.getId());//Edit this to actually return the real number of types
    }
    /*public void registerCacheManager(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
    }*/
    
    public URL getLinkURL(String field, String id) throws UnimplementedFeatureException, DataSourceException {
    	throw new UnimplementedFeatureException("Not implemented");
    }
    public Collection<DasEntryPoint> getEntryPoints(Integer start, Integer stop) throws UnimplementedFeatureException, DataSourceException {
            throw new UnimplementedFeatureException("Not implemented");
    }
    
    public String getEntryPointVersion() throws UnimplementedFeatureException, DataSourceException {
            throw new UnimplementedFeatureException("Not implemented");
    }
    
    public int getTotalEntryPoints() throws UnimplementedFeatureException, DataSourceException {
            throw new UnimplementedFeatureException("Not implemented");
    }
  
    @Override
    public DasAnnotatedSegment getFeatures(String segmentId, Integer maxbins,
                    Range rows) throws BadReferenceObjectException,
                    DataSourceException, UnimplementedFeatureException {
            throw new UnimplementedFeatureException("No implemented");
    }

    @Override
    public Collection<DasAnnotatedSegment> getFeatures(
                    Collection<String> featureIdCollection, Integer maxbins, Range rows)
                    throws UnimplementedFeatureException, DataSourceException {
            throw new UnimplementedFeatureException("Not implemented");
    }
    @Override
    public DasAnnotatedSegment getFeatures(
                    String featureId, int start, int stop, Integer maxbins, Range rows)
                    throws UnimplementedFeatureException, DataSourceException {
            throw new UnimplementedFeatureException("Not implemented");
    }
}