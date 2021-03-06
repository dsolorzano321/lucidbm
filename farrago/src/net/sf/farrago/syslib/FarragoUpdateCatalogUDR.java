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
package net.sf.farrago.syslib;

import java.sql.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.fem.config.*;
import net.sf.farrago.runtime.*;
import net.sf.farrago.session.*;


/**
 * FarragoUpdateCatalogUDR implements system procedures for updating Farrago
 * catalogs (intended for use as part of installation).
 *
 * @author Zelaine Fong
 * @version $Id$
 */
public class FarragoUpdateCatalogUDR
    extends FarragoAbstractCatalogInit
{
    //~ Constructors -----------------------------------------------------------

    private FarragoUpdateCatalogUDR(FarragoRepos repos)
    {
        super(repos);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Updates Farrago config objects in cases where they have been set to dummy
     * values.
     */
    public static void updateConfiguration()
    {
        tracer.info("Updating Farrago system parameters");

        //FarragoSession session = FarragoUdrRuntime.getSession();
        //FarragoRepos repos = session.getRepos();
        FarragoRepos repos = FarragoUdrRuntime.getRepos();

        FarragoUpdateCatalogUDR init = null;
        FarragoReposTxnContext txn = repos.newTxnContext(true);
        boolean rollback = true;
        try {
            try {
                txn.beginWriteTxn();
                init = new FarragoUpdateCatalogUDR(repos);
                init.updateSystemParameters();
                rollback = false;
            } finally {
                if (init != null) {
                    // Guarantee that publishObjects is called
                    init.publishObjects(rollback);
                }
            }
        } finally {
            // Guarantee that the txn is cleaned up
            if (rollback) {
                txn.rollback();
            } else {
                txn.commit();
            }
        }
        tracer.info("Update of Farrago system parameters complete");
    }

    /**
     * Updates catalogs with system objects, adding them if they don't exist
     * yet.
     */
    public static void updateSystemObjects()
    {
        tracer.info("Updating system-owned catalog objects");
        FarragoRepos repos = FarragoUdrRuntime.getRepos();

        FarragoUpdateCatalogUDR init = null;
        FarragoReposTxnContext txn = repos.newTxnContext(true);
        boolean rollback = true;
        try {
            try {
                txn.beginWriteTxn();
                init = new FarragoUpdateCatalogUDR(repos);
                init.updateSystemTypes();
                rollback = true;
            } finally {
                if (init != null) {
                    // Guarantee that publishObjects is called
                    init.publishObjects(rollback);
                }
            }
        } finally {
            // Guarantee that the txn is cleaned up
            if (rollback) {
                txn.rollback();
            } else {
                txn.commit();
            }
        }
        tracer.info("Update of system-owned catalog objects committed");
    }
}

// End FarragoUpdateCatalogUDR.java
