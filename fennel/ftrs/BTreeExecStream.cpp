/*
// $Id$
// Fennel is a library of data storage and processing components.
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

#include "fennel/common/CommonPreamble.h"
#include "fennel/ftrs/BTreeExecStream.h"
#include "fennel/btree/BTreeWriter.h"

FENNEL_BEGIN_CPPFILE("$Id$");

void BTreeExecStream::prepare(BTreeExecStreamParams const &params)
{
    SingleOutputExecStream::prepare(params);

    copyParamsToDescriptor(treeDescriptor, params, params.pCacheAccessor);
    scratchAccessor = params.scratchAccessor;
    pRootMap = params.pRootMap;
    rootPageIdParamId = params.rootPageIdParamId;
}

void BTreeExecStream::open(bool restart)
{
    SingleOutputExecStream::open(restart);
    if (restart) {
        endSearch();
    }
    if (!restart) {
        if (pRootMap) {
            treeDescriptor.rootPageId = pRootMap->getRoot(
                treeDescriptor.pageOwnerId);
            if (pBTreeAccessBase) {
                pBTreeAccessBase->setRootPageId(treeDescriptor.rootPageId);
            }
        }
    }
}

void BTreeExecStream::closeImpl()
{
    endSearch();
    if (pRootMap && pBTreeAccessBase) {
        treeDescriptor.rootPageId = NULL_PAGE_ID;
        pBTreeAccessBase->setRootPageId(NULL_PAGE_ID);
    }
    SingleOutputExecStream::closeImpl();
}

SharedBTreeReader BTreeExecStream::newReader()
{
    SharedBTreeReader pReader = SharedBTreeReader(
        new BTreeReader(treeDescriptor));
    pBTreeAccessBase = pBTreeReader = pReader;
    return pReader;
}

SharedBTreeWriter BTreeExecStream::newWriter(bool monotonic)
{
    SharedBTreeWriter pWriter = SharedBTreeWriter(
        new BTreeWriter(treeDescriptor, scratchAccessor, monotonic));
    pBTreeAccessBase = pBTreeReader = pWriter;
    return pWriter;
}

SharedBTreeWriter BTreeExecStream::newWriter(
    BTreeExecStreamParams const &params)
{
    BTreeDescriptor treeDescriptor;
    copyParamsToDescriptor(treeDescriptor, params, params.pCacheAccessor);
    return SharedBTreeWriter(
        new BTreeWriter(
            treeDescriptor, params.scratchAccessor));
}

void BTreeExecStream::copyParamsToDescriptor(
    BTreeDescriptor &treeDescriptor,
    BTreeParams const &params,
    SharedCacheAccessor const &pCacheAccessor)
{
    treeDescriptor.segmentAccessor.pSegment = params.pSegment;
    treeDescriptor.segmentAccessor.pCacheAccessor = pCacheAccessor;
    treeDescriptor.tupleDescriptor = params.tupleDesc;
    treeDescriptor.keyProjection = params.keyProj;
    treeDescriptor.rootPageId = params.rootPageId;
    treeDescriptor.segmentId = params.segmentId;
    treeDescriptor.pageOwnerId = params.pageOwnerId;
}

void BTreeExecStream::endSearch()
{
    if (pBTreeReader && pBTreeReader->isSingular() == false) {
        pBTreeReader->endSearch();
    }
}

BTreeExecStreamParams::BTreeExecStreamParams()
{
    pRootMap = NULL;
}

BTreeOwnerRootMap::~BTreeOwnerRootMap()
{
}

FENNEL_END_CPPFILE("$Id$");

// End BTreeExecStream.cpp
