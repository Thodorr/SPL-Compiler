package de.thm.mni.compilerbau.phases._04b_semant;

import de.thm.mni.compilerbau.CommandLineOptions;
import de.thm.mni.compilerbau.absyn.*;
import de.thm.mni.compilerbau.absyn.visitor.DoNothingVisitor;
import de.thm.mni.compilerbau.table.*;
import de.thm.mni.compilerbau.types.ArrayType;
import de.thm.mni.compilerbau.types.PrimitiveType;
import de.thm.mni.compilerbau.types.Type;
import de.thm.mni.compilerbau.utils.NotImplemented;
import de.thm.mni.compilerbau.utils.SplError;

import java.util.List;

/**
 * This class is used to check if the currently compiled SPL program is semantically valid.
 * The body of each procedure has to be checked, consisting of {@link Statement}s, {@link Variable}s and {@link Expression}s.
 * Each node has to be checked for type issues or other semantic issues.
 * Calculated {@link Type}s can be stored in and read from the dataType field of the {@link Expression} and {@link Variable} classes.
 */
public class ProcedureBodyChecker {

    private final CommandLineOptions options;

    public ProcedureBodyChecker(CommandLineOptions options) {
        this.options = options;
    }

    public void checkProcedures(Program program, SymbolTable globalTable) {
        //TODO (assignment 4b): Check all procedure bodies for semantic errors
        checkForMain(globalTable);
        program.accept(new ProcedureBodyCheckVisitor(globalTable));
    }

    private void checkForMain(SymbolTable globalTable) {
        Entry entry = globalTable.lookup(new Identifier("main"));
        if (entry == null) {
            throw SplError.MainIsMissing();
        }
        if (!(entry instanceof ProcedureEntry)) {
            throw SplError.MainIsNotAProcedure();
        }
        List<ParameterType> params = ((ProcedureEntry)entry).parameterTypes;
        if (params.size() != 0) {
            throw SplError.MainMustNotHaveParameters();
        }
    }

    private static class ProcedureBodyCheckVisitor extends DoNothingVisitor {
        private SymbolTable table;

        private ProcedureBodyCheckVisitor(SymbolTable table) {
            this.table = table;
        }

        public void visit(Program program) {
            for (GlobalDeclaration decl : program.declarations) {
                decl.accept(this);
            }
        }

        public void workList(List<Statement> list) {
            for (Statement s : list) {
                s.accept(this);
            }
        }

        public void visit(ProcedureDeclaration procedureDeclaration) {
            SymbolTable parentTable = table;
            ProcedureEntry entry = (ProcedureEntry) table.lookup(procedureDeclaration.name);
            table = entry.localTable;
            workList(procedureDeclaration.body);
            table = parentTable;
        }


        public void visit(CompoundStatement comp) {
            workList(comp.statements);
        }

        public void visit(IfStatement ifStatement) {
            ifStatement.condition.accept(this);
            if (ifStatement.condition.dataType != PrimitiveType.boolType) {
                throw SplError.IfConditionMustBeBoolean(ifStatement.position, ifStatement.condition.dataType);
            }
            ifStatement.thenPart.accept(this);
            ifStatement.elsePart.accept(this);
        }

        public void visit(WhileStatement whileStatement) {
            whileStatement.condition.accept(this);
            if (whileStatement.condition.dataType != PrimitiveType.boolType) {
                throw SplError.WhileConditionMustBeBoolean(whileStatement.position, whileStatement.condition.dataType);
            }
            whileStatement.body.accept(this);
        }

        public void visit(AssignStatement assignStatement) {
            assignStatement.target.accept(this);
            assignStatement.value.accept(this);
            if (assignStatement.target.dataType instanceof ArrayType) {
                throw SplError.IllegalAssignmentToArray(assignStatement.position);
            }
            if (assignStatement.target.dataType != assignStatement.value.dataType) {
                throw SplError.IllegalAssignment(assignStatement.position, assignStatement.target.dataType, assignStatement.value.dataType);
            }
        }

        public void visit(CallStatement callStatement) {
            Entry entry = table.lookup(callStatement.procedureName);

            if (entry == null) {
                throw SplError.UndefinedProcedure(callStatement.position, callStatement.procedureName);
            }
            if (!(entry instanceof ProcedureEntry)) {
                throw SplError.CallOfNonProcedure(callStatement.position, callStatement.procedureName);
            }
            ProcedureEntry procedureEntry = (ProcedureEntry) entry;
            List<ParameterType> parameters = procedureEntry.parameterTypes;
            int counter = 0;
            for (Expression expression : callStatement.arguments) {
                counter++;
            }
            if (counter < parameters.size()) {
                throw SplError.TooFewArguments(callStatement.position, callStatement.procedureName);
            } else if (counter > parameters.size()) {
                throw SplError.TooManyArguments(callStatement.position, callStatement.procedureName);
            }
            int i = 0;
            for (Expression expression : callStatement.arguments) {
                expression.accept(this);
                if (expression.dataType != parameters.get(i).type) {
                    throw SplError.ArgumentTypeMismatch(callStatement.position, callStatement.procedureName, i+1, expression.dataType, parameters.get(i).type);
                }
                if (parameters.get(i).isReference && !(expression instanceof VariableExpression)) {
                    throw SplError.ArgumentMustBeAVariable(callStatement.position, callStatement.procedureName, i+1);
                }
                i++;
            }

        }

        public void visit(IntLiteral intLiteral) {
            intLiteral.dataType = PrimitiveType.intType;
        }

        public void visit(UnaryExpression unaryExpression) {
            //For some reason the datatype is null
            if (unaryExpression.operand.dataType != PrimitiveType.intType) {
                throw SplError.NoSuchOperator(unaryExpression.position, unaryExpression.operator, unaryExpression.operand.dataType);
            }
        }

        public void visit(BinaryExpression binaryExpression) {
            binaryExpression.leftOperand.accept(this);
            binaryExpression.rightOperand.accept(this);

            if (binaryExpression.leftOperand.dataType != binaryExpression.rightOperand.dataType) {
                throw SplError.NoSuchOperator(binaryExpression.position, binaryExpression.operator, binaryExpression.leftOperand.dataType, binaryExpression.rightOperand.dataType);
            }

            switch (binaryExpression.operator) {
                case NEQ:
                case GRT:
                case GRE:
                case EQU:
                case LSE:
                case LST: {
                    if (binaryExpression.leftOperand.dataType != PrimitiveType.intType || binaryExpression.rightOperand.dataType != PrimitiveType.intType) {
                        throw SplError.NoSuchOperator(binaryExpression.position, binaryExpression.operator, binaryExpression.leftOperand.dataType, binaryExpression.rightOperand.dataType);
                    }

                    binaryExpression.dataType = PrimitiveType.boolType;
                } break;

                default:
                    if (binaryExpression.leftOperand.dataType != PrimitiveType.intType || binaryExpression.rightOperand.dataType != PrimitiveType.intType) {
                        throw SplError.NoSuchOperator(binaryExpression.position, binaryExpression.operator, binaryExpression.leftOperand.dataType, binaryExpression.rightOperand.dataType);
                    }
                    binaryExpression.dataType = PrimitiveType.intType;
                    break;
            }
        }

        public void visit(VariableExpression variableExpression) {
            variableExpression.variable.accept(this);
            variableExpression.dataType = variableExpression.variable.dataType;
        }

        public void visit(NamedVariable var) {
            Entry entry = table.lookup(var.name);
            if (entry == null) {
                throw SplError.UndefinedVariable(var.position, var.name);
            }
            if (!(entry instanceof VariableEntry)) {
                throw SplError.NotAVariable(var.position, var.name);
            }
            var.dataType = ((VariableEntry)entry).type;
        }

        public void visit(ArrayAccess array) {
            array.index.accept(this);
            if (array.index.dataType != PrimitiveType.intType) {
                throw SplError.IndexingWithNonInteger(array.position);
            }
            array.array.accept(this);
            if (!(array.array.dataType instanceof ArrayType)) {
                throw SplError.IndexingNonArray(array.position);
            }
            array.dataType = ((ArrayType)array.array.dataType).baseType;
        }
    }

}
