package autograder;

import java.io.IOException;
import arithlang.*;
import arithlang.AST.Program;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.*;
import java.util.stream.Stream;

public class Autograder {
    public static void main(String[] args){
        System.out.println("===== AUTOGRADER =====");
        Reader reader = new Reader();
        Evaluator eval = new Evaluator();
        Printer printer = new Printer();

        // For each question in the hw
        try (DirectoryStream<Path> questionStream = Files.newDirectoryStream(Paths.get("src/autograder/testcases"))) {
            for (Path questionPath : questionStream) {
                if (Files.isRegularFile(questionPath)) {
                    continue;
                }
                try (DirectoryStream<Path> testCaseStream = Files.newDirectoryStream(Paths.get(questionPath.toString()))) {
                    for (Path testCasePath : testCaseStream) {
                        testSet set = readFile(testCasePath);
                        Boolean[] fails = new Boolean[set.tests.size()];
                        Arrays.fill(fails, false);

                        for(int i = 0; i < set.tests.size(); i++){
                            Program p = null;
                            try {
                                p = reader.parse(set.tests.get(i));
                                if(p._e == null) continue;
                                Value val = eval.valueOf(p);
                                if (val.toString() != set.ans.get(i).toString()) {
                                    fails[i] = true;
                                }
                            } catch (NullPointerException e) {
                                System.out.println("Error:" + e.getMessage());
                            }
                        }

                        System.out.println(Arrays.toString(fails));
                    }
                }
            }
        }
        catch (IOException e){
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static testSet readFile(Path file) throws IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            String[] metadata = br.readLine().split(",");
            
            String testName = metadata[0].strip();
            String length = metadata[1].strip();


            ArrayList<String> tests = new ArrayList<>();
            ArrayList<String> answers = new ArrayList<>();

            int i = Integer.parseInt(length);
            while (i-- > 0) {
                tests.add(br.readLine());
                answers.add(br.readLine());
            }
            
            return new testSet(metadata, tests, answers);
        }
    }

    public static class testSet {
        ArrayList<String> tests, ans;
        String[] meta;

        public testSet(String[] meta, ArrayList<String> tests, ArrayList<String> ans){
            this.tests = tests;
            this.ans = ans;
            this.meta = meta;
        }

        @Override 
        public String toString(){
            return tests.toString() + ans.toString();
        }
    }
}

