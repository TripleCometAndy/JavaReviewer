/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.javareviewer;

import com.github.javaparser.JavaParser;
import static com.github.javaparser.StaticJavaParser.parse;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;

/**
 *
 * @author Andy
 */
public class JavaReviewer {
    private final int ultraman = 2;
    
    public static class MethodInfo {
        private String className;
        private String methodName;
        private int lineNumber;

        public MethodInfo(String className, String methodName, int lineNumber) {
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }

    public static List<MethodInfo> findMethods(String filePath) {
        List<MethodInfo> methodList = new ArrayList<>();
        try {
            // Parse the file
            FileInputStream in = new FileInputStream(filePath);
            CompilationUnit cu = parse(in);
            in.close();

            // Visit and print the methods names and line numbers
            cu.accept(new MethodVisitor(methodList), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return methodList;
    }

    private static class MethodVisitor extends VoidVisitorAdapter<Void> {
        private List<MethodInfo> methodList;

        public MethodVisitor(List<MethodInfo> methodList) {
            this.methodList = methodList;
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            // Get the class name
            String className = n.findAncestor(ClassOrInterfaceDeclaration.class).get().getNameAsString();
            // Get the method name
            String methodName = n.getNameAsString();
            // Get the line number
            int lineNumber = n.getBegin().get().line;
            methodList.add(new MethodInfo(className, methodName, lineNumber));
        }
    }
    
    
    public static class VariableInfo {
        public String className;
        public String variableName;
        public int lineNumber;

        public VariableInfo(String className, String variableName, int lineNumber) {
            this.className = className;
            this.variableName = variableName;
            this.lineNumber = lineNumber;
        }

        @Override
        public String toString() {
            return "Class Name: " + className + ", Variable Name: " + variableName + ", Line Number: " + lineNumber;
        }
    }

    public static List<VariableInfo> findVariables(String filePath) {
        List<VariableInfo> variablesInfo = new ArrayList<>();
        try {
            FileInputStream in = new FileInputStream(filePath);

            CompilationUnit cu = parse(in);

            cu.accept(new VariableVisitor(variablesInfo), null);
            return variablesInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return variablesInfo;
        }
    }

    private static class VariableVisitor extends VoidVisitorAdapter<Void> {
        private List<VariableInfo> variablesInfo;

        public VariableVisitor(List<VariableInfo> variablesInfo) {
            this.variablesInfo = variablesInfo;
        }

        @Override
        public void visit(VariableDeclarator variable, Void arg) {
            String className = variable.findAncestor(com.github.javaparser.ast.body.TypeDeclaration.class).get().getName().asString();
            String variableName = variable.getName().asString();
            int lineNumber = variable.getBegin().get().line;
            variablesInfo.add(new VariableInfo(className, variableName, lineNumber));
        }
    }

    
    
    public static List<String> findJavaFiles(File directory) {
        List<String> javaFiles = new ArrayList<>();

        File[] files = directory.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().endsWith(".java");
            }
        });

        for (File file : files) {
            if (file.isDirectory()) {
                javaFiles.addAll(findJavaFiles(file));
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file.getAbsolutePath());
            }
        }

        return javaFiles;
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        JFileChooser fileChooser = new JFileChooser("/Users/tyson/Projects/testdata");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        while (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            List<String> javaFiles = findJavaFiles(file);
            for (String path : javaFiles) {
                System.out.println("PATH: " + path);
                List<VariableInfo> variables = findVariables(path);
                
                for (VariableInfo variable : variables) {
                    System.out.println(variable);
                }
                
                List<MethodInfo> methods = findMethods(path);
                
                for (MethodInfo method : methods) {
                    System.out.println("METHOD: " + method.getClassName() + ", " + method.getMethodName() + ", " + method.getLineNumber());
                }
            }
        }
    }
}
