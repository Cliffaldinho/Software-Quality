package metrics;
/*
 * File name:    FanInOutParser.java
 * Author:       Naneth Sayao
 * Date:         29 May 2020
 * Version:      10.1
 * Description:  A specific parser for the fan-in and fan-out metrics.
 *                  This parser will use the 'Parser' class written by Cliff.
 * */

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FanInOutParser {
    private List<FanInOutMethod> methodsList = new ArrayList<>();
    private String currentClassName = "";

    //This inner class separates the methods of one class and puts the methods in a static array list
    public class MethodVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(MethodDeclaration md, Void arg){
            super.visit(md, arg);

            //create new FanInOutMethod Object
            FanInOutMethod temp = new FanInOutMethod();

            //update values of FanInOutMethod object
            temp.setMethodName(md.getNameAsString());
            temp.setMethodBlock(md.toString());
            temp.setParentClass(currentClassName);

            methodSplitter(temp);
        }
    }

    //This inner class separates the methods of one class and puts the methods in a static array list
    public class ConstructorVisitor extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(ConstructorDeclaration md, Void arg){
            super.visit(md, arg);

            //create new FanInOutMethod Object
            FanInOutMethod temp = new FanInOutMethod();

            //update values of FanInOutMethod object
            temp.setMethodName(md.getNameAsString());
            temp.setMethodBlock(md.toString());
            temp.setParentClass(currentClassName);
            temp.setConstructor(true); //for identifying constructors

            methodSplitter(temp);

        }
    }

    //This method helps(is used by) the two visitor classes; MethodSplitter, and ConstructorVisitor
    public void methodSplitter(FanInOutMethod temp){
        //prepare parser
        TypeSolver typeSolver = new ReflectionTypeSolver();
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        //parse input
        //add dummy class declaration to surround method coz it won't compile code with error
        CompilationUnit cu2 = StaticJavaParser.parse("public class Dummy {" + temp.getMethodBlock() + "}");

        //visit each method call expression
        cu2.findAll(MethodCallExpr.class).forEach(mce -> {
            //save the method name to the FanInMethod object's ArrayList of method calls
            temp.getCalledMethodsList().add(mce.getNameAsString());
        });

        //visit each object creation / constructor calls
        cu2.findAll(ObjectCreationExpr.class).forEach(mce -> {
            //save the method name to the FanInMethod object's ArrayList of method calls
            temp.getCalledMethodsList().add(mce.getTypeAsString());
        });

        //add temp to methodsList
        methodsList.add(temp);
    }

    /*
     * Use this method if parsing only one class or .java file
     * This method separates the methods and constructors in the source
     */
    public void singleClassVisitor(CompilationUnit cu){
        //clear our methodsList
        methodsList = new ArrayList<>();

        //create visitor for normal methods
        VoidVisitor<?> methodVisitor = new FanInOutParser.MethodVisitor();
        methodVisitor.visit(cu, null);

        //create visitor for normal methods
        VoidVisitor<?> constructorVisitor = new FanInOutParser.ConstructorVisitor();
        constructorVisitor.visit(cu, null);
    }

    /*
     * Use this method if parsing a whole project
     * This method separates the Java classes in the source
     */
    public void wholeProjectVisitor(List<CompilationUnit> allCU){
        //clear our methodsList
        methodsList = new ArrayList<>();

        for(int i = 0; i < allCU.size(); i++){
            //now we want to separate each methods and save them in a FanInOutMethod object

            //save the class name into the object
            String packageName = "";
            if(allCU.get(i).getPackageDeclaration().isPresent()) {
                packageName = allCU.get(i).getPackageDeclaration().get().getName().toString()+".";
            }
            currentClassName = allCU.get(i).getPrimaryTypeName().toString();

            //Trim the string from Optional[Classname] to Classname
            currentClassName = currentClassName.substring(currentClassName.indexOf("[")+1);
            currentClassName = currentClassName.substring(0,currentClassName.indexOf("]"));

            //add the package name
            currentClassName = packageName + currentClassName;
            //System.out.println(currentClassName);

            //create visitor for normal methods
            VoidVisitor<?> methodVisitor = new MethodVisitor();
            methodVisitor.visit(allCU.get(i), null);
        }

        for(int j = 0; j < allCU.size(); j++){
            //now we want to separate each methods and save them in a FanInOutMethod object

            //save the class name into the object
            String packageName = "";
            if(allCU.get(j).getPackageDeclaration().isPresent()) {
                packageName = allCU.get(j).getPackageDeclaration().get().getName().toString()+".";
            }
            //save the class name into the object
            currentClassName = packageName + allCU.get(j).getPrimaryTypeName().toString();

            //Trim the string from Optional[Classname] to Classname
            currentClassName = currentClassName.substring(currentClassName.indexOf("[")+1);
            currentClassName = currentClassName.substring(0,currentClassName.indexOf("]"));

            //add the package name
            currentClassName = packageName + currentClassName;
            //create visitor for normal methods
            VoidVisitor<?> constructorVisitor = new ConstructorVisitor();
            constructorVisitor.visit(allCU.get(j), null);
        }

    }

    //This method returns the methodsList
    public List<FanInOutMethod> getMethodsList() {
        return methodsList;
    }

}