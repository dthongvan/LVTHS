package visitors;
import cfg.ICFG;
import cfg.generation.CFGGeneration;
import cfg.generation.CFGGenerationSubCondition;
import cfg.generation.testpath.Check.FullTestpaths;
//import cfg.nodes.ICfgNode;
import cfg.nodes.DeclarationStatementCfgNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import cfg.nodes.ICfgNode;
import org.eclipse.jdt.core.dom.VariableDeclaration;

//import java.util.ArrayList;
//import  visitors.TestPathGeneration;

public class VariableCheckingVisitor extends ASTVisitor {

    public boolean visit(MethodDeclaration node) {
        System.out.println(node.parameters());
        System.out.println(node.getAST());
        System.out.println("method" );
        CFGGeneration cfgGeneration = new CFGGeneration(node);
        System.out.println("check cfgGeneration");
        System.out.println("Get all node:");
        ArrayList<ICfgNode> iCfgNodes = cfgGeneration.generateCFG().getAllNodes();
//        ICFG cfg = cfgGeneration.generateCFG();
//        CFGGenerationSubCondition cfgGenerationForSubCondition = new CFGGenerationSubCondition(cfg, 1);
//        ICFG cfgSubCondition = cfgGenerationForSubCondition.generateCFG();
//        FullTestpaths testPaths = new  TestPathGeneration(cfgSubCondition).getPossibleTestpaths();
        ArrayList<String> variableNames = this.getAllVariables(iCfgNodes);
        Set<String> variableSet = new HashSet<>(variableNames);
        for (String variableName : variableSet) {
            System.out.println(variableName + " " +this.checkDuplicateDeclarationVariable(variableName,variableNames));
        }
        return true;
    }

    public boolean checkDuplicateDeclarationVariable(String variableName, ArrayList<String> variableNames){
        int counter = 0;
        for(String e : variableNames){
            if(e.equals(variableName)){
                counter++;
            }
        }
        if(counter == 1){
            return true;
        }
        return false;
    }
    public ArrayList<String> getAllVariables(ArrayList<ICfgNode> iCfgNodes ) {
        ArrayList<String> variables = new ArrayList<String>();
        for (ICfgNode node : iCfgNodes) {
            if (node instanceof DeclarationStatementCfgNode) {
                variables.addAll(this.getVariablesNames(node));
            }
        }
        return variables;
    }


    public  ArrayList<String> getVariablesNames(ICfgNode iCfgNode ) {
        String declaration = iCfgNode.getContent(); // ví dụ: "int a, b = 5, c;"

        // Bước 1: Bỏ từ khóa kiểu dữ liệu
        String[] parts = declaration.trim().split("\\s+", 2);
        // parts[0] = "int", parts[1] = "a, b = 5, c;"

        // Bước 2: Lấy phần sau kiểu dữ liệu
        String variablesPart = parts[1];

        // Bước 3: Bỏ dấu ";" ở cuối
        variablesPart = variablesPart.replace(";", "").trim();

        // Bước 4: Tách theo dấu phẩy
        String[] varDeclarations = variablesPart.split(",");

        // Bước 5: Với mỗi phần tử, lấy tên biến (trước dấu '=' nếu có)
        ArrayList<String> variableNames = new ArrayList<>();
        for (String var : varDeclarations) {
            String varName = var.trim().split("=")[0].trim();
            variableNames.add(varName);
        }
        return variableNames;
    }
}
