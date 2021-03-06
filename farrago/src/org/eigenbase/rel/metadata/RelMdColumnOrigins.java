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
package org.eigenbase.rel.metadata;

import java.util.*;

import org.eigenbase.rel.*;
import org.eigenbase.relopt.*;
import org.eigenbase.rex.*;
import org.eigenbase.util.Util;


/**
 * RelMdColumnOrigins supplies a default implementation of {@link
 * RelMetadataQuery#getColumnOrigins} for the standard logical algebra.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class RelMdColumnOrigins
    extends ReflectiveRelMetadataProvider
{
    //~ Constructors -----------------------------------------------------------

    public RelMdColumnOrigins()
    {
        // Tell superclass reflection about parameter types expected
        // for various metadata queries.

        // This corresponds to getColumnOrigins(rel, int iOutputColumn); note
        // that we don't specify the rel type because we always overload on
        // that.
        mapParameterTypes(
            "getColumnOrigins",
            Collections.<Class>singletonList(Integer.TYPE));
    }

    //~ Methods ----------------------------------------------------------------

    public Set<RelColumnOrigin> getColumnOrigins(
        AggregateRelBase rel,
        int iOutputColumn)
    {
        int n = iOutputColumn;
        final int sysFieldCount = rel.getSystemFieldList().size();
        final int groupCount = rel.getGroupSet().cardinality();

        // System columns pass through directly.
        if (n < sysFieldCount) {
            return invokeGetColumnOrigins(
                rel.getChild(),
                n);
        }
        n -= sysFieldCount;

        // Next, group columns.
        if (n < groupCount) {
            return invokeGetColumnOrigins(
                rel.getChild(),
                Util.toList(rel.getGroupSet()).get(n));
        }
        n -= groupCount;

        // Aggregate columns are derived from input columns
        AggregateCall call = rel.getAggCallList().get(n);

        Set<RelColumnOrigin> set = new HashSet<RelColumnOrigin>();
        for (Integer iInput : call.getArgList()) {
            Set<RelColumnOrigin> inputSet =
                invokeGetColumnOrigins(
                    rel.getChild(),
                    iInput);
            inputSet = createDerivedColumnOrigins(inputSet);
            if (inputSet != null) {
                set.addAll(inputSet);
            }
        }
        return set;
    }

    public Set<RelColumnOrigin> getColumnOrigins(
        JoinRelBase rel,
        int iOutputColumn)
    {
        final int sysFieldCount = rel.getSystemFieldList().size();
        iOutputColumn -= sysFieldCount;
        int nLeftColumns = rel.getLeft().getRowType().getFieldList().size();
        Set<RelColumnOrigin> set;
        boolean derived = false;
        if (iOutputColumn < nLeftColumns) {
            set =
                invokeGetColumnOrigins(
                    rel.getLeft(),
                    iOutputColumn);
            if (rel.getJoinType().generatesNullsOnLeft()) {
                derived = true;
            }
        } else {
            set =
                invokeGetColumnOrigins(
                    rel.getRight(),
                    iOutputColumn - nLeftColumns);
            if (rel.getJoinType().generatesNullsOnRight()) {
                derived = true;
            }
        }
        if (derived) {
            // nulls are generated due to outer join; that counts
            // as derivation
            set = createDerivedColumnOrigins(set);
        }
        return set;
    }

    public Set<RelColumnOrigin> getColumnOrigins(
        SetOpRel rel,
        int iOutputColumn)
    {
        Set<RelColumnOrigin> set = new HashSet<RelColumnOrigin>();
        for (RelNode input : rel.getInputs()) {
            Set inputSet =
                invokeGetColumnOrigins(
                    input,
                    iOutputColumn);
            if (inputSet == null) {
                return null;
            }
            set.addAll(inputSet);
        }
        return set;
    }

    public Set<RelColumnOrigin> getColumnOrigins(
        ProjectRelBase rel,
        int iOutputColumn)
    {
        final RelNode child = rel.getChild();
        RexNode rexNode = rel.getProjectExps()[iOutputColumn];

        if (rexNode instanceof RexInputRef) {
            // Direct reference:  no derivation added.
            RexInputRef inputRef = (RexInputRef) rexNode;
            return invokeGetColumnOrigins(
                child,
                inputRef.getIndex());
        }

        // Anything else is a derivation, possibly from multiple
        // columns.
        final Set<RelColumnOrigin> set = new HashSet<RelColumnOrigin>();
        RexVisitor visitor =
            new RexVisitorImpl<Void>(true) {
                public Void visitInputRef(RexInputRef inputRef)
                {
                    Set<RelColumnOrigin> inputSet =
                        invokeGetColumnOrigins(
                            child,
                            inputRef.getIndex());
                    if (inputSet != null) {
                        set.addAll(inputSet);
                    }
                    return null;
                }
            };
        rexNode.accept(visitor);

        return createDerivedColumnOrigins(set);
    }

    public Set<RelColumnOrigin> getColumnOrigins(
        FilterRelBase rel,
        int iOutputColumn)
    {
        return invokeGetColumnOrigins(
            rel.getChild(),
            iOutputColumn);
    }

    public Set<RelColumnOrigin> getColumnOrigins(
        SortRel rel,
        int iOutputColumn)
    {
        return invokeGetColumnOrigins(
            rel.getChild(),
            iOutputColumn);
    }

    public Set<RelColumnOrigin> getColumnOrigins(
        TableFunctionRelBase rel,
        int iOutputColumn)
    {
        Set<RelColumnOrigin> set = new HashSet<RelColumnOrigin>();
        Set<RelColumnMapping> mappings = rel.getColumnMappings();
        if (mappings == null) {
            if (rel.getInputs().length > 0) {
                // This is a non-leaf transformation:  say we don't
                // know about origins, because there are probably
                // columns below.
                return null;
            } else {
                // This is a leaf transformation: say there are fer sure no
                // column origins.
                return set;
            }
        }
        for (RelColumnMapping mapping : mappings) {
            if (mapping.iOutputColumn != iOutputColumn) {
                continue;
            }
            Set<RelColumnOrigin> origins =
                invokeGetColumnOrigins(
                    rel.getInputs()[mapping.iInputRel],
                    mapping.iInputColumn);
            if (origins == null) {
                return null;
            }
            if (mapping.isDerived) {
                origins = createDerivedColumnOrigins(origins);
            }
            set.addAll(origins);
        }
        return set;
    }

    // Catch-all rule when none of the others apply.
    public Set<RelColumnOrigin> getColumnOrigins(
        RelNode rel,
        int iOutputColumn)
    {
        // NOTE jvs 28-Mar-2006: We may get this wrong for a physical table
        // expression which supports projections.  In that case,
        // it's up to the plugin writer to override with the
        // correct information.

        if (rel.getInputs().length > 0) {
            // No generic logic available for non-leaf rels.
            return null;
        }

        Set<RelColumnOrigin> set = new HashSet<RelColumnOrigin>();

        RelOptTable table = rel.getTable();
        if (table == null) {
            // Somebody is making column values up out of thin air, like a
            // VALUES clause, so we return an empty set.
            return set;
        }

        // Detect the case where a physical table expression is performing
        // projection, and say we don't know instead of making any assumptions.
        // (Theoretically we could try to map the projection using column
        // names.)  This detection assumes the table expression doesn't handle
        // rename as well.
        if (table.getRowType() != rel.getRowType()) {
            return null;
        }

        set.add(new RelColumnOrigin(table, iOutputColumn, false));
        return set;
    }

    protected Set<RelColumnOrigin> invokeGetColumnOrigins(
        RelNode rel,
        int iOutputColumn)
    {
        return RelMetadataQuery.getColumnOrigins(rel, iOutputColumn);
    }

    private Set<RelColumnOrigin> createDerivedColumnOrigins(
        Set<RelColumnOrigin> inputSet)
    {
        if (inputSet == null) {
            return null;
        }
        Set<RelColumnOrigin> set = new HashSet<RelColumnOrigin>();
        for (RelColumnOrigin rco : inputSet) {
            RelColumnOrigin derived =
                new RelColumnOrigin(
                    rco.getOriginTable(),
                    rco.getOriginColumnOrdinal(),
                    true);
            set.add(derived);
        }
        return set;
    }
}

// End RelMdColumnOrigins.java
