/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
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
package net.sf.farrago.namespace.mdr;

import java.util.*;
import java.util.List;

import javax.jmi.model.*;

import openjava.ptree.*;

import org.eigenbase.oj.rel.*;
import org.eigenbase.rel.*;
import org.eigenbase.rel.metadata.*;
import org.eigenbase.relopt.*;
import org.eigenbase.reltype.RelDataTypeField;
import org.eigenbase.rex.*;


/**
 * MedMdrJoinRel is the relational expression corresponding to a join via
 * association to an MedMdrClassExtent on the right hand side.
 *
 * @author John V. Sichi
 * @version $Id$
 */
class MedMdrJoinRel
    extends JoinRelBase
    implements JavaRel
{
    //~ Instance fields --------------------------------------------------------

    private int leftOrdinal;
    private Reference rightReference;

    //~ Constructors -----------------------------------------------------------

    MedMdrJoinRel(
        RelOptCluster cluster,
        RelNode left,
        RelNode right,
        RexNode condition,
        JoinRelType joinType,
        int leftOrdinal,
        Reference rightReference)
    {
        super(
            cluster,
            CallingConvention.ITERATOR.singletonSet,
            left,
            right,
            condition,
            joinType,
            Collections.<String>emptySet());
        assert ((joinType == JoinRelType.INNER)
            || (joinType == JoinRelType.LEFT));

        this.leftOrdinal = leftOrdinal;
        this.rightReference = rightReference;
    }

    //~ Methods ----------------------------------------------------------------

    int getLeftOrdinal()
    {
        return leftOrdinal;
    }

    Reference getRightReference()
    {
        return rightReference;
    }

    @Override
    public MedMdrJoinRel copy(
        RexNode conditionExpr,
        List<RelDataTypeField> systemFieldList,
        RelNode left,
        RelNode right)
    {
        return new MedMdrJoinRel(
            getCluster(),
            left,
            right,
            conditionExpr,
            joinType,
            leftOrdinal,
            rightReference);
    }

    // implement RelNode
    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        // TODO:  refine
        double rowCount = RelMetadataQuery.getRowCount(this);
        return planner.makeCost(
            rowCount,
            0,
            rowCount * getRowType().getFieldList().size());
    }

    // implement RelNode
    public double getRows()
    {
        if (rightReference == null) {
            // TODO:  selectivity
            // many-to-one
            return RelMetadataQuery.getRowCount(left);
        } else {
            // one-to-many:  assume a fanout of five, capped by the
            // total number of rows on the right
            return Math.min(
                5 * RelMetadataQuery.getRowCount(left),
                RelMetadataQuery.getRowCount(right));
        }
    }

    // implement RelNode
    public ParseTree implement(JavaRelImplementor implementor)
    {
        MedMdrJoinRelImplementor joinImplementor =
            new MedMdrJoinRelImplementor(this);
        return joinImplementor.implement(implementor);
    }
}

// End MedMdrJoinRel.java
