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
        // Push the program's return type onto the returnTypeStack
        returnTypeStack.push(program.getType());

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

        // Pop the program's return type from the returnTypeStack
        returnTypeStack.pop();

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
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
        Expr left = binaryExpr.getLeftExpr();
        Expr right = binaryExpr.getRightExpr();

        Type leftType = (Type) left.visit(this, arg);
        Type rightType = (Type) right.visit(this, arg);

        // Check if the types of the left and right expressions are compatible for the operation
        switch (binaryExpr.getOpKind()) {
            case PLUS:
            case MINUS:
            case TIMES:
            case DIV:
                if (leftType == Type.INT && rightType == Type.INT) {
                    binaryExpr.setType(Type.INT);
                    return Type.INT;
                }
                break;
            // Add other binary operations and their type-checking logic here...
            default:
                throw new PLCCompilerException("Unsupported binary operation: " + binaryExpr.getOpKind());
        }

        throw new PLCCompilerException("Unable to determine result type for binary expression");
    }



    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) throws PLCCompilerException {
        // Visit the operand to get its type
        Type operandType = (Type) expr.getExpr().visit(this, arg);

        // Infer the resulting type based on the unary operator and operand type
        Type resultType;
        switch (expr.getOp()) {
            case MINUS:
                // For negation
                if (operandType == Type.INT) {
                    resultType = Type.INT;
                } else {
                    throw new PLCCompilerException("Invalid operand type for unary negation");
                }
                break;
            case BANG:
                // For logical NOT
                if (operandType == Type.BOOLEAN) {
                    resultType = Type.BOOLEAN;
                } else {
                    throw new PLCCompilerException("Invalid operand type for unary NOT");
                }
                break;
            // Handle other unary operators like width, height, etc.
            // ...
            default:
                throw new PLCCompilerException("Unrecognized unary operator");
        }

        // Set the type of the unary expression
        expr.setType(resultType);

        return resultType;
    }

    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
        Type type = (Type) postfixExpr.primary().visit(this, arg);
        if (postfixExpr.pixel() != null) {
            postfixExpr.pixel().visit(this, arg);
        }
        if (postfixExpr.channel() != null) {
            postfixExpr.channel().visit(this, arg);
        }
        // Check if primary type is IMAGE, PixelSelector is not null, and ChannelSelector is not null
        if (type == Type.IMAGE && postfixExpr.pixel() != null && postfixExpr.channel() != null) {
            type = Type.INT;
        }
        postfixExpr.setType(type);
        return type;
    }



    @Override
    public Object visitConditionalExpr(ConditionalExpr expr, Object arg) throws PLCCompilerException {
        // Ensure the guard expression evaluates to a boolean type
        Type guardType = (Type) expr.getGuardExpr().visit(this, arg);
        if (guardType != Type.BOOLEAN) {
            throw new PLCCompilerException("Guard expression in a conditional must evaluate to a BOOLEAN type");
        }

        // Visit the true and false expressions to get their types
        Type trueType = (Type) expr.getTrueExpr().visit(this, arg);
        Type falseType = (Type) expr.getFalseExpr().visit(this, arg);

        // Ensure the types of trueExpr and falseExpr are the same or compatible
        if (trueType != falseType) {
            throw new PLCCompilerException("The types of the true and false expressions in a conditional must be the same");
        }

        // Set the type of the conditional expression
        expr.setType(trueType);

        return trueType;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr expr, Object arg) throws PLCCompilerException {
        // Set the type of the StringLitExpr to STRING since string literals are of string type
        expr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr expr, Object arg) throws PLCCompilerException {
        System.out.println("Debug: Inside visitNumLitExpr");  // Debug statement

        expr.setType(Type.INT);

        System.out.println("Debug: NumLitExpr type set to: " + expr.getType());  // Debug statement

        return Type.INT;
    }




    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        Symbol symbol = symbolTable.lookup(identExpr.getName());
        if (symbol == null) {
            throw new PLCCompilerException("Variable " + identExpr.getName() + " not declared in current scope.");
        }

        // Set the type of the IdentExpr based on the type of the Symbol.
        // Convert the string representation of type from Symbol to the Type enum.
        Type varType = Type.valueOf(symbol.getType().toUpperCase());
        identExpr.setType(varType);

        return varType;
    }



    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        // Set the type of the BooleanLitExpr to BOOLEAN
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }

    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        // Determine the type based on the specific constant value
        if ("Z".equals(constExpr.getName())) {
            constExpr.setType(Type.INT);
        } else {
            constExpr.setType(Type.PIXEL);
        }
        return constExpr.getType();
    }


    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expr, Object arg) throws PLCCompilerException {
        Type redType = (Type) expr.getRed().visit(this, arg);
        Type greenType = (Type) expr.getGreen().visit(this, arg);
        Type blueType = (Type) expr.getBlue().visit(this, arg);

        if (redType != Type.INT || greenType != Type.INT || blueType != Type.INT) {
            throw new PLCCompilerException("All components of an ExpandedPixelExpr must be of type INT");
        }

        // Return the type of the expression
        return Type.INT; // or whatever the appropriate type is for this expression
    }


    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        Type xType = (Type) pixelSelector.xExpr().visit(this, arg);
        Type yType = (Type) pixelSelector.yExpr().visit(this, arg);

        if (xType != Type.INT || yType != Type.INT) {
            throw new PLCCompilerException("PixelSelector coordinates should be of type INT.");
        }

        return Type.IMAGE;  // Pixel selector returns a pixel, which is part of an image.
    }


    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        Kind colorKind = channelSelector.color();

        if (colorKind != Kind.RES_red && colorKind != Kind.RES_green && colorKind != Kind.RES_blue) {
            throw new PLCCompilerException("Invalid color channel: " + colorKind);
        }

        return Type.INT;  // Channel selector returns an integer value for the specific color channel.
    }



    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
        // If the declaration has an initializer, visit it first
        if (declaration.getInitializer() != null) {
            Type exprType = (Type) declaration.getInitializer().visit(this, arg);
            declaration.getNameDef().getType(); // Access the type of the NameDef
            // Set the type of the NameDef directly
//            declaration.getNameDef().getType() = exprType;
        }

        // Visit the NameDef part of the declaration to set its type
        NameDef nameDef = declaration.getNameDef();
        nameDef.visit(this, arg);

        // Check for re-declarations in the current scope
        if (symbolTable.lookup(nameDef.getName()) != null) {
            throw new PLCCompilerException("Variable " + nameDef.getName() + " already declared in the current scope.");
        }

        // Add the variable to the symbol table
        symbolTable.insert(nameDef.getName(), nameDef.getType().toString());

        // If the declaration has an initializer, check its type
        if (declaration.getInitializer() != null) {
            Type exprType = (Type) declaration.getInitializer().visit(this, arg);
            if (exprType != nameDef.getType()) {
                throw new PLCCompilerException("Type mismatch in declaration of " + nameDef.getName());
            }
        }

        return declaration.getNameDef().getType(); // Return the type of the declaration
    }



    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        LValue lValue = assignmentStatement.getlValue();
        String varName = lValue.getName();  // Corrected from lValue.toString()

        Symbol symbol = symbolTable.lookup(varName);
        if (symbol == null) {
            throw new PLCCompilerException("Variable " + varName + " not declared.");
        }

        // Visit the expression on the right-hand side
        Type exprType = (Type) assignmentStatement.getE().visit(this, arg);

        // Check the type
        if (!exprType.equals(Type.valueOf(symbol.getType()))) {
            throw new PLCCompilerException("Type mismatch in assignment.");
        }

        return exprType;  // Return the type of the expression
    }




    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        // Since StatementBlock contains a Block, simply visit the block for type-checking
        statementBlock.getBlock().visit(this, arg);
        return null;  // Adjust as per your logic
    }





    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        System.out.println("Debug: Visiting Dimension");  // Debug statement

        Type widthType = (Type) dimension.getWidth().visit(this, arg);
        Type heightType = (Type) dimension.getHeight().visit(this, arg);

        // Check width and height types
        if (widthType != Type.INT || heightType != Type.INT) {
            throw new PLCCompilerException("Width and height in dimension must be of type INT");
        }

        return Type.INT;  // Return the type as INT
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
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Expr returnedExpr = returnStatement.getE();
        Type returnedType = returnedExpr.getType();

        if (returnTypeStack.isEmpty()) {
            throw new PLCCompilerException("Unexpected return statement outside of function or method scope.");
        }

        // Peek at the top of the stack for expected return type
        Type expectedReturnType = returnTypeStack.peek();

        if (returnedType != expectedReturnType) {
            throw new PLCCompilerException("Mismatched return type. Expected " + expectedReturnType + " but found " + returnedType);
        }
        returnedExpr.visit(this, arg);
        return returnedType;
    }



    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        Expr expr = writeStatement.getExpr();
        Type exprType = (Type) expr.visit(this, arg); // Ensure that the expression's type is determined
        if (exprType == null) {
            throw new PLCCompilerException("Type of the expression in WriteStatement has not been determined.");
        }
        return exprType; // Return the determined type
    }





}
