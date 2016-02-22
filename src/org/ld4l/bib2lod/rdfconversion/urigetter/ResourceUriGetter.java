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
        return "n" + getHashCode(uniqueKey);
    }
 
    // Subclasses should call super.getUniqueKey() if they have failed to 
    // identify another key.
    protected String getUniqueKey() {
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
