package com.ccip.bank.user;

import helloMatrix.hydtTest;
import hydtColdHot.ColdHot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lpsolve.LpSolveException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.tensorflow.Operation;
import org.tensorflow.Output;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import riskZscore.riskData;
import test.test;
import trainClassifier_Tree.creditQuality;
import ES3.industryES3;
import HC_Index.HC;
import KS_Index.KS;
import L2Normal.L2;
import MinMaxScaler.MinMax;
import Risk.testRisk;
import Zone_ES3.ZoneInvest;

import com.ccip.bank.bean.CNNbusinessBean;
import com.ccip.bank.bean.ScienceInvest;
import com.ccip.bank.bean.businessBean;
import com.ccip.bank.bean.creditQualityBean;
import com.ccip.bank.bean.financialRiskBean;
import com.ccip.bank.model.Company;
import com.ccip.bank.model.InvestFactor;
import com.ccip.bank.model.InvestPotential;
import com.ccip.bank.model.Market;
import com.ccip.bank.model.TFModelPred;
import com.ccip.bank.service.CompanyService;
import com.ccip.bank.utils.javaDea.DataEnvelopmentAnalysis;
import com.ccip.bank.utils.javaDea.DeaRecord;
import com.ccip.bank.utils.javaDea.GetExcelInfo;
import com.jfinal.aop.Before;
import com.jfinal.core.ActionKey;
import com.jfinal.core.Controller;
import com.jfinal.kit.JsonKit;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.upload.UploadFile;
import com.jmatio.io.MatFileReader;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import com.mathworks.toolbox.javabuilder.MWClassID;
import com.mathworks.toolbox.javabuilder.MWComplexity;
import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import com.mathworks.toolbox.javabuilder.external.org.json.JSONException;

/**
 * 
 * @author Mason
 * @data 20180323 5大模型算法实现及数据操作
 */

public class PredictController extends Controller {

    static  //get WEB Root path Notice:confige tomcat server location to project path
    String paths = System.getProperty("user.dir");
    static String modelPathPrex = paths + "/TFModel/";
    static String dataSetPrex = paths + "/datasets/";

    static CompanyService service = new CompanyService();
    String path =  this.getClass().getClassLoader().getResource(".").getPath();


    @ActionKey("/predict")
    @Before(com.ccip.bank.interceptor.UserAuthInterceptor.class)
    public void index() {

    }


    // 异步请求加载数据库：行业动态最终指标数据
    public void marketData() throws MWException {
        hydtTest datas = new hydtTest();
        String input;
        Object[] Results = null;
        int type = getParaToInt("type"); // 获取行业类型：0房地产、1汽车业、2信息服务业
        if (type == 1) {
            input = dataSetPrex + "hydt/car_data.txt"; // 汽车业
            Results = datas.financialRiskCal(6, input, 4, 4, 4);
        } else if (type == 0) {
            input = dataSetPrex + "hydt/home_data.txt"; // 房地产市场
            Results = datas.financialRiskCal(6, input, 3, 6, 3); // 房地产市场
        } else {
            input = dataSetPrex + "hydt/info_data.txt"; // 信息服务业
            Results = datas.financialRiskCal(6, input, 4, 4, 4); // 信息服务业
        }
        // 用于保存输出矩阵
        MWNumericArray output = null;
        MWNumericArray output1 = null;
        MWNumericArray output2 = null;
        MWNumericArray output3 = null;
        MWNumericArray output4 = null;
        MWNumericArray output5 = null;
        // 将结果object转换成MWNumericArray
        output = (MWNumericArray) Results[0];
        output1 = (MWNumericArray) Results[1];
        output2 = (MWNumericArray) Results[2];
        output3 = (MWNumericArray) Results[3];
        output4 = (MWNumericArray) Results[4];
        output5 = (MWNumericArray) Results[5];
        // 从MWNumericArray对象中读取数据
        int[] res = output.rowIndex();
        float[] myList = new float[res.length];
        float[] myList1 = new float[res.length];
        float[] myList2 = new float[res.length];
        float[] myList3 = new float[res.length];
        float[] myList4 = new float[res.length];
        float[] myList5 = new float[res.length];
        for (int i = 0; i < res.length; i++) {
            myList[i] = output.getFloat(i + 1); // 盈利
            myList1[i] = output1.getFloat(i + 1); // 经营
            myList2[i] = output2.getFloat(i + 1); // 偿债
            myList3[i] = output3.getFloat(i + 1); // 发展
            myList4[i] = output4.getFloat(i + 1); // 综合
            myList5[i] = output5.getFloat(i + 1); // 年份
        }

        setAttr("r1", myList);
        setAttr("r2", myList1);
        setAttr("r3", myList2);
        setAttr("r4", myList3);
        setAttr("r5", myList4);
        setAttr("year", myList5);
        System.out.println(myList);
        renderJson(new String[] { "year", "r1", "r2", "r3", "r4", "r5" });

    }


    /**
     * @deprecated 20180910  实现三次指数平滑法与长短期记忆网络算法模型服务器端代码 预测指标：财务风险、景气指数、市场风险、区域投资潜力
     * @deprecated 三大行业：房地产、汽车制造业、信息服务业
     * @author Mason
     * @throws MWException
     */
    @Deprecated
    public void ajaxReal() throws MWException {
        // 行业0-2
        Integer industry = getParaToInt("industry");
        // 预测指标0-4
        Integer Index = getParaToInt("predIndex");
        // 模型选择0-ES3；1-LSTM
        Integer modelType = getParaToInt("ModelType");
        // 预测时长1-5季度或年
        Integer time = getParaToInt("predTime");
        String input;
        Object[] Results = null;
        // 用于保存输出矩阵
        MWNumericArray output = null;
        MWNumericArray output1 = null;
        MWNumericArray output2 = null;
        MWNumericArray output3 = null;
        MWNumericArray output4 = null;
        MWNumericArray output5 = null;
        MWNumericArray output6 = null;
        if (modelType == 0) {
            // 指数平滑法
            if (Index == 0) {
                // 财务风险
                industryES3 model;
                try {
                    model = new industryES3();
                    if (industry == 0) {
                        // 房地产
                        input = dataSetPrex + "hydt/home_data.txt";
                        Results = model.ES3(6, input, time, 0.5, 0.5, 0.5, 3,
                                6, 3);
                    } else if (industry == 1) {
                        // 汽车制造业
                        input = dataSetPrex + "hydt/car_data.txt";
                        Results = model.ES3(6, input, time, 0.5, 0.5, 0.5, 4,
                                4, 4);
                    } else {
                        // 信息技术服务业
                        input = dataSetPrex + "hydt/info_data.txt";
                        Results = model.ES3(6, input, time, 0.5, 0.5, 0.5, 4,
                                4, 4);
                    }
                    output = (MWNumericArray) Results[0];// 将结果object转换成MWNumericArray
                    output1 = (MWNumericArray) Results[1];
                    output2 = (MWNumericArray) Results[2];
                    output3 = (MWNumericArray) Results[3];
                    output4 = (MWNumericArray) Results[4];
                    output5 = (MWNumericArray) Results[5];
                    int[] res = output5.rowIndex();// 从MWNumericArray对象中读取数据
                    double[] myList = new double[res.length];
                    double[] myList1 = new double[res.length];
                    double[] myList2 = new double[res.length];
                    double[] myList3 = new double[res.length];
                    double[] myList4 = new double[res.length];
                    double[] myList5 = new double[res.length];

                    for (int i = 0; i < res.length; i++) {
                        myList[i]  = Math.round(output.getFloat(i + 1)*10000)/10000.0; // 盈利
                        myList1[i] = Math.round(output1.getFloat(i + 1)*10000)/10000.0; // 经营
                        myList2[i] = Math.round(output2.getFloat(i + 1)*10000)/10000.0; // 偿债
                        myList3[i] = Math.round(output3.getFloat(i + 1)*10000)/10000.0; // 发展
                        myList4[i] = Math.round(output4.getFloat(i + 1)*10000)/10000.0; // 综合
                        myList5[i] = Math.round(output5.getFloat(i + 1)*10000)/10000.0; // 年份
                    }
                    setAttr("r1", myList);
                    setAttr("r2", myList1);
                    setAttr("r3", myList2);
                    setAttr("r4", myList3);
                    setAttr("r5", myList4);
                    setAttr("year", myList5);
                    renderJson(new String[] { "year", "r1", "r2", "r3", "r4",
                    "r5" });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (Index == 2) {
                // 区域投资潜力 三次指数平滑法 20181023
                // 加载模型、加载房地产区位投资潜力指数
                ZoneInvest model = new ZoneInvest();
                input = dataSetPrex + "/hydt/Zone-Invester_data.txt";
                Results = model.Zone_ES3(5, input, time, 0.5, 0.5, 0.5);
                output1 = (MWNumericArray) Results[0]; // 将结果object转换成MWNumericArray
                output2 = (MWNumericArray) Results[1];
                output3 = (MWNumericArray) Results[2];
                output4 = (MWNumericArray) Results[3];
                output5 = (MWNumericArray) Results[4];
                int[] res = output1.rowIndex();// 从MWNumericArray对象中读取数据
                double[] myList1 = new double[res.length], myList2 = new double[res.length], myList3 = new double[res.length], myList4 = new double[res.length], myList5 = new double[res.length];
                for (int i = 0; i < res.length; i++) {
                    myList1[i] = Math.round(output1.getFloat(i + 1)*10000)/10000.0; // 区位
                    myList2[i] = Math.round(output2.getFloat(i + 1)*10000)/10000.0; // 经济
                    myList3[i] = Math.round(output3.getFloat(i + 1)*10000)/10000.0; // 人口
                    myList4[i] = Math.round(output4.getFloat(i + 1)*10000)/10000.0; // 市场
                    myList5[i] = Math.round(output5.getFloat(i + 1)*10000)/10000.0; // 潜力

                }
                setAttr("zone", myList1);
                setAttr("financial", myList2);
                setAttr("people", myList3);
                setAttr("market", myList4);
                setAttr("invest", myList5);
                renderJson(new String[] { "zone", "financial", "people",
                        "market", "invest" });

            }
            if (Index == 3 || Index == 1) {
                // 合成指数 市场预警基于合成指数中的领先指数预测值
                HC model;
                try {
                    model = new HC();
                    if (industry == 0) {
                        // 房地产
                        input = dataSetPrex
                                + "/hydt/JingQiIndex/JingQi_real_Index.txt";
                        Results = model.ES3(4, input, time, 0.5, 0.5, 0.5,
                                13.0, 16.0, 0);
                    } else if (industry == 1) {
                        // 汽车制造业
                        input = dataSetPrex
                                + "/hydt/JingQiIndex/JingQi_car_Index.txt";
                        Results = model.ES3(4, input, time, 0.5, 0.5, 0.5, 3.0,
                                13.0, 1);
                    } else {
                        // 信息技术服务业
                        input = dataSetPrex
                                + "/hydt/JingQiIndex/JingQi_Info_Index.txt";
                        Results = model.ES3(4, input, time, 0.5, 0.5, 0.5, 8.0,
                                10.0, 2);
                    }
                    output1 = (MWNumericArray) Results[0]; // 将结果object转换成MWNumericArray
                    output2 = (MWNumericArray) Results[1];
                    output3 = (MWNumericArray) Results[2];
                    output4 = (MWNumericArray) Results[3];
                    int[] res = output1.rowIndex();// 从MWNumericArray对象中读取数据
                    double[] myList1 = new double[res.length], myList2 = new double[res.length], myList3 = new double[res.length], myList4 = new double[res.length];
                    for (int i = 0; i < res.length; i++) {
                        myList1[i] = Math.round(output1.getFloat(i + 1)*10000)/10000.0; // 同步
                        myList2[i] = Math.round(output2.getFloat(i + 1)*10000)/10000.0; // 领先
                        myList3[i] = Math.round(output3.getFloat(i + 1)*10000)/10000.0; // 滞后
                        myList4[i] = Math.round(output4.getFloat(i + 1)*10000)/10000.0; // HPY

                    }
                    setAttr("tb", myList1);
                    setAttr("lead", myList2);
                    setAttr("lag", myList3);
                    setAttr("hpy", myList4);
                    renderJson(new String[] { "tb", "lead", "lag", "hpy" });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (Index == 4) {
                // 扩散指数
                KS model;
                try {
                    model = new KS();
                    if (industry == 0) {
                        // 房地产
                        input = dataSetPrex
                                + "/hydt/JingQiIndex/JingQi_real_Index.txt";
                        Results = model.ES3(3, input, time, 0.5, 0.5, 0.5,
                                13.0, 16.0);
                    } else if (industry == 1) {
                        // 汽车制造业
                        input = dataSetPrex
                                + "/hydt/JingQiIndex/JingQi_car_Index.txt";
                        Results = model.ES3(3, input, time, 0.5, 0.5, 0.5, 3.0,
                                13.0);
                    } else {
                        // 信息技术服务业
                        input = dataSetPrex
                                + "/hydt/JingQiIndex/JingQi_Info_Index.txt";
                        Results = model.ES3(3, input, time, 0.5, 0.5, 0.5, 8.0,
                                10.0);
                    }
                    output1 = (MWNumericArray) Results[0]; // 将结果object转换成MWNumericArray
                    output2 = (MWNumericArray) Results[1];
                    output3 = (MWNumericArray) Results[2];
                    int[] res = output1.rowIndex();// 从MWNumericArray对象中读取数据
                    double[] myList1 = new double[res.length], myList2 = new double[res.length], myList3 = new double[res.length];
                    for (int i = 0; i < res.length; i++) {
                        myList1[i] = Math.round(output1.getFloat(i + 1)*10000)/10000.0; // 同步
                        myList2[i] = Math.round(output2.getFloat(i + 1)*10000)/10000.0; // 领先
                        myList3[i] = Math.round(output3.getFloat(i + 1)*10000)/10000.0; // 滞后

                    }
                    setAttr("tb", myList1);
                    setAttr("lead", myList2);
                    setAttr("lag", myList3);
                    renderJson(new String[] { "tb", "lead", "lag" });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else {
            // LSTM模型实现20181010 房地产业
            if (Index == 0) {

                // 根据行业进行预测指标历史数据的导入， by HangHang Li at20181018
                String inputIndexs = "";
                if (industry == 0)
                    inputIndexs = dataSetPrex
                    + "hydt/businessRisk/businessRisk.txt"; // 房地产市场
                else if (industry == 1)
                    inputIndexs = dataSetPrex
                    + "hydt/businessRisk/carFinancialRisk.txt"; // 汽车制造业
                else
                    inputIndexs = dataSetPrex
                    + "hydt/businessRisk/infoFinancialRisk.txt"; // 信息技术服务业

                // 财务风险：盈利、增长、偿债、经营;还需计算适度下线、偏冷线两个临界值
                // 接收数据的最大、最小值
                // 存储财务风险各单项指标的偏冷线与适度下线
                ColdHot cHots = new ColdHot();
                MWNumericArray predInDebt = MWNumericArray.newInstance(
                        new int[] { time, 1 }, MWClassID.DOUBLE,
                        MWComplexity.REAL);
                MWNumericArray predInProfit = MWNumericArray.newInstance(
                        new int[] { time, 1 }, MWClassID.DOUBLE,
                        MWComplexity.REAL);
                MWNumericArray predInBusiness = MWNumericArray.newInstance(
                        new int[] { time, 1 }, MWClassID.DOUBLE,
                        MWComplexity.REAL);
                MWNumericArray predInIncrease = MWNumericArray.newInstance(
                        new int[] { time, 1 }, MWClassID.DOUBLE,
                        MWComplexity.REAL);
                // 接收临界值计算结果
                Object[] ResDebt = null;
                Object[] ResProfit = null;
                Object[] ResBusiness = null;
                Object[] ResIncrease = null;

                // 保存偏冷及适度下限
                float[] resProfit = new float[4];
                float[] resIncrease = new float[4];
                float[] resDebt = new float[4];
                float[] resBusiness = new float[4];
                // save ResVal
                Deque<Float> predQueProfit = new ArrayDeque<Float>();
                Deque<Float> predQueIncrease = new ArrayDeque<Float>();
                Deque<Float> predQueDebt = new ArrayDeque<Float>();
                Deque<Float> predQueBusiness = new ArrayDeque<Float>();
                for (int p = 1; p < 5; p++) {
                    MinMax minMaxs = new MinMax();
                    Object[] Result = null;
                    Results = minMaxs.MinMaxScaler(2, inputIndexs, p);
                    MWNumericArray outputMin = null;
                    MWNumericArray outputMax = null;
                    outputMin = (MWNumericArray) Results[0]; // 将结果object转换成MWNumericArray
                    outputMax = (MWNumericArray) Results[1];
                    double min = Math.round(outputMin.getFloat(1)*10000)/10000.0;
                    double max = Math.round(outputMax.getFloat(1)*10000)/10000.0;
                    // System.out.println(min + "……………………" + max);

                    // 加载LSTM训练模型，根据行业加载各自模型 20181018 ((pred * (max - min) + min)*10000)/10000.0)
                    SavedModelBundle SB = null;
                    if (industry == 0)
                        SB = SavedModelBundle.load(modelPathPrex
                                + "/hydt/financialRisk/model_" + p, "mytag");
                    else if (industry == 1)
                        SB = SavedModelBundle.load(modelPathPrex
                                + "/hydt/financialRisk/carIndustry/model_" + p,
                                "mytag");
                    else
                        SB = SavedModelBundle
                        .load(modelPathPrex
                                + "/hydt/financialRisk/infoIndustry/model_"
                                + p, "mytag");
                    Session tfSession = SB.session();
                    Operation operationPredict = SB.graph().operation(
                            "rnn/preds"); // 要执行的op
                    Output outputs = new Output(operationPredict, 0);

                    // 初始化队列元素，即训练数据最后一列
                    Deque<Float> queue = new ArrayDeque<Float>();
                    // save result val
                    Deque<Float> predQue = new ArrayDeque<Float>();

                    // LSTM 模型需要加载最后一个步子的数据。这里的time_step=5或4，根据行业进行初始化赋值
                    if (industry == 0) {
                        // 房地产业
                        if (p == 1) {
                            queue.add(0.19816204334991228f);
                            queue.add(0.5298361243503276f);
                            queue.add(0.8278919745347146f);
                            queue.add(0.7761027853921891f);
                            queue.add(0.8451376282298926f);
                        }
                        if (p == 2) {
                            queue.add(1.0000000000000002f);
                            queue.add(0.6170115960870924f);
                            queue.add(0.14256865261554458f);
                            queue.add(0.29564707980990373f);
                            queue.add(0.24449770080641547f);
                        }
                        if (p == 3) {
                            // 偿债能力
                            queue.add(0.41456469726929257f);
                            queue.add(0.4520722419784474f);
                            queue.add(0.4257669281458468f);
                            queue.add(0.38015608366588005f);
                            queue.add(0.22595107727146024f);
                        }
                        if (p == 4) {
                            // 经营能力
                            queue.add(0.31339307363088215f);
                            queue.add(0.44824200150085197f);
                            queue.add(0.5147971952368775f);
                            queue.add(0.6276184487903889f);
                            queue.add(0.7437064711475722f);
                        }
                    } else if (industry == 1) {
                        // 汽车制造业
                        if (p == 1) {
                            // 盈利能力
                            queue.add(0.005309106972699956f);
                            queue.add(0.017964620304506865f);
                            queue.add(0.010522664341723914f);
                            queue.add(0.07708688381904073f);
                            queue.add(0.12404379480776483f);
                        }
                        if (p == 2) {
                            // 增长潜力
                            queue.add(0.5289412334715269f);
                            queue.add(0.5979844474658633f);
                            queue.add(0.6999709486969574f);
                            queue.add(0.8347285947436083f);
                            queue.add(1.0f);
                        }
                        if (p == 3) {
                            // 偿债能力
                            queue.add(0.08734807357175484f);
                            queue.add(0.09320827401641069f);
                            queue.add(0.09054904797885172f);
                            queue.add(0.09009346566646964f);
                            queue.add(0.10916516358383527f);
                        }
                        if (p == 4) {
                            // 经营能力
                            queue.add(0.046689006674815126f);
                            queue.add(0.45496030901966655f);
                            queue.add(0.4060541538247695f);
                            queue.add(0.6283080772620129f);
                            queue.add(1.0000000000000004f);
                        }

                    } else {
                        // 信息技术服务业
                        if (p == 1) {
                            queue.add(0.29779253432556524f);
                            queue.add(0.36821487965304195f);
                            queue.add(0.324054829090011f);
                            queue.add(0.32049502553047565f);
                            queue.add(0.34028191771712374f);
                        }
                        if (p == 2) {
                            queue.add(0.5774441281520573f);
                            queue.add(0.6564407020453682f);
                            queue.add(0.8380367071654036f);
                            queue.add(0.9696446018053202f);
                            queue.add(1.00f);
                        }
                        if (p == 3) {
                            // 偿债能力
                            queue.add(0.5193504923273591f);
                            queue.add(0.9094569928331753f);
                            queue.add(1.0f);
                            queue.add(0.7627055083672278f);
                            queue.add(0.942012297857044f);
                        }
                        if (p == 4) {
                            // 经营能力
                            queue.add(0.21604059991114966f);
                            queue.add(0.47621869554685503f);
                            queue.add(0.1781776109508435f);
                            queue.add(0.27378955517006887f);
                            queue.add(0.3645739565658097f);
                        }

                    }
                    float predValue = 0.0f;
                    // 根据预测年数进行循环
                    for (int i = 1; i <= time; i++) {
                        int n = 0;
                        float[][] a = null;
                        // 根据不同batch_size 的大小调整二维数组行数
                        if (industry == 0) {
                            if (p == 2) {
                                a = new float[3][queue.size()];
                            } else if (p == 4) {
                                a = new float[4][queue.size()];
                            } else {
                                a = new float[5][queue.size()];
                            }
                        } else if (industry == 1)
                            a = new float[5][queue.size()];
                        else
                            a = new float[4][queue.size()];

                        for (Iterator<Float> itr = queue.iterator(); itr
                                .hasNext();) {
                            a[0][n] = itr.next();
                            n++;
                        }
                        Tensor input_x = Tensor.create(a);
                        List<Tensor<?>> out = tfSession.runner()
                                .feed("inputs", input_x).fetch(outputs).run();
                        for (Tensor s : out) {
                            // 字符串数组，使用for(:)获得数据 ;根据模型时间 不的大小动态调整数组长度
                            float[][] t = null;
                            // 房地产模型训练的batch-size不一致
                            if (industry == 0) {
                                if (p == 2) {
                                    t = new float[3 * queue.size()][1];
                                } else if (p == 4) {
                                    t = new float[4 * queue.size()][1];
                                } else {
                                    t = new float[5 * queue.size()][1];
                                }
                            } else if (industry == 1)
                                t = new float[5 * queue.size()][1];
                            else
                                t = new float[4 * queue.size()][1];
                            s.copyTo(t);
                            for (float pred : t[4]) {
                                // 必须经过转化后才可得到真实预测值
                                predValue = Math.round(((pred * (max - min) + min)*10000)/10000.0);
                                queue.remove(); // 队首元素出队
                                queue.add(pred);
                            }
                        }
                        if (p == 1) {
                            predQueProfit.add(predValue);
                            predInProfit.set(new int[] { i, 1 }, predValue);
                            ResProfit = cHots.hydtColdHot(4, inputIndexs,
                                    predInProfit);
                        }
                        if (p == 2) {
                            predQueIncrease.add(predValue);
                            predInIncrease.set(new int[] { i, 1 }, predValue);
                            ResIncrease = cHots.hydtColdHot(4, inputIndexs,
                                    predInIncrease);
                        }

                        if (p == 3) {
                            predQueDebt.add(predValue);
                            predInDebt.set(new int[] { i, 1 }, predValue);
                            ResDebt = cHots.hydtColdHot(4, inputIndexs,
                                    predInDebt);
                        }
                        if (p == 4) {
                            predQueBusiness.add(predValue);
                            predInBusiness.set(new int[] { i, 1 }, predValue);
                            ResBusiness = cHots.hydtColdHot(4, inputIndexs,
                                    predInBusiness);

                        }

                    }
                    for (int q = 0; q < 2; q++) {
                        // 一种容易得到矩阵返回一维数据的取值方式
                        if (p == 1) {
                            float profit = ((MWNumericArray) ResProfit[q])
                                    .getFloat();
                            resProfit[q] = profit;
                        }
                        if (p == 2) {
                            float increase = ((MWNumericArray) ResIncrease[q])
                                    .getFloat();
                            resIncrease[q] = increase;
                        }
                        if (p == 3) {
                            float debt = ((MWNumericArray) ResDebt[q])
                                    .getFloat();
                            resDebt[q] = debt;
                        }
                        if (p == 4) {
                            float business = ((MWNumericArray) ResBusiness[q])
                                    .getFloat();
                            resBusiness[q] = business;
                        }
                    }
                }
                // 返回偏冷线与适度下限
                setAttr("resProfit", resProfit);
                setAttr("resIncrease", resIncrease);
                setAttr("resDebt", resDebt);
                setAttr("resBusiness", resBusiness);

                System.out.println(predQueProfit);
                // 返回财务风险预测结果
                setAttr("profitIndex", predQueProfit);
                setAttr("increaseIndex", predQueIncrease);
                setAttr("debtIndex", predQueDebt);
                setAttr("businessIndex", predQueBusiness);

                renderJson(new String[] { "resProfit", "resIncrease",
                        "resDebt", "resBusiness", "profitIndex",
                        "increaseIndex", "debtIndex", "businessIndex" });
            }

            if (Index == 2) {
                // 房地产行业投资潜力 20181022 区位因子、经济因子、人口因子、市场供需因子、房地产投资潜力
                String inputIndex = dataSetPrex + "hydt/Zone-Invester_data.txt"; // 区位因子训练数据
                // save result val
                Deque<Float> predQueQ = new ArrayDeque<Float>();
                Deque<Float> predQueJ = new ArrayDeque<Float>();
                Deque<Float> predQueR = new ArrayDeque<Float>();
                Deque<Float> predQueS = new ArrayDeque<Float>();
                Deque<Float> predQueI = new ArrayDeque<Float>();
                for (int p = 0; p < 5; p++) {
                    MinMax minMaxs = new MinMax();
                    Object[] Result = null;
                    Results = minMaxs.MinMaxScaler(2, inputIndex, p + 1);
                    MWNumericArray outputMin = null;
                    MWNumericArray outputMax = null;
                    outputMin = (MWNumericArray) Results[0];
                    outputMax = (MWNumericArray) Results[1];
                    float min = outputMin.getFloat(1);
                    float max = outputMax.getFloat(1);
                    System.out.println(min + "******" + max);
                    // 加载LSTM训练模型
                    SavedModelBundle SB = null;
                    SB = SavedModelBundle.load(modelPathPrex
                            + "/hydt/Zone_Invest/model_" + (p + 1), "mytag");
                    Session tfSession = SB.session();
                    Operation operationPredict = SB.graph().operation(
                            "rnn/preds"); // 要执行的op
                    Output outputs = new Output(operationPredict, 0);

                    // 初始化队列元素，即训练数据最后一列
                    Deque<Float> queue = new ArrayDeque<Float>();
                    // save result val
                    Deque<Float> predQue = new ArrayDeque<Float>();

                    if (p == 0) {
                        queue.add(0.5034819009932718f);
                        queue.add(0.0f);
                        queue.add(0.8536570678678572f);
                        queue.add(0.8106905006321554f);
                        queue.add(0.7581823170504194f);
                    }
                    if (p == 1) {
                        queue.add(0.3966452719522806f);
                        queue.add(0.3695221140090297f);
                        queue.add(0.5277861561816763f);
                        queue.add(0.473322827982102f);
                        queue.add(0.53795979197902f);
                    }
                    if (p == 2) {
                        queue.add(0.40203175841838856f);
                        queue.add(0.5284635009890621f);
                        queue.add(0.6301238668347366f);
                        queue.add(0.7800377679586603f);
                        queue.add(0.8259661953024278f);
                    }
                    if (p == 3) {
                        queue.add(0.4764315530085952f);
                        queue.add(0.38087158836624724f);
                        queue.add(0.5420147211563773f);
                        queue.add(0.0f);
                        queue.add(0.9344236224474969f);
                    }
                    if (p == 4) {
                        queue.add(0.4172190978402597f);
                        queue.add(0.0f);
                        queue.add(0.7717069455450343f);
                        queue.add(0.6744452686357363f);
                        queue.add(0.7808614481845808f);
                    }

                    float predValue = 0.0f;
                    // 根据预测年数进行循环
                    for (int i = 1; i <= time; i++) {
                        int n = 0;
                        float[][] a = new float[5][5];
                        for (Iterator<Float> itr = queue.iterator(); itr
                                .hasNext();) {
                            a[0][n] = itr.next();
                            n++;
                        }
                        Tensor input_x = Tensor.create(a);
                        List<Tensor<?>> out = tfSession.runner()
                                .feed("inputs", input_x).fetch(outputs).run();
                        for (Tensor s : out) {
                            // 字符串数组，使用for(:)获得数据
                            float[][] t = new float[25][1];
                            s.copyTo(t);
                            for (float pred : t[4]) {
                                // 必须经过转化后才可得到真实预测值 
                                predValue = Math.round(((pred * (max - min) + min)*10000)/10000.0);
                                queue.remove();// 队首元素出队
                                queue.add(pred);
                            }
                        }
                        if (p == 0) {
                            predQueQ.add(predValue);
                        }
                        if (p == 1)
                            predQueJ.add(predValue);
                        if (p == 2)
                            predQueR.add(predValue);
                        if (p == 3)
                            predQueS.add(predValue);
                        if (p == 4)
                            predQueI.add(predValue);
                    }

                }
                setAttr("zoneIndex", predQueQ);
                setAttr("financialIndex", predQueJ);
                setAttr("peopleIndex", predQueR);
                setAttr("marketIndex", predQueS);
                setAttr("investIndex", predQueI);
                // 返回区位指数预测结果
                renderJson(new String[] { "zoneIndex", "financialIndex",
                        "peopleIndex", "marketIndex", "investIndex" });
            }

            /**
             * 计算合成指数以及市场预警
             */
            if (Index == 3 || Index == 1) {
                // 景气指数中的合成指数：领先、同步、滞后、HPY
                // 接收数据的最大、最小值
                String inputIndex;
                if (industry == 0)
                    inputIndex = dataSetPrex
                    + "hydt/JingQiIndex/JingQi_finall_Index.txt"; // 房地产市场
                else if (industry == 1)
                    inputIndex = dataSetPrex
                    + "hydt/JingQiIndex/car_JingQi_finall_Index.txt"; // 汽车制造业
                else
                    inputIndex = dataSetPrex
                    + "hydt/JingQiIndex/info_JingQi_finall_Index.txt"; // 信息技术服务业

                // 存储预警预测结果偏冷、偏热线等
                MWNumericArray predIn = MWNumericArray.newInstance(new int[] {
                        time, 1 }, MWClassID.DOUBLE, MWComplexity.REAL);

                // save result val
                Deque<Double> predQueL = new ArrayDeque<Double>();
                Deque<Double> predQueT = new ArrayDeque<Double>();
                Deque<Double> predQueZ = new ArrayDeque<Double>();
                Deque<Double> predQueH = new ArrayDeque<Double>();
                for (int p = 0; p < 4; p++) {
                    MinMax minMaxs = new MinMax();
                    Object[] Result = null;
                    Results = minMaxs.MinMaxScaler(2, inputIndex, p + 1);
                    MWNumericArray outputMin = null;
                    MWNumericArray outputMax = null;
                    outputMin = (MWNumericArray) Results[0]; // 将结果object转换成MWNumericArray
                    outputMax = (MWNumericArray) Results[1];
                    float min = outputMin.getFloat(1);
                    float max = outputMax.getFloat(1);
                    System.out.println(min + "******" + max);
                    // 加载LSTM训练模型
                    SavedModelBundle SB = null;
                    if (industry == 0)
                        // 房地产
                        SB = SavedModelBundle
                        .load(modelPathPrex
                                + "/hydt/JingQi_MarketRisk/model_" + p,
                                "mytag");
                    else if (industry == 1)
                        // 汽车制造业
                        SB = SavedModelBundle
                        .load(modelPathPrex
                                + "/hydt/JingQi_MarketRisk/carIndustry_hecheng/model_"
                                + (p + 1), "mytag");
                    else
                        // 信息技术服务业
                        SB = SavedModelBundle
                        .load(modelPathPrex
                                + "/hydt/JingQi_MarketRisk/InfoIndustry_hecheng/model_"
                                + (p + 1), "mytag");
                    Session tfSession = SB.session();
                    Operation operationPredict = SB.graph().operation(
                            "rnn/preds"); // 要执行的op
                    Output outputs = new Output(operationPredict, 0);

                    // 初始化队列元素，即训练数据最后一列
                    Deque<Float> queue = new ArrayDeque<Float>();
                    // save result val
                    Deque<Float> predQue = new ArrayDeque<Float>();
                    if (industry == 0) {
                        if (p == 0) {
                            queue.add(0.2984940142146755f);
                            queue.add(0.29307985123528013f);
                            queue.add(0.24918521130146942f);
                            queue.add(0.200000000000000f);
                            queue.add(0.219999999999999f);
                        }
                        if (p == 1) {
                            queue.add(0.6238169157899023f);
                            queue.add(0.5651990667341238f);
                            queue.add(0.5624293086506924f);
                            queue.add(0.388888888888883f);
                            queue.add(0.388888888888883f);
                        }
                        if (p == 2) {
                            queue.add(0.6918663566556589f);
                            queue.add(0.7156781310964391f);
                            queue.add(0.7546369272327169f);
                            queue.add(0.842105263157896f);
                            queue.add(0.842105263157896f);
                        }
                        if (p == 3) {
                            queue.add(0.5214401294498372f);
                            queue.add(0.528317152103559f);
                            queue.add(0.4591423948220066f);
                            queue.add(0.335355987055016f);
                            queue.add(0.260922330097087f);

                        }
                    } else if (industry == 1) {

                        if (p == 0) {
                            queue.add(0.4460945695182543f);
                            queue.add(0.43913349957875614f);
                            queue.add(0.4095382164179924f);
                            queue.add(0.3798646738803928f);
                            queue.add(0.3733876155864815f);
                        }
                        if (p == 1) {
                            queue.add(0.4527227017782476f);
                            queue.add(0.43920300972164483f);
                            queue.add(0.41016213030265636f);
                            queue.add(0.356547865182927f);
                            queue.add(0.28525093346891417f);
                        }
                        if (p == 2) {
                            queue.add(0.5055565763555769f);
                            queue.add(0.518219790224312f);
                            queue.add(0.5530845496738501f);
                            queue.add(0.6043849001687551f);
                            queue.add(0.6553420475990008f);

                        }
                        if (p == 3) {
                            queue.add(0.5877549927321404f);
                            queue.add(0.5492435238582516f);
                            queue.add(0.5998436302330328f);
                            queue.add(0.4072047737560771f);
                            queue.add(0.4895928188069121f);
                        }
                    } else {
                        if (p == 0) {
                            queue.add(0.7368284536084291f);
                            queue.add(0.7365417620037613f);
                            queue.add(0.7892224485723158f);
                            queue.add(0.3047849435529546f);
                            queue.add(0.14986361940020565f);
                        }
                        if (p == 1) {
                            queue.add(0.4819416001954471f);
                            queue.add(0.5003112368493721f);
                            queue.add(0.5223679765030118f);
                            queue.add(1.0f);
                            queue.add(0.9006658025669303f);
                        }
                        if (p == 2) {
                            queue.add(0.1685043237449122f);
                            queue.add(0.1430724208754608f);
                            queue.add(0.0439064751257785f);
                            queue.add(0.0f);
                            queue.add(0.3400425918386638f);
                        }
                        if (p == 3) {
                            queue.add(0.6301653951142914f);
                            queue.add(0.49866001029393736f);
                            queue.add(0.0f);
                            queue.add(1.0f);
                            queue.add(0.6556335855601638f);

                        }
                    }

                    double predValue = 0.0;
                    // 根据预测年数进行循环
                    for (int i = 1; i <= time; i++) {
                        int n = 0;
                        float[][] a = new float[5][5];
                        for (Iterator<Float> itr = queue.iterator(); itr
                                .hasNext();) {
                            a[0][n] = itr.next();
                            n++;
                        }
                        Tensor input_x = Tensor.create(a);
                        List<Tensor<?>> out = tfSession.runner()
                                .feed("inputs", input_x).fetch(outputs).run();
                        for (Tensor s : out) {
                            // 字符串数组，使用for(:)获得数据
                            float[][] t = new float[25][1];
                            s.copyTo(t);
                            for (float pred : t[4]) {
                                // 必须经过转化后才可得到真实预测值
                                predValue = Math.round(((pred * (max - min) + min)*10000)/10000.0);
                                queue.remove();// 队首元素出队
                                queue.add(pred);
                            }
                        }
                        if (p == 0) {
                            predQueL.add(predValue);
                            predIn.set(new int[] { i, 1 }, predValue);
                        }
                        if (p == 1)
                            predQueT.add(predValue);
                        if (p == 2)
                            predQueZ.add(predValue);
                        if (p == 3)
                            predQueH.add(predValue);
                    }

                }
                setAttr("leadIndex", predQueL);
                setAttr("tbIndex", predQueT);
                setAttr("lagIndex", predQueZ);
                setAttr("hpyIndex", predQueH);
                // 市场风险预测执行计算临界值
                if (Index == 1) {
                    // 如果计算行业预警信息，可通过MATLAB编译包ColdHot进行计算;输出偏冷线\偏热线\适度上限\适度下限
                    ColdHot cHot = new ColdHot();
                    // 得到偏冷、偏热线、上下限
                    Results = cHot.hydtColdHot(4, inputIndex, predIn);
                    double[] rest = new double[4];
                    for (int q = 0; q < 4; q++) {
                        // 一种容易得到矩阵返回一维数据的取值方式
                        MWNumericArray vals = (MWNumericArray) Results[q];
                        double a = vals.getFloat();
                        rest[q] = Math.round(a*10000)/10000.0;
                    }
                    setAttr("res", rest);
                    renderJson(new String[] { "leadIndex", "tbIndex",
                            "lagIndex", "hpyIndex", "res" });

                } else
                    // 返回合成指数预测结果
                    renderJson(new String[] { "leadIndex", "tbIndex",
                            "lagIndex", "hpyIndex" });
            }

            // 扩散指数：领先、同步、滞后 at 20181011 by HangHang Lee
            if (Index == 4) {
                String inputIndex;
                if (industry == 0)
                    inputIndex = dataSetPrex
                    + "hydt/JingQiIndex/JingQi_diffusion_finall_Index.txt"; // 房地产市场扩散指数最终指标
                else if (industry == 1)
                    inputIndex = dataSetPrex
                    + "hydt/JingQiIndex/car_JingQi_diffusion_finall_Index.txt"; // 汽车制造业市场扩散指数最终指标
                else
                    inputIndex = dataSetPrex
                    + "hydt/JingQiIndex/info_JingQi_diffusion_finall_Index.txt"; // 信息技术服务业市场扩散指数最终指标

                // save result val
                Deque<Double> predQueL = new ArrayDeque<Double>();
                Deque<Double> predQueT = new ArrayDeque<Double>();
                Deque<Double> predQueZ = new ArrayDeque<Double>();
                for (int p = 0; p < 3; p++) {
                    MinMax minMaxs = new MinMax();
                    Object[] Result = null;
                    Results = minMaxs.MinMaxScaler(2, inputIndex, p + 1);
                    MWNumericArray outputMin = null;
                    MWNumericArray outputMax = null;
                    outputMin = (MWNumericArray) Results[0]; // 将结果object转换成MWNumericArray
                    outputMax = (MWNumericArray) Results[1];
                    double min = Math.round(outputMin.getFloat(1)*10000)/10000.0;
                    double max = Math.round(outputMax.getFloat(1)*10000)/10000.0;
                    System.out.println(min + "*****" + max);
                    // 加载LSTM训练模型
                    SavedModelBundle SB = null;
                    if (industry == 0)
                        SB = SavedModelBundle
                        .load(modelPathPrex
                                + "/hydt/JingQi_MarketRisk/diffusionIndex/model_"
                                + p, "mytag");
                    else if (industry == 1)
                        SB = SavedModelBundle
                        .load(modelPathPrex
                                + "/hydt/JingQi_MarketRisk/car_diffusion/model_"
                                + (p + 1), "mytag");
                    else
                        SB = SavedModelBundle
                        .load(modelPathPrex
                                + "/hydt/JingQi_MarketRisk/Info_diffusion/model_"
                                + (p + 1), "mytag");

                    Session tfSession = SB.session();
                    Operation operationPredict = SB.graph().operation(
                            "rnn/preds"); // 要执行的op
                    Output outputs = new Output(operationPredict, 0);

                    // 初始化队列元素，即训练数据最后一列
                    Deque<Float> queue = new ArrayDeque<Float>();
                    if (industry == 0) {
                        if (p == 0) {
                            queue.add(0.6366279069767441f);
                            queue.add(0.7267441860465116f);
                            queue.add(0.1816860465116279f);
                            queue.add(0.5450581395348837f);
                            queue.add(0.5450581395348837f);
                        }
                        if (p == 1) {
                            queue.add(0.400520156046814f);
                            queue.add(0.5006501950585175f);
                            queue.add(0.5006501950585175f);
                            queue.add(0.1001300390117035f);
                            queue.add(0.400520156046814f);
                        }
                        if (p == 2) {
                            queue.add(0.42857142857142855f);
                            queue.add(0.5714285714285714f);
                            queue.add(0.6422857142857143f);
                            queue.add(0.2857142857142857f);
                            queue.add(0.2137142857142857f);
                        }
                    } else if (industry == 1) {
                        // 汽车制造业 领先 同步 滞后 HPY
                        if (p == 0) {
                            queue.add(0.8571428571428571f);
                            queue.add(0.8571428571428571f);
                            queue.add(0.2857142857142857f);
                            queue.add(0.42857142857142855f);
                            queue.add(1.0f);
                        }
                        if (p == 1) {
                            queue.add(0.6666666666666666f);
                            queue.add(1.0f);
                            queue.add(0.6666666666666666f);
                            queue.add(0.3333333333333333f);
                            queue.add(0.3333333333333333f);
                        }
                        if (p == 2) {
                            queue.add(1.0f);
                            queue.add(0.8888888888888891f);
                            queue.add(0.555555555555555f);
                            queue.add(1.0f);
                            queue.add(0.8888888888888891f);
                        }
                    } else {
                        // 信息技术服务业
                        if (p == 0) {
                            queue.add(0.33333333333333337f);
                            queue.add(0.6666666666666667f);
                            queue.add(0.6666666666666667f);
                            queue.add(0.8333333333333334f);
                            queue.add(0.33333333333333337f);
                        }
                        if (p == 1) {
                            queue.add(0.33333333333333337f);
                            queue.add(0.33333333333333337f);
                            queue.add(0.8333333333333334f);
                            queue.add(0.0f);
                            queue.add(0.33333333333333337f);
                        }
                        if (p == 2) {
                            queue.add(0.5f);
                            queue.add(0.16666666666666666f);
                            queue.add(0.5f);
                            queue.add(0.5f);
                            queue.add(0.5f);
                        }
                    }

                    double predValue = 0.0f;
                    // 根据预测年数进行循环
                    for (int i = 1; i <= time; i++) {
                        int n = 0;

                        float[][] a = null;
                        if (industry == 0 || industry == 2)
                            a = new float[5][5];
                        else if (industry == 1)
                            a = new float[2][5];
                        for (Iterator<Float> itr = queue.iterator(); itr
                                .hasNext();) {
                            a[0][n] = itr.next();
                            n++;
                        }
                        Tensor input_x = Tensor.create(a);
                        List<Tensor<?>> out = tfSession.runner()
                                .feed("inputs", input_x).fetch(outputs).run();
                        for (Tensor s : out) {
                            // 字符串数组，使用for(:)获得数据
                            float[][] t = null;
                            if (industry == 0 || industry == 2)
                                t = new float[5 * queue.size()][1];
                            if (industry == 1)
                                t = new float[2 * queue.size()][1];
                            s.copyTo(t);
                            for (float pred : t[4]) {
                                // 必须经过转化后才可得到真实预测值
                                predValue = Math.round((pred * (max - min) + min)*10000)/10000.0;
                                queue.remove();// 队首元素出队
                                queue.add(pred);
                            }
                        }
                        if (p == 0)
                            predQueL.add(Math.round(predValue*10000)/10000.0);
                        if (p == 1)
                            predQueT.add(Math.round(predValue*10000)/10000.0);
                        if (p == 2)
                            predQueZ.add(Math.round(predValue*10000)/10000.0);

                    }
                }
                setAttr("leadIndex", predQueL);
                setAttr("tbIndex", predQueT);
                setAttr("lagIndex", predQueZ);
                // 返回扩散指数预测结果
                renderJson(new String[] { "leadIndex", "tbIndex", "lagIndex" });

            }
        }

    }


    // 房地产合成指数数据0326
    public void getCI() {
        int type = getParaToInt("type");
        List<Market> cIdata = Market.ci.getCi(type);
        String cJson = JsonKit.toJson(cIdata);
        renderJson(cJson);
    }


    // 房地产扩散指数数据0326
    public void getDiffIndex() {
        // 以行业进行数据查询
        int type = getParaToInt("type");
        List<Market> diffIdata = Market.diffusion.getDiffusionIndex(type);
        String diffJson = JsonKit.toJson(diffIdata);
        // System.out.println(diffJson);
        renderJson(diffJson);
    }


    // 房地产风险预警数据0328
    public void getRiskPreAlarming() {
        int type = getParaToInt("type");
        List<Market> Riskdata = Market.ci.getRiskPreAlarming(type);
        String RiskJson = JsonKit.toJson(Riskdata);
        // System.out.println(RiskJson);
        renderJson(RiskJson);
    }


    // 获取区域数据
    public void getZone() {

        List<InvestPotential> get_zone = InvestPotential.dao.getZone();
        renderJson(JsonKit.toJson(get_zone));// 地区数据

    }


    public void getProvince() {
        String zone = getPara("zone");
        List<InvestPotential> get_province = InvestPotential.dao
                .getProvince(zone);
        renderJson(JsonKit.toJson(get_province));

    }


    public void getCity() {
        String province = getPara("province");
        List<InvestPotential> get_city = InvestPotential.dao.getCity(province);
        renderJson(JsonKit.toJson(get_city));

    }


    public void getFactorData() {
        String city = getPara("city");
        List<InvestFactor> get_factor = InvestFactor.dao.getDatas(city);
        renderJson(JsonKit.toJson(get_factor));

    }


    // 按发展速度划分
    // 获取区域数据
    public void getDevLevel() {
        List<InvestPotential> get_level_zone = InvestPotential.dao
                .devlopLevel();
        renderJson(JsonKit.toJson(get_level_zone));// 地区数据

    }


    public void getCitys() {
        String level = getPara("level");
        List<InvestPotential> get_citys = InvestPotential.dao.getCitys(level);
        renderJson(JsonKit.toJson(get_citys));

    }


    // 行业动态页面
    @Before(com.ccip.bank.interceptor.UserAuthInterceptor.class)
    public void hydt() throws URISyntaxException {
        render("hydt.html");
    }


    // 信用评级页面
    @Before(com.ccip.bank.interceptor.UserAuthInterceptor.class)
    public void xypj() {
        render("xypj.html");
    }


    // update 0410
    public void creditQuality() throws MWException, IOException {
        // 接受测试参数.【caution！！】需要在form表单中将字段name改为Bean名首字母小写.字段名，如本表单：creditQuality.Cost
        creditQualityBean paraBean = getBean(creditQualityBean.class);
        // System.out.println(paraBean);
        // 实例化信用评级模型对象
        creditQuality CQ = new creditQuality();
        String input = dataSetPrex + "Data554.mat";

        // 180525 增加ANN进行等级预测
        test grade = new test();
        String input_ANN = dataSetPrex + "xypj/ANN_model.mat";

        // 初始化1*6矩阵
        MWNumericArray test_data = MWNumericArray.newInstance(
                new int[] { 1, 6 }, MWClassID.DOUBLE, MWComplexity.REAL);
        Object[] result = null;
        // 组装投入数据为矩阵
        test_data.set(new int[] { 1, 1 }, paraBean.getDebt_rate());
        test_data.set(new int[] { 1, 2 }, paraBean.getInterest());
        test_data.set(new int[] { 1, 3 }, paraBean.getInventoryTurn());
        test_data.set(new int[] { 1, 4 }, paraBean.getFlowDebt());
        test_data.set(new int[] { 1, 5 }, paraBean.getCost());
        test_data.set(new int[] { 1, 6 }, paraBean.getOwnerEquity());
        // 使用人工神经网络得到等级概率
        Object[] test = grade.guid(2, input_ANN, test_data);
        result = CQ.trainClassifier_Tree(3, input, test_data);
        // 得到公司信用评级
        MWNumericArray cqNum = (MWNumericArray) result[2];
        // System.out.print(cqNum);
        List lst = new ArrayList();
        lst.add(cqNum.getInt());
        // 读取mat文件
        MatFileReader read = new MatFileReader(input);
        MLArray mlArray = read.getMLArray("Data");// 获取mat中Data矩阵变量的内容
        MLDouble d = (MLDouble) mlArray;
        HashMap<String, double[]> map1 = new HashMap<String, double[]>();
        double[][] matrix = (d.getArray());
        int len = matrix.length;
        double[] debt = new double[len];
        double[] interst = new double[len];
        double[] turn = new double[len];
        double[] rate = new double[len];
        double[] profit = new double[len];
        double[] owner = new double[len];
        for (int i = 0; i < len; i++) {
            debt[i] = matrix[i][0];
            interst[i] = matrix[i][1];
            turn[i] = matrix[i][2];
            rate[i] = matrix[i][3];
            profit[i] = matrix[i][4];
            owner[i] = matrix[i][5];
        }
        map1.put("debt", debt);
        map1.put("interest", interst);
        map1.put("turn", turn);
        map1.put("rate", rate);
        map1.put("profit", profit);
        map1.put("owner", owner);
        setAttr("map", map1);
        setAttr("lst", lst);
        setAttr("para", paraBean);
        setAttr("ANNRes", test[1].toString());
        String matrixData = JsonKit.toJson(map1);
        renderJson(new String[] { "map", "lst", "para", "ANNRes" });
    }


    /**
     * @author Mason Hang
     * @date 20190104
     * @deprecated 下载信用评级模板文件
     */
    @Deprecated
    @Before(com.ccip.bank.interceptor.UserAuthInterceptor.class)
    public void downLoad() {
        File file = new File(paths + "/download/creditGrade_Templet.csv");
        System.out.println(paths);
        if (file.exists()) {
            renderFile(file);
        } else {
            renderJson("文件不存在");
        }
    }


    // 风险评估页面
    @Before(com.ccip.bank.interceptor.UserAuthInterceptor.class)
    public void fxpg() {
        System.out.println(PathKit.getWebRootPath());
        render("fxpg.html");
    }


    // 财务风险0503 update 0507
    public void financial_risk_model() throws MWException {		
        financialRiskBean paraBean = getBean(financialRiskBean.class);
        MWNumericArray in = MWNumericArray.newInstance(new int[] { 11, 1 },
                MWClassID.DOUBLE, MWComplexity.REAL);
        in.set(new int[] { 1, 1 }, paraBean.getFlowAssertRate());
        in.set(new int[] { 2, 1 }, paraBean.getCheckRate());

        in.set(new int[] { 3, 1 }, paraBean.getCreditGrade());
        in.set(new int[] { 4, 1 }, paraBean.getFinancialCostRate());
        in.set(new int[] { 5, 1 }, paraBean.getEquityRatio());

        in.set(new int[] { 6, 1 }, paraBean.getFlowPercent());
        in.set(new int[] { 7, 1 }, paraBean.getDebtRate());
        in.set(new int[] { 8, 1 }, paraBean.getInterest());
        in.set(new int[] { 9, 1 }, paraBean.getCashFlow());

        in.set(new int[] { 10, 1 }, paraBean.getGrowthRateOperateIncome());
        in.set(new int[] { 11, 1 }, paraBean.getAllAsserrtIncrease());
        String input = dataSetPrex + "fxpg/2014.csv";
        String input_text = dataSetPrex + "fxpg/financial_index.txt";
        testRisk testRisk = new testRisk();
        Object[] financialRes = null;
        financialRes = testRisk.financial(2, input, input_text, in);
        // get Result
        setAttr("firstlyIndex", financialRes[1].toString());
        setAttr("secondaryIndex", financialRes[0].toString());
        renderJson(new String[] { "secondaryIndex", "firstlyIndex" });

    }

    // 0417风险等级评估模型 竞争风险算法
    public void fxpg_competition_model() throws MWException {
        int num1 = getParaToInt("num1"); // 竞品
        int num2 = getParaToInt("num2");// 对外投资
        MWNumericArray in = MWNumericArray.newInstance(new int[] { 2, 1 },
                MWClassID.DOUBLE, MWComplexity.REAL);
        in.set(new int[] { 1, 1 }, num1);
        in.set(new int[] { 2, 1 }, num2);
        String input = dataSetPrex + "fxpg/2014.csv";
        String input_text = dataSetPrex + "fxpg/financial_index.txt";
        testRisk testRisk = new testRisk();
        Object[] Res = null;
        Res = testRisk.competion(1, input, input_text, in);
        renderJson("firstlyIndex", Res[0].toString());
    }

    // 0417风险等级评估模型算法--技术风险评估 update at 20180504
    public void jszl_model() throws MWException {
        int num1 = getParaToInt("num1");
        int num2 = getParaToInt("num2");
        int num3 = getParaToInt("num3");
        MWNumericArray in = MWNumericArray.newInstance(new int[] { 3, 1 },
                MWClassID.DOUBLE, MWComplexity.REAL);
        in.set(new int[] { 1, 1 }, num1);
        in.set(new int[] { 2, 1 }, num2);
        in.set(new int[] { 3, 1 }, num3);
        String input = dataSetPrex + "fxpg/2014.csv";
        String input_text = dataSetPrex + "fxpg/financial_index.txt";
        testRisk testRisk = new testRisk();
        Object[] Res = null;
        Res = testRisk.tec(1, input, input_text, in);
        renderJson("firstlyIndex", Res[0].toString());
    }


    // 0417风险等级评估模型算法--经营风险评估 update param at 20180502 1-11
    public void org_risk_model() throws MWException {
        businessBean paraBean = getBean(businessBean.class);
        // 初始化矩阵
        MWNumericArray in = MWNumericArray.newInstance(new int[] { 14, 1 },
                MWClassID.DOUBLE, MWComplexity.REAL);
        in.set(new int[] { 1, 1 }, paraBean.getNetSaleRate());
        in.set(new int[] { 2, 1 }, paraBean.getTotalAssetRewardRate());
        in.set(new int[] { 3, 1 }, paraBean.getNetAssetYield());
        in.set(new int[] { 4, 1 }, paraBean.getInventoryTurnover());
        in.set(new int[] { 5, 1 }, paraBean.getTotalAssetTurnover());
        in.set(new int[] { 6, 1 }, paraBean.getCostProfitMargin());
        in.set(new int[] { 7, 1 }, paraBean.getLitigationNum());
        in.set(new int[] { 8, 1 }, paraBean.getAbOperNum());
        in.set(new int[] { 9, 1 }, paraBean.getAdminSancteNum());
        in.set(new int[] { 10, 1 }, paraBean.getChattelPledgeNum());
        in.set(new int[] { 11, 1 }, paraBean.getSelfRiskNum());
        in.set(new int[] { 12, 1 }, paraBean.getPeripheralRiskNum());
        in.set(new int[] { 13, 1 }, paraBean.getShareholdNum());
        in.set(new int[] { 14, 1 }, paraBean.getFirstMaxShare());
        String input = dataSetPrex + "fxpg/2014.csv";
        String input_text = dataSetPrex + "fxpg/financial_index.txt";
        testRisk testRisk = new testRisk();
        Object[] Res = null;
        Res = testRisk.business(2, input, input_text, in);
        System.out.println(Res[0].toString() + "==" + Res[1].toString());
        setAttr("firstlyIndex", Res[1].toString());
        setAttr("secondaryIndex", Res[0].toString());
        renderJson(new String[] { "secondaryIndex", "firstlyIndex" });
    }

    // 企业风险评估综合指标0504
    public void ajaxIntegrateIndex() throws MWException {
        testRisk testRisk = new testRisk();
        Object[] result = testRisk.Result(5);// 得到所有一级二级指标
        System.out.println(result[1]);
        if (result[0].toString().length() == 1) {
            setAttr("secondaryIndex", 0);
            setAttr("firstlyIndex", 0);
            renderJson(new String[] { "secondaryIndex", "firstlyIndex" });
        } else {
            String data[] = { result[1].toString(), result[2].toString(),
                    result[3].toString(), result[4].toString() };
            setAttr("secondaryIndex", data);
            // System.out.println(data);
            setAttr("firstlyIndex", result[0].toString());
            renderJson(new String[] { "secondaryIndex", "firstlyIndex" });
        }

    }

    // 科研投入页面
    @Before(com.ccip.bank.interceptor.UserAuthInterceptor.class)
    public void kytr() {
        System.out.println(System.getProperty("java.library.path"));
        render("kytr.html");
    }

    // 0405 实现 0425update java dea
    public void kytr_model() throws MWException, LpSolveException {
        // 接受测试参数
        ScienceInvest paraBean = getBean(ScienceInvest.class);
        // 基于Java实现DEA算法
        Map<String, DeaRecord> records = new LinkedHashMap<>();
        GetExcelInfo obj = new GetExcelInfo();
        File file = new File(dataSetPrex + "zhibiao.xls");
        records = obj.readExcel(file);
        double[] input = new double[4];
        double[] output = new double[6];
        input[0] = paraBean.getSci_invest();
        input[1] = paraBean.getSci_invest() / paraBean.getReceipt_num();
        input[2] = paraBean.getEdu_num();
        input[3] = paraBean.getWork_num();

        output[0] = paraBean.getSoft_num();
        output[1] = paraBean.getPatent_num();
        output[2] = paraBean.getBrand_num();
        output[3] = paraBean.getWorks_num();
        output[4] = paraBean.getWeb_num();
        output[5] = paraBean.getProfit_num();
        records.put("new", new DeaRecord(output, input));
        DataEnvelopmentAnalysis dea = new DataEnvelopmentAnalysis();
        Map<String, Double> results = null;
        results = dea.estimateEfficiency(records);
        renderJson("ret", (new TreeMap<>(results)).get("new"));

    }

    /**
     * 企业科研投入，基于深度学习技术：记忆网络的实现
     * 
     * @return grade
     * @date 20181023
     * @author Mason
     * @throws MWException
     * 
     */
    public void MemoryNetModel() throws MWException {
        long predVal = 0;
        ScienceInvest paraBean = getBean(ScienceInvest.class); // 接收前台数据
        double[] inputVal = new double[9];
        inputVal[0] = paraBean.getSci_invest();
        inputVal[1] = (paraBean.getSci_invest() / paraBean.getReceipt_num()); // 营业收入占比
        inputVal[2] = (paraBean.getEdu_num() / paraBean.getWork_num()); // 本科及以上学历占总人数比例
        inputVal[3] = (paraBean.getSoft_num());
        inputVal[4] = paraBean.getPatent_num();
        inputVal[5] = paraBean.getBrand_num();
        inputVal[6] = paraBean.getWorks_num();
        inputVal[7] = paraBean.getWeb_num();
        inputVal[8] = paraBean.getProfit_num();
        // 对输入数据进行L2正则化预处理 20181025 HangHang Li
        String raw_data = dataSetPrex + "kytr/raw_data.txt";
        L2 normal = new L2();
        Object[] res = null;
        res = normal.L2Normal(1, raw_data, inputVal);
        double[] Val = (double[]) ((MWNumericArray) res[0]).getData();
        float[] input = new float[Val.length];
        for (int i = 0; i < Val.length; i++) {
            input[i] = (float) Val[i];
        }
        TFModelPred tm = new TFModelPred(); // 加载模型处理函数
        String modelPath = modelPathPrex + "kytr/pred-model.pb";
        predVal = tm.KnowledgePredict(modelPath, input);
        System.out.println("您所预测公司的科研投入等级为：" + predVal);
        renderJson(predVal);

    }

    // 贷后预警页面
    @Before(com.ccip.bank.interceptor.UserAuthInterceptor.class)
    public void dhyj() {
        render("dhyj.html");
    }

    @Before(com.ccip.bank.interceptor.UserAuthInterceptor.class)
    public void dhyjContent() {
        String code = getPara("num");
        String cname = getPara("name");
        setAttr("cname", cname);
        setAttr("code", code);
        render("dhyjContent.html");
    }

    public void renderPageForLayUI(Page<?> page) {
        renderPageForLayUI(page, 0, "");
    }

    /**
     * 按照layUI格式分页获取数据
     * 
     * @param page
     * @param code
     * @param message
     */
    public void renderPageForLayUI(Page<?> page, int code, String message) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("code", code);
        result.put("msg", message);
        result.put("count", page.getTotalRow());
        result.put("data", page.getList());
        super.renderJson(result);
    }

    // 0428 基于layUI的分页实现
    public void getMethod_change() {
        int pageIndex = getParaToInt("page");
        int pageSize = getParaToInt("limit");
        String code = getPara("num");
        Page<Company> page = service.paginat_change(pageIndex, pageSize, code);
        renderPageForLayUI(page, 0, "操作成功");
    }

    public void getMethod_lawsuit() {
        int pageIndex = getParaToInt("page");
        int pageSize = getParaToInt("limit");
        String code = getPara("num");
        Page<Company> pages = service.paginats(pageIndex, pageSize, code);
        renderPageForLayUI(pages, 0, "操作成功");
    }

    public void getMethod_financial() {
        int pageIndex = getParaToInt("page");
        int pageSize = getParaToInt("limit");
        String name = getPara("name");
        Page<Company> pages = service.paginat_financial(pageIndex, pageSize,
                name);
        System.out.println(name);
        renderPageForLayUI(pages, 0, "操作成功");
    }

    public void getNews()
    {
        String name = getPara("name");
        List<Company> get_news = service.getBynews(name);
        renderJson(JsonKit.toJson(get_news));
    }


    // 获取所有公司并分页
    public void getAllCompany() {
        int pageIndexs = getParaToInt("page");
        int pageSizes = getParaToInt("limit");
        String cname = getPara("KeyWords");
        Page<Company> page_all = service.paginats_all_company(pageIndexs,
                pageSizes, cname);
        renderPageForLayUI(page_all, 0, "操作成功");
    }

    // 舆情信息处理
    public void getSentimentRes() throws Exception {
        // 接收处理文本
        String texts = getPara("text");
        System.out.println(texts);
        String data = texts.replaceAll("\\pP|\\pS|\\s*", "");
        // 防止URL编码问题
        String dataEncode = URLEncoder. encode(data, "utf-8" );
        //发送 POST 请求
        String sr= sendGetMethod("http://124.16.71.33:8600/Sentiment", "text="+dataEncode, "utf-8");
        System.out.println(sr);
        renderJson(sr);		
    }


    /**
     * 通过get方式发送请求，并返回响应结果
     * 
     * @param url
     *            请求地址
     * @param params
     *            参数列表，例如name=小明&age=8里面的中文要经过Uri.encode编码
     * @param encoding
     *            编码格式
     * @return 服务器响应结果
     * @throws Exception
     */
    private static final int HTTP_STATUS_OK = 200;
    public static String sendGetMethod(String url, String params,
            String encoding) throws Exception {
        String result = "";
        // String dataEncode = URLEncoder.encode(params, "utf-8" );
        url += ((-1 == url.indexOf("?")) ? "?" : "&") + params;

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        get.setHeader("charset", encoding);

        try {
            HttpResponse response = client.execute(get);
            if (HTTP_STATUS_OK == response.getStatusLine().getStatusCode()) {
                result = EntityUtils.toString(response.getEntity(), encoding);
            } else {
                throw new Exception("Invalide response from Api!"
                        + response.toString());
            }
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }



    /**
     * 以FileWriter方式写入txt文件。
     * 
     * @param File
     *            file： 要写入的文件
     * @param String
     *            content： 要写入的内容
     * @param String
     *            charset: 要写入内容的编码方式
     */
    public static void contentToTxt(String filePath, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
                    filePath), true));
            writer.write(content + "\n");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 基于Jfinal的文件上传并调用CNN模型实现信用等级评估
    public void upload() throws JSONException, IllegalArgumentException, IOException {
        // 网页端接收上传.csv文件
        UploadFile uploadFile = getFile();
        float predValue = 0;
        // 获取上传文件
        String fileName = uploadFile.getOriginalFileName();
        // 获取文件上传的路径
        File delfile = new File(uploadFile.getUploadPath() + "/"
                + uploadFile.getFileName());
        System.out.println(delfile);
        String filePath = delfile.getPath();
        // 调用模型处理函数
        TFModelPred tm = new TFModelPred();
        String modelPath = modelPathPrex + "CreditModel/cnn.pb";
        // 预测值
        predValue = tm.CreditGrade(modelPath, filePath);
        renderJson(predValue);
    }

    /**
     * 卷积神经网络财务风险评估：流动性、筹资方面、清偿方面、成长力方面
     * 
     * @throws IOException
     * @throws FileNotFoundException
     * @throws MWException
     * 
     */
    public void liquidity() throws FileNotFoundException, IOException,
    MWException {
        CNNbusinessBean paraBeans = getBean(CNNbusinessBean.class);
        Object[] res = null;
        // 数据归一化处理过程 by HangHangLi 0919
        riskData lData = new riskData();
        String riskData = dataSetPrex + "fxpg/raw_data.txt";
        MWNumericArray input = null; // 用于保存输入矩阵
        float[] predData = new float[] { paraBeans.getLiquidRate(),
                paraBeans.getReceivables(), paraBeans.getTotalCurrentAssets(),
                paraBeans.getTotalNoCurrentAssets(),
                paraBeans.getTotalCurrentLiabilities(), paraBeans.getWctor() };
        input = new MWNumericArray(predData, MWClassID.DOUBLE);
        res = lData.risk(1, riskData, input, 0);
        double[] pred = (double[]) ((MWNumericArray) res[0]).getData();
        float[] test = new float[pred.length];
        for (int i = 0; i < pred.length; i++) {
            test[i] = (float) pred[i];
        }
        System.out.println(test);
        TFModelPred tm = new TFModelPred();
        String modelPath = modelPathPrex + "Risk/LD.pb";
        float predValue = tm.RiskGrade(test, modelPath);
        // 反解求出最终值

        renderJson(predValue);
    }

    public void funding() throws FileNotFoundException, IOException {
        CNNbusinessBean paraBean = getBean(CNNbusinessBean.class);
        float[] predData = new float[] { paraBean.getTaxGrade(),
                paraBean.getFinancialCostRate(), paraBean.getEquityRatio(),
                paraBean.getQuickRatio(), paraBean.getCashRatio(),
                paraBean.getCashCoverageRatio() };
        TFModelPred tm = new TFModelPred();
        String modelPath = modelPathPrex + "Risk/chouzi.pb";
        float predValue = tm.RiskGrade(predData, modelPath);
        renderJson(predValue);
    }

    public void discharge() throws FileNotFoundException, IOException {
        CNNbusinessBean paraBean = getBean(CNNbusinessBean.class);
        float[] predData = new float[] { paraBean.getCurrentAssets(),
                paraBean.getCurrentDebte(), paraBean.getAssetsTotal(),
                paraBean.getNetProfit(), paraBean.getIncomeTax(),
                paraBean.getNeCashFlow() };
        TFModelPred tm = new TFModelPred();
        String modelPath = modelPathPrex + "Risk/QC_Model.pb";
        float predValue = tm.RiskGrade(predData, modelPath);
        renderJson(predValue);

    }

    public void growth() throws FileNotFoundException, IOException {
        CNNbusinessBean paraBean = getBean(CNNbusinessBean.class);
        float[] predData = new float[] { paraBean.getPorir(),
                paraBean.getTagr(), paraBean.getRoe(), paraBean.getApr(),
                paraBean.getCashIncomeR(), paraBean.getPayCash() };
        TFModelPred tm = new TFModelPred();
        String modelPath = modelPathPrex + "Risk/chengzhang.pb";
        float predValue = tm.RiskGrade(predData, modelPath);
        renderJson(predValue);

    }

    /**
     * 经营风险--经营力方面
     * 
     * @throws FileNotFoundException
     * @throws IOException
     * 
     */
    public void bussinessRisk() throws FileNotFoundException, IOException {
        businessBean paraBean = getBean(businessBean.class);
        float[] predData = new float[] { (float) paraBean.getNetSaleRate(),
                (float) paraBean.getTotalAssetRewardRate(),
                (float) paraBean.getNetAssetYield(),
                (float) paraBean.getInventoryTurnover(),
                (float) paraBean.getTotalAssetTurnover(),
                (float) paraBean.getCostProfitMargin() };
        // 预测数据预处理与结果值转化 20181023

        TFModelPred tm = new TFModelPred();
        String modelPath = modelPathPrex + "Risk/经营力.pb";
        float predValue = tm.RiskGrade(predData, modelPath);
        renderJson(predValue);

    }

    /**
     * 经营风险--司法方面
     * 
     * @throws IOException
     * @throws FileNotFoundException
     * 
     */
    public void lawRisk() throws FileNotFoundException, IOException {
        businessBean paraBean = getBean(businessBean.class);
        float[] predData = new float[] { paraBean.getLitigationNum(),
                paraBean.getAbOperNum(),
                paraBean.getAdminSancteNum(),
                paraBean.getChattelPledgeNum(),
                paraBean.getSelfRiskNum(),
                paraBean.getPeripheralRiskNum() };
        TFModelPred tm = new TFModelPred();
        String modelPath = modelPathPrex + "Risk/司法.pb";
        float predValue = tm.RiskGrade(predData, modelPath);
        renderJson(predValue);
    }

}
