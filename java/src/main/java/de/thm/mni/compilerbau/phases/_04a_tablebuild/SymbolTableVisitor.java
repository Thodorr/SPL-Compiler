package de.thm.mni.compilerbau.phases._04a_tablebuild;

import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.Program;
import de.thm.mni.compilerbau.absyn.visitor.*;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.types.Type;
import de.thm.mni.compilerbau.utils.SplError;

import java.util.ArrayList;
import java.util.List;


public class SymbolTableVisitor extends DoNothingVisitor {
    private SymbolTable symbolTable;
    private Type type;
    private List<ParameterType> paramList;

    public SymbolTableVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void visit(Program program) {
        for (GlobalDeclaration decl : program.declarations) {
            decl.accept(this);
        }
    }

    public void visit(TypeDeclaration typeDeclaration) {
        typeDeclaration.typeExpression.accept(this);
        TypeEntry entry = new TypeEntry(type);

        Entry testEntry = symbolTable.lookup(typeDeclaration.name);
        if (testEntry != null) {
            throw SplError.RedeclarationAsType(typeDeclaration.position, typeDeclaration.name);
        }

        symbolTable.enter(typeDeclaration.name, entry);
    }

    public void visit(VariableDeclaration variableDeclaration) {
        variableDeclaration.typeExpression.accept(this);
        VariableEntry entry = new VariableEntry(type, false);

        Entry testEntry = symbolTable.lookup(variableDeclaration.name);
        if (testEntry != null) {
            if (testEntry instanceof VariableEntry) {
                throw SplError.RedeclarationAsVariable(variableDeclaration.position, variableDeclaration.name);
            }
        }

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

    /*
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

     */


    public void visit(ProcedureDeclaration procDec) {
        SymbolTable parentTable = symbolTable;
        symbolTable = new SymbolTable(symbolTable);
        paramList = new ArrayList<>();
        for (ParameterDeclaration parameter : procDec.parameters) {
            parameter.accept(this);
        }
        for (VariableDeclaration variable : procDec.variables) {
            variable.accept(this);
        }

        /*
        for (ParameterDeclaration parameter : procDec.parameters) {
            ParameterType paramType = new ParameterType(type, parameter.isReference);
            paramList.add(paramType);
        }
         */
        ProcedureEntry entry = new ProcedureEntry(symbolTable, paramList);
        symbolTable = parentTable;
        paramList = null;

        Entry testEntry = symbolTable.lookup(procDec.name);
        if (testEntry != null) {
            throw SplError.RedeclarationAsProcedure(procDec.position, procDec.name);
        }

        symbolTable.enter(procDec.name, entry);

        TableBuilder.printSymbolTableAtEndOfProcedure(procDec.name, entry);
    }

    public void visit(ParameterDeclaration parameterDeclaration) {
        parameterDeclaration.typeExpression.accept(this);
        paramList.add(new ParameterType(type, parameterDeclaration.isReference));
        VariableEntry entry = new VariableEntry(type, parameterDeclaration.isReference);

        Entry testEntry = symbolTable.lookup(parameterDeclaration.name);
        if (testEntry != null) {
            throw SplError.RedeclarationAsParameter(parameterDeclaration.position, parameterDeclaration.name);
        }

        symbolTable.enter(parameterDeclaration.name, entry);

        if (!parameterDeclaration.isReference && type instanceof ArrayType) {
            throw SplError.MustBeAReferenceParameter(parameterDeclaration.position, parameterDeclaration.name);
        }
    }

    public void visit(NamedTypeExpression namedTypeExpression) {
        Entry entry = symbolTable.lookup(namedTypeExpression.name);
        if (entry == null) {
            throw SplError.UndefinedType(namedTypeExpression.position, namedTypeExpression.name);
        }
        if (!(entry instanceof TypeEntry)) {
            throw SplError.NotAType(namedTypeExpression.position, namedTypeExpression.name);
        }
        TypeEntry typeEntry = (TypeEntry)entry;
        type = typeEntry.type;
    }

    public void visit(ArrayTypeExpression arrayTy) {
        arrayTy.baseType.accept(this);
        type = new ArrayType(type, arrayTy.arraySize);
    }


    // Implement other visit methods for different AST nodes if necessary

}