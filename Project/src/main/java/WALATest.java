import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

import java.io.*;
import java.util.*;

public class WALATest {
    // 测试的三个参数
    private String testGranularity;
    private String targetPath;
    private ArrayList<String[]> changeInfo = new ArrayList<String[]>();

    private CallGraph cg;
    private String projectName;
    private HashSet<String> classGranularity = new HashSet<String>();
    private HashSet<String> methodGranularity = new HashSet<String>();
    private ArrayList<String> classQueue = new ArrayList<String>();
    private ArrayList<String> methodQueue = new ArrayList<String>();

    /**
     * 启动类生成/pdf生成程序
     *
     * @param args 命令行参数
     */
    void boot(String[] args) throws IOException, ClassHierarchyException, InvalidClassFileException, CancelException {
        //构建分析域
        File exFile = new FileProvider().getFile("exclusion.txt");
        AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", exFile, ClassLoader.getSystemClassLoader());
        //this.getFakeParams();
        this.getParams(args);
        this.readClasses(scope, new File(this.targetPath + "classes"));
        this.readClasses(scope, new File(this.targetPath + "test-classes"));

        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        Iterable<Entrypoint> entryPoints = new AllApplicationEntrypoints(scope, cha);

        AnalysisOptions option = new AnalysisOptions(scope, entryPoints);
        SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, option, new AnalysisCacheImpl(), cha, scope);
        this.cg = builder.makeCallGraph(option);
        //遍历调用图节点，并找出所需的ShrinkBTMethod的调用者集合
        for (int i = 0; i < cg.getNumberOfNodes(); i++) {
            CGNode node = cg.getNode(i);
            this.addShrikeBTMethodCaller(node);
        }
        //this.creatDot();
        this.makeText();
        this.clear();
    }

    /**
     * 判断一个方法是否是ShrikeBTMethod,如果是则遍历其调用者，并加入cg
     * 方法结束时，cg将变为一个未经过调用传递处理的调用图子图
     *
     * @param node 调用图节点
     */
    private void addShrikeBTMethodCaller(CGNode node) {
        if (node.getMethod() instanceof ShrikeBTMethod) {
            ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
            if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                String classInnerName = method.getDeclaringClass().getName().toString();
                String signature = method.getSignature();
                //执行用例选择方法
                this.select(node, classInnerName, signature);
                //执行Dot选择方法
                //this.selectDot(node, classInnerName, signature);
            }
        }
    }

    /**
     * 加载命令行参数
     *
     * @param args 命令行参数
     */
    private void getParams(String[] args) {
        this.testGranularity = args[0];
        this.targetPath = args[1];
        String changeInfoPath = args[2];

        this.getProjectName();
        try {
            //从change_info.txt中按行读入数据
            BufferedReader changeInfoReader = new BufferedReader(new FileReader(changeInfoPath));
            String selector;
            while ((selector = changeInfoReader.readLine()) != null) {
                String[] selectors = selector.split(" ");
                this.changeInfo.add(selectors);
            }
            changeInfoReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试程序：加载伪命令行参数
     */
    private void getFakeParams() {
        this.testGranularity = "-c";
        this.targetPath = "Data/ClassicAutomatedTesting/5-MoreTriangle/target/";
        this.getProjectName();
    }

    /**
     * 从target路径中获取项目的名字
     */
    private void getProjectName() {
        String[] path = this.targetPath.split("/");
        this.projectName = path[path.length - 2].substring(2);
    }

    /**
     * 遍历文件夹下所有子目录中的.class文件并添加到scope中
     *
     * @param scope      分析域
     * @param classesDir 类目录
     * @throws InvalidClassFileException 无效类文件异常
     */
    private void readClasses(AnalysisScope scope, File classesDir) throws InvalidClassFileException {
        File[] classes = classesDir.listFiles();
        if (classes == null) {
            return;
        }
        for (File clazz : classes) {
            if (clazz.isDirectory()) {
                this.readClasses(scope, clazz);
            } else if (clazz.isFile()) {
                if (clazz.getName().matches(".*\\.class")) {
                    scope.addClassFileToScope(ClassLoaderReference.Application, clazz);
                }
            }
        }
    }

    /**
     * 选择构建Dot需要的节点
     *
     * @param callee         被调用节点
     * @param classInnerName 类名
     * @param signature      方法名
     */
    private void selectDot(CGNode callee, String classInnerName, String signature) {
        Iterator<CGNode> nodes = cg.getPredNodes(callee);
        while (nodes.hasNext()) {
            //遍历前驱节点，即调用者
            CGNode n = nodes.next();
            if (n.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod m = (ShrikeBTMethod) n.getMethod();
                if ("Application".equals(m.getDeclaringClass().getClassLoader().toString())) {
                    //将表示call的字符串存入队列，以构建Dot
                    this.dotPreDeal(m, classInnerName, signature);
                }
            }
        }
    }

    /**
     * 生成一个表示方法调用的字符串并存进序列
     *
     * @param m              方法节点
     * @param classInnerName 类名
     * @param signature      方法名
     */
    private void dotPreDeal(ShrikeBTMethod m, String classInnerName, String signature) {
        String call;
        String callerClass = m.getDeclaringClass().getName().toString();
        call = "\"" + classInnerName + "\" -> \"" + callerClass + "\";";
        if (this.classGranularity.add(call)) {
            this.classQueue.add(call);
        }
        String callerMethod = m.getSignature();
        call = "\"" + signature + "\" -> \"" + callerMethod + "\";";
        if (this.methodGranularity.add(call)) {
            this.methodQueue.add(call);
        }
    }

    /**
     * 从类级队列和方法级队列中生成Dot文件
     */
    private void creatDot() {
        try {
            BufferedWriter dotC = new BufferedWriter(new FileWriter("Report/class-" + this.projectName + ".dot"));
            dotC.write("digraph " + this.projectName.toLowerCase() + "_class {\n");
            for (String s : this.classQueue) {
                dotC.write("    " + s + "\n");
            }
            dotC.write("}");
            dotC.close();

            BufferedWriter dotM = new BufferedWriter(new FileWriter("Report/method-" + this.projectName + ".dot"));
            dotM.write("digraph " + this.projectName.toLowerCase() + "_method {\n");
            for (String s : this.methodQueue) {
                dotM.write("    " + s + "\n");
            }
            dotM.write("}");
            dotM.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从类级队列和方法级队列中生成选择表单
     *
     * @param callee         被调用节点
     * @param classInnerName 类名
     * @param signature      方法名
     */
    private void select(CGNode callee, String classInnerName, String signature) {
        for (String[] selectors : this.changeInfo) {
            if ((this.testGranularity.equals("-m") && classInnerName.equals(selectors[0]) && signature.equals(selectors[1])) ||
                    (this.testGranularity.equals("-c") && classInnerName.equals(selectors[0]))) {
                //当被调用节点与changeInfo的行对应时，查找该生产代码的所有调用者
                ArrayList<String[]> calleeList = new ArrayList<String[]>();
                calleeList.add(selectors);
                this.findCaller(calleeList, callee);
            }
        }
    }

    /**
     * 查找一个change的调用者
     *
     * @param calleeList change链条中的元素
     * @param rootCallee 目前要查找的被调用者
     */
    private void findCaller(ArrayList<String[]> calleeList, CGNode rootCallee) {
        LinkedList<CGNode> calleeQueue = new LinkedList<CGNode>();
        calleeQueue.add(rootCallee);
        while (!calleeQueue.isEmpty()) {
            CGNode callee = calleeQueue.poll();
            Iterator<CGNode> callers = this.cg.getPredNodes(callee);
            while (callers.hasNext()) {
                CGNode caller = callers.next();
                if (caller.getMethod() instanceof ShrikeBTMethod) {
                    ShrikeBTMethod method = (ShrikeBTMethod) caller.getMethod();
                    String callerClass = method.getDeclaringClass().getName().toString();
                    String callerMethod = method.getSignature();
                    if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                        String[] callerFeature = new String[]{callerClass, callerMethod};
                        if (this.isTest(method)) {
                            //当前调用者属于某个测试类时，在队列里添加一条筛选信息
                            if (this.testGranularity.equals("-c")) {
                                if (this.classGranularity.add(callerClass + " " + callerMethod)) {
                                    this.classQueue.add(callerClass + " " + callerMethod);
                                    this.findClassCaller(callerClass);
                                }
                            } else if (this.testGranularity.equals("-m")) {
                                if (this.methodGranularity.add(callerClass + " " + callerMethod)) {
                                    this.methodQueue.add(callerClass + " " + callerMethod);
                                }
                            }
                        }
                        if (!this.containsStrings(calleeList, callerFeature)) {
                            //用一个ArrayList储存该调用链条的内容，当不存在递归情况时，查找调用点
                            calleeList.add(callerFeature);
                            calleeQueue.add(caller);
                        }
                    }
                }
            }
        }
    }

    /**
     * 判断一个方法是否是测试方法
     * @param method 方法
     * @return 是否是测试方法
     */
    private boolean isTest(ShrikeBTMethod method) {
        boolean needStore = false;
        for (Object a : method.getAnnotations()) {
            if (a.toString().matches(".*Test.*")) {
                needStore = true;
                break;
            }
        }
        return needStore;
    }

    /**
     * 判断列表中是否有一个字符串数组
     * @param calleeList 被调用者列表
     * @param callerFeature 被调用者的类名和方法名数组
     * @return 是否包含
     */
    private boolean containsStrings(ArrayList<String[]> calleeList, String[] callerFeature) {
        for (String[] calleeFeature : calleeList) {
            if (calleeFeature[0].equals(callerFeature[0]) && calleeFeature[1].equals(callerFeature[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 找出某个类下的所有方法
     * @param className 类名
     */
    private void findClassCaller(String className) {
        for (CGNode node : this.cg) {
            if (node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod nodeMethod = (ShrikeBTMethod) node.getMethod();
                if (this.isTest(nodeMethod)) {
                    if (nodeMethod.getDeclaringClass().getName().toString().equals(className)) {
                        if (this.classGranularity.add(className + " " + nodeMethod.getSignature())) {
                            this.classQueue.add(className + " " + nodeMethod.getSignature());
                        }
                    }
                }
            }
        }
    }

    /**
     * 创建类选择或方法选择文件
     */
    private void makeText() {
        String txtName;
        if (this.testGranularity.equals("-c")) {
            txtName = "selection-class.txt";
            this.creatSelectTxt(txtName, this.classQueue);
        } else if (this.testGranularity.equals("-m")) {
            txtName = "selection-method.txt";
            this.creatSelectTxt(txtName, this.methodQueue);
        }
    }

    /**
     * 创建一个选择文件
     *
     * @param txtName  创建文件的名字
     * @param contexts 创建文件的内容队列
     */
    private void creatSelectTxt(String txtName, ArrayList<String> contexts) {
        try {
            BufferedWriter txtWriter = new BufferedWriter(new FileWriter(txtName));
            for (String s : contexts) {
                txtWriter.write(s + "\n");
            }
            txtWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清空所有队列
     */
    private void clear() {
        this.classGranularity.clear();
        this.classQueue.clear();
        this.methodGranularity.clear();
        this.methodQueue.clear();
    }

    public static void main(String[] args) {
        WALATest walaTest = new WALATest();
        try {
            walaTest.boot(args);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassHierarchyException e) {
            e.printStackTrace();
        } catch (InvalidClassFileException e) {
            e.printStackTrace();
        } catch (CancelException e) {
            e.printStackTrace();
        }
    }
}