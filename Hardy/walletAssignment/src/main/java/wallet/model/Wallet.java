package wallet.model;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import io.vavr.control.Either;
import wallet.error.DomainError;

@Getter

public class Wallet {

    private final UUID id;
    private final String ownerName;
    private final Currency currency;
    private final LocalDateTime createdDate;
    private BigDecimal balance;

    public Wallet(String ownerName, Currency currency) {

        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name cannot be null or blank");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }

        this.id = UUID.randomUUID();
        this.ownerName = ownerName;
        this.currency = currency;
        this.createdDate = LocalDateTime.now();
        this.balance = BigDecimal.ZERO;
    }


    public synchronized Either<DomainError, Wallet> deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return Either.left(DomainError.INVALID_AMOUNT);

        this.balance = this.balance.add(amount);
        return Either.right(this);
    }

    public synchronized Either<DomainError, Wallet> withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return Either.left(DomainError.INVALID_AMOUNT);

        if (balance.compareTo(amount) < 0)
            return Either.left(DomainError.INSUFFICIENT_BALANCE);

        this.balance = this.balance.subtract(amount);
        return Either.right(this);
    }

    @Override
    public String toString() {
        return String.format("Wallet [id=%s, ownerName=%s, currency=%s, createdDate=%s, balance=%s]", id, ownerName, currency, createdDate, balance);
    }

}

