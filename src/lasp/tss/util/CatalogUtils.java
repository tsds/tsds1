package lasp.tss.util;

import java.net.URI;
import java.util.List;

import lasp.tss.TSSException;

import org.apache.log4j.Logger;

import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;

public class CatalogUtils {

    // Initialize a logger.
    private static final Logger _logger = Logger.getLogger(CatalogUtils.class);
    
    
    public static InvCatalogImpl readCatalog(String url) {
        InvCatalogImpl catalog = null;
        
        URI catalogURI = null;
        try {
            catalogURI = new URI(url);

            boolean validate = true; //TODO:  doesn't work?
            InvCatalogFactory factory = new InvCatalogFactory("default", validate);
            catalog = (InvCatalogImpl) factory.readXML(catalogURI);
            
            if (catalog.hasFatalError()) {
                String msg = catalog.getLog();
                _logger.warn("Error reading catalog " + catalogURI + ": " + msg);
                catalog = null;
            }
            
        } catch (Throwable t) {
            String msg = "Unable to read the catalog: " + catalogURI;
            _logger.warn(msg, t);
            throw new TSSException(msg, t);
        }
        
        return catalog;
    }
    
    /**
     * Find the Dataset within the given catalog with the given name.
     * Nested datasets will have names delimited by "/".
     */
    public static InvDataset findDataset(InvCatalog catalog, String dsname) {
        List<InvDataset> datasets = catalog.getDatasets();
        //TODO: will this get CatalogRefs?
        
        InvDataset dataset = findDataset(datasets, dsname);
        return dataset;
    }

    /**
     * Find the Dataset within the given list of datasets with the given name.
     * Nested datasets will have names delimited by "/". Recurse to find nested
     * datasets. This will try to match based on "ID"s first then "name" (or "title"
     * in the case of catalogRefs).
     */
    private static InvDataset findDataset(List<InvDataset> datasets, String dsname) {
        InvDataset dataset = null;
        
        //Split nested dataset name on first "/".
        //If the dataset is not nested (i.e. no "/") then names will be of length 1.
        String[] names = dsname.split("/", 2); //apply split at first match only
        
        for (InvDataset ds : datasets) {
            //does the ds match the highest level name
            String name = ds.getID();
            if (name == null) name = ds.getName();
            if (name.equals(names[0])) {
                dataset = ds;
            }
            if (dataset != null && names.length == 2) {
                //we matched but still need to dig deeper
                //if this is a catalog ref, pass over the referenced catalog node
                //TODO: ref'd cat with one dataset won't preserve catalog layer
                if (dataset instanceof InvCatalogRef) dataset = dataset.getDatasets().get(0);
                List<InvDataset> dss = dataset.getDatasets();
                dataset = findDataset(dss, names[1]); //recursive
            }
            
            if (dataset != null) break; //we found it, so we're done
        }
        
        return dataset;
    }

}
