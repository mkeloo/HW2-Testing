package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;

public class Symbol {
    String name;
    String type;
    int serialNumber;
    private NameDef nameDef;


    public Symbol(String name, String type, int serialNumber, NameDef nameDef) {
        this.name = name;
        this.type = type;
        this.serialNumber = serialNumber;
        this.nameDef = nameDef;

    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public NameDef getNameDef() {
        return nameDef;
    }
}
