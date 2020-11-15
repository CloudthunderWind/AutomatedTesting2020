import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class WALATestTest {
    private static WALATest walaTest;

    private static String loc = "E:/Projects/AutomatedTesting2020/Data/ClassicAutomatedTesting/";
    private static String locALU = loc + "1-ALU/";
    private static String locDataLog = loc + "2-DataLog/";
    private static String locBinaryHeap = loc + "3-BinaryHeap/";
    private static String locNextDay = loc + "4-NextDay/";
    private static String locMoreTriangle = loc + "5-MoreTriangle/";
    private static String target = "target/";
    private static String data = "data/";
    private static String Data = "./";
    private static String changeLog = "change_info.txt";
    private static String className = "selection-class.txt";
    private static String methodName = "selection-method.txt";

    private static String[][] paramList = new String[][]{
            {"-c", locALU + target, locALU + data + changeLog},
            {"-m", locALU + target, locALU + data + changeLog},
            {"-c", locDataLog + target, locDataLog + data + changeLog},
            {"-m", locDataLog + target, locDataLog + data + changeLog},
            {"-c", locBinaryHeap + target, locBinaryHeap + data + changeLog},
            {"-m", locBinaryHeap + target, locBinaryHeap + data + changeLog},
            {"-c", locNextDay + target, locNextDay + data + changeLog},
            {"-m", locNextDay + target, locNextDay + data + changeLog},
            {"-c", locMoreTriangle + target, locMoreTriangle + data + changeLog},
            {"-m", locMoreTriangle + target, locMoreTriangle + data + changeLog}
    };

    private static String[][] compareList = new String[][]{
            {locALU + data + className, Data + className},
            {locALU + data + methodName, Data + methodName},
            {locDataLog + data + className, Data + className},
            {locDataLog + data + methodName, Data + methodName},
            {locBinaryHeap + data + className, Data + className},
            {locBinaryHeap + data + methodName, Data + methodName},
            {locNextDay + data + className, Data + className},
            {locNextDay + data + methodName, Data + methodName},
            {locMoreTriangle + data + className, Data + className},
            {locMoreTriangle + data + methodName, Data + methodName}
    };

    private boolean fileEquals(String dst, String src) {
        try {
            BufferedReader dstReader = new BufferedReader(new FileReader(dst));
            BufferedReader srcReader = new BufferedReader(new FileReader(src));
            String dstLine = dstReader.readLine();
            String srcLine = srcReader.readLine();
            ArrayList<String> dstContent = new ArrayList<String>();
            ArrayList<String> srcContent = new ArrayList<String>();
            while (dstLine != null) {
                if (!dstLine.equals("")) {
                    dstContent.add(dstLine);
                }
                dstLine = dstReader.readLine();
            }
            while (srcLine != null) {
                if (!srcLine.equals("")) {
                    srcContent.add(srcLine);
                }
                srcLine = srcReader.readLine();
            }
            if (dstContent.size() != srcContent.size()) {
                dstReader.close();
                srcReader.close();
                return false;
            }
            for (String s : dstContent) {
                if (!srcContent.contains(s)) {
                    dstReader.close();
                    srcReader.close();
                    return false;
                }
            }
            dstReader.close();
            srcReader.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Test
    public void Test0() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[0]);
        assertTrue(fileEquals(compareList[0][0], compareList[0][1]));
    }

    @Test
    public void Test1() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[1]);
        assertTrue(fileEquals(compareList[1][0], compareList[1][1]));
    }

    @Test
    public void Test2() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[2]);
        assertTrue(fileEquals(compareList[2][0], compareList[2][1]));
    }

    @Test
    public void Test3() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[3]);
        assertTrue(fileEquals(compareList[3][0], compareList[3][1]));
    }

    @Test
    public void Test4() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[4]);
        assertTrue(fileEquals(compareList[4][0], compareList[4][1]));
    }

    @Test
    public void Test5() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[5]);
        assertTrue(fileEquals(compareList[5][0], compareList[5][1]));
    }

    @Test
    public void Test6() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[6]);
        assertTrue(fileEquals(compareList[6][0], compareList[6][1]));
    }

    @Test
    public void Test7() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[7]);
        assertTrue(fileEquals(compareList[7][0], compareList[7][1]));
    }

    @Test
    public void Test8() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[8]);
        assertTrue(fileEquals(compareList[8][0], compareList[8][1]));
    }

    @Test
    public void Test9() throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        walaTest = new WALATest();
        walaTest.boot(paramList[9]);
        assertTrue(fileEquals(compareList[9][0], compareList[9][1]));
    }
}