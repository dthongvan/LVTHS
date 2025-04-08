package visitors;

import cfg.CFG;
import cfg.ICFG;
import cfg.Utils;
import cfg.generation.ICFGGeneration;
import cfg.nodes.*;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class CFGGeneration implements ICFGGeneration {

    // public static logger = new MyLogger("CFGGeneration");

    public static double IF_FLAG = 0;
    public static double DO_FLAG = 1;

    public static double WHILE_FLAG = 2;

    public static double FOR_FLAG = 3;
    public static double FORENHANCED_FLAG = 4;
    public static double SEPARATE_FOR_INTO_SEVERAL_NODES = 1;

    private ICfgNode _BEGIN = new BeginFlagCfgNode();
    private ICfgNode _END = new EndFlagCfgNode();
    private MethodDeclaration _functionNode;

    @Override
    public ICFG generateCFG() {
        return this.parse(this._functionNode);
    }

    private ICFG parse(MethodDeclaration functionNode) {
        Block body = functionNode.getBody();
        if (body instanceof Block) {
            Block block = body;
            this.visitBlock(block, this._BEGIN, this._END, null);
        }

        ArrayList<ICfgNode> statementList = new ArrayList<ICfgNode>();
        this.linkStatement(this._BEGIN, statementList);
        for (int i = 0; i < statementList.size(); i++) {
            statementList.get(i).setParent(this.nextConcrete(statementList.get(i).getParent()));
        }
        CFG result = new CFG(statementList, functionNode);
        result.setRoot(this._BEGIN);
        return result;
    }

    private void visitBlock(Block block, ICfgNode beginNode, ICfgNode endNode, ICfgNode otherNode) {
        List children = block.statements();
        if (children.size() == 0) {
            beginNode.setBranch(endNode);
            return;
        }

        CfgNode scopeIn = ScopeCfgNode.newOpenScope();// cái này trả về Scope với content là {
        beginNode.setBranch(scopeIn);

        CfgNode[] points = new CfgNode[children.size() + 1];
        points[0] = scopeIn;

        CfgNode scopeOut = ScopeCfgNode.newCloseScope(endNode);
        points[children.size()] = scopeOut;

        String check = "";
        List<Integer> checkNumber = new ArrayList<>();
        List<String> checkStatus = new ArrayList<String>();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof IfStatement) {
                check = this.checkContiBreakReturn((Statement) children.get(i));
                if (check == "Continue") {
                    checkNumber.add(i);
                    checkStatus.add("Continue");
                } else if (check == "Break") {
                    checkNumber.add(i);
                    checkStatus.add("Break");
                } else if (check == "Return") {
                    checkNumber.add(i);
                    checkStatus.add("Return");
                }
            }
        }

        int count = 0;

        for (int i = 1; i <= children.size() - 1; i++) { // lặp các phần tử mảng từ 1 => sát cuối // khởi tạo các phần tử
            points[i] = new ForwardCfgNode();
        }

        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof ForStatement || children.get(i) instanceof WhileStatement
                    || children.get(i) instanceof DoStatement || children.get(i) instanceof EnhancedForStatement) {
                int nextElement = i +1 ;
                if (nextElement  == children.size()) {
                    this.visitIterStatement2((Statement) children.get(i), points[i],
                            points[i + 1]);
                } else  {
                    this.visitIterStatement((Statement) children.get(i), (Statement) children.get(i + 1), points[i],
                            points[i + 1]);
                }
            } else if (checkNumber.size() != 0) {
                if ((checkNumber.get(count) == i) && (checkStatus.get(count) == "Continue")) {
                    this.visitStatementBreConRet((Statement) children.get(i), points[i], points[i + 1], endNode);
                    if (count >= checkNumber.size() - 1) {
//                        System.out.println("no count ++ ");
                    } else {
                        count++;
                    }
                } else if ((checkNumber.get(count) == i) && (checkStatus.get(count) == "Break")) {
                    this.visitStatementBreConRet((Statement) children.get(i), points[i], points[i + 1], otherNode);
                    if (count >= checkNumber.size() - 1) {
//                        System.out.println("no count ++ ");
                    } else {
                        count++;
                    }
                } else if ((checkNumber.get(count) == i) && (checkStatus.get(count) == "Return")) {

                    this.visitStatementBreConRet((Statement) children.get(i), points[i], points[i + 1], this._END);
                    if (count >= checkNumber.size() - 1) {
//                        System.out.println("no count ++ ");
                    } else {
                        count++;
                    }
                } else {
                    this.visitStatement((Statement) children.get(i), points[i], points[i + 1]);
                }
            } else {
                this.visitStatement((Statement) children.get(i), points[i], points[i + 1]);
            }
        }
        scopeOut.resetId();
    }

    private void handleSwitchStatement(List list, ICfgNode beginNode, ICfgNode endNode) {
        List children = list;
        int countCaseClause = 0;
        int checkCountCaseClause = 0;
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof SwitchCase) {
                countCaseClause++;
            }
        }

        CfgNode scopeIn = ScopeCfgNode.newOpenScope();// cái này trả về Scope với content là {
        beginNode.setBranch(scopeIn);
        CfgNode scopeOut = ScopeCfgNode.newCloseScope(endNode);

        CfgNode[] points = new CfgNode[list.size() + 1];
        points[0] = scopeIn;
        points[list.size()] = scopeOut;
        int nextSwitchCase = 0;

        for (int i = 1; i <= list.size() - 1; i++) { // lặp các phần tử mảng từ 1 => sát cuối // khởi tạo các phần tử
            points[i] = new ForwardCfgNode();
        }

        for (int i = 0; i <= list.size() - 1; i++) { // lặp các phần tử mảng từ 1 => sát cuối // khởi tạo các phần tử
            if (list.get(i) instanceof SwitchCase) {
                if (checkCountCaseClause == countCaseClause - 1) {
                    this.linkSwitch2((Statement) list.get(i), points[i], points[i + 1], null);
                } else {
                    for (int j = i + 1; j <= list.size() - 1; j++) {
                        if (list.get(j) instanceof SwitchCase) {
                            nextSwitchCase = j;
                            break;
                        }
                    }
                    this.linkSwitch2((Statement) list.get(i), points[i], points[i + 1], points[nextSwitchCase]);
                }
                checkCountCaseClause++;
            } else if (list.get(i) instanceof ExpressionStatement) {
                this.linkSwitch((Statement) list.get(i), points[i], points[i + 1], null);
            } else if (list.get(i) instanceof BreakStatement) {
                this.linkSwitch((Statement) list.get(i), points[i], points[list.size()], null);
            }
        }
    }


    private void linkSwitch(Statement statement, ICfgNode begin, ICfgNode end, Statement nextstatement) {
        if (statement instanceof SwitchCase) {
            if (nextstatement == null) {
                this.visitStatement(statement, begin, end);
            } else {
                SwitchCase switchCase = (SwitchCase) statement;
                CaseCfgNode caseCfgNode = new CaseCfgNode(statement);
                caseCfgNode.setContent(switchCase.toString());

                SwitchCase nextSwitchCase = (SwitchCase) statement;
                CaseCfgNode nextcaseCfgNode = new CaseCfgNode(nextSwitchCase);
                nextcaseCfgNode.setContent(nextSwitchCase.toString());

                begin.setBranch(caseCfgNode);
                caseCfgNode.setFalseNode(nextcaseCfgNode);
                caseCfgNode.setTrueNode(end);
            }
        } else {
            this.visitStatement(statement, begin, end);
        }
    }

    private void linkSwitch2(Statement statement, ICfgNode begin, ICfgNode end, ICfgNode nextstatement) {


        if (statement instanceof SwitchCase) {
            if (nextstatement == null) {
                this.visitStatement(statement, begin, end);
                SwitchCase switchCase = (SwitchCase) statement;
                CaseCfgNode caseCfgNode = new CaseCfgNode(statement);
                caseCfgNode.setContent(switchCase.toString());

                begin.setBranch(caseCfgNode);
                caseCfgNode.setBranch(end);
            } else {
                SwitchCase switchCase = (SwitchCase) statement;
                CaseCfgNode caseCfgNode = new CaseCfgNode(statement);
                caseCfgNode.setContent(switchCase.toString());

                begin.setBranch(caseCfgNode);
                caseCfgNode.setFalseNode(nextstatement);
                caseCfgNode.setTrueNode(end);
            }
        } else {
            this.visitStatement(statement, begin, end);
        }
    }

    private void linkStatement(ICfgNode root, ArrayList<ICfgNode> statements) {
        if (root == null || root.isVisited() == true) {
            return;
        }
        root.setVisited(true);
        statements.add(root);// them node do vao cay
        ICfgNode tmp = root.getTrueNode();// lay node trai
        ICfgNode trueStatement = this.nextConcrete(tmp);
        root.setTrueNode(trueStatement);
        ICfgNode falseStatement = this.nextConcrete(root.getFalseNode());
        root.setFalseNode(falseStatement);

        this.linkStatement(trueStatement, statements);
        this.linkStatement(falseStatement, statements);
    }

    private ICfgNode nextConcrete(ICfgNode stmt) {
        while (stmt instanceof ForwardCfgNode) {
            ForwardCfgNode tmp = (ForwardCfgNode) stmt;
            stmt = tmp.getTrueNode();
        }
        return stmt;
    }

    private void visitSimpleStatement(Statement statement, ICfgNode begin, ICfgNode end) {
        if (statement instanceof VariableDeclarationStatement) {
            DeclarationStatementCfgNode simpleCfgNode = new DeclarationStatementCfgNode(statement);
            begin.setBranch(simpleCfgNode);
            simpleCfgNode.setBranch(end);
            simpleCfgNode.setContent(statement.toString());
        } else if (statement instanceof ExpressionStatement) {
            ExpressionStatementCfgNode simpleCfgNode = new ExpressionStatementCfgNode(statement);
            begin.setBranch(simpleCfgNode);
            simpleCfgNode.setBranch(end);
            simpleCfgNode.setContent(statement.toString());
        } else if (statement instanceof ReturnStatement) {
            ReturnCfgNode returnCfgNode = new ReturnCfgNode(statement);
            begin.setBranch(returnCfgNode);
            returnCfgNode.setBranch(end);
            returnCfgNode.setContent(statement.toString());
        } else if (statement instanceof BreakStatement) {
            BreakStatement breakStatement = (BreakStatement) statement;
            BreakCfgNode breakNode = new BreakCfgNode(statement);
            begin.setBranch(breakNode);
            breakNode.setBranch(end);
            breakNode.setContent(breakStatement.toString());
        } else if (statement instanceof ContinueStatement) {
            ContinueStatement continueStatement = (ContinueStatement) statement;
            ContinueCfgNode continueNode = new ContinueCfgNode(statement);
            begin.setBranch(continueNode);
            continueNode.setBranch(end);
            continueNode.setContent(continueStatement.toString());
        }  else if (statement instanceof ThrowStatement) {
            ThrowStatement throwStatement = (ThrowStatement) statement;
            ThrowCfgNode throwNode = new ThrowCfgNode(statement);
            begin.setBranch(throwNode);
            throwNode.setBranch(end);
            throwNode.setContent(throwStatement.toString());
        } else {
            OtherCfgNode otherCfgNode = new OtherCfgNode(statement);
            begin.setBranch(otherCfgNode);
            otherCfgNode.setBranch(end);
            otherCfgNode.setContent(statement.toString());
        }

        // TODO xử lý lệnh throw, return....
    }

    private void visitInitLoop(SimpleCfgNode simpleCfgNode, ICfgNode beginNode, ICfgNode endNode) {
        beginNode.setBranch(simpleCfgNode);
        simpleCfgNode.setBranch(endNode);
    }


    private String checkContiBreakReturn(Statement statement) {
        String check = "";

        IfStatement ifStatement = (IfStatement) statement;
        Statement thenStmt = ifStatement.getThenStatement();
        if(thenStmt instanceof Block){
            Block block = (Block) thenStmt;
            List children = block.statements();
            for (int i = 0; i < children.size(); i++) {
                if (children.get(i) instanceof ContinueStatement) {
                    check = "Continue";
                } else if (children.get(i) instanceof BreakStatement) {
                    check = "Break";
                } else if (children.get(i) instanceof ReturnStatement) {
                    check = "Return";
                }
            }
        } else {
            if (thenStmt instanceof ContinueStatement) {
                check = "Continue";
            } else if (thenStmt instanceof BreakStatement) {
                check = "Break";
            } else if (thenStmt instanceof ReturnStatement) {
                check = "Return";
            }
        }


        return check;
    }

    private void visitIterStatement2(Statement statement, ICfgNode begin, ICfgNode end) {
        if (statement instanceof ForStatement) {
            //Lấy ra các thông tin về các nút khởi tạo
            ForStatement forStatement = (ForStatement) statement;
            List listIntAst = forStatement.initializers();
            List updaters = forStatement.updaters();
            Expression condition = forStatement.getExpression();
            Statement thenStmt = forStatement.getBody();

            CfgNode[] points = new CfgNode[listIntAst.size() + 1];
            for (int i = 0; i < listIntAst.size() + 1; i++) {
                points[i] = new ForwardCfgNode();
            }
            begin.setBranch(points[0]);

            for (int i = 0; i < listIntAst.size(); i++) {
                ForInitializerCfgNode initNode = new ForInitializerCfgNode((Expression) listIntAst.get(i));
                initNode.setContent(listIntAst.get(i).toString());
                this.visitInitLoop(initNode, points[i], points[i + 1]);
            }

            CfgNode[] tmps = new CfgNode[updaters.size() + 1];
            for (int i = 0; i < updaters.size() + 1; i++) {
                tmps[i] = new ForwardCfgNode();
            }

            for (int i = 0; i < updaters.size(); i++) {
                ForUpdaterCfgNode iterNode = new ForUpdaterCfgNode((Expression) updaters.get(i));
                iterNode.setContent(updaters.get(i).toString());
                this.visitInitLoop(iterNode, tmps[i], tmps[i + 1]);
            }

            ForwardCfgNode afterTrue = new ForwardCfgNode();
            ForwardCfgNode afterFalse = new ForwardCfgNode();

            this.visitCondition(condition, tmps[updaters.size()], points[listIntAst.size()], afterTrue, afterFalse, CFGGeneration.FOR_FLAG);
            //Chỉnh sửa lại chỗ này
            /*
             * chưa check trường hợp thenStmt  hay
             * */
            this.visitStatement3(thenStmt, afterTrue, tmps[0], afterFalse);
//            this.visitStatement3(nextstatement, afterFalse, end, afterFalse);
            afterFalse.setBranch(end);
//            this.visitStatement(nextstatement, afterFalse, end);


        } else if (statement instanceof WhileStatement) {

            WhileStatement whileStatement = (WhileStatement) statement;
            Expression condition = whileStatement.getExpression();
            Statement thenStmt = whileStatement.getBody();
            ForwardCfgNode tmp = new ForwardCfgNode();

            ForwardCfgNode afterTrue = new ForwardCfgNode();
            ForwardCfgNode afterFalse = new ForwardCfgNode();
            this.visitCondition(condition, tmp, begin, afterTrue, afterFalse, CFGGeneration.WHILE_FLAG);
            this.visitStatement3(thenStmt, afterTrue, tmp, afterFalse);
            afterFalse.setBranch(end);
//            this.visitStatement(nextstatement, afterFalse, end);
        } else if (statement instanceof DoStatement) {
            DoStatement doStatement = (DoStatement) statement;
            Expression condition = doStatement.getExpression();
            Statement thenStmt = doStatement.getBody();
            ForwardCfgNode tmp2 = new ForwardCfgNode();
//            this.visitStatement(thenStmt, begin, tmp2);

            ForwardCfgNode afterTrue = new ForwardCfgNode();
            ForwardCfgNode afterFalse = new ForwardCfgNode();
            this.visitStatement3(thenStmt, begin, tmp2 , afterFalse);

            this.visitCondition(condition, begin, tmp2, afterTrue, afterFalse, CFGGeneration.DO_FLAG);
            afterFalse.setBranch(end);
//            this.visitStatement(nextstatement, afterFalse, end);
        } else if (statement instanceof EnhancedForStatement) {
            EnhancedForStatement enhancedForStatement = (EnhancedForStatement) statement;
            Expression condition = enhancedForStatement.getExpression();// cars
            Statement thenStmt = enhancedForStatement.getBody();// body
            SingleVariableDeclaration variableDeclaration = enhancedForStatement.getParameter();//string car

            SimpleCfgNode variable = new SimpleCfgNode(variableDeclaration);
            variable.setContent(variableDeclaration.toString());// tao node string car
            ICfgNode hasMoreNode = new HasMoreNode();


            SimpleCfgNode conditionCfgNode = new SimpleCfgNode(condition);
            begin.setBranch(conditionCfgNode);
//            conditionCfgNode.setBranch(variable);
            conditionCfgNode.setBranch(hasMoreNode);
            conditionCfgNode.setContent(condition.toString());


            hasMoreNode.setContent(hasMoreNode.toString());
            ForwardCfgNode afterFalse = new ForwardCfgNode();
            ForwardCfgNode afterTrue = new ForwardCfgNode();

            hasMoreNode.setTrueNode(afterTrue);
            hasMoreNode.setFalseNode(afterFalse);
            afterTrue.setBranch(variable);

            this.visitStatement(thenStmt, variable, conditionCfgNode);
//            this.visitStatement(nextstatement, afterFalse, end);

//            afterTrue.setBranch(conditionCfgNode);

        }

    }


    private void visitIterStatement(Statement statement, Statement nextstatement, ICfgNode begin, ICfgNode end) {
        if (statement instanceof ForStatement) {
            //Lấy ra các thông tin về các nút khởi tạo
            ForStatement forStatement = (ForStatement) statement;
            List listIntAst = forStatement.initializers();
            List updaters = forStatement.updaters();
            Expression condition = forStatement.getExpression();
            Statement thenStmt = forStatement.getBody();

            CfgNode[] points = new CfgNode[listIntAst.size() + 1];
            for (int i = 0; i < listIntAst.size() + 1; i++) {
                points[i] = new ForwardCfgNode();
            }
            begin.setBranch(points[0]);

            for (int i = 0; i < listIntAst.size(); i++) {
                ForInitializerCfgNode initNode = new ForInitializerCfgNode((Expression) listIntAst.get(i));
                initNode.setContent(listIntAst.get(i).toString());
                this.visitInitLoop(initNode, points[i], points[i + 1]);
            }

            CfgNode[] tmps = new CfgNode[updaters.size() + 1];
            for (int i = 0; i < updaters.size() + 1; i++) {
                tmps[i] = new ForwardCfgNode();
            }

            for (int i = 0; i < updaters.size(); i++) {
                ForUpdaterCfgNode iterNode = new ForUpdaterCfgNode((Expression) updaters.get(i));
                iterNode.setContent(updaters.get(i).toString());
                this.visitInitLoop(iterNode, tmps[i], tmps[i + 1]);
            }

            ForwardCfgNode afterTrue = new ForwardCfgNode();
            ForwardCfgNode afterFalse = new ForwardCfgNode();

            this.visitCondition(condition, tmps[updaters.size()], points[listIntAst.size()], afterTrue, afterFalse, CFGGeneration.FOR_FLAG);
            //Chỉnh sửa lại chỗ này
            /*
            * chưa check trường hợp thenStmt  hay
            * */
            this.visitStatement3(thenStmt, afterTrue, tmps[0], afterFalse);
//            this.visitStatement3(nextstatement, afterFalse, end, afterFalse);
            afterFalse.setBranch(end);
//            this.visitStatement(nextstatement, afterFalse, end);


        } else if (statement instanceof WhileStatement) {

            WhileStatement whileStatement = (WhileStatement) statement;
            Expression condition = whileStatement.getExpression();
            Statement thenStmt = whileStatement.getBody();
            ForwardCfgNode tmp = new ForwardCfgNode();

            ForwardCfgNode afterTrue = new ForwardCfgNode();
            ForwardCfgNode afterFalse = new ForwardCfgNode();
            this.visitCondition(condition, tmp, begin, afterTrue, afterFalse, CFGGeneration.WHILE_FLAG);
            this.visitStatement3(thenStmt, afterTrue, tmp, afterFalse);
            afterFalse.setBranch(end);
//            this.visitStatement(nextstatement, afterFalse, end);
        } else if (statement instanceof DoStatement) {
            DoStatement doStatement = (DoStatement) statement;
            Expression condition = doStatement.getExpression();
            Statement thenStmt = doStatement.getBody();
            ForwardCfgNode tmp2 = new ForwardCfgNode();
            ForwardCfgNode afterTrue = new ForwardCfgNode();
            ForwardCfgNode afterFalse = new ForwardCfgNode();
            this.visitStatement3(thenStmt, begin, tmp2 , afterFalse);

            this.visitCondition(condition, begin, tmp2, afterTrue, afterFalse, CFGGeneration.DO_FLAG);
            afterFalse.setBranch(end);
//            this.visitStatement(nextstatement, afterFalse, end);
        } else if (statement instanceof EnhancedForStatement) {
            EnhancedForStatement enhancedForStatement = (EnhancedForStatement) statement;
            Expression condition = enhancedForStatement.getExpression();// cars
            Statement thenStmt = enhancedForStatement.getBody();// body
            SingleVariableDeclaration variableDeclaration = enhancedForStatement.getParameter();//string car

            SimpleCfgNode variable = new SimpleCfgNode(variableDeclaration);
            variable.setContent(variableDeclaration.toString());// tao node string car
            ICfgNode hasMoreNode = new HasMoreNode();


            SimpleCfgNode conditionCfgNode = new SimpleCfgNode(condition);
            begin.setBranch(conditionCfgNode);
            conditionCfgNode.setBranch(hasMoreNode);
            conditionCfgNode.setContent(condition.toString());


            hasMoreNode.setContent(hasMoreNode.toString());
            ForwardCfgNode afterFalse = new ForwardCfgNode();
            ForwardCfgNode afterTrue = new ForwardCfgNode();

            hasMoreNode.setTrueNode(afterTrue);
            hasMoreNode.setFalseNode(afterFalse);
            afterTrue.setBranch(variable);

            this.visitStatement(thenStmt, variable, conditionCfgNode);
            this.visitStatement(nextstatement, afterFalse, end);
        }

    }

    //Dành cho các câu lệnh break,continue,return
    private void visitStatementBreConRet(Statement statement, ICfgNode begin, ICfgNode end, ICfgNode otherNode) {
        if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            Expression condition = ifStatement.getExpression();
            if (condition == null) {
                System.out.println("Condition is null");
            } else {
                Statement thenStmt = ifStatement.getThenStatement();
                Statement elseStmt = ifStatement.getElseStatement();

                ForwardCfgNode afterTrue = new ForwardCfgNode();
                ForwardCfgNode afterFalse = new ForwardCfgNode();
                this.visitCondition(condition, begin, begin, afterTrue, afterFalse, CFGGeneration.IF_FLAG);

                this.visitStatement(thenStmt, afterTrue, otherNode);
                this.visitStatement(elseStmt, afterFalse, end);
            }
        }
    }


    private void visitStatement3(Statement statement, ICfgNode begin, ICfgNode end, ICfgNode otherNode) {
        // Implement the visitStatement method logic here
        if (statement instanceof Block) {
            Block block = (Block) statement;
            this.visitBlock(block, begin, end, otherNode);
        }  else if (statement == null) {
            begin.setBranch(end);
        }else {
            if (statement instanceof IfStatement) {
                IfStatement ifStatement = (IfStatement) statement;
                Expression condition = ifStatement.getExpression();
                if (condition == null) {
                    System.out.println("Condition is null");
                } else {
                    Statement thenStmt = ifStatement.getThenStatement();
                    Statement elseStmt = ifStatement.getElseStatement();

                    ForwardCfgNode afterTrue = new ForwardCfgNode();
                    ForwardCfgNode afterFalse = new ForwardCfgNode();
                    this.visitCondition(condition, begin, begin, afterTrue, afterFalse, CFGGeneration.IF_FLAG);

                    this.visitStatement(thenStmt, afterTrue, end);
                    this.visitStatement(elseStmt, afterFalse, end);
                }

            } else{
                this.visitSimpleStatement(statement, begin, end);
            }

        }
    }


    private void visitStatement(Statement statement, ICfgNode begin, ICfgNode end) {
        // Implement the visitStatement method logic here
        if (statement instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement) statement;
            Expression condition = ifStatement.getExpression();
            if (condition == null) {
                System.out.println("Condition is null");
            } else {
                Statement thenStmt = ifStatement.getThenStatement();
                Statement elseStmt = ifStatement.getElseStatement();

                ForwardCfgNode afterTrue = new ForwardCfgNode();
                ForwardCfgNode afterFalse = new ForwardCfgNode();
                this.visitCondition(condition, begin, begin, afterTrue, afterFalse, CFGGeneration.IF_FLAG);

                this.visitStatement(thenStmt, afterTrue, end);
                this.visitStatement(elseStmt, afterFalse, end);
            }

        } else if (statement instanceof Block) {
            Block block = (Block) statement;
            this.visitBlock(block, begin, end, null);
        } else if (statement == null) {
            begin.setBranch(end);
        } else if (statement instanceof SwitchStatement) {
            SwitchStatement switchStatement = (SwitchStatement) statement;
            List statementOfSwitch = switchStatement.statements();
            Expression switchCondition = switchStatement.getExpression();

            SwitchCfgNode switchCfgNode = new SwitchCfgNode(switchCondition);
            switchCfgNode.setContent(switchCondition.toString());
            begin.setBranch(switchCfgNode);
            ForwardCfgNode tmp = new ForwardCfgNode();
            switchCfgNode.setBranch(tmp);

            this.handleSwitchStatement(statementOfSwitch, tmp, end);

        } else {
            this.visitSimpleStatement(statement, begin, end);
        }
    }

    public void visitCondition(Expression condition, ICfgNode iterNode, ICfgNode begin, ICfgNode endTrue,
                               ICfgNode endFalse, double flag) {
        condition = Utils.shortenASTCondition(condition);
        this.visitShortenCondition(condition, iterNode, begin, endTrue, endFalse, flag);
    }

    private void visitShortenCondition(Expression condition, ICfgNode iterNode, ICfgNode begin, ICfgNode endTrue,
                                       ICfgNode endFalse, double flag) {
        ICfgNode conditionCfgNode = null;
        if (flag == CFGGeneration.IF_FLAG) {
            conditionCfgNode = new IfConditionCfgNode(condition);
        } else if (flag == CFGGeneration.FOR_FLAG) {
            conditionCfgNode = new ForConditionCfgNode(condition);
            iterNode.setBranch(conditionCfgNode);
        } else if (flag == CFGGeneration.WHILE_FLAG) {
            conditionCfgNode = new WhileConditionCfgNode(condition);
            iterNode.setBranch(conditionCfgNode);
        } else if (flag == CFGGeneration.DO_FLAG) {
            conditionCfgNode = new DoConditionCfgNode(condition);
        } else if (flag == CFGGeneration.FORENHANCED_FLAG) {
            conditionCfgNode = new ForEnhancedConditionCfgNode(condition);
        }
        begin.setBranch(conditionCfgNode);
        conditionCfgNode.setTrueNode(endTrue);
        conditionCfgNode.setFalseNode(endFalse);
        conditionCfgNode.setContent(condition.toString());

        if (flag == CFGGeneration.DO_FLAG) {
            conditionCfgNode.setTrueNode(iterNode);
        }
    }


    @Override
    public MethodDeclaration getFunctionNode() {

        return this._functionNode;
    }

    @Override
    public void setFunctionNode(MethodDeclaration functionNode) {
        this._functionNode = functionNode;
    }

    public CFGGeneration(MethodDeclaration functionNode) {
        this._functionNode = functionNode;
    }

}
