/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2006 The Eigenbase Project
// Copyright (C) 2006 SQLstream, Inc.
// Copyright (C) 2006 Dynamo BI Corporation
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
package org.eigenbase.sarg;

import org.eigenbase.rex.*;


/**
 * SargBinding represents the binding of a {@link SargExpr} to a particular
 * {@link RexInputRef}.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class SargBinding
{
    //~ Instance fields --------------------------------------------------------

    private final SargExpr expr;

    private final RexInputRef inputRef;

    //~ Constructors -----------------------------------------------------------

    public SargBinding(SargExpr expr, RexInputRef inputRef)
    {
        this.expr = expr;
        this.inputRef = inputRef;
    }

    //~ Methods ----------------------------------------------------------------

    public SargExpr getExpr()
    {
        return expr;
    }

    public RexInputRef getInputRef()
    {
        return inputRef;
    }
}

// End SargBinding.java
