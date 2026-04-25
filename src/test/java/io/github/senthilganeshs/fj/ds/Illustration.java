package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.*;

/**
 * A realistic example of using functional data structures to process a product catalog 
 * and validate customer orders.
 */
public class Illustration {

    record Product(String id, String name, double price) {}
    record OrderItem(String productId, int quantity) {}
    record Order(String orderId, String customerId, Collection<OrderItem> items) {}

    public static void main(String[] args) {
        // 1. Setup a product catalog using our HAMT-based HashMap
        HashMap<String, Product> catalog = HashMap.<String, Product>nil()
            .put("P1", new Product("P1", "Laptop", 1200.0))
            .put("P2", new Product("P2", "Mouse", 25.0))
            .put("P3", new Product("P3", "Keyboard", 75.0));

        // 2. A list of orders to process - using generic List interface
        List<Order> orders = List.of(
            new Order("O1", "C1", List.of(new OrderItem("P1", 1), new OrderItem("P2", 2))),
            new Order("O2", "C2", List.of(new OrderItem("P3", 1))),
            new Order("O3", "C1", List.of(new OrderItem("P2", 5))),
            new Order("O4", "C3", List.of(new OrderItem("INVALID", 1))) // This will fail validation
        );

        System.out.println("--- 1. Order Processing Summary ---");
        
        // Use HashMap to accumulate total spend per customer
        HashMap<String, Double> customerSpend = orders.foldl(HashMap.nil(), (acc, order) -> {
            double orderTotal = calculateOrderTotal(catalog, order).orElse(0.0);
            double currentTotal = acc.get(order.customerId).orElse(0.0);
            return acc.put(order.customerId, currentTotal + orderTotal);
        });

        customerSpend.forEach(entry -> 
            System.out.println("Customer " + entry.key() + " total spend: $" + entry.value())
        );

        System.out.println("\n--- 2. Detailed Order Validation ---");
        
        // Validate each order and use Either to handle success/failure cases
        orders.forEach(order -> {
            validateOrder(catalog, order).either(
                error -> {
                    System.out.println("[FAILED]  Order " + order.orderId + ": " + error);
                    return null;
                },
                total -> {
                    System.out.println("[SUCCESS] Order " + order.orderId + ": Processed $" + total);
                    return null;
                }
            );
        });

        System.out.println("\n--- 3. Lazy sequence of Transaction IDs ---");
        
        // Use List interface even for lazy implementations
        List<String> txIds = LazyList.iterate(1001, i -> i + 1).map(i -> "TX-" + i);
        System.out.println("Next 5 Transaction IDs: " + txIds.take(5));
    }

    /**
     * Calculates the total price of an order. 
     * Returns Maybe.nothing() if any product in the order is not in the catalog.
     */
    private static Maybe<Double> calculateOrderTotal(HashMap<String, Product> catalog, Order order) {
        return (Maybe<Double>) order.items.foldl(Maybe.some(0.0), (accMaybe, item) -> 
            (Maybe<Double>) accMaybe.flatMap(acc -> 
                catalog.get(item.productId).map(p -> acc + (p.price() * item.quantity()))
            )
        );
    }

    /**
     * Validates an order and returns Either the total price or an error message.
     */
    private static Either<String, Double> validateOrder(HashMap<String, Product> catalog, Order order) {
        return calculateOrderTotal(catalog, order).foldl(
            Either.left("Invalid product ID found in order " + order.orderId),
            (acc, total) -> Either.right(total)
        );
    }
}
