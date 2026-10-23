package app.freerouting.datastructures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.freerouting.datastructures.QueryRegionRecorder.Region;
import app.freerouting.geometry.planar.IntBox;
import org.junit.jupiter.api.Test;

class QueryRegionRecorderTest {

  @Test
  void queriesOutsideAFrameAreNotRecorded() {
    QueryRegionRecorder.recordQuery(new IntBox(0, 0, 10, 10).toIntOctagon(), 1L);
    QueryRegionRecorder recorder = QueryRegionRecorder.forThread();
    recorder.begin();
    assertTrue(recorder.end().isEmpty());
  }

  @Test
  void nestedFramesAddTheirRegionToTheEnclosingFrame() {
    QueryRegionRecorder recorder = QueryRegionRecorder.forThread();
    recorder.begin();
    QueryRegionRecorder.recordQuery(new IntBox(0, 0, 10, 10).toIntOctagon(), 1L);
    recorder.begin();
    QueryRegionRecorder.recordQuery(new IntBox(50, -5, 60, 5), 4L);
    Region inner = recorder.end();
    recorder.add(new Region(-20, 0, -10, 3, 2L));
    Region outer = recorder.end();

    assertEquals(new Region(50, -5, 60, 5, 4L), inner);
    assertEquals(new Region(-20, -5, 60, 10, 7L), outer);
  }

  @Test
  void aBoardWideReadCoversEverything() {
    QueryRegionRecorder recorder = QueryRegionRecorder.forThread();
    recorder.begin();
    QueryRegionRecorder.recordEverything();
    assertEquals(Region.EVERYTHING, recorder.end());
  }
}
