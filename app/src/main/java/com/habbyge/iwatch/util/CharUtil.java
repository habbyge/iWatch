package com.habbyge.iwatch.util;

/**
 * <p>Operations on char primitives and Character objects.</p>
 *
 * <p>This class tries to handle {@code null} input gracefully.
 * An exception will not be thrown for a {@code null} input.
 * Each method documents its behaviour in more detail.</p>
 *
 * <p>#ThreadSafe#</p>
 * @since 2.1
 * @version $Id: CharUtil.java 1158279 2011-08-16 14:06:45Z ggregory $
 */
public final class CharUtil {

    private static final String[] CHAR_STRING_ARRAY = new String[128];

    /**
     * {@code \u000a} linefeed LF ('\n').
     *
     * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#101089">JLF: Escape Sequences
     *      for Character and String Literals</a>
     * @since 2.2
     */
    public static final char LF = '\n';

    /**
     * {@code \u000d} carriage return CR ('\r').
     *
     * @see <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#101089">JLF: Escape Sequences
     *      for Character and String Literals</a>
     * @since 2.2
     */
    public static final char CR = '\r';


    static {
        for (char c = 0; c < CHAR_STRING_ARRAY.length; c++) {
            CHAR_STRING_ARRAY[c] = String.valueOf(c);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Converts the character to a Character.</p>
     *
     * <p>For ASCII 7 bit characters, this uses a cache that will return the
     * same Character object each time.</p>
     *
     * <pre>
     *   CharUtil.toCharacterObject(' ')  = ' '
     *   CharUtil.toCharacterObject('A')  = 'A'
     * </pre>
     *
     * @deprecated Java 5 introduced {@link Character#valueOf(char)} which caches chars 0 through 127.
     * @param ch  the character to convert
     * @return a Character of the specified character
     */
    @Deprecated
    public static Character toCharacterObject(char ch) {
        return Character.valueOf(ch);
    }

    /**
     * <p>Converts the String to a Character using the first character, returning
     * null for empty Strings.</p>
     *
     * <p>For ASCII 7 bit characters, this uses a cache that will return the
     * same Character object each time.</p>
     *
     * <pre>
     *   CharUtil.toCharacterObject(null) = null
     *   CharUtil.toCharacterObject("")   = null
     *   CharUtil.toCharacterObject("A")  = 'A'
     *   CharUtil.toCharacterObject("BA") = 'B'
     * </pre>
     *
     * @param str  the character to convert
     * @return the Character value of the first letter of the String
     */
    public static Character toCharacterObject(String str) {
        if (StringUtil.isEmpty(str)) {
            return null;
        }
        return Character.valueOf(str.charAt(0));
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Converts the Character to a char throwing an exception for {@code null}.</p>
     *
     * <pre>
     *   CharUtil.toChar(' ')  = ' '
     *   CharUtil.toChar('A')  = 'A'
     *   CharUtil.toChar(null) throws IllegalArgumentException
     * </pre>
     *
     * @param ch  the character to convert
     * @return the char value of the Character
     * @throws IllegalArgumentException if the Character is null
     */
    public static char toChar(Character ch) {
        if (ch == null) {
            throw new IllegalArgumentException("The Character must not be null");
        }
        return ch.charValue();
    }

    /**
     * <p>Converts the Character to a char handling {@code null}.</p>
     *
     * <pre>
     *   CharUtil.toChar(null, 'X') = 'X'
     *   CharUtil.toChar(' ', 'X')  = ' '
     *   CharUtil.toChar('A', 'X')  = 'A'
     * </pre>
     *
     * @param ch  the character to convert
     * @param defaultValue  the value to use if the  Character is null
     * @return the char value of the Character or the default if null
     */
    public static char toChar(Character ch, char defaultValue) {
        if (ch == null) {
            return defaultValue;
        }
        return ch.charValue();
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Converts the String to a char using the first character, throwing
     * an exception on empty Strings.</p>
     *
     * <pre>
     *   CharUtil.toChar("A")  = 'A'
     *   CharUtil.toChar("BA") = 'B'
     *   CharUtil.toChar(null) throws IllegalArgumentException
     *   CharUtil.toChar("")   throws IllegalArgumentException
     * </pre>
     *
     * @param str  the character to convert
     * @return the char value of the first letter of the String
     * @throws IllegalArgumentException if the String is empty
     */
    public static char toChar(String str) {
        if (StringUtil.isEmpty(str)) {
            throw new IllegalArgumentException("The String must not be empty");
        }
        return str.charAt(0);
    }

    /**
     * <p>Converts the String to a char using the first character, defaulting
     * the value on empty Strings.</p>
     *
     * <pre>
     *   CharUtil.toChar(null, 'X') = 'X'
     *   CharUtil.toChar("", 'X')   = 'X'
     *   CharUtil.toChar("A", 'X')  = 'A'
     *   CharUtil.toChar("BA", 'X') = 'B'
     * </pre>
     *
     * @param str  the character to convert
     * @param defaultValue  the value to use if the  Character is null
     * @return the char value of the first letter of the String or the default if null
     */
    public static char toChar(String str, char defaultValue) {
        if (StringUtil.isEmpty(str)) {
            return defaultValue;
        }
        return str.charAt(0);
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Converts the character to the Integer it represents, throwing an
     * exception if the character is not numeric.</p>
     *
     * <p>This method coverts the char '1' to the int 1 and so on.</p>
     *
     * <pre>
     *   CharUtil.toIntValue('3')  = 3
     *   CharUtil.toIntValue('A')  throws IllegalArgumentException
     * </pre>
     *
     * @param ch  the character to convert
     * @return the int value of the character
     * @throws IllegalArgumentException if the character is not ASCII numeric
     */
    public static int toIntValue(char ch) {
        if (isAsciiNumeric(ch) == false) {
            throw new IllegalArgumentException("The character " + ch + " is not in the range '0' - '9'");
        }
        return ch - 48;
    }

    /**
     * <p>Converts the character to the Integer it represents, throwing an
     * exception if the character is not numeric.</p>
     *
     * <p>This method coverts the char '1' to the int 1 and so on.</p>
     *
     * <pre>
     *   CharUtil.toIntValue('3', -1)  = 3
     *   CharUtil.toIntValue('A', -1)  = -1
     * </pre>
     *
     * @param ch  the character to convert
     * @param defaultValue  the default value to use if the character is not numeric
     * @return the int value of the character
     */
    public static int toIntValue(char ch, int defaultValue) {
        if (isAsciiNumeric(ch) == false) {
            return defaultValue;
        }
        return ch - 48;
    }

    /**
     * <p>Converts the character to the Integer it represents, throwing an
     * exception if the character is not numeric.</p>
     *
     * <p>This method coverts the char '1' to the int 1 and so on.</p>
     *
     * <pre>
     *   CharUtil.toIntValue('3')  = 3
     *   CharUtil.toIntValue(null) throws IllegalArgumentException
     *   CharUtil.toIntValue('A')  throws IllegalArgumentException
     * </pre>
     *
     * @param ch  the character to convert, not null
     * @return the int value of the character
     * @throws IllegalArgumentException if the Character is not ASCII numeric or is null
     */
    public static int toIntValue(Character ch) {
        if (ch == null) {
            throw new IllegalArgumentException("The character must not be null");
        }
        return toIntValue(ch.charValue());
    }

    /**
     * <p>Converts the character to the Integer it represents, throwing an
     * exception if the character is not numeric.</p>
     *
     * <p>This method coverts the char '1' to the int 1 and so on.</p>
     *
     * <pre>
     *   CharUtil.toIntValue(null, -1) = -1
     *   CharUtil.toIntValue('3', -1)  = 3
     *   CharUtil.toIntValue('A', -1)  = -1
     * </pre>
     *
     * @param ch  the character to convert
     * @param defaultValue  the default value to use if the character is not numeric
     * @return the int value of the character
     */
    public static int toIntValue(Character ch, int defaultValue) {
        if (ch == null) {
            return defaultValue;
        }
        return toIntValue(ch.charValue(), defaultValue);
    }

    //-----------------------------------------------------------------------
    /**
     * <p>Converts the character to a String that contains the one character.</p>
     *
     * <p>For ASCII 7 bit characters, this uses a cache that will return the
     * same String object each time.</p>
     *
     * <pre>
     *   CharUtil.toString(' ')  = " "
     *   CharUtil.toString('A')  = "A"
     * </pre>
     *
     * @param ch  the character to convert
     * @return a String containing the one specified character
     */
    public static String toString(char ch) {
        if (ch < 128) {
            return CHAR_STRING_ARRAY[ch];
        }
        return new String(new char[] {ch});
    }

    /**
     * <p>Converts the character to a String that contains the one character.</p>
     *
     * <p>For ASCII 7 bit characters, this uses a cache that will return the
     * same String object each time.</p>
     *
     * <p>If {@code null} is passed in, {@code null} will be returned.</p>
     *
     * <pre>
     *   CharUtil.toString(null) = null
     *   CharUtil.toString(' ')  = " "
     *   CharUtil.toString('A')  = "A"
     * </pre>
     *
     * @param ch  the character to convert
     * @return a String containing the one specified character
     */
    public static String toString(Character ch) {
        if (ch == null) {
            return null;
        }
        return toString(ch.charValue());
    }

    //--------------------------------------------------------------------------
    /**
     * <p>Converts the string to the Unicode format '\u0020'.</p>
     *
     * <p>This format is the Java source code format.</p>
     *
     * <pre>
     *   CharUtil.unicodeEscaped(' ') = "\u0020"
     *   CharUtil.unicodeEscaped('A') = "\u0041"
     * </pre>
     *
     * @param ch  the character to convert
     * @return the escaped Unicode string
     */
    public static String unicodeEscaped(char ch) {
        if (ch < 0x10) {
            return "\\u000" + Integer.toHexString(ch);
        } else if (ch < 0x100) {
            return "\\u00" + Integer.toHexString(ch);
        } else if (ch < 0x1000) {
            return "\\u0" + Integer.toHexString(ch);
        }
        return "\\u" + Integer.toHexString(ch);
    }

    /**
     * <p>Converts the string to the Unicode format '\u0020'.</p>
     *
     * <p>This format is the Java source code format.</p>
     *
     * <p>If {@code null} is passed in, {@code null} will be returned.</p>
     *
     * <pre>
     *   CharUtil.unicodeEscaped(null) = null
     *   CharUtil.unicodeEscaped(' ')  = "\u0020"
     *   CharUtil.unicodeEscaped('A')  = "\u0041"
     * </pre>
     *
     * @param ch  the character to convert, may be null
     * @return the escaped Unicode string, null if null input
     */
    public static String unicodeEscaped(Character ch) {
        if (ch == null) {
            return null;
        }
        return unicodeEscaped(ch.charValue());
    }

    //--------------------------------------------------------------------------
    /**
     * <p>Checks whether the character is ASCII 7 bit.</p>
     *
     * <pre>
     *   CharUtil.isAscii('a')  = true
     *   CharUtil.isAscii('A')  = true
     *   CharUtil.isAscii('3')  = true
     *   CharUtil.isAscii('-')  = true
     *   CharUtil.isAscii('\n') = true
     *   CharUtil.isAscii('&copy;') = false
     * </pre>
     *
     * @param ch  the character to check
     * @return true if less than 128
     */
    public static boolean isAscii(char ch) {
        return ch < 128;
    }

    /**
     * <p>Checks whether the character is ASCII 7 bit printable.</p>
     *
     * <pre>
     *   CharUtil.isAsciiPrintable('a')  = true
     *   CharUtil.isAsciiPrintable('A')  = true
     *   CharUtil.isAsciiPrintable('3')  = true
     *   CharUtil.isAsciiPrintable('-')  = true
     *   CharUtil.isAsciiPrintable('\n') = false
     *   CharUtil.isAsciiPrintable('&copy;') = false
     * </pre>
     *
     * @param ch  the character to check
     * @return true if between 32 and 126 inclusive
     */
    public static boolean isAsciiPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }

    /**
     * <p>Checks whether the character is ASCII 7 bit control.</p>
     *
     * <pre>
     *   CharUtil.isAsciiControl('a')  = false
     *   CharUtil.isAsciiControl('A')  = false
     *   CharUtil.isAsciiControl('3')  = false
     *   CharUtil.isAsciiControl('-')  = false
     *   CharUtil.isAsciiControl('\n') = true
     *   CharUtil.isAsciiControl('&copy;') = false
     * </pre>
     *
     * @param ch  the character to check
     * @return true if less than 32 or equals 127
     */
    public static boolean isAsciiControl(char ch) {
        return ch < 32 || ch == 127;
    }

    /**
     * <p>Checks whether the character is ASCII 7 bit alphabetic.</p>
     *
     * <pre>
     *   CharUtil.isAsciiAlpha('a')  = true
     *   CharUtil.isAsciiAlpha('A')  = true
     *   CharUtil.isAsciiAlpha('3')  = false
     *   CharUtil.isAsciiAlpha('-')  = false
     *   CharUtil.isAsciiAlpha('\n') = false
     *   CharUtil.isAsciiAlpha('&copy;') = false
     * </pre>
     *
     * @param ch  the character to check
     * @return true if between 65 and 90 or 97 and 122 inclusive
     */
    public static boolean isAsciiAlpha(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    /**
     * <p>Checks whether the character is ASCII 7 bit alphabetic upper case.</p>
     *
     * <pre>
     *   CharUtil.isAsciiAlphaUpper('a')  = false
     *   CharUtil.isAsciiAlphaUpper('A')  = true
     *   CharUtil.isAsciiAlphaUpper('3')  = false
     *   CharUtil.isAsciiAlphaUpper('-')  = false
     *   CharUtil.isAsciiAlphaUpper('\n') = false
     *   CharUtil.isAsciiAlphaUpper('&copy;') = false
     * </pre>
     *
     * @param ch  the character to check
     * @return true if between 65 and 90 inclusive
     */
    public static boolean isAsciiAlphaUpper(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    /**
     * <p>Checks whether the character is ASCII 7 bit alphabetic lower case.</p>
     *
     * <pre>
     *   CharUtil.isAsciiAlphaLower('a')  = true
     *   CharUtil.isAsciiAlphaLower('A')  = false
     *   CharUtil.isAsciiAlphaLower('3')  = false
     *   CharUtil.isAsciiAlphaLower('-')  = false
     *   CharUtil.isAsciiAlphaLower('\n') = false
     *   CharUtil.isAsciiAlphaLower('&copy;') = false
     * </pre>
     *
     * @param ch  the character to check
     * @return true if between 97 and 122 inclusive
     */
    public static boolean isAsciiAlphaLower(char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    /**
     * <p>Checks whether the character is ASCII 7 bit numeric.</p>
     *
     * <pre>
     *   CharUtil.isAsciiNumeric('a')  = false
     *   CharUtil.isAsciiNumeric('A')  = false
     *   CharUtil.isAsciiNumeric('3')  = true
     *   CharUtil.isAsciiNumeric('-')  = false
     *   CharUtil.isAsciiNumeric('\n') = false
     *   CharUtil.isAsciiNumeric('&copy;') = false
     * </pre>
     *
     * @param ch  the character to check
     * @return true if between 48 and 57 inclusive
     */
    public static boolean isAsciiNumeric(char ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * <p>Checks whether the character is ASCII 7 bit numeric.</p>
     *
     * <pre>
     *   CharUtil.isAsciiAlphanumeric('a')  = true
     *   CharUtil.isAsciiAlphanumeric('A')  = true
     *   CharUtil.isAsciiAlphanumeric('3')  = true
     *   CharUtil.isAsciiAlphanumeric('-')  = false
     *   CharUtil.isAsciiAlphanumeric('\n') = false
     *   CharUtil.isAsciiAlphanumeric('&copy;') = false
     * </pre>
     *
     * @param ch  the character to check
     * @return true if between 48 and 57 or 65 and 90 or 97 and 122 inclusive
     */
    public static boolean isAsciiAlphanumeric(char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
    }

}
