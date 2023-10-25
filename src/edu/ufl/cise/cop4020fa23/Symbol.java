//package edu.ufl.cise.cop4020fa23;
//
//
//public class Symbol {
//    String name;
//    String type;
//    int serialNumber;
//
//    public Symbol(String name, String type, int serialNumber) {
//        this.name = name;
//        this.type = type;
//        this.serialNumber = serialNumber;
//    }
//
//    public String getName() {
//    	return name;
//    }
//
//    public String getType() {
//    	return type;
//    }
//
//    public int getSerialNumber() {
//    	return serialNumber;
//    }
//
//
//
//    @Override
//    public String toString() {
//        return "edu.ufl.cise.cop4020fa23.Symbol(name=" + name + ", type=" + type + ", serialNumber=" + serialNumber + ")";
//    }
//}


package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;  // Assuming this is the correct import for NameDef

public class Symbol {
    private final NameDef nameDef;
    private final int serialNumber;

    public Symbol(NameDef nameDef, int serialNumber) {
        this.nameDef = nameDef;
        this.serialNumber = serialNumber;
    }

    public NameDef getNameDef() {
        return nameDef;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    @Override
    public String toString() {
        return "edu.ufl.cise.cop4020fa23.Symbol(nameDef=" + nameDef + ", serialNumber=" + serialNumber + ")";
    }
}













//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.Stack;
//
//class edu.ufl.cise.cop4020fa23.Symbol {
//    String name;
//    String type;
//    int serialNumber;
//
//    public edu.ufl.cise.cop4020fa23.Symbol(String name, String type, int serialNumber) {
//        this.name = name;
//        this.type = type;
//        this.serialNumber = serialNumber;
//    }
//
//    @Override
//    public String toString() {
//        return "edu.ufl.cise.cop4020fa23.Symbol(name=" + name + ", type=" + type + ", serialNumber=" + serialNumber + ")";
//    }
//}
//
//class edu.ufl.cise.cop4020fa23.SymbolTable {
//    HashMap<String, edu.ufl.cise.cop4020fa23.Symbol> table;
//    Stack<LinkedList<edu.ufl.cise.cop4020fa23.Symbol>> stack;
//    int serialNumber;
//
//    public edu.ufl.cise.cop4020fa23.SymbolTable() {
//        table = new HashMap<>();
//        stack = new Stack<>();
//        serialNumber = 0;
//    }
//
//    public void enterScope() {
//        stack.push(new LinkedList<>());
//    }
//
//    public void exitScope() {
//        LinkedList<edu.ufl.cise.cop4020fa23.Symbol> symbols = stack.pop();
//        for (edu.ufl.cise.cop4020fa23.Symbol symbol : symbols) {
//            table.remove(symbol.name);
//        }
//    }
//
//    public void addSymbol(String name, String type) {
//        edu.ufl.cise.cop4020fa23.Symbol symbol = new edu.ufl.cise.cop4020fa23.Symbol(name, type, serialNumber++);
//        table.put(name, symbol);
//        stack.peek().add(symbol);
//    }
//
//    public edu.ufl.cise.cop4020fa23.Symbol getSymbol(String name) {
//        return table.get(name);
//    }
//
//    public boolean containsSymbol(String name) {
//        return table.containsKey(name);
//    }
//
//    @Override
//    public String toString() {
//        return "edu.ufl.cise.cop4020fa23.SymbolTable(table=" + table + ", stack=" + stack + ", serialNumber=" + serialNumber + ")";
//    }
//}

