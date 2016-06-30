package com.bredelet.mistigri;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Engine extends Thread
{
    TemplateReader input;
    PipedWriter output;
    String openBrace;
    Pattern closeBrace;
    Logger log;
    
    Engine(TemplateReader input, PipedReader sink, Mistigri control, Map<String, Object> config) throws java.io.IOException {
        this.input = input;
        this.output = new PipedWriter(sink);
        this.openBrace = (String) control.getOption("openBrace", config);
        String closeBraceText = (String) control.getOption("closeBrace", config);
        this.closeBrace = Pattern.compile(closeBraceText, Pattern.LITERAL);
        this.log = control.log;
    }
    
    public void run() {
        try {
            output.write(input.readPart());
            while (true)
            {
                String part = input.readPart();
                if (part == null) break;
                String[] mitext = closeBrace.split(part, 2);
                String mistigri = mitext[0];
                String text = (mitext.length > 1) ? mitext[1] : "";
                log.info("action=" + parseAction(mistigri));
                output.write("{{Mistigri}}");
                output.write(text);
            }
        }
        catch (java.io.IOException e) {
            log.warning(e.getLocalizedMessage());
        }
        finally {
            try {
                output.close();
            }
            catch (Exception e) {
                log.warning(e.getLocalizedMessage());
            }
        }
    }
    
    private static final Pattern parser = Pattern.compile("^\\s*(\\S+)\\s*(.*)", Pattern.DOTALL);

    private String parseAction(String tag) {
        Matcher them = parser.matcher(tag);
        if (!them.matches()) return "";
        return them.group(1);
    }
}
