package visitors;

import cfg.ICFG;
import cfg.generation.testpath.Check.FullTestpath;
import cfg.generation.testpath.Check.FullTestpaths;
import cfg.generation.testpath.Check.ITestpath;
import cfg.generation.testpath.ITestpathGeneration;
import cfg.generation.testpath.Testpath;
import cfg.generation.testpath.TestpathGenerationConfig;
import cfg.nodes.FlagCfgNode;
import cfg.nodes.ICfgNode;
import cfg.nodes.LoopConditionCfgNode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TestPathGeneration implements ITestpathGeneration {

    private ICFG _cfg;
    private ArrayList<Testpath> _testpaths;
    private TestpathGenerationConfig _config;
    private int maxIterationsforEachLoop;
    private long totalRunningTime = 0;// ms
    private boolean checkLoop = false;// ms

    private FullTestpaths possibleTestpaths = new FullTestpaths();


    public TestPathGeneration(ICFG cfg, TestpathGenerationConfig config) {
        this._cfg = cfg;
        this._config = config;
    }

    public TestPathGeneration(ICFG cfg, int maxLoop) {
        this._cfg = cfg;
        this._config = null;
        this.maxIterationsforEachLoop = maxLoop;
    }

    /*
    OLD
    * */
    public TestPathGeneration(ICFG cfg) {
        this._cfg = cfg;
        this._config = null;
    }
    private void traverseCFG(ICfgNode stm, FullTestpath tp, FullTestpaths testPaths) throws Exception {
        tp.add(stm);
        if (FlagCfgNode.isEndNode(stm)) {
            testPaths.add((FullTestpath) tp.clone());
            tp.remove(tp.size() - 1);
        } else {
            ICfgNode trueNode = stm.getTrueNode();
            ICfgNode falseNode = stm.getFalseNode();

            if (stm.isCondition())

                if (stm instanceof LoopConditionCfgNode) {

                    int currentIterations = tp.count(trueNode);
                    if (currentIterations < 2) {
                        traverseCFG(falseNode, tp, testPaths);
                        traverseCFG(trueNode, tp, testPaths);

                    } else
                        traverseCFG(falseNode, tp, testPaths);
                } else {
                    traverseCFG(falseNode, tp, testPaths);
                    traverseCFG(trueNode, tp, testPaths);
                }
            else
                traverseCFG(trueNode, tp, testPaths);
            tp.remove(tp.size() - 1);

        }
    }
    /*
    OLD
    * */





    public void generateTestpaths() {
        Date startTime = Calendar.getInstance().getTime();
        FullTestpaths testpaths_ = new FullTestpaths();
        ICfgNode beginNode = this._cfg.getBeginNode();
        FullTestpath initialTestpath = new FullTestpath();
        initialTestpath.setFunctionNode(this._cfg.getFunctionNode());
        try {
            traverseCFG(beginNode, initialTestpath, testpaths_);
//            traverseCFGforIteraction(beginNode, initialTestpath, testpaths_);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (ITestpath tp : testpaths_)
            tp.setFunctionNode(this._cfg.getFunctionNode());

    }

    private void traverseCFGforIteraction(ICfgNode stm, FullTestpath tp, FullTestpaths testpaths) throws Exception {
        tp.add(stm);
        if (FlagCfgNode.isEndNode(stm)) {
            if(checkLoop) {
                testpaths.add((FullTestpath) tp.clone());
                checkLoop = false;
            }
            tp.remove(tp.size() - 1);
        } else {
            ICfgNode trueNode = stm.getTrueNode();
            ICfgNode falseNode = stm.getFalseNode();

            if (stm.isCondition()) {
                if (stm instanceof LoopConditionCfgNode) {

                    int currentIterations = tp.count(trueNode);

                    if (currentIterations < maxIterationsforEachLoop) {
                        traverseCFGforIteraction(falseNode, tp, testpaths);
                        traverseCFGforIteraction(trueNode, tp, testpaths);
                    } else {
                        checkLoop = true;
                        traverseCFGforIteraction(falseNode, tp, testpaths);
//                        traverseCFGforIteraction(trueNode, tp, testpaths);
                    }
                } else {
                    traverseCFGforIteraction(falseNode, tp, testpaths);
                    traverseCFGforIteraction(trueNode, tp, testpaths);
                }
            } else {
                traverseCFGforIteraction(trueNode, tp, testpaths);
            }
            tp.remove(tp.size() - 1);
        }
    }

    private void filter(FullTestpaths testpaths) throws Exception {

    }


    //    public ArrayList<ITestpath> getPossibleTestpaths() {


    @Override
    public FullTestpaths getPossibleTestpaths() {
        FullTestpaths testpaths_ = new FullTestpaths();
        ICfgNode beginNode = this._cfg.getBeginNode();
        FullTestpath initialTestpath = new FullTestpath();
        initialTestpath.setFunctionNode(this._cfg.getFunctionNode());
        try {
            traverseCFG(beginNode, initialTestpath, testpaths_);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (ITestpath tp : testpaths_)
            tp.setFunctionNode(this._cfg.getFunctionNode());
        return testpaths_;
    }

    public ICFG getCfg() {
        return this._cfg;
    }

    public void setCfg(ICFG value) {
        this._cfg = value;
    }

    public ArrayList<Testpath> getTestpaths() {
        return this._testpaths;
    }

    public void setTestpaths(ArrayList<Testpath> value) {
        this._testpaths = value;
    }

    public TestpathGenerationConfig getConfig() {
        return this._config;
    }

    public void setConfig(TestpathGenerationConfig value) {
        this._config = value;
    }
}



