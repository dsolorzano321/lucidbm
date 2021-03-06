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
package org.eigenbase.sql.fun;

import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;
import org.eigenbase.sql.type.*;
import org.eigenbase.sql.validate.*;


/**
 * The <code>POSITION</code> function.
 *
 * @author Wael Chatila
 * @version $Id$
 */
public class SqlPositionFunction
    extends SqlFunction
{
    //~ Constructors -----------------------------------------------------------

    // FIXME jvs 25-Jan-2009:  POSITION should verify that
    // params are all same character set, like OVERLAY does implicitly
    // as part of rtiDyadicStringSumPrecision

    public SqlPositionFunction()
    {
        super(
            "POSITION",
            SqlKind.OTHER_FUNCTION,
            SqlTypeStrategies.rtiNullableInteger,
            null,
            SqlTypeStrategies.otcStringSameX2,
            SqlFunctionCategory.Numeric);
    }

    //~ Methods ----------------------------------------------------------------

    public void unparse(
        SqlWriter writer,
        SqlNode [] operands,
        int leftPrec,
        int rightPrec)
    {
        final SqlWriter.Frame frame = writer.startFunCall(getName());
        operands[0].unparse(writer, leftPrec, rightPrec);
        writer.sep("IN");
        operands[1].unparse(writer, leftPrec, rightPrec);
        writer.endFunCall(frame);
    }

    public String getSignatureTemplate(final int operandsCount)
    {
        switch (operandsCount) {
        case 2:
            return "{0}({1} IN {2})";
        }
        assert (false);
        return null;
    }

    public boolean checkOperandTypes(
        SqlCallBinding callBinding,
        boolean throwOnFailure)
    {
        SqlValidator validator = callBinding.getValidator();
        SqlCall call = callBinding.getCall();

        //check that the two operands are of same type.
        RelDataType type0 = validator.getValidatedNodeType(call.operands[0]);
        RelDataType type1 = validator.getValidatedNodeType(call.operands[1]);
        if (!SqlTypeUtil.inSameFamily(type0, type1)) {
            if (throwOnFailure) {
                throw callBinding.newValidationSignatureError();
            }
            return false;
        }

        return getOperandTypeChecker().checkOperandTypes(
            callBinding,
            throwOnFailure);
    }
}

// End SqlPositionFunction.java
