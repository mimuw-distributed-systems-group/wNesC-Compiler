package pl.edu.mimuw.nesc.codepartition.context;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.codepartition.BankSchema;
import pl.edu.mimuw.nesc.codepartition.BankTable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a context for a single partition operation.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class PartitionContext {
    private final BankTable bankTable;
    private final TreeMap<Integer, Set<String>> banksFreeSpaceMap;
    private final Map<String, Range<Integer>> functionsSizes;

    public PartitionContext(BankSchema bankSchema, Map<String, Range<Integer>> functionsSizes) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        checkNotNull(functionsSizes, "sizes of functions cannot be null");

        final PrivateBuilder builder = new PrivateBuilder(bankSchema);
        this.bankTable = new BankTable(bankSchema);
        this.banksFreeSpaceMap = builder.buildBanksFreeSpaceMap();
        this.functionsSizes = functionsSizes;
    }

    public int getFunctionSize(FunctionDecl functionDecl) {
        final String uniqueName = DeclaratorUtils.getUniqueName(functionDecl.getDeclarator()).get();
        return getFunctionSize(uniqueName);
    }

    public int getFunctionSize(String funUniqueName) {
        return functionsSizes.get(funUniqueName).upperEndpoint();
    }

    public void assign(FunctionDecl function, String bankName) {
        final int funSize = getFunctionSize(function);
        final int oldTargetBankFreeSpace = bankTable.getFreeSpace(bankName);
        if (funSize > oldTargetBankFreeSpace) {
            throw new IllegalStateException("assigning a function to a bank without sufficient space");
        }

        bankTable.allocate(bankName, function, funSize);
        modifyBankFreeSpace(bankName, oldTargetBankFreeSpace,
                bankTable.getFreeSpace(bankName));
    }

    protected void modifyBankFreeSpace(String bankName, int oldBankFreeSpace,
            int newBankFreeSpace) {
        if (oldBankFreeSpace == newBankFreeSpace) {
            return;
        }

        final Set<String> banksMapSet = banksFreeSpaceMap.get(oldBankFreeSpace);
        if (!banksMapSet.remove(bankName)) {
            throw new IllegalStateException("inconsistent information about free space in bank " + bankName);
        }
        if (banksMapSet.isEmpty()) {
            banksFreeSpaceMap.remove(oldBankFreeSpace);
        }

        if (!banksFreeSpaceMap.containsKey(newBankFreeSpace)) {
            banksFreeSpaceMap.put(newBankFreeSpace, new HashSet<String>());
        }
        banksFreeSpaceMap.get(newBankFreeSpace).add(bankName);
    }

    public BankTable getBankTable() {
        return bankTable;
    }

    public Optional<String> getCeilingBank(int size) {
        final Optional<Map.Entry<Integer, Set<String>>> entry = Optional.fromNullable(
                banksFreeSpaceMap.lastEntry());
        return entry.isPresent() && entry.get().getKey() >= size
                ? Optional.of(entry.get().getValue().iterator().next())
                : Optional.<String>absent();
    }

    public Optional<String> getFloorBank(int size) {
        final Optional<Map.Entry<Integer, Set<String>>> entry = Optional.fromNullable(
                banksFreeSpaceMap.ceilingEntry(size));
        return entry.isPresent()
                ? Optional.of(entry.get().getValue().iterator().next())
                : Optional.<String>absent();
    }

    /**
     * Helper class that builds elements of a partition context.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class PrivateBuilder {
        private final BankSchema bankSchema;

        private PrivateBuilder(BankSchema bankSchema) {
            this.bankSchema = bankSchema;
        }

        private TreeMap<Integer, Set<String>> buildBanksFreeSpaceMap() {
            final TreeMap<Integer, Set<String>> result = new TreeMap<>();
            for (String bankName : bankSchema.getBanksNames()) {
                final int capacity = bankSchema.getBankCapacity(bankName);
                if (!result.containsKey(capacity)) {
                    result.put(capacity, new HashSet<String>());
                }
                result.get(capacity).add(bankName);
            }
            return result;
        }
    }
}
