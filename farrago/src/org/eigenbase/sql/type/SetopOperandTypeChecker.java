/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
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
package org.eigenbase.sql.type;

import java.util.*;

import org.eigenbase.reltype.*;
import org.eigenbase.resource.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.validate.*;
import org.eigenbase.util.*;


/**
 * Parameter type-checking strategy for a set operator (UNION, INTERSECT,
 * EXCEPT).
 *
 * <p>Both arguments must be records with the same number of fields, and the
 * fields must be union-compatible.
 *
 * @author Jack Frost
 * @version $Id$
 */
public class SetopOperandTypeChecker
    implements SqlOperandTypeChecker
{
    //~ Methods ----------------------------------------------------------------

    public boolean checkOperandTypes(
        SqlCallBinding callBinding,
        boolean throwOnFailure)
    {
        assert callBinding.getOperandCount() == 2
            : "setops are binary (for now)";
        RelDataType [] argTypes =
            new RelDataType[callBinding.getOperandCount()];
        int colCount = -1;
        final SqlValidator validator = callBinding.getValidator();
        for (int i = 0; i < argTypes.length; i++) {
            final RelDataType argType =
                argTypes[i] = callBinding.getOperandType(i);
            Util.permAssert(
                argType.isStruct(),
                "setop arg must be a struct");

            // Each operand must have the same number of columns.
            final RelDataTypeField [] fields = argType.getFields();
            if (i == 0) {
                colCount = fields.length;
                continue;
            }

            if (fields.length != colCount) {
                if (throwOnFailure) {
                    SqlNode node = callBinding.getCall().getOperands()[i];
                    if (node instanceof SqlSelect) {
                        node = ((SqlSelect) node).getSelectList();
                    }
                    throw validator.newValidationError(
                        node,
                        EigenbaseResource.instance().ColumnCountMismatchInSetop
                        .ex(
                            callBinding.getOperator().getName()));
                } else {
                    return false;
                }
            }
        }

        // The columns must be pairwise union compatible. For each column
        // ordinal, form a 'slice' containing the types of the ordinal'th
        // column j.
        RelDataType [] colTypes =
            new RelDataType[callBinding.getOperandCount()];
        for (int i = 0; i < colCount; i++) {
            for (int j = 0; j < argTypes.length; j++) {
                final RelDataTypeField field = argTypes[j].getFields()[i];
                colTypes[j] = field.getType();
            }
            final RelDataType type =
                callBinding.getTypeFactory().leastRestrictive(colTypes);
            if (type == null) {
                if (throwOnFailure) {
                    SqlNode field =
                        SqlUtil.getSelectListItem(
                            callBinding.getCall().operands[0],
                            i);
                    throw validator.newValidationError(
                        field,
                        EigenbaseResource.instance().ColumnTypeMismatchInSetop
                        .ex(
                            i + 1, // 1-based
                            callBinding.getOperator().getName()));
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    public SqlOperandCountRange getOperandCountRange()
    {
        return SqlOperandCountRange.Two;
    }

    public String getAllowedSignatures(SqlOperator op, String opName)
    {
        return "{0} " + opName + " {1}"; // todo: Wael, please review.
    }
}

// End SetopOperandTypeChecker.java
