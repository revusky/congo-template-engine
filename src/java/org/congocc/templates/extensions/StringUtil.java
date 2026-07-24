package org.congocc.templates.extensions;

import java.util.*;

/**
 *  Some text related utilities.
 *
 *  @version $Id: StringUtil.java,v 1.48 2005/06/01 22:39:08 ddekany Exp $
 */
public class StringUtil {
    /**
     *  HTML encoding (does not convert line breaks).
     *  Replaces all '&gt;' '&lt;' '&amp;' and '"' with entity reference
     */
    public static String HTMLEnc(String s) {
        return XMLOrXHTMLEnc(s, "'");
    }

    /**
     *  XML Encoding.
     *  Replaces all '&gt;' '&lt;' '&amp;', "'" and '"' with entity reference
     */
    public static String XMLEnc(String s) {
        return XMLOrXHTMLEnc(s, "&apos;");
    }

    /**
     *  XHTML Encoding.
     *  Replaces all '&gt;' '&lt;' '&amp;', "'" and '"' with entity reference
     *  suitable for XHTML decoding in common user agents (including legacy
     *  user agents, which do not decode "&apos;" to "'", so "&#39;" is used
     *  instead [see http://www.w3.org/TR/xhtml1/#C_16])
     */
    public static String XHTMLEnc(String s) {
        return XMLOrXHTMLEnc(s, "&#39;");
    }

    private static String XMLOrXHTMLEnc(String s, String aposReplacement) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '<' -> buf.append("&lt;");
                case '>' -> buf.append("&gt;");
                case '&' -> buf.append("&amp;");
                case '"' -> buf.append("&quot;");
                case '\'' -> buf.append(aposReplacement);
                default -> buf.append(ch);
            }
        }
        return buf.toString();
    }

    public static String RTFEnc(String s) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' || ch == '{' || ch == '}') {
                buf.append('\\');
            }
            buf.append(ch);
        }
        return buf.length() == s.length() ? s : buf.toString();
    }

    public static String capitalize(String s) {
        StringTokenizer st = new StringTokenizer(s, " \t\r\n", true);
        StringBuilder buf = new StringBuilder(s.length());
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            buf.append(tok.substring(0, 1).toUpperCase());
            buf.append(tok.substring(1).toLowerCase());
        }
        return buf.toString();
    }

    /**
     * Removes the line-break from the end of the string.
     */
    public static String chomp(String s) {
        if (s.endsWith("\r\n")) return s.substring(0, s.length() - 2);
        char lastChar = s.length() == 0 ? 0 : s.charAt(s.length()-1);
        return (lastChar != '\n' && lastChar != '\r') ? s : s.substring(0, s.length() - 1);
    }

    /**
     * Escapes the <code>String</code> with the escaping rules of Java language
     * string literals, so it is safe to insert the value into a string literal.
     * The resulting string will not be quoted.
     *
     * <p>In additional, all characters under UCS code point 0x20, that has no
     * dedicated escape sequence in Java language, will be replaced with UNICODE
     * escape (<tt>\<!-- -->u<i>XXXX</i></tt>).
     *
     * @see #jQuote(String)
     */
    public static String javaStringEncode(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\' || c < 0x20) {
                StringBuilder b = new StringBuilder(ln + 4);
                b.append(s.substring(0, i));
                while (true) {
                    if (c == '"') {
                        b.append("\\\"");
                    } else if (c == '\\') {
                        b.append("\\\\");
                    } else if (c < 0x20) {
                        if (c == '\n') {
                            b.append("\\n");
                        } else if (c == '\r') {
                            b.append("\\r");
                        } else if (c == '\f') {
                            b.append("\\f");
                        } else if (c == '\b') {
                            b.append("\\b");
                        } else if (c == '\t') {
                            b.append("\\t");
                        } else {
                            b.append("\\u00");
                            int x = c / 0x10;
                            b.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'a'));
                            x = c & 0xF;
                            b.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'a'));
                        }
                    } else {
                        b.append(c);
                    }
                    i++;
                    if (i >= ln) {
                        return b.toString();
                    }
                    c = s.charAt(i);
                }
            } // if has to be escaped
        } // for each characters
        return s;
    }

    /**
     * Escapes a <code>String</code> according the JavaScript string literal
     * escaping rules. The resulting string will not be quoted.
     *
     * <p>It escapes both <tt>'</tt> and <tt>"</tt>.
     * In additional it escapes <tt>></tt> as <tt>\></tt> (to avoid
     * <tt>&lt;/script></tt>). Furthermore, all characters under UCS code point
     * 0x20, that has no dedicated escape sequence in JavaScript language, will
     * be replaced with hexadecimal escape (<tt>\x<i>XX</i></tt>).
     */
    public static String javaScriptStringEnc(String s) {
        int ln = s.length();
        for (int i = 0; i < ln; i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\'' || c == '\\' || c == '>' || c < 0x20) {
                StringBuilder buf = new StringBuilder(ln + 4);
                buf.append(s.substring(0, i));
                while (true) {
                    if (c == '"') {
                        buf.append("\\\"");
                    } else if (c == '\'') {
                        buf.append("\\'");
                    } else if (c == '\\') {
                        buf.append("\\\\");
                    } else if (c == '>') {
                        buf.append("\\>");
                    } else if (c < 0x20) {
                        if (c == '\n') {
                            buf.append("\\n");
                        } else if (c == '\r') {
                            buf.append("\\r");
                        } else if (c == '\f') {
                            buf.append("\\f");
                        } else if (c == '\b') {
                            buf.append("\\b");
                        } else if (c == '\t') {
                            buf.append("\\t");
                        } else {
                            buf.append("\\x");
                            int x = c / 0x10;
                            buf.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'A'));
                            x = c & 0xF;
                            buf.append((char)
                                    (x < 0xA ? x + '0' : x - 0xA + 'A'));
                        }
                    } else {
                        buf.append(c);
                    }
                    i++;
                    if (i >= ln) {
                        return buf.toString();
                    }
                    c = s.charAt(i);
                }
            } // if has to be escaped
        } // for each characters
        return s;
    }

}
