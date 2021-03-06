/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2003 SQLstream, Inc.
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
package net.sf.farrago.fennel.rel;

import net.sf.farrago.query.*;

import openjava.ptree.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;


/**
 * FennelOptionalRel is a {@link FennelRel} which either takes zero inputs or
 * takes a single FennelRel as input.
 *
 * @author John Pham
 * @version $Id$
 */
public abstract class FennelOptionalRel
    extends FennelSingleRel
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FennelOptionalRel object with an input rel.
     *
     * @param cluster RelOptCluster for this rel
     * @param child input rel
     */
    protected FennelOptionalRel(
        RelOptCluster cluster,
        RelNode child)
    {
        super(cluster, child);
    }

    /**
     * Creates a new FennelOptionalRel object without an input rel.
     *
     * @param cluster RelOptCluster for this rel
     */
    protected FennelOptionalRel(
        RelOptCluster cluster)
    {
        super(cluster, null);
    }

    //~ Methods ----------------------------------------------------------------

    // override SingleRel
    public RelNode [] getInputs()
    {
        if (getChild() != null) {
            return super.getInputs();
        }
        return AbstractRelNode.emptyArray;
    }

    // override SingleRel
    public void childrenAccept(RelVisitor visitor)
    {
        if (getChild() != null) {
            super.childrenAccept(visitor);
        }
    }

    // override FennelSingleRel
    public Object implementFennelChild(FennelRelImplementor implementor)
    {
        if (getChild() != null) {
            return super.implementFennelChild(implementor);
        }
        return Literal.constantNull();
    }

    // override FennelSingleRel
    public double getRows()
    {
        if (getChild() != null) {
            return super.getRows();
        }
        return 1.0;
    }
}

// End FennelOptionalRel.java
