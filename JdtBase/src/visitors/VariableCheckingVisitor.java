package visitors;
import cfg.ICFG;
import cfg.generation.CFGGeneration;
import cfg.generation.CFGGenerationSubCondition;
import cfg.generation.testpath.Check.FullTestpaths;
//import cfg.nodes.ICfgNode;
import cfg.generation.testpath.Check.IFullTestpath;
import cfg.nodes.DeclarationStatementCfgNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cfg.nodes.ICfgNode;

//import java.util.ArrayList;
//import  visitors.TestPathGeneration;

public class VariableCheckingVisitor extends ASTVisitor {

    private IFullTestpath a;

    public boolean visit(MethodDeclaration node) {
        System.out.println(node.parameters());
        System.out.println(node.getAST());
//        System.out.println("method" );
        CFGGeneration cfgGeneration = new CFGGeneration(node);
//        System.out.println("check cfgGeneration");
//        System.out.println("Get all node:");
        ArrayList<ICfgNode> iCfgNodes = cfgGeneration.generateCFG().getAllNodes();
        ICFG cfg = cfgGeneration.generateCFG();
        CFGGenerationSubCondition cfgGenerationForSubCondition = new CFGGenerationSubCondition(cfg, 1);
        ICFG cfgSubCondition = cfgGenerationForSubCondition.generateCFG();
        FullTestpaths testPaths = new  TestPathGeneration(cfgSubCondition).getPossibleTestpaths();

        ArrayList<String> variableNames = this.getAllVariables(iCfgNodes);
        Set<String> variableSet = new HashSet<>(variableNames);
        for (String variableName : variableSet) {
            ArrayList<IFullTestpath> truePaths = new ArrayList<>();
            ArrayList<IFullTestpath> falsePaths = new ArrayList<>();
            for( int i = 0 ; i < testPaths.size(); i++){
                if(this.checkPathDuplicateDeclarationVariable(testPaths.get(i), variableName=variableName)) truePaths.add(testPaths.get(i));
                else falsePaths.add(testPaths.get(i));
            }
            System.out.println(variableName);
            System.out.println("TruePaths: ------------->>>>> " );

            for(IFullTestpath truePath : truePaths){
                System.out.println(truePath.toString());
            }
            System.out.println("FalsePaths: ------------->>>>>" );

            for(IFullTestpath falsePath : falsePaths){
                System.out.println(falsePath.toString());
            }
            System.out.println("************************");

        }
        return true;
    }

    public boolean checkPathDuplicateDeclarationVariable(IFullTestpath path, String variableName) {
        int counter = 0;
        for(ICfgNode e: path.getAllCfgNodes()){
            if (e instanceof DeclarationStatementCfgNode) {
                if (this.getVariablesNames(e).contains(variableName)) {
                    counter++;
                }
                if (counter > 1) {
                    return false;
                }
            }
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
        if(counter == 1) return true;
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

    public ArrayList<List<ICfgNode>> getAllPathsDefOfVariables(ArrayList<ICfgNode> iCfgNodes , String variableName) {
        ArrayList<List<ICfgNode>> result = new ArrayList<>();

        return result;
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
