package autograder;

import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Extending of ANTLR's error listener to pickup parser/lexer errors.
 * @author CallumLindars
 */
public class AutograderErrorListener extends BaseErrorListener {
    public record SyntaxError(
        int line,
        int column,
        String message,
        Object offendingSymbol,
        RecognitionException exception
    ) {}

    private final List<SyntaxError> errors = new ArrayList<>();

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e
    ) {
        errors.add(new SyntaxError(
            line,
            charPositionInLine,
            msg,
            offendingSymbol,
            e
        ));
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void clearErrors(){
        errors.clear();
    }
}