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
 * <p>Uses the Java Path Finder model checking tool to check BeanCounterLogic in
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
		
		slotCount=Verify.getInt(1, 5);	//Set slotcounter to values 1-5
		beanCount=Verify.getInt(0,3);	//Set beanCounter to values 0-3
		isLuck=Verify.getBoolean();		//Set is luck as either true or false.
		
		// Create the internal logic
		logic = BeanCounterLogic.createInstance(slotCount);
		// Create the beans
		beans = new Bean[beanCount];
		for (int i = 0; i < beanCount; i++) {
			beans[i] = Bean.createInstance(slotCount, isLuck, new Random());
		}
		
		// A failstring useful to pass to assertions to get a more descriptive error.
		failString = "Failure in (slotCount=" + slotCount + ", beanCount=" + beanCount
				+ ", isLucky=" + isLuck + "):";
	}

	@AfterClass
	public static void tearDown() {
	}

	/**
	 * Test case for void void reset(Bean[] beans).
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 * Invariants: If beanCount is greater than 0,
	 *             remaining bean count is beanCount - 1
	 *             in-flight bean count is 1 (the bean initially at the top)
	 *             in-slot bean count is 0.
	 *             If beanCount is 0,
	 *             remaining bean count is 0
	 *             in-flight bean count is 0
	 *             in-slot bean count is 0.
	 */
	@Test
	public void testReset() {
		System.out.println("testReset");
		
		logic.reset(beans);
		if(beanCount>0) {													//If beanCount is greater than 0,
			assertEquals(failString, logic.getRemainingBeanCount(), beanCount-1);		//remaining bean count is beanCount - 1
			assertEquals(failString, logic.getInFlightBeanXPos(0),0);					//in-flight bean is at top
			for(int i=0;i<slotCount;i++) {
				assertEquals(failString, logic.getSlotBeanCount(i),0);					//in-slot bean count is 0.
				if(i!=0)	assertEquals(failString, logic.getInFlightBeanXPos(i),-1);	//Verifies no in flight beans on any lower levels
			}
		}
		if(beanCount==0) {		//If beanCount is 0,
			assertEquals(failString, logic.getRemainingBeanCount(),0);		//remaining bean count is 0.
			for(int i=0;i<slotCount;i++) {
				assertEquals(failString, logic.getSlotBeanCount(i),0);		//in-slot bean count is 0.
				assertEquals(failString, logic.getInFlightBeanXPos(i),-1);	//Verifies no in flight beans
			}
		}
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After each advanceStep(),
	 *             all positions of in-flight beans are legal positions in the logical coordinate system.
	 */
	@Test
	public void testAdvanceStepCoordinates() {
		System.out.println("testAdvanceStepCoordinates "+slotCount+" "+beanCount+" "+isLuck);
		logic.reset(beans);
		boolean flag=true;	//Used to detect if machine is done.
		int high=0,low=slotCount-1;		//High is the y pos of the farthest possible bean, low is the y pos of the lowest possible bean. 
		while(flag) {
			flag=logic.advanceStep();
			for(int j=0;j<slotCount;j++) {
				int pos=logic.getInFlightBeanXPos(j);
				
				if(j<(slotCount-low-1)||j>high){	//Beans will no longer appear on lower levels. i will begin to decrease
					assertEquals(failString+" "+low+" "+high, pos, -1);				//Verifies no beans are on impossible levels.
				}
				else if(slotCount!=1)
					assertTrue(failString +" "+high+" "+low+" "+pos+" "+j, pos > -1 && pos <=j);	//Verify each level that can have an in-flight bean is within the correct bounds.	

			}
			
			//Increment high or decrease low.
			if(logic.getRemainingBeanCount()==0)		low--;	
			else if(high<slotCount-1)					high++;
		}
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After each advanceStep(),
	 *             the sum of remaining, in-flight, and in-slot beans is equal to beanCount.
	 */
	@Test
	public void testAdvanceStepBeanCount() {
		System.out.println("testAdvanceStepBeanCount");
		logic.reset(beans);
		boolean flag=true;
		while(flag) {
			flag=logic.advanceStep();
			
			int count=0;	//Total beans in flight and in slot
			for(int i=0;i<slotCount;i++) {
				if(logic.getInFlightBeanXPos(i)!=-1)	count++;
				count+=logic.getSlotBeanCount(i);
			}
			assertEquals(failString, count+logic.getRemainingBeanCount(), beanCount);	//Assert the sum is equal to the total possible beans
		}
	}

	/**
	 * Test case for boolean advanceStep().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: After the machine terminates,
	 *             remaining bean count is 0
	 *             in-flight bean count is 0
	 *             in-slot bean count is beanCount.
	 */
	@Test
	public void testAdvanceStepPostCondition() {
		System.out.println("testAdvanceStepPostCondition");
		logic.reset(beans);
		
		//Run machine until termination
		boolean flag=true;
		while(flag) {
			flag=logic.advanceStep();
		}
		
		int count=0;	//Counter variable for total beans to help with assertions.
		assertEquals(failString, logic.getRemainingBeanCount(),0);	//No remaining beans
		for(int i=0;i<slotCount;i++)	assertEquals(failString, logic.getInFlightBeanXPos(i), -1);	//No beans in flight
		for(int i=0;i<slotCount;i++)	count+=logic.getSlotBeanCount(i);
		assertEquals(failString, count,beanCount);
	}
	
	/**
	 * Test case for void lowerHalf()().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.lowerHalf().
	 * Invariants: After calling logic.lowerHalf(),
	 *             slots in the machine contain only the lower half of the original beans.
	 *             Remember, if there were an odd number of beans, (N+1)/2 beans should remain.
	 *             Check each slot for the expected number of beans after having called logic.lowerHalf().
	 */
	@Test
	public void testLowerHalf() {
		System.out.println("testLowerHalf");
		logic.reset(beans);

		//Run machine until termination
		boolean flag=true;
		while(flag) {
			flag=logic.advanceStep();
		}

		int[] slots=new int[slotCount];
		for(int i=0;i<slotCount;i++)	slots[i]=logic.getSlotBeanCount(i);		//Record normal slot count for testing
		logic.lowerHalf();

		int count=(beanCount%2==1) ? (beanCount+1)/2 : beanCount/2;
		int remain=0;
		for(int i=0;i<slotCount;i++) {
			count-=logic.getSlotBeanCount(i);
			if(count==0)
				remain=i;
		}
		assertEquals(failString, count, 0);																//First verifies correct amount of beans remain.
		assertTrue(failString, slots[remain]>=logic.getSlotBeanCount(remain));							//Verifies final cell with beans has decreased at least.
		for(int i=remain+1;i<slotCount;i++)		assertEquals(failString, logic.getSlotBeanCount(i),0);	//Makes sure no beans remain in the upper half.
		
		
	}
	
	/**
	 * Test case for void upperHalf().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.lowerHalf().
	 * Invariants: After calling logic.upperHalf(),
	 *             slots in the machine contain only the upper half of the original beans.
	 *             Remember, if there were an odd number of beans, (N+1)/2 beans should remain.
	 *             Check each slot for the expected number of beans after having called logic.upperHalf().
	 */
	@Test
	public void testUpperHalf() {
		System.out.println("testUpperHalf");
		logic.reset(beans);
		
		//Run machine until termination
		boolean flag=true;
		while(flag) {
			flag=logic.advanceStep();
		}
		
		int[] slots=new int[slotCount];
		for(int i=0;i<slotCount;i++)	slots[i]=logic.getSlotBeanCount(i);		//Record normal slot count for testing
		logic.lowerHalf();
		
		int count=(beanCount%2==1) ? (beanCount+1)/2 : beanCount/2;
		int remain=0;
		for(int i=slotCount-1;i>=0;i--) {
			count-=logic.getSlotBeanCount(i);
			if(count==0)
				remain=i;
		}
		assertEquals(failString, count, 0);																//First verifies correct amount of beans remain.
		assertTrue(failString, slots[remain]>=logic.getSlotBeanCount(remain));							//Verifies final cell with beans has decreased at least.
		for(int i=remain-1;i>=0;i++)		assertEquals(failString, logic.getSlotBeanCount(i),0);		//Makes sure no beans remain in the lower half.
		
	}
	
	/**
	 * Test case for void repeat().
	 * Preconditions: None.
	 * Execution steps: Call logic.reset(beans).
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 *                  Call logic.repeat();
	 *                  Call logic.advanceStep() in a loop until it returns false (the machine terminates).
	 * Invariants: If the machine is operating in skill mode,
	 *             bean count in each slot is identical after the first run and second run of the machine. 
	 */
	@Test
	public void testRepeat() {
		System.out.println("testRepeat");
		logic.reset(beans);
		
		//Run machine until termination
		boolean flag=true;
		while(flag) {
			flag=logic.advanceStep();
		}
		
		int[] slots=new int[slotCount];
		for(int i=0;i<slotCount;i++)	slots[i]=logic.getSlotBeanCount(i);		//Record normal slot count for testing
		logic.repeat();
		
		//Run machine until termination
		flag=true;
		while(flag) {
			flag=logic.advanceStep();
		}
		if(!isLuck) {
			for(int i=0;i<slotCount;i++)	assertEquals(failString, slots[i],logic.getSlotBeanCount(i));
		}
	}
}
