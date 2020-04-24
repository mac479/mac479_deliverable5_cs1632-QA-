import gov.nasa.jpf.vm.Verify;

import java.util.Formatter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Code by @author Wonsun Ahn
 * 
 * <p>
 * BeanCounterLogic: The bean counter, also known as a quincunx or the Galton
 * box, is a device for statistics experiments named after English scientist Sir
 * Francis Galton. It consists of an upright board with evenly spaced nails (or
 * pegs) in a triangular form. Each bean takes a random path and falls into a
 * slot.
 *
 * <p>
 * Beans are dropped from the opening of the board. Every time a bean hits a
 * nail, it has a 50% chance of falling to the left or to the right. The piles
 * of beans are accumulated in the slots at the bottom of the board.
 * 
 * <p>
 * This class implements the core logic of the machine. The MainPanel uses the
 * state inside BeanCounterLogic to display on the screen.
 * 
 * <p>
 * Note that BeanCounterLogic uses a logical coordinate system to store the
 * positions of in-flight beans.For example, for a 4-slot machine: (0, 0) (0, 1)
 * (1, 1) (0, 2) (1, 2) (2, 2) (0, 3) (1, 3) (2, 3) (3, 3) [Slot0] [Slot1]
 * [Slot2] [Slot3]
 */

public class BeanCounterLogicImpl implements BeanCounterLogic {
	private int totalBeans;
	private int slotCount;
	private int[] slots;
	private Queue<BeanImpl> remainingBeans; // Queue of remaining beans
	private LinkedList<BeanImpl> inSlot;
	private LinkedList<BeanImpl> inFlight;
	private int[] posMap; // Map of beans x coords at posMap[Y]
	private boolean noBean;

	private int stepPadding = 0; // padding to help step through the last few iterations

	/**
	 * Constructor - creates the bean counter logic object that implements the core
	 * logic with the provided number of slots.
	 * 
	 * @param slotCount the number of slots in the machine
	 */
	BeanCounterLogicImpl(int slotCount) {
		this.slotCount = slotCount;
	}

	/**
	 * Returns the number of slots the machine was initialized with.
	 * 
	 * @return number of slots
	 */
	public int getSlotCount() {
		return slotCount;
	}

	/**
	 * Returns the number of beans remaining that are waiting to get inserted.
	 * 
	 * @return number of beans remaining
	 */
	public int getRemainingBeanCount() {
		return remainingBeans.size();
	}

	/**
	 * Returns the x-coordinate for the in-flight bean at the provided y-coordinate.
	 * 
	 * @param yPos the y-coordinate in which to look for the in-flight bean
	 * @return the x-coordinate of the in-flight bean; if no bean in y-coordinate,
	 *         return NO_BEAN_IN_YPOS
	 */
	public int getInFlightBeanXPos(int yPos) {
		return posMap[yPos];
	}

	/**
	 * Returns the number of beans in the ith slot.
	 * 
	 * @param i index of slot
	 * @return number of beans in slot
	 */
	public int getSlotBeanCount(int i) {
		return slots[i];
	}

	/**
	 * Calculates the average slot number of all the beans in slots.
	 * 
	 * @return Average slot number of all the beans in slots.
	 */
	public double getAverageSlotBeanCount() {
		int total = 0;
		for (int i = 0; i < slotCount; i++)
			total += slots[i];
		return ((double) total / (double) slotCount);
	}

	/**
	 * Removes the lower half of all beans currently in slots, keeping only the
	 * upper half. If there are an odd number of beans, remove (N-1)/2 beans, where
	 * N is the number of beans. So, if there are 3 beans, 1 will be removed and 2
	 * will be remaining.
	 */
	public void upperHalf() {
		int target = (totalBeans % 2 == 1) ? (totalBeans - 1) / 2 : totalBeans / 2;
		totalBeans = target;
		int slotIndex = 0;
		for (int i = 0; i < target; i++) {
			inSlot.remove();
			if (i == slots[slotIndex]) {
				target -= i;
				i = 0;
				slots[slotIndex] = 0;
				slotIndex++;
			}
		}
		slots[slotIndex] -= target;
	}

	/**
	 * Removes the upper half of all beans currently in slots, keeping only the
	 * lower half. If there are an odd number of beans, remove (N-1)/2 beans, where
	 * N is the number of beans. So, if there are 3 beans, 1 will be removed and 2
	 * will be remaining.
	 */
	public void lowerHalf() {
		int target = (totalBeans % 2 == 1) ? (totalBeans - 1) / 2 : totalBeans / 2;
		totalBeans = target;
		int slotIndex = slotCount - 1;
		for (int i = 0; i < target; i++) {
			inSlot.removeLast();
			if (i == slots[slotIndex]) {
				target -= i;
				i = 0;
				slots[slotIndex] = 0;
				slotIndex--;
			}
		}
		slots[slotIndex] -= target;
	}

	/**
	 * A hard reset. Initializes the machine with the passed beans. The machine
	 * starts with one bean at the top. Note: the Bean interface does not have any
	 * methods except the constructor, so you will need to downcast the passed Bean
	 * objects to BeanImpl objects to be able to work with them. This is always safe
	 * by construction (always, BeanImpl objects are created with
	 * BeanCounterLogicImpl objects and BeanBuggy objects are created with
	 * BeanCounterLogicBuggy objects according to the Config class).
	 * 
	 * @param beans array of beans to add to the machine
	 */
	public void reset(Bean[] beans) {
		stepPadding=0;
		remainingBeans = new LinkedList<BeanImpl>();
		slots = new int[slotCount];
		posMap = new int[slotCount];
		for (int i = 0; i < posMap.length; i++)
			posMap[i] = NO_BEAN_IN_YPOS; // Clears position map.
		inFlight = new LinkedList<BeanImpl>();
		inFlight.clear();
		inSlot = new LinkedList<BeanImpl>();
		totalBeans = beans.length;

		for (int i = 0; i < totalBeans; i++) {
			if (beans[i] != null)
				remainingBeans.add((BeanImpl) beans[i]);
		}
		if (totalBeans != 0) {
			noBean=false;
			posMap[0] = 0;
			inFlight.offerFirst(remainingBeans.remove()); // Pops front of queue to top of map.
		}
		else {
			posMap[0]=NO_BEAN_IN_YPOS;
			noBean=true;
		}
		// System.err.println("--------------------------------------------
		// "+slotCount+" "+inFlight.size()+" "+posMap[slotCount-1]!=NO_BEAN_IN_YPOS+"
		// "+posMap.length);

	}

	/**
	 * Repeats the experiment by scooping up all beans in the slots and all beans
	 * in-flight and adding them into the pool of remaining beans. As in the
	 * beginning, the machine starts with one bean at the top.
	 */
	public void repeat() {
		stepPadding=0;
		slots = new int[slotCount];
		posMap = new int[slotCount];
		for (int i = 0; i < posMap.length; i++)
			posMap[i] = NO_BEAN_IN_YPOS; // Clears position map.
		inFlight = new LinkedList<BeanImpl>();

		while (!inSlot.isEmpty()) {
			inSlot.peek().restart();
			remainingBeans.add(inSlot.poll());
		}
		while (!inFlight.isEmpty()) {
			inFlight.peek().restart();
			remainingBeans.add(inFlight.poll());
		}

		if (totalBeans != 0) {
			noBean=false;
			posMap[0] = 0;
			inFlight.offerFirst(remainingBeans.remove()); // Pops front of queue to top of map.
		}
		else {
			posMap[0]=NO_BEAN_IN_YPOS;
			noBean=true;
		}
	}

	/**
	 * Advances the machine one step. All the in-flight beans fall down one step to
	 * the next peg. A new bean is inserted into the top of the machine if there are
	 * beans remaining.
	 * 
	 * @return whether there has been any status change. If there is no change, that
	 *         means the machine is finished.
	 */
	public boolean advanceStep() {// Detects if changes need to be made before acting.
		if ((remainingBeans.isEmpty() && inFlight.isEmpty())||noBean)
			return false;

		//Handle case of the map being 1 tall.
		if(posMap.length==1) {
			//Bean is already technically in slot just need to be recorded
			slots[0]++;
			inSlot.add(inFlight.pollLast());
			if(!remainingBeans.isEmpty()) {
				posMap[0]=0;
				inFlight.addFirst(remainingBeans.remove());
			}
			else {
				posMap[0]=NO_BEAN_IN_YPOS;
			}
			return true;
		}
		
		//Step beans down if possible
		for(int i=inFlight.size()-1;i>=0 && posMap.length!=1;i--) {
			if(inFlight.get(i).getChoice()) {
				posMap[i+stepPadding+1]=posMap[i+stepPadding]+1;
			}
			else {
				posMap[i+stepPadding+1]=posMap[i+stepPadding];
			}
		}
		
		//detects if a bean is in a slot, records it and removes it from the board
		if(posMap[slotCount-1]!=NO_BEAN_IN_YPOS) {
			slots[posMap[slotCount-1]]++;
			int count=0;
			for(int i=0;i<posMap[slotCount-1];i++) {
				count+=slots[i];
			}
			inSlot.add(count, inFlight.pollLast());
			posMap[slotCount-1]=NO_BEAN_IN_YPOS;
		}
		
		if(!remainingBeans.isEmpty()) {
			posMap[0]=0;
			inFlight.addFirst(remainingBeans.remove());
		}
		else {
			//adjusts padding if there are no more beans being added to the map and clears space behind it.
			posMap[stepPadding]=NO_BEAN_IN_YPOS;
			stepPadding++;
		}
		
		return true;
		
		/*
		// First gets choices and sets next position starting from the bottom up.
		for (int i = inFlight.size() - 1; i >= stepPadding && posMap.length != 1; i--) {
			System.out.println(i+" STEP "+stepPadding+" "+posMap.length);
			if (inFlight.get(i).getChoice())
				posMap[i+ stepPadding+1] = posMap[i+ stepPadding] + 1;
			else
				posMap[i+ stepPadding+1] = posMap[i+ stepPadding];
			if (i == stepPadding && remainingBeans.isEmpty())
				posMap[i+ stepPadding+1] = NO_BEAN_IN_YPOS;
		}
		// Check if one bean fell into a slot, record its slot, and then remove it from
		// map.
		if (posMap[slotCount - 1] != NO_BEAN_IN_YPOS) {
			slots[posMap[slotCount - 1]]++; // Increase slot count recording bean.
			int count = 0;
			for (int i = 0; i < posMap[slotCount - 1]; i++)
				count += slots[i]; // Gets index to insert new bean at.
			inSlot.add(count, inFlight.removeLast()); // Removes bean from map and inserts it at the end index of the
														// slot it fell in.
			posMap[slotCount - 1] = NO_BEAN_IN_YPOS;

		}
		// Grab a new bean for the head
		if (!remainingBeans.isEmpty()) {
			inFlight.offerFirst(remainingBeans.remove());
			posMap[0]=0;
		}
		else
			stepPadding++;
*/
		//return true;
	}

	/**
	 * Number of spaces in between numbers when printing out the state of the
	 * machine. Make sure the number is odd (even numbers don't work as well).
	 */
	private int xspacing = 3;

	/**
	 * Calculates the number of spaces to indent for the given row of pegs.
	 * 
	 * @param yPos the y-position (or row number) of the pegs
	 * @return the number of spaces to indent
	 */
	private int getIndent(int yPos) {
		int rootIndent = (getSlotCount() - 1) * (xspacing + 1) / 2 + (xspacing + 1);
		return rootIndent - (xspacing + 1) / 2 * yPos;
	}

	/**
	 * Constructs a string representation of the bean count of all the slots.
	 * 
	 * @return a string with bean counts for each slot
	 */
	public String getSlotString() {
		StringBuilder bld = new StringBuilder();
		Formatter fmt = new Formatter(bld);
		String format = "%" + (xspacing + 1) + "d";
		for (int i = 0; i < getSlotCount(); i++) {
			fmt.format(format, getSlotBeanCount(i));
		}
		fmt.close();
		return bld.toString();
	}

	/**
	 * Constructs a string representation of the entire machine. If a peg has a bean
	 * above it, it is represented as a "1", otherwise it is represented as a "0".
	 * At the very bottom is attached the slots with the bean counts.
	 * 
	 * @return the string representation of the machine
	 */
	public String toString() {
		StringBuilder bld = new StringBuilder();
		Formatter fmt = new Formatter(bld);
		for (int yPos = 0; yPos < getSlotCount(); yPos++) {
			int xBeanPos = getInFlightBeanXPos(yPos);
			for (int xPos = 0; xPos <= yPos; xPos++) {
				int spacing = (xPos == 0) ? getIndent(yPos) : (xspacing + 1);
				String format = "%" + spacing + "d";
				if (xPos == xBeanPos) {
					fmt.format(format, 1);
				} else {
					fmt.format(format, 0);
				}
			}
			fmt.format("%n");
		}
		fmt.close();
		return bld.toString() + getSlotString();
	}

	/**
	 * Prints usage information.
	 */
	public static void showUsage() {
		System.out.println("Usage: java BeanCounterLogic slot_count bean_count <luck | skill> [debug]");
		System.out.println("Example: java BeanCounterLogic 10 400 luck");
		System.out.println("Example: java BeanCounterLogic 20 1000 skill debug");
	}

	/**
	 * Auxiliary main method. Runs the machine in text mode with no bells and
	 * whistles. It simply shows the slot bean count at the end.
	 * 
	 * @param args commandline arguments; see showUsage() for detailed information
	 */
	public static void main(String[] args) {
		boolean debug;
		boolean luck;
		int slotCount = 0;
		int beanCount = 0;

		if (args.length != 3 && args.length != 4) {
			showUsage();
			return;
		}

		try {
			slotCount = Integer.parseInt(args[0]);
			beanCount = Integer.parseInt(args[1]);
		} catch (NumberFormatException ne) {
			showUsage();
			return;
		}
		if (beanCount < 0) {
			showUsage();
			return;
		}

		if (args[2].equals("luck")) {
			luck = true;
		} else if (args[2].equals("skill")) {
			luck = false;
		} else {
			showUsage();
			return;
		}

		if (args.length == 4 && args[3].equals("debug")) {
			debug = true;
		} else {
			debug = false;
		}

		// Create the internal logic
		BeanCounterLogicImpl logic = new BeanCounterLogicImpl(slotCount);
		// Create the beans (in luck mode)
		BeanImpl[] beans = new BeanImpl[beanCount];
		for (int i = 0; i < beanCount; i++) {
			beans[i] = new BeanImpl(slotCount, luck, new Random());
		}
		// Initialize the logic with the beans
		logic.reset(beans);

		if (debug) {
			System.out.println(logic.toString());
		}

		// Perform the experiment
		while (true) {
			if (!logic.advanceStep()) {
				break;
			}
			if (debug) {
				System.out.println(logic.toString());
			}
		}
		// display experimental results
		System.out.println("Slot bean counts:");
		System.out.println(logic.getSlotString());
	}
}
