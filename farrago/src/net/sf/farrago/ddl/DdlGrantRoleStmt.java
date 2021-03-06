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
 * DdlGrantRoleStmt represents a DDL GRANT ROLE statement.
 *
 * @author Quoc Tai Tran
 * @version $Id$
 */
public class DdlGrantRoleStmt
    extends DdlGrantStmt
{
    //~ Instance fields --------------------------------------------------------

    protected List<SqlIdentifier> roleList;

    //~ Constructors -----------------------------------------------------------

    /**
     * Constructs a new DdlGrantRoleStmt.
     */
    public DdlGrantRoleStmt()
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

        List<FemRole> grantedRoles = new ArrayList<FemRole>();
        for (SqlIdentifier roleId : roleList) {
            FemRole grantedRole =
                FarragoCatalogUtil.getRoleByName(
                    repos, roleId.getSimple());
            if (grantedRole == null) {
                throw FarragoResource.instance().ValidatorInvalidRole.ex(
                    repos.getLocalizedObjectName(roleId.getSimple()));
            }
            grantedRoles.add(grantedRole);
        }

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
        for (FemRole grantedRole : grantedRoles) {
            privChecker.requestAccess(
                grantedRole,
                user,
                role,
                PrivilegedActionEnum.INHERIT_ROLE.toString(),
                true);
        }
        privChecker.checkAccess();

        for (SqlIdentifier granteeId : granteeList) {
            // Find the repository element id for the grantee.
            FemAuthId granteeAuthId =
                FarragoCatalogUtil.getAuthIdByName(
                    repos,
                    granteeId.getSimple());
            if (granteeAuthId == null) {
                throw FarragoResource.instance().ValidatorInvalidGrantee.ex(
                    repos.getLocalizedObjectName(granteeId.getSimple()));
            }

            // For each role in the list, we instantiate a repository element
            // for the grant. Note that this makes it easier to revoke the
            // privs on an individual basis.
            for (FemRole grantedRole : grantedRoles) {
                // we could probably gang all of these up into a single
                // LURQL query, but for now execute one check per
                // granted role
                checkCycle(
                    ddlValidator.getInvokingSession(),
                    grantedRole,
                    granteeAuthId);

                // create a privilege object and set its properties
                FemGrant grant = findExistingGrant(
                    repos,
                    grantedRole,
                    grantorAuthId,
                    granteeAuthId,
                    PrivilegedActionEnum.INHERIT_ROLE.toString());
                if (grant == null) {
                    grant =
                        FarragoCatalogUtil.newRoleGrant(
                            repos,
                            grantorAuthId,
                            granteeAuthId,
                            grantedRole);
                }
                // Note that for an existing grant without admin option, we
                // upgrade in place.
                if (grantOption) {
                    grant.setWithGrantOption(true);
                }
            }
        }
    }

    private void checkCycle(
        FarragoSession session, FemAuthId grantedRole, FemAuthId granteeRole)
    {
        String lurql =
            FarragoInternalQuery.instance().SecurityRoleCycleCheck.str();
        Map<String, String> argMap = new HashMap<String, String>();
        argMap.put("grantedRoleName", grantedRole.getName());
        Collection<RefObject> result =
            session.executeLurqlQuery(
                lurql,
                argMap);
        for (RefObject o : result) {
            FemRole role = (FemRole) o;
            if (role.getName().equals(granteeRole.getName())) {
                throw FarragoResource.instance().ValidatorRoleCycle.ex(
                    session.getRepos().getLocalizedObjectName(grantedRole),
                    session.getRepos().getLocalizedObjectName(granteeRole));
            }
        }
    }

    public void setRoleList(List<SqlIdentifier> roleList)
    {
        this.roleList = roleList;
    }
}

// End DdlGrantRoleStmt.java
