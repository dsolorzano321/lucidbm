/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2010 The Eigenbase Project
// Copyright (C) 2010 SQLstream, Inc.
// Copyright (C) 2010 Dynamo BI Corporation
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
package org.eigenbase.test;

import java.math.BigDecimal;
import java.util.*;

import junit.framework.TestCase;

import org.eigenbase.oj.OJTypeFactoryImpl;
import org.eigenbase.oj.util.JavaRexBuilder;
import org.eigenbase.reltype.*;
import org.eigenbase.rex.*;
import org.eigenbase.sql.fun.SqlStdOperatorTable;
import org.eigenbase.sql.type.*;
import org.eigenbase.util.*;


/**
 * Unit tests for {@link RexProgram} and
 * {@link org.eigenbase.rex.RexProgramBuilder}.
 *
 * @author jhyde
 * @version $Id$
 */
public class RexProgramTest
    extends TestCase
{
    //~ Instance fields --------------------------------------------------------
    private OJTypeFactoryImpl typeFactory;
    private RexBuilder rexBuilder;

    //~ Methods ----------------------------------------------------------------

    /**
     * Creates a RexProgramTest.
     */
    public RexProgramTest()
    {
        super();
    }

    /**
     * Creates a RexProgramTest with given name.
     */
    public RexProgramTest(String name)
    {
        super(name);
    }

    protected void setUp()
        throws Exception
    {
        typeFactory = new OJTypeFactoryImpl();
        rexBuilder = new JavaRexBuilder(typeFactory);
    }

    /**
     * Tests construction of a RexProgram.
     */
    public void testBuildProgram()
    {
        final RexProgramBuilder builder = createProg(0);
        final RexProgram program = builder.getProgram(false);
        final String programString = program.toString();
        TestUtil.assertEqualsVerbose(
            "(expr#0..1=[{inputs}], expr#2=[+($0, 1)], expr#3=[77], "
            + "expr#4=[+($0, $1)], expr#5=[+($0, $0)], expr#6=[+($t4, $t2)], "
            + "a=[$t6], b=[$t5])",
            programString);

        // Normalize the program using the RexProgramBuilder.normalize API.
        // Note that unused expression '77' is eliminated, input refs (e.g. $0)
        // become local refs (e.g. $t0), and constants are assigned to locals.
        final RexProgram normalizedProgram =
            RexProgramBuilder.normalize(
                rexBuilder,
                program);
        final String normalizedProgramString = normalizedProgram.toString();
        TestUtil.assertEqualsVerbose(
            "(expr#0..1=[{inputs}], expr#2=[+($t0, $t1)], expr#3=[1], "
            + "expr#4=[+($t0, $t3)], expr#5=[+($t2, $t4)], "
            + "expr#6=[+($t0, $t0)], a=[$t5], b=[$t6])",
            normalizedProgramString);
    }

    /**
     * Tests construction and normalization of a RexProgram.
     */
    public void testNormalize()
    {
        final RexProgramBuilder builder = createProg(0);
        final String program = builder.getProgram(true).toString();
        TestUtil.assertEqualsVerbose(
            "(expr#0..1=[{inputs}], expr#2=[+($t0, $t1)], expr#3=[1], "
            + "expr#4=[+($t0, $t3)], expr#5=[+($t2, $t4)], "
            + "expr#6=[+($t0, $t0)], a=[$t5], b=[$t6])",
            program);
    }

    /**
     * Tests construction and normalization of a RexProgram.
     */
    public void testElimDups()
    {
        final RexProgramBuilder builder = createProg(1);
        final String unnormalizedProgram = builder.getProgram(false).toString();
        TestUtil.assertEqualsVerbose(
            "(expr#0..1=[{inputs}], expr#2=[+($0, 1)], expr#3=[77], "
            + "expr#4=[+($0, $1)], expr#5=[+($0, 1)], expr#6=[+($0, $t5)], "
            + "expr#7=[+($t4, $t2)], a=[$t7], b=[$t6])",
            unnormalizedProgram);

        // normalize eliminates dups (specifically "+($0, $1)")
        final RexProgramBuilder builder2 = createProg(1);
        final String program2 = builder2.getProgram(true).toString();
        TestUtil.assertEqualsVerbose(
            "(expr#0..1=[{inputs}], expr#2=[+($t0, $t1)], expr#3=[1], "
            + "expr#4=[+($t0, $t3)], expr#5=[+($t2, $t4)], "
            + "expr#6=[+($t0, $t4)], a=[$t5], b=[$t6])",
            program2);
    }

    /**
     * Tests that AND(x, x) is translated to x.
     */
    public void testDuplicateAnd()
    {
        final RexProgramBuilder builder = createProg(2);
        final String program = builder.getProgram(true).toString();
        TestUtil.assertEqualsVerbose(
            "(expr#0..1=[{inputs}], expr#2=[+($t0, $t1)], expr#3=[1], "
            + "expr#4=[+($t0, $t3)], expr#5=[+($t2, $t4)], "
            + "expr#6=[+($t0, $t0)], expr#7=[>($t2, $t0)], "
            + "a=[$t5], b=[$t6], $condition=[$t7])",
            program);
    }

    /**
     * Creates a program, depending on variant:
     * <ol>
     * <li><code>select (x + y) + (x + 1) as a, (x + x) as b from t(x, y)</code>
     * <li><code>select (x + y) + (x + 1) as a, (x + (x + 1)) as b
     *     from t(x, y)</code>
     * <li><code>select (x + y) + (x + 1) as a, (x + x) as b from t(x, y)
     *     where ((x + y) > 1) and ((x + y) > 1)</code>
     * <li><code>select 1 as a, cast(null as integer) as b, x + 1 as c
     *     from t(x)</code>
     * </ul>
     */
    private RexProgramBuilder createProg(int variant)
    {
        assert variant == 0 || variant == 1 || variant == 2 || variant == 3;
        List<RelDataType> types =
            Arrays.asList(
                typeFactory.createSqlType(SqlTypeName.INTEGER),
                typeFactory.createSqlType(SqlTypeName.INTEGER));
        List<String> names = Arrays.asList("x", "y");
        RelDataType inputRowType = typeFactory.createStructType(types, names);
        final RexProgramBuilder builder =
            new RexProgramBuilder(inputRowType, rexBuilder);

        // $t0 = x
        // $t1 = y
        // $t2 = $t0 + 1 (i.e. x + 1)
        final RexNode i0 = rexBuilder.makeInputRef(
            types.get(0), 0);
        final RexLiteral c1 = rexBuilder.makeExactLiteral(
            BigDecimal.ONE);
        RexLocalRef t2 =
            builder.addExpr(
                rexBuilder.makeCall(
                    SqlStdOperatorTable.plusOperator,
                    i0,
                    c1));

        if (variant == 3) {
            final RexNode t3 =
                rexBuilder.makeNullLiteral(SqlTypeName.INTEGER);
            builder.addProject(c1, "a");
            builder.addProject(t3, "b");
            builder.addProject(t2, "c");
            return builder;
        }

        // $t3 = 77 (not used)
        final RexLiteral c77 =
            rexBuilder.makeExactLiteral(
                BigDecimal.valueOf(77));
        RexLocalRef t3 =
            builder.addExpr(
                c77);
        Util.discard(t3);
        // $t4 = $t0 + $t1 (i.e. x + y)
        final RexNode i1 = rexBuilder.makeInputRef(
            types.get(1), 1);
        RexLocalRef t4 =
            builder.addExpr(
                rexBuilder.makeCall(
                    SqlStdOperatorTable.plusOperator,
                    i0,
                    i1));
        RexLocalRef t5;
        switch (variant) {
        case 0:
        case 2:
            // $t5 = $t0 + $t0 (i.e. x + x)
            t5 = builder.addExpr(
                rexBuilder.makeCall(
                    SqlStdOperatorTable.plusOperator,
                    i0,
                    i0));
            break;
        case 1:
            // $tx = $t0 + 1
            RexLocalRef tx =
                builder.addExpr(
                    rexBuilder.makeCall(
                        SqlStdOperatorTable.plusOperator,
                        i0,
                        c1));
            // $t5 = $t0 + $tx (i.e. x + (x + 1))
            t5 =
                builder.addExpr(
                    rexBuilder.makeCall(
                        SqlStdOperatorTable.plusOperator,
                        i0,
                        tx));
            break;
        default:
            throw Util.newInternal("unexpected variant " + variant);
        }
        // $t6 = $t4 + $t2 (i.e. (x + y) + (x + 1))
        RexLocalRef t6 =
            builder.addExpr(
                rexBuilder.makeCall(
                    SqlStdOperatorTable.plusOperator,
                    t4,
                    t2));
        builder.addProject(t6.getIndex(), "a");
        builder.addProject(t5.getIndex(), "b");

        if (variant == 2) {
            // $t7 = $t4 > $i0 (i.e. (x + y) > 0)
            RexLocalRef t7 =
                builder.addExpr(
                    rexBuilder.makeCall(
                        SqlStdOperatorTable.greaterThanOperator,
                        t4,
                        i0));
            // $t8 = $t7 AND $t7
            RexLocalRef t8 =
                builder.addExpr(
                    rexBuilder.makeCall(
                        SqlStdOperatorTable.andOperator,
                        t7,
                        t7));
            builder.addCondition(t8);
            builder.addCondition(t7);
        }
        return builder;
    }

    /**
     * Unit test for {@link org.eigenbase.rex.RexBuilder#makeZeroLiteral}.
     */
    public void testRexBuilderMakeZeroLiteral()
    {
        for (BasicSqlType type : SqlLimitsTest.getTypes()) {
            checkMakeZeroLiteral(type, Boolean.TRUE);
            checkMakeZeroLiteral(type, Boolean.FALSE);
            checkMakeZeroLiteral(type, null);
        }
    }

    private void checkMakeZeroLiteral(BasicSqlType type, Boolean allowCast)
    {
        final RexNode literal =
            allowCast == null
            ? rexBuilder.makeZeroLiteral(type)
            : rexBuilder.makeZeroLiteral(type, allowCast);
        assertNotNull(literal);
        assertNotNull(literal.toString());
        if (allowCast == Boolean.TRUE) {
            assertEquals(
                type.getSqlTypeName(), literal.getType().getSqlTypeName());
            if (type.getSqlTypeName().allowsPrec()) {
                assertEquals(
                    type.getPrecision(), literal.getType().getPrecision());
            }
            if (type.getSqlTypeName().allowsScale()) {
                assertEquals(type.getScale(), literal.getType().getScale());
            }
        } else {
            assertTrue(literal instanceof RexLiteral);
        }
        assertFalse(literal.getType().isNullable());
    }

    /**
     * Unit test for
     * {@link org.eigenbase.rex.RexProgram#getSourceExpression(int)}.
     */
    public void testRexProgramGetSourceExpression()
    {
        final RexProgram program0 = createProg(0).getProgram();
        assertEquals(
            "+(+($0, $1), +($0, 1))",
            program0.getSourceExpression(0).toString());
        assertEquals(
            "+($0, $0)",
            program0.getSourceExpression(1).toString());
        final RexProgram program3 = createProg(3).getProgram();
        assertEquals(
            "1",
            program3.getSourceExpression(0).toString());
        assertEquals(
            "CAST(null):INTEGER",
            program3.getSourceExpression(1).toString());
        assertEquals(
            "+($0, 1)",
            program3.getSourceExpression(2).toString());
    }
}

// End RexProgramTest.java
