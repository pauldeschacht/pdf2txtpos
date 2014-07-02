package com.pauldeschacht.pdf2txtpos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;

public class PDF2TxtPos {

    private static final Log LOG = LogFactory.getLog(PDF2TxtPos.class);
    
    protected static int startPage = -1;
    protected static int endPage = -1;
    protected static float lineMargin = 1.5f; //should be based on the height of the font
    
    

    private PDF2TxtPos() {
    };

    public static void usage(Options options) {
        System.out.println("pdf2txtpos extracts the textual information from a PDF file.");
        System.out.println();
        if (options != null) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "pdf2txtpos", options );            
        }
        System.out.println();        
        System.out.println("Each line of the output contains");
        System.out.println("1.  Page number");
        System.out.println("2.  Line number");
        System.out.println("3.  Font name");
        System.out.println("4.  Font size");
        System.out.println("5.  Width of the space character of the font");
        System.out.println("6.  x1 (x1,y1)-(x2,y2) is bounding box of the word");
        System.out.println("7.  x2");
        System.out.println("8.  y1");
        System.out.println("9.  y2");
        System.out.println("10. word");
    }
    
    public static void main(String[] args) throws Exception {
        
        Options options = new Options();
        options.addOption("f", "file",      true, "PDF file");
        options.addOption("d", "directory", true, "directory with PDF files");
        options.addOption("s", "start",     true, "start page (first page is 1)");
        options.addOption("e", "end",       true, "end page");
        options.addOption("h", "height",    true, "height of a line (only needed for fine tuning");
        options.addOption("b", "bottom",    true, "delta bottom line (only needed for fine tuning");

        for(String s:args) {
            System.out.println(s);
        }
        
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse( options, args);
        
        String tmp = cmd.getOptionValue("h");
        if (tmp != null) {
            System.out.println("Height: " + tmp);
            lineMargin = Float.parseFloat(tmp);
        }
        tmp = cmd.getOptionValue("s");
        if (tmp != null) {
            startPage = Integer.parseInt(tmp);
        }
        tmp = cmd.getOptionValue("e");
        if (tmp != null) {
            endPage = Integer.parseInt(tmp);
        }
        tmp = cmd.getOptionValue("b");
        if (tmp != null) {
            float delta = Float.parseFloat(tmp);
            WordPositionComparator.DELTA=delta;
        }
        else {
            WordPositionComparator.DELTA=0.00001f;

        }
        
        String filename = cmd.getOptionValue("f");
        if (filename != null) {
            File file = new File(filename);
            if (file.isFile() && file.getName().endsWith(".pdf")) {
                parseFile(file.getAbsolutePath());
            }
        }
        else {
            String dir = cmd.getOptionValue("d");
            if (dir != null) {
                //process all PDF in the folder
                File[] files = new File(dir).listFiles();
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".pdf")) {
                        parseFile(file.getAbsolutePath());
                    }
                }
            }
            else {
                usage(options);
            }
        }
    }

    protected static void parseFile(String pdfFile) throws Exception {
        String baseFilename = pdfFile.substring(0, pdfFile.lastIndexOf('.'));
        String txtposFilename = baseFilename + ".info";
        File txtposFile = new File(txtposFilename);
        if (!txtposFile.exists()) {
            txtposFile.createNewFile();
        }
        FileWriter txtposFW = new FileWriter(txtposFile.getAbsoluteFile());
        BufferedWriter txtposWriter = new BufferedWriter(txtposFW);

        LOG.info("Processing file " + pdfFile);
        
        PDDocument document = null;
        try {
            // extract the grid lines from the pdf
            document = PDDocument.load(pdfFile);

            // process page by page
            List pages = document.getDocumentCatalog().getAllPages();
            
            startPage = Math.max(startPage,1);
            if(endPage==-1) {
                endPage = pages.size();
            }
            endPage = Math.min(endPage,pages.size());
                
            for (int pageNb = startPage; pageNb <= endPage; pageNb++) {
                // extract the words and their positions from the page
                PDFWordPositionStripper wordPositionStripper = new PDFWordPositionStripper();
                wordPositionStripper.setStartPage(pageNb);
                wordPositionStripper.setEndPage(pageNb);
                wordPositionStripper.getText(document);
                List<WordPosition> words = wordPositionStripper.getWordPositions();
                //sort on bottom line of each bounding box 
                //and assign a line number based on that bottom line
                Collections.sort(words, new WordPositionComparator());
                float lineY = words.get(0).y1();
                int lineNb = 0;
                for (WordPosition word : words) {
                    float y = word.y1();
                    if (Math.abs(lineY - y) > lineMargin) {
                        lineNb++;
                        lineY = y;
                    }
                    //to gradually slide the line position: lineY = y;
                    word.setLineNb(lineNb);
                }
                //sort the words according line number
                Collections.sort(words, new WordPositionLineComparator());
                // build a map so that all the word in a single line can be accessed.
                // csv file: page, line, x1,y1,x2,y2, word
                Map<Integer, List<WordPosition>> lines = new HashMap<Integer, List<WordPosition>>();
                for (WordPosition word : words) {
                    Integer line = word.getLineNb();
                    if (lines.containsKey(line) == true) {
                        lines.get(line).add(word);
                    } else {
                        lines.put(line, new ArrayList<WordPosition>());
                        lines.get(line).add(word);
                    }
                }
                //Sometimes a space is used as thousand separator. 
                //Collapse the 2 words together in a single word
                for(Map.Entry<Integer,List<WordPosition>> kv: lines.entrySet()) {
                    List<WordPosition> line = kv.getValue();
                    
                    int i=0;
                    while(i<line.size()-1) {
                        WordPosition w1 = line.get(i);
                        WordPosition w2 = line.get(i+1);
                        if(w1.isNumber() && w2.isNumber() && (w1.x2() + (1.5f * w1.getSpaceWidth()) > w2.x1())) {
                            //if both words are numbers and the space between the words is small (bit more than space width) --> merge the 2 words
                            w1.merge(w2);
                            line.remove(i+1);
                        }
                        else {
                            i++;
                        }
                    }
                }
                
                for (WordPosition word : words) {
                    txtposWriter.write(Integer.toString(pageNb) + ";");
                    txtposWriter.write(Integer.toString(word.getLineNb()) + ";");
                    txtposWriter.write(word.toString());
                    txtposWriter.write("\n");
                }
                txtposWriter.flush();
            }
        }
        catch (IOException e) {
            LOG.error(e);
        } finally {
            if (document != null) {
                document.close();
            }
        }                
    } 
}