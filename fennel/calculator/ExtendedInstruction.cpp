/*
// $Id$
// Fennel is a library of data storage and processing components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2004 SQLstream, Inc.
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

#include "fennel/common/CommonPreamble.h"
#include "fennel/calculator/ExtendedInstruction.h"

FENNEL_BEGIN_CPPFILE("$Id$");

// helpers

// -- ExtendedInstructionDef methods ------------------------------------------

string ExtendedInstructionDef::computeSignature()
{
    string sig = _name;
    sig += "(";
    for (uint i = 0; i < _parameterTypes.size(); i++) {
        if (i > 0) {
            sig += ",";
        }

        char const *pParamString =
            StandardTypeDescriptor::toString(_parameterTypes[i]);
        sig += pParamString;
    }
    sig += ")";
    return sig;
}

FENNEL_END_CPPFILE("$Id$");

// End ExtendedInstruction.cpp
