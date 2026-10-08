package autograder;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import arithlang.*;
import arithlang.AST.Program;
import autograder.AutograderErrorListener.SyntaxError;

/**
 * Reads input test cases from <code>autograder/testcases/</code> sorted by folder.
 * @author CallumLindars
 * 
 * 
*/
public class Autograder {
    public static void main(String[] args){
        System.out.println("===== AUTOGRADER ====\n");

        AutograderReader reader = new AutograderReader();
        Evaluator eval = new Evaluator();

        // For each question in the hw
        try (DirectoryStream<Path> questionStream = Files.newDirectoryStream(Paths.get("src/autograder/testcases"))) {
            for (Path questionPath : questionStream) {
                if (Files.isRegularFile(questionPath) || isEmptyTestDir(questionPath)) {
                    continue;
                }

                System.out.printf("\nQuestion: %s\n===========\n\n", questionPath.subpath(3, 4).toString());

                //For each test file in the question folder
                try (DirectoryStream<Path> testCaseStream = Files.newDirectoryStream(Paths.get(questionPath.toString()))) {
                    for (Path testCasePath : testCaseStream) {
                        if(!testCasePath.toString().endsWith(".test")){
                            continue;
                        }

                        TestSet set = readFile(testCasePath);
                        set.question = questionPath.subpath(3, 4).toString();

                        //TODO get scoring
                        runTestSet(set, reader, eval);
                    }
                }

                //TODO print total question score
            }

            //TODO print final score
        }
        catch (IOException e){
            System.err.println("Error reading file: " + e.getMessage());
        }
        catch(IndexOutOfBoundsException e){
            System.err.println("Incorrect defined question count: " + e.getMessage());
        }
        catch(InvalidTestSizeException e){
            System.err.println("Incorrect defined question count: " + e.getMessage());
        }
    }

    public static boolean isEmptyTestDir(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
                Iterator<Path> it = directoryStream.iterator();
                if(!it.hasNext()) {
                    return true;
                }
                else {
                    while(it.hasNext()){
                        Path file = it.next();
                        if (file.toString().endsWith(".test")) { 
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false; // Not a directory
    }

    /**
     * Run a collection of tests
     * @param set TestSet to run
     * @param reader language reader
     * @param eval language evaluator
     */
    private static void runTestSet(TestSet set, AutograderReader reader, Evaluator eval){
        //TODO add scoring for questions

        ResultSet results = new ResultSet();
        for(int i = 0; i < set.size(); i++){
            TestResult res = runTest(set.get(i), reader, eval);
            res.row = set.get(i).row;
            res.id = set.get(i).id+1;
            results.add(res);
        }

        // If any error show error header
        if(results.hasErrors() || results.hasFails()){
            System.out.printf("*** FAIL: %s %s\n", set.name, set.path);
        }
        // Return early if there are no errors (show passing whole file rather than each test case)
        else{
            System.out.printf("*** PASS: %s %s\n", set.name, set.path);
            return;
        }

        for(int i = 0; i < set.size(); i++){
            TestResult result = results.get(i);

            // Errors
            if(!result.perror.isEmpty() || !result.lerror.isEmpty()){
                System.out.printf("***\tFAIL: %s Test #%s %s:%s\n", set.name, result.id, set.path, result.row);
                for(SyntaxError e : result.lerror){
                    System.out.printf(
                        "***\t\tLexer error: %d:%d: %s%n",
                        e.line(),
                        e.column(),
                        e.message()
                    );
                }
                for(SyntaxError e : result.perror){
                    System.out.printf(
                        "***\t\tParser error: %d:%d: %s%n",
                        e.line(),
                        e.column(),
                        e.message()
                    );
                }
                if(result.note != null){
                    System.out.printf("***\t\tNote: %s\n", result.note.toString().replace("\n", "\n***\t\t"));
                }
            }

            // If incorrect display student and correct solution + note if available
            else if(!result.correct){
                System.out.printf("***\tFAIL: %s:%s\n", set.path, result.row);
                System.out.printf("***\tStudent Solution: %s\n", result.studentResponse);
                System.out.printf("***\tCorrect Solution: %s\n", result.correctResponse);
                if(result.note != null){
                    System.out.printf("***\n***\t\tNote: %s\n", result.note.toString().replace("\n", "\n***\t\t"));
                }
            }

            else if(result.correct){
                System.out.printf("***\tPASS: %s:%s\n", set.path, result.row);
            }

            else{
                System.err.println("Tell Callum to fix this...");
            }
        }
    }

    /**
     * Run an individual test
     * @param t Individual test to run
     * @param reader language reader
     * @param eval language evaluator
     * @return TestResult with results from the test
     */
    private static TestResult runTest(Test t, AutograderReader reader, Evaluator eval){
        List<AutograderErrorListener.SyntaxError> parserErrors = new ArrayList<>();
        List<AutograderErrorListener.SyntaxError> lexerErrors = new ArrayList<>();

        Boolean correct = false;
        Value val = null;
        Program p = null;
        try {
            for(String input : t.input){
                p = reader.parse(input);
            }
            lexerErrors.addAll(reader.lexerErrorListener.getErrors());
            parserErrors.addAll(reader.parserErrorListener.getErrors());

            reader.lexerErrorListener.clearErrors();
            reader.parserErrorListener.clearErrors();
            
            if(p._e == null) return new TestResult(correct, lexerErrors, parserErrors, null, t.solution, t.note);
            val = eval.valueOf(p);
            if (val.toString().equals(t.solution.toString())) {
                correct = true;
            }
        } catch (NullPointerException e) {
            System.out.println("Error:" + e.getMessage());
        }
        return new TestResult(correct, lexerErrors, parserErrors, val.toString(), t.solution, t.note);
    }

    /**
     * Turns a file path into a TestSet
     * @param file file to read
     * @return TestSet based on file read
     * @throws IOException
     */
    private static TestSet readFile(Path file) throws IOException, IndexOutOfBoundsException, InvalidTestSizeException{
        try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
            String[] metadata = new String[2];
            int row = 3;

            String name = br.readLine().split(":")[1].strip();
            int numTests = Integer.parseInt(br.readLine().split(":")[1].strip());

            metadata[0] = name;
            metadata[1] = Integer.toString(numTests);

            List<String>[] inputs = new ArrayList[numTests];
            String[] outputs = new String[numTests];
            String[] notes = new String[numTests];
            int[] weights = new int[numTests];
            int[] locations = new int[numTests];
            List<Integer> clears = new ArrayList<>();

            for(int i = 0; i < numTests; i++){
                inputs[i] = new ArrayList<String>();
            }

            String text = br.readLine();
            int i = -1;
            while(text != null) {
                if(text.startsWith("#")){
                    text = br.readLine();
                    row++;
                    continue;
                }
                String[] line = text.strip().split("#")[0].split(":");
                switch (line[0]) {
                    case "input":
                        String[] test_inputs = line[1].strip().split(",");
                        for(String input : test_inputs){
                            inputs[i].add(input);
                        }
                        break;
                    case "output":
                        outputs[i] = line[1].strip();
                        break;
                    case "weight":
                        weights[i] = Integer.parseInt(line[1].strip());
                        break;
                    case "note":
                        String note = line[1].strip();

                        while(!note.substring(note.indexOf("\"")+1).contains("\"")){
                            note += br.readLine() + "\n";
                            row++;
                        }

                        notes[i] = note.substring(1, note.length()-2);
                        break;

                    case "test":
                        i++;
                        locations[i] = row;
                        break;

                    case "":
                        break;

                    case "clear":
                        clears.add(i);
                        break;
                
                    default:
                        System.err.println("Invalid Test Format");
                }
                text = br.readLine();
                row++;
            }

            if(inputs[numTests-1].isEmpty()){
                throw new InvalidTestSizeException("Number of tests is smaller than specified.");
            }

            TestSet set = new TestSet(name);
            set.path = file.toString();

            for(int j = 0; j < numTests; j++){
                set.add(new Test(j, inputs[j], outputs[j], notes[j], locations[j], weights[j]));
            }

            set.clears = clears;

            return set;
        }
    }
    
    /**
     * Individual Test 
     */
    public static class Test {
        int id, row, weight;
        String solution, note;
        List<String> input;

        public Test(int id, List<String> input, String solution, String note, int loc, int weight){
            this.id = id;
            this.input = input;
            this.solution = solution;
            this.note = note;
            this.row = loc;
            this.weight = weight;
        }

        @Override 
        public String toString(){
            return String.format("Test ID: %s, Test Input: %s, Expected Solution: %s, Notes: %s", id, input, solution, note);
        }
    }

    /**
     * Collection of Tests
     */
    public static class TestSet {
        String name;
        String path;
        String question;
        List<Integer> clears;
        List<Test> tests = new ArrayList<Test>();

        public TestSet(String name){
            this.name = name;
        }

        @Override 
        public String toString(){
            return tests.toString();
        }

        public void add(Test t){
            tests.add(t);
        }

        public Test get(int i){
            return tests.get(i);
        }

        public int size(){
            return tests.size();
        }
    }

    /**
     * Result of Test run
     */
    public static class TestResult {
        Boolean correct;
        List<SyntaxError> lerror, perror;
        int row, id;
        String studentResponse, correctResponse, note;

        public TestResult(Boolean correct, List<SyntaxError> lerror, List<SyntaxError> perror, String sr, String cr, String note){
            this.correct = correct;
            this.lerror = lerror;
            this.perror = perror;
            studentResponse = sr;
            correctResponse = cr;
            this.note = note;
        }

        @Override
        public String toString() {
            return "Test Result: " + correct.toString() + note;
        }
    }

    public static class ResultSet {
        List<TestResult> list = new ArrayList<>();

        public void add(TestResult t){
            list.add(t);
        }

        public Boolean hasErrors(){
            for(TestResult t : list){
                if(!t.perror.isEmpty()){
                    return true;
                }
                if(!t.lerror.isEmpty()){
                    return true;
                }
            }
            return false;
        }

        public Boolean hasFails(){
            for(TestResult t : list){
                if(!t.correct){
                    return true;
                }
            }
            return false;
        }

        public TestResult get(int i){
            return list.get(i);
        }

        @Override 
        public String toString(){
            return list.toString();
        }
    }

    public static class InvalidTestSizeException extends Exception {
        InvalidTestSizeException(String msg){
            super(msg);
        }
    }
}

