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

        // Check from the most recent scope to the outermost scope
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            for (Symbol symbol : table.get(name)) {
                if (symbol.serialNumber == scopeStack.get(i)) {
                    return symbol;
                }
            }
        }

        return null; // Name is not bound in the current or enclosing scopes
    }

    @Override
    public String toString() {
        return "edu.ufl.cise.cop4020fa23.SymbolTable(table=" + table + ", scopeStack=" + scopeStack + ")";
    }
}

