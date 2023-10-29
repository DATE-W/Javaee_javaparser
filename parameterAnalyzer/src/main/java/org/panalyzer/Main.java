package org.panalyzer;

public class Main {

    public static void main(String[] args) {
        ParameterAnalyzer analyzer = new ParameterAnalyzer();
        String projectDirectoryPath = "src/main/java/main"; // Assume the project directory is passed as an argument.
        analyzer.analyzeProject(projectDirectoryPath); // Call the analyzeProject method
        analyzer.displayResults();
    }
}
