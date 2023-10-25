package edu.ufl.cise.cop4020fa23;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

import edu.ufl.cise.cop4020fa23.ast.NameDef;


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


    public boolean insert(String key, NameDef value) {
        Symbol symbol = new Symbol(value, currentNum);

        if (!table.containsKey(key)) {
            table.put(key, new LinkedList<>());
        } else {
            // Check for re-declaration in the current scope
            for (Symbol existingSymbol : table.get(key)) {
                if (existingSymbol.getSerialNumber() == currentNum) {
                    return false;  // Symbol with the same name already exists in the current scope
                }
            }
        }

        return table.get(key).add(symbol);  // This will return true if the addition to the list was successful
    }


    public Symbol lookup(String name) {
        if (!table.containsKey(name)) {
            return null;
        }

        // Check from the most recent scope to the outermost scope
        for (int i = scopeStack.size() - 1; i >= 0; i--) {
            for (Symbol symbol : table.get(name)) {
                if (symbol.getSerialNumber() == scopeStack.get(i)) {  // Fix here: Use getter method
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






//    public boolean insert(String key, NameDef value) {
//        // Assuming you want to use the name and type from the NameDef for the Symbol
//        String name = value.getName();  // Assuming NameDef has a getName() method
//        Type type = value.getType();    // Assuming NameDef has a getType() method
//
//        Symbol symbol = new Symbol(name, type, currentNum);  // Assuming Symbol constructor accepts these types
//        table.putIfAbsent(name, new LinkedList<>());
//
//        List<Symbol> existingSymbols = table.get(name);
//
//        // Check if the symbol already exists in the current scope before adding it
//        for (Symbol existingSymbol : existingSymbols) {
//            if (existingSymbol.getName().equals(name)) {
//                return false;  // Indicate that the insertion failed
//            }
//        }
//
//        // Add the new symbol
//        existingSymbols.add(symbol);
//        return true;  // Indicate successful insertion
//    }
