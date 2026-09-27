package autograder;

import org.antlr.v4.runtime.*;
import arithlang.parser.*; 

@SuppressWarnings("deprecation")

/**
 * Extending Arithlang reader to use custom error listener
 * @author CallumLindars
 */
public class AutograderReader extends arithlang.Reader {
    public AutograderErrorListener lexerErrorListener = new AutograderErrorListener();
    public AutograderErrorListener parserErrorListener = new AutograderErrorListener();

    @Override 
    protected Lexer getLexer(org.antlr.v4.runtime.ANTLRInputStream s) {
        Lexer lexer = new ArithLangLexer(s);
        lexer.removeErrorListeners();
        lexer.addErrorListener(lexerErrorListener);

		return lexer;
	}
	
    @Override 
	protected ArithLangParser getParser(org.antlr.v4.runtime.CommonTokenStream s) {
        ArithLangParser parser = new ArithLangParser(s);
        parser.removeErrorListeners();
        parser.addErrorListener(parserErrorListener);

		return parser;
	}
}
