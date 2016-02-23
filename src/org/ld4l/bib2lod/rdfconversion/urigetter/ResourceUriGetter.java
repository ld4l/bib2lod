package org.ld4l.bib2lod.rdfconversion.urigetter;

import java.util.List;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.util.MurmurHash;
import org.ld4l.bib2lod.util.NacoNormalizer;

// If not needed as a fallback KeyGetter, make abstract.
public class ResourceUriGetter {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(ResourceUriGetter.class);

    protected final Resource resource;
    protected final String localNamespace;

    public ResourceUriGetter(Resource resource, String localNamespace) {
        this.resource = resource;
        this.localNamespace = localNamespace;
    }

    // Subclasses that may not generate a URI within the local namespace should
    // override this method. Otherwise, they need only override getUniqueKey().
    public String getUniqueUri() {        
        String uniqueLocalName = getUniqueLocalName();
        return localNamespace + uniqueLocalName;
    }
    
    private final String getUniqueLocalName() {
        String uniqueKey = getUniqueKey();
        LOGGER.debug("Created unique key " + uniqueKey + " for resource "
                + resource.getURI());
        return "n" + getHashCode(uniqueKey);
    }
 
    // Subclasses should call super.getUniqueKey() if they have failed to 
    // identify another key.
    protected String getUniqueKey() {
        
        // Use for entity types that cannot be reconciled either because there
        // is not currently enough data (e.g., Events, where we have only
        // eventPlace and eventDate), or which are always unique (e.g., 
        // Providers). In the latter case, Providers could only be the same
        // if the related Instances were the same. This is (I think) unlikely to
        // occur within a single catalog, and would require a second pass 
        // after the Instances have been reconciled. Don't do that unless it is
        // sufficiently justified.
        return resource.getLocalName();
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
    
    private String getHashCode(String key) {
        // long hash64 = Crc64.checksum(key);
        // long hash64 = Crc64Mod.checksum(key);
        // See https://en.wikipedia.org/wiki/MurmurHash on various MurmurHash
        // algorithms and 
        // http://blog.reverberate.org/2012/01/state-of-hash-functions-2012.html
        // for improved algorithms.There are variants of Murmur Hash optimized 
        // for a 64-bit architecture. 
        long hash64 = MurmurHash.hash64(key);
        return Long.toHexString(hash64);        
    }
    
}
