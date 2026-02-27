package wallet.domain;

import java.math.BigDecimal;
import io.vavr.control.Either;
import lombok.Value;
@Value
public class Money {
        BigDecimal amount;
        CurrencyType currency;


    public static Either<String, Money> create(BigDecimal amount, CurrencyType currency) {
         if (amount == null ) {
             return  Either.left("Amount must not be null");
         }
        if (currency == null) {
                return Either.left("Currency must not be null");
            }
         if (amount.compareTo(BigDecimal.ZERO) < 0) {
             return Either.left(("Amount must be non-negative"));
         }
            return Either.right(new Money(amount, currency));

     }

     public  Either <String, Money> add(Money other){
         if(other == null){
             return Either.left("Other money must not be null");
         }

         if(this.currency != other.currency){
             return Either.left("Currency types must match for addition");
         }

         return Either.right(new Money(this.amount.add(other.amount), this.currency));
     }

     public Either <String, Money> subtract (Money other){
         if(other == null) {
             return Either.left("Other money must not be null");
         }
         if (this.currency != other.currency) {
             return Either.left("Currency types must match for subtraction");
         }
            if (this.amount.compareTo(other.amount) < 0) {
                return Either.left("Insufficient funds for subtraction");
            }

            return Either.right(new Money(this.amount.subtract(other.amount), this.currency));
     }

    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0  ;
    }

     public Either<String, Boolean> isGreaterThan (Money other){
            if(other == null) {
                return Either.left("Other money must not be null");
            }
            if (this.currency != other.currency) {
                return Either.left("Currency types must match for comparison");
            }

            return Either.right(this.amount.compareTo(other.amount) > 0);
     }


}