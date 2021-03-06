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
package org.eigenbase.rel.rules;

import java.util.Collections;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.RelDataTypeField;


/**
 * <code>UnionToDistinctRule</code> translates a distinct {@link UnionRel}
 * (<code>all</code> = <code>false</code>) into an {@link AggregateRel} on top
 * of a non-distinct {@link UnionRel} (<code>all</code> = <code>true</code>).
 */
public class UnionToDistinctRule
    extends RelOptRule
{
    public static final UnionToDistinctRule instance =
        new UnionToDistinctRule();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a UnionToDistinctRule.
     */
    private UnionToDistinctRule()
    {
        super(new RelOptRuleOperand(UnionRel.class, ANY));
    }

    //~ Methods ----------------------------------------------------------------

    public void onMatch(RelOptRuleCall call)
    {
        UnionRel union = (UnionRel) call.rels[0];
        if (!union.isDistinct()) {
            return; // nothing to do
        }
        UnionRel unionAll =
            new UnionRel(
                union.getCluster(),
                union.getInputs().clone(),
                true);
        call.transformTo(
            RelOptUtil.createDistinctRel(
                unionAll,
                // REVIEW: Is systemFieldList always empty? This rule could be
                // applied to a UNION that is aware of ROWTIME.
                Collections.<RelDataTypeField>emptyList()));
    }
}

// End UnionToDistinctRule.java
