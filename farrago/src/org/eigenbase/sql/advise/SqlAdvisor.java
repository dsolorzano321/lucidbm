/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2002 SQLstream, Inc.
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
package org.eigenbase.sql.advise;

import java.io.*;

import java.util.*;
import java.util.logging.*;

import org.eigenbase.sql.*;
import org.eigenbase.sql.parser.*;
import org.eigenbase.sql.validate.*;
import org.eigenbase.trace.*;
import org.eigenbase.util.*;


/**
 * An assistant which offers hints and corrections to a partially-formed SQL
 * statement. It is used in the SQL editor user-interface.
 *
 * @author tleung
 * @version $Id$
 * @since Jan 16, 2004
 */
public class SqlAdvisor
{
    //~ Static fields/initializers ---------------------------------------------

    public static final Logger tracer = EigenbaseTrace.parserTracer;

    //~ Instance fields --------------------------------------------------------

    // Flags indicating precision/scale combinations
    private final SqlValidatorWithHints validator;
    private final String hintToken = "_suggest_";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a SqlAdvisor with a validator instance
     *
     * @param validator Validator
     */
    public SqlAdvisor(
        SqlValidatorWithHints validator)
    {
        this.validator = validator;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Gets completion hints for a partially completed or syntatically incorrect
     * sql statement with cursor pointing to the position where completion hints
     * are requested.
     *
     * <p>Writes into <code>replaced[0]</code> the string that is being
     * replaced. Includes the cursor and the preceding identifier. For example,
     * if <code>sql</code> is "select abc^de from t", sets <code>
     * replaced[0]</code> to "abc". If the cursor is in the middle of
     * whitespace, the replaced string is empty. The replaced string is never
     * null.
     *
     * @param sql A partial or syntatically incorrect sql statement for which to
     * retrieve completion hints
     * @param cursor to indicate the 0-based cursor position in the query at
     * @param replaced String which is being replaced (output)
     *
     * @return completion hints
     */
    public List<SqlMoniker> getCompletionHints(
        String sql,
        int cursor,
        String [] replaced)
    {
        // search backward starting from current position to find a "word"
        int wordStart = cursor;
        boolean quoted = false;
        while (
            (wordStart > 0)
            && Character.isJavaIdentifierPart(sql.charAt(wordStart - 1)))
        {
            --wordStart;
        }
        if ((wordStart > 0)
            && (sql.charAt(wordStart - 1) == '"'))
        {
            quoted = true;
            --wordStart;
        }

        if (wordStart < 0) {
            return Collections.emptyList();
        }

        // Search forwards to the end of the word we should remove. Eat up
        // trailing double-quote, if any
        int wordEnd = cursor;
        while (
            (wordEnd < sql.length())
            && Character.isJavaIdentifierPart(sql.charAt(wordEnd)))
        {
            ++wordEnd;
        }
        if (quoted
            && (wordEnd < sql.length())
            && (sql.charAt(wordEnd) == '"'))
        {
            ++wordEnd;
        }

        // remove the partially composed identifier from the
        // sql statement - otherwise we get a parser exception
        String word = replaced[0] = sql.substring(wordStart, cursor);
        if (wordStart < wordEnd) {
            sql =
                sql.substring(0, wordStart)
                + sql.substring(wordEnd, sql.length());
        }

        final List<SqlMoniker> completionHints =
            getCompletionHints0(sql, wordStart);

        // If cursor was part of the way through a word, only include hints
        // which start with that word in the result.
        final List<SqlMoniker> result;
        if (word.length() > 0) {
            result = new ArrayList<SqlMoniker>();
            if (quoted) {
                // Quoted identifier. Case-sensitive match.
                word = word.substring(1);
                for (SqlMoniker hint : completionHints) {
                    String cname = hint.toString();
                    if (cname.startsWith(word)) {
                        result.add(hint);
                    }
                }
            } else {
                // Regular identifier. Case-insensitive match.
                for (SqlMoniker hint : completionHints) {
                    String cname = hint.toString();
                    if ((cname.length() >= word.length())
                        && cname.substring(0, word.length()).equalsIgnoreCase(
                            word))
                    {
                        result.add(hint);
                    }
                }
            }
        } else {
            result = completionHints;
        }

        return result;
    }

    public List<SqlMoniker> getCompletionHints0(String sql, int cursor)
    {
        String simpleSql = simplifySql(sql, cursor);
        int idx = simpleSql.indexOf(hintToken);
        if (idx < 0) {
            return Collections.emptyList();
        }
        SqlParserPos pos = new SqlParserPos(1, idx + 1);
        return getCompletionHints(simpleSql, pos);
    }

    /**
     * Gets completion hints for a syntatically correct sql statement with dummy
     * SqlIdentifier
     *
     * @param sql A syntatically correct sql statement for which to retrieve
     * completion hints
     * @param pos to indicate the line and column position in the query at which
     * completion hints need to be retrieved. For example, "select a.ename,
     * b.deptno from sales.emp a join sales.dept b "on a.deptno=b.deptno where
     * empno=1"; setting pos to 'Line 1, Column 17' returns all the possible
     * column names that can be selected from sales.dept table setting pos to
     * 'Line 1, Column 31' returns all the possible table names in 'sales'
     * schema
     *
     * @return an array of hints ({@link SqlMoniker}) that can fill in at the
     * indicated position
     */
    public List<SqlMoniker> getCompletionHints(String sql, SqlParserPos pos)
    {
        // First try the statement they gave us. If this fails, just return
        // the tokens which were expected at the failure point.
        List<SqlMoniker> hintList = new ArrayList<SqlMoniker>();
        SqlNode sqlNode = tryParse(sql, hintList);
        if (sqlNode == null) {
            return hintList;
        }

        // Now construct a statement which is bound to fail. (Character 7 BEL
        // is not legal in any SQL statement.)
        final int x = pos.getColumnNum() - 1;
        sql = sql.substring(0, x)
            + " \07"
            + sql.substring(x);
        tryParse(sql, hintList);

        // Add the identifiers which are expected at the point of interest.
        try {
            validator.validate(sqlNode);
        } catch (Exception e) {
            // mask any exception that is thrown during the validation, i.e.
            // try to continue even if the sql is invalid. we are doing a best
            // effort here to try to come up with the requested completion
            // hints
            Util.swallow(e, tracer);
        }
        final List<SqlMoniker> validatorHints =
            validator.lookupHints(sqlNode, pos);
        hintList.addAll(validatorHints);
        return hintList;
    }

    /**
     * Tries to parse a SQL statement.
     *
     * <p>If succeeds, returns the parse tree node; if fails, populates the list
     * of hints and returns null.
     *
     * @param sql SQL statement
     * @param hintList List of hints suggesting allowable tokens at the point of
     * failure
     *
     * @return Parse tree if succeeded, null if parse failed
     */
    private SqlNode tryParse(String sql, List<SqlMoniker> hintList)
    {
        try {
            return parseQuery(sql);
        } catch (SqlParseException e) {
            for (String tokenName : e.getExpectedTokenNames()) {
                // Only add tokens which are keywords, like '"BY"'; ignore
                // symbols such as '<Identifier>'.
                if (tokenName.startsWith("\"")
                    && tokenName.endsWith("\""))
                {
                    hintList.add(
                        new SqlMonikerImpl(
                            tokenName.substring(1, tokenName.length() - 1),
                            SqlMonikerType.Keyword));
                }
            }
            return null;
        } catch (EigenbaseException e) {
            Util.swallow(e, null);
            return null;
        }
    }

    /**
     * Gets the fully qualified name for a {@link SqlIdentifier} at a given
     * position of a sql statement.
     *
     * @param sql A syntactically correct sql statement for which to retrieve a
     * fully qualified SQL identifier name
     * @param cursor to indicate the 0-based cursor position in the query that
     * represents a SQL identifier for which its fully qualified name is to be
     * returned.
     *
     * @return a {@link SqlMoniker} that contains the fully qualified name of
     * the specified SQL identifier, returns null if none is found or the SQL
     * statement is invalid.
     */
    public SqlMoniker getQualifiedName(String sql, int cursor)
    {
        SqlNode sqlNode;
        try {
            sqlNode = parseQuery(sql);
            validator.validate(sqlNode);
        } catch (Exception e) {
            return null;
        }
        SqlParserPos pos = new SqlParserPos(1, cursor + 1);
        try {
            return validator.lookupQualifiedName(sqlNode, pos);
        } catch (EigenbaseContextException e) {
            return null;
        } catch (java.lang.AssertionError e) {
            return null;
        }
    }

    /**
     * Attempts to complete and validate a given partially completed sql
     * statement, and returns whether it is valid.
     *
     * @param sql A partial or syntatically incorrect sql statement to validate
     *
     * @return whether SQL statement is valid
     */
    public boolean isValid(String sql)
    {
        SqlSimpleParser simpleParser = new SqlSimpleParser(hintToken);
        String simpleSql = simpleParser.simplifySql(sql);
        SqlNode sqlNode;
        try {
            sqlNode = parseQuery(simpleSql);
        } catch (Exception e) {
            // if the sql can't be parsed we wont' be able to validate it
            return false;
        }
        try {
            validator.validate(sqlNode);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Attempts to parse and validate a SQL statement. Throws the first
     * exception encountered. The error message of this exception is to be
     * displayed on the UI
     *
     * @param sql A user-input sql statement to be validated
     *
     * @return a List of ValidateErrorInfo (null if sql is valid)
     */
    public List<ValidateErrorInfo> validate(String sql)
    {
        SqlNode sqlNode;
        List<ValidateErrorInfo> errorList = new ArrayList<ValidateErrorInfo>();

        sqlNode = collectParserError(sql, errorList);
        if (!errorList.isEmpty()) {
            return errorList;
        }
        try {
            validator.validate(sqlNode);
        } catch (EigenbaseContextException e) {
            ValidateErrorInfo errInfo = new ValidateErrorInfo(e);

            // validator only returns 1 exception now
            errorList.add(errInfo);
            return errorList;
        } catch (Exception e) {
            ValidateErrorInfo errInfo =
                new ValidateErrorInfo(
                    1,
                    1,
                    1,
                    sql.length(),
                    e.getMessage());

            // parser only returns 1 exception now
            errorList.add(errInfo);
            return errorList;
        }
        return null;
    }

    /**
     * Turns a partially completed or syntatically incorrect sql statement into
     * a simplified, valid one that can be passed into getCompletionHints()
     *
     * @param sql A partial or syntatically incorrect sql statement
     * @param cursor to indicate column position in the query at which
     * completion hints need to be retrieved.
     *
     * @return a completed, valid (and possibly simplified SQL statement
     */
    public String simplifySql(String sql, int cursor)
    {
        SqlSimpleParser parser = new SqlSimpleParser(hintToken);
        return parser.simplifySql(sql, cursor);
    }

    /**
     * Return an array of SQL reserved and keywords
     *
     * @return an of SQL reserved and keywords
     */
    public List<String> getReservedAndKeyWords()
    {
        Collection<String> c = SqlAbstractParserImpl.getSql92ReservedWords();
        List<String> l =
            Arrays.asList(
                getParserImpl().getMetadata().getJdbcKeywords().split(","));
        List<String> al = new ArrayList<String>();
        al.addAll(c);
        al.addAll(l);
        return al;
    }

    /**
     * Returns the underlying Parser implementation class.
     *
     * <p>To use a different parser (recognizing a different dialect of SQL),
     * derived class should override.
     *
     * @return a {@link SqlAbstractParserImpl} instance
     */
    protected SqlAbstractParserImpl getParserImpl()
    {
        SqlParser parser = new SqlParser(new StringReader(""));
        return parser.getParserImpl();
    }

    /**
     * Wrapper function to parse a SQL query (SELECT or VALUES, but not INSERT,
     * UPDATE, DELETE, CREATE, DROP etc.), throwing a {@link SqlParseException}
     * if the statement is not syntactically valid.
     *
     * @param sql SQL statement
     *
     * @return parse tree
     *
     * @throws SqlParseException if not syntactically valid
     */
    protected SqlNode parseQuery(String sql)
        throws SqlParseException
    {
        SqlParser parser = new SqlParser(sql);
        return parser.parseStmt();
    }

    /**
     * Attempts to parse a SQL statement and adds to the errorList if any syntax
     * error is found. This implementation uses {@link SqlParser}. Subclass can
     * re-implement this with a different parser implementation
     *
     * @param sql A user-input sql statement to be parsed
     * @param errorList A {@link List} of error to be added to
     *
     * @return {@link SqlNode } that is root of the parse tree, null if the sql
     * is not valid
     */
    protected SqlNode collectParserError(
        String sql,
        List<ValidateErrorInfo> errorList)
    {
        try {
            return parseQuery(sql);
        } catch (SqlParseException e) {
            ValidateErrorInfo errInfo =
                new ValidateErrorInfo(
                    e.getPos(),
                    e.getMessage());

            // parser only returns 1 exception now
            errorList.add(errInfo);
            return null;
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * An inner class that represents error message text and position info of a
     * validator or parser exception
     */
    public class ValidateErrorInfo
    {
        private int startLineNum;
        private int startColumnNum;
        private int endLineNum;
        private int endColumnNum;
        private String errorMsg;

        /**
         * Creates a new ValidateErrorInfo with the position coordinates and an
         * error string.
         *
         * @param startLineNum Start line number
         * @param startColumnNum Start column number
         * @param endLineNum End line number
         * @param endColumnNum End column number
         * @param errorMsg Error message
         */
        public ValidateErrorInfo(
            int startLineNum,
            int startColumnNum,
            int endLineNum,
            int endColumnNum,
            String errorMsg)
        {
            this.startLineNum = startLineNum;
            this.startColumnNum = startColumnNum;
            this.endLineNum = endLineNum;
            this.endColumnNum = endColumnNum;
            this.errorMsg = errorMsg;
        }

        /**
         * Creates a new ValidateErrorInfo with an EigenbaseContextException.
         *
         * @param e Exception
         */
        public ValidateErrorInfo(
            EigenbaseContextException e)
        {
            this.startLineNum = e.getPosLine();
            this.startColumnNum = e.getPosColumn();
            this.endLineNum = e.getEndPosLine();
            this.endColumnNum = e.getEndPosColumn();
            this.errorMsg = e.getCause().getMessage();
        }

        /**
         * Creates a new ValidateErrorInfo with a SqlParserPos and an error
         * string.
         *
         * @param pos Error position
         * @param errorMsg Error message
         */
        public ValidateErrorInfo(
            SqlParserPos pos,
            String errorMsg)
        {
            this.startLineNum = pos.getLineNum();
            this.startColumnNum = pos.getColumnNum();
            this.endLineNum = pos.getEndLineNum();
            this.endColumnNum = pos.getEndColumnNum();
            this.errorMsg = errorMsg;
        }

        /**
         * @return 1-based starting line number
         */
        public int getStartLineNum()
        {
            return startLineNum;
        }

        /**
         * @return 1-based starting column number
         */
        public int getStartColumnNum()
        {
            return startColumnNum;
        }

        /**
         * @return 1-based end line number
         */
        public int getEndLineNum()
        {
            return endLineNum;
        }

        /**
         * @return 1-based end column number
         */
        public int getEndColumnNum()
        {
            return endColumnNum;
        }

        /**
         * @return error message
         */
        public String getMessage()
        {
            return errorMsg;
        }
    }
}

// End SqlAdvisor.java
