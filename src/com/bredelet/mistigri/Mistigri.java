package com.bredelet.mistigri;

import java.io.Reader;
import java.io.PipedReader;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

public class Mistigri
{
    public Options options = new Options();
    Logger log;

    public Mistigri() {
        this(null, null);
    }

    public Mistigri(Logger logger) {
        this(null, logger);
    }
    
    public Mistigri(Map<String, Object> config, Logger logger) {
        log = (logger == null) ? Logger.getLogger("mistigri") : logger;
        options.putAll(defaults);
        if (config != null && config.size() <= defaults.size())
        {
            options.putAll(config);
        }
        else if (config != null)
        {
            // Don't spam Mistigri!
            log.warning("Invalid option set, skipping.");
        }
    }
    
    public static class Options extends HashMap<String, Object>
    {
        Options() {
            super(defaults.size());
        }
    }
    
    private static final Map<String, Object> defaults = new HashMap<String, Object>();
    static {
        defaults.put("openBrace", "{{");
        defaults.put("closeBrace", "}}");
        defaults.put("placeholder", "N/A");
        defaults.put("methodCall", false);
        defaults.put("escapeFunction", null);
        defaults.put("reader", null);
    }
    
    Object getOption(String name, Map<String, Object> config) {
        return (config != null && config.containsKey(name)) ? config.get(name) : options.get(name);
    }

    public Reader prrcess(Reader template, Map<String, Object> model) throws java.io.IOException {
        return prrcess(template, model, null);
    }
    
    public Reader prrcess(Reader template, Map<String, Object> model, Map<String, Object> config) throws java.io.IOException {
        String openBrace = (String) getOption("openBrace", config);
        TemplateReader input;
        if (template instanceof TemplateReader && ((TemplateReader) template).separator == openBrace)
        {
            input = (TemplateReader) template;
        }
        else
        {
            input = new TemplateReader(template, openBrace);
        }
        PipedReader result = new PipedReader();
        new Engine(input, result, this, config).start();
        return result;
    }
    
    public static void main(String[] args) throws Exception {
        Reader r = new Mistigri().prrcess(new java.io.InputStreamReader(System.in), null);
        java.io.BufferedReader br = new java.io.BufferedReader(r);
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            System.out.println(line);
        }
    }
}
