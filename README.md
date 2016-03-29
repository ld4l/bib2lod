# bib2lod
Bib2Lod is a Java application that converts BIBFRAME RDF to LD4L LOD.
```
usage: java -jar Bib2Lod.jar -a <action> -i <input_directory> -n
       <local_namespace> [-ne] [-np] -o <output_directory>
       
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
