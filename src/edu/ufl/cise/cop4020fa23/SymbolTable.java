package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

class SymbolTable {
    private HashMap<String, LinkedList<Symbol>> table;
    private Stack<Integer> scopeStack;
    private int currentNum;
    private int nextNum;

    public SymbolTable() {
        table = new HashMap<>();
        scopeStack = new Stack<>();
        currentNum = 0;
        nextNum = 1;
        scopeStack.push(currentNum);
    }

    public void enterScope() {
        currentNum = nextNum++;
        scopeStack.push(currentNum);
    }

    public void leaveScope() {
        int oldScope = scopeStack.pop();
        for (LinkedList<Symbol> list : table.values()) {
            while (!list.isEmpty() && list.getFirst().getSerialNumber() == currentNum) {
                list.removeFirst();
            }
        }
        currentNum = oldScope;
    }

    public boolean addSymbol(String name, String type, NameDef nameDef) {
        LinkedList<Symbol> list = table.get(name);
        if (list != null && !list.isEmpty() && list.getFirst().getSerialNumber() == currentNum) {
            // Symbol with the same name already exists in the current scope
            return false;
        }
        Symbol sym = new Symbol(name, type, currentNum, nameDef);
        if (list == null) {
            list = new LinkedList<>();
            table.put(name, list);
        }
        list.addFirst(sym);
        return true;
    }



    public Symbol lookup(String name) {
        LinkedList<Symbol> list = table.get(name);
        if (list == null || list.isEmpty()) return null;

        for (int scope : scopeStack) {
            for (Symbol symbol : list) {
                if (symbol.getSerialNumber() == scope) {
                    return symbol;
                }
            }
        }
        return null;
    }

    public Symbol lookupCurrentScope(String name) {
        LinkedList<Symbol> list = table.get(name);
        if (list == null || list.isEmpty()) return null;

        int currentScope = scopeStack.peek(); // Get the current scope
        for (Symbol symbol : list) {
            if (symbol.getSerialNumber() == currentScope) {
                return symbol; // Return the symbol if it's in the current scope
            }
        }
        return null; // Return null if the symbol is not found in the current scope
    }


    public void dump() {
        System.out.println("SYMBOL TABLE");
        for (String name : table.keySet()) {
            LinkedList<Symbol> list = table.get(name);
            for (Symbol s : list) {
                System.out.println(s.getName() + " " + s.getType() + " " + s.getSerialNumber());
            }
        }
    }
}
