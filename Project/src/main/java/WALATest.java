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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class WALATest {
    // 测试的三个参数
    private String testGranularity;
    private String targetPath;
    private String changeInfo;

    private CallGraph cg;
    private String projectName;
    private HashSet<String> classGranularity = new HashSet<String>();
    private HashSet<String> methodGranularity = new HashSet<String>();
    private ArrayList<String> classQueue = new ArrayList<String>();
    private ArrayList<String> methodQueue = new ArrayList<String>();

    /**
     * 启动pdf生成程序
     */
    private void boot() throws IOException, ClassHierarchyException, InvalidClassFileException, CancelException {
        File exFile = new FileProvider().getFile("exclusion.txt");
        AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", exFile, ClassLoader.getSystemClassLoader());
        this.getParams();
        this.readClasses(scope, new File(this.targetPath + "classes"));
        this.readClasses(scope, new File(this.targetPath + "test-classes"));

        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        Iterable<Entrypoint> entryPoints = new AllApplicationEntrypoints(scope, cha);

        AnalysisOptions option = new AnalysisOptions(scope, entryPoints);
        SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, option, new AnalysisCacheImpl(), cha, scope);
        this.cg = builder.makeCallGraph(option);
        for (int i = 0; i < cg.getNumberOfNodes(); i++) {
            CGNode node = cg.getNode(i);
            this.isShrikeBTMethod(node);
        }
        this.creatDotByCallGraph();
    }

    /**
     * 判断一个方法是否是ShrikeBTMethod,如果是则遍历其调用者
     *
     * @param node 调用图节点
     */
    private void isShrikeBTMethod(CGNode node) {
        if (node.getMethod() instanceof ShrikeBTMethod) {
            ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
            if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                String classInnerName = method.getDeclaringClass().getName().toString();
                String signature = method.getSignature();
                System.out.println(classInnerName + " " + signature);
                Iterator<CGNode> nodes = cg.getPredNodes(node);
                String call;
                while (nodes.hasNext()) {
                    CGNode n = nodes.next();
                    if (n.getMethod() instanceof ShrikeBTMethod) {
                        ShrikeBTMethod m = (ShrikeBTMethod) n.getMethod();
                        if ("Application".equals(m.getDeclaringClass().getClassLoader().toString())) {
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
                    }
                }
            }
        }
    }


    /**
     * 从命令行读取参数
     */
    private void getParams() {
        this.targetPath = "E:/魔鬼的力量/大三上课程/自动化测试/大作业/WALA/ClassicAutomatedTesting/5-MoreTriangle/target/";
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
     * 从类级队列和方法级队列中生成Dot文件
     */
    private void creatDotByCallGraph() {
        try {
            File curDir = new File(".");
            String abPath = curDir.getAbsolutePath();

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
        this.clear();
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
            walaTest.boot();
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
