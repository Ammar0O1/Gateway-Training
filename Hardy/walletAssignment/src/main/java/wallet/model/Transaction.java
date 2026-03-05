package wallet.model;

import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Transaction {
    private final UUID transactionId;
    private final TransactionType type;
    private final UUID fromWallet;
    private final UUID toWallet;
    private final BigDecimal amount;
    private final LocalDateTime transactionDate;
    private final TransactionStatus status;

    public Transaction(TransactionType type, BigDecimal amount, TransactionStatus status, UUID fromWallet, UUID toWallet) {

        if (type == null) throw new IllegalArgumentException("Transaction type cannot be null");
        if (status == null) throw new IllegalArgumentException("Transaction status cannot be null");
        if (status == TransactionStatus.SUCCESS) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Amount must be positive");
        }

        this.transactionId = UUID.randomUUID();
        this.type = type;
        this.amount = amount;
        this.transactionDate = LocalDateTime.now();
        this.status = status;
        this.fromWallet = fromWallet;
        this.toWallet = toWallet;
    }

    @Override
    public String toString() {
        return String.format(
                "Transaction [transactionId=%s, type=%s, amount=%s, transactionDate=%s, status=%s, fromWallet=%s, toWallet=%s]",
                transactionId, type, amount, transactionDate, status, fromWallet, toWallet
        );
    }
}