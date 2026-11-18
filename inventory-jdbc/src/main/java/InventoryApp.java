import java.sql.*;
import java.util.Scanner;

public class InventoryApp {
    private static final String URL = "jdbc:mysql://localhost:3306/retail_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    // Use "" for an empty MySQL password. Otherwise replace the text above
    // with the password for the database you are connecting to.

    public static void main(String[] args) {
        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
             Scanner sc = new Scanner(System.in)) {

            int choice;
            do {
                System.out.println("\n--- Inventory Menu ---");
                System.out.println("1. Insert three products");
                System.out.println("2. Find low-stock products by category");
                System.out.println("3. Add stock");
                System.out.println("4. Search and change product price");
                System.out.println("5. Show inventory value and delete zero-stock product");
                System.out.println("0. Exit");

                choice = readInt(sc, "Choose an option: ");

                switch (choice) {
                    case 1 -> insertThreeProducts(con, sc);
                    case 2 -> findLowStockProducts(con, sc);
                    case 3 -> addStock(con, sc);
                    case 4 -> changePrice(con, sc);
                    case 5 -> showValueAndDelete(con, sc);
                    case 0 -> System.out.println("Goodbye.");
                    default -> System.out.println("Invalid option.");
                }
            } while (choice != 0);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // (a) Insert three products. Duplicate IDs are reported and retried.
    private static void insertThreeProducts(Connection con, Scanner sc)
            throws SQLException {
        String sql = "INSERT INTO Product "
                + "(product_id, product_name, category, quantity, price) "
                + "VALUES (?, ?, ?, ?, ?)";

        int inserted = 0;
        while (inserted < 3) {
            int id = readInt(sc, "Product ID: ");
            System.out.print("Product name: ");
            String name = sc.nextLine();
            System.out.print("Category: ");
            String category = sc.nextLine();
            int quantity = readInt(sc, "Quantity: ");
            double price = readDouble(sc, "Price: ");

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.setString(2, name);
                ps.setString(3, category);
                ps.setInt(4, quantity);
                ps.setDouble(5, price);

                ps.executeUpdate();
                inserted++;
                System.out.println("Product inserted.");
            } catch (SQLException e) {
                if ("23000".equals(e.getSQLState())) {
                    System.out.println("That product ID already exists. Try another ID.");
                } else {
                    throw e;
                }
            }
        }
    }

    // (b) Category match, quantity below 20, price descending.
    private static void findLowStockProducts(Connection con, Scanner sc)
            throws SQLException {
        System.out.print("Enter category: ");
        String category = sc.nextLine();

        String sql = "SELECT * FROM Product "
                + "WHERE category = ? AND quantity < 20 "
                + "ORDER BY price DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    printProduct(rs);
                    found = true;
                }
                if (!found) {
                    System.out.println("No matching products found.");
                }
            }
        }
    }

    // (c) Add the entered amount to existing stock.
    private static void addStock(Connection con, Scanner sc) throws SQLException {
        int id = readInt(sc, "Product ID: ");
        int amount = readInt(sc, "Quantity to add: ");

        if (amount <= 0) {
            System.out.println("Quantity to add must be positive.");
            return;
        }

        String sql = "UPDATE Product SET quantity = quantity + ? "
                + "WHERE product_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();
            if (rows == 1) {
                System.out.println("Stock updated.");
            } else {
                System.out.println("Product does not exist.");
            }
        }
    }

    // (d) Check the product first; only update when it exists and price is positive.
    private static void changePrice(Connection con, Scanner sc)
            throws SQLException {
        int id = readInt(sc, "Product ID: ");
        double newPrice = readDouble(sc, "New price: ");

        if (!displayProduct(con, id)) {
            System.out.println("Product does not exist.");
            return;
        }

        if (newPrice <= 0) {
            System.out.println("Price must be greater than zero.");
            return;
        }

        String sql = "UPDATE Product SET price = ? WHERE product_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, id);
            ps.executeUpdate();
        }

        System.out.println("Updated product details:");
        displayProduct(con, id);
    }

    // (e) Show SUM(quantity * price), then delete only a zero-stock product.
    private static void showValueAndDelete(Connection con, Scanner sc)
            throws SQLException {
        String sumSql = "SELECT COALESCE(SUM(quantity * price), 0) "
                + "AS total_value FROM Product";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sumSql)) {
            if (rs.next()) {
                System.out.println("Total inventory value: "
                        + rs.getDouble("total_value"));
            }
        }

        int id = readInt(sc, "Product ID to delete: ");
        String deleteSql = "DELETE FROM Product "
                + "WHERE product_id = ? AND quantity = 0";

        try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
            ps.setInt(1, id);

            if (ps.executeUpdate() == 1) {
                System.out.println("Zero-stock product deleted.");
            } else {
                System.out.println("Product not found or its quantity is not zero.");
            }
        }
    }

    private static boolean displayProduct(Connection con, int id)
            throws SQLException {
        String sql = "SELECT * FROM Product WHERE product_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    printProduct(rs);
                    return true;
                }
            }
        }
        return false;
    }

    private static void printProduct(ResultSet rs) throws SQLException {
        System.out.println(
                rs.getInt("product_id") + " | "
                + rs.getString("product_name") + " | "
                + rs.getString("category") + " | Quantity: "
                + rs.getInt("quantity") + " | Price: "
                + rs.getDouble("price"));
    }

    private static int readInt(Scanner sc, String prompt) {
        System.out.print(prompt);
        return Integer.parseInt(sc.nextLine().trim());
    }

    private static double readDouble(Scanner sc, String prompt) {
        System.out.print(prompt);
        return Double.parseDouble(sc.nextLine().trim());
    }
}