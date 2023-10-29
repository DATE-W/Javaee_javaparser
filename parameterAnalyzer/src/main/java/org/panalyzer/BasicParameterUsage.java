package org.panalyzer;

public class BasicParameterUsage implements ParameterUsage {
    private String value;
    private boolean isVariable;
    private String location;

    public BasicParameterUsage(String value, boolean isVariable, String location) {
        this.value = value;
        this.isVariable = isVariable;
        this.location = location;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean isVariable() {
        return isVariable;
    }

    @Override
    public String getLocation() {
        return location;
    }
}