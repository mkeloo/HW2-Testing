package edu.ufl.cise.cop4020fa23;

import java.util.Stack;
import java.util.HashMap;
import java.util.Map;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.*;
import edu.ufl.cise.cop4020fa23.SymbolTable;

public class TypeCheckVisitor implements ASTVisitor {

    private SymbolTable symbolTable;

    private Map<NumLitExpr, Type> numLitExprMap = new HashMap<>();


    Type currentReturnType = null;


    // Stack to store return types of functions/programs
    private Stack<Type> returnTypeStack = new Stack<>();


    public TypeCheckVisitor() throws TypeCheckException {
        this.symbolTable = new SymbolTable();
    }


    @Override
    public Object visitProgram(Program program, Object arg) throws TypeCheckException, PLCCompilerException {
        // Convert typeToken to Type
        String returnTypeString = program.getTypeToken().text();
        Type returnType = Type.valueOf(returnTypeString.toUpperCase());

        // Push the return type onto the returnTypeStack
        returnTypeStack.push(returnType);

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

        // Leave the program's scope using leaveScope method
        symbolTable.leaveScope();

        // Pop the return type from the returnTypeStack
        returnTypeStack.pop();

        return program.getType();
    }



    @Override
    public Object visitBlock(Block block, Object arg) throws TypeCheckException, PLCCompilerException {
        System.out.println("Entering scope");

        // Enter a new scope for the block
        symbolTable.enterScope();

        // Visit each element in the block
        for (Block.BlockElem elem : block.getElems()) {
            elem.visit(this, arg);
        }

        System.out.println("Leaving scope");

        // Leave the block's scope
        symbolTable.leaveScope();

        return null;
    }



    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws TypeCheckException {
        // Ensure typeToken refers to a valid type
        Type nameDefType = nameDef.getType();

        if (nameDefType == Type.IMAGE && nameDef.getDimension() != null) {
            // Ensure that dimensions are integers
            // This is just a placeholder; you'd need to further check the dimension expressions for their types.
        } else if (nameDefType != Type.INT && nameDefType != Type.BOOLEAN &&
                nameDefType != Type.STRING && nameDefType != Type.PIXEL &&
                nameDefType != Type.IMAGE) {
            throw new TypeCheckException("Invalid type for NameDef: " + nameDefType);
        }

        // Insert nameDef into symbolTable
        symbolTable.insert(nameDef);

        return nameDef;  // Adjust as per your logic
    }




    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException, TypeCheckException {
        System.out.println("Visiting Declaration: " + declaration.getNameDef().getName());

        // Visit the initializer first, if it exists
        Expr initializer = declaration.getInitializer();
        Type initType = null;
        if (initializer != null) {
            initType = (Type) initializer.visit(this, arg);
        }

        // Visit the NameDef part of the declaration to set and get its type
        NameDef nameDef = declaration.getNameDef();
        nameDef.visit(this, arg);
        Type declaredType = nameDef.getType();

        // Handle dimensioned declarations
        Dimension dimension = nameDef.getDimension();
        if (dimension != null) {
            // Visit the dimension
            dimension.visit(this, arg);
        }

        // If an initializer is present, check the types
        if (initializer != null) {
            if (initializer instanceof PostfixExpr) {
                PostfixExpr postfixExpr = (PostfixExpr) initializer;
                if (postfixExpr.channel() != null) {
                    initType = Type.INT; // Channel selector results in an int value
                }
            }
            if (initType != declaredType && !(initType == Type.STRING && declaredType == Type.IMAGE)) {
                throw new TypeCheckException("Type mismatch in declaration: expected " + declaredType + ", found " + initType);
            }
        }

        // Insert the NameDef into the symbol table
        symbolTable.insert(nameDef);

        return declaredType;
    }




    @Override
    public Object visitConditionalExpr(ConditionalExpr expr, Object arg) throws TypeCheckException, PLCCompilerException {
        // Ensure the guard expression evaluates to a boolean type
        Type guardType = (Type) expr.getGuardExpr().visit(this, arg);
        if (guardType != Type.BOOLEAN) {
            throw new TypeCheckException("Guard expression in a conditional must evaluate to a BOOLEAN type");
        }

        // Visit the true and false expressions to get their types
        Type trueType = (Type) expr.getTrueExpr().visit(this, arg);
        Type falseType = (Type) expr.getFalseExpr().visit(this, arg);

        // Ensure the types of trueExpr and falseExpr are the same
        if (trueType != falseType) {
            throw new TypeCheckException("The types of the true and false expressions in a conditional must be the same");
        }

        // Set the type of the conditional expression
        expr.setType(trueType);

        return trueType;
    }



    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws TypeCheckException, PLCCompilerException {
        Expr left = binaryExpr.getLeftExpr();
        Expr right = binaryExpr.getRightExpr();

        Type leftType = (Type) left.visit(this, arg);
        Type rightType = (Type) right.visit(this, arg);

        switch (binaryExpr.getOpKind()) {
            case PLUS:
                if (leftType == rightType) {
                    binaryExpr.setType(leftType);
                    return leftType;
                }
                break;
            case MINUS:
            case TIMES:
            case DIV:
                if ((leftType == Type.INT || leftType == Type.PIXEL || leftType == Type.IMAGE) && leftType == rightType) {
                    binaryExpr.setType(leftType);
                    return leftType;
                } else if ((leftType == Type.PIXEL || leftType == Type.IMAGE) && rightType == Type.INT) {
                    binaryExpr.setType(leftType);
                    return leftType;
                }
                break;
            case BITAND:
            case BITOR:
                if (leftType == Type.PIXEL && rightType == Type.PIXEL) {
                    binaryExpr.setType(Type.PIXEL);
                    return Type.PIXEL;
                }
                break;
            case AND:
            case OR:
                if (leftType == Type.BOOLEAN && rightType == Type.BOOLEAN) {
                    binaryExpr.setType(Type.BOOLEAN);
                    return Type.BOOLEAN;
                }
                break;
            case LT:
            case GT:
            case LE:
            case GE:
                if (leftType == Type.INT && rightType == Type.INT) {
                    binaryExpr.setType(Type.BOOLEAN);
                    return Type.BOOLEAN;
                }
                break;
            case EQ:
                if (leftType == rightType) {
                    binaryExpr.setType(Type.BOOLEAN);
                    return Type.BOOLEAN;
                }
                break;
            case EXP:
                if (leftType == Type.INT && rightType == Type.INT) {
                    binaryExpr.setType(Type.INT);
                    return Type.INT;
                } else if (leftType == Type.PIXEL && rightType == Type.INT) {
                    binaryExpr.setType(Type.PIXEL);
                    return Type.PIXEL;
                }
                break;
            default:
                throw new TypeCheckException("Unsupported binary operation: " + binaryExpr.getOpKind());
        }

        throw new TypeCheckException("Mismatched types in binary expression: " + leftType + ", " + rightType);
    }



    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) throws TypeCheckException, PLCCompilerException {
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
                    throw new TypeCheckException("Invalid operand type for unary negation");
                }
                break;
            case BANG:
                // For logical NOT
                if (operandType == Type.BOOLEAN) {
                    resultType = Type.BOOLEAN;
                } else {
                    throw new TypeCheckException("Invalid operand type for unary NOT");
                }
                break;
            case RES_width:
            case RES_height:
                // For image width and height
                if (operandType == Type.IMAGE) {
                    resultType = Type.INT;
                } else {
                    throw new TypeCheckException("Invalid operand type for unary " + expr.getOp());
                }
                break;
            default:
                throw new TypeCheckException("Unrecognized unary operator");
        }

        // Set the type of the unary expression
        expr.setType(resultType);

        return resultType;
    }




    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws TypeCheckException, PLCCompilerException {
        Type primaryType = (Type) postfixExpr.primary().visit(this, arg);
        PixelSelector pixelSelector = postfixExpr.pixel();
        ChannelSelector channelSelector = postfixExpr.channel();

        // Check conditions based on the table for inferPostfixExprType
        if (pixelSelector == null && channelSelector == null) {
            postfixExpr.setType(primaryType);
        } else if (primaryType == Type.IMAGE && pixelSelector != null && channelSelector == null) {
            postfixExpr.setType(Type.PIXEL);
            pixelSelector.visit(this, arg);
        } else if (primaryType == Type.IMAGE && pixelSelector != null && channelSelector != null) {
            postfixExpr.setType(Type.INT);
            pixelSelector.visit(this, arg);
            channelSelector.visit(this, arg);
        } else if (primaryType == Type.IMAGE && pixelSelector == null && channelSelector != null) {
            postfixExpr.setType(Type.IMAGE);
            channelSelector.visit(this, arg);
        } else if (primaryType == Type.PIXEL && pixelSelector == null && channelSelector != null) {
            postfixExpr.setType(Type.INT);
            channelSelector.visit(this, arg);
        } else {
            throw new TypeCheckException("Invalid combination in PostfixExpr.");
        }

//        return postfixExpr.getType();
        return primaryType;
    }




    @Override
    public Object visitStringLitExpr(StringLitExpr expr, Object arg) throws TypeCheckException, PLCCompilerException {
        System.out.println("Visiting NumLitExpr with value: " + expr.getText()); // assuming a method called getValue() exists
        Type type = Type.STRING;
        expr.setType(Type.STRING);
        return type;
    }




    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws TypeCheckException, PLCCompilerException {
        System.out.println("Visiting NumLitExpr with value: " + numLitExpr.getText());
        Type type = Type.INT;
        numLitExpr.setType(type);
        System.out.println("After setting, NumLitExpr type: " + numLitExpr.getType());
        System.out.println("NumLitExpr hash code: " + System.identityHashCode(numLitExpr));

        // Additional Debug Statements
        System.out.println("Before returning from visitNumLitExpr, NumLitExpr type: " + numLitExpr.getType());
        System.out.println("Before returning from visitNumLitExpr, NumLitExpr hash code: " + System.identityHashCode(numLitExpr));

        return type;
    }




    public static final String IN_PIXEL_EXPRESSION_CONTEXT = "inPixelExpressionContext";

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws TypeCheckException, PLCCompilerException {
        String name = identExpr.getName();
        NameDef nameDef = symbolTable.lookup(name);

        if (nameDef == null) {
            // If the variable is not declared but used in the context of a pixel selector or pixel expression,
            // create a synthetic variable and add it to the symbol table
            if (IN_LVALUE_CONTEXT.equals(arg)) {
                SyntheticNameDef syntheticNameDef = new SyntheticNameDef(name);
                symbolTable.insert(syntheticNameDef);
                identExpr.setType(Type.INT);  // Assuming coordinates are of type INT
                return Type.INT;
            } else {
                throw new TypeCheckException("Variable " + name + " not declared in current scope.");
            }
        }

        identExpr.setType(nameDef.getType());
        return nameDef.getType();
    }




    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws TypeCheckException, PLCCompilerException {
        System.out.println("Visiting BooleanLitExpr: " + booleanLitExpr + ", setting type to BOOLEAN");
        booleanLitExpr.setType(Type.BOOLEAN);
        System.out.println("After setting, BooleanLitExpr type: " + booleanLitExpr.getType());
        System.out.println("BooleanLitExpr hash code: " + System.identityHashCode(booleanLitExpr));
        return Type.BOOLEAN;
    }




    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws TypeCheckException, PLCCompilerException {
        if ("Z".equals(constExpr.getName())) {
            constExpr.setType(Type.INT);
        } else {
            constExpr.setType(Type.PIXEL);
        }
        return constExpr.getType();
    }



    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws TypeCheckException, PLCCompilerException {
        Kind colorKind = channelSelector.color();

        if (colorKind != Kind.RES_red && colorKind != Kind.RES_green && colorKind != Kind.RES_blue) {
            throw new TypeCheckException("Invalid color channel: " + colorKind);
        }

        return Type.INT;  // Channel selector returns an integer value for the specific color channel.
    }



    private static final String IN_LVALUE_CONTEXT = "IN_LVALUE_CONTEXT";


    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws TypeCheckException, PLCCompilerException {
        Expr xExpr = pixelSelector.xExpr();
        Expr yExpr = pixelSelector.yExpr();

        // Check if xExpr and yExpr are INT type
        Type xType = (Type) xExpr.visit(this, arg);
        Type yType = (Type) yExpr.visit(this, arg);

        if (xType != Type.INT || yType != Type.INT) {
            throw new TypeCheckException("PixelSelector coordinates should be of type INT.");
        }

        // If PixelSelector is in the context of an LValue
        if (IN_LVALUE_CONTEXT.equals(arg)) {
            // Handle conditions for IdentExpr
            if (xExpr instanceof IdentExpr) {
                IdentExpr xIdentExpr = (IdentExpr) xExpr;
                if (symbolTable.lookup(xIdentExpr.getName()) == null) {
                    SyntheticNameDef syntheticNameDef = new SyntheticNameDef(xIdentExpr.getName());
                    symbolTable.enterScope();
                    symbolTable.insert(syntheticNameDef);
                    symbolTable.leaveScope();
                }
            }

            if (yExpr instanceof IdentExpr) {
                IdentExpr yIdentExpr = (IdentExpr) yExpr;
                if (symbolTable.lookup(yIdentExpr.getName()) == null) {
                    SyntheticNameDef syntheticNameDef = new SyntheticNameDef(yIdentExpr.getName());
                    symbolTable.enterScope();
                    symbolTable.insert(syntheticNameDef);
                    symbolTable.leaveScope();
                }
            }
        }

        return Type.IMAGE;
    }




    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expr, Object arg) throws TypeCheckException, PLCCompilerException {
        Type redType = (Type) expr.getRed().visit(this, arg);
        Type greenType = (Type) expr.getGreen().visit(this, arg);
        Type blueType = (Type) expr.getBlue().visit(this, arg);

        if (redType != Type.INT || greenType != Type.INT || blueType != Type.INT) {
            throw new TypeCheckException("All components of an ExpandedPixelExpr must be of type INT");
        }

        return Type.PIXEL;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws TypeCheckException, PLCCompilerException {
        Type widthType = (Type) dimension.getWidth().visit(this, arg);
        Type heightType = (Type) dimension.getHeight().visit(this, arg);

        if (widthType != Type.INT || heightType != Type.INT) {
            throw new TypeCheckException("Width and height in dimension must be of type INT");
        }

        return Type.INT;  // Or consider changing this based on your language semantics
    }


    @Override
    public Object visitLValue(LValue lValue, Object arg) throws TypeCheckException, PLCCompilerException {
        NameDef nameDef = lValue.getNameDef();

        if (nameDef == null) {
            nameDef = symbolTable.lookup(lValue.getName());
            if (nameDef == null) {
                throw new TypeCheckException("LValue refers to an undefined name: " + lValue.getName());
            }
        }

        Type varType = nameDef.getType();  // corresponds to LValue.varType

        PixelSelector pixelSelector = lValue.getPixelSelector();  // Assuming you have such a method
        ChannelSelector channelSelector = lValue.getChannelSelector();  // Assuming you have such a method

        if (pixelSelector != null && varType != Type.IMAGE) {
            throw new TypeCheckException("PixelSelector present, but LValue varType is not IMAGE. Found: " + varType);
        }

        if (channelSelector != null && (varType != Type.PIXEL && varType != Type.IMAGE)) {
            throw new TypeCheckException("ChannelSelector present, but LValue varType is not PIXEL or IMAGE. Found: " + varType);
        }

        // Check conditions based on the table for inferLValueType
        if (pixelSelector == null && channelSelector == null) {
            lValue.setType(varType);
        } else if (varType == Type.IMAGE && pixelSelector != null && channelSelector == null) {
            lValue.setType(Type.PIXEL);
            if (arg instanceof LValue) {
                symbolTable.enterScope();
                symbolTable.insert(new SyntheticNameDef("x"));
                symbolTable.insert(new SyntheticNameDef("y"));
                pixelSelector.visit(this, arg);
                symbolTable.leaveScope();
            } else {
                pixelSelector.visit(this, arg);
            }
        } else if (varType == Type.IMAGE && pixelSelector != null && channelSelector != null) {
            lValue.setType(Type.INT);
            pixelSelector.visit(this, arg);
            channelSelector.visit(this, arg);
        } else if (varType == Type.IMAGE && pixelSelector == null && channelSelector != null) {
            lValue.setType(Type.INT);  // According to the table, it should be INT not IMAGE
            channelSelector.visit(this, arg);
        } else if (varType == Type.PIXEL && pixelSelector == null && channelSelector != null) {
            lValue.setType(Type.INT);
            channelSelector.visit(this, arg);
        } else {
            throw new TypeCheckException("Invalid combination in LValue.");
        }

        return lValue.getType();
    }



    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws TypeCheckException, PLCCompilerException {
        System.out.println("Visiting Assignment Statement: " + assignmentStatement.getlValue().getName());

        LValue lValue = assignmentStatement.getlValue();
        Type lValueType;

        // Enter a new scope for the assignment statement
        symbolTable.enterScope();

        // Check if the LValue is in the context of a PixelSelector
        if (lValue.getPixelSelector() != null) {
            lValueType = (Type) lValue.visit(this, IN_LVALUE_CONTEXT);
        } else {
            lValueType = (Type) lValue.visit(this, arg);
        }

        // Visit the expression on the right-hand side
        Type exprType = (Type) assignmentStatement.getE().visit(this, arg);

        // Leave the scope of the assignment statement
        symbolTable.leaveScope();

        // Check compatibility based on the provided table
        if (!(lValueType == exprType
                || (lValueType == Type.PIXEL && exprType == Type.INT)
                || (lValueType == Type.IMAGE && (exprType == Type.PIXEL || exprType == Type.INT || exprType == Type.STRING)))) {
            throw new TypeCheckException("Type mismatch in assignment. LValue type: " + lValueType + ", Expr type: " + exprType);
        }

        return exprType;  // Return the type of the expression
    }



    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws TypeCheckException, PLCCompilerException {
        Expr expr = writeStatement.getExpr();
        Type exprType = (Type) expr.visit(this, arg);
        if (exprType == null) {
            throw new TypeCheckException("Type of the expression in WriteStatement has not been determined.");
        }
        return exprType;
    }


    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws TypeCheckException, PLCCompilerException {
        symbolTable.enterScope();
        try {
            for (GuardedBlock gBlock : doStatement.getGuardedBlocks()) {
                Type guardType = (Type) gBlock.getGuard().visit(this, arg);
                if (guardType != Type.BOOLEAN) {
                    throw new TypeCheckException("Guard expression in DoStatement must be of type BOOLEAN");
                }
                gBlock.getBlock().visit(this, arg);
            }
        } finally {
            symbolTable.leaveScope();
        }
        return doStatement;
    }


    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws TypeCheckException, PLCCompilerException {
        for (GuardedBlock gBlock : ifStatement.getGuardedBlocks()) {
            Type guardType = (Type) gBlock.getGuard().visit(this, arg);
            if (guardType != Type.BOOLEAN) {
                throw new TypeCheckException("Guard expression in IfStatement's GuardedBlock must be of type BOOLEAN");
            }
        }
        return ifStatement;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws TypeCheckException, PLCCompilerException {
        Type guardType = (Type) guardedBlock.getGuard().visit(this, arg);
        if (guardType != Type.BOOLEAN) {
            throw new TypeCheckException("Guard expression in GuardedBlock must be of type BOOLEAN");
        }
        return guardType;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws TypeCheckException, PLCCompilerException {
        Expr returnedExpr = returnStatement.getE();
        Type returnedType = (Type) returnedExpr.visit(this, arg);

        if (returnTypeStack.isEmpty()) {
            throw new TypeCheckException("Unexpected return statement outside of function or method scope.");
        }

        // Peek at the top of the stack for expected return type
        Type expectedReturnType = returnTypeStack.peek();

        if (returnedType != expectedReturnType) {
            throw new TypeCheckException("Mismatched return type. Expected " + expectedReturnType + " but found " + returnedType);
        }

        return returnedType;
    }



    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws TypeCheckException, PLCCompilerException {
        // Visit the block for type-checking
        statementBlock.getBlock().visit(this, arg);
        return statementBlock;
    }



}
