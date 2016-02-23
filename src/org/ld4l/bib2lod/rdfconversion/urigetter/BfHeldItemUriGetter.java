package org.ld4l.bib2lod.rdfconversion.urigetter;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;

public class BfHeldItemUriGetter extends ResourceUriGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfHeldItemUriGetter.class);

    // Order is crucial here
    private static String[] keyTypes = 
        { "lcc", "ddc", "nlm", "udc", "barcode", "id" };        

    private static String sparql = 
                "SELECT ?item ?lcc ?ddc ?nlm ?udc ?barcode ?id "
                + "?shelfMark ?scheme ?label "
                + "WHERE { "
                /*
                 * We really only want to use one of these OPTIONALS if 
                 * previous ones haven't been matched, since one match is
                 * sufficient. This constraint will be managed during 
                 * query result processing to keep the query from getting
                 * unmanageably complex.
                 */
                + "?item a " + BfType.BF_HELD_ITEM.sparqlUri() + " . "
                + "OPTIONAL { ?item "
                + BfProperty.BF_SHELF_MARK_LCC.sparqlUri() + " ?lcc . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_SHELF_MARK_DDC.sparqlUri() + " ?ddc . } "
                + "OPTIONAL { ?item "
                + BfProperty.BF_SHELF_MARK_NLM.sparqlUri() + " ?nlm . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_SHELF_MARK_UDC.sparqlUri() + " ?udc . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_BARCODE.sparqlUri() + " ?barcode . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_ITEM_ID.sparqlUri() + " ?id . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_SHELF_MARK.sparqlUri() + " ?shelfMark ; "
                + BfProperty.BF_SHELF_MARK_SCHEME.sparqlUri() 
                + " ?scheme . } "   
                + "OPTIONAL { ?item "
                + BfProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "}";
    
    public BfHeldItemUriGetter(Resource resource, String localNamespace) {
        super(resource, localNamespace);
    }

    @Override
    protected String getUniqueKey() {

        QueryExecution qexec = 
                QueryExecutionFactory.create(sparql, resource.getModel());
        ResultSet results = qexec.execSelect();
        
        // If there is more than one (which there shouldn't be), we'll get the
        // first one, but doesn't matter if we have no selection criteria. 
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();

            for (String k : keyTypes) {
                Literal lit = soln.getLiteral(k);
                if (lit != null) {
                    LOGGER.debug("Getting key of type " + k + " with value " 
                            + lit.getLexicalForm());          
                    return lit.getLexicalForm();
                } else {
                    LOGGER.debug("No value for " + k);
                }
            }
            
            // If matching on shelf mark, the shelf mark scheme must also match.
            Literal mark = soln.getLiteral("shelfMark");
            if (mark != null) {
                Literal scheme = soln.getLiteral("scheme");
                if (scheme != null) {
                    LOGGER.debug("Getting unique key from shelfMark and "
                            + "shelfMarkScheme");
                    return scheme.getLexicalForm() + mark.getLexicalForm();
                } else {
                    LOGGER.debug("No value for shelfMark and shelfMarkScheme");
                }
            }
        }
        
        return super.getUniqueKey();
    }
}
