package org.panalyzer;

public interface ParameterUsage {
    String getValue();
    boolean isVariable();
    String getLocation();
}