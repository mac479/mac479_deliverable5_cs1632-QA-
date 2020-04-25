import static org.junit.Assert.*;

import gov.nasa.jpf.vm.Verify;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Random;

/**
 * Code by @author Wonsun Ahn
 * 
 * <p>
 * Uses the Java Path Finder model checking tool to check BeanCounterLogic in
 * various modes of operation. It checks BeanCounterLogic in both "luck" and
 * "skill" modes for various numbers of slots and beans. It also goes down all
 * the possible random path taken by the beans during operation.
 */

public class BeanCounterLogicTest {
	private static BeanCounterLogic logic; // The core logic of the program
	private static Bean[] beans; // The beans in the machine
	private static String failString; // A descriptive fail string for assertions

	private static int slotCount; // The number of slots in the machine we want to test
	private static int beanCount; // The number of beans in the machine we want to test
	private static boolean isLuck; // Whether the machine we want to test is in "luck" or "skill" mode

	/**
	 * Sets up the test fixture.
	 */
	@BeforeClass
	public static void setUp() {

		slotCount = Verify.getInt(1, 5); // Set slotcounter to values 1-5
		beanCount = Verify.getInt(0, 3); // Set beanCounter to values 0-3
		isLuck = Verify.getBoolean(); // Set is luck as either true or false.

		// Create the internal logic
		logic = BeanCounterLogic.createInstance(slotCount);
		// Create the beans
		beans = new Bean[beanCount];
		for (int i = 0; i < beanCount; i++) {
			beans[i] = Bean.createInstance(slotCount, isLuck, new Random());
		}

		// A failstring useful to pass to assertions to get a more descriptive error.
		failString = "Failure in (slotCount=" + slotCount + ", beanCount="
					+ beanCount + ", isLucky=" + isLuck + "):";
	}

	@AfterClass
	public static void tearDown() {
	}

	/**
	 * Test case for void void reset(Bean[] beans). Preconditions: None. Execution
	 * steps: Call logic.reset(beans). Invariants: If beanCount is greater than 0,
	 * remaining bean count is beanCount - 1 in-flight bean count is 1 (the bean
	 * initially at the top) in-slot bean count is 0. If beanCount is 0, remaining
	 * bean count is 0 in-flight bean count is 0 in-slot bean count is 0.
	 */
	@Test
	public void testReset() {

		logic.reset(beans);
		// If beanCount is greater than 0,
		if (beanCount > 0) {
			// remaining bean count is beanCount
			assertEquals(failString, logic.getRemainingBeanCount(), beanCount - 1);
			// in-flight bean is at top
			assertEquals(failString, logic.getInFlightBeanXPos(0), 0); 
			for (int i = 0; i < slotCount; i++) {
				 // in-slot bean count is 0.
				assertEquals(failString, logic.getSlotBeanCount(i), 0);
				if (i != 0) {
					// Verifies no in flight beans on any
					// lower levels
					assertEquals(failString, logic.getInFlightBeanXPos(i), -1);
				}
			}
		}
		if (beanCount == 0) { // If beanCount is 0,
			// remaining bean count is 0.
			assertEquals(failString, logic.getRemainingBeanCount(), 0);
			for (int i = 0; i < slotCount; i++) {
				// in-slot bean count is 0.
				assertEquals(failString, logic.getSlotBeanCount(i), 0);
				// Verifies no in flight beans
				assertEquals(failString, logic.getInFlightBeanXPos(i), -1);
			}
		}
	}

	/**
	 * Test case for boolean advanceStep(). Preconditions: None. Execution steps:
	 * Call logic.reset(beans). Call logic.advanceStep() in a loop until it returns
	 * false (the machine terminates). Invariants: After each advanceStep(), all
	 * positions of in-flight beans are legal positions in the logical coordinate
	 * system.
	 */
	@Test
	public void testAdvanceStepCoordinates() {
		logic.reset(beans);
		boolean flag = true; // Used to detect if machine is done.
		int high = 0;
		int low = 0;
		// High is the y pos of the farthest possible bean, low is the y pos of the
		// lowest possible bean.

		while (flag) {
			// Increment low or decrease high.
			if (logic.getRemainingBeanCount() == 0) {
				low++;
			}
			if (high < slotCount - 2) {
				high++;
			}

			flag = logic.advanceStep();
			for (int j = 0; j < slotCount; j++) {
				int pos = logic.getInFlightBeanXPos(j);

				// Checks if there are beans to drop and loop is looking at correct area of map.
				if (low <= j && j <= high && beanCount != 0) {
					// Verify each level that can have an in-flight bean
					// is within the correct bounds.
					assertTrue(failString, pos > -1 && pos <= j);
				} else {
					// Verifies no beans are on impossible levels.
					assertEquals(failString, -1, pos);
				}
			}
		}
	}

	/**
	 * Test case for boolean advanceStep(). Preconditions: None. Execution steps:
	 * Call logic.reset(beans). Call logic.advanceStep() in a loop until it returns
	 * false (the machine terminates). Invariants: After each advanceStep(), the sum
	 * of remaining, in-flight, and in-slot beans is equal to beanCount.
	 */
	@Test
	public void testAdvanceStepBeanCount() {
		logic.reset(beans);
		boolean flag = true;
		while (flag) {
			flag = logic.advanceStep();

			int count = 0; // Total beans in flight and in slot
			for (int i = 0; i < slotCount; i++) {
				if (logic.getInFlightBeanXPos(i) != -1) {
					count++;
				}
				count += logic.getSlotBeanCount(i);
			}
			// Assert the sum is equal to
			// the total possible beans
			assertEquals(failString, count + logic.getRemainingBeanCount(), beanCount);
		}
	}

	/**
	 * Test case for boolean advanceStep(). Preconditions: None. Execution steps:
	 * Call logic.reset(beans). Call logic.advanceStep() in a loop until it returns
	 * false (the machine terminates). Invariants: After the machine terminates,
	 * remaining bean count is 0 in-flight bean count is 0 in-slot bean count is
	 * beanCount.
	 */
	@Test
	public void testAdvanceStepPostCondition() {
		logic.reset(beans);

		// Run machine until termination
		boolean flag = true;
		while (flag) {
			flag = logic.advanceStep();
		}

		// Counter variable for total beans to help with assertions.
		int count = 0;
		// No remaining beans
		assertEquals(failString, logic.getRemainingBeanCount(), 0);
		for (int i = 0; i < slotCount; i++) {
			// No beans in flight
			assertEquals(failString, logic.getInFlightBeanXPos(i), -1);
		}
		for (int i = 0; i < slotCount; i++) {
			count += logic.getSlotBeanCount(i);
		}
		assertEquals(failString, count, beanCount);
	}

	/**
	 * Test case for void lowerHalf()(). Preconditions: None. Execution steps: Call
	 * logic.reset(beans). Call logic.advanceStep() in a loop until it returns false
	 * (the machine terminates). Call logic.lowerHalf(). Invariants: After calling
	 * logic.lowerHalf(), slots in the machine contain only the lower half of the
	 * original beans. Remember, if there were an odd number of beans, (N+1)/2 beans
	 * should remain. Check each slot for the expected number of beans after having
	 * called logic.lowerHalf().
	 */
	@Test
	public void testLowerHalf() {
		logic.reset(beans);

		// Run machine until termination
		boolean flag = true;
		while (flag) {
			flag = logic.advanceStep();
		}

		int[] slots = new int[slotCount];
		for (int i = 0; i < slotCount; i++) {
			// Record normal slot count for testing
			slots[i] = logic.getSlotBeanCount(i);
		}
		logic.lowerHalf();

		int count = (beanCount % 2 == 1) ? (beanCount + 1) / 2 : beanCount / 2;
		int remain = 0;
		for (int i = 0; i < slotCount; i++) {
			count -= logic.getSlotBeanCount(i);
			if (count == 0) {
				remain = i;
			}
		}
		// First verifies correct amount of beans remain.
		assertEquals(failString, count, 0);
		// Verifies final cell with beans has
		// decreased at least.
		assertTrue(failString, slots[remain] >= logic.getSlotBeanCount(remain));
		for (int i = remain + 1; i < slotCount; i++) {
			// Makes sure no beans remain in the upper half.
			assertEquals(failString, logic.getSlotBeanCount(i), 0);
		}

	}

	/**
	 * Test case for void upperHalf(). Preconditions: None. Execution steps: Call
	 * logic.reset(beans). Call logic.advanceStep() in a loop until it returns false
	 * (the machine terminates). Call logic.lowerHalf(). Invariants: After calling
	 * logic.upperHalf(), slots in the machine contain only the upper half of the
	 * original beans. Remember, if there were an odd number of beans, (N+1)/2 beans
	 * should remain. Check each slot for the expected number of beans after having
	 * called logic.upperHalf().
	 */
	@Test
	public void testUpperHalf() {
		logic.reset(beans);

		// Run machine until termination
		boolean flag = true;
		while (flag) {
			flag = logic.advanceStep();
		}

		int[] slots = new int[slotCount];
		for (int i = 0; i < slotCount; i++) {
			// Record normal slot count for testing
			slots[i] = logic.getSlotBeanCount(i);
		}
		logic.lowerHalf();

		int count = (beanCount % 2 == 1) ? (beanCount + 1) / 2 : beanCount / 2;
		int remain = 0;
		for (int i = slotCount - 1; i >= 0; i--) {
			count -= logic.getSlotBeanCount(i);
			if (count == 0) {
				remain = i;
			}
		}
		// First verifies correct amount of beans remain.
		assertEquals(failString, count, 0);
		// Verifies final cell with beans has
		// decreased at least.
		assertTrue(failString, slots[remain] >= logic.getSlotBeanCount(remain));
		for (int i = remain - 1; i >= 0; i++) {
			// Makes sure no beans remain in the lower half.
			assertEquals(failString, logic.getSlotBeanCount(i), 0);
		}

	}

	/**
	 * Test case for void repeat(). Preconditions: None. Execution steps: Call
	 * logic.reset(beans). Call logic.advanceStep() in a loop until it returns false
	 * (the machine terminates). Call logic.repeat(); Call logic.advanceStep() in a
	 * loop until it returns false (the machine terminates). Invariants: If the
	 * machine is operating in skill mode, bean count in each slot is identical
	 * after the first run and second run of the machine.
	 */
	@Test
	public void testRepeat() {
		// System.out.println("testRepeat");
		logic.reset(beans);

		// Run machine until termination
		boolean flag = true;
		while (flag) {
			// System.out.println(slotCount+" "+beanCount+" "+isLuck);
			flag = logic.advanceStep();
		}

		int[] slots = new int[slotCount];
		for (int i = 0; i < slotCount; i++) {
			slots[i] = logic.getSlotBeanCount(i); // Record normal slot count for testing
		}
		logic.repeat();

		// Run machine until termination
		flag = true;
		while (flag) {
			flag = logic.advanceStep();
		}
		if (!isLuck) {
			for (int i = 0; i < slotCount; i++) {
				assertEquals(failString, slots[i], logic.getSlotBeanCount(i));
			}
		}
	}
}