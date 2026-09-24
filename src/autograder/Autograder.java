package autograder;

import java.io.IOException;
import arithlang.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.stream.Stream;

public class Autograder {
    public static void main(String[] args){
        System.out.println("Runnning autograder...");
        Reader reader = new Reader();
        Evaluator eval = new Evaluator();
        Printer printer = new Printer();

        // For each question in the hw
        try (DirectoryStream<Path> questionStream = Files.newDirectoryStream(Paths.get("src/autograder/testcases"))) {
            for (Path questionPath : questionStream) {
                if (Files.isRegularFile(questionPath)) {
                    continue;
                }
                
                System.out.println("Trying to read questions...");
                try (DirectoryStream<Path> testCaseStream = Files.newDirectoryStream(Paths.get(questionPath.toString()))) {
                    for (Path testCasePath : testCaseStream) {
                        System.out.println(readFile(testCasePath));
                    }
                }
            }
        }
        catch (IOException e){
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static String readFile(Path file) throws IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            
            return sb.toString();
        }
    }
}

