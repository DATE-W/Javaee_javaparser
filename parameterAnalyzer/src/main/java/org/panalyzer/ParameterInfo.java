package org.panalyzer;

import java.util.ArrayList;
import java.util.List;

public class ParameterInfo {
    private final int index;
    private final String type;
    private final String name;
    private final List<ParameterUsage> usages;

    public ParameterInfo(int index, String type, String name) {
        this.index = index;
        this.type = type;
        this.name = name;
        this.usages = new ArrayList<>();
    }

    public void addUsage(ParameterUsage usage) {
        this.usages.add(usage);
    }

    public int getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<ParameterUsage> getUsages() {
        return usages;
    }
}

