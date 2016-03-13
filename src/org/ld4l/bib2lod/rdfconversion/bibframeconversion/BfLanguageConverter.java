package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfLanguageConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfLanguageConverter.class);
    
    
    private static final ParameterizedSparqlString langOfPartPss = 
            new ParameterizedSparqlString(
                    "SELECT ?work ?langResource ?langLit "
                    // Not dealing with bf:resourcePart for now. See notes
                    // below.
                    // + "?resPart "
                    + "WHERE { { "
                    + "?work " + BfProperty.BF_LANGUAGE.sparqlUri() 
                    + " ?lang . "
                    + "?lang " 
                    + BfProperty.BF_LANGUAGE_OF_PART_URI.sparqlUri() + " "
                    + "?langResource .  "
                    + "} UNION { "
                    + "?work " + BfProperty.BF_LANGUAGE.sparqlUri() 
                    + " ?lang . "
                    + "?lang " 
                    + BfProperty.BF_LANGUAGE_OF_PART.sparqlUri() + " "
                    + "?langLit . "                   
                    + "} } ");
    
    
    public BfLanguageConverter(String localNamespace) {
        super(localNamespace);
    }
        
    @Override
    protected Model convert() {

        convertLanguageOfPart();
        return super.convert();  
    }

    /*
     * This is an attempt to roughly model Steven's comments, but is not final.
     * See TODOs below.
     * 
     * <http://draft.ld4l.org/cornell/187> <http://bibframe.org/vocab/language> <http://id.loc.gov/vocabulary/languages/eng> . 
     * <http://draft.ld4l.org/cornell/187> <http://bibframe.org/vocab/language> <http://draft.ld4l.org/cornell/187language9> . 
     * <http://draft.ld4l.org/cornell/187language9> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://bibframe.org/vocab/Language> . 
     * <http://draft.ld4l.org/cornell/187language9> <http://bibframe.org/vocab/resourcePart> "original" . 
     * <http://draft.ld4l.org/cornell/187language9> <http://bibframe.org/vocab/languageOfPartUri> <http://id.loc.gov/vocabulary/languages/ger> . 
     * 
     * According to the RDF above, the converter is trying to say not only 
     * that the English was translated from the German, but also this work 
     * contains both the English and German, at least according to the 
     * bf:resourcePart property. The problem is that the converter seems to 
     * only be using the 041 first indicator ‘1’, which means that the "Item 
     * is or includes a translation" 
     * (http://www.loc.gov/marc/bibliographic/bd041.html). The converter, 
     * instead, should be testing if there is a first indicator ‘1’, then 
     * look to see if the $a’s and $h’s have the same language code. $a’s 
     * are for languages contained in the work in hand and are repeatable. 
     * $h is for the original language. That gets us half way there. 
     * Then we need to look for a 240. This is where (if the original is 
     * referred to explicitly in the record) we’ll find it. What it will 
     * look like is this: 
     * https://newcatalog.library.cornell.edu/catalog/2360735/librarian_view
     * So in this case we have a French translation of a text originally 
     * written in English, but the text we have doesn’t include the 
     * original. If it did, the 041 would look like this:
     * 041 1 $a fre $a eng $h eng
     * (eng code repeated in both $a and $h)
     * If there is a 240 the converter should create a separate Work. If 
     * we're interested in consistent semantics, and there isn’t a 240, 
     * then I  think we could/should still have a URI for the original work 
     * even if all we know about it is the original language.
     */

    private void convertLanguageOfPart() {
        
        langOfPartPss.setIri("lang", subject.getURI());

        LOGGER.debug(langOfPartPss.toString());
        Query query = langOfPartPss.asQuery();
        LOGGER.debug(query.toString());
        QueryExecution qexec = QueryExecutionFactory.create(
                query, subject.getModel());
        ResultSet results =  qexec.execSelect();

        while (results.hasNext()) {
            
            QuerySolution soln = results.next();
            Resource work = soln.getResource("work");
            
            // Create a new work which is part of the original work. The new
            // work has the language which is the object of 
            // bf:languageOfPart(Uri).
            // TODO Rather than creating a new work, we should first look in
            // the catalog to see if the work already exists. Match on 
            // Bibframe language string value.
            Resource newWork = outputModel.createResource(
                    RdfProcessor.mintUri(localNamespace));
            outputModel.add(newWork, RDF.type, Ld4lType.WORK.type());
            outputModel.add(work, Ld4lProperty.HAS_PART.property(), newWork);
            
            Resource langResource;
            
            if (soln.contains("langResource")) {
                langResource = soln.getResource("langResource");
                
            } else {
                Literal langLiteral = soln.getLiteral("langLit");
                
                // TODO Rather than generating a random URI for the new 
                // language, and keeping the string as a label, we should 
                // reconcile the string to an existing language term in the
                // vocabulary of choice.
                langResource = outputModel.createResource(
                        RdfProcessor.mintUri(localNamespace));
                outputModel.add(
                        langResource, RDF.type, Ld4lType.LANGUAGE.type());
                // Note sure if rdfs:label is the property we want here.
                outputModel.add(langResource, Ld4lProperty.LABEL.property(),
                        langLiteral);           
            }

            outputModel.add(newWork, Ld4lProperty.HAS_LANGUAGE.property(),
                    langResource);

            // TODO Currently ignoring the resourcePart value, which can be
            // either "original" or "summary or abstract." This should be used
            // to further specify the hasPart relationship between the original
            // work and the new work.
        }
        
        qexec.close();        
    }
 
}
