/*
// $Id$
// Fennel is a library of data storage and processing components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 1999 John V. Sichi
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

#ifndef Fennel_ScratchMemExcn_Included
#define Fennel_ScratchMemExcn_Included

#include "fennel/common/FennelExcn.h"
#include "fennel/common/FennelResource.h"

using namespace std;

FENNEL_BEGIN_NAMESPACE

/**
 * Exception class for scratch memory allocation errors
 */
class FENNEL_SEGMENT_EXPORT ScratchMemExcn
    : public FennelExcn
{
public:
    /**
     * Constructs a new ScratchMemExcn.
     */
    explicit ScratchMemExcn()
        : FennelExcn(FennelResource::instance().scratchMemExhausted())
    {
    }
};

FENNEL_END_NAMESPACE

#endif

// End ScratchMemExcn.h
