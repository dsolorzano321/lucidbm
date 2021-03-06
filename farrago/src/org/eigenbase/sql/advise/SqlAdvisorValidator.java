/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2002 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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
package org.eigenbase.sql.advise;

import java.util.*;

import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.sql.validate.*;
import org.eigenbase.util.*;


/**
 * <code>SqlAdvisorValidator</code> is used by {@link SqlAdvisor} to traverse
 * the parse tree of a SQL statement, not for validation purpose but for setting
 * up the scopes and namespaces to facilitate retrieval of SQL statement
 * completion hints.
 *
 * @author tleung
 * @version $Id$
 * @since Jan 16, 2005
 */
public class SqlAdvisorValidator
    extends SqlValidatorImpl
{
    //~ Instance fields --------------------------------------------------------

    private final Set<SqlValidatorNamespace> activeNamespaces =
        new HashSet<SqlValidatorNamespace>();

    private final RelDataType emptyStructType =
        SqlTypeUtil.createEmptyStructType(typeFactory);

    private final List<RelDataTypeField> systemFieldList;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a SqlAdvisor validator.
     *
     * @param opTab Operator table
     * @param catalogReader Catalog reader
     * @param typeFactory Type factory
     * @param conformance Compatibility mode
     *
     * @pre opTab != null
     * @pre // node is a "query expression" (per SQL standard)
     * @pre catalogReader != null
     * @pre typeFactory != null
     */
    public SqlAdvisorValidator(
        SqlOperatorTable opTab,
        SqlValidatorCatalogReader catalogReader,
        RelDataTypeFactory typeFactory,
        SqlConformance conformance)
    {
        super(opTab, catalogReader, typeFactory, conformance);
        this.systemFieldList = getTypeFactory().getSystemFieldList();
    }

    //~ Methods ----------------------------------------------------------------

    public List<RelDataTypeField> getSystemFields()
    {
        return systemFieldList;
    }

    /**
     * Registers the identifier and its scope into a map keyed by ParserPostion.
     */
    public void validateIdentifier(SqlIdentifier id, SqlValidatorScope scope)
    {
        registerId(id, scope);
        try {
            super.validateIdentifier(id, scope);
        } catch (EigenbaseException e) {
            Util.swallow(e, tracer);
        }
    }

    /**
     * Registers an identifier.
     *
     * @param id Identifier
     * @param scope Scope
     */
    private void registerId(
        SqlIdentifier id,
        SqlValidatorScope scope)
    {
        for (int i = 0; i < id.names.length; i++) {
            final SqlParserPos subPos = id.getComponentParserPosition(i);
            final List<String> nameList = Arrays.asList(id.names);
            SqlIdentifier subId =
                (i == (id.names.length - 1)) ? id
                : new SqlIdentifier(
                    nameList.subList(0, i + 1).toArray(new String[i]),
                    subPos);
            idPositions.put(
                subPos.toString(),
                new IdInfo(scope, subId));
        }
    }

    public SqlNode expand(SqlNode expr, SqlValidatorScope scope)
    {
        // Disable expansion. It doesn't help us come up with better hints.
        return expr;
    }

    public SqlNode expandOrderExpr(SqlSelect select, SqlNode orderExpr)
    {
        // Disable expansion. It doesn't help us come up with better hints.
        return orderExpr;
    }

    /**
     * Calls the parent class method and mask Farrago exception thrown.
     */
    public RelDataType deriveType(
        SqlValidatorScope scope,
        SqlNode operand)
    {
        // REVIEW Do not mask Error (indicates a serious system problem) or
        // UnsupportedOperationException (a bug). I have to mask
        // UnsupportedOperationException because
        // SqlValidatorImpl.getValidatedNodeType throws it for an unrecognized
        // identifier node I have to mask Error as well because
        // AbstractNamespace.getRowType  called in super.deriveType can do a
        // Util.permAssert that throws Error
        try {
            return super.deriveType(scope, operand);
        } catch (EigenbaseException e) {
            return unknownType;
        } catch (UnsupportedOperationException e) {
            return unknownType;
        } catch (Error e) {
            return unknownType;
        }
    }

    // we do not need to validate from clause for traversing the parse tree
    // because there is no SqlIdentifier in from clause that need to be
    // registered into {@link #idPositions} map
    protected void validateFrom(
        SqlNode node,
        RelDataType targetRowType,
        SqlValidatorScope scope)
    {
        try {
            super.validateFrom(node, targetRowType, scope);
        } catch (EigenbaseException e) {
            Util.swallow(e, tracer);
        }
    }

    /**
     * Calls the parent class method and masks Farrago exception thrown.
     */
    protected void validateWhereClause(SqlSelect select)
    {
        try {
            super.validateWhereClause(select);
        } catch (EigenbaseException e) {
            Util.swallow(e, tracer);
        }
    }

    /**
     * Calls the parent class method and masks Farrago exception thrown.
     */
    protected void validateHavingClause(SqlSelect select)
    {
        try {
            super.validateHavingClause(select);
        } catch (EigenbaseException e) {
            Util.swallow(e, tracer);
        }
    }

    protected void validateOver(SqlCall call, SqlValidatorScope scope)
    {
        try {
            final OverScope overScope = (OverScope) getOverScope(call);
            final SqlNode relation = call.operands[0];
            validateFrom(relation, unknownType, scope);
            final SqlNode window = call.operands[1];
            SqlValidatorScope opScope = scopes.get(relation);
            if (opScope == null) {
                opScope = overScope;
            }
            validateWindow(window, opScope, null);
        } catch (EigenbaseException e) {
            Util.swallow(e, tracer);
        }
    }

    protected void validateNamespace(final SqlValidatorNamespace namespace)
    {
        // Only attempt to validate each namespace once. Otherwise if
        // validation fails, we may end up cycling.
        if (activeNamespaces.add(namespace)) {
            super.validateNamespace(namespace);
        } else {
            namespace.setRowType(emptyStructType, emptyStructType);
        }
    }

    protected boolean shouldAllowOverRelation()
    {
        return true; // no reason not to be lenient
    }
}

// End SqlAdvisorValidator.java
