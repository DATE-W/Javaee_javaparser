package org.parser;

import java.util.List;

// 泛型接口，表示一个可分析的实体
public interface Analyzable<T> {
    List<T> analyze();
}
