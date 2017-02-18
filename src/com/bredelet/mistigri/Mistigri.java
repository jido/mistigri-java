package com.bredelet.mistigri;

import java.io.Reader;
import java.io.Writer;
import java.io.PipedWriter;
import java.io.PipedReader;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.function.Function;

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
    
    private static String htmlEscape(String s) {
        if (s == null || s.length() == 0) 
        {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;"); 
                    break;
                case '\'':
                    sb.append("&#39;"); 
                    break;
                case '/':
                    sb.append("&#47;"); 
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
    
    
    private static final Map<String, Object> defaults = new HashMap<String, Object>();
    static {
        defaults.put("openBrace", "{{");
        defaults.put("closeBrace", "}}");
        defaults.put("placeholder", "N/A");
        defaults.put("methodCall", false);
        defaults.put("escapeFunction", (Function<String, String>) text -> htmlEscape(text));
        defaults.put("reader", null);
    }
    
    Object getOption(String name, Map<String, Object> config) {
        return (config != null && config.containsKey(name)) ? config.get(name) : options.get(name);
    }

    public Reader prrcess(Reader template, Map<String, Object> model) throws java.io.IOException {
        return prrcess(template, model, null);
    }
    
    public Reader prrcess(Reader template, Map<String, Object> model, Map<String, Object> config) throws java.io.IOException {
        PipedReader result = new PipedReader();
        process(template, new PipedWriter(result), model, config);
        return result;
    }

    public void process(Reader template, Writer output, Map<String, Object> model) throws java.io.IOException {
        process(template, output, model, null);
    }
    
    public void process(Reader template, Writer output, Map<String, Object> model, Map<String, Object> config) throws java.io.IOException {
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
        new Engine(input, model, output, this, config).start();
    }
    
    public static void main(String[] args) throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("raw", 20);
        model.put("comment", 0);
        new Mistigri().process(new java.io.InputStreamReader(System.in), new java.io.OutputStreamWriter(System.out), model);
    }
}
