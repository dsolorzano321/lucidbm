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
package net.sf.farrago.ddl;

import java.util.*;

import javax.jmi.reflect.*;

import net.sf.farrago.catalog.*;
import net.sf.farrago.cwm.core.*;
import net.sf.farrago.fem.security.*;
import net.sf.farrago.resource.*;
import net.sf.farrago.session.*;

import org.eigenbase.sql.*;


/**
 * DdlGrantPrivStmt represents a DDL GRANT privileges statement.
 *
 * @author Quoc Tai Tran
 * @version $Id$
 */
public class DdlGrantPrivStmt
    extends DdlGrantStmt
{
    //~ Instance fields --------------------------------------------------------

    private CwmModelElement grantedObject;
    private List<SqlIdentifier> privList;
    private boolean hierarchyOption;
    private SqlIdentifier grantor;

    //~ Constructors -----------------------------------------------------------

    /**
     * Constructs a new DdlGrantPrivStmt.
     */
    public DdlGrantPrivStmt()
    {
        super();
    }

    //~ Methods ----------------------------------------------------------------

    // implement DdlStmt
    public void visit(DdlVisitor visitor)
    {
        visitor.visit(this);
    }

    // implement FarragoSessionDdlStmt
    public void preValidate(FarragoSessionDdlValidator ddlValidator)
    {
        FarragoRepos repos = ddlValidator.getRepos();

        // Initialize the privilege lookup table.
        validatePrivileges(ddlValidator);

        FemAuthId grantorAuthId = determineGrantor(ddlValidator);
        FemUser user = null;
        FemRole role = null;
        if (grantorAuthId instanceof FemUser) {
            user = (FemUser) grantorAuthId;
        } else {
            role = (FemRole) grantorAuthId;
        }

        FarragoSessionPrivilegeChecker privChecker =
            ddlValidator.getStmtValidator().getPrivilegeChecker();
        for (SqlIdentifier id : privList) {
            privChecker.requestAccess(
                grantedObject,
                user,
                role,
                id.getSimple(),
                true);
        }
        privChecker.checkAccess();

        for (SqlIdentifier id : granteeList) {
            // Find the repository element id for the grantee
            FemAuthId granteeAuthId =
                FarragoCatalogUtil.getAuthIdByName(
                    repos,
                    id.getSimple());
            if (granteeAuthId == null) {
                throw FarragoResource.instance().ValidatorInvalidGrantee.ex(
                    repos.getLocalizedObjectName(id.getSimple()));
            }

            // For each privilege in the list, we instantiate a repository
            // element. Note that this makes it easier to revoke the privs on
            // an individual basis.
            for (SqlIdentifier privId : privList) {
                // Duplicate check
                FemGrant grant = findExistingGrant(
                    repos,
                    grantedObject,
                    grantorAuthId,
                    granteeAuthId,
                    privId.getSimple());
                if (grant == null) {
                    // Create the grant object and set its properties.
                    grant = repos.newFemGrant();

                    // Set the privilege name (i.e. action) and properties.
                    grant.setAction(privId.getSimple());

                    // Associate the privilege with the grantor, grantee,
                    // and object.
                    grant.setGrantor(grantorAuthId);
                    grant.setGrantee(granteeAuthId);
                    grant.setElement(grantedObject);
                }

                // Note that for an existing grant without grant option, we
                // upgrade in place.
                if (grantOption) {
                    grant.setWithGrantOption(true);
                }
            }
        }
    }

    public void setPrivList(List<SqlIdentifier> privList)
    {
        this.privList = privList;
    }

    public void setGrantedObject(CwmModelElement grantedObject)
    {
        this.grantedObject = grantedObject;
    }

    public void setGrantor(SqlIdentifier grantor)
    {
        this.grantor = grantor;
    }

    public void setHierarchyOption(boolean hierarchyOption)
    {
        this.hierarchyOption = hierarchyOption;
    }

    private void validatePrivileges(FarragoSessionDdlValidator ddlValidator)
    {
        FarragoSession session = ddlValidator.getStmtValidator().getSession();
        Set<String> legalPrivSet =
            session.getPrivilegeMap().getLegalPrivilegesForType(
                grantedObject.refClass());
        for (SqlIdentifier privId : privList) {
            if (!legalPrivSet.contains(privId.getSimple())) {
                // throw an exception, because this is an illegal privilege
                // REVIEW jvs 13-Aug-2005:  maybe report all illegal
                // privileges at once instead of just the first one?
                // Goes for other stuff above too such as existence
                // of grantee.
                throw FarragoResource.instance().ValidatorInvalidGrant.ex(
                    privId.getSimple(),
                    grantedObject.getName());
            }
        }
    }
}

// End DdlGrantPrivStmt.java
