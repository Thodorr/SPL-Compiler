package de.thm.mni.compilerbau.phases._04a_tablebuild;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.CommandLineOptions;
import de.thm.mni.compilerbau.absyn.Expression;
import de.thm.mni.compilerbau.absyn.Program;
import de.thm.mni.compilerbau.absyn.TypeExpression;
import de.thm.mni.compilerbau.absyn.Variable;
import de.thm.mni.compilerbau.absyn.visitor.*;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.Type;
import de.thm.mni.compilerbau.utils.NotImplemented;

import java.util.ArrayList;
import java.util.List;


public class SymbolTableVisitor extends DoNothingVisitor {
    private final SymbolTable symbolTable;
    private Type type;

    public SymbolTableVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void visit(Program program) {
        // Visit global declarations
        for (GlobalDeclaration decl : program.declarations) {
            decl.accept(this);
        }
    }

    public void visit(TypeDeclaration typeDeclaration) {
        typeDeclaration.accept(this);
        TypeEntry entry = new TypeEntry(type);
        symbolTable.enter(typeDeclaration.name, entry);
    }

    public void visit(VariableDeclaration variableDeclaration) {
        VariableEntry entry = new VariableEntry(variableDeclaration.typeExpression.dataType, false);
        symbolTable.enter(variableDeclaration.name, entry);
    }

    /*
    public void visit(ProcedureDeclaration procedureDeclaration) {
        List<ParameterType> types = new ArrayList<ParameterType>();
        for (ParameterDeclaration decl : procedureDeclaration.parameters) {
            ParameterType type = new ParameterType(decl.typeExpression.dataType, decl.isReference);
        }
        ProcedureEntry entry = new ProcedureEntry(symbolTable, types);
        symbolTable.enter(procedureDeclaration.name, entry);

        // Visit the procedure's block and add its symbols to the local table
        for (VariableDeclaration var : procedureDeclaration.variables) {
            var.accept(this);
        }
        for (ParameterDeclaration para : procedureDeclaration.parameters) {
            para.accept(this);
        }

        // Create a new local symbol table for the procedure
        SymbolTable localTable = new SymbolTable(symbolTable);
        TableBuilder.printSymbolTableAtEndOfProcedure(procedureDeclaration.name, (ProcedureEntry) symbolTable.lookup(procedureDeclaration.name));
    }
     */

    public void visit(ProcedureDeclaration procedureDeclaration) {
        // Create a new local symbol table for the procedure
        SymbolTable localSymbolTable = new SymbolTable(symbolTable);

        // Visit the procedure's parameters and add them to the local symbol table
        for (ParameterDeclaration parameter : procedureDeclaration.parameters) {
            parameter.accept(this);
        }

        // Visit the procedure's local variables and add them to the local symbol table
        for (VariableDeclaration variable : procedureDeclaration.variables) {
            variable.accept(this);
        }

        // Visit the procedure's block and add its symbols to the local symbol table
        for (Statement statement : procedureDeclaration.body) {
            statement.accept(this);
        }

        // Create a ProcedureEntry with the local symbol table and parameter types
        List<ParameterType> parameterTypes = new ArrayList<>();
        for (ParameterDeclaration parameter : procedureDeclaration.parameters) {
            ParameterType type = new ParameterType(parameter.typeExpression.dataType, parameter.isReference);
            parameterTypes.add(type);
        }
        ProcedureEntry entry = new ProcedureEntry(localSymbolTable, parameterTypes);

        // Add the procedure entry to the global symbol table
        symbolTable.enter(procedureDeclaration.name, entry);

        // Print the symbol table at the end of the procedure
        TableBuilder.printSymbolTableAtEndOfProcedure(procedureDeclaration.name, entry);
    }
    public void visit(ParameterDeclaration parameterDeclaration) {
        VariableEntry entry = new VariableEntry(parameterDeclaration.typeExpression.dataType, parameterDeclaration.isReference);
        symbolTable.enter(parameterDeclaration.name, entry);
    }


    // Implement other visit methods for different AST nodes if necessary

}