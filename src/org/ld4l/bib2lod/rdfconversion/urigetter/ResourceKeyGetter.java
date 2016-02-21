package org.ld4l.bib2lod.rdfconversion.uniquekey;

import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.util.NacoNormalizer;

// If not needed as a fallback KeyGetter, make abstract.
public class ResourceKeyGetter {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(ResourceKeyGetter.class);

    protected final Resource resource;

    public ResourceKeyGetter(Resource resource) {
        this.resource = resource;
    }

    // Subclasses should call super.getKey() if they have failed to identify
    // another key.
    protected String getKey() {

        // TODO Doesn't apply to bf:Instance. Either test for that or move into
        // specific methods; or never send back null from the Instance method.
        // (The latter approach may be error-prone.)
        String key = getKeyFromBfLabel();

        if (key == null) {
            // TEMPORARY! - or will this be the fallback?
            key = resource.getLocalName();
        }
       
        return key;
    }

    protected String getKeyFromBfLabel() {
        
        String bfLabel = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.BF_LABEL.property()).toList();

        // If there is more than one (which there shouldn't be), we'll get the
        // first one, but doesn't matter if we have no selection criteria. 
        for (Statement statement : statements) {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                bfLabel = object.asLiteral().getLexicalForm();
                break;
            }
        }
    
        bfLabel = NacoNormalizer.normalize(bfLabel);
        LOGGER.debug("Got bf:label key " + bfLabel + " for resource " 
                + resource.getURI());
                
        return bfLabel;
    }
}
