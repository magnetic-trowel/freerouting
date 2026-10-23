package app.freerouting.board.model.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.board.facade.RoutingBoard;
import app.freerouting.board.searchtree.ShapeSearchTree;
import app.freerouting.board.state.Communication;
import app.freerouting.geometry.planar.IntPoint;
import app.freerouting.geometry.planar.Line;
import app.freerouting.geometry.planar.PolygonShape;
import app.freerouting.geometry.planar.PolylineShape;
import app.freerouting.geometry.planar.TileShape;
import app.freerouting.rules.BoardRules;
import app.freerouting.rules.ClearanceMatrix;
import org.junit.jupiter.api.Test;

/** The outline strips stored as octagons or boxes must not block the inside of the board. */
class BoardOutlineEdgePiecesTest {

  /**
   * Coordinates are mils (the default communication); the board is about 100 mm x 50 mm. The
   * outline strips reach {@link BoardOutline#getHalfWidth()} = 100 board units on each side.
   */
  private static RoutingBoard board(AngleRestriction angles, IntPoint... corners) {
    LayerStructure layers =
        new LayerStructure(new Layer[] {new Layer("Top", true), new Layer("Bottom", true)});
    BoardRules rules = new BoardRules(layers, ClearanceMatrix.getDefaultInstance(layers, 4));
    rules.createDefaultNetClass();
    rules.setTraceAngleRestriction(angles);
    PolylineShape outline = new PolygonShape(corners);
    return new RoutingBoard(
        outline.boundingBox(),
        layers,
        new PolylineShape[] {outline},
        0,
        rules,
        new Communication());
  }

  private static boolean outlineCovers(RoutingBoard board, IntPoint point) {
    ShapeSearchTree tree = board.searchTreeManager.getAutorouteTree(1);
    BoardOutline outline = board.getOutline();
    TileShape pointShape = TileShape.getInstance(point);
    for (int i = 0; i < outline.treeShapeCount(tree); i++) {
      if (outline.getTreeShape(tree, i).intersects(pointShape)) {
        return true;
      }
    }
    return false;
  }

  @Test
  void rectangularOutlineKeepsOneStripPerEdge() {
    RoutingBoard board =
        board(
            AngleRestriction.FORTYFIVE_DEGREE,
            new IntPoint(0, 0),
            new IntPoint(4000, 0),
            new IntPoint(4000, 2000),
            new IntPoint(0, 2000));
    BoardOutline outline = board.getOutline();
    PolylineShape shape = outline.getShape(0);
    Line[][] pieces = outline.edgePieces();
    assertEquals(4, pieces.length);
    for (int i = 0; i < 4; i++) {
      assertSame(shape.borderLine((i + 3) % 4).getClass(), pieces[i][0].getClass());
      assertTrue(shape.borderLine((i + 3) % 4).equals(pieces[i][0]));
      assertTrue(shape.borderLine(i).equals(pieces[i][1]));
      assertTrue(shape.borderLine((i + 1) % 4).equals(pieces[i][2]));
    }
  }

  @Test
  void steepEdgeDoesNotBlockTheBoardInTheFortyFiveDegreeTree() {
    // The hypotenuse from (4000, 2000) to (0, 0) runs at 26.6 degrees.
    RoutingBoard board =
        board(
            AngleRestriction.FORTYFIVE_DEGREE,
            new IntPoint(0, 0),
            new IntPoint(4000, 0),
            new IntPoint(4000, 2000));
    assertTrue(board.getOutline().edgePieces().length > 3, "the hypotenuse is cut into pieces");
    // 270 mil (6.8 mm) below the hypotenuse: outside its strip, inside the strip's whole octagon.
    assertFalse(outlineCovers(board, new IntPoint(2400, 900)));
    // The outline itself is still covered.
    assertTrue(outlineCovers(board, new IntPoint(2400, 1200)));
  }

  @Test
  void fortyFiveDegreeEdgeStaysWholeExceptForTheNinetyDegreeTree() {
    IntPoint[] corners = {new IntPoint(0, 0), new IntPoint(4000, 0), new IntPoint(4000, 4000)};
    assertEquals(
        3, board(AngleRestriction.FORTYFIVE_DEGREE, corners).getOutline().edgePieces().length);
    RoutingBoard boxes = board(AngleRestriction.NINETY_DEGREE, corners);
    assertTrue(boxes.getOutline().edgePieces().length > 3);
    // 280 mil inside the 45-degree edge: outside its strip, inside the strip's whole bounding box.
    assertFalse(outlineCovers(boxes, new IntPoint(2500, 2100)));
  }
}
