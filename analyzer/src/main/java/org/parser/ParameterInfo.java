package org.parser;

// 后续肯定要修改的
public class ParameterInfo {

    private String parameterName;
    private String parameterClass;
    private int parameterLine;

    public ParameterInfo(String parameterName, String parameterClass, int parameterLine) {
        this.parameterName = parameterName;
        this.parameterClass = parameterClass;
        this.parameterLine = parameterLine;
    }
}

