package pl.edu.mimuw.nesc.codepartition.context;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.codepartition.BankSchema;
import pl.edu.mimuw.nesc.codepartition.BankTable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Partition context that allows changing the assignment of functions into
 * banks and allows for assigning functions to banks without sufficient
 * amount of free space.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class DynamicPartitionContext extends PartitionContext {
    /**
     * Contents of each bank. Keys are names of banks and values are functions
     * allocated to these banks.
     */
    private final SetMultimap<String, FunctionDecl> banksContents;

    /**
     * Free space for each bank. Keys are names of banks and values are amounts
     * of free space in the corresponding banks.
     */
    private final Map<String, Integer> freeSpace;

    /**
     * Allocation of functions into banks. Keys are AST nodes of functions and
     * values are names of banks they are allocated to.
     */
    private final Map<FunctionDecl, String> allocation;

    /**
     * Set with names of banks whose capacity is exceeded by the current
     * assignment.
     */
    private final NavigableSet<String> overloadedBanks;

    /**
     * Map with banks that are empty. Keys are their capacities and
     */
    private final NavigableSet<String> emptyBanks;

    /**
     * Unmodifiable view of the set with overloaded banks.
     */
    private final SortedSet<String> unmodifiableOverloadedBanks;

    /**
     * Unmodifiable view of the set with empty banks.
     */
    private final SortedSet<String> unmodifiableEmptyBanks;

    public DynamicPartitionContext(BankSchema bankSchema, Map<String, Range<Integer>> functionsSizes) {
        super(bankSchema, functionsSizes);
        final PrivateBuilder builder = new RealBuilder(bankSchema);
        this.banksContents = HashMultimap.create();
        this.allocation = new HashMap<>();
        this.freeSpace = builder.buildFreeSpaceMap();
        this.overloadedBanks = builder.buildOverloadedBanks();
        this.emptyBanks = builder.buildEmptyBanks();
        this.unmodifiableOverloadedBanks = Collections.unmodifiableSortedSet(this.overloadedBanks);
        this.unmodifiableEmptyBanks = Collections.unmodifiableSortedSet(this.emptyBanks);
    }

    @Override
    public void assign(FunctionDecl function, String bankName) {
        checkNotNull(function, "function cannot be null");
        checkNotNull(bankName, "name of the bank cannot be null");
        checkArgument(getBankTable().getBanksNames().contains(bankName), "bank with given name does not exist");
        checkState(!allocation.containsKey(function), "the given function has been already allocated");

        // Save the allocation
        allocation.put(function, bankName);
        banksContents.put(bankName, function);

        // Update free space of banks
        final int funSize = getFunctionSize(function);
        final int oldBankFreeSpace = freeSpace.get(bankName);
        final int newBankFreeSpace = oldBankFreeSpace - funSize;
        freeSpace.put(bankName, newBankFreeSpace);
        modifyBankFreeSpace(bankName, oldBankFreeSpace, newBankFreeSpace);

        // Update overloaded banks
        if (newBankFreeSpace < 0) {
            overloadedBanks.add(bankName);
        }

        // Update empty banks
        emptyBanks.remove(bankName);
    }

    /**
     * Get an unmodifiable view of the set with functions currently assigned
     * to the bank with given name.
     *
     * @param bankName Name of the bank.
     * @return Set with functions currently allocated to the given bank.
     */
    public Set<FunctionDecl> getBankContents(String bankName) {
        checkNotNull(bankName, "name of the bank cannot be null");
        checkArgument(getBankTable().getSchema().getBanksNames().contains(bankName),
                "bank with given name does not exist in the schema");
        return Collections.unmodifiableSet(banksContents.get(bankName));
    }

    /**
     * Get the name of the bank the given function has been allocated to.
     *
     * @param function AST node of the function.
     * @return Name of the bank of allocation of the given function. The object
     *         is absent if the function has not been allocated yet.
     */
    public Optional<String> getTargetBank(FunctionDecl function) {
        checkNotNull(function, "function cannot be null");
        return Optional.fromNullable(allocation.get(function));
    }

    /**
     * Get the amount of free space in the given bank for the current
     * assignment.
     *
     * @param bankName Name of a bank whose free space can be checked.
     * @return Amount of free space in the bank with given name for the current
     *         assignment.
     */
    public int getFreeSpace(String bankName) {
        checkNotNull(bankName, "bank name cannot be null");
        checkArgument(!bankName.isEmpty(), "bank name cannot be an empty string");
        checkState(getBankTable().getSchema().getBanksNames().contains(bankName),
                "bank with given name does not exist in the schema");
        return freeSpace.get(bankName);
    }

    /**
     * Remove the assignment of the given function. After call to this method,
     * the given function is unassigned.
     *
     * @param function Function whose assignment to a bank will be removed.
     */
    public void remove(FunctionDecl function) {
        checkNotNull(function, "function cannot be null");
        checkState(allocation.containsKey(function), "the given function has not been assigned yet");

        // Update the allocation
        final String oldBank = allocation.remove(function);
        banksContents.remove(oldBank, function);

        // Update free space of banks
        final int funSize = getFunctionSize(function);
        final int oldBankFreeSpace = freeSpace.get(oldBank);
        final int newBankFreeSpace = oldBankFreeSpace + funSize;
        freeSpace.put(oldBank, newBankFreeSpace);
        modifyBankFreeSpace(oldBank, oldBankFreeSpace, newBankFreeSpace);

        // Update overloaded banks
        if (newBankFreeSpace >= 0) {
            overloadedBanks.remove(oldBank);
        }

        // Update empty banks
        if (banksContents.get(oldBank).isEmpty()) {
            emptyBanks.add(oldBank);
            if (freeSpace.get(oldBank) != getBankTable().getSchema().getBankCapacity(oldBank)) {
                throw new RuntimeException("an empty bank with free space different from its capacity");
            }
        }
    }

    /**
     * Get an unmodifiable view of the set with overloaded banks, e.g. banks
     * whose capacity is exceeded by total size of allocated functions.
     *
     * @return Unmodifiable set of overloaded banks.
     */
    public SortedSet<String> getOverloadedBanks() {
        return unmodifiableOverloadedBanks;
    }

    /**
     * Get an unmodifiable view of the set with empty banks, e.g. banks without
     * any function allocated. The set is sorted ascending by capacity of banks.
     *
     * @return Unmodifiable set of empty banks.
     */
    public SortedSet<String> getEmptyBanks() {
        return unmodifiableEmptyBanks;
    }

    /**
     * Store the current assignment of functions into banks to the bank table
     * created for this context.
     *
     * @return The bank table created for this context.
     */
    public BankTable finish() {
        checkState(getOverloadedBanks().isEmpty(), "cannot finish because there are overloaded banks");
        final BankTable bankTable = getBankTable();
        for (Map.Entry<String, FunctionDecl> assignment : banksContents.entries()) {
            bankTable.allocate(assignment.getKey(), assignment.getValue(),
                    getFunctionSize(assignment.getValue()));
        }
        return bankTable;
    }

    private interface PrivateBuilder {
        Map<String, Integer> buildFreeSpaceMap();
        NavigableSet<String> buildOverloadedBanks();
        NavigableSet<String> buildEmptyBanks();
    }

    private static final class RealBuilder implements PrivateBuilder {
        private final BankSchema bankSchema;

        private RealBuilder(BankSchema bankSchema) {
            this.bankSchema = bankSchema;
        }

        @Override
        public Map<String, Integer> buildFreeSpaceMap() {
            final Map<String, Integer> freeSpace = new HashMap<>();
            for (String bankName : bankSchema.getBanksNames()) {
                freeSpace.put(bankName, bankSchema.getBankCapacity(bankName));
            }
            return freeSpace;
        }

        @Override
        public NavigableSet<String> buildOverloadedBanks() {
            return new TreeSet<>();
        }

        @Override
        public NavigableSet<String> buildEmptyBanks() {
            final TreeSet<String> emptyBanks = new TreeSet<>(new EmptyBankComparator(bankSchema));
            emptyBanks.addAll(bankSchema.getBanksNames());
            return emptyBanks;
        }
    }

    /**
     * Comparator for the set of empty banks. The order defined by this banks
     * is the order of banks sorted ascending with their capacity. If two banks
     * have equal capacity, then the bank with greater name is the greater one.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class EmptyBankComparator implements Comparator<String> {
        private final BankSchema bankSchema;

        private EmptyBankComparator(BankSchema bankSchema) {
            checkNotNull(bankSchema, "bank schema cannot be null");
            this.bankSchema = bankSchema;
        }

        @Override
        public int compare(String bank1, String bank2) {
            checkNotNull(bank1, "the first bank cannot be null");
            checkNotNull(bank2, "the second bank cannot be null");
            checkArgument(bankSchema.getBanksNames().contains(bank1),
                    "the first bank does not exist in the schema");
            checkArgument(bankSchema.getBanksNames().contains(bank2),
                    "the second bank does not exist in the schema");

            final int capacityResult = Integer.compare(bankSchema.getBankCapacity(bank1),
                    bankSchema.getBankCapacity(bank2));
            return capacityResult != 0
                    ? capacityResult
                    : bank1.compareTo(bank2);
        }
    }
}
