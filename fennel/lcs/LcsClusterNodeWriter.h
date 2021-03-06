/*
// $Id$
// Fennel is a library of data storage and processing components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2010 SQLstream, Inc.
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

#ifndef Fennel_LcsClusterNodeWriter_Included
#define Fennel_LcsClusterNodeWriter_Included

#include "fennel/lcs/LcsClusterAccessBase.h"
#include "fennel/lcs/LcsBitOps.h"
#include "fennel/lcs/LcsClusterDump.h"
#include "fennel/btree/BTreeWriter.h"
#include "fennel/tuple/TupleData.h"
#include "fennel/tuple/UnalignedAttributeAccessor.h"
#include <boost/scoped_array.hpp>

FENNEL_BEGIN_NAMESPACE

const int LcsMaxRollBack = 8;
const int LcsMaxLeftOver = 7;
const int LcsMaxSzLeftError = 4;

enum ForceMode { none = 0, fixed = 1, variable = 2 };

/**
 * Constructs a cluster page, managing the amount of space currently in use
 * on the page and determining the offsets where different elements are to
 * be stored
 */
class FENNEL_LCS_EXPORT LcsClusterNodeWriter
    : public LcsClusterAccessBase, public TraceSource
{
private:
    /**
     * Writes btree corresponding to cluster
     */
    SharedBTreeWriter bTreeWriter;

    /**
     * Accessor for scratch segments
     */
    SegmentAccessor scratchAccessor;

    /**
     * Lock on scratch page
     */
    ClusterPageLock bufferLock;

    /**
     * Cluster page header
     */
    PLcsClusterNode pHdr;

    /**
     * Size of the cluster page header
     */
    uint hdrSize;

    /**
     * Cluster page to be written
     */
    PBuffer pIndexBlock;

    /**
     * Array of pointers to temporary blocks, 1 block for each column cluster
     */
    PBuffer *pBlock;

    /**
     * Size of the cluster page
     */
    uint szBlock;

    /**
     * Minimum size left on the page
     */
    int minSzLeft;

    /**
     * Batch directories for the batches currently being constructed,
     * one per cluster column
     */
    boost::scoped_array<LcsBatchDir> batchDirs;

    /**
     * Temporary storage for values, used for fixed mode batches; one per
     * cluster column
     */
    boost::scoped_array<PBuffer> pValBank;

    /**
     * First offset in the bank for each column in the cluster
     * value bank
     */
    boost::scoped_array<uint16_t> oValBank;

    /**
     * Start of each cluster column in the value bank
     */
    boost::scoped_array<uint16_t> valBankStart;

    /**
     * Offsets to the batch directories on the temporary pages,
     * one per cluster column
     */
    boost::scoped_array<uint16_t> batchOffset;

    /**
     * Count of the number of batches in the temporary pages, one per
     * cluster column
     */
    boost::scoped_array<uint> batchCount;

    /**
     * Number of bytes left on the page
     */
    int szLeft;

    /**
     * Number of bits required to store the value codes for each column
     * in the cluster, for the batches currently being constructed
     */
    boost::scoped_array<uint> nBits;

    /**
     * Number of values that will cause the next nBit change for the column
     * in the cluster
     */
    boost::scoped_array<uint> nextWidthChange;

    /**
     * Indicates whether temporary arrays have already been allocated
     */
    bool arraysAllocated;

    /**
     * Set when the mode of a batch should be forced to a particular value
     */
    boost::scoped_array<ForceMode> bForceMode;

    /**
     * Number of times force mode has been used for each cluster column
     */
    boost::scoped_array<uint> forceModeCount;

    /**
     * Max value size encountered thus far for each cluster column
     */
    boost::scoped_array<uint> maxValueSize;

    /**
     * Accessors for reading unaligned values.
     */
    boost::scoped_array<UnalignedAttributeAccessor> attrAccessors;

    /**
     * Cluster dump
     */
    SharedLcsClusterDump clusterDump;

    /**
     * Tuple descriptor of the columns being loaded
     */
    TupleDescriptor colTupleDesc;

    /**
     * Associates an offset with an address, determining whether a value is
     * stored in the temporary block or the temporary value bank
     *
     * @param lastValOffset offset of the last value for this particular column
     *
     * @param pValBank buffer storing values in the value bank
     *
     * @param oValBank offset of first value for column in the value bank
     *
     * @param pBlock temporary block for column
     *
     * @param f desired offset
     *
     * @return address corresponding to offset
     */
    PBuffer valueSource(
        uint16_t lastValOffset, PBuffer pValBank, uint16_t oValBank,
        PBuffer pBlock, uint16_t f)
    {
        // if value not in back use
        if (f < lastValOffset) {
            return pValBank + f - oValBank;
        } else {
            return pBlock + f;
        }
    }

    /**
     * Moves all cluster data from cluster page to temporary storage
     *
     * @return number of rows currently on page
     */
    RecordNum moveFromIndexToTemp();

    /**
     * Moves all cluster data from temporary storage to the actual
     * cluster page
     */
    void moveFromTempToIndex();

    /**
     * Allocates temporary arrays used during cluster writes
     */
    void allocArrays();

    /**
     * Rounds a 32-bit value to a boundary of 8
     *
     * @param val value to be rounded
     */
    inline uint32_t round8Boundary(uint32_t val)
    {
        return val & 0xfffffff8;
    }

    /**
     * Rounds a 32-bit value to a boundary of 8 if it is > 8
     *
     * @param val value to be rounded
     */
    inline uint32_t roundIf8Boundary(uint32_t val)
    {
        if (val > 8) {
            return round8Boundary(val);
        }
    }

public:
    explicit LcsClusterNodeWriter(
        BTreeDescriptor const &treeDescriptorInit,
        SegmentAccessor const &accessorInit,
        TupleDescriptor const &colTupleDescInit,
        SharedTraceTarget pTraceTargetInit,
        std::string nameInit);

    ~LcsClusterNodeWriter();

    /**
     * Gets the last cluster page
     *
     * @param pBlock output param returning the cluster page
     *
     * @param firstRid output param returning first rid stored on cluster page
     *
     * @return true if cluster is non-empty
     */
    bool getLastClusterPageForWrite(PLcsClusterNode &pBlock, LcsRid &firstRid);

    /**
     * Allocates a new cluster page
     *
     * @param firstRid first rid to be stored on cluster page
     *
     * @return page allocated
     */
    PLcsClusterNode allocateClusterPage(LcsRid firstRid);

    /**
     * Initializes object with parameters relevant to the cluster page that
     * will be written
     *
     * @param nColumns number of columns in the cluster
     *
     * @param indexBlock pointer to the cluster page to be written
     *
     * @param pBlock array of pointers to temporary pages to be used while
     * writing this cluster page
     *
     * @param szBlock size of cluster page, reflecting max amount of space
     * available to write cluster data
     */
    void init(uint nColumns, PBuffer indexBlock, PBuffer *pBlock, uint szBlock);

    void close();

    /**
     * Prepares a cluster page as a new one
     *
     * @param startRID first RID on the page
     */
    void openNew(LcsRid startRID);

    /**
     * Prepares an existing cluster page for appending new data, and determines
     * whether the page is already full and cannot accomodate any more data.
     *
     *
     * @param nValOffsets pointer to output array reflecting the number of
     * values currently in each column on this page
     *
     * @param lastValOffsets pointer to output array reflecting the offset of
     * the last value currently on the page for each cluster column
     *
     * @param nrows returns number of rows currently on page
     *
     * @return true if the page is already full
     */
    bool openAppend(
        uint *nValOffsets, uint16_t *lastValOffsets, RecordNum &nrows);

    /**
     * Returns parameters describing the last batch for a given column
     *
     * @param column the column to be described
     *
     * @param dRow output parameter returning the number of rows over
     * the multiple of 8 boundary
     *
     * @param recSize output parameter returning the record size for the
     * batch
     */
    void describeLastBatch(uint column, uint &dRow, uint &recSize);

    /**
     * Returns the offset of the next value in a batch
     *
     * @param column column we want the value for
     *
     * @param thisVal offset of the value currently positioned at
     */
    uint16_t getNextVal(uint column, uint16_t thisVal);

    /**
     * Rolls back the last 8 value (or less) from a batch
     *
     * @param column column to be rolled back
     *
     * @param pVal buffer where the rolled back values will be copied;
     * the buffer is assumed to be fixedRec * (nRows % 8) in size, as
     * determined by the last call to describeLastBatch
     */
    void rollBackLastBatch(uint column, PBuffer pVal);

    /**
     * Returns true if the batch is not being forced to compress mode
     *
     * @param column column being described
     */
    inline bool noCompressMode(uint column) const
    {
        return bForceMode[column] == fixed
            || bForceMode[column] == variable;
    };

    /**
     * Translates an offset for a column to the pointer to the actual value
     *
     * @param column offset corresponds to this column
     *
     * @param offset offset to be translated
     *
     * @return pointer to value
     */
    inline PBuffer getOffsetPtr(uint column, uint16_t offset)
    {
        return pBlock[column] + offset;
    };

    /**
     * Adds a value to the page, in the case where the value already exists
     * in the column
     *
     * @param column column corresponding to the value being added
     *
     * @param bFirstTimeInBatch true if this is the first time the value
     * is encountered for this batch
     *
     * @return true if there is enough room in the page for the value
     */
    bool addValue(uint column, bool bFirstTimeInBatch);

    /**
     * Adds a new value to the page.  In the case of compressed or variable
     * mode, adds the value to the bottom of the page.  In the case of
     * fixed mode, adds the value to the "value bank".
     *
     * @param column column corresponding to the value being added
     *
     * @param pVal value to be added
     *
     * @param oVal returns the offset where the value has been added
     *
     * @return true if there is enough room in the page for the value
     */
    bool addValue(uint column, PBuffer pVal, uint16_t *oVal);

    /**
     * Undoes the last value added to the current batch for a column
     *
     * @param column column corresponding to the value to be undone
     *
     * @param pVal value to be undone
     *
     * @param bFirstInBatch true if the value being undone is the first
     * such value for the batch
     */
    void undoValue(uint column, PBuffer pVal, bool bFirstInBatch);

    /**
     * Writes a compressed mode batch into the temporary cluster page for
     * a column.  Only a multiple of 8 rows is written, if this is not the
     * last batch in the cluster.
     *
     * Excess rows are written into a temporary buffer.  If this is the last
     * batch in the load, then it is ok to have < 8 rows, as the next load
     * will roll it back to fill it up with more rows.
     *
     * Note that it is assumed that the caller has already copied the
     * key offsets for this batch into the cluster page.  This call will
     * only copy the bit vectors and batch directory corresponding to this
     * batch
     *
     * @param column column corresponding to the batch
     *
     * @param pRows array mapping rows to key offsets
     *
     * @param pBuf temporary buffer where excess row values will be copied;
     * assumed to be (nRow % 8)*fixedRec big
     */
    void putCompressedBatch(uint column, PBuffer pRows, PBuffer pBuf);

    /**
     * Writes a fixed or variable mode batch into a temporary cluster page for
     * a column.  Only a multiple of 8 rows is written, if this is not the
     * last batch in the cluster.
     *
     * Excess rows are written into a temporary buffer.  If this is the last
     * batch in the load, then it is ok to have < 8 rows, as the next load
     * will roll it back to fill it up with more rows.
     *
     * In the variable mode case, the key offsets are written to the batch
     * area on the page.  In the fixed mode case, the values themselves are
     * written to the batch area.  In both cases, the batch directory is
     * also written out.
     *
     * @param column column corresponding to the batch
     *
     * @param pRows array of offsets to values
     *
     * @param pBuf temporary buffer where excess row values will be copied;
     * assumed to be (nRow % 8)*fixedRec big
     */
    void putFixedVarBatch(uint column, uint16_t *pRows, PBuffer pBuf);

    /**
     * Determines which compression mode to use for a batch
     *
     * @param column column for which compression mode is being determined
     *
     * @param fixedSize size of record in the case of fixed size compression
     *
     * @param nRows number of rows in the batch
     *
     * @param pValOffset returns a pointer to the offset of the start of
     * the batch
     *
     * @param compressionMode returns the chosen compression mode
     */
    void pickCompressionMode(
        uint column, uint fixedSize, uint nRows,
        uint16_t **pValOffset,
        LcsBatchMode &compressionMode);

    /**
     * Returns true if there is no space left in the cluster page
     */
    bool isEndOfBlock()
    {
        uint col;
        int valueSizeNeeded;

        for (valueSizeNeeded = 0, col = 0; col < nClusterCols; col++) {
            valueSizeNeeded += batchDirs[col].recSize * LcsMaxLeftOver;
        }

        return szLeft <= (minSzLeft + valueSizeNeeded);
    }

    /**
     * Done with the current cluster page.  Moves all data from temporary
     * pages into the real cluster page
     */
    void endBlock()
    {
        moveFromTempToIndex();
    }
};

FENNEL_END_NAMESPACE

#endif

// End LcsClusterNodeWriter.h
