package pl.edu.mimuw.nesc.codepartition;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Table that holds allocation of functions into banks.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class BankTable {
    /**
     * Object with information about all available banks.
     */
    private final BankSchema bankSchema;

    /**
     * Functions allocated to specific banks. Keys are names of banks and values
     * are functions inside them.
     */
    private final ListMultimap<String, FunctionDecl> allocation;

    /**
     * Map with free space for each bank. Keys are names of banks and values are
     * remaining space.
     */
    private final Map<String, Integer> freeSpace;

    /**
     * Set with names of functions that are already allocated to banks.
     */
    private final Set<String> allocatedFunctions;

    /**
     * Create a new bank table that allows allocating functions to banks
     * according to the given schema.
     *
     * @param bankSchema Schema of banks for the table.
     */
    public BankTable(BankSchema bankSchema) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        this.bankSchema = bankSchema;
        this.allocation = ArrayListMultimap.create(bankSchema.getBanksNames().size(), 100);
        this.allocatedFunctions = new HashSet<>();
        this.freeSpace = new HashMap<>();
        for (String bankName : bankSchema.getBanksNames()) {
            freeSpace.put(bankName, bankSchema.getBankCapacity(bankName));
        }
    }

    /**
     * Get the bank schema used by this table.
     *
     * @return Schema used by this table.
     */
    public BankSchema getSchema() {
        return bankSchema;
    }

    /**
     * Allocate an area of given size of bank with given name for the given
     * function.
     *
     * @param bankName Name of the bank whose area is allocated.
     * @param function Function that is allocated.
     * @param size Size of the area of the bank that is allocated.
     * @throws IllegalStateException There is no bank with given name. Function
     *                               with given name has been already allocated.
     *                               There is less space in the given bank than
     *                               the size required for the function.
     * @throws IllegalArgumentException Size of the function is negative.
     */
    public void allocate(String bankName, FunctionDecl function, int size) {
        checkState(bankSchema.getBanksNames().contains(bankName),
                "bank '%s' does not exist in the schema of this table", bankName);
        final String uniqueName = DeclaratorUtils.getUniqueName(function.getDeclarator()).get();
        checkState(!allocatedFunctions.contains(uniqueName),
                "function '%s' has been already allocated", uniqueName);
        checkArgument(size >= 0, "size cannot be negative");
        checkState(freeSpace.get(bankName) >= size, "not enough free space for allocation of function '%s' to bank '%s'",
                uniqueName, bankName);

        allocation.put(bankName, function);
        freeSpace.put(bankName, freeSpace.get(bankName) - size);
        allocatedFunctions.add(uniqueName);
    }

    /**
     * Get an unmodifiable list of functions allocated to bank with given name.
     *
     * @param bankName Name of a bank from the schema.
     * @return Unmodifiable list with functions allocated to the bank with given
     *         name.
     */
    public List<FunctionDecl> getBankContents(String bankName) {
        checkBankName(bankName);
        return Collections.unmodifiableList(allocation.get(bankName));
    }

    /**
     * Get the size of the area that is available in the bank with given name.
     *
     * @param bankName Name of a bank from the schema.
     * @return Size of area in bank with given name that has not been yet used.
     */
    public int getFreeSpace(String bankName) {
        checkBankName(bankName);
        return freeSpace.get(bankName);
    }

    /**
     * A convenient method for getting names of banks in the given table. The
     * operation is delegated to the schema of this table.
     *
     * @return Set with names of the banks of this table.
     */
    public ImmutableSet<String> getBanksNames() {
        return bankSchema.getBanksNames();
    }

    /**
     * Name of the common bank of this table. The operation is delegated to the
     * schema object of this table.
     *
     * @return Name of the common bank of the schema of this table.
     */
    public String getCommonBankName() {
        return bankSchema.getCommonBankName();
    }

    private void checkBankName(String bankName) {
        checkNotNull(bankName, "name of the bank cannot be null");
        checkArgument(!bankName.isEmpty(), "name of the bank cannot be an empty string");
        checkState(bankSchema.getBanksNames().contains(bankName), "bank '%s' does not exist in the schema",
                bankName);
    }
}
