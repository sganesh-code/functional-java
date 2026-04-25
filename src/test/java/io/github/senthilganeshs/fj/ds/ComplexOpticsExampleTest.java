package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ComplexOpticsExampleTest {

    record Location(String city) {}
    record Shipment(String id, Location destination) {}
    record Order(String orderId, List<Either<String, Shipment>> results) {}

    @Test
    public void testDeepReroutingWithSimplifiedOptics() {
        // 1. Basic Lenses
        Lens<Shipment, Location> destL = Lens.of(Shipment::destination, (l, s) -> new Shipment(s.id(), l));
        Lens<Location, String> cityL = Lens.of(Location::city, (c, l) -> new Location(c));
        
        // 2. Composed "Master Optic" using Standard Optics factories
        // Reach into List -> through the Either (if Success) -> into Shipment -> into Location -> target City
        Traversal<Collection<Either<String, Shipment>>, String> allCitiesT = 
            Collection.<Either<String, Shipment>>eachP() // Focus each element
            .compose(Either.rightP())                    // Focus on Right (Success)
            .compose(destL)                             // Focus on Destination
            .compose(cityL);                            // Focus on City

        // 3. Setup Data
        Order oldOrder = new Order("ORD-1", List.of(
            Either.right(new Shipment("SHP-1", new Location("Paris"))),
            Either.left("Warehouse unavailable"),
            Either.right(new Shipment("SHP-2", new Location("Berlin")))
        ));

        // 4. Update using a lens for the Order record results field
        Lens<Order, List<Either<String, Shipment>>> resultsL = Lens.of(Order::results, (res, o) -> new Order(o.orderId(), res));
        
        Order updatedOrder = resultsL.modify(oldOrder, results -> List.from(allCitiesT.set("London", results)));

        // 5. Verify
        Either<String, Shipment> first = updatedOrder.results().headMaybe().orElse(null);
        Assert.assertEquals(first.orElse(null).destination().city(), "London");

        Either<String, Shipment> second = updatedOrder.results().drop(1).headMaybe().orElse(null);
        Assert.assertTrue(second.isLeft());

        Either<String, Shipment> third = updatedOrder.results().lastMaybe().orElse(null);
        Assert.assertEquals(third.orElse(null).destination().city(), "London");
    }
}
