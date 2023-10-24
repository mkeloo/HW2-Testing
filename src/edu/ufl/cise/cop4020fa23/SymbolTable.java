package edu.ufl.cise.cop4020fa23;


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
    }

    public void enterScope() {
        currentNum = nextNum++;
        scopeStack.push(currentNum);
    }

    public void closeScope() {
        currentNum = scopeStack.pop();
    }

    public void insert(String name, String type) {
        Symbol symbol = new Symbol(name, type, currentNum);
        table.putIfAbsent(name, new LinkedList<>());
        table.get(name).add(symbol);
    }

    public Symbol lookup(String name) {
        if (!table.containsKey(name)) {
            return null;
        }

        for (Symbol symbol : table.get(name)) {
            if (scopeStack.contains(symbol.serialNumber)) {
                return symbol;
            }
        }

        return null; // Name is not bound in the current scope
    }

    @Override
    public String toString() {
        return "edu.ufl.cise.cop4020fa23.SymbolTable(table=" + table + ", scopeStack=" + scopeStack + ")";
    }
}

