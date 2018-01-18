import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RevRead {
    public static void read(String filename, String charset) {

        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile(filename, "r");
            long len = rf.length();
            long start = rf.getFilePointer();
            long nextend = start + len - 1;
            String line;
            rf.seek(nextend);
            int c = -1;
            while (nextend > start) {
                c = rf.read();
                if (c == '\n' || c == '\r') {
                    line = rf.readLine();
                    if (line != null) {
                        System.out.println(new String(line
                                .getBytes("ISO-8859-1"), charset));
                    } else {
//                        System.out.println(line);
                    }
                    nextend--;
                }
                nextend--;
                rf.seek(nextend);
                if (nextend == 0) {// 当文件指针退至文件开始处，输出第一行
                    // System.out.println(rf.readLine());
                    System.out.println(new String(rf.readLine().getBytes(
                            "ISO-8859-1"), charset));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rf != null)
                    rf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        read("src/1514258737023.txt", "UTF-8");
    }
}
