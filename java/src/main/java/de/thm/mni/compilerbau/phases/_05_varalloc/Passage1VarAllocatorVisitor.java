package de.thm.mni.compilerbau.phases._05_varalloc;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.phases._04a_tablebuild.SymbolTableVisitor;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.PrimitiveType;

import java.util.List;

public class Passage1VarAllocatorVisitor extends DoNothingVisitor {
    private SymbolTable table;
    private List<ParameterType> list;
    private int decIndex;
    private int decOffset;
    private int refByteSize = 4;

    public Passage1VarAllocatorVisitor(SymbolTable table) {
        this.table = table;
    }

    public void visit(Program program) {
        for (GlobalDeclaration decl : program.declarations) {
            decl.accept(this);
        }
    }

    public void visit(ProcedureDeclaration procedureDeclaration) {
        Entry entry = table.lookup(procedureDeclaration.name);
        ProcedureEntry procEntry = (ProcedureEntry) entry;
        SymbolTable globalTable = table;
        list = procEntry.parameterTypes;
        table = procEntry.localTable;

        decIndex = 0;
        decOffset = 0;

        for (ParameterDeclaration parameter : procedureDeclaration.parameters) {
            parameter.accept(this);
        }
        procEntry.stackLayout.argumentAreaSize = decOffset;

        decOffset = 0;

        for (VariableDeclaration var : procedureDeclaration.variables) {
            var.accept(this);
        }

        procEntry.stackLayout.localVarAreaSize = Math.abs(decOffset);

        //prepare for second pass
        procEntry.stackLayout.outgoingAreaSize = Integer.MIN_VALUE;

        table = globalTable;
    }

    public void visit(ParameterDeclaration parameterDeclaration) {
        ParameterType type = list.get(decIndex);
        type.offset = decOffset;
        Entry entry = table.lookup(parameterDeclaration.name);
        VariableEntry variableEntry = (VariableEntry) entry;
        variableEntry.offset = decOffset;
        if (parameterDeclaration.isReference) {
            decOffset += refByteSize;
        } else {
            decOffset += type.type.byteSize;
        }
        decIndex++;
    }

    public void visit(VariableDeclaration variableDeclaration) {
        Entry entry = table.lookup(variableDeclaration.name);
        VariableEntry varEntry = (VariableEntry) entry;
        decOffset -= varEntry.type.byteSize;
        varEntry.offset = decOffset;
    }
}
