package com.manager;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Either;


public class Main {
    public static void main(String[] args) {

        List<User> users = getUsers();

        System.out.println("Testing getUserEmail with all users:");
        users.forEach(user -> {
            Either<DomainError, String> result = getUserEmail(user);
            if (result.isRight()) {
                System.out.println("Success: " + result.get());
            } else {
                System.out.println("Error: " + result.getLeft());
            }
        });

        System.out.println("\nTesting getUserEmail with null user:");
        Either<DomainError, String> nullUserResult = getUserEmail(null);
        if (nullUserResult.isRight()) {
            System.out.println("Success: " + nullUserResult.get());
        } else {
            System.out.println("Error: " + nullUserResult.getLeft());
        }
        System.out.println("\nTesting withdraw with valid account:");
        Account account = new Account(100,true);
        Either<DomainError,Account> withdrawResult = withdraw(account,50);
        System.out.println(withdrawResult.get());

        System.out.println("\nTesting withdraw with invalid account:");
        withdrawResult = withdraw(account,150);
        System.out.println(withdrawResult.getLeft());
    }

    public static List<User> getUsers() {
        return List.of(
                new User("a@test.com", true),
                new User("b@test.com", false),
                new User(null,true)

        );
    }

    public static Either<DomainError,String> getUserEmail(User user) {
        Either<DomainError, User> userEither =
                Option.of(user)
                        .toEither(DomainError.USER_NOT_FOUND)
                        .filterOrElse(User::isActive,
                                    u-> DomainError.USER_INACTIVE);

        return userEither.flatMap(u->
                        Option.of(u.getEmail())
                .filter(email ->  !email.isEmpty())
                .toEither(DomainError.EMAIL_MISSING)
        );
    }

    public static Either<DomainError,Account> withdraw(Account account, double amount) {
        return Option.of(account)
                .toEither(DomainError.ACCOUNT_NOT_FOUND)
                .filterOrElse(Account::isActive,a->DomainError.USER_INACTIVE)
                .filterOrElse(a-> amount>0,a->DomainError.INVALID_AMOUNT)
                .filterOrElse(a-> a.getBalance()>=amount,a->DomainError.INSUFFICIENT_FUNDS)
                .map(a-> new Account(a.getBalance()-amount,a.isActive()));

    }
}
