package app.freerouting.autoroute.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.freerouting.Freerouting;
import app.freerouting.board.facade.BasicBoard;
import app.freerouting.board.model.items.Item;
import app.freerouting.board.model.items.Trace;
import app.freerouting.board.model.items.Via;
import app.freerouting.board.model.structure.FixedState;
import app.freerouting.datastructures.UndoableObjects;
import app.freerouting.geometry.planar.FloatPoint;
import app.freerouting.io.specctra.DsnTestFixtures;
import app.freerouting.settings.GlobalSettings;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The sorted candidate order must equal the order of the former selection loop. */
class BatchOptimizerOrderTest {

  @BeforeEach
  void setUp() {
    Freerouting.globalSettings = new GlobalSettings();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Issue103-Board-Routed.dsn",
        "Issue575-drc_BBD_Mars-64_6_track_1_hole_clearance_violations.dsn",
        "Issue187-processor.Z80.dsn",
        "Issue756-tomu-fpga8.dsn"
      })
  void matchesTheSelectionLoop(String fixture) throws Exception {
    BasicBoard board = DsnTestFixtures.loadBoard(fixture);
    // DSN wiring may be imported fixed; unfix it so every trace and via is a candidate.
    for (Item item : board.getItems()) {
      if (item instanceof Trace || item instanceof Via) {
        item.setFixedState(FixedState.UNFIXED);
      }
    }
    List<Item> expected = new ArrayList<>();
    SelectionLoop loop = new SelectionLoop(board);
    for (Item item = loop.next(); item != null; item = loop.next()) {
      expected.add(item);
    }
    assertFalse(expected.isEmpty(), "fixture should have candidates");
    assertEquals(expected, BatchOptimizer.optimizationOrder(board));
  }

  /** The candidate selection as it was before the sort, kept as the reference. */
  private static final class SelectionLoop {
    private final BasicBoard board;
    private FloatPoint minItemCoor = new FloatPoint(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private int minItemLayer = -1;

    SelectionLoop(BasicBoard board) {
      this.board = board;
    }

    Item next() {
      Item result = null;
      FloatPoint currentMinCoor = new FloatPoint(Integer.MAX_VALUE, Integer.MAX_VALUE);
      int currentMinLayer = Integer.MAX_VALUE;
      Iterator<UndoableObjects.UndoableObjectNode> it = board.itemList.startReadObject();
      for (; ; ) {
        UndoableObjects.Storable currentItem = board.itemList.readObject(it);
        if (currentItem == null) {
          break;
        }
        if (currentItem instanceof Via currentVia) {
          if (!currentVia.isUserFixed() && !currentVia.hasIgnoredNets()) {
            FloatPoint currentViaCenter = currentVia.getCenter().toFloat();
            int currentViaMinLayer = currentVia.firstLayer();
            if (currentViaCenter.x > minItemCoor.x
                || currentViaCenter.x == minItemCoor.x
                    && (currentViaCenter.y > minItemCoor.y
                        || currentViaCenter.y == minItemCoor.y
                            && currentViaMinLayer > minItemLayer)) {
              if (currentViaCenter.x < currentMinCoor.x
                  || currentViaCenter.x == currentMinCoor.x
                      && (currentViaCenter.y < currentMinCoor.y
                          || currentViaCenter.y == currentMinCoor.y
                              && currentViaMinLayer < currentMinLayer)) {
                currentMinCoor = currentViaCenter;
                currentMinLayer = currentViaMinLayer;
                result = currentVia;
              }
            }
          }
        }
      }
      it = board.itemList.startReadObject();
      for (; ; ) {
        UndoableObjects.Storable currentItem = board.itemList.readObject(it);
        if (currentItem == null) {
          break;
        }
        if (currentItem instanceof Trace currentTrace) {
          if (!currentTrace.isShoveFixed() && !currentTrace.hasIgnoredNets()) {
            FloatPoint firstCorner = currentTrace.firstCorner().toFloat();
            FloatPoint lastCorner = currentTrace.lastCorner().toFloat();
            FloatPoint compareCorner;
            if (firstCorner.x < lastCorner.x
                || firstCorner.x == lastCorner.x && firstCorner.y < lastCorner.y) {
              compareCorner = lastCorner;
            } else {
              compareCorner = firstCorner;
            }
            int currentTraceLayer = currentTrace.getLayer();
            if (compareCorner.x > minItemCoor.x
                || compareCorner.x == minItemCoor.x
                    && (compareCorner.y > minItemCoor.y
                        || compareCorner.y == minItemCoor.y && currentTraceLayer > minItemLayer)) {
              if (compareCorner.x < currentMinCoor.x
                  || compareCorner.x == currentMinCoor.x
                      && (compareCorner.y < currentMinCoor.y
                          || compareCorner.y == currentMinCoor.y
                              && currentTraceLayer < currentMinLayer)) {
                boolean isConnectedToVia = false;
                for (Item currentContact : currentTrace.getNormalContacts()) {
                  if (currentContact instanceof Via && !currentContact.isUserFixed()) {
                    isConnectedToVia = true;
                    break;
                  }
                }
                if (!isConnectedToVia) {
                  currentMinCoor = compareCorner;
                  currentMinLayer = currentTraceLayer;
                  result = currentTrace;
                }
              }
            }
          }
        }
      }
      minItemCoor = currentMinCoor;
      minItemLayer = currentMinLayer;
      return result;
    }
  }
}
