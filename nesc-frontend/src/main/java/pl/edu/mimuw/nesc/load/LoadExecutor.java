package pl.edu.mimuw.nesc.load;

import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.FrontendContext;

import java.io.IOException;
import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The class responsible for processing source files in the proper order.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class LoadExecutor {

    private final FrontendContext context;

    /**
     * Creates parser executor.
     *
     * @param context context
     */
    public LoadExecutor(FrontendContext context) {
        checkNotNull(context, "context cannot be null");
        this.context = context;
    }

    /**
     * Parses file. Cached data are used when possible.
     *
     * @param filePath      file path
     * @param isDefaultFile indicates if file is included by default
     * @return list of file datas (the first one is the "root" file's data)
     * @throws IOException
     */
    public LinkedList<FileCache> parse(String filePath, boolean isDefaultFile) throws IOException {
        if (context.isStandalone()) {
            return new StandaloneLoadExecutor(context, filePath).parseFile();
        } else if (isDefaultFile) {
            return new PluginLoadDefaultExecutor(context, filePath).parseFile();
        } else {
            return new PluginLoadExecutor(context, filePath, true).parseFile();
        }
    }
}
