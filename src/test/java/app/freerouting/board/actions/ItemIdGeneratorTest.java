package app.freerouting.board.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/** The item ids only grow: a wrapped id would collide with a live item. */
class ItemIdGeneratorTest {

  @Test
  void idsGrowFromOne() {
    ItemIdGenerator generator = new ItemIdGenerator();
    assertEquals(0, generator.maxGeneratedId());
    assertEquals(1, generator.newId());
    assertEquals(2, generator.newId());
    assertEquals(2, generator.maxGeneratedId());
  }

  @Test
  void theLastIdIsUsedAndThenTheGeneratorFailsInsteadOfWrapping() throws Exception {
    ItemIdGenerator generator = new ItemIdGenerator();
    Field last = ItemIdGenerator.class.getDeclaredField("lastGeneratedId");
    last.setAccessible(true);
    last.setInt(generator, Integer.MAX_VALUE - 1);

    assertEquals(Integer.MAX_VALUE, generator.newId());
    assertThrows(IllegalStateException.class, generator::newId);
    assertEquals(Integer.MAX_VALUE, generator.maxGeneratedId());
  }
}
