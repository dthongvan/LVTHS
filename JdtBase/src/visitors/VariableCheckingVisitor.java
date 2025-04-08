package visitors;
import cfg.ICFG;
import cfg.generation.CFGGeneration;
import cfg.generation.CFGGenerationSubCondition;
import cfg.generation.testpath.Check.FullTestpaths;
import cfg.nodes.ICfgNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import  visitors.TestPathGeneration;

public class VariableCheckingVisitor extends ASTVisitor {

    public boolean visit(MethodDeclaration node) {
        System.out.println(node.parameters());
        System.out.println(node.getAST());
        System.out.println("method" );
        CFGGeneration cfgGenerration = new CFGGeneration(node);
        System.out.println("check cfgGeneration");
//        System.out.println(cfgGenerration.getFunctionNode());

        System.out.println("Get all node:");
        ArrayList<ICfgNode> iCfgNodes = cfgGenerration.generateCFG().getAllNodes();
        ICFG cfg = cfgGenerration.generateCFG();
        CFGGenerationSubCondition cfgGenerrationforsubcondition = new CFGGenerationSubCondition(cfg, 1);
        ICFG cfgsubcondition = cfgGenerrationforsubcondition.generateCFG();
        FullTestpaths testPaths = new  TestPathGeneration(cfgsubcondition).getPossibleTestpaths();
        System.out.println(testPaths.toString());
        return true;
    }

}
