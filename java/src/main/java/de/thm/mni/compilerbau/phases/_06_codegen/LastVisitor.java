package de.thm.mni.compilerbau.phases._06_codegen;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.table.ParameterType;
import de.thm.mni.compilerbau.table.ProcedureEntry;
import de.thm.mni.compilerbau.table.SymbolTable;
import de.thm.mni.compilerbau.table.VariableEntry;
import de.thm.mni.compilerbau.types.ArrayType;

import java.util.List;

public class LastVisitor extends DoNothingVisitor {
    private SymbolTable table;
    private CodePrinter printer;

    Register stackPointerRegister = new Register(29);
    Register framePointerRegister = new Register(25);
    Register returnPointerRegister = new Register(31);

    public LastVisitor(SymbolTable table, CodePrinter printer) {
        this.table = table;
        this.printer = printer;
    }

    public void visit(Program program) {
        for (GlobalDeclaration decl : program.declarations) {
            decl.accept(this);
        }
    }

    public void visit(ProcedureDeclaration procedureDeclaration) {
        ProcedureEntry procedureEntry = (ProcedureEntry) table.lookup(procedureDeclaration.name);
        SymbolTable prevTable = table;
        table = procedureEntry.localTable;

        printer.emit("");
        printer.emitExport(procedureDeclaration.name.toString());

        printer.emitLabel(procedureDeclaration.name.toString());

        printer.emitInstruction("sub", stackPointerRegister, stackPointerRegister, procedureEntry.stackLayout.frameSize(), "allocate frame");
        printer.emitInstruction("stw", framePointerRegister, stackPointerRegister, procedureEntry.stackLayout.oldFramePointerOffset(), "save old frame pointer");
        printer.emitInstruction("add", framePointerRegister, stackPointerRegister, procedureEntry.stackLayout.frameSize(), "setup new frame pointer");

        if (procedureEntry.stackLayout.outgoingAreaSize != -1) {
            printer.emitInstruction("stw", returnPointerRegister, framePointerRegister, procedureEntry.stackLayout.oldFramePointerOffset(), "save return register");
        }

        for(Statement statement : procedureDeclaration.body) {
            statement.accept(this);
        }

        if (procedureEntry.stackLayout.outgoingAreaSize != -1) {
            printer.emitInstruction("ldw", returnPointerRegister, framePointerRegister, procedureEntry.stackLayout.oldFramePointerOffset(), "restore return register");
        }
        printer.emitInstruction("ldw", framePointerRegister, stackPointerRegister, procedureEntry.stackLayout.oldFramePointerOffset(), "restore old frame pointer");
        printer.emitInstruction("add", stackPointerRegister, stackPointerRegister, procedureEntry.stackLayout.frameSize(), "release frame");
        printer.emitInstruction("jr", returnPointerRegister, "return");
        table = prevTable;
    }

    public void visit(List<Statement> statementList) {
        for (Statement statement : statementList) {
            statement.accept(this);
        }
    }

    public void visit(CompoundStatement compoundStatement) {
        for (Statement statement : compoundStatement.statements) {
            statement.accept(this);
        }
    }

    public void visit(CallStatement callStatement) {
        ProcedureEntry entry = (ProcedureEntry) table.lookup(callStatement.procedureName);
        List<ParameterType> params = entry.parameterTypes;

        int counter = 0;
        for (Expression expression : callStatement.arguments) {
            ParameterType parameter = params.get(counter);
            if (!parameter.isReference) {
                expression.accept(this);
            } else {
                ((VariableExpression)expression).variable.accept(this);
            }
            Register topRegister = getTopRegister();
            printer.emitInstruction("stw", topRegister, stackPointerRegister, parameter.offset, String.format("store arg #%d", counter));
            popRegister();
            counter++;
        }
        String emitStr = "\tjal\t" + callStatement.procedureName.toString() + "\n";
        printer.emit(emitStr);
    }

    public void visit(VariableExpression variableExpression) {
        variableExpression.variable.accept(this);
        final Register variableRegister = getTopRegister();
        printer.emitInstruction("ldw", variableRegister, variableRegister, 0);
    }

    public void visit(AssignStatement assignStatement) {
        //Might need to switch
        assignStatement.target.accept(this);
        Register targetRegister = getTopRegister();
        assignStatement.value.accept(this);
        Register valueRegister = getTopRegister();
        printer.emitInstruction("stw", targetRegister, valueRegister, 0);
        popRegister();
        popRegister();
    }

    public void visit(NamedVariable variable) {
        VariableEntry entry = (VariableEntry) table.lookup(variable.name);
        if (!entry.isReference) {
            printer.emitInstruction("add", pushRegister(), framePointerRegister, entry.offset);
        } else {
            printer.emitInstruction("add", pushRegister(), framePointerRegister, entry.offset);
            printer.emitInstruction("ldw", pushRegister(), pushRegister(), 0);
        }
    }

    public void visit(ArrayAccess array) {
        array.array.accept(this);
        Register variableRegister = getTopRegister();

        array.index.accept(this);
        Register indexRegister = getTopRegister();

        ArrayType arrayType = (ArrayType)array.array.dataType;
        Register sizeRegister = pushRegister();
        printer.emitInstruction("add", sizeRegister, new Register(0), arrayType.arraySize);

        printer.emitInstruction("bgeu", indexRegister, sizeRegister, "_indexError");
        popRegister();

        printer.emitInstruction("mul", indexRegister, indexRegister, arrayType.baseType.byteSize);
        printer.emitInstruction("add", variableRegister, variableRegister, indexRegister);
        popRegister();
    }

    public void visit(IntLiteral intLiteral) {
        Register register = pushRegister();
        if (intLiteral.value < 0) {
            printer.emitInstruction("sub", register, new Register(0), -intLiteral.value);
        } else {
            printer.emitInstruction("add", register, new Register(0), intLiteral.value);
        }
    }

    public void visit(BinaryExpression binaryExpression) {
        binaryExpression.leftOperand.accept(this);
        Register leftRegister = getTopRegister();
        binaryExpression.rightOperand.accept(this);
        Register rightRegister = getTopRegister();
        popRegister();
        popRegister();
        Register resultRegister;
        switch (binaryExpression.operator) {
            case ADD:
                resultRegister = pushRegister();
                printer.emitInstruction("add", resultRegister, leftRegister, rightRegister);
                break;
            case SUB:
                resultRegister = pushRegister();
                printer.emitInstruction("sub", resultRegister, leftRegister, rightRegister);
                break;
            case MUL:
                resultRegister = pushRegister();
                printer.emitInstruction("mul", resultRegister, leftRegister, rightRegister);
                break;
            case DIV:
                resultRegister = pushRegister();
                printer.emitInstruction("div", resultRegister, leftRegister, rightRegister);
                break;
        }
    }

    /*
    public void visit(List<Expression> expressionList) {
        for (Expression expression : expressionList) {
            expression.accept(this);
        }
    }

     */

    int label = 0;
    public void visit(WhileStatement whileStatement) {
        String repeatLabelName = labelName(label);
        printer.emitLabel(repeatLabelName);
        label++;
        String exitLabelName = labelName(label);
        label++;
        BinaryExpression condition = (BinaryExpression) whileStatement.condition;
        manageBinary(condition, exitLabelName);
        whileStatement.body.accept(this);
        printer.emit("\tj\t" + repeatLabelName +"\n");
        printer.emitLabel(exitLabelName);
    }

    public void visit(IfStatement ifStatement) {
        BinaryExpression condition = (BinaryExpression) ifStatement.condition;
        String elseLabelName = null;
        if (ifStatement.elsePart != null && !(ifStatement.elsePart instanceof EmptyStatement)) {
            elseLabelName = labelName(label);
            label++;
        }
        String exitLabelName = labelName(label);
        label++;
        manageBinary(condition, elseLabelName != null ? elseLabelName : exitLabelName);
        ifStatement.thenPart.accept(this);

        if (elseLabelName != null) {
            printer.emit("\tj\t" + exitLabelName +"\n");
            printer.emitLabel(elseLabelName);
            ifStatement.elsePart.accept(this);
        }
        printer.emitLabel(exitLabelName);
    }

    private void manageBinary(BinaryExpression binaryExpression, String labelJumpToIfFalse) {
        binaryExpression.leftOperand.accept(this);
        Register leftReg = getTopRegister();
        binaryExpression.rightOperand.accept(this);
        Register rightReg = getTopRegister();
        popRegister();
        popRegister();
        switch (binaryExpression.operator) {
            case EQU:
                printer.emitInstruction("bne", leftReg, rightReg, labelJumpToIfFalse);
                break;
            case NEQ:
                printer.emitInstruction("beq", leftReg, rightReg, labelJumpToIfFalse);
                break;
            case LST:
                printer.emitInstruction("bge", leftReg, rightReg, labelJumpToIfFalse);
                break;
            case LSE:
                printer.emitInstruction("bgt", leftReg, rightReg, labelJumpToIfFalse);
                break;
            case GRT:
                printer.emitInstruction("ble", leftReg, rightReg, labelJumpToIfFalse);
                break;
            case GRE:
                printer.emitInstruction("blt", leftReg, rightReg, labelJumpToIfFalse);
                break;
        }
    }

    public static class RegisterStackException extends RuntimeException {
        public RegisterStackException(String message) {
            super(message);
        }
    }

    //Maybe put them in separate class
    private int currentTopRegister = 8;
    private Register getTopRegister() {
        /*
        if (currentTopRegister <= 8) {
            throw new RegisterStackException("Register stack is underflowing");
        }

         */
        return new Register(currentTopRegister - 1);
    }
    private Register popRegister() {
        currentTopRegister--;
        /*
        if (currentTopRegister < 8) {
            throw new RegisterStackException("Register stack is underflowing");
        }

         */
        return new Register(currentTopRegister);
    }
    private Register pushRegister() {
        if (currentTopRegister > 23) {
            throw new RegisterStackException("No more register");
        }
        return new Register(currentTopRegister+1);
    }

    private String labelName(int labelNumber) {
        return String.format("L%d", labelNumber);
    }
}
