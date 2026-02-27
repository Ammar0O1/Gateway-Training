package wallet.domain;

import lombok.Value;
import java.util.UUID;
import io.vavr.control.Either;
import java.time.LocalDateTime;
@Value
public class Wallet {
    UUID id;
    String ownerName;
    Money balance;
    CurrencyType currency;
    LocalDateTime createdAt;
    Long version;

    // deposit money into the wallet
    public Either<String, Wallet> deposit(Money amount) {
        if (amount == null){
            return Either.left("Amount must not be null");
        }
        if (!amount.isPositive()){
            return Either.left("Amount must be positive");
        }
        if  (amount.getCurrency() != this.currency){
            return Either.left("Currency types must match for deposit");
        }
     return this.balance.add(amount).map(newBalance -> new Wallet(this.id, this.ownerName, newBalance,this.currency, this.createdAt, this.version + 1));
    }

    public Either<String, Wallet> withdraw (Money amount) {
        if (amount == null){
            return Either.left("Amount must not be null");
        }
        if (!amount.isPositive()){
            return Either.left("Amount must be positive");
        }
        if (this.balance.getAmount().compareTo(amount.getAmount()) < 0) {
            return Either.left("Insufficient funds for withdrawal");
        }


        if  (amount.getCurrency() != this.currency){
            return Either.left("Currency types must match for withdrawal");
        }
        return this.balance.subtract(amount).map(newBalance -> new Wallet(this.id, this.ownerName, newBalance,this.currency, this.createdAt, this.version + 1));

    }

    // has balance

    public Either<String, Boolean> hasBalance(Money amount) {
        if (amount == null){
            return Either.left("Amount must not be null");
        }
        if (!amount.isPositive()){
            return Either.left("Amount must be positive");
        }
        if  (amount.getCurrency() != this.currency){
            return Either.left("Currency types must match for balance check");
        }
        return Either.right(this.balance.getAmount().compareTo(amount.getAmount()) >= 0);
    }




}
