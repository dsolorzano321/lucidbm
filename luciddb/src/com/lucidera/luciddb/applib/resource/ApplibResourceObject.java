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
package com.lucidera.luciddb.applib.resource;

/**
 * Contains a singleton instance of {@link ApplibResource} class for default 
 * locale.  Note: this is a workaround since ApplibResource.instance() has
 * problems loading the bundle from the jar. 
 *
 * @author Elizabeth Lin
 * @version $Id$
 */
public class ApplibResourceObject
{
    private static final ApplibResource res;
    
    static 
    {
        try {
            res = new ApplibResource();
        } catch (Throwable ex) {
            throw new Error(ex);
        }
    }

    public static ApplibResource get() {
        return res;
    }
}

// End ApplibResourceObject.java
