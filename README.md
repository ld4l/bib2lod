# bib2lod
Bib2Lod is a Java application that converts BIBFRAME 1.0 RDF generated by the <a href="https://github.com/lcnetdev/marc2bibframe">LC marc2bibframe converter</a> to LD4L LOD. It uses the Jena API to tranform RDF from the BIBFRAME 1.0 data model to the LD4L data model.

The converter is in a preliminary stage, and there are bugs as well as non-implemented features. Complete documentation of known bugs and desired additional features is forthcoming.

#### The LD4L bibliographic ontology ####

The LD4L bibliographic ontology is based on Rob Sanderson's and LD4L's recommended changes to BIBFRAME 1.0 to better reflect best practices in the Linked Data domain. Additional information on the LD4L ontology can be found at:

Overview of the ontology, including rationale, motivation, and development process: http://ld4l.org/ontology-overview<br />
Ontology specification: http://bib.ld4l.org/ontology.rdf<br />
Human-readable documentation: http://bib.ld4l.org/ontology.html<br />

Note: Towards the end of the development of the LD4L ontology, the Library of Congress began to incorporate a number of elements of the Sanderson report into a <a href="https://www.loc.gov/bibframe/docs/index.html">series of proposals for BIBFRAME 2.0</a>. Future versions of this LD4L converter may target BIBFRAME 2.0 if a stable version is released, or a hybrid version of BIBFRAME 2.0 and LD4L.

Both BIBFRAME 1.0 and LD4L ontologies are included in the /rdf directory of the repository.

#### Processing stages ####

NB: Each processing stage assumes that previous stages have been applied. See notes below on commandline options specifying actions and prerequisites.

##### RDF cleanup #####

Because the subsequent stages of the converter use Jena Models, the RDF from the marc2bibframe converter goes through an initial cleanup step to avoid Jena exceptions. When Jena throws an exception when reading an RDF file into a Model, the entire file is discarded, so this cleanup step, while not encompassing all cases of invalid RDF, prevents some loss of data during subsequent processing.

##### URI deduping #####

The marc2bibframe converter processes records as atomic units. If the same entity occurs in multiple records, each occurrence will receive a different URI. The LD4L converter reconciles multiple URIs that point at the same individual, or what is presumed to be the same individual. This phase applies fairly naive string matching techniques on uniquely identifying data, which varies according to the type of entity. In this preliminary version there is no attempt at fuzzy matching, application of distance algorithms, etc. Some entities provide enough data for accurate reconciliation, while others do not. The converter errs on the side of false negatives; that is, when there is insufficient data to make a confident match, there is no reconciliation, and the entities retain different URIs.

##### RDF conversion #####

This is the heart of the converter, which maps data expressed in the BIBFRAME ontology to the LD4L data model.



#### Usage ####
```
java -jar Bib2Lod.jar -a <action> -i <input_directory> -n <local_namespace>
        [-ne] [-np] -o <output_directory>
       
 -a,--action <action>               Processing action. Valid actions: clean_rdf,
                                    dedupe, convert_bibframe. Can be invoked
                                    more than once to specify multiple actions.
 -i,--indir <input_directory>       Absolute or relative path to directory
                                    containing input files.
 -n,--namespace <local_namespace>   Local HTTP namespace for minting and
                                    deduping URIs.
 -ne,--no_erase                     Keep intermediate output. Default is to
                                    erase.
 -np,--no_prereqs                   Do not add prerequisite actions to those
                                    specified. Allows restarting at intermediate
                                    processing stages. Default is to add
                                    prereqs. This option should be used ONLY
                                    when intermediate output is used as input;
                                    it cannot be used to sidestep processing
                                    steps altogether, since some later steps
                                    assume the application of earlier ones. All
                                    subsequent steps must be specified on the
                                    commandline.
 -o,--outdir <output_directory>     Absolute or relative path to output
                                    directory. Will be created if it does not
                                    exist.
```

#### Details on commandline options ####

##### Actions #####

Three possible actions may be specified, representing the three processing stages outlined above: clean_rdf, dedupe, and convert_bibframe. Because each step depends on application of the previous steps, under normal usage the converter will apply these prerequisites when only one action is specified. That is, if the convert_bibframe action is specified, RDF cleanup and URI deduping will also apply. 

If the -np option is additionally specified, then no prerequisite actions are applied. This allows the converter to be restarted at one step after application of previous steps, without having to restart the entire process. The -np option should be used only in this case, so that a processing step is not fed data it does not expect. In this case, it is possible to specify multiple actions. For example, if I want to run the converter on output from the RDF cleanup process, I can specify:

```
-a dedupe -a convert_bibframe -np

```

##### No erase #####

Each processing step writes out data to files, and the next step reads in those files. As a space-saving measure, the converter normally erases one set of input files once the next processing stage is complete. With the -ne option, intermediate output will be retained.

#### Included jar file ####

The repository includes a jar file so that it is not necessary to build your own. The commit that the jar was built from is indicated in the jar file name.

Apache log4j2 is used for logging. The jar file logs at level INFO, to a log file but not to stdout. For customized logging you need to build the application yourself. Sample log files are included in the repository.
