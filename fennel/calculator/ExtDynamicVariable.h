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

#ifndef Fennel_ExtDynamicVariable_Included
#define Fennel_ExtDynamicVariable_Included

#include "fennel/calculator/RegisterReference.h"
#include "fennel/calculator/ExtendedInstruction.h"

FENNEL_BEGIN_NAMESPACE

//! dynamicVaraiable. Gets the dynamic variable corresponding to id and casts
//! into a 4 byte integer
void
dynamicVariable(
    RegisterRef<int> *result,
    RegisterRef<int> *id);

class ExtendedInstructionTable;

void
ExtDynamicVariableRegister(ExtendedInstructionTable* eit);


FENNEL_END_NAMESPACE

#endif

// End ExtDynamicVariable.h
