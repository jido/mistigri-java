package com.bredelet.mistigri;

import java.io.Reader;
import java.io.BufferedReader;

/***
 * This reader extends BufferedReader with a new method which returns the
 * text up to an arbitrary separator, not just a newline.<p>
 *
 * The separator is set on creation:
 * <pre>new TemplateReader(reader, separator)
 * </pre>
 *
 * Unlike java.util.Scanner the delimiter is a string, not a regular
 * expression which allows for very quick detection. But when reading
 * from disk or the network it is unlikely to make much a difference in
 * speed.<p>
 */
public class TemplateReader extends BufferedReader
{
    String separator;
    private char[] sepchars;
    
    private static final int bufsize = 400;
    private int averageLength = 100;
    
    /***
     * Creates a new TemplateReader for the specified source and separator.<p>
     *
     * @param source The underlying reader to read from
     * @param separator The delimiter string to use in readPart()
     */
    public TemplateReader(Reader source, String separator) {
        super(source);
        this.separator = separator;
        this.averageLength += separator.length();
    }
    
    /***
     * Reads from the underlying source until the requested length or until
     * the source is completely read.<p>
     *
     * @param buf A buffer to read into
     * @param start The offset in the buffer
     * @param len The number of characters to read
     *
     * @return The number of characters actually read
     */
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
    
    /***
     * Reads from the underlying source until the separator is found and returns
     * the part of text that precedes.<p>
     *
     * Note that the separator is thrown away.
     */
    public String readPart() throws java.io.IOException {
        // assert(averageLength > separator.length());
        StringBuilder part = new StringBuilder(averageLength);
        char[] buf = new char[bufsize];
        int seplen = separator.length();
        int start = 0;
        int end = 0;
        outer: do {
            if (start + seplen * 2 >= bufsize)
            {
                // Flush buffer
                part.append(buf, 0, start);
                start = 0;
            }
            int actual = readFull(buf, start, seplen);
            if (actual < seplen)
            {
                end = start + actual;
                if (end == -1) return null;
                break;
            }
            int last = start + seplen - 1;
            char witness = buf[last];
            int findSep = separator.lastIndexOf(witness);
            int first = start;
            while (findSep != -1)
            {
                // Found separator character!
                if (findSep == last - first)
                {
                    // Position coincides with 'first'
                    int pos = first;
                    boolean differs = false;
                    if (sepchars == null)
                    {
                        sepchars = separator.toCharArray();
                    }
                    for (char sepchar: sepchars)
                    {
                        differs = sepchar != buf[pos];
                        if (differs) break;
                        ++pos;
                    }
                    if (differs)
                    {
                        // Check if the separator character repeats at another position
                        findSep = separator.lastIndexOf(witness, findSep - 1);
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
                    if (actual2 < extra)
                    {
                        end = first + seplen + actual2;
                        break outer;
                    }
                    first += extra;
                    // assert(findSep == last - first);
                }
            }
            start = first + seplen;
        } while (true);
        part.append(buf, 0, end);
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