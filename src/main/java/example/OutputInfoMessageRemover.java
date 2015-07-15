package example; /**
 * Created by roy on 2015/07/15.
 */

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

//Info logging of resultsProcessor may become prohibitively large

public class OutputInfoMessageRemover {
    public static void main(String[] args) {
        //Name of text file resultsProcessor saves output to specified as command line argument
        File file = new File(args[0]);
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (!line.contains("INFO")) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
