package org.ld4l.bib2lod.processor.deduper;

import java.io.File;

import org.ld4l.bib2lod.processor.Processor;


public class Deduper extends Processor {

    public Deduper(String localNamespace, String rdfFormat, File inputDir, 
            File outputDir, File logDir) {
        super(localNamespace, localNamespace, inputDir, inputDir, inputDir);
        // TODO Auto-generated constructor stub
    }
    
}