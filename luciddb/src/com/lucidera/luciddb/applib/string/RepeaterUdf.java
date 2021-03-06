/*
// $Id$
// LucidDB is a DBMS optimized for business intelligence.
// Copyright (C) 2006-2007 LucidEra, Inc.
// Copyright (C) 2006-2007 The Eigenbase Project
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
package com.lucidera.luciddb.applib.string;

import java.sql.Types;
import com.lucidera.luciddb.applib.resource.*;

/**
 * Repeater returns the input string repeated N times
 *
 * Ported from //bb/bb713/server/SQL/repeater.java
 */
public class RepeaterUdf
{
    /**
     * Ported from //bb/bb713/server/SQL/BBString.java
     *
     * @param in String to be repeated
     * @param times Number of times to repeat string
     * @return The repeated string
     * @exception ApplibException
     */
    public static String execute( String in, int times )
    {
        if( times < 0 ) {
            throw ApplibResourceObject.get().RepSpecifyNonNegative.ex();
        }

        int len = in.length();

        // clip maximum size of output to 64k
        if ( ( times * len ) > ( 64 * 1024 ) ) {
            times = ( 64 * 1024 ) / len;
        }

        char[] outArray = new char[ times * len ];
        char[] inArray = in.toCharArray();

        for( int i=0; i<times; i++ ) {
            System.arraycopy( inArray, 0, outArray, i * len, len );
        }

        return new String( outArray );
    }
}

// End RepeaterUdf.java
