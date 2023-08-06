package de.thm.mni.compilerbau.phases._05_varalloc;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.table.*;

import java.util.List;


public class Passage2VarAllocatorVisitor extends DoNothingVisitor {
    private SymbolTable table;
    private int outgoingSize;

    public Passage2VarAllocatorVisitor(SymbolTable table) {
        this.table = table;
    }

    public void visit(Program program) {
        for (GlobalDeclaration decl : program.declarations) {
            decl.accept(this);
        }
    }

    public void visit(ProcedureDeclaration procedureDeclaration) {
        Entry entry = table.lookup(procedureDeclaration.name);
        ProcedureEntry procedureEntry = (ProcedureEntry) entry;
        if (procedureEntry.stackLayout.outgoingAreaSize != Integer.MIN_VALUE) {
            return;
        }

        outgoingSize = -1;
        for (Statement statement : procedureDeclaration.body) {
            statement.accept(this);
        }
        procedureEntry.stackLayout.outgoingAreaSize = outgoingSize;
    }

    public void visit(List<Statement> statements) {
        for (Statement statement : statements) {
            statement.accept(this);
        }
    }

    public void visit(WhileStatement whileStatement) {
        whileStatement.body.accept(this);
    }

    public void visit(IfStatement ifStatement) {
        ifStatement.thenPart.accept(this);
        ifStatement.elsePart.accept(this);
    }

    public void visit(CompoundStatement compoundStatement) {
        for (Statement statement : compoundStatement.statements) {
            statement.accept(this);
        }
    }

    public void visit(CallStatement call) {
        Entry entry = table.lookup(call.procedureName);
        ProcedureEntry procedureEntry = (ProcedureEntry) entry;

        outgoingSize = Math.max(outgoingSize, procedureEntry.stackLayout.argumentAreaSize);
    }
}