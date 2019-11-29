package submit;

import flow.Flow;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;

import java.util.Set;
import java.util.TreeSet;

public class RedundantNullChecks implements Flow.Analysis {

    public enum Mode {
        PRINT, VERBOSE, REMOVE
    }

    private VarSet[] in, out;
    private VarSet entry, exit;
    private TransferFunction transferfn = new TransferFunction();
    private Mode mode = Mode.PRINT;

    private boolean isRedundantNullCheck(Quad q) {
        // check operator type
        if (!(q.getOperator() instanceof Operator.NullCheck)) {
            return false;
        }
        // check if all used registers all redundant
        for (RegisterOperand r: q.getUsedRegisters()) {
            if (!in[q.getID()].contains(r.getRegister().toString())) {
                return false;
            }
        }
        return true;
    }

    public void setMode(Mode m) {
        this.mode = m;
    }

    public void preprocess(ControlFlowGraph cfg) {
        /* Generate initial conditions. */
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int x = qit.next().getID();
            if (x > max) max = x;
        }
        max += 1;
        in = new VarSet[max];
        out = new VarSet[max];
        qit = new QuadIterator(cfg);

        Set<String> s = new TreeSet<String>();
        VarSet.universalSet = s;

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            s.add("R" + i);
        }

        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                s.add(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                s.add(use.getRegister().toString());
            }
        }

        entry = new VarSet();
        entry.setToBottom();
        exit = new VarSet();
        transferfn.val = new VarSet();
        for (int i = 0; i < in.length; i++) {
            in[i] = new VarSet();
            out[i] = new VarSet();
        }
    }

    public void postprocess(ControlFlowGraph cfg) {
        Set<Integer> redundantChecks = new TreeSet<Integer>();
        QuadIterator it = new QuadIterator(cfg);
        while (it.hasNext()) {
            Quad q = it.next();
            if (isRedundantNullCheck(q)) {
                redundantChecks.add(q.getID());
                // remove if told to do so
                if (mode == Mode.REMOVE) {
                    it.remove();
                }
            }
        }
        // determine whether to print
        if (mode == Mode.PRINT) {
            System.out.print(cfg.getMethod().getName().toString());
            for (Integer q: redundantChecks) {
                System.out.print(" " + q);
            }
            System.out.println();
        } else if (mode == Mode.VERBOSE) {
            System.out.println("Method: " + cfg.getMethod().getName().toString());
            System.out.println("entry: " + entry.toString());
            QuadIterator qit = new QuadIterator(cfg);
            while (qit.hasNext()) {
                Quad q = qit.next();
                if (q.getOperator() instanceof Operator.NullCheck) {
                    System.out.println(q.getID() + " in: " + in[q.getID()].toString());
                    System.out.println(q.getID() + " out: " + out[q.getID()].toString());
                }
            }
            System.out.println("exit: " + exit.toString());
        }
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward() {
        return true;
    }

    /* Routines for interacting with dataflow values. */

    public Flow.DataflowObject getEntry() {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }

    public void setEntry(Flow.DataflowObject value) {
        entry.copy(value);
    }

    public Flow.DataflowObject getExit() {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }

    public void setExit(Flow.DataflowObject value) {
        exit.copy(value);
    }

    public Flow.DataflowObject getIn(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }

    public Flow.DataflowObject getOut(Quad q) {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }

    public void setIn(Quad q, Flow.DataflowObject value) {
        in[q.getID()].copy(value);
    }

    public void setOut(Quad q, Flow.DataflowObject value) {
        out[q.getID()].copy(value);
    }

    public Flow.DataflowObject newTempVar() {
        return new VarSet();
    }

    /* Actually perform the transfer operation on the relevant
     * quad. */

    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        transferfn.visitQuad(q);
        out[q.getID()].copy(transferfn.val);
    }

    public static class VarSet implements Flow.DataflowObject {
        public static Set<String> universalSet;
        private Set<String> set;

        public VarSet() {
            set = new TreeSet<String>(universalSet);
        }

        public void setToTop() {
            set = new TreeSet<String>(universalSet);
        }

        public void setToBottom() {
            set = new TreeSet<String>();
        }

        public void meetWith(Flow.DataflowObject o) {
            VarSet a = (VarSet) o;
            set.retainAll(a.set);
        }

        public void copy(Flow.DataflowObject o) {
            VarSet a = (VarSet) o;
            set = new TreeSet<String>(a.set);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof VarSet) {
                VarSet a = (VarSet) o;
                return set.equals(a.set);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }

        @Override
        public String toString() {
            return set.toString();
        }

        public void add(String v) {
            set.add(v);
        }

        public void remove(String v) {
            set.remove(v);
        }

        public boolean contains(String v) {
            return set.contains(v);
        }
    }

    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        VarSet val;

        @Override
        public void visitQuad(Quad q) {
            if (q.getOperator() instanceof Operator.NullCheck) {
                for (RegisterOperand r: q.getUsedRegisters()) {
                    val.add(r.getRegister().toString());
                }
            } else {
                for (RegisterOperand r: q.getDefinedRegisters()) {
                    val.remove(r.getRegister().toString());
                }
            }
        }
    }
}
