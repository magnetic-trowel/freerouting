package app.freerouting.drc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Trace;
import app.freerouting.io.BoardReadResult;
import app.freerouting.io.specctra.DsnReader;
import app.freerouting.settings.GlobalSettings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The hole clearance of a via is checked against each item with that item's own clearance class.
 * Via: pad radius 300 um, drill radius 150 um, default class; hole clearance 500 um, so other
 * copper must stay 350 um from the pad edge (and the class clearance).
 */
class HoleClearanceClassTest {

  private static final String BOARD =
      """
      (pcb hole-class.dsn
        (parser (string_quote ") (space_in_quoted_tokens on) (host_cad "KiCad's Pcbnew"))
        (resolution um 10)
        (unit um)
        (structure
          (layer F.Cu (type signal) (property (index 0)))
          (layer B.Cu (type signal) (property (index 1)))
          (boundary (path pcb 0  0 0  20000 0  20000 20000  0 20000  0 0))
          (via "Via[0-1]_600:300_um")
          (rule (width 200) (clearance 200))
        )
        (placement)
        (library
          (padstack "Via[0-1]_600:300_um"
            (shape (circle F.Cu 600)) (shape (circle B.Cu 600)) (attach off))
        )
        (network
          (net A) (net D) (net W) (net X)
          (class kicad_default A D (circuit (use_via "Via[0-1]_600:300_um"))
            (rule (width 200) (clearance 200)))
          (class wide W X (circuit (use_via "Via[0-1]_600:300_um"))
            (rule (width 200) (clearance 400)))
        )
        (wiring
          (via "Via[0-1]_600:300_um" 10000 10000 (net A))
          (wire (path F.Cu 200 10850 2000 10850 18000) (net W) (type route))
          (wire (path F.Cu 200 8000 2000 8000 18000) (net X) (type route))
          (wire (path B.Cu 200 10600 2000 10600 18000) (net D) (type route))
        )
      )
      """;

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @Test
  void holeClearanceUsesTheClassOfTheOtherItem() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(BOARD.getBytes(StandardCharsets.UTF_8)), null, null);
    assertInstanceOf(BoardReadResult.Success.class, result);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    board.rules.setHoleClearance(5000);
    board.searchTreeManager.reinsertTreeItems();

    Collection<ClearanceViolation> violations =
        new DesignRulesChecker(board, null).getAllClearanceViolations();

    Set<String> nets = new TreeSet<>();
    for (ClearanceViolation violation : violations) {
      for (Item item : new Item[] {violation.firstItem, violation.secondItem}) {
        if (item instanceof Trace trace) {
          nets.add(board.rules.nets.get(trace.getNetNumber(0)).name);
        }
      }
    }
    // W: 450 um from the pad, 600 um from the hole: clear of both its 400 um class clearance
    // and the hole clearance. X: far away. D: 200 um clearance kept (200 um from the pad) but
    // only 450 um from the hole.
    assertEquals(Set.of("D"), nets, violations.toString());
    // D keeps the copper clearance, so it is a hole clearance violation, measured from the hole.
    ClearanceViolation violation = violations.iterator().next();
    assertTrue(violation.holeClearance);
    assertEquals(5000, violation.expectedClearance, 1);
    assertEquals(3500, violation.actualClearance, 20);
  }

  @Test
  void copperClearanceViolationNearAViaStaysAClearanceViolation() {
    BoardReadResult result =
        DsnReader.readBoard(
            new ByteArrayInputStream(
                BOARD
                    .replace("10850 2000 10850 18000", "10650 2000 10650 18000")
                    .getBytes(StandardCharsets.UTF_8)),
            null,
            null);
    RoutingBoard board = (RoutingBoard) ((BoardReadResult.Success) result).board();
    board.rules.setHoleClearance(5000);
    board.searchTreeManager.reinsertTreeItems();

    // W is now 250 um from the pad: inside its 400 um copper clearance.
    long copperViolations =
        new DesignRulesChecker(board, null)
            .getAllClearanceViolations().stream().filter(v -> !v.holeClearance).count();
    assertEquals(1, copperViolations);
  }
}
