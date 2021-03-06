/*
// $Id$
// Farrago is an extensible data management system.
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
package net.sf.farrago.ojrex;

import net.sf.farrago.catalog.*;
import net.sf.farrago.runtime.*;

import openjava.mop.*;

import openjava.ptree.*;

import org.eigenbase.rex.*;


/**
 * This implementor writes code to retrieve the next value from a sequence.
 *
 * @author John Pham
 * @version $Id$
 */
class FarragoOJRexNextValueImplementor
    extends FarragoOJRexImplementor
{
    //~ Static fields/initializers ---------------------------------------------

    private static String GET_SEQUENCE_METHOD_NAME = "getSequenceAccessor";

    //~ Methods ----------------------------------------------------------------

    public Expression implementFarrago(
        FarragoRexToOJTranslator translator,
        RexCall call,
        Expression [] operands)
    {
        // make sequence a member variable to avoid retrieving it every
        // iteration
        Variable sequence = translator.newVariable();
        FieldDeclaration declaration =
            translator.newMember(
                ModifierList.PRIVATE,
                OJClass.forClass(FarragoSequenceAccessor.class),
                sequence,
                null);
        translator.addMember(declaration);

        // before processing a row, inialize the sequence if
        // it has not been initialized yet; no need to synchronize because
        // member is non-static
        Expression mofId = translator.toString(operands[0]);
        Expression expForSequence =
            new MethodCall(
                translator.getRelImplementor().getConnectionVariable(),
                GET_SEQUENCE_METHOD_NAME,
                new ExpressionList(mofId));
        Statement stmt = translator.setIfNull(sequence, expForSequence);
        translator.addStatement(stmt);

        // perform value access once per row; a variable is returned
        // to avoid recomputation of the expression
        OJClass ojClass = translator.typeToOJClass(call.getType());
        Variable value = translator.newVariable();
        Expression expForValue =
            new MethodCall(
                sequence,
                FarragoSequenceAccessor.NEXT_VALUE_METHOD_NAME,
                new ExpressionList());
        stmt = translator.declareLocalVariable(ojClass, value, expForValue);
        translator.addStatement(stmt);

        return value;
    }
}

// End FarragoOJRexNextValueImplementor.java
