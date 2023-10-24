package edu.ufl.cise.cop4020fa23;
import java.util.Stack;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.SymbolTable;

public class TypeCheckVisitor implements ASTVisitor {

    private SymbolTable symbolTable;

    // Stack to store return types of functions/programs
    private Stack<Type> returnTypeStack = new Stack<>();


    public TypeCheckVisitor() {
        this.symbolTable = new SymbolTable();
    }


    @Override
    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
        // Set the program's type based on the typeToken using the kind method
        program.setType(Type.kind2type(program.getTypeToken().kind()));

        // Enter a new scope for the program
        symbolTable.enterScope();

        // Visit the parameters (NameDef objects) of the program
        for (NameDef param : program.getParams()) {
            param.visit(this, arg);
        }

        // Visit the main block of the program
        program.getBlock().visit(this, arg);

        // Leave the program's scope using closeScope method
        symbolTable.closeScope();

        // Return the program's type
        return program.getType();
    }


    @Override
    public Object visitBlock(Block block, Object arg) throws PLCCompilerException {
        // Enter a new scope for the block
        symbolTable.enterScope();

        // Visit each element in the block
        for (Block.BlockElem elem : block.getElems()) {
            elem.visit(this, arg);
        }

        // Leave the block's scope
        symbolTable.closeScope();

        return null; // Since a block doesn't have a return type
    }


    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) throws PLCCompilerException {
        // Visit the left and right operands to get their types
        Type leftType = (Type) expr.getLeftExpr().visit(this, arg);
        Type rightType = (Type) expr.getRightExpr().visit(this, arg);

        // Infer the resulting type based on the operator and operand types
        Type resultType = null;  // Initialize resultType to null for now
        switch (expr.getOpKind()) {
            case PLUS:
            case MINUS:
            case TIMES:
                // Additional logic to determine resultType based on operand types
                // ...
                break;
            // Handle other operators similarly
            // ...
            default:
                throw new PLCCompilerException("Unrecognized binary operator");
        }

        // Check if resultType has been determined
        if (resultType == null) {
            throw new PLCCompilerException("Unable to determine result type for binary expression");
        }

        // Set the type of the binary expression
        expr.setType(resultType);

        return resultType;
    }


//    @Override
//    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
//        Type leftType = binaryExpr.getLeftExpr().getType();
//        Type rightType = binaryExpr.getRightExpr().getType();
//
//        // For addition, both operands should be numbers (assuming addition is for integers here)
//        if (binaryExpr.getOpKind().equals(Kind.PLUS)) {  // Adjust based on your enum values and how you identify operations
//            if (!leftType.equals(Type.INT) || !rightType.equals(Type.INT)) {
//                throw new PLCCompilerException("Type mismatch in binary expression.");
//            }
//        }
//        // Handle other operations similarly
//
//        // Return the resulting type of the binary operation. For simplicity, assuming it's the type of either operand
//        return leftType;
//    }



    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCCompilerException {
        Expr e = unaryExpr.getExpr();
        if (unaryExpr.getOp() == Kind.BANG && e.getType() != Type.BOOLEAN) {
            throw new PLCCompilerException("Invalid type for unary negation. Expected BOOLEAN but found " + e.getType());
        }
        // Set type for unaryExpr
        unaryExpr.setType(e.getType());
        return unaryExpr.getType();
    }


    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        // Visit the NameDef part of the declaration to set its type
        declaration.getNameDef().visit(this, arg);

        // If there's an initializer (initial value) associated with the declaration, visit it
        if (declaration.getInitializer() != null) {
            Type exprType = (Type) declaration.getInitializer().visit(this, arg);

            // Check if the expression type matches the declaration type or if there's any allowed type conversion
            if (exprType != declaration.getNameDef().getType() &&
                    !(exprType == Type.STRING && declaration.getNameDef().getType() == Type.IMAGE)) {
                throw new PLCCompilerException("Type mismatch in declaration");
            }
        }

        return declaration.getNameDef().getType();
    }



    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        LValue lValue = assignmentStatement.getlValue();
        String varName = lValue.toString();  // You might have to adjust this based on how you retrieve the variable name from LValue

        Symbol symbol = symbolTable.lookup(varName);
        if (symbol == null) {
            throw new PLCCompilerException("Variable " + varName + " not declared.");
        }

        // Check the type of the expression being assigned and compare with the variable's type
        Type exprType = assignmentStatement.getE().getType();
        if (!exprType.equals(Type.valueOf(symbol.getType()))) {  // Assuming the type in symbol table is stored as string
            throw new PLCCompilerException("Type mismatch in assignment.");
        }
        return null;
    }





    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        // Since StatementBlock contains a Block, simply visit the block for type-checking
        statementBlock.getBlock().visit(this, arg);
        return null;  // Adjust as per your logic
    }


    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        // Check the type of xExpr and yExpr
        Type xType = pixelSelector.xExpr().getType();
        Type yType = pixelSelector.yExpr().getType();

        // Ensure both are of type INT
        if (xType != Type.INT || yType != Type.INT) {
            throw new PLCCompilerException("PixelSelector coordinates should be of type INT.");
        }

        return null;  // Adjust as per your logic
    }

    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        Kind colorKind = channelSelector.color();

        // Check if the color is one of the expected values
        if (colorKind != Kind.RES_red && colorKind != Kind.RES_green && colorKind != Kind.RES_blue) {
            throw new PLCCompilerException("Invalid color channel: " + colorKind);
        }

        return null;  // Adjust as per your logic
    }



    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCCompilerException {
        Type guardType = conditionalExpr.getGuardExpr().getType();
        Type trueExprType = conditionalExpr.getTrueExpr().getType();
        Type falseExprType = conditionalExpr.getFalseExpr().getType();

        // Check guard type
        if (guardType != Type.BOOLEAN) {
            throw new PLCCompilerException("Guard expression in conditional must be of type BOOLEAN");
        }

        // Check that the true and false expressions have the same type
        if (trueExprType != falseExprType) {
            throw new PLCCompilerException("True and false expressions in conditional must have the same type");
        }

        // Assuming the type of the whole conditional expression is the type of its true/false branches
        conditionalExpr.setType(trueExprType);

        return null;  // Adjust as per your logic
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type widthType = dimension.getWidth().getType();
        Type heightType = dimension.getHeight().getType();

        // Check width and height types
        if (widthType != Type.INT || heightType != Type.INT) {
            throw new PLCCompilerException("Width and height in dimension must be of type INT");
        }

        return null;  // Adjust as per your logic
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        for (GuardedBlock gBlock : doStatement.getGuardedBlocks()) {
            Type guardType = gBlock.getGuard().getType(); // Assuming GuardedBlock has a method getGuard() that returns the guard expression
            if (guardType != Type.BOOLEAN) {
                throw new PLCCompilerException("Guard expression in DoStatement must be of type BOOLEAN");
            }
            // If there's more logic to check inside GuardedBlock, we can do it here.
        }
        return null;  // Adjust as per your logic
    }


    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCCompilerException {
        Type redType = expandedPixelExpr.getRed().getType();
        Type greenType = expandedPixelExpr.getGreen().getType();
        Type blueType = expandedPixelExpr.getBlue().getType();

        if (redType != Type.INT || greenType != Type.INT || blueType != Type.INT) {
            throw new PLCCompilerException("Red, Green, and Blue expressions in ExpandedPixelExpr must be of type INT");
        }

        // Assuming the type of the whole ExpandedPixelExpr is PIXEL (if there is such a type)
        expandedPixelExpr.setType(Type.PIXEL);

        return null;  // Adjust as per your logic
    }


    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        // Using the getName() method from IdentExpr to get the name of the variable.
        String varName = identExpr.getName();

        Symbol symbol = symbolTable.lookup(varName);

        if (symbol == null) {
            throw new PLCCompilerException("Variable " + varName + " not declared in current scope.");
        }

        // Continue type checking using the type from the symbol
        String varType = symbol.getType();  // Replace with actual method name from Symbol class
        return null;  // Adjust as per your logic
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        Type guardType = guardedBlock.getGuard().getType();
        if (guardType != Type.BOOLEAN) {
            throw new PLCCompilerException("Guard expression in GuardedBlock must be of type BOOLEAN");
        }
        // If there's more logic to check inside the block of the GuardedBlock, we can do it here.
        return null;  // Adjust as per your logic
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        for (GuardedBlock gBlock : ifStatement.getGuardedBlocks()) {
            Type guardType = gBlock.getGuard().getType();
            if (guardType != Type.BOOLEAN) {
                throw new PLCCompilerException("Guard expression in IfStatement's GuardedBlock must be of type BOOLEAN");
            }
            // If there's more logic to check inside each GuardedBlock, we can do it here.
        }
        return null;  // Adjust as per your logic
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        if (lValue.getNameDef() == null) {
            throw new PLCCompilerException("LValue refers to an undefined name.");
        }
        Type lValueType = lValue.getNameDef().getType();
        // Further checks can be done based on the type, especially if pixelSelector or channelSelector are involved.
        return null;  // Adjust as per your logic
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCCompilerException {
        // Ensure typeToken refers to a valid type
        Type nameDefType = nameDef.getType();
        if (nameDefType == Type.IMAGE && nameDef.getDimension() != null) {
            // Ensure that dimensions are integers
            // This is just a placeholder; you'd need to further check the dimension expressions for their types.
        }
        return null;  // Adjust as per your logic
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCCompilerException {
        // If there are specific constraints on the numeric value, check them here.
        // Otherwise, there might not be much to check since it's inherently a numeric type.
        return null;  // Adjust as per your logic
    }


    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        return null;
    }


    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Expr returnedExpr = returnStatement.getE();
        Type returnedType = returnedExpr.getType();

        // Peek at the top of the stack for expected return type
        Type expectedReturnType = returnTypeStack.peek();

        if (returnedType != expectedReturnType) {
            throw new PLCCompilerException("Mismatched return type. Expected " + expectedReturnType + " but found " + returnedType);
        }
        return null;
    }


//    @Override
//    public Object visitProgram(Program program, Object arg) throws PLCCompilerException {
//        Type programType = program.getType();
//        if (programType == null) {
//            throw new PLCCompilerException("Program lacks a return type.");
//        }
//
//        // Check each parameter in the program
//        for (NameDef param : program.getParams()) {
//            Type paramType = param.getType();
//            // Further checks on each parameter's type can be done here.
//        }
//
//        // Type check the block
//        // This is a placeholder; you'd need to further check the block's statements for their types.
//        return null;  // Adjust as per your logic
//    }
//
//    @Override
//    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
//        Expr returnedExpr = returnStatement.getE();
//        Type returnedType = returnedExpr.getType(); // Assuming every expression has a getType() method
//
//        // This is a hypothetical method to fetch the expected return type
//        // You will have to replace it with actual logic to determine the enclosing function's return type
//        Type expectedReturnType = getCurrentFunctionReturnType();
//
//        if (returnedType != expectedReturnType) {
//            throw new PLCCompilerException("Mismatched return type. Expected " + expectedReturnType + " but found " + returnedType);
//        }
//        return null;
//    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCCompilerException {
        String value = stringLitExpr.getText();
            // If there are specific constraints on the string value, check them here.
            // For instance, let's assume there's a maximum length of 500 characters for strings:
        if (value.length() > 500) {
            throw new PLCCompilerException("String literal exceeds maximum length of 500 characters.");
        }

        // Set type for stringLitExpr
        stringLitExpr.setType(Type.STRING); // Assuming you have a Type.STRING for string literals
        return Type.STRING;
    }



    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        Expr exprToPrint = writeStatement.getExpr();

        // Check if the expression has been type-checked
        if (exprToPrint.getType() == null) {
            throw new PLCCompilerException("Type of the expression in WriteStatement has not been determined.");
        }

        // Disallow printing of VOID and PIXEL types as an example
        if (exprToPrint.getType() == Type.VOID || exprToPrint.getType() == Type.PIXEL) {
            throw new PLCCompilerException("Cannot print expressions of type " + exprToPrint.getType());
        }

        // If there are other constraints or checks you'd like to add, place them here.

        return null;  // If everything is fine, return null or adjust as per your logic.
    }



    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        String value = booleanLitExpr.getText();
        if (!value.equals("true") && !value.equals("false")) {
            throw new PLCCompilerException("Invalid boolean literal value: " + value);
        }
        // Set type for booleanLitExpr
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        String value = constExpr.getName();
        Type type;
        if (value.matches("^\\d+$")) { // If the value is a number
            type = Type.INT;
        } else if (value.startsWith("\"") && value.endsWith("\"")) { // If the value is a string
            type = Type.STRING;
        } else {
            throw new PLCCompilerException("Unknown constant type for value: " + value);
        }
        // Set type for constExpr
        constExpr.setType(type);
        return type;
    }


}
