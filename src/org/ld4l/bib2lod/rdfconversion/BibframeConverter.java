package org.ld4l.bib2lod.rdfconversion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfAuthorityConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfHeldItemConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfInstanceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfLanguageConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfMeetingConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPersonConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfResourceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfTopicConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfWorkConverter;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

/**
 * Converts Bibframe RDF to LD4L RDF.
 * @author rjy7
 *
 */
public class BibframeConverter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(BibframeConverter.class);
      
    private static final Map<BfType, Class<?>> CONVERTERS_BY_TYPE =
            new HashMap<BfType, Class<?>>();
    // Commented out converters are called from another converter, so don't
    // need to be included here. If they get called independently, need to be
    // included in this mapping.
    static {
        CONVERTERS_BY_TYPE.put(BfType.BF_AGENT, BfAuthorityConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_ANNOTATION, 
//                BfAnnnotation.class);  
//        CONVERTERS_BY_TYPE.put(BfType.BF_CATEGORY, 
//                BfCategoryConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_CLASSIFICATION, 
//                BfClassificationConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_EVENT, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_FAMILY, BfAuthorityConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_HELD_ITEM, 
                BfHeldItemConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_IDENTIFIER, 
//                BfIdentifierConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_INSTANCE, BfInstanceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_JURISDICTION,  
                BfAuthorityConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_LANGUAGE, BfLanguageConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_MEETING, BfMeetingConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_ORGANIZATION, 
                BfAuthorityConverter.class);        
        CONVERTERS_BY_TYPE.put(BfType.BF_PERSON, BfPersonConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_PLACE, BfAuthorityConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_PROVIDER, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_RESOURCE, BfResourceConverter.class);
        // Temporal is an Authority subtype, but we don't want the bf:label =>
        // foaf:name conversion for Temporal entities.
        CONVERTERS_BY_TYPE.put(BfType.BF_TEMPORAL, BfResourceConverter.class);
//      CONVERTERS_BY_TYPE.put(BfType.BF_TITLE, BfResourceConverter.class);       
        // Maybe can just use BfAuthorityConverter for Topics?
        CONVERTERS_BY_TYPE.put(BfType.BF_TOPIC, BfTopicConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_WORK,  BfWorkConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_AUTHORITY, 
//                BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_COMPLEX_SUBJECT,  
//                BfResourceConverter.class); 
    }
    
    /* 
     * The types of resources that should be included with the related primary
     * type resource (Work, Instance, Topic, Person, etc.) when building a model 
     * for that resource for ld4l conversion. 
     * Note that this is the complement of the CONVERTERS_BY_TYPE.keySet(). If
     * they have a specific converter, the converter is called from the 
     * converter of the related primary resource, not from here. It's also the
     * set of types that TypeSplitter adds to the file of the primary type.
     * TODO Couldl use this fact to streamline the queries in TypeSplitter and
     * building a subjectModel in getSubjectModelToConvert() below.
     */
    private static final List<Resource> SECONDARY_TYPES = 
            new ArrayList<Resource>();
    static {
        SECONDARY_TYPES.add(BfType.BF_ANNOTATION.ontClass());
        SECONDARY_TYPES.add(BfType.BF_CATEGORY.ontClass());
        SECONDARY_TYPES.add(BfType.BF_CLASSIFICATION.ontClass());
        SECONDARY_TYPES.add(BfType.BF_IDENTIFIER.ontClass());
        SECONDARY_TYPES.add(BfType.BF_PROVIDER.ontClass());
        SECONDARY_TYPES.add(BfType.BF_TITLE.ontClass());
        SECONDARY_TYPES.add(BfType.MADSRDF_AUTHORITY.ontClass());   
    }

    public BibframeConverter(OntModel bfOntModelInf,        
            String localNamespace, String inputDir, String mainOutputDir) {
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
    }

    public BibframeConverter(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);
    }

    
    @Override
    public String process() {

        Instant processStart = Instant.now();
        LOGGER.info("Start Bibframe RDF conversion.");
        
        String outputDir = getOutputDir();  

        int totalSubjectCount = convertFiles(inputDir, outputDir);

        LOGGER.info("End Bibframe RDF conversion in all input files. Converted " 
                + totalSubjectCount + " "     
                + Bib2LodStringUtils.simplePlural("resource", totalSubjectCount)
                + " . " + TimerUtils.getDuration(processStart));

        return outputDir;        
    }
    
    private int convertFiles(String inputDir, String outputDir) {
        
        File[] inputFiles = new File(inputDir).listFiles();
        int totalSubjectCount = 0;
        
        for ( File file : inputFiles ) {
            totalSubjectCount += convertFile(file);
        }  

        /*
         * Possibly we need other steps here: go back through files to do URI
         * replacements. This entails keeping separate models for statements 
         * that shouldn't undergo URI replacement: e.g., the new statements
         * we create linking ld4l entities to bf entities, where the latter
         * shouldn't undergo URI replacement.
         */
        
        return totalSubjectCount;
    }

    private int convertFile(File file) {
        
        String filename = file.getName();
        String basename = FilenameUtils.getBaseName(filename);
        
        // LOGGER.debug("FILENAME: " + filename);
        
        if (basename.equals(ResourceDeduper.getNewAssertionsFilename())) {
            // LOGGER.debug("Copying new assertions file.");
            copyFile(file);
            return 0;
        }

        BfType typeForFile;
        
        if (basename.equals(ResourceDeduper.getRemainderFilename())) {
//            // LOGGER.debug("Copying remainder file.");
//            copyFile(file);
//            return 0;
            // This file needs further analysis - at least some statements 
            // should be included in other files
            typeForFile = BfType.BF_RESOURCE;            
        } else {
            typeForFile = BfType.typeForFilename(filename);
        }
        
        // Should not happen
        if (typeForFile == null) {
            return 0;
        }

        String outputFile = FilenameUtils.getBaseName(file.toString());
        
        BfResourceConverter converter = getConverter(typeForFile);
              
        Model inputModel = readModelFromFile(file); 

        Instant fileStart = Instant.now();
        LOGGER.info("Start converting Bibframe RDF in file " + filename + ".");
        
        Model outputModel = ModelFactory.createDefaultModel();
        
        // Iterate over the subjects of the input model
        ResIterator subjects = inputModel.listSubjects();
        
        Instant subjectStart = Instant.now();
        int subjectCount = 0;
        int subjectCountForFile = 0;
        // int subjectCountToWrite = 0;
        
        while (subjects.hasNext()) {

            Resource inputSubject = subjects.nextResource();
            
            // LOGGER.debug("Found subject " + inputSubject.getURI());

            // Statements about other, "secondary" subjects of other types that 
            // have been  included in this file have distinct converters that 
            // are called from the primary subject's converter. They are 
            // converted with the primary subject because some of these  
            // assertions are needed for or incorporated into conversion of the 
            // primary subject. Examples: Work Titles, Instance Titles and
            // Identifiers.
            if (inputSubject.hasProperty(RDF.type, typeForFile.ontClass()) ||
                    // For file other.nt - convert any subject
                    typeForFile.equals(BfType.BF_RESOURCE)) {
                
                subjectCount++;
                subjectCountForFile++;
                // subjectCountToWrite++;
                
                // LOGGER.debug("Converting " + typeForFile + " subject " 
                //         + inputSubject.getURI());
                
                // Get the statements about this subject and related objects
                // that must be converted together.
                Resource subject = getSubjectModelToConvert(inputSubject);

                // appendModelToFile(converter.convert(subject), outputFile);
                
                outputModel.add(converter.convert(subject));

//                if (subjectCountToWrite >= 1000) {
//                    LOGGER.debug("Appending RDF for " + subjectCountToWrite                              
//                            + " " + Bib2LodStringUtils.simplePlural(
//                                    "resource", subjectCountToWrite)
//                            + " to file.");
//                    appendModelToFile(outputModel, outputFile);
//                    outputModel.removeAll();
//                    subjectCountToWrite = 0;
//                }
                
                if (subjectCount == TimerUtils.NUM_ITEMS_TO_TIME) {
                    LOGGER.info("Converted Bibframe RDF for " + subjectCount 
                            + " " + Bib2LodStringUtils.simplePlural(
                                    "resource", subjectCount)
                            + " in file " + filename + ". " 
                            + TimerUtils.getDuration(subjectStart));
                    subjectCount = 0;
                    subjectStart = Instant.now();
                }   
                
            } else {
                
                // LOGGER.debug("Skipping subject " + inputSubject.getURI()
                //        + " since it's not of type " + typeForFile);                        
            }
        }
        
        if (subjectCount > 0) {
            LOGGER.info("Converted Bibframe RDF for " + subjectCount + " " 
                    + Bib2LodStringUtils.simplePlural("resource", subjectCount)
                    + " in file " + filename + ". " 
                    + TimerUtils.getDuration(subjectStart));  
        }
       
        writeModelToFile(outputModel, outputFile);   
         
//        if (subjectCountToWrite > 0) {
//            LOGGER.info("Appending RDF for " + subjectCountToWrite + " "                              
//                    + Bib2LodStringUtils.simplePlural(
//                            "resource", subjectCountToWrite)
//                    + " to file.");
//            appendModelToFile(outputModel, outputFile);  
//        }
        
        LOGGER.info("Done converting Bibframe RDF in file " + filename 
                + ". Processed " + subjectCountForFile + " " 
                + Bib2LodStringUtils.simplePlural(
                        "resource", subjectCountForFile)
                + " of type " + typeForFile.uri()
                + ". " + TimerUtils.getDuration(fileStart));
        
        return subjectCountForFile;
    }

    
    /*
     * Construct a subset of the input model that pertains to the specified 
     * resource. This includes statements where the resource is the subject
     * or the object, as well as some statements in which the related objects 
     * and subject are subjects.
     * Note that the available statements have already been constrained by 
     * TypeSplitter, since some statements that meet the criteria below have
     * already been written to different files.
     */
    public static Resource getSubjectModelToConvert(Resource resource) {
            
        // LOGGER.debug("Constructing subject model for " + resource.getURI());
        Model inputModel = resource.getModel();
        
        // Create the new model of statements related to this subject
        Model modelForSubject = ModelFactory.createDefaultModel();
        
        // Start with statements of which this subject is the subject
        List<Statement> resourceAsSubjectStmts = 
                resource.listProperties().toList();
        modelForSubject.add(resourceAsSubjectStmts);
    
        /* 
         * If the object of one of these statements is of a "secondary"  
         * type, add statements with the object as subject to the model. 
         * I.e., DON'T include statements about a work which, is, say the 
         * object of a :work bf:relatedWork :work statement - these should
         * be handled when that work is converted (i.e., on another 
         * iteration of the main loop over subjects in the input model. DO 
         * include statements about Identifiers, Titles, Annotations, etc. 
         * which are objects of statements about the current subject 
         * resource. Note that the statements of this sort that are 
         * included in the current inputModel have already been constrained 
         * in TypeSplitter.
         */ 
        for (Statement stmt : resourceAsSubjectStmts) {
            modelForSubject.add(getRelatedResourceStmts(stmt.getObject()));              
        }                
        
        // Then add statements with the current resource as object. Examples:
        // In BfLanguageConverter: :work bf:language :language
        // In BfWorkConverter: :annotation bf:annotates :work
        List<Statement> resourceAsObjectStmts = 
                inputModel.listStatements(null, null, resource).toList();
        
        if (! resourceAsObjectStmts.isEmpty()) {
            modelForSubject.add(resourceAsObjectStmts);

            /* 
             * If the subject of one of these statements is of a "secondary"  
             * type, add statements with the subject as subject to the model. 
             * I.e., DON'T include statements about a work which, is, say the 
             * object of a :work bf:relatedWork :work statement - these should
             * be handled when that work is converted (i.e., on another 
             * iteration of the main loop over subjects in the input model. DO 
             * include statements about Identifiers, Titles, Annotations, etc. 
             * which are objects of statements about the current subject 
             * resource. Note that the statements of this sort that are 
             * included in the current inputModel have already been constrained 
             * in TypeSplitter.
             */           
            for (Statement stmt : resourceAsObjectStmts) {
                modelForSubject.add(getRelatedResourceStmts(stmt.getSubject()));              
            }  
        }
        
        // Other converters will not need to process these statements, so they 
        // can be removed from the input model.
        inputModel.remove(modelForSubject);
           
        // NB At this point, resource.getModel() is the inputModel, not 
        // the modelForSubject. Get the subject of the modelForSubject 
        // instead.            
        return modelForSubject.getResource(resource.getURI());               
    } 
    
    /* 
     * RDFNode node is a node related to the current subject, either by a 
     * statement where the subject is the subject and the node is the object, 
     * or the reverse. If this node is of a "secondary" type, such as an
     * Identifier, Annotation, Classification, Title, Provider, etc., add 
     * statements in which it is the subject to the model, since these 
     * statements should be converted with the primary subject.
     * 
     * TODO Consider: this complication may be eliminated if we were to do
     * a second type-splitting after resource deduping, putting secondary
     * types in their own files. The question is whether the secondary types
     * can be converted on their own, without information about their related
     * resource of primary type. This might not always be possible - e.g., 
     * see how the Instance bf:titleStatement is eliminated if another Title
     * with the same label exists. That requires having all the Instance's
     * title information together when converting the Instance.
     */
    private static Model getRelatedResourceStmts(RDFNode node) {
            
        Model model = ModelFactory.createDefaultModel();
        
        if (node.isResource()) {
            Resource resource = node.asResource();
            StmtIterator typeStmts = resource.listProperties(RDF.type);
            while (typeStmts.hasNext()) {
                Resource type = typeStmts.nextStatement().getResource();
                if (SECONDARY_TYPES.contains(type)) {
                    model.add(resource.listProperties());     
                    break;
                }
            }
        }           
        return model;
    }
   
    private BfResourceConverter getConverter(BfType bfType) {
        
        Class<?> converterClass = CONVERTERS_BY_TYPE.get(bfType);
        if (converterClass != null) {
            try {
                BfResourceConverter converter = 
                        (BfResourceConverter) converterClass
                        .getConstructor(BfType.class, String.class)
                        .newInstance(bfType, localNamespace);   
                // LOGGER.debug("Got converter for type " + bfType);
                return converter;
            } catch (Exception e) {
                LOGGER.warn("Can't instantiate class " 
                        + converterClass.getName());
                e.printStackTrace();
            } 
        }

        // LOGGER.debug("No converter found for type " + bfType);
        return null;
    }

}
