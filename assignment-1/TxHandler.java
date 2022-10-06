import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        UTXOPool utxos = new UTXOPool();
        double sumOfInput = 0;
        double sumOfOutput = 0;

        for (int i = 0; i < tx.numInputs(); ++i) {
            Transaction.Input txInput = tx.getInput(i);
            if (txInput  == null) return false;

            Transaction.Output txOutput = utxoPool.getTxOutput(new UTXO(txInput.prevTxHash, txInput.outputIndex));
            if (txOutput == null) return false;

            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);

            if (!utxoPool.contains(utxo)) return false;

            if (!Crypto.verifySignature(txOutput.address, tx.getRawDataToSign(i), txInput.signature)) return false;

            if (utxos.contains(utxo)) return false;
            utxos.addUTXO(utxo, txOutput);

            sumOfInput += txOutput.value;
        }
        for (int i = 0; i < tx.numOutputs(); ++i) {
            if (tx.getOutput(i).value < 0) return false;
            sumOfOutput += tx.getOutput(i).value;
        }

        return sumOfInput > sumOfOutput;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Set<Transaction> vTxs = new HashSet<>();

        for (Transaction transaction : possibleTxs) {
            if (isValidTx(transaction)) {
                vTxs.add(transaction);
                for (Transaction.Input input : transaction.getInputs()) {
                    utxoPool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
                }

                for (int i = 0; i < transaction.numOutputs(); ++i) {
                    utxoPool.addUTXO(new UTXO(transaction.getHash(), i), transaction.getOutput(i));
                }
            }
        }

        Transaction[] validTxArray = new Transaction[vTxs.size()];
        return vTxs.toArray(validTxArray);
    }

}
