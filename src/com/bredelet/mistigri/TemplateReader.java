package com.bredelet.mistigri;

import java.io.Reader;
import java.io.BufferedReader;

public class TemplateReader extends BufferedReader
{
    String separator;
    
    private static final int bufsize = 400;
    private int averageLength = 100;
    
    public TemplateReader(Reader source, String separator) {
        super(source);
        this.separator = separator;
        this.averageLength += separator.length();
    }
    
    int readFull(char[] buf, int start, int len) throws java.io.IOException {
        int actual = read(buf, start, len);
        // System.err.println("-read " + actual);
        if (actual < len)
        {
            int current;
            for (current = actual; actual != -1 && current < len; current += actual)
            {
                actual = read(buf, start + current, len - current);
                // System.err.println("-read " + actual);
            }
            return current;
        }
        return actual;
    }
    
    public String readPart() throws java.io.IOException {
        // assert(averageLength >= 2 * separator.length());
        char[] buf = new char[bufsize];
        StringBuilder part = new StringBuilder(averageLength);
        int seplen = separator.length();
        int start = 0;
        int end = 0;
        outer: do {
            if (start + seplen * 2 >= bufsize)
            {
                // Flush buffer
                // System.err.println("%%%FLUSH: start=" + start);
                part.append(buf, 0, start);
                start = 0;
            }
            int actual = readFull(buf, start, seplen);
            if (actual < seplen)
            {
                end = start + actual;
                break;
            }
            int last = start + seplen - 1;
            int findSep = separator.lastIndexOf(buf[last]);
            int first = start;
            while (findSep != -1)
            {
                // Found separator character!
                // System.err.println("%%% FOUND: " + buf[last] + " at " + last);
                if (findSep == last - first)
                {
                    // Position coincides with 'first'
                    int pos = first;
                    boolean differs = false;
                    for (char sepchar: separator.toCharArray())
                    {
                        differs = sepchar != buf[pos];
                        if (differs) break;
                        ++pos;
                    }
                    if (differs)
                    {
                        // Check if the separator character repeats at another position
                        findSep = separator.lastIndexOf(buf[last], findSep - 1);
                    }
                    else
                    {
                        // Matched separator
                        end = first;
                        break outer;
                    }
                }
                else
                {
                    // Does not coincide, read extra characters from input and adjust 'first'.
                    int extra = last - findSep - first;
                    int actual2 = readFull(buf, first + seplen, extra);
                    if (actual2 != extra)
                    {
                        end = first + seplen + actual2;
                        break outer;
                    }
                    // System.err.println("%%% MORE: '" + new String(buf, first + seplen, extra) + "'");
                    first += extra;
                }
            }
            start = first + seplen;
        } while (true);
        if (end == -1) return null;
        part.append(buf, 0, end);
        // System.err.println("%%% RESULT: " + part);
        return part.toString();
    }

    public static void main(String[] args) throws Exception {
        TemplateReader r = new TemplateReader(new java.io.InputStreamReader(System.in), args[0]);
        for (String part = r.readPart(); part != null; part = r.readPart())
        {
            System.out.println("%%%PART: " + part);
        }
    }
}