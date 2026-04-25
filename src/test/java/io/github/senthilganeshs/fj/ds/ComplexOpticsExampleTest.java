package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ComplexOpticsExampleTest {

    record Location(String city) {}
    record Shipment(String id, Location destination) {}
    record Order(String orderId, List<Either<String, Shipment>> results) {}

    @Test
    public void testDeepReroutingWithOptics() {
        Lens<Shipment, Location> destL = Lens.of(Shipment::destination, (l, s) -> new Shipment(s.id(), l));
        Lens<Location, String> cityL = Lens.of(Location::city, (c, l) -> new Location(c));
        
        Prism<Either<String, Shipment>, Shipment> successP = Prism.of(
            e -> e.isRight() ? Maybe.some(e.orElse(null)) : Maybe.nothing(),
            Either::right
        );

        Traversal<Collection<Either<String, Shipment>>, Either<String, Shipment>> eachResult = Traversal.fromCollection();

        Traversal<Collection<Either<String, Shipment>>, String> allCitiesT = 
            eachResult
            .compose(successP) 
            .compose(destL)    
            .compose(cityL);   

        Order oldOrder = new Order("ORD-1", List.of(
            Either.right(new Shipment("SHP-1", new Location("Paris"))),
            Either.left("Warehouse unavailable"),
            Either.right(new Shipment("SHP-2", new Location("Berlin")))
        ));

        Lens<Order, List<Either<String, Shipment>>> resultsL = Lens.of(Order::results, (res, o) -> new Order(o.orderId(), res));
        
        // Execute the deep update
        Order updatedOrder = resultsL.modify(oldOrder, results -> List.from(allCitiesT.set("London", results)));

        // Manual verification for clarity
        Either<String, Shipment> first = updatedOrder.results().headMaybe().orElse(null);
        Assert.assertEquals(first.orElse(null).destination().city(), "London");

        Either<String, Shipment> second = updatedOrder.results().drop(1).headMaybe().orElse(null);
        Assert.assertTrue(second.isLeft());
        Assert.assertEquals(second.toString(), "Left Warehouse unavailable");

        Either<String, Shipment> third = updatedOrder.results().lastMaybe().orElse(null);
        Assert.assertEquals(third.orElse(null).destination().city(), "London");
    }
}
