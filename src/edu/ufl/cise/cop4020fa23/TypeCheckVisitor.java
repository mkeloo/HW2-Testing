package edu.ufl.cise.cop4020fa23;
import java.util.Stack;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.*;
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
        if (!symbolTable.insert(nameDef.getName(), nameDef)) {
            throw new TypeCheckException("Name already declared in this scope: " + nameDef.getName());
        }

        return null;  // Adjust as per your logic
    }


//    @Override
//    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {
//
//        // If the declaration has an initializer, visit it and get its type
//        Type initializerType = null;
//        if (declaration.getInitializer() != null) {
//            initializerType = (Type) declaration.getInitializer().visit(this, arg);
//        }
//
//        // Visit the NameDef part of the declaration to set its type
//        NameDef nameDef = declaration.getNameDef();
//        nameDef.visit(this, arg);
//
//        // Ensure type is set for the NameDef
//        Type declaredType = nameDef.getType();
//        if (declaredType == null) {
//            throw new PLCCompilerException("Type not set for variable " + nameDef.getName());
//        }
//
//        // Check for re-declarations in the current scope
//        if (symbolTable.lookup(nameDef.getName()) != null) {
//            throw new PLCCompilerException("Variable " + nameDef.getName() + " already declared in the current scope.");
//        }
//
//        // Add the variable to the symbol table
//        symbolTable.insert(nameDef.getName(), nameDef);
//
//        // If the declaration has an initializer, check its type
//        if (initializerType != null) {
//            if (initializerType != declaredType) {
//                throw new PLCCompilerException("Type mismatch in declaration of " + nameDef.getName());
//            }
//        }
//
//        return declaredType; // Return the type of the declaration
//    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCCompilerException {

        // Visit the NameDef part of the declaration to set its type
        NameDef nameDef = declaration.getNameDef();
        nameDef.visit(this, arg);

        // Ensure type is set for the NameDef
        Type declaredType = nameDef.getType();
        if (declaredType == null) {
            throw new PLCCompilerException("Type not set for variable " + nameDef.getName());
        }

        // If the declaration has an initializer, visit it and get its type
        Type initializerType = null;
        if (declaration.getInitializer() != null) {
            initializerType = (Type) declaration.getInitializer().visit(this, arg);
        }

//        // Check for re-declarations in the current scope
//        if (symbolTable.lookup(nameDef.getName()) != null) {
//            throw new PLCCompilerException("Variable " + nameDef.getName() + " already declared in the current scope.");
//        }

        // If the declaration has an initializer, check its type
        if (initializerType != null) {
            if (initializerType != declaredType) {
                throw new PLCCompilerException("Type mismatch in declaration of " + nameDef.getName());
            }
        }

        // Add the variable to the symbol table
        symbolTable.insert(nameDef.getName(), nameDef);

        return declaredType; // Return the type of the declaration
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
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCCompilerException {
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
                throw new PLCCompilerException("Unsupported binary operation: " + binaryExpr.getOpKind());
        }

        throw new PLCCompilerException("Mismatched types in binary expression: " + leftType + ", " + rightType);
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
            case RES_width:
            case RES_height:
                // For image width and height
                if (operandType == Type.IMAGE) {
                    resultType = Type.INT;
                } else {
                    throw new PLCCompilerException("Invalid operand type for unary " + expr.getOp());
                }
                break;
            default:
                throw new PLCCompilerException("Unrecognized unary operator");
        }

        // Set the type of the unary expression
        expr.setType(resultType);

        return resultType;
    }




    @Override
    public Object visitPostfixExpr(PostfixExpr postfixExpr, Object arg) throws PLCCompilerException {
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
            throw new PLCCompilerException("Invalid combination in PostfixExpr.");
        }

        return postfixExpr.getType();
    }




    @Override
    public Object visitStringLitExpr(StringLitExpr expr, Object arg) throws PLCCompilerException {
        expr.setType(Type.STRING);
        return Type.STRING;
    }


    @Override
    public Object visitNumLitExpr(NumLitExpr expr, Object arg) throws PLCCompilerException {
        System.out.println("Visiting NumLitExpr with value: " + expr.getText()); // assuming a method called getValue() exists
        expr.setType(Type.INT);
        return Type.INT;
    }



    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCCompilerException {
        Symbol symbol = symbolTable.lookup(identExpr.getName());
        if (symbol == null) {
            throw new PLCCompilerException("Variable " + identExpr.getName() + " not declared in current scope.");
        }

        // Get the NameDef from the Symbol object
        NameDef nameDef = symbol.getNameDef();

        // Set the type of the IdentExpr based on the type of the NameDef.
        identExpr.setType(nameDef.getType());

        return identExpr.getType();
    }



    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws PLCCompilerException {
        booleanLitExpr.setType(Type.BOOLEAN);
        return Type.BOOLEAN;
    }


    @Override
    public Object visitConstExpr(ConstExpr constExpr, Object arg) throws PLCCompilerException {
        if ("Z".equals(constExpr.getName())) {
            constExpr.setType(Type.INT);
        } else {
            constExpr.setType(Type.PIXEL);
        }
        return constExpr.getType();
    }



    @Override
    public Object visitChannelSelector(ChannelSelector channelSelector, Object arg) throws PLCCompilerException {
        Kind colorKind = channelSelector.color();

        if (colorKind != Kind.RES_red && colorKind != Kind.RES_green && colorKind != Kind.RES_blue) {
            throw new PLCCompilerException("Invalid color channel: " + colorKind);
        }

        return Type.INT;  // Channel selector returns an integer value for the specific color channel.
    }



    private static final String IN_LVALUE_CONTEXT = "IN_LVALUE_CONTEXT";


    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCCompilerException {
        Expr xExpr = pixelSelector.xExpr();
        Expr yExpr = pixelSelector.yExpr();

        // Check if xExpr and yExpr are INT type
        Type xType = (Type) xExpr.visit(this, arg);
        Type yType = (Type) yExpr.visit(this, arg);

        if (xType != Type.INT || yType != Type.INT) {
            throw new PLCCompilerException("PixelSelector coordinates should be of type INT.");
        }

        // If PixelSelector is in the context of an LValue
        if (IN_LVALUE_CONTEXT.equals(arg)) {
            // Handle conditions for IdentExpr
            if (xExpr instanceof IdentExpr) {
                IdentExpr xIdentExpr = (IdentExpr) xExpr;
                if (symbolTable.lookup(xIdentExpr.getName()) == null) {
                    SyntheticNameDef syntheticNameDef = new SyntheticNameDef(xIdentExpr.getName());
                    symbolTable.insert(syntheticNameDef.getName(), syntheticNameDef);
                }
            }

            if (yExpr instanceof IdentExpr) {
                IdentExpr yIdentExpr = (IdentExpr) yExpr;
                if (symbolTable.lookup(yIdentExpr.getName()) == null) {
                    SyntheticNameDef syntheticNameDef = new SyntheticNameDef(yIdentExpr.getName());
                    symbolTable.insert(syntheticNameDef.getName(), syntheticNameDef);
                }
            }
        }

        return Type.IMAGE;  // Pixel selector returns a pixel, which is part of an image.
    }



    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expr, Object arg) throws PLCCompilerException {
        Type redType = (Type) expr.getRed().visit(this, arg);
        Type greenType = (Type) expr.getGreen().visit(this, arg);
        Type blueType = (Type) expr.getBlue().visit(this, arg);

        if (redType != Type.INT || greenType != Type.INT || blueType != Type.INT) {
            throw new PLCCompilerException("All components of an ExpandedPixelExpr must be of type INT");
        }

        return Type.PIXEL;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCCompilerException {
        Type widthType = (Type) dimension.getWidth().visit(this, arg);
        Type heightType = (Type) dimension.getHeight().visit(this, arg);

        if (widthType != Type.INT || heightType != Type.INT) {
            throw new PLCCompilerException("Width and height in dimension must be of type INT");
        }

        return Type.INT;  // Or consider changing this based on your language semantics
    }



    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCCompilerException {
        NameDef nameDef = lValue.getNameDef();
        if (nameDef == null) {
            throw new PLCCompilerException("LValue refers to an undefined name.");
        }
        Type varType = nameDef.getType();  // corresponds to LValue.varType

        PixelSelector pixelSelector = lValue.getPixelSelector();  // Assuming you have such a method
        ChannelSelector channelSelector = lValue.getChannelSelector();  // Assuming you have such a method

        if (pixelSelector != null && varType != Type.IMAGE) {
            throw new PLCCompilerException("PixelSelector present, but LValue varType is not IMAGE.");
        }

        if (channelSelector != null && (varType != Type.PIXEL && varType != Type.IMAGE)) {
            throw new PLCCompilerException("ChannelSelector present, but LValue varType is not PIXEL or IMAGE.");
        }

        // Check conditions based on the table for inferLValueType
        if (pixelSelector == null && channelSelector == null) {
            lValue.setType(varType);
        } else if (varType == Type.IMAGE && pixelSelector != null && channelSelector == null) {
            lValue.setType(Type.PIXEL);
            pixelSelector.visit(this, IN_LVALUE_CONTEXT);  // Indicate context is in an LValue
        } else if (varType == Type.IMAGE && pixelSelector != null && channelSelector != null) {
            lValue.setType(Type.INT);
            pixelSelector.visit(this, IN_LVALUE_CONTEXT);
            channelSelector.visit(this, arg);
        } else if (varType == Type.IMAGE && pixelSelector == null && channelSelector != null) {
            lValue.setType(Type.IMAGE);
            channelSelector.visit(this, arg);
        } else if (varType == Type.PIXEL && pixelSelector == null && channelSelector != null) {
            lValue.setType(Type.INT);
            channelSelector.visit(this, arg);
        } else {
            throw new PLCCompilerException("Invalid combination in LValue.");
        }

        return lValue.getType();
    }




    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws PLCCompilerException {
        LValue lValue = assignmentStatement.getlValue();
        Type lValueType = (Type) lValue.visit(this, arg);

        // If the LValue contains a PixelSelector
        if (lValue.getPixelSelector() != null) {
            PixelSelector pixelSelector = lValue.getPixelSelector();
            Expr xExpr = pixelSelector.xExpr();
            Expr yExpr = pixelSelector.yExpr();

            // Enter a new scope
            symbolTable.enterScope();

            // If x and/or y are IdentExpr and not declared, declare them
            if (xExpr instanceof IdentExpr && symbolTable.lookup(((IdentExpr) xExpr).getName()) == null) {
                symbolTable.insert(((IdentExpr) xExpr).getName(), new SyntheticNameDef(((IdentExpr) xExpr).getName()));
            }
            if (yExpr instanceof IdentExpr && symbolTable.lookup(((IdentExpr) yExpr).getName()) == null) {
                symbolTable.insert(((IdentExpr) yExpr).getName(), new SyntheticNameDef(((IdentExpr) yExpr).getName()));
            }


            // Visit the PixelSelector with special context
            pixelSelector.visit(this, IN_LVALUE_CONTEXT);
        }

        // Visit the expression on the right-hand side
        Type exprType = (Type) assignmentStatement.getE().visit(this, arg);

        // Check compatibility based on the provided table
        if (!(lValueType == exprType
                || (lValueType == Type.PIXEL && exprType == Type.INT)
                || (lValueType == Type.IMAGE && (exprType == Type.PIXEL || exprType == Type.INT || exprType == Type.STRING)))) {
            throw new PLCCompilerException("Type mismatch in assignment. LValue type: " + lValueType + ", Expr type: " + exprType);
        }

        // Leave the scope if we entered one at the start
        if (lValue.getPixelSelector() != null) {
            symbolTable.closeScope();
        }

        return exprType;  // Return the type of the expression
    }






    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws PLCCompilerException {
        Expr expr = writeStatement.getExpr();
        Type exprType = (Type) expr.visit(this, arg);
        if (exprType == null) {
            throw new PLCCompilerException("Type of the expression in WriteStatement has not been determined.");
        }
        return exprType;
    }

    @Override
    public Object visitDoStatement(DoStatement doStatement, Object arg) throws PLCCompilerException {
        for (GuardedBlock gBlock : doStatement.getGuardedBlocks()) {
            Type guardType = (Type) gBlock.getGuard().visit(this, arg);
            if (guardType != Type.BOOLEAN) {
                throw new PLCCompilerException("Guard expression in DoStatement must be of type BOOLEAN");
            }
        }
        return null;
    }

    @Override
    public Object visitIfStatement(IfStatement ifStatement, Object arg) throws PLCCompilerException {
        for (GuardedBlock gBlock : ifStatement.getGuardedBlocks()) {
            Type guardType = (Type) gBlock.getGuard().visit(this, arg);
            if (guardType != Type.BOOLEAN) {
                throw new PLCCompilerException("Guard expression in IfStatement's GuardedBlock must be of type BOOLEAN");
            }
        }
        return null;
    }

    @Override
    public Object visitGuardedBlock(GuardedBlock guardedBlock, Object arg) throws PLCCompilerException {
        Type guardType = (Type) guardedBlock.getGuard().visit(this, arg);
        if (guardType != Type.BOOLEAN) {
            throw new PLCCompilerException("Guard expression in GuardedBlock must be of type BOOLEAN");
        }
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCCompilerException {
        Expr returnedExpr = returnStatement.getE();
        Type returnedType = (Type) returnedExpr.visit(this, arg);

        if (returnTypeStack.isEmpty()) {
            throw new PLCCompilerException("Unexpected return statement outside of function or method scope.");
        }

        // Peek at the top of the stack for expected return type
        Type expectedReturnType = returnTypeStack.peek();

        if (returnedType != expectedReturnType) {
            throw new PLCCompilerException("Mismatched return type. Expected " + expectedReturnType + " but found " + returnedType);
        }

        return returnedType;
    }

    @Override
    public Object visitBlockStatement(StatementBlock statementBlock, Object arg) throws PLCCompilerException {
        // Visit the block for type-checking
        statementBlock.getBlock().visit(this, arg);
        return null;
    }



}
