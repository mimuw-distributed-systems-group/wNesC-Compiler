package pl.edu.mimuw.nesc;

import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.problem.NescIssue;

import java.io.FileNotFoundException;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Main {

    public static void main(String[] args) throws InvalidOptionsException {
        final Frontend frontend = NescFrontend.builder()
                .standalone(true)
                .build();
        final ContextRef contextRef = frontend.createContext(args);
        frontend.rebuild(contextRef);
    }

}
