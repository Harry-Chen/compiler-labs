package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Skeleton class for implementing the Flow.Solver interface.
 */
public class MySolver implements Flow.Solver {

    protected Flow.Analysis analysis;

    /**
     * Sets the analysis.  When visitCFG is called, it will
     * perform this analysis on a given CFG.
     *
     * @param analyzer The analysis to run
     */
    public void registerAnalysis(Flow.Analysis analyzer) {
        this.analysis = analyzer;
    }

    /**
     * Runs the solver over a given control flow graph.  Prior
     * to calling this, an analysis must be registered using
     * registerAnalysis
     *
     * @param cfg The control flow graph to analyze.
     */
    public void visitCFG(ControlFlowGraph cfg) {

        // this needs to come first.
        analysis.preprocess(cfg);

        /***********************
         * Your code goes here *
         ***********************/

        // iteration direction
        boolean forward = analysis.isForward();

        // initialize the top element
        Flow.DataflowObject top = analysis.newTempVar();
        top.setToTop();

        // find the (single) entry and (multiple) exits
        Quad entryQuad = null;
        List<Quad> exitQuads = new ArrayList<Quad>();

        for (QuadIterator it = new QuadIterator(cfg); it.hasNext(); ) {
            Quad current = it.next();
            // initialize all dataflow objects to Top element
            if (forward) {
                analysis.setOut(current, top);
            } else {
                analysis.setIn(current, top);
            }
            // exit nodes
            if (it.successors1().contains(null)) {
                exitQuads.add(current);
            }
            // entry node
            if (it.predecessors1().contains(null)) {
                if (entryQuad != null) {
                    throw new IllegalStateException("One CFG must have only one entry quad");
                }
                entryQuad = current;
            }
        }

        // main iteration
        boolean change = true;
        while (change) {
            change = false;
            for (QuadIterator it = new QuadIterator(cfg, forward); forward ? it.hasNext() : it.hasPrevious(); ) {
                if (forward) {
                    Quad current = it.next();
                    // calculate new IN
                    Flow.DataflowObject in = analysis.newTempVar();
                    for (Iterator<Quad> iter = it.predecessors(); iter.hasNext(); ) {
                        Quad pred = iter.next();
                        in.meetWith(pred == null ? analysis.getEntry() : analysis.getOut(pred));
                    }
                    analysis.setIn(current, in);
                    // calculate new OUT
                    Flow.DataflowObject oldOut = analysis.getOut(current);
                    analysis.processQuad(current);
                    // determine end condition
                    if (!analysis.getOut(current).equals(oldOut)) {
                        change = true;
                    }
                } else {
                    Quad current = it.previous();
                    // calculate new OUT
                    Flow.DataflowObject out = analysis.newTempVar();
                    for (Iterator<Quad> iter = it.successors(); iter.hasNext(); ) {
                        Quad pred = iter.next();
                        out.meetWith(pred == null ? analysis.getExit() : analysis.getIn(pred));
                    }
                    analysis.setOut(current, out);
                    // calculate new IN
                    Flow.DataflowObject oldIn = analysis.getIn(current);
                    analysis.processQuad(current);
                    // determine end condition
                    if (!analysis.getIn(current).equals(oldIn)) {
                        change = true;
                    }
                }
            }
        }

        if (forward) {
            // calculate the final OUT of CFG
            Flow.DataflowObject out = analysis.newTempVar();
            out.setToTop();
            for (Quad quad: exitQuads) {
                out.meetWith(analysis.getOut(quad));
            }
            analysis.setExit(out);
        } else {
            analysis.setEntry(analysis.getIn(entryQuad));
        }

        // this needs to come last.
        analysis.postprocess(cfg);
    }
}
