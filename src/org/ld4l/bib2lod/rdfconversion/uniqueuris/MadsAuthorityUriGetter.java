package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.util.NacoNormalizer;

public class MadsAuthorityUriGetter extends ResourceUriGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(MadsAuthorityUriGetter.class);
    
    public MadsAuthorityUriGetter(Resource resource, String localNamespace) {
        super(resource, localNamespace);
    }

    @Override
    protected String getUniqueKey() {
        
        String authoritativeLabel = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.MADSRDF_AUTHORITATIVE_LABEL.property()).toList();

        for (Statement statement : statements) {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                authoritativeLabel = literal.getLexicalForm();
                break;
            }
        }
    
        // Prefix an additional string to the label, else the local name of 
        // the Authority will collide with that of the related resource, since
        // generally the madsrdf:authorizedLabel of the Authority and the
        // bf:authorizedAccessPoint of the related resource are identical.
        // *** TODO Are there other blank nodes that also need to be 
        // distinguished from the related resources?
        authoritativeLabel = "authority "  
                + NacoNormalizer.normalize(authoritativeLabel);
        LOGGER.debug("Got authoritativeLabel key " + authoritativeLabel
                + " for resource " + resource.getURI());
        
        return authoritativeLabel;
    }
}
