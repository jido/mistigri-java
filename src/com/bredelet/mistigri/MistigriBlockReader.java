package com.bredelet.mistigri;

import java.io.Reader;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;

public class MistigriBlockReader extends TemplateReader {
    private String action;
    private Pattern closeBrace;
    private Consumer<String> setEnding;
    private List<String> parts = new ArrayList<String>();
    private int position = 0;
    private int nesting = 0;
    private boolean marked = false;
    private static final Pattern parser = Pattern.compile("^\\s*(\\S+)");
    
    MistigriBlockReader(TemplateReader source, String action, String first, Pattern closeBrace, Consumer<String> setEnding) {
        super(source, source.separator);
        this.action = action;
        parts.add(first);
        this.setEnding = setEnding;
        this.closeBrace = closeBrace;
    }
    
    boolean matchAction(String part) {
        Matcher them = parser.matcher(part);
        return them.find() && them.group(1).equals(action);
    }
    
    /***
     * Reads the next part from the underlying source or from a stored list of
     * parts (after reset).<p>
     *
     * @see TemplateReader
     */
    @Override public String readPart() throws java.io.IOException {
        String part;
        if (parts != null && position < parts.size())
        {
            part = parts.get(position);
            ++position;
            return part;
        }
        part = super.readPart();
        if (part == null)
        {
            setEnding.accept("");
            return null;
        }
        switch (part.charAt(0))
        {
            case '#':
            case '^':
            case '/':
                String[] mitext = closeBrace.split(part.substring(1), 2);
                String mistigri = mitext[0];
                if (matchAction(mistigri))
                {
                    if (part.charAt(0) == '/')
                    {
                        if (nesting == 0)
                        {
                            String text = (mitext.length > 1) ? mitext[1] : "";
                            setEnding.accept(text);
                            part = null;
                        }
                        --nesting;
                    }
                    else
                    {
                        ++nesting;
                    }

                }
        }
        if (marked) parts.add(part);
        return part;
    }
    
    /***
     * Start storing the read parts in a list so that they can be read again
     *
     * @param unused
     */
    @Override public void mark(int unused) {
        marked = true;
    }
    
    /***
     * Read parts from the last mark onwards
     */
   @Override public void reset() throws java.io.IOException {
        if (!marked)
        {
            throw new java.io.IOException("No mark set");
        }
        // assert(!parts.isEmpty());
        if (parts.get(parts.size() - 1) != null)
        {
            throw new java.io.IOException("Only use reset() after reading the full block");
        }
        position = 0;
    }
}
