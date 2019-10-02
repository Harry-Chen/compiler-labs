package submit;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;
import joeq.Main.Helper;

import java.util.Set;
import java.util.TreeSet;

/**
 * Skeleton class for implementing a faint variable analysis
 * using the Flow.Analysis interface.
 */
public class Faintness implements Flow.Analysis {

    /**
     * Class for the dataflow objects in the Faintness analysis.
     * You are free to change this class or move it to another file.
     */
    public static class VarSet implements Flow.DataflowObject {
        private Set<String> set;
        public static Set<String> universalSet;
        public VarSet() { set = new TreeSet<String>(); }

        public void setToTop() { set = new TreeSet<String>(universalSet); }
        public void setToBottom() { set = new TreeSet<String>(); }
        public void meetWith(Flow.DataflowObject o) { set.retainAll(((VarSet)o).set); }
        public void copy(Flow.DataflowObject o) { set = new TreeSet<String>(((VarSet) o).set); }

        @Override
        public boolean equals(Object o) {
            if (o instanceof VarSet) {
                return set.equals(((VarSet) o).set);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return set.hashCode();
        }
        @Override
        public String toString()
        {
            return set.toString();
        }

        // remove one var from faint set if it is used to generate an not fainted var
        // or just conditionally (by passing null to dest)
        private void wakeVar(String used, String dest) {
            if (dest == null || !set.contains(dest)) {
                set.remove(used);
            }
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
    private VarSet[] in, out;
    private VarSet entry, exit;

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
        in = new VarSet[max];
        out = new VarSet[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new VarSet();
            out[id] = new VarSet();
        }

        // initialize the entry and exit points.
        entry = new VarSet();
        exit = new VarSet();

        /************************************************
         * Your remaining initialization code goes here *
         ************************************************/

        Set<String> s = new TreeSet<String>();
        VarSet.universalSet = s;

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            s.add("R"+i);
        }

        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            for (Operand.RegisterOperand def : q.getDefinedRegisters()) {
                s.add(def.getRegister().toString());
            }
            for (Operand.RegisterOperand use : q.getUsedRegisters()) {
                s.add(use.getRegister().toString());
            }
        }

        // all variables are fainted at exit
        exit.setToTop();

        transferfn.val = new VarSet();
        System.out.println("Initialization completed.");
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
        for (int i=1; i<in.length; i++) {
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
    public boolean isForward () { return false; }
    public Flow.DataflowObject getEntry() { return copy(entry); }
    public Flow.DataflowObject getExit() { return copy(exit); }
    public void setEntry(Flow.DataflowObject value) { entry.copy(value); }
    public void setExit(Flow.DataflowObject value) { exit.copy(value); }
    public Flow.DataflowObject getIn(Quad q) { return copy(in[q.getID()]); }
    public Flow.DataflowObject getOut(Quad q) { return copy(out[q.getID()]); }
    public void setIn(Quad q, Flow.DataflowObject value) { in[q.getID()].copy(value); }
    public void setOut(Quad q, Flow.DataflowObject value) { out[q.getID()].copy(value); }
    public Flow.DataflowObject newTempVar() { return new VarSet(); }

    public void processQuad(Quad q) {
        transferfn.val = (VarSet) getOut(q);
        Helper.runPass(q, transferfn); // use the visitor pattern
        setIn(q, transferfn.val);
    }

    private TransferFunction transferfn = new TransferFunction ();

    private Flow.DataflowObject copy(Flow.DataflowObject target) {
        Flow.DataflowObject result = newTempVar();
        result.copy(target);
        return result;
    }

    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        VarSet val;

        @Override
        public void visitMove(Quad q) {
            Operand oper = Operator.Move.getSrc(q);
            String dest = Operator.Move.getDest(q).getRegister().toString();
            // ignore constant
            if (oper instanceof Operand.RegisterOperand) {
                String used = ((Operand.RegisterOperand) oper).getRegister().toString();
                val.wakeVar(used, dest);
            }
        }

        @Override
        public void visitBinary(Quad q) {
            Operand oper1 = Operator.Binary.getSrc1(q);
            Operand oper2 = Operator.Binary.getSrc2(q);
            String dest = Operator.Binary.getDest(q).getRegister().toString();
            if (oper1 instanceof Operand.RegisterOperand) {
                String used = ((Operand.RegisterOperand) oper1).getRegister().toString();
                val.wakeVar(used, dest);
            }
            if (oper2 instanceof Operand.RegisterOperand) {
                String used = ((Operand.RegisterOperand) oper2).getRegister().toString();
                val.wakeVar(used, dest);
            }
        }

        @Override
        public void visitQuad(Quad q) {
            Operator oper = q.getOperator();
            if (!(oper instanceof Operator.Move || oper instanceof Operator.Binary)) {
                // forcibly mark all used variables as not fainted
                for (Operand.RegisterOperand used: q.getUsedRegisters()) {
                    val.wakeVar(used.getRegister().toString(), null);
                }
            }
        }
    }
}
