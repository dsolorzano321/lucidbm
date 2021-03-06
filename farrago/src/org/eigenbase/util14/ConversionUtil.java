/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2002 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 2003 John V. Sichi
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.eigenbase.util14;

import java.nio.*;

import java.sql.*;

import java.text.*;

import org.eigenbase.resource.*;


/**
 * Utility functions for converting from one type to another
 *
 * @author angel
 * @version $Id$
 * @since Jan 22, 2006
 */
public class ConversionUtil
{
    //~ Static fields/initializers ---------------------------------------------

    public static final String NATIVE_UTF16_CHARSET_NAME =
        (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) ? "UTF-16BE"
        : "UTF-16LE";

    /**
     * A constant string which can be used wherever a Java string containing
     * Unicode characters is needed in a test. It spells 'anthropos' in Greek.
     */
    public static final String TEST_UNICODE_STRING =
        "\u03B1\u03BD\u03B8\u03C1\u03C9\u03C0\u03BF\u03C2";

    /**
     * A constant string which can be used wherever a SQL literal containing
     * Unicode escape characters is needed in a test. It spells 'anthropos' in
     * Greek. The escape character is the SQL default (backslash); note that the
     * backslash-doubling here is for Java only, so by the time the SQL parser
     * gets it, there is only one backslash.
     */
    public static final String TEST_UNICODE_SQL_ESCAPED_LITERAL =
        "\\03B1\\03BD\\03B8\\03C1\\03C9\\03C0\\03BF\\03C2";

    //~ Methods ----------------------------------------------------------------

    /**
     * Converts a byte array into a bit string or a hex string.
     *
     * <p>For example, <code>toStringFromByteArray(new byte[] {0xAB, 0xCD},
     * 16)</code> returns <code>ABCD</code>.
     */
    public static String toStringFromByteArray(
        byte [] value,
        int radix)
    {
        assert (2 == radix) || (16 == radix)
            : "Make sure that the algorithm below works for your radix";
        if (0 == value.length) {
            return "";
        }

        int trick = radix * radix;
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < value.length; i++) {
            ret.append(
                Integer.toString(trick | (0x0ff & value[i]), radix).substring(
                    1));
        }

        return ret.toString().toUpperCase();
    }

    /**
     * Converts a string into a byte array. The inverse of {@link
     * #toStringFromByteArray(byte[], int)}.
     */
    public static byte [] toByteArrayFromString(
        String value,
        int radix)
    {
        assert 16 == radix
            : "Specified string to byte array conversion not supported yet";
        assert (value.length() % 2) == 0
            : "Hex binary string must contain even number of characters";

        byte [] ret = new byte[value.length() / 2];
        for (int i = 0; i < ret.length; i++) {
            int digit1 =
                Character.digit(
                    value.charAt(i * 2),
                    radix);
            int digit2 =
                Character.digit(
                    value.charAt((i * 2) + 1),
                    radix);
            assert (digit1 != -1) && (digit2 != -1)
                : "String could not be converted to byte array";
            ret[i] = (byte) ((digit1 * radix) + digit2);
        }
        return ret;
    }

    /**
     * Converts an approximate value into a string, following the SQL 2003
     * standard.
     */
    public static String toStringFromApprox(double d, boolean isFloat)
    {
        NumberFormat nf = NumberUtil.getApproxFormatter(isFloat);
        return nf.format(d);
    }

    /**
     * Converts a string into a boolean
     */
    public static Boolean toBoolean(String str)
    {
        if (str == null) {
            return null;
        }
        str = str.trim();
        if (str.equalsIgnoreCase("TRUE")) {
            return Boolean.TRUE;
        } else if (str.equalsIgnoreCase("FALSE")) {
            return Boolean.FALSE;
        } else if (str.equalsIgnoreCase("UNKNOWN")) {
            return null;
        } else {
            throw EigenbaseResource.instance().InvalidBoolean.ex(str);
        }
    }
}

// End ConversionUtil.java
