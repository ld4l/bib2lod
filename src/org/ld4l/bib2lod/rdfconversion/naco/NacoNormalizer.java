package org.ld4l.bib2lod.rdfconversion.naco;

/**
 * Normalize authority name according to NACO authority name comparison rules
 * for identity matching.
 * 
 * NACO references:
 * http://www.loc.gov/aba/pcc/naco/normrule-2.html
 * http://labs.oclc.org/nacotestbed
 * This one may be outdated:
 * http://www.oclc.org/content/dam/research/publications/library/2006/naco-lrts.pdf
 * 
 */

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NacoNormalizer {

    private static final Logger LOGGER =          
            LogManager.getLogger(NacoNormalizer.class);

    // static final private char DEFAULT_SUBFIELD_SEPARATOR = 0x1F;
    
    static final private char[] DELETE = {
        '\'', 
        '[', 
        ']',
        '\u044a', // lowercase Cyrillic hard sign
        '\u044c', // uppercase Cyrillic soft sign
        '\u02be', // Romanized alif
        '\u02bb', // Romanized ayn 
        
        /* Combining diacritics 
         * Characters marked with * - not to be confused with non-combining 
         * (spacing) character equivalents.
         * NB Not sure why some diacritics are included and not others. Is this
         * deliberate or omission?
         * Couldn't find: pseudo question mark, right cedilla
         */
        '\u0301', // acute
        '\u0306', // breve
        '\u0310', // candrabindu       
        '\u0327', // cedilla
        '\u212b', // angstrom
        '\u0325', // circle below         
        '\u0302', // circumflex*
        '\u0323', // dot below
        '\u0300', // grave*    
        '\u030b', // double acute
        '\u0360', // double tilde
        '\u0333', // double underscore
        '\u030c', // hacek (caron)
        '\u0313', // high comma centered (comma above)
        '\u0325', // high comma off-center (comma above right)
        '\u0309', // hook above (? NACO has left hook)
        '\ufe20', // ligature left half
        '\ufe21', // ligature right half
        '\u0304', // macron
        '\u0328', // right hook, ogonek
        '\u0307', // dot above (superior dot)
        '\u0303', // tilde*
        '\u0308', // umlaut, diaresis
        '\u0332', // underscore*
        '\u1cf6', // upadhmaniya       
    };
    
    static final private char[] TO_WHITESPACE = {
        '|', 
        '\u00b7', // middle dot
        '!',
        '"',
        '(',
        ')',
        '-',
        '{',
        '}',
        '<',
        '>',
        ';',
        ':',
        '.',
        '?',    
        '\u00bf', // inverted question mark
        '\u00a1', // inverted exclamation mark
        '/',
        '\\',
        '*',
        '%',
        '=',
        '+',
        '\u207a', // superscript plus
        '\u207b', // superscript minus
        '\u00ae', // patent mark
        '\u24c5', // sound recording copyright 
        '\u00a9', // copyright symbol
        '\u00b0', // degree sign
        '\u005e', // circumflex
        '\u005f', // spacing underscore
        '\u0060', // spacing grave
        '\u007e', // spacing tilde 
    };

    static final private Map<Character, String> CONVERT = 
            new HashMap<Character, String>();
    static {
        CONVERT.put('\u2070', "0");  // superscript 0
        CONVERT.put('\u2080', "0");  // subscript 0
        CONVERT.put('\u2071', "1");  // superscript 1
        CONVERT.put('\u2081', "1");  // subscript 1
        CONVERT.put('\u2072', "2");  // superscript 2
        CONVERT.put('\u2082', "2");  // subscript 2
        CONVERT.put('\u2073', "3");  // superscript 3
        CONVERT.put('\u2083', "3");  // subscript 3
        CONVERT.put('\u2074', "4");  // superscript 4
        CONVERT.put('\u2084', "4");  // subscript 4
        CONVERT.put('\u2075', "5");  // superscript 5
        CONVERT.put('\u2085', "5");  // subscript 5
        CONVERT.put('\u2076', "6");  // superscript 6
        CONVERT.put('\u2086', "6");  // subscript 6
        CONVERT.put('\u2077', "7");  // superscript 7
        CONVERT.put('\u2087', "7");  // subscript 7
        CONVERT.put('\u2078', "8");  // superscript 8
        CONVERT.put('\u2088', "8");  // subscript 8
        CONVERT.put('\u2079', "9");  // superscript 9
        CONVERT.put('\u2089', "9");  // subscript 9
        CONVERT.put('\u00c6', "AE"); // uppercase ae digraph
        CONVERT.put('\u0152', "OE"); // uppercase oe digraph
        CONVERT.put('\u0110', "D");  // uppercase d with crossbar
        CONVERT.put('\u00d0', "TH"); // uppercase eth
        CONVERT.put('\u0131', "I");  // uppercase Turkish i 
        CONVERT.put('\u2113', "L");  // script l
        CONVERT.put('\u0141', "L");  // uppercase Polish l
        CONVERT.put('\u01a0', "O");  // uppercase o hook
        CONVERT.put('\u01af', "U");  // uppercase u hook
        CONVERT.put('\u00d8', "O");  // uppercase Scandinavian o
        CONVERT.put('\u00de', "TH"); // uppercase Icelandic thorn
        CONVERT.put('\u00df', "SS"); // eszett  
    }
        
    static public String normalize(String s) {
        
        StringBuilder sb = new StringBuilder();
        
        // Convert to uppercase first in order to simplify the CONVERT map. 
        // NACO rules call for conversion to uppercase anyway.
        s = s.toUpperCase();
        
        CharacterIterator it = new StringCharacterIterator(s);
        boolean firstCommaFound = false;
        for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
            if (ArrayUtils.contains(DELETE, c)) {
              // do nothing
            } else if (ArrayUtils.contains(TO_WHITESPACE, c)) {
                sb.append(' ');
            } else if (CONVERT.containsKey(c)) {
                sb.append(CONVERT.get(c));
            } else if (c == ',') {
                if (!firstCommaFound) {
                    sb.append(c);
                    firstCommaFound = true;
                } else {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        }
        
        String normalized = sb.toString();
        
        // Collapse sequences of whitespace
        normalized = normalized.replaceAll("\\s+", " ");
        
        // Trim leading and trailing whitespace
        normalized = StringUtils.strip(normalized);
        
        return normalized;
    }
    
}
