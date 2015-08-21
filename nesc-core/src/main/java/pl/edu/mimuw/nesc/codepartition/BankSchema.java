package pl.edu.mimuw.nesc.codepartition;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Class whose objects carry information about available banks and their
 * capacity.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class BankSchema {
    /**
     * Map with the information. Keys are names of banks and values are their
     * capacities.
     */
    private final ImmutableSortedMap<String, Integer> banksMap;

    /**
     * String with name of the common bank.
     */
    private final String commonBankName;

    /**
     * Get a new builder that will create a new Banks object.
     *
     * @param commonBankName Name of the common bank in the created banks
     *                       object.
     * @param commonBankCapacity Capacity of the common bank in the created
     *                           banks object.
     * @return Newly created builder of Banks objects.
     */
    public static Builder builder(String commonBankName, int commonBankCapacity) {
        return new Builder(commonBankName, commonBankCapacity);
    }

    private BankSchema(ImmutableSortedMap<String, Integer> banksMap, String commonBankName) {
        this.banksMap = banksMap;
        this.commonBankName = commonBankName;
    }

    /**
     * Get the name of the common bank. <code>null</code> and empty string are
     * never returned by this method.
     *
     * @return Name of the common bank.
     */
    public String getCommonBankName() {
        return commonBankName;
    }

    /**
     * Get an immutable set that contains names of all banks.
     *
     * @return Names of all banks contained in a set.
     */
    public ImmutableSortedSet<String> getBanksNames() {
        return banksMap.keySet();
    }

    /**
     * Get the capacity of the bank with given name.
     *
     * @param bankName Name of a bank contained in this object.
     * @return Capacity of the bank with given name. Always positive value or
     *         zero is returned.
     * @throws IllegalStateException Object does not have information about bank
     *                               with given name.
     */
    public int getBankCapacity(String bankName) {
        checkNotNull(bankName, "name of the bank cannot be null");
        checkArgument(!bankName.isEmpty(), "name of the bank cannot be an empty string");
        checkState(banksMap.containsKey(bankName), "bank '%s' does not exist in this object", bankName);
        return banksMap.get(bankName);
    }

    /**
     * Builder for the banks object.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Information necessary to build a banks object.
         */
        private final Map<String, Integer> banksMap = new HashMap<>();
        private final String commonBankName;

        private Builder(String commonBankName, int commonBankCapacity) {
            checkNotNull(commonBankName, "name of the common bank cannot be null");
            checkArgument(!commonBankName.isEmpty(), "name of the common bank cannot be an empty string");
            checkArgument(commonBankCapacity >= 0, "capacity of the common bank cannot be negative");

            this.commonBankName = commonBankName;
            this.banksMap.put(commonBankName, commonBankCapacity);
        }

        /**
         * Adds a bank with given name and capacity.
         *
         * @param bankName Name of the bank to add.
         * @param bankCapacity Capacity of the bank to add.
         * @return <code>this</code>
         * @throws IllegalArgumentException Name of the bank is an empty string
         *                                  or the capacity of the bank is
         *                                  negative.
         * @throws IllegalStateException A bank with given name has been already
         *                               added.
         */
        public Builder addBank(String bankName, int bankCapacity) {
            checkNotNull(bankName, "name of the bank cannot be null");
            checkArgument(!bankName.isEmpty(), "name of the bank cannot be an empty string");
            checkArgument(bankCapacity >= 0, "capacity of the bank cannot be negative");
            checkState(!banksMap.containsKey(bankName), "bank with given name has been already added");

            this.banksMap.put(bankName, bankCapacity);
            return this;
        }

        public BankSchema build() {
            return new BankSchema(ImmutableSortedMap.copyOf(banksMap), commonBankName);
        }
    }
}
