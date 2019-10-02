package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;

import java.util.*;

/**
 * Skeleton class for implementing a reaching definition analysis
 * using the Flow.Analysis interface.
 */
public class ReachingDefs implements Flow.Analysis {

    // store the index and variable of all definitions
    private Map<Integer, String> definitions;

    /**
     * Class for the dataflow objects in the ReachingDefs analysis.
     * You are free to change this class or move it to another file.
     */
    public class DefPoints implements Flow.DataflowObject {

        // store the definitions that can reach
        private Set<Integer> defPoints;

        DefPoints() {
            defPoints = new TreeSet<Integer>();
        }

        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * These need to be filled in.
         */
        public void setToTop() { defPoints = new TreeSet<Integer>(); }
        public void setToBottom() { defPoints = definitions.keySet(); }
        public void meetWith (Flow.DataflowObject o) { defPoints.addAll(((DefPoints) o).defPoints); }
        public void copy (Flow.DataflowObject o) { defPoints = new TreeSet<Integer>(((DefPoints) o).defPoints); }

        /**
         * toString() method for the dataflow objects which is used
         * by postprocess() below.  The format of this method must
         * be of the form "[ID0, ID1, ID2, ...]", where each ID is
         * the identifier of a quad defining some register, and the
         * list of IDs must be sorted.  See src/test/Test.rd.out
         * for example output of the analysis.  The output format of
         * your reaching definitions analysis must match this exactly.
         */
        @Override
        public String toString() { return defPoints.toString(); }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DefPoints)) return false;
            return defPoints.equals(((DefPoints) o).defPoints);
        }

        @Override
        public int hashCode() { return defPoints.hashCode(); }

        // mark some definition as used
        private void addDef(int index) {
            defPoints.add(index);
        }

        // find all definition points of one variable then remove them
        private void killDefs(String name) {
            Set<Integer> killedDef = new HashSet<Integer>();
            for (int i: defPoints) {
                if (definitions.get(i).equals(name)) {
                    killedDef.add(i);
                }
            }
            defPoints.removeAll(killedDef);
        }
    }

    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     *
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private DefPoints[] in, out;
    private DefPoints entry, exit;

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg  The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        System.out.println("Method: "+cfg.getMethod().getName().toString());

        // get the amount of space we need to allocate for the in/out arrays.
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int id = qit.next().getID();
            if (id > max)
                max = id;
        }
        max += 1;

        // allocate the in and out arrays.
        in = new DefPoints[max];
        out = new DefPoints[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new DefPoints();
            out[id] = new DefPoints();
        }

        // initialize the entry and exit points.
        entry = new DefPoints();
        exit = new DefPoints();

        /************************************************
         * Your remaining initialization code goes here *
         ************************************************/

        transferfn.val = new DefPoints();

        // find all definitions
        definitions = new TreeMap<Integer, String>();
        for (QuadIterator it = new QuadIterator(cfg); it.hasNext(); ) {
            Quad current = it.next();
            if (current.getDefinedRegisters().size() > 0) {
                definitions.put(current.getID(), current.getDefinedRegisters().get(0).getRegister().toString());
            }
        }

        // entry and exit does not need to be specially initialized
        // the former is naturally empty, while the latter needs to be calculated

    }

    /**
     * This method is called after the fixpoint is reached.
     * It must print out the dataflow objects associated with
     * the entry, exit, and all interior points of the CFG.
     * Unless you modify in, out, entry, or exit you shouldn't
     * need to change this method.
     *
     * @param cfg  Unused.
     */
    public void postprocess (ControlFlowGraph cfg) {
        System.out.println("entry: " + entry.toString());
        for (int i=0; i<in.length; i++) {
            if (in[i] != null) {
                System.out.println(i + " in:  " + in[i].toString());
                System.out.println(i + " out: " + out[i].toString());
            }
        }
        System.out.println("exit: " + exit.toString());
    }

    /**
     * Other methods from the Flow.Analysis interface.
     * See Flow.java for the meaning of these methods.
     * These need to be filled in.
     */
    public boolean isForward () { return true; }
    public Flow.DataflowObject getEntry() { return copy(entry); }
    public Flow.DataflowObject getExit() { return copy(exit); }
    public void setEntry(Flow.DataflowObject value) { entry.copy(value); }
    public void setExit(Flow.DataflowObject value) { exit.copy(value); }
    public Flow.DataflowObject getIn(Quad q) { return copy(in[q.getID()]); }
    public Flow.DataflowObject getOut(Quad q) { return copy(out[q.getID()]); }
    public void setIn(Quad q, Flow.DataflowObject value) { in[q.getID()].copy(value); }
    public void setOut(Quad q, Flow.DataflowObject value) { out[q.getID()].copy(value); }
    public Flow.DataflowObject newTempVar() { return new DefPoints(); }

    public void processQuad(Quad q) {
        transferfn.val = (DefPoints) getIn(q);
        transferfn.visitQuad(q);
        setOut(q, transferfn.val);
    }

    private TransferFunction transferfn = new TransferFunction();

    private Flow.DataflowObject copy(Flow.DataflowObject target) {
        Flow.DataflowObject result = newTempVar();
        result.copy(target);
        return result;
    }

    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        DefPoints val;
        @Override
        public void visitQuad(Quad q) {
            if (q.getDefinedRegisters().size() > 0) {
                // kill all definitions
                for (Operand.RegisterOperand r: q.getDefinedRegisters()) {
                    // in fact we can safely assume one quad will define at most one variable
                    // but for elegance an for-each is used here
                    val.killDefs(r.getRegister().toString());
                }
                val.addDef(q.getID());
            }
        }
    }
}
