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

    private void boot() throws IOException, ClassHierarchyException, InvalidClassFileException, CancelException {
        File exFile = new FileProvider().getFile("exclusion.txt");
        AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", exFile, ClassLoader.getSystemClassLoader());
        this.targetPath = "E:/魔鬼的力量/大三上课程/自动化测试/大作业/WALA/ClassicAutomatedTesting/0-CMD/target/";
        this.getProjectName();
        scope.addClassFileToScope(ClassLoaderReference.Application, new File(this.targetPath + "classes/net/mooctest/CMD.class"));
        scope.addClassFileToScope(ClassLoaderReference.Application, new File(this.targetPath + "test-classes/net/mooctest/CMDTest.class"));
        scope.addClassFileToScope(ClassLoaderReference.Application, new File(this.targetPath + "test-classes/net/mooctest/CMDTest1.class"));
        scope.addClassFileToScope(ClassLoaderReference.Application, new File(this.targetPath + "test-classes/net/mooctest/CMDTest2.class"));
        scope.addClassFileToScope(ClassLoaderReference.Application, new File(this.targetPath + "test-classes/net/mooctest/CMDTest3.class"));

        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        Iterable<Entrypoint> entryPoints = new AllApplicationEntrypoints(scope, cha);

        AnalysisOptions option = new AnalysisOptions(scope, entryPoints);
        SSAPropagationCallGraphBuilder builder = Util.makeZeroCFABuilder(Language.JAVA, option, new AnalysisCacheImpl(), cha, scope);
        this.cg = builder.makeCallGraph(option);
        for (int i = 0; i < cg.getNumberOfNodes(); i++) {
            CGNode node = cg.getNode(i);
            if (this.isShrikeBTMethod(node)) {
                System.out.println("---");
            }
        }
        this.creatDotByCallGraph();
    }

    private boolean isShrikeBTMethod(CGNode node) {
        if (node.getMethod() instanceof ShrikeBTMethod) {
            ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
            if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())) {
                String classInnerName = method.getDeclaringClass().getName().toString();
                String signature = method.getSignature();
                System.out.println(classInnerName + " " + signature);
                this.printPredNodes(classInnerName, cg.getPredNodes(node), 'c');
                this.printPredNodes(signature, cg.getPredNodes(node), 'm');
            }
            return true;
        }
        return false;
    }

    private void printPredNodes(String faName, Iterator<CGNode> nodes, char mode) {
        while (nodes.hasNext()) {
            CGNode node = nodes.next();
            if (this.isShrikeBTMethod(node)) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if (mode == 'c') {
                    String classInnerName = method.getDeclaringClass().getName().toString();
                    String call = "\"" + faName + "\" -> \"" + classInnerName + "\";";
                    if (this.classGranularity.add(call)) {
                        this.classQueue.add(call);
                    }
                } else if (mode == 'm') {
                    String signature = method.getSignature();
                    String call = "\"" + faName + "\" -> \"" + signature + "\";";
                    if (this.methodGranularity.add(call)) {
                        this.methodQueue.add(call);
                    }
                }
            }
        }
    }

    private void getParams() {

    }

    private void getProjectName() {
        String[] path = this.targetPath.split("/");
        this.projectName = path[path.length - 2].substring(2);
    }

    private void creatDotByCallGraph() throws FileNotFoundException {
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

    private void clear() {
        this.classGranularity.clear();
        this.methodGranularity.clear();
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
