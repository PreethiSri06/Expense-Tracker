import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// ─── Model ────────────────────────────────────────────────────────────────────

class Transaction {
    private static int idCounter = 1;

    private int id;
    private String type;       // "income" or "expense"
    private String category;
    private String description;
    private double amount;
    private LocalDate date;

    public Transaction(String type, String category, String description, double amount, LocalDate date) {
        this.id          = idCounter++;
        this.type        = type;
        this.category    = category;
        this.description = description;
        this.amount      = amount;
        this.date        = date;
    }

    // Getters
    public int       getId()          { return id; }
    public String    getType()        { return type; }
    public String    getCategory()    { return category; }
    public String    getDescription() { return description; }
    public double    getAmount()      { return amount; }
    public LocalDate getDate()        { return date; }

    @Override
    public String toString() {
        return String.format("[%d] %s | %-12s | %-20s | ₹%8.2f | %s",
                id, type.toUpperCase(), category, description, amount,
                date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
    }
}

// ─── Service ──────────────────────────────────────────────────────────────────

class ExpenseService {

    private List<Transaction> transactions = new ArrayList<>();

    /** Add a new transaction */
    public void add(String type, String category, String description, double amount, LocalDate date) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        transactions.add(new Transaction(type, category, description, amount, date));
    }

    /** Delete by ID */
    public boolean delete(int id) {
        return transactions.removeIf(t -> t.getId() == id);
    }

    /** All transactions (newest first) */
    public List<Transaction> getAll() {
        List<Transaction> copy = new ArrayList<>(transactions);
        copy.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return Collections.unmodifiableList(copy);
    }

    /** Filter by type */
    public List<Transaction> getByType(String type) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : transactions)
            if (t.getType().equalsIgnoreCase(type)) result.add(t);
        result.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return result;
    }

    /** Filter by month (1–12) and year */
    public List<Transaction> getByMonth(int month, int year) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : transactions)
            if (t.getDate().getMonthValue() == month && t.getDate().getYear() == year)
                result.add(t);
        result.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return result;
    }

    /** Total income */
    public double totalIncome() {
        return transactions.stream()
                .filter(t -> t.getType().equals("income"))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /** Total expenses */
    public double totalExpenses() {
        return transactions.stream()
                .filter(t -> t.getType().equals("expense"))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /** Net balance */
    public double balance() {
        return totalIncome() - totalExpenses();
    }

    /** Expenses grouped by category → used to build pie chart data */
    public Map<String, Double> expensesByCategory() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            if (t.getType().equals("expense")) {
                map.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }
        // Sort descending by value
        List<Map.Entry<String, Double>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        Map<String, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : entries) sorted.put(e.getKey(), e.getValue());
        return sorted;
    }

    /** Monthly totals for the last N months → used to build bar chart data */
    public Map<String, double[]> last6MonthsTrend() {
        Map<String, double[]> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 5; i >= 0; i--) {
            LocalDate m = today.minusMonths(i);
            String label = m.format(fmt);
            double income   = 0, expense = 0;
            for (Transaction t : transactions) {
                if (t.getDate().getMonthValue() == m.getMonthValue()
                        && t.getDate().getYear() == m.getYear()) {
                    if (t.getType().equals("income"))  income  += t.getAmount();
                    else                               expense += t.getAmount();
                }
            }
            result.put(label, new double[]{income, expense});
        }
        return result;
    }

    /** Print summary to console */
    public void printSummary() {
        System.out.println("\n========== EXPENSE TRACKER SUMMARY ==========");
        System.out.printf("  Total Income   : ₹%.2f%n", totalIncome());
        System.out.printf("  Total Expenses : ₹%.2f%n", totalExpenses());
        System.out.printf("  Net Balance    : ₹%.2f%n", balance());

        System.out.println("\n--- Expenses by Category ---");
        expensesByCategory().forEach((cat, amt) ->
                System.out.printf("  %-15s : ₹%.2f%n", cat, amt));

        System.out.println("\n--- All Transactions ---");
        getAll().forEach(System.out::println);
        System.out.println("==============================================\n");
    }
}

// ─── Main / Demo ──────────────────────────────────────────────────────────────

public class ExpenseTracker {

    public static void main(String[] args) {
        ExpenseService service = new ExpenseService();
        LocalDate now = LocalDate.now();

        // Seed some demo data
        service.add("income",  "Salary",        "Monthly salary",         55000, now.withDayOfMonth(1));
        service.add("income",  "Freelance",      "Website project",        12000, now.minusMonths(1).withDayOfMonth(15));
        service.add("expense", "Food",           "Groceries",               3200, now.minusDays(2));
        service.add("expense", "Food",           "Zomato orders",           1800, now.minusDays(5));
        service.add("expense", "Transport",      "Petrol",                  2500, now.minusDays(3));
        service.add("expense", "Transport",      "Uber rides",               900, now.minusDays(7));
        service.add("expense", "Utilities",      "Electricity bill",        1400, now.minusDays(10));
        service.add("expense", "Entertainment",  "Netflix + Hotstar",        600, now.minusDays(1));
        service.add("expense", "Shopping",       "Clothes",                 4500, now.minusDays(4));
        service.add("expense", "Health",         "Gym membership",          1200, now.withDayOfMonth(1));
        service.add("expense", "Education",      "Udemy courses",           2000, now.minusDays(8));
        service.add("income",  "Salary",         "Monthly salary",         55000, now.minusMonths(1).withDayOfMonth(1));
        service.add("expense", "Food",           "Restaurant dinner",       2100, now.minusMonths(1).minusDays(3));
        service.add("expense", "Shopping",       "Electronics",             8000, now.minusMonths(1).minusDays(12));

        service.printSummary();

        // Demo: monthly trend
        System.out.println("--- Last 6 Months Trend ---");
        service.last6MonthsTrend().forEach((month, vals) ->
                System.out.printf("  %-12s  Income: ₹%.0f  |  Expense: ₹%.0f%n",
                        month, vals[0], vals[1]));

        // Demo: delete a transaction
        System.out.println("\nDeleting transaction ID 3...");
        boolean removed = service.delete(3);
        System.out.println(removed ? "Deleted successfully." : "Not found.");
        System.out.printf("New total expenses: ₹%.2f%n", service.totalExpenses());
    }
}
