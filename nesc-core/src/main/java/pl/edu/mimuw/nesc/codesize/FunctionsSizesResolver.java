package pl.edu.mimuw.nesc.codesize;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Object that reads .REL files and computes sizes of functions declared in
 * them.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class FunctionsSizesResolver {
    /**
     * Regular expression that specifies the language of numbers in hexadecimal
     * notation.
     */
    private static final String REGEXP_HEX_NUMBER = "[0-9a-fA-F]+";

    /**
     * Regular expression that describes a single entry of a code segment in
     * a .REL file.
     */
    private static final Pattern REGEXP_SEGMENT_ENTRY = Pattern.compile(
            "S _(?<funName>\\w+) Def(?<offset>" + REGEXP_HEX_NUMBER + ")");

    /**
     * Regular expression that depicts the header of the code segment with
     * functions whose code size is estimated.
     */
    private final Pattern regexpCodeSegment;

    FunctionsSizesResolver(String codeSegmentName) {
        checkNotNull(codeSegmentName, "code segment name cannot be null");
        checkArgument(!codeSegmentName.isEmpty(), "code segment name cannot be an empty string");

        this.regexpCodeSegment = Pattern.compile("A " + codeSegmentName + " size (?<size>"
                + REGEXP_HEX_NUMBER + ") flags " + REGEXP_HEX_NUMBER + " addr "
                + REGEXP_HEX_NUMBER);
    }

    /**
     * Reads information about the code segment given at construction from the
     * given .REL file and computes sizes of functions by subtracting their
     * offsets.
     *
     * @param relFilePath Name of the .REL file with information.
     * @return Map with sizes of functions read from the .REL file.
     */
    public ImmutableMap<String, Integer> resolve(String relFilePath) throws FileNotFoundException {
        checkNotNull(relFilePath, "file name cannot be null");
        checkArgument(!relFilePath.isEmpty(), "file name cannot be an empty string");
        return computeFunctionsSizes(readSegmentData(relFilePath));
    }

    private CodeSegmentData readSegmentData(String relFilePath) throws FileNotFoundException {
        final Map<String, Integer> offsets = new HashMap<>();
        Optional<Integer> totalSize = Optional.absent();

        try (final Scanner scanner = new Scanner(new FileInputStream(relFilePath))) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();

                if (!totalSize.isPresent()) {
                    final Matcher headerMatcher = regexpCodeSegment.matcher(line);
                    if (headerMatcher.matches()) {
                        totalSize = Optional.of(Integer.valueOf(headerMatcher.group("size"), 16));
                    }
                } else {
                    final Matcher entryMatcher = REGEXP_SEGMENT_ENTRY.matcher(line);
                    if (entryMatcher.matches()) {
                        offsets.put(entryMatcher.group("funName"),
                                Integer.valueOf(entryMatcher.group("offset"), 16));
                    } else {
                        break;
                    }
                }
            }
        }

        if (!totalSize.isPresent()) {
            throw new RuntimeException("cannot find the code segment entry in .REL file");
        }

        return new CodeSegmentData(totalSize.get(), offsets);
    }

    private ImmutableMap<String, Integer> computeFunctionsSizes(CodeSegmentData segmentData) {
        // Sort functions in ascending order according to their offsets
        final List<String> functionsNames = new ArrayList<>(segmentData.offsets.keySet());
        Collections.sort(functionsNames, new FunctionOffsetComparator(segmentData.offsets));

        // Compute sizes of functions
        final Map<String, Integer> workMap = segmentData.offsets;
        for (int i = 0; i < functionsNames.size(); ++i) {
            final int minuend = i + 1 < functionsNames.size()
                    ? workMap.get(functionsNames.get(i + 1))
                    : segmentData.totalSize;
            workMap.put(functionsNames.get(i), minuend - workMap.get(functionsNames.get(i)));
        }

        return ImmutableMap.copyOf(workMap);
    }

    /**
     * A small helper class that represents data read from .REL file created by
     * SDCC.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class CodeSegmentData {
        private final Map<String, Integer> offsets;
        private final int totalSize;

        private CodeSegmentData(int totalSize, Map<String, Integer> offsets) {
            checkArgument(totalSize >= 0, "total size cannot be negative");
            checkNotNull(offsets, "offsets cannot be null");
            this.offsets = offsets;
            this.totalSize = totalSize;
        }
    }

    /**
     * Comparator that compares names of functions using their offsets from the
     * beginning of a code segment.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionOffsetComparator implements Comparator<String> {
        private final Map<String, Integer> offsets;

        private FunctionOffsetComparator(Map<String, Integer> offsets) {
            checkNotNull(offsets, "offsets cannot be null");
            this.offsets = offsets;
        }

        @Override
        public int compare(String funName1, String funName2) {
            checkNotNull(funName1, "name of the first function cannot be null");
            checkNotNull(funName2, "name of the second function cannot be null");
            checkState(offsets.containsKey(funName1), "unknown function '"
                    + funName1 + "'");
            checkState(offsets.containsKey(funName2), "unknown function '"
                    + funName2 + "'");
            return Integer.compare(offsets.get(funName1), offsets.get(funName2));
        }
    }
}
