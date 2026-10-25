package app.freerouting.board.model.items;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import app.freerouting.autoroute.ItemAutorouteInfo;
import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.core.RoutingJob;
import app.freerouting.datastructures.ShapeTree;
import app.freerouting.management.HeadlessBoardManager;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Reading a tree shape must not clear the item's other derived data. */
class ItemTreeShapeTest {

  private static final String DSN =
      """
      (pcb shapes (resolution um 1)
        (structure (layer top (type signal)) (layer bottom (type signal))
          (boundary (rect pcb 0 0 20000 20000))
          (via V) (rule (width 200) (clearance 200)))
        (placement (component C (place C1 5000 5000 front 0) (place C2 15000 15000 front 0)))
        (library
          (image C (pin P 1 0 0))
          (padstack P (shape (rect top -500 -500 500 500)))
          (padstack V (shape (circle top 600)) (shape (circle bottom 600)) (attach off)))
        (network (net A (pins C1-1 C2-1))))
      """;

  @Test
  void anIndexOutsideTheShapesKeepsTheAutorouteInfo() {
    HeadlessBoardManager manager = new HeadlessBoardManager(new RoutingJob());
    manager.loadFromSpecctraDsn(
        new ByteArrayInputStream(DSN.getBytes(StandardCharsets.UTF_8)),
        null,
        new ItemIdGenerator());
    RoutingBoard board = manager.getRoutingBoard();
    Item pin = board.getItems().stream().filter(Pin.class::isInstance).findFirst().orElseThrow();
    ShapeTree tree = board.searchTreeManager.getDefaultTree();
    final ItemAutorouteInfo info = pin.getAutorouteInfo();

    assertNotNull(pin.getTreeShape(tree, 0));
    assertNull(pin.getTreeShape(tree, pin.treeShapeCount(tree)));
    assertNull(pin.getTreeShape(tree, -1));

    assertSame(info, pin.getAutorouteInfoPur(), "a getter must not drop the autoroute info");
  }
}
