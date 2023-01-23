/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.javareviewer;

import com.github.javaparser.JavaParser;
import static com.github.javaparser.StaticJavaParser.parse;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

/**
 *
 * @author Andy
 */
public class JavaReviewer {
    private final int ultraman = 2;
    
    private static void getChanges(String repoPath, String oldHash, String newHash) throws IOException {

        try (Repository repository = Git.open(new File(repoPath)).getRepository()) {
            ObjectId oldId = repository.resolve(oldHash);
            ObjectId newId = repository.resolve(newHash);

            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit oldCommit = revWalk.parseCommit(oldId);
                RevCommit newCommit = revWalk.parseCommit(newId);

                try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                    diffFormatter.setRepository(repository);
                    diffFormatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
                    diffFormatter.setDetectRenames(true);

                    List<DiffEntry> diffs = diffFormatter.scan(oldCommit.getTree(), newCommit.getTree());
                    for (DiffEntry diff : diffs) {
                        System.out.println("Change type: " + diff.getChangeType());
                        System.out.println("Old file path: " + diff.getOldPath());
                        System.out.println("New file path: " + diff.getNewPath());

                        if (diff.getChangeType() != DiffEntry.ChangeType.DELETE) {
                            FileHeader fileHeader = diffFormatter.toFileHeader(diff);
                            for (HunkHeader hunk : fileHeader.getHunks()) {
                                for (Edit edit : hunk.toEditList()) {
                                    System.out.println("Line range: " + edit.getBeginA() + "-" + edit.getEndA() + " to " + edit.getBeginB() + "-" + edit.getEndB());
                                }
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    public static class NonEmptyDiamond {
        public int startLine;
        public int endLine;
    }

    public static List<NonEmptyDiamond> getNonEmptyDiamondOperators(String fileName) throws Exception {
        List<NonEmptyDiamond> nonEmptyDiamonds = new ArrayList<>();

        FileInputStream in = new FileInputStream(fileName);
        CompilationUnit cu = parse(in);

        cu.findAll(ObjectCreationExpr.class).forEach(oce -> {
            if (oce.getType().isClassOrInterfaceType() && oce.getType().asClassOrInterfaceType().getTypeArguments().isPresent()
                && !oce.getType().asClassOrInterfaceType().getTypeArguments().get().isEmpty()
                && oce.getParentNode().isPresent() && oce.getParentNode().get() instanceof VariableDeclarator) {
                NonEmptyDiamond nonEmptyDiamond = new NonEmptyDiamond();
                nonEmptyDiamond.startLine = oce.getBegin().map(p -> p.line).orElse(-1);
                nonEmptyDiamond.endLine = oce.getEnd().map(p -> p.line).orElse(-1);
                nonEmptyDiamonds.add(nonEmptyDiamond);
            }
        });

        return nonEmptyDiamonds;
    }
    
    public static class ChainedMethodCall {
        public int startLine;
        public int endLine;
        public String methodCall;
    }

    public static List<ChainedMethodCall> getChainedMethodCalls(String fileName) throws Exception {
        List<ChainedMethodCall> chainedMethodCalls = new ArrayList<>();
        FileInputStream in = new FileInputStream(fileName);
        CompilationUnit cu = parse(in);
        cu.findAll(MethodCallExpr.class).forEach(mce -> {
            Node node = mce;
            while (node instanceof MethodCallExpr) {
                MethodCallExpr methodCallExpr = (MethodCallExpr) node;
                node = methodCallExpr.getScope().orElse(null);
                if(node instanceof MethodCallExpr){
                    ChainedMethodCall chainedMethodCall = new ChainedMethodCall();
                    chainedMethodCall.startLine = methodCallExpr.getBegin().map(p -> p.line).orElse(-1);
                    chainedMethodCall.endLine = methodCallExpr.getEnd().map(p -> p.line).orElse(-1);
                    chainedMethodCall.methodCall = methodCallExpr.toString();
                    chainedMethodCalls.add(chainedMethodCall);
                }
            }
        });
    return chainedMethodCalls;
}
    
    
    public static class StringLiteral {
        public final int lineNumber;
        public final String value;

        public StringLiteral(int lineNumber, String value) {
            this.lineNumber = lineNumber;
            this.value = value;
        }
    }

    public static List<StringLiteral> findStringLiterals(String filePath) throws Exception {
        List<StringLiteral> stringLiterals = new ArrayList<>();
        FileInputStream in = new FileInputStream(filePath);
        CompilationUnit cu = parse(in);
        new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(StringLiteralExpr n, Void arg) {
                stringLiterals.add(new StringLiteral(n.getBegin().get().line, n.getValue()));
                super.visit(n, arg);
            }
        }.visit(cu, null);
        in.close();
        return stringLiterals;
    }
    
    private static class BooleanMethod {
        String name;
        int lineNumber;

        public BooleanMethod(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
    }
    
    public static List<BooleanMethod> getBooleanMethods(String filePath) {
        List<BooleanMethod> booleanMethods = new ArrayList<>();
        try {
            FileInputStream in = new FileInputStream(filePath);
            com.github.javaparser.ast.CompilationUnit cu = parse(in);

            new BooleanMethodVisitor().visit(cu, booleanMethods);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return booleanMethods;
    }

    private static class BooleanMethodVisitor extends com.github.javaparser.ast.visitor.VoidVisitorAdapter<List<BooleanMethod>> {
        @Override
        public void visit(MethodDeclaration md, List<BooleanMethod> arg) {
            super.visit(md, arg);
            if (md.getType().toString().equals("boolean") || md.getType().toString().equals("Boolean")) {
                arg.add(new BooleanMethod(md.getNameAsString(), md.getBegin().get().line));
            }
        }
    }
    
    private static class NumericVariable {
        String name;
        int lineNumber;

        public NumericVariable(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
    }
    
    public static List<NumericVariable> getNumericVariables(String filePath) {
        List<NumericVariable> numericVariables = new ArrayList<>();
        try {
            FileInputStream in = new FileInputStream(filePath);
            com.github.javaparser.ast.CompilationUnit cu = parse(in);

            new NumericVariableVisitor().visit(cu, numericVariables);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return numericVariables;
    }

    private static class NumericVariableVisitor extends VoidVisitorAdapter<List<NumericVariable>> {
        @Override
        public void visit(VariableDeclarator vd, List<NumericVariable> arg) {
            super.visit(vd, arg);
            if (vd.getType().asString().equals("double") || vd.getType().asString().equals("Double") || vd.getType().asString().equals("int") || vd.getType().asString().equals("Integer") || vd.getType().asString().equals("float")) {
                NameExpr nameExpr = vd.getNameAsExpression();
                int lineNumber = nameExpr.getBegin().get().line;
                arg.add(new NumericVariable(nameExpr.getName().asString(), lineNumber));
            }
        }
    }
    
    private static class NonVoidMethod {
        String name;
        int lineNumber;

        public NonVoidMethod(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
    }
    
    public static List<NonVoidMethod> getNonVoidMethods(String filePath) {
        List<NonVoidMethod> nonVoidMethods = new ArrayList<NonVoidMethod>();
        try {
            FileInputStream in = new FileInputStream(filePath);
            com.github.javaparser.ast.CompilationUnit cu = parse(in);

            new NonVoidMethodVisitor().visit(cu, nonVoidMethods);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nonVoidMethods;
    }

    private static class NonVoidMethodVisitor extends com.github.javaparser.ast.visitor.VoidVisitorAdapter<List<NonVoidMethod>> {
        @Override
        public void visit(MethodDeclaration md, List<NonVoidMethod> arg) {
            super.visit(md, arg);
            if (!md.getType().toString().equals("void")) {
                arg.add(new NonVoidMethod(md.getNameAsString(), md.getBegin().get().line));
            }
        }
    }
    
    private static class PrivateStaticMethod {
        String name;
        int lineNumber;

        public PrivateStaticMethod(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
    }
    
    private static boolean isFalse() {
        return true;
    }
    
    public static List<PrivateStaticMethod> getPrivateStaticMethods(String filePath) {
        List<PrivateStaticMethod> privateStaticMethods = new ArrayList<PrivateStaticMethod>();
        try {
            FileInputStream in = new FileInputStream(filePath);
            com.github.javaparser.ast.CompilationUnit cu = parse(in);

            new PrivateStaticMethodVisitor().visit(cu, privateStaticMethods);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return privateStaticMethods;
    }

    private static class PrivateStaticMethodVisitor extends com.github.javaparser.ast.visitor.VoidVisitorAdapter<List<PrivateStaticMethod>> {
        @Override
        public void visit(MethodDeclaration md, List<PrivateStaticMethod> arg) {
            super.visit(md, arg);
            if (md.isPrivate() && md.isStatic()) {
                arg.add(new PrivateStaticMethod(md.getNameAsString(), md.getBegin().get().line));
            }
        }
    }
    
    
    private static class PrivateMethod {
        String name;
        int lineNumber;

        public PrivateMethod(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
    }
    
    private boolean isTrue() {
        return false;
    }
    
    public static List<PrivateMethod> getPrivateMethods(String filePath) {
        List<PrivateMethod> privateMethods = new ArrayList<PrivateMethod>();
        try {
            FileInputStream in = new FileInputStream(filePath);
            com.github.javaparser.ast.CompilationUnit cu = parse(in);

            new PrivateMethodVisitor().visit(cu, privateMethods);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return privateMethods;
    }

    private static class PrivateMethodVisitor extends com.github.javaparser.ast.visitor.VoidVisitorAdapter<List<PrivateMethod>> {
        @Override
        public void visit(MethodDeclaration md, List<PrivateMethod> arg) {
            super.visit(md, arg);
            if (md.isPrivate()) {
                arg.add(new PrivateMethod(md.getNameAsString(), md.getBegin().get().line));
            }
        }
    }
    
    private static class BooleanVariable {
        String name;
        int lineNumber;

        public BooleanVariable(String name, int lineNumber) {
            this.name = name;
            this.lineNumber = lineNumber;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public static List<BooleanVariable> getBooleanVariables(String filePath) {
        List<BooleanVariable> booleanVariables = new ArrayList<BooleanVariable>();
        try {
            FileInputStream in = new FileInputStream(filePath);
            com.github.javaparser.ast.CompilationUnit cu = parse(in);

            new BooleanVariableVisitor().visit(cu, booleanVariables);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return booleanVariables;
    }

    private static class BooleanVariableVisitor extends VoidVisitorAdapter<List<BooleanVariable>> {
        @Override
        public void visit(VariableDeclarator vd, List<BooleanVariable> arg) {
            super.visit(vd, arg);
            if (vd.getType().asString().equals("boolean")) {
                NameExpr nameExpr = vd.getNameAsExpression();
                int lineNumber = nameExpr.getBegin().get().line;
                arg.add(new BooleanVariable(nameExpr.getName().asString(), lineNumber));
            }
        }
    }
    
    private static class MethodArgument {
        private String name;
        private int line;
        MethodArgument(String name, int line) {
            this.name = name;
            this.line = line;
        }
        public String getName() {
            return name;
        }
        public int getLine() {
            return line;
        }
    }
    
    public static List<MethodArgument> getMethodArguments(String filePath) {
        List<MethodArgument> arguments = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(filePath)) {
            CompilationUnit cu = parse(in);
            cu.findAll(MethodDeclaration.class).forEach(method -> {
                method.getParameters().forEach(param -> {
                    arguments.add(new MethodArgument(param.getNameAsString(), param.getRange().get().begin.line));
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arguments;
    }
    
    public static List<Integer> getReturnStatementLineNumbers(String filePath) {
        List<Integer> lineNumbers = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(filePath)) {
            CompilationUnit cu = parse(in);
            cu.findAll(ReturnStmt.class).forEach(returnStmt -> {
                lineNumbers.add(returnStmt.getRange().get().begin.line);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lineNumbers;
    }
    
    public static List<Integer> getIfStatementLineNumbers(String filePath) {
        List<Integer> lineNumbers = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(filePath)) {
            CompilationUnit cu = parse(in);

            cu.findAll(IfStmt.class).forEach(ifStmt -> {
                lineNumbers.add(ifStmt.getRange().get().begin.line);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lineNumbers;
    }
    
        public static List<Integer> getElseStatementLineNumbers(String filePath) {
        List<Integer> lineNumbers = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(filePath)) {
            CompilationUnit cu = parse(in);
            cu.findAll(IfStmt.class).forEach(ifStmt -> {
                if (ifStmt.getElseStmt().isPresent()) {
                    lineNumbers.add(ifStmt.getElseStmt().get().getRange().get().begin.line);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lineNumbers;
    }
    
    public static List<Integer> findStringInFile(String path, String searchString) {
        List<Integer> lineNumbers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (line.contains(searchString)) {
                    lineNumbers.add(lineNumber);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineNumbers;
    }
    
    public static class MethodCall {
        public String name;
        public int line;

        public MethodCall(String name, int line) {
            this.name = name;
            this.line = line;
        }
    }

    public static List<MethodCall> findMethodCalls(String filePath) throws Exception {
        FileInputStream in = new FileInputStream(filePath);
        List<MethodCall> methodCalls = new ArrayList<>();
        new VoidVisitorAdapter<Object>() {
            @Override
            public void visit(MethodCallExpr n, Object arg) {
                methodCalls.add(new MethodCall(n.getNameAsString(), n.getBegin().get().line));
                super.visit(n, arg);
            }
        }.visit(parse(in), null);
        in.close();
        return methodCalls;
    }
    
    public static class MemberVariable {
        public String name;
        public int line;
        public String className;        

        public MemberVariable(String name, int line, String className) {
            this.name = name;
            this.line = line;
            this.className = className;
        }
    }

    public static List<MemberVariable> findMemberVariables(String filePath) throws Exception {
        FileInputStream in = new FileInputStream(filePath);
        List<MemberVariable> variables = new ArrayList<>();
        new VoidVisitorAdapter<Object>() {
            String currentClass = "";
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                currentClass = n.getNameAsString();
                super.visit(n, arg);
            }

            @Override
            public void visit(FieldDeclaration n, Object arg) {
                variables.add(new MemberVariable(n.getVariables().get(0).getNameAsString(), n.getBegin().get().line, currentClass));
                super.visit(n, arg);
            }
        }.visit(parse(in), null);
        in.close();
        return variables;
    }
    
    
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
    public static void main(String[] args) throws FileNotFoundException, Exception {
        JFileChooser fileChooser = new JFileChooser("/Users/tyson/Projects/testdata");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (true) {
            
        }
        else {
           
        }
        
        getChanges("C:/Users/Andy/projects/JavaReviewer/JavaReviewer", "4f3e9563d56502e906f57c284b842e43ddd0a9e5", "2456f52208d087bf9d68ac757c94073e4d2a7de2");
        
        while (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            List<String> javaFiles = findJavaFiles(file);
            for (String path : javaFiles) {
                System.out.println("PATH: " + path);
                List<VariableInfo> variables = findVariables(path);
                
                for (VariableInfo variable : variables) {
                    System.out.println(variable);
                }
                
                boolean isTrue = false;
                
                List<MethodInfo> methods = findMethods(path);
                
                for (MethodInfo method : methods) {
                    System.out.println("METHOD: " + method.getClassName() + ", " + method.getMethodName() + ", " + method.getLineNumber());
                }
                
                List<MemberVariable> memberVariables = findMemberVariables(path);
                
                for (MemberVariable memberVariable : memberVariables) {
                    System.out.println("MEMBER VAR: " + memberVariable.className + ", " + memberVariable.name + ", " + memberVariable.line);
                }
                
                List<MethodCall> methodCalls = findMethodCalls(path);
                
                for (MethodCall methodCall : methodCalls) {
                    System.out.println("METHOD CALL: " + methodCall.name + ", " + methodCall.line);
                }
                
                List<Integer> assignments = findStringInFile(path, " = ");
                
                for (Integer lineNumber : assignments) {
                    System.out.println("Assignement line number: " + lineNumber);
                }
                
                List<Integer> ifStatements = getIfStatementLineNumbers(path);
                
                for (Integer lineNumber : ifStatements) {
                    System.out.println("IF STATEMENT: " + lineNumber);
                }
                
                List<Integer> elseStatements = getElseStatementLineNumbers(path);
                
                for (Integer lineNumber: elseStatements) {
                    System.out.println("ELSE STATEMENT PRESENT AT: " + lineNumber);
                }
                
                List<Integer> returnStatements = getReturnStatementLineNumbers(path);
                
                for (Integer lineNumber: returnStatements) {
                    System.out.println("RETURN STATEMENT: " + lineNumber);
                }
                
                List<MethodArgument> methodArguments = getMethodArguments(path);
                
                for (MethodArgument arg : methodArguments) {
                    System.out.println("METHOD ARG: " + arg.name + ", " + arg.line);
                }
                
                List<BooleanVariable> booleanVariables = getBooleanVariables(path);
                
                for (BooleanVariable booleanVar : booleanVariables) {
                    System.out.println("BOOL: " + booleanVar.name + ", " + booleanVar.lineNumber);
                }
                
                List<PrivateMethod> privateMethods = getPrivateMethods(path);
                
                for (PrivateMethod privateMethod : privateMethods) {
                    System.out.println("PRIVATE METHOD: " + privateMethod.name + ", " + privateMethod.lineNumber);
                }
                
                List<PrivateStaticMethod> privateStaticMethods = getPrivateStaticMethods(path);
                
                for (PrivateStaticMethod privateStaticMethod : privateStaticMethods) {
                    System.out.println("PRIVATE STATIC METHOD: " + privateStaticMethod.name + privateStaticMethod.lineNumber);
                }
                
                List<NonVoidMethod> nonVoidMethods = getNonVoidMethods(path);
                
                for (NonVoidMethod nonVoidMethod : nonVoidMethods) {
                    System.out.println("NON VOID METHOD: " + nonVoidMethod.name + ", " + nonVoidMethod.lineNumber);
                }
                
                List<NumericVariable> numericVariables = getNumericVariables(path);
                
                for (NumericVariable numericVariable : numericVariables) {
                    System.out.println("NUM: " + numericVariable.name + ", " + numericVariable.lineNumber);
                }
                
                List<BooleanMethod> booleanMethods = getBooleanMethods(path);
                
                for (BooleanMethod booleanMethod : booleanMethods) {
                    System.out.println("BOOL METHOD: " + booleanMethod.name + ", " + booleanMethod.lineNumber);
                }
                
                List<StringLiteral> hardcodedStrings = findStringLiterals(path);
                
                for (StringLiteral hardcodedString : hardcodedStrings) {
                    System.out.println("HARDCODED STRING: " + hardcodedString.value + ", " + hardcodedString.lineNumber);
                }
                
                List<ChainedMethodCall> chainedMethodCalls = getChainedMethodCalls(path);
                
                for (ChainedMethodCall chainedMethodCall : chainedMethodCalls) {
                    System.out.println("CHAINED METHOD CALL AT: " + chainedMethodCall.startLine + ", " + chainedMethodCall.endLine + ", " + chainedMethodCall.methodCall);
                }
                
                List<NonEmptyDiamond> nonEmptyDiamonds = getNonEmptyDiamondOperators(path);
                
                for (NonEmptyDiamond nonEmptyDiamond : nonEmptyDiamonds) {
                    System.out.println("NON EMPTY DIAMOND: " + nonEmptyDiamond.startLine + ", " + nonEmptyDiamond.endLine);
                }
                
            }
        }
    }
}
