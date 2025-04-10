package tree;

import java.util.*;

import java_cup.runtime.ComplexSymbolFactory.Location;
import syms.Scope;
import syms.SymEntry;
import source.ErrorHandler;

/**
 * class StatementNode - Abstract syntax tree representation of statements.
 * Classes defined within StatementNode extend it to give nodes for each kind
 * of statement.
 */
public abstract class StatementNode {
    /**
     * Location in the input source program (line and column number effectively).
     * All statements have a location within the original source code.
     */
    private final Location loc;

    /**
     * Constructor
     */
    StatementNode(Location loc) {
        this.loc = loc;
    }

    public Location getLocation() {
        return loc;
    }

    /**
     * All statement nodes provide an accept method to implement the visitor
     * pattern to traverse the tree.
     *
     * @param visitor class implementing the details of the particular
     *                traversal.
     */
    public void accept(StatementVisitor visitor){
        ErrorHandler.getErrorHandler().fatal(
                "accept visitor not implemented for " +
                this.getClass().getName(),
                loc);
        //not reachable after fatal
    }


    /**
     * Debugging output of a statement at an indent level
     */
    public abstract String toString(int level);

    /**
     * Debugging output at level 0
     */
    @Override
    public String toString() {
        return this.toString(0);
    }

    /**
     * Returns a string with a newline followed by spaces of length 2n.
     */
    public static String newLine(int n) {
        StringBuilder indent = new StringBuilder("\n");
        while (n > 0) {
            indent.append("  ");
            n--;
        }
        return indent.toString();
    }

    /**
     * Statement node representing an erroneous statement.
     */
    public static class ErrorNode extends StatementNode {
        public ErrorNode(Location loc) {
            super(loc);
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitStatementErrorNode(this);
        }

        @Override
        public String toString(int level) {
            return "ERROR";
        }
    }

    /**
     * Node representing a Block consisting of declarations and
     * body of a procedure, function, or the main program.
     */
    public static class BlockNode extends StatementNode {
        private final DeclNode.DeclListNode procedures; // declared within block
        private StatementNode body; // compound statement body
        private final Scope blockLocals;  // scope of locals within block

        /**
         * Constructor for a block within a procedure
         */
        public BlockNode(Location loc, DeclNode.DeclListNode procedures,
                         StatementNode body, Scope blockLocals) {
            super(loc);
            this.procedures = procedures;
            this.body = body;
            this.blockLocals = blockLocals;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitBlockNode(this);
        }

        public DeclNode.DeclListNode getProcedures() {
            return procedures;
        }

        public StatementNode getBody() {
            return body;
        }

        public void setBody(StatementNode body) { this.body = body; }

        @Override
        public String toString(int level) {
            return getProcedures().toString(level + 1) +
                    newLine(level) + "BEGIN" +
                    newLine(level + 1) + body.toString(level + 1) +
                    newLine(level) + "END";
        }
    }


    /**
     * Tree node representing an assignment statement.
     */
    public static class AssignmentNode extends StatementNode {
        /* Now its a list of single assignments 1+ */
        private final List<SingleAssignNode> assignments;



        public ExpNode getLValue() {
            return isSingle() ? assignments.get(0).getLValue() : null;
        }


        public AssignmentNode(List<SingleAssignNode> assignments) {
            super(assignments.get(0).getLocation());
            this.assignments = assignments;
        }

        // ✅ Preserves support for single assignment
        public ExpNode getVariable() {
            return isSingle() ? assignments.get(0).getLValue() : null;
        }

        public ExpNode getExp() {
            return isSingle() ? assignments.get(0).getExp() : null;
        }

        public boolean isSingle() {
            return assignments.size() == 1;
        }

        public List<SingleAssignNode> getAssignments() {
            return assignments;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitAssignmentNode(this);
        }

        @Override
        public String toString(int level) {
            StringBuilder sb = new StringBuilder();
            String sep = "";
            for (SingleAssignNode assign : assignments) {
                sb.append(sep).append(assign.toString());
                sep = " | ";
            }
            return sb.toString();
        }
    }

    /**
     * Tree node representing a "read" statement.
     */
    /**
     * Tree node representing a "read" statement.
     */
    public static class ReadNode extends StatementNode {
        ExpNode lValue;  // lValue to be assigned by read

        public ReadNode(Location loc, ExpNode lValue) {
            super(loc);
            this.lValue = lValue;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitReadNode(this);
        }

        public ExpNode getLValue() {
            return lValue;
        }

        public void setLValue(ExpNode lValue) {
            this.lValue = lValue;
        }
        @Override
        public String toString(int level) {
            return "Read " + lValue;
        }
    }

    /**
     * Tree node representing a "write" statement.
     */
    public static class WriteNode extends StatementNode {
        private ExpNode exp;

        public WriteNode(Location loc, ExpNode exp) {
            super(loc);
            this.exp = exp;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitWriteNode(this);
        }

        public ExpNode getExp() {
            return exp;
        }

        public void setExp(ExpNode exp) {
            this.exp = exp;
        }

        @Override
        public String toString(int level) {
            return "WRITE " + exp;
        }
    }

    /**
     * Tree node representing a "call" statement.
     */
    public static class CallNode extends StatementNode {
        private final String id;
        private SymEntry.ProcedureEntry procEntry;

        public CallNode(Location loc, String id) {
            super(loc);
            this.id = id;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitCallNode(this);
        }

        public String getId() {
            return id;
        }

        public SymEntry.ProcedureEntry getEntry() {
            return procEntry;
        }

        public void setEntry(SymEntry.ProcedureEntry entry) {
            this.procEntry = entry;
        }

        @Override
        public String toString(int level) {
            StringBuilder s = new StringBuilder("CALL " + id + "(");
            return s + ")";
        }
    }

    /**
     * Tree node representing a statement list.
     */
    public static class ListNode extends StatementNode {
        private final List<StatementNode> statements;

        public ListNode(Location loc, List<StatementNode> sl) {
            super(loc);
            this.statements = sl;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitStatementListNode(this);
        }

        public List<StatementNode> getStatements() {
            return statements;
        }

        @Override
        public String toString(int level) {
            StringBuilder result = new StringBuilder();
            String sep = "";
            for (StatementNode s : statements) {
                result.append(sep).append(s.toString(level));
                sep = ";" + newLine(level);
            }
            return result.toString();
        }
    }

    /**
     * Tree node representing an "if" statement.
     */
    public static class IfNode extends StatementNode {
        private ExpNode condition;
        private final StatementNode thenStmt;
        private final StatementNode elseStmt;

        public IfNode(Location loc, ExpNode condition,
                      StatementNode thenStmt, StatementNode elseStmt) {
            super(loc);
            this.condition = condition;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitIfNode(this);
        }

        public ExpNode getCondition() {
            return condition;
        }

        public void setCondition(ExpNode cond) {
            this.condition = cond;
        }

        public StatementNode getThenStmt() {
            return thenStmt;
        }

        public StatementNode getElseStmt() {
            return elseStmt;
        }

        @Override
        public String toString(int level) {
            return "IF " + condition.toString() + " THEN" +
                    newLine(level + 1) + thenStmt.toString(level + 1) +
                    newLine(level) + "ELSE" +
                    newLine(level + 1) + elseStmt.toString(level + 1);
        }
    }

    /**
     * Tree node representing a "while" statement.
     */
    public static class WhileNode extends StatementNode {
        private ExpNode condition;
        private final StatementNode loopStmt;

        public WhileNode(Location loc, ExpNode condition,
                         StatementNode loopStmt) {
            super(loc);
            this.condition = condition;
            this.loopStmt = loopStmt;
        }

        @Override
        public void accept(StatementVisitor visitor) {
            visitor.visitWhileNode(this);
        }

        public ExpNode getCondition() {
            return condition;
        }

        public void setCondition(ExpNode cond) {
            this.condition = cond;
        }

        public StatementNode getLoopStmt() {
            return loopStmt;
        }

        @Override
        public String toString(int level) {
            return "WHILE " + condition.toString() + " DO" +
                    newLine(level + 1) + loopStmt.toString(level + 1);
        }
    }

    /* Single Assign Node */
    public static class SingleAssignNode {
        private final Location loc;
        private ExpNode lValue;
        private ExpNode exp;

        public SingleAssignNode(Location loc, ExpNode lValue, ExpNode exp) {
            this.loc = loc;
            this.lValue = lValue;
            this.exp = exp;
        }

        /* Add getters and setters */

        public Location getLocation() {
            return loc;
        }

        public ExpNode getLValue() {
            return lValue;
        }

        public void setLValue(ExpNode lValue) {
            this.lValue = lValue;
        }

        public ExpNode getExp() {
            return exp;
        }

        public void setExp(ExpNode exp) {
            this.exp = exp;
        }

        @Override
        public String toString() {
            return lValue.toString() + " := " + exp.toString();
        }
    }

}

