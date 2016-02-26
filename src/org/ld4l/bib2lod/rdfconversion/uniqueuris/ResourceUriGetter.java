package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import java.util.List;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.OntNamespace;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.util.MurmurHash;
import org.ld4l.bib2lod.util.NacoNormalizer;

// If not needed as a fallback KeyGetter, make abstract.
public class ResourceUriGetter {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(ResourceUriGetter.class);
    
    // Default resource submodel is all the statements in which the resource is
    // either the subject or the object.
    private static ParameterizedSparqlString resourceSubModelPss = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?resource ?p1 ?o . "
                    + " ?s ?p2 ?resource . "                
                    + "} WHERE {  { "                                                      
                    + "?resource ?p1 ?o . "
                    + "} UNION { "
                    + "?s ?p2 ?resource . "
                    + "} } ");

    protected final Resource resource;
    protected final String localNamespace;

    public ResourceUriGetter(Resource resource, String localNamespace) {
        Model resourceSubModel = getResourceSubModel(resource);
        this.resource = resourceSubModel.createResource(resource.getURI());
        this.localNamespace = localNamespace;
    }

    // Subclasses that may not generate a URI within the local namespace should
    // override this method. Otherwise, they need only override getUniqueKey().
    public String getUniqueUri() {        
        String uniqueLocalName = getUniqueLocalName();
        return localNamespace + uniqueLocalName;
    }
    
    protected ParameterizedSparqlString getResourceSubModelPss() {
        return resourceSubModelPss;
    }

    /*
     * Get the submodel of the input model consisting of statements in which 
     * this resource is either the subject or object.
     */
    // **** TODO The type of the resource may determine the query. For example,
    // with instances we need info about identifiers, etc. So we need to 
    // get the type first, then get the resource sub model from the uri getter,
    // with the default in ResourceUriGetter and subclasses overriding where
    // needed. ****
    // Also, if the input files are small enough, there may not be a need to
    // get the submodel, rather than just querying the entire input model. It's
    // not clear whether there would be a performance difference and in which
    // direction. 
    private Model getResourceSubModel(Resource resource) {

        LOGGER.debug("Getting resource submodel for " + resource.getURI());
        
        ParameterizedSparqlString pss = getResourceSubModelPss();
        pss.setNsPrefix(OntNamespace.BIBFRAME.prefix(),
                OntNamespace.BIBFRAME.uri());
        pss.setIri("resource", resource.getURI());
        
        Query query = pss.asQuery();
        LOGGER.debug(query.toString());
        QueryExecution qexec = QueryExecutionFactory.create(
                query, resource.getModel());
        Model resourceSubModel =  qexec.execConstruct();
        qexec.close();
        
        RdfProcessor.printModel(resourceSubModel, 
                "Submodel for resource " + resource.getURI());
        
        return resourceSubModel;

        /*
         * Experiments with inference models. 
         * 
         * Bibframe only contains RDFS-level axioms, and that is all we need
         * here (subclass, subproperty, and domain/range inferencing).
         * 
         * In the first scenario, we create an inference model for the entire 
         * file, pass it here, and query it to build the submodel. This results
         * in a small submodel that contains only the assertions and inferences
         * related to the resource. This is the approach we would want.
         * 
         * In the second scenario, we build the construct model as above by 
         * querying the input model, then construct an inference model from it.
         * This results in a large submodel that contains the ontology and all
         * the inferences on it, in addition to assertions and inferences 
         * related to the resource.
         * 
         * For example, with an input model for one resource containing 7
         * assertions, the final submodel in scenario 1 is 9 statements, while
         * the final submodel in scenario 2 is a few thousand statements.
         * 
         * NB Sizes of inference models reported by InfModel.size() are not 
         * accurate. It is necessary to look at the actual statements as output 
         * by RdfProcessor.printModel().
         * 
         * Inferences from the Bibframe ontology are not always reliable. For
         * example, consider:
         <rdf:Property rdf:about="http://bibframe.org/vocab/subject">
             <rdfs:domain rdf:resource="http://bibframe.org/vocab/Work"/>
             <rdfs:label>Subject</rdfs:label>
             <rdfs:range rdf:resource="http://bibframe.org/vocab/Authority"/>
             <rdfs:range rdf:resource="http://bibframe.org/vocab/Work"/>
             <rdfs:comment>Subject term(s) describing a resource.</rdfs:comment>
         </rdf:Property>
         * Now every object of bf:subject is inferred to be a Work. The object 
         * of the range assertion should be the UNION of bf:Authority and 
         * bf:Work, but of course since Bibframe is not an OWL ontology it 
         * cannot use unionOf. 
         *
         LOGGER.debug("Submodel with no inferencing: ");
         LOGGER.debug("Submodel size: " + resourceSubModel.size());
         RdfProcessor.printModel(resourceSubModel, "Resource submodel: ");
        
         QueryExecution qexecInf = QueryExecutionFactory.create(
                 query, infModel);
         Model resourceSubModel1 = qexecInf.execConstruct();  
         qexecInf.close();
         LOGGER.debug("Submodel built from querying the inference model that "
                 + "is based on the entire file:");
         LOGGER.debug("Submodel size: " + resourceSubModel1.size());
         RdfProcessor.printModel(resourceSubModel1, "Resource submodel:");

         InfModel resourceSubModel2 = ModelFactory.createInfModel(
                 ReasonerRegistry.getRDFSReasoner(), bfOntModel, 
                 resourceSubModel);
         LOGGER.debug("Submodel built from creating an inference model based on "
                 + "the submodel built from querying the input model:");
         LOGGER.debug("Submodel size: " + resourceSubModel2.size());
         RdfProcessor.printModel(resourceSubModel2, "Resource submodel: ");
         */
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
        // if the related Instances were the same. This is (maybe) unlikely to
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
