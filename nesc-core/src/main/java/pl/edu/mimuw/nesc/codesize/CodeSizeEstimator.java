package pl.edu.mimuw.nesc.codesize;

import java.io.IOException;

/**
 * <p>Interface with operations for estimating size of the code, i.e. the size
 * of each function of a program.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface CodeSizeEstimator {
    /**
     * <p>Perform the estimation of sizes of functions that constitute
     * a program. The functions for the operations shall be given at
     * construction of the object.</p>
     *
     * @return Map with functions names as keys and estimations of their sizes
     *         as values. If a mapping <code>key -> [V1, V2]</code> is present,
     *         it means that the size of function named <code>key</code> has
     *         size at least <code>V1</code> bytes and at most <code>V2</code>
     *         bytes.
     * @throws InterruptedException The estimation operation uses some kind of
     *                              waiting and it has been interrupted.
     * @throws IOException An IO operation is necessary for the estimation and
     *                     the operation has failed.
     */
    CodeSizeEstimation estimate() throws InterruptedException, IOException;
}
