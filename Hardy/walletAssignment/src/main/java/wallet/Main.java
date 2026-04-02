package wallet;

import wallet.model.Currency;
import wallet.model.Wallet;
import wallet.service.WalletService;
import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {

        WalletService service = new WalletService();

        // ── Create Wallets ──────────────────────────────────────────
        System.out.println("========== CREATE WALLETS ==========");
        Wallet ali  = service.createWallet("Ali",  Currency.IQD);
        Wallet sara = service.createWallet("Sara", Currency.IQD);
        Wallet john = service.createWallet("John", Currency.USD);
        System.out.println(ali);
        System.out.println(sara);
        System.out.println(john);
        System.out.println();

        // ── Deposit ─────────────────────────────────────────────────
        System.out.println("========== DEPOSIT ==========");

        service.deposit(ali.getId(), new BigDecimal("100000"))
                .peek(w  -> System.out.println("SUCCESS | Ali deposited 100000 | balance: " + w.getBalance()))
                .peekLeft(e -> System.out.println("FAILED  | " + e.message()));

        service.deposit(sara.getId(), new BigDecimal("50000"))
                .peek(w  -> System.out.println("SUCCESS | Sara deposited 50000 | balance: " + w.getBalance()))
                .peekLeft(e -> System.out.println("FAILED  | " + e.message()));

        // invalid amount
        service.deposit(ali.getId(), new BigDecimal("-500"))
                .peek(w  -> System.out.println("SUCCESS | deposited -500"))
                .peekLeft(e -> System.out.println("FAILED  | deposit -500 → " + e.message()));

        // zero amount
        service.deposit(ali.getId(), BigDecimal.ZERO)
                .peek(w  -> System.out.println("SUCCESS | deposited 0"))
                .peekLeft(e -> System.out.println("FAILED  | deposit 0 → " + e.message()));
        System.out.println();

        // ── Withdraw ────────────────────────────────────────────────
        System.out.println("========== WITHDRAW ==========");

        service.withdraw(sara.getId(), new BigDecimal("10000"))
                .peek(w  -> System.out.println("SUCCESS | Sara withdrew 10000 | balance: " + w.getBalance()))
                .peekLeft(e -> System.out.println("FAILED  | " + e.message()));

        // insufficient balance
        service.withdraw(sara.getId(), new BigDecimal("999999"))
                .peek(w  -> System.out.println("SUCCESS | withdrew 999999"))
                .peekLeft(e -> System.out.println("FAILED  | withdraw 999999 → " + e.message()));
        System.out.println();

        // ── Transfer ────────────────────────────────────────────────
        System.out.println("========== TRANSFER ==========");

        // success - same currency
        service.transfer(ali.getId(), sara.getId(), new BigDecimal("20000"))
                .peek(t  -> System.out.println("SUCCESS | Ali → Sara 20000 | " + t))
                .peekLeft(e -> System.out.println("FAILED  | " + e.message()));

        // self transfer
        service.transfer(ali.getId(), ali.getId(), new BigDecimal("1000"))
                .peek(t  -> System.out.println("SUCCESS | self transfer"))
                .peekLeft(e -> System.out.println("FAILED  | self transfer → " + e.message()));

        // insufficient balance
        service.transfer(sara.getId(), ali.getId(), new BigDecimal("999999"))
                .peek(t  -> System.out.println("SUCCESS | transferred 999999"))
                .peekLeft(e -> System.out.println("FAILED  | insufficient → " + e.message()));

        // currency mismatch
        service.transfer(ali.getId(), john.getId(), new BigDecimal("5000"))
                .peek(t  -> System.out.println("SUCCESS | IQD → USD transfer"))
                .peekLeft(e -> System.out.println("FAILED  | currency mismatch → " + e.message()));
        System.out.println();

        // ── Balance ─────────────────────────────────────────────────
        System.out.println("========== BALANCES ==========");

        service.getBalance(ali.getId())
                .peek(b  -> System.out.println("Ali  balance: " + b + " IQD"))
                .peekLeft(e -> System.out.println("FAILED | " + e.message()));

        service.getBalance(sara.getId())
                .peek(b  -> System.out.println("Sara balance: " + b + " IQD"))
                .peekLeft(e -> System.out.println("FAILED | " + e.message()));

        service.getBalance(john.getId())
                .peek(b  -> System.out.println("John balance: " + b + " USD"))
                .peekLeft(e -> System.out.println("FAILED | " + e.message()));
        System.out.println();

        // ── Transaction History ─────────────────────────────────────
        System.out.println("========== ALI TRANSACTION HISTORY ==========");
        service.getTransactionHistory(ali.getId())
                .peek(list -> {
                    if (list.isEmpty()) System.out.println("No transactions found.");
                    else list.forEach(System.out::println);
                })
                .peekLeft(e -> System.out.println("FAILED | " + e.message()));
        System.out.println();


    }
}
