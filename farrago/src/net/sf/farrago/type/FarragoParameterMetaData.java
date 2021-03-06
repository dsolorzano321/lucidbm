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
package net.sf.farrago.type;

import java.sql.*;
import java.util.*;

import org.eigenbase.reltype.*;


/**
 * FarragoParameterMetaData implements the ParameterMetaData interface by
 * reading a Farrago type descriptor.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class FarragoParameterMetaData
    extends FarragoJdbcMetaDataImpl
    implements ParameterMetaData
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FarragoParameterMetaData object.
     *
     * @param rowType type info to return
     */
    public FarragoParameterMetaData(RelDataType rowType)
    {
        super(
            rowType,
            Collections.<List<String>>nCopies(rowType.getFieldCount(), null));
    }

    //~ Methods ----------------------------------------------------------------

    // implement ParameterMetaData
    public String getParameterClassName(int param)
        throws SQLException
    {
        return getFieldClassName(param);
    }

    // implement ParameterMetaData
    public int getParameterCount()
        throws SQLException
    {
        return getFieldCount();
    }

    // implement ParameterMetaData
    public int getParameterMode(int param)
        throws SQLException
    {
        return ParameterMetaData.parameterModeIn;
    }

    // implement ParameterMetaData
    public int getParameterType(int param)
        throws SQLException
    {
        return getFieldJdbcType(param);
    }

    // implement ParameterMetaData
    public String getParameterTypeName(int param)
        throws SQLException
    {
        return getFieldTypeName(param);
    }

    // implement ParameterMetaData
    public int getPrecision(int param)
        throws SQLException
    {
        return getFieldPrecision(param);
    }

    // implement ParameterMetaData
    public int getScale(int param)
        throws SQLException
    {
        return getFieldScale(param);
    }

    // implement ParameterMetaData
    public int isNullable(int param)
        throws SQLException
    {
        return isFieldNullable(param);
    }

    // implement ParameterMetaData
    public boolean isSigned(int param)
        throws SQLException
    {
        return isFieldSigned(param);
    }
}

// End FarragoParameterMetaData.java
