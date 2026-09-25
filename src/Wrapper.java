import autograder.Autograder;

public class Wrapper {
    public static void main(String[] args){
        if (args.length > 0 && args[0].equals("autograder")) {
            Autograder.main(args);
        } else {
            arithlang.Interpreter.main(args);
        }
    }
}