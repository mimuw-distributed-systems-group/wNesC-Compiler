package pl.edu.mimuw.nesc;

import pl.edu.mimuw.nesc.exception.InvalidOptionsException;

import java.io.FileNotFoundException;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Main {

    public static void main(String[] args) throws InvalidOptionsException, FileNotFoundException {
        final Frontend frontend = NescFrontend.builder()
                .standalone(true)
                .build();
        final ContextRef contextRef = frontend.createContext(args);
    }

}
