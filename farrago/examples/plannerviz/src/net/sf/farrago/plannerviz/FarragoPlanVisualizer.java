/*
// $Id$
// Farrago is an extensible data management system.
// Copyright (C) 2005 The Eigenbase Project
// Copyright (C) 2005 SQLstream, Inc.
// Copyright (C) 2005 Dynamo BI Corporation
// Portions Copyright (C) 2005 John V. Sichi
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
package net.sf.farrago.plannerviz;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

import org.jgrapht.*;
import org.jgrapht.ext.*;
import org.jgrapht.graph.*;
import org.jgrapht.graph.DefaultEdge;

import org.jgraph.*;
import org.jgraph.graph.*;
import org.jgraph.layout.*;
import org.jgraph.algebra.*;

import org.eigenbase.relopt.*;
import org.eigenbase.rel.*;
import org.eigenbase.rel.convert.*;

import java.util.*;
import java.util.logging.*;

// TODO jvs 18-Feb-2005:  avoid these two dependencies
import org.eigenbase.relopt.volcano.*;
import org.eigenbase.relopt.hep.*;

import net.sf.farrago.trace.*;

import java.util.List;

/**
 * FarragoPlanVizualizer uses JGraph to visualize the machinations
 * of the planner.
 *
 * @author John V. Sichi
 * @version $Id$
 */
public class FarragoPlanVisualizer
    extends JApplet
    implements RelOptListener, WindowListener
{
    private static final Logger tracer =
        FarragoTrace.getPlannerVizTracer();

    private static final int STATE_CRAWLING = 0;

    private static final int STATE_STEPPING = 1;

    private static final int STATE_WALKING = 2;

    private static final int STATE_RUNNING = 3;

    private static final int STATE_FLYING = 4;

    private static final int STATE_CLOSED = 5;

    private static final int DETAIL_LOGICAL = 1;

    private static final int DETAIL_PHYSICAL = 2;

    private static final int DETAIL_PHYSIOLOGICAL = 3;

    private int state;

    private int detail;

    private int currentGenerationNumber;

    private Object stepVar;

    private JFrame frame;

    private JScrollPane scrollPane;

    private JGraph graph;

    private GraphLayoutCache graphView;

    private JGraphModelAdapter graphAdapter;

    private ListenableDirectedGraph<VisualVertex, VisualEdge> graphModel;

    private JMenuItem status;

    private UnionFind physicalEquivMap;

    private UnionFind logicalEquivMap;

    private double scale;

    private Set<RelNode> rels;

    private Map<Object, VisualVertex> objToVertexMap;

    private AttributeMap normalVertexAttributes;

    private AttributeMap newVertexAttributes;

    private AttributeMap oldVertexAttributes;

    private AttributeMap finalVertexAttributes;

    private List<VisualVertex> highlightList;

    private String ruleName;

    public FarragoPlanVisualizer()
    {
        status = new JMenuItem("Building abstract plan");

        JMenuBar menuBar = new JMenuBar();
        addStateButton(menuBar, "CRAWL", STATE_CRAWLING);
        addStateButton(menuBar, "STEP", STATE_STEPPING);
        addStateButton(menuBar, "WALK", STATE_WALKING);
        addStateButton(menuBar, "RUN", STATE_RUNNING);
        addStateButton(menuBar, "FLY", STATE_FLYING);
        addZoomButton(menuBar, "ZOOMIN", 1.5);
        addZoomButton(menuBar, "ZOOMOUT", 1.0 / 1.5);

        frame = new JFrame();
        frame.setJMenuBar(menuBar);
        frame.addWindowListener(this);
        frame.getContentPane().add(this);
        frame.setTitle("Plannerviz");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        init();
        frame.setVisible(true);

        stepVar = new Object();
        state = STATE_CRAWLING;
        scale = 1;
        rels = new HashSet<RelNode>();
        objToVertexMap = new HashMap<Object, VisualVertex>();
        physicalEquivMap = new UnionFind();
        logicalEquivMap = new UnionFind();
        highlightList = new ArrayList<VisualVertex>();

        if (tracer.getLevel() == Level.FINEST) {
            detail = DETAIL_PHYSIOLOGICAL;
        } else if (tracer.getLevel() == Level.FINER) {
            detail = DETAIL_PHYSICAL;
        } else {
            detail = DETAIL_LOGICAL;
        }
    }

    private void addStateButton(
        JMenuBar menuBar, String name, final int newState)
    {
        JMenuItem button = new JMenuItem(name);
        button.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    changeState(newState);
                }
            });
        menuBar.add(button);
    }

    private void addZoomButton(
        JMenuBar menuBar, String name, final double factor)
    {
        JMenuItem button = new JMenuItem(name);
        button.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    scale *= factor;
                    graph.setScale(scale);
                }
            });
        menuBar.add(button);
    }

    private void changeState(int newState)
    {
        synchronized (stepVar) {
            state = newState;
            stepVar.notifyAll();
        }
    }

    // implement WindowListener
    public void windowClosing(WindowEvent e)
    {
    }

    // implement WindowListener
    public void windowClosed(WindowEvent e)
    {
        changeState(STATE_CLOSED);
    }

    // implement WindowListener
    public void windowOpened(WindowEvent e)
    {
    }

    // implement WindowListener
    public void windowIconified(WindowEvent e)
    {
    }

    // implement WindowListener
    public void windowDeiconified(WindowEvent e)
    {
    }

    // implement WindowListener
    public void windowActivated(WindowEvent e)
    {
    }

    // implement WindowListener
    public void windowDeactivated(WindowEvent e)
    {
    }

    // implement RelOptListener
    public void relChosen(RelChosenEvent event)
    {
        if (state > STATE_RUNNING) {
            return;
        }

        if (event.getRel() == null) {
            setStatus("Final plan complete");
            return;
        }

        if (!includeRel(event.getRel())) {
            return;
        }

        setStatus("Adding node to final plan");
        VisualVertex vertex = makeVertex(event.getRel());
        paintVertex(vertex, finalVertexAttributes);
        waitForInput();
    }

    // implement RelOptListener
    public void relDiscarded(RelDiscardedEvent event)
    {
        if (state == STATE_CRAWLING) {
            setStatus("Discarding " + event.getRel());
            updateGraph();
            highlightVertex(makeVertex(event.getRel()), oldVertexAttributes);
            waitForInput();
        }
        rels.remove(event.getRel());
    }

    // implement RelOptListener
    public void relEquivalenceFound(RelEquivalenceEvent event)
    {
        if (state > STATE_RUNNING) {
            return;
        }

        if (detail == DETAIL_LOGICAL) {
            if (event.isPhysical()) {
                return;
            }
        } else if (detail == DETAIL_PHYSICAL) {
            if (!event.isPhysical()) {
                return;
            }
        }

        if (!includeRel(event.getRel())) {
            return;
        }

        UnionFind equivMap;
        String type;
        if (event.isPhysical()) {
            type = "Physical";
            equivMap = physicalEquivMap;
        } else {
            type = "Logical";
            equivMap = logicalEquivMap;
        }

        // REVIEW jvs 19-Feb-2005: we intentionally create the UnionFind sets
        // in this order for Volcano to make sure that the representation
        // chosen is the equivalence class, not the original rel.
        Object equivSet = equivMap.find(event.getEquivalenceClass());
        Object relSet = equivMap.find(event.getRel());
        equivMap.union(equivSet, relSet);

        if (detail == DETAIL_LOGICAL) {
            // In this case, we don't visualize subsets, but we do
            // record their equivalence so that inputs get connected
            // correctly.
            if (event.getRel() instanceof RelSubset) {
                return;
            }
        }

        boolean newRel = rels.add(event.getRel());

        if (event.getEquivalenceClass() instanceof HepRelVertex) {
            // For Hep, we don't care about equivalences much.
            newRel = true;
        }

        if ((state == STATE_CRAWLING)
            || (!newRel && (state == STATE_STEPPING)))
        {
            String newStatus;
            if (newRel) {
                newStatus = "New expression added to "
                    + type.toLowerCase() + " equivalence class";
            } else {
                newStatus = type + " equivalence found for "
                    + event.getEquivalenceClass();
            }
            if (ruleName != null) {
                newStatus += " by rule " + ruleName;
            }
            setStatus(newStatus);
            updateGraph();
            highlightVertex(makeVertex(event.getRel()), newVertexAttributes);
            waitForInput();
        }
    }

    // implement RelOptListener
    public void ruleAttempted(RuleAttemptedEvent event)
    {
        if (event.isBefore()) {
            ruleName = event.getRuleCall().getRule().toString();
        } else {
            ruleName = null;
        }
    }

    // implement RelOptListener
    public void ruleProductionSucceeded(RuleProductionEvent event)
    {
        if (state > STATE_RUNNING) {
            return;
        }

        if (state == STATE_CRAWLING) {
            if (!event.isBefore()) {
                // already previewed; skip post-display
                return;
            }
        }

        if (!includeRel(event.getRel())) {
            return;
        }

        String verb;
        if (!event.isBefore()) {
            if (!rels.contains(event.getRel())) {
                // The rel didn't get registered, so it must have
                // matched an existing one; don't bother showing the
                // rule production.
                return;
            }
            verb = "produced";
        } else {
            verb = "producing";
        }

        if (state > STATE_WALKING) {
            updateGraph();
            return;
        }
        setStatus(
            "Rule " + event.getRuleCall().getRule() + " " + verb
            + " new expression " + event.getRel());
        updateGraph();
        highlightRuleVertices(event);
        waitForInput();
    }

    private void setStatus(String text)
    {
        status.setText(text);
        for (VisualVertex vertex : highlightList) {
            paintVertex(vertex, normalVertexAttributes);
        }
        highlightList.clear();
    }

    private void highlightRuleVertices(RuleProductionEvent event)
    {
        if (!event.isBefore()) {
            highlightVertex(makeVertex(event.getRel()), newVertexAttributes);

            if (event.getRuleCall() instanceof HepRuleCall) {
                return;
            }
        }

        RelNode [] rels = event.getRuleCall().rels;
        for (RelNode rel : rels) {
            if (includeRel(rel)) {
                highlightVertex(makeVertex(rel), oldVertexAttributes);
            }
        }
    }

    private boolean includeRel(RelNode rel)
    {
        if (rel == null) {
            return false;
        }
        if (detail == DETAIL_LOGICAL) {
            if (isConverterRel(rel)) {
                return false;
            }
        }
        if (rel instanceof AbstractConverter) {
            return false;
        }
        return true;
    }

    private boolean isConverterRel(RelNode rel)
    {
        boolean volcano = false;
        if (volcano) {
            return (rel instanceof ConverterRel);
        } else {
            return false;
        }
    }

    private void highlightVertex(VisualVertex vertex, AttributeMap attributes)
    {
        paintVertex(vertex, attributes);
        highlightList.add(vertex);
    }

    private void paintVertex(VisualVertex vertex, AttributeMap map)
    {
        DefaultGraphCell cell = graphAdapter.getVertexCell(vertex);
        if (cell == null) {
            return;
        }
        CellView cellView = graphView.getMapping(cell, true);
        cellView.changeAttributes(map);
        graphView.cellViewsChanged(new CellView[]{cellView});
        if (!graphView.isVisible(cell)) {
            graph.scrollCellToVisible(cell);
        }
    }

    private void updateGraph()
    {
        if (state != STATE_RUNNING) {
            graph.setVisible(false);
        }

        // update the graph model to reflect current state
        ++currentGenerationNumber;

        for (RelNode rel : rels) {
            VisualVertex v1 = makeVertex(rel);
            if (rel instanceof RelSubset) {
                Object set = logicalEquivMap.find(rel);
                if (set != rel) {
                    VisualVertex v2 = makeVertex(set);
                    makeEdge(v2, v1, "");
                }
                continue;
            }
            UnionFind equivMap;
            if (detail == DETAIL_LOGICAL) {
                equivMap = logicalEquivMap;
            } else {
                equivMap = physicalEquivMap;
            }
            Object set = equivMap.find(rel);
            if ((set != rel) && !(set instanceof HepRelVertex)) {
                VisualVertex v2 = makeVertex(set);
                makeEdge(v2, v1, "");
            }
            // converters can lead to cycles around subsets, so
            // omit the edges for their inputs
            if (!isConverterRel(rel)) {
                RelNode[] inputs = rel.getInputs();
                for (int i = 0; i < inputs.length; ++i) {
                    Object inputSet = equivMap.find(inputs[i]);
                    if (inputSet instanceof HepRelVertex) {
                        inputSet = ((HepRelVertex) inputSet).getCurrentRel();
                    }
                    VisualVertex v2 = makeVertex(inputSet);
                    if (inputSet != set) {
                        String label;
                        if (inputs.length > 1) {
                            label = Integer.toString(i);
                        } else {
                            label = "";
                        }
                        makeEdge(v1, v2, label);
                    }
                }
            }
        }

        List<Object> recyclingList = new ArrayList<Object>();

        // collect obsolete edges
        for (VisualEdge edge : graphModel.edgeSet()) {
            if (edge.generationNumber != currentGenerationNumber) {
                recyclingList.add(edge);
            }
        }

        // dispose of obsolete edges
        for (Object obj : recyclingList) {
            graphModel.removeEdge((VisualEdge) obj);
        }

        recyclingList.clear();

        // collect roots and obsolete vertices
        List<DefaultGraphCell> roots = new ArrayList<DefaultGraphCell>();
        for (VisualVertex vertex : graphModel.vertexSet()) {
            if (vertex.generationNumber != currentGenerationNumber) {
                recyclingList.add(vertex);
                continue;
            }
            if (graphModel.inDegreeOf(vertex) == 0) {
                roots.add(graphAdapter.getVertexCell(vertex));
            }
        }

        // dispose of obsolete vertices
        for (Object obj : recyclingList) {
            VisualVertex visualVertex = (VisualVertex) obj;
            graphModel.removeVertex(visualVertex);
            objToVertexMap.remove(visualVertex.obj);
        }

        // compute graph layout
        assert (!roots.isEmpty());
        JGraphLayoutAlgorithm layout = new SugiyamaLayoutAlgorithm();
        JGraphLayoutAlgorithm.applyLayout(graph, layout, roots.toArray(), null);

        // SugiyamaLayoutAlgorithm doesn't normalize its output, leading to a
        // cumulative bias for each round.  We compensate for this by computing
        // the bounding rectangle and applying a corresponding negative
        // translation.
        Rectangle2D bounds = graph.getCellBounds(graph.getRoots());
        GraphLayoutCache.translateViews(
            graphView.getCellViews(),
            -bounds.getX(),
            -bounds.getY());
        graphView.update(graphView.getCellViews());
        graph.clearSelection();

        if (state != STATE_RUNNING) {
            graph.setVisible(true);
        }
    }

    private void waitForInput()
    {
        synchronized (stepVar) {
            try {
                switch (state) {
                case STATE_CRAWLING:
                case STATE_STEPPING:
                    stepVar.wait();
                    break;
                case STATE_WALKING:
                    stepVar.wait(1000);
                    break;
                default:
                    break;
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    private VisualVertex makeVertex(Object obj)
    {
        VisualVertex vertex = objToVertexMap.get(obj);
        if (vertex == null) {
            vertex = new VisualVertex(obj);
            objToVertexMap.put(obj, vertex);
            graphModel.addVertex(vertex);
        }
        vertex.generationNumber = currentGenerationNumber;
        return vertex;
    }

    private VisualEdge makeEdge(VisualVertex v1, VisualVertex v2, String label)
    {
        VisualEdge edge = graphModel.getEdge(v1, v2);
        if (edge == null) {
            edge = new VisualEdge(label);
            graphModel.addEdge(v1, v2, edge);
        } else {
            // e.g. self-join
            if (!edge.toString().contains(label)) {
                edge.setLabel(edge.toString() + ", " + label);
            }
        }
        edge.generationNumber = currentGenerationNumber;
        return edge;
    }

    public void init()
    {
        graphModel =
            new ListenableDirectedGraph<VisualVertex, VisualEdge>(
                new DefaultDirectedGraph<VisualVertex, VisualEdge>(
                    new VisualEdgeFactory()));
        AttributeMap defaultVertexAttributes =
            JGraphModelAdapter.createDefaultVertexAttributes();
        GraphConstants.setBounds(
            defaultVertexAttributes,
            new Rectangle2D.Double(50, 50, 200, 30));
        normalVertexAttributes = new AttributeMap();
        GraphConstants.setBackground(
            defaultVertexAttributes,
            Color.GRAY);
        GraphConstants.setBackground(
            normalVertexAttributes,
            Color.GRAY);
        oldVertexAttributes = new AttributeMap();
        GraphConstants.setBackground(
            oldVertexAttributes,
            Color.GREEN);
        finalVertexAttributes = new AttributeMap();
        GraphConstants.setBackground(
            finalVertexAttributes,
            Color.BLUE);
        newVertexAttributes = new AttributeMap();
        GraphConstants.setBackground(
            newVertexAttributes,
            Color.RED);
        graphAdapter = new JGraphModelAdapter<VisualVertex, VisualEdge>(
            graphModel,
            defaultVertexAttributes,
            JGraphModelAdapter.createDefaultEdgeAttributes(graphModel));
        graph = new JGraph(graphAdapter);
        scrollPane = new JScrollPane(graph);
        scrollPane.setPreferredSize(new Dimension(500, 500));
        scrollPane.setColumnHeaderView(status);
        graph.setAutoscrolls(true);
        graphView = graph.getGraphLayoutCache();
        getContentPane().add(scrollPane);
        frame.pack();
    }

    private static class VisualVertex
    {
        int generationNumber;
        final RelNode rel;
        final String name;
        final Object obj;

        VisualVertex(Object obj)
        {
            this.obj = obj;
            if (obj instanceof RelNode) {
                rel = (RelNode) obj;
                name = rel.getId() + ":" + rel;
            } else {
                rel = null;
                name = obj.toString();
            }
        }

        public String toString()
        {
            return name;
        }
    }

    private static class VisualEdge extends DefaultEdge
    {
        private String label;
        int generationNumber;

        VisualEdge(
            String label)
        {
            this.label = label;
        }

        public void setLabel(String label)
        {
            this.label = label;
        }

        public String toString()
        {
            return label;
        }
    }

    private static class VisualEdgeFactory
        implements EdgeFactory<VisualVertex, VisualEdge>
    {
        public VisualEdge createEdge(
            VisualVertex sourceVertex,
            VisualVertex targetVertex)
        {
            return new VisualEdge("");
        }
    }
}

// End FarragoPlanVisualizer.java
