/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 2004 John V. Sichi
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
package net.sf.farrago.session;

import java.sql.*;


/**
 * FarragoSessionConnectionSource creates new JDBC connections in the context of
 * a session.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public interface FarragoSessionConnectionSource
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Creates a new connection.
     *
     * @return new connection
     */
    public Connection newConnection();

    /**
     * Creates a new connection with session variables preset.
     *
     * @param sessionVariables session variables to set for new connection
     *
     * @return new connection
     */
    public Connection newConnection(FarragoSessionVariables sessionVariables);
}

// End FarragoSessionConnectionSource.java
