package org.panalyzer;

import java.util.ArrayList;
import java.util.List;

public class MethodCallInfo {
    private final String methodName;
    private final List<ParameterInfo> parameters;

    public MethodCallInfo(String methodName) {
        this.methodName = methodName;
        this.parameters = new ArrayList<>();
    }

    public void addParameter(ParameterInfo parameter) {
        this.parameters.add(parameter);
    }

    public String getMethodName() {
        return methodName;
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }
}
