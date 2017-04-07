package com.bredelet.mistigri;

import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.io.Reader;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.function.Function;
import java.util.Collection;
import java.util.Arrays;

public class Engine extends Thread
{
    TemplateReader input;
    Writer output;
    Map<String, Object> model;
    String openBrace;
    Pattern closeBrace;
    String defaultText;
    boolean bind;
    Function<String, String> escapeFunc;
    Logger log;
    
    Engine(TemplateReader input, Map<String, Object> model, Writer output, Mistigri control, Map<String, Object> config) throws java.io.IOException {
        this.input = input;
        this.model = model;
        this.output = output;
        this.openBrace = (String) control.getOption("openBrace", config);
        String closeBraceText = (String) control.getOption("closeBrace", config);
        this.closeBrace = Pattern.compile(closeBraceText, Pattern.LITERAL);
        this.defaultText = (String) control.getOption("placeholder", config);
        this.bind = (boolean) control.getOption("methodCall", config);
        this.escapeFunc = (Function<String, String>) control.getOption("escapeFunction", config);
        this.log = control.log;
        // java.util.logging.Handler h = new java.util.logging.StreamHandler(System.err, new java.util.logging.SimpleFormatter());
        // h.setLevel(java.util.logging.Level.ALL);
        // log.addHandler(h);
        // log.setLevel(java.util.logging.Level.ALL);
    }
    
    public void run() {
        render(input, model);
        try {
            output.close();
        }
        catch (Exception e) {
            log.warning(e.getLocalizedMessage());
        }
    }
    
    private void render(TemplateReader template, Map<String, Object> model) {
        log.fine("TemplateReader=" + template);
        Map<String, Object> args = new HashMap<String, Object>();
        String rendered = "";
        String action;
        int position = 0;
        try {
            output.write(template.readPart());
            while (true)
            {
                String part = template.readPart();
                log.fine("%%%PART = \"" + part +"\"");
                if (part == null) break;
                String[] mitext = closeBrace.split(part, 2);
                String mistigri = mitext[0];
                String text = (mitext.length > 1) ? mitext[1] : "";
                args.clear();
                args.put("$position", position);
                args.put("$template", input);
                args.put("$model", model);
                args.put("$placeholder", defaultText);
                boolean invert = false;
                switch (part.charAt(0))
                {
                    case '^':
                        invert = true;
                        // Fall through to next case
                    case '#':
                        action = parseAction(mistigri.substring(1), args);
                        args.put("$prelude", rendered);
                        //includes.offset = offset + rendered.length;
                        handleBlock(action, invert, args, text);
                        rendered = "";
                        text = (String) args.get("$ending");
                        break;
                    case '/':
                        output.write(mistigri);   // invalid close tag
                        break;
                    case '!':
                        break;  // just a comment
                    case '&':
                        action = parseAction(mistigri.substring(1), args);
                        output.write(handleValue(action, args));
                        break;
                    default:
                        log.fine("default case, tag=" + mistigri);
                        action = parseAction(mistigri, args);
                        output.write(escape(handleValue(action, args), escapeFunc));
                }
                output.write(text);
                rendered += text;
            }
            log.fine("finished writing parts TemplateReader=" + template);
        }
        catch (java.io.IOException e) {
            log.warning(e.getLocalizedMessage());
        }
    }
    
    private String escape(String text, Function<String, String> function) {
        try {
            return function.apply(text);
        }
        catch (Exception error) {
            log.warning("Mistigri encountered an error while escaping '" + text + "'");
            log.warning(error.getLocalizedMessage());
            return defaultText;
        }
    }
    
    private String handleValue(String action, Map<String, Object> args) {
        Object result = model.get(action);
        return (result == null) ? defaultText : result.toString();
    }
    
    private String callFilter(String name, Object value, Map<String, Object> args) {
        try {
            Function<Map<String, Object>, String> filter = ((Function<Map<String, Object>, String>) value);
            try {
                return filter.apply(args);
            }
            catch (Exception e) {
                log.warning("Mistigri called " + name + " and received an error");
                log.warning(e.getLocalizedMessage());
            }
        }
        catch (ClassCastException cce) {
            log.warning("Mistigri only takes a function which takes a map of arguments and returns a string");
        }
        return defaultText;
    }
    
    private void handleBlock(String action, boolean invert, Map<String, Object> args, String text) {
        MistigriBlockReader blockReader = new MistigriBlockReader(input, action, text, closeBrace, ending -> args.put("$ending", ending));
        Object value = model.get(action);//valueFor(action, model, bind);
        log.fine("%%% action=" + action + " value=" + value);
        String prelude = (String) args.get("$prelude");    // preserve value
        if (value instanceof Function)
        {
            log.fine("action is a function, calling");
            args.put("$template", blockReader);
            args.put("$invertBlock", invert);
            value = callFilter(action, value, args);
            if (value instanceof String)
            {
                try {
                    output.write(value.toString()); // add to output
                }
                catch (java.io.IOException ioe) {
                    log.warning("Error");
                }
                return;
            }
            else if (value instanceof Reader)
            {
                //includes.work.push({deferred: value, at: includes.offset, path: null, model: null, render: true});
                return;
            }
        }
        boolean isEmptyColl = value instanceof Collection && ((Collection<?>) value).isEmpty();
        boolean isEmptyArray = value instanceof Object[] && ((Object[]) value).length == 0;
        boolean isEmptyString = value instanceof String && value.equals("");
        boolean isFalse = value instanceof Boolean && !((Boolean) value);
        boolean isZero = value instanceof Number && ((Number) value).equals(0);
        boolean isEmpty = isEmptyColl || isEmptyArray || isEmptyString || isFalse || isZero;
        if ((isEmpty && invert) || (!isEmpty && !invert))
        {
            String suffix = (String) args.get("suffix");
            String separator = (String) args.get("separator");
            String middle = separator;
            if (isEmptyColl || isEmptyArray)
            {
                value = null;   // special handling for empty list
            }
            Collection<Object> list = (value instanceof Collection) ? ((Collection<Object>) value) :
                (value instanceof Object[]) ? Arrays.asList((Object[]) value) :
                Arrays.asList(new Object[] {value});
            int total = invert ? 0 : list.size();
            if (total > 1) blockReader.mark(0);
            int count = 0;
            for (Object item: list)
            {
                count += 1;
                log.fine("Item " + count + " = " + item);
                Map<String, Object> submodel = prepModel(model, item, count, total, suffix);
                if (count > 1 && middle != null)
                {
                    try {
                        output.write(middle);
                    }
                    catch (java.io.IOException ioe) {
                        log.warning("Error");
                    }
                }
                //includes.offset = offset + result.length;
                render(blockReader, submodel);
                if (total > 1) {
                    try {
                        blockReader.reset();
                    }
                    catch (java.io.IOException ioe) {
                        // Programming error
                        throw new RuntimeException(ioe);
                    }
                    if (count == 1) 
                    {
                        String tags = getMiddleTags(prelude, args);
                        if (tags != null)
                        {
                            middle = tags;
                        }
                    }
                }
            }
        }
        else
        {
            try {
                while (blockReader.readPart() != null)
                {
                    log.fine("Skip block part");
                }
            }
            catch (java.io.IOException ioe) {
                log.warning("Error");
            }
        }
        log.fine("%%%Exiting");
    }
    
    private static String getMiddleTags(String prelude, Map<String, Object> args) {
        if (args.containsKey("tag"))
        {
            String ending = (String) args.get("$ending");
            String tag = ((String) args.get("tag")).toLowerCase();
            int left = prelude.toLowerCase().lastIndexOf("<" + tag);
            int right = ending.toLowerCase().indexOf("</" + tag);
            if (left != -1 && right != -1)
            {
                right = ending.indexOf(">", right) + 1;
                return ending.substring(0, right) + prelude.substring(left);
            }
        }
        return null;
    }
    
    Map<String, Object> prepModel(Map<String, Object> model, Object item, int count, int total, String suffix) {
        return model;
    }
    
    private static final Pattern parser = Pattern.compile("^\\s*(\\S+)\\s*(.*)", Pattern.DOTALL);

    private String parseAction(String tag, Map<String, Object> args) {
        Matcher them = parser.matcher(tag);
        if (!them.matches()) return "";
        return them.group(1);
    }
}
