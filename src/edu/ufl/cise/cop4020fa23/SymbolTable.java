//// Purpose: Symbol table for the type checker
//package edu.ufl.cise.cop4020fa23;
//
//import edu.ufl.cise.cop4020fa23.ast.NameDef;
//import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.Stack;
//
//class SymbolTable {
//    private final HashMap<String, LinkedList<Symbol>> table;
//    private final Stack<Integer> scopeStack;
//    private int currentScope;
//    private int nextScope;
//
//    public SymbolTable() {
//        table = new HashMap<>();
//        scopeStack = new Stack<>();
//        currentScope = 0;
//        nextScope = 1;
//        scopeStack.push(currentScope);
//    }
//
//    public void enterScope() {
//        currentScope = nextScope++;
//        scopeStack.push(currentScope);
//    }
//
//    public void leaveScope() {
//        if (scopeStack.size() > 1) {  // Ensure that we don't leave the global scope
//            int leavingScope = scopeStack.pop();
//            for (LinkedList<Symbol> list : table.values()) {
//                while (!list.isEmpty() && list.getFirst().getSerialNumber() == leavingScope) {
//                    list.removeFirst();
//                }
//            }
//            currentScope = scopeStack.peek();  // Update currentScope to the new top of the stack
//        } else {
//            throw new IllegalStateException("Cannot leave global scope");
//        }
//    }
//
//    public void insert(NameDef nameDef) throws TypeCheckException {
//        String name = nameDef.getIdentToken().text();
//        LinkedList<Symbol> list = table.get(name);
//
////        if (list != null && !list.isEmpty() && list.getFirst().getSerialNumber() == currentScope) {
////            throw new TypeCheckException("Name already defined in the current scope: " + name);
////        }
//
//        Symbol symbol = new Symbol(name, currentScope, nameDef);
//
//        if (list == null) {
//            list = new LinkedList<>();
//            table.put(name, list);
//        }
//
//        list.addFirst(symbol);
//    }
//
//    public NameDef lookup(String name) {
//        LinkedList<Symbol> list = table.get(name);
//        if (list == null || list.isEmpty()) {
//            return null;
//        }
//
//        for (int i = scopeStack.size() - 1; i >= 0; i--) {
//            int scope = scopeStack.get(i);
//            for (Symbol symbol : list) {
//                if (symbol.getSerialNumber() == scope) {
//                    return symbol.getNameDef();
//                }
//            }
//        }
//        return null;
//    }
//}


package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

class SymbolTable {
    private final HashMap<String, LinkedList<Symbol>> table;
    private final Stack<Integer> scopeStack;
    private int currentScope;
    private int nextScope;

    public SymbolTable() {
        table = new HashMap<>();
        scopeStack = new Stack<>();
        currentScope = 0;
        nextScope = 1;
        scopeStack.push(currentScope);
    }

    public void enterScope() {
        currentScope = nextScope++;
        scopeStack.push(currentScope);
    }

    public void leaveScope() {
        if (!scopeStack.isEmpty()) {
            int oldScope = scopeStack.pop();
            for (LinkedList<Symbol> list : table.values()) {
                while (!list.isEmpty() && list.getFirst().getSerialNumber() == oldScope) {
                    list.removeFirst();
                }
            }
            if (!scopeStack.isEmpty()) {
                currentScope = scopeStack.peek();
            }
        }
    }

//    public void insert(NameDef nameDef) throws TypeCheckException {
//        System.out.println("Inserting to SymbolTable: " + nameDef.getName() + " in scope " + currentScope);
//        String name = nameDef.getName();
//        LinkedList<Symbol> list = table.get(name);
//        if (list == null) {
//            list = new LinkedList<>();
//            table.put(name, list);
//        } else {
//            for (Symbol symbol : list) {
//                if (symbol.getSerialNumber() == currentScope) {
//                    throw new TypeCheckException("Name already defined in the current scope: " + name);
//                }
//            }
//        }
//        Symbol symbol = new Symbol(name, currentScope, nameDef);
//        list.addFirst(symbol);
//    }


    public void insert(NameDef nameDef) throws TypeCheckException {
        System.out.println("Inserting to SymbolTable: " + nameDef.getName() + " in scope " + currentScope);
        String name = nameDef.getName();
        LinkedList<Symbol> list = table.get(name);
        if (list == null) {
            list = new LinkedList<>();
            table.put(name, list);
        } else {
            for (Symbol symbol : list) {
                if (symbol.getSerialNumber() == currentScope) {
                    if (symbol.getNameDef() == nameDef) {
                        // The same declaration is being revisited, safely ignore
                        return;
                    } else {
                        throw new TypeCheckException("Name already defined in the current scope: " + name);
                    }
                }
            }
        }
        Symbol symbol = new Symbol(name, currentScope, nameDef);
        list.addFirst(symbol);
    }



/*    public NameDef lookup(String name) {
        LinkedList<Symbol> list = table.get(name);
        if (list != null) {
            for (int scope : scopeStack) {
                for (Symbol symbol : list) {
                    if (symbol.getSerialNumber() == scope) {
                        return symbol.getNameDef();
                    }
                }
            }
        }
        return null;
    }*/

    public NameDef lookup(String name) {
        LinkedList<Symbol> list = table.get(name);
        if (list != null) {
            for (int i = scopeStack.size() - 1; i >= 0; i--) {
                int scope = scopeStack.get(i);
                for (Symbol symbol : list) {
                    if (symbol.getSerialNumber() == scope) {
                        return symbol.getNameDef();
                    }
                }
            }
        }
        return null;
    }


    public NameDef lookupCurrentScope(String name) {
        LinkedList<Symbol> list = table.get(name);
        if (list != null) {
            for (Symbol symbol : list) {
                if (symbol.getSerialNumber() == currentScope) {
                    return symbol.getNameDef();
                }
            }
        }
        return null;
    }

}
