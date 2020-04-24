import gov.nasa.jpf.annotation.FilterField;

import java.util.Random;

/**
 * Code by @author Wonsun Ahn
 * 
 * <p>
 * Bean: Each bean is assigned a skill level from 0-9 on creation according to a
 * normal distribution with average SKILL_AVERAGE and standard deviation
 * SKILL_STDEV. The formula to calculate the skill level is:
 * 
 * <p>
 * SKILL_AVERAGE = (double) SLOT_COUNT * 0.5 SKILL_STDEV = (double)
 * Math.sqrt(SLOT_COUNT * 0.5 * (1 - 0.5)) SKILL_LEVEL = (int)
 * Math.round(rand.nextGaussian() * SKILL_STDEV + SKILL_AVERAGE)
 * 
 * <p>
 * A skill level of 9 means it always makes the "right" choices (pun intended)
 * when the machine is operating in skill mode ("skill" passed on command line).
 * That means the bean will always go right when a peg is encountered, resulting
 * it falling into slot 9. A skill evel of 0 means that the bean will always go
 * left, resulting it falling into slot 0. For the in-between skill levels, the
 * bean will first go right then left. For example, for a skill level of 7, the
 * bean will go right 7 times then go left twice.
 * 
 * <p>
 * Skill levels are irrelevant when the machine operates in luck mode. In that
 * case, the bean will have a 50/50 chance of going right or left, regardless of
 * skill level. The formula to calculate the direction is: rand.nextInt(2). If
 * the return value is 0, the bean goes left. If the return value is 1, the bean
 * goes right.
 */

public class BeanImpl implements Bean {
	private int initSkill;
	private int currSkill;
	private Random rng;
	private boolean isLuck;

	/**
	 * Constructor - creates a bean in either luck mode or skill mode.
	 * 
	 * @param slotCount the number of slots in the machine
	 * @param isLuck    whether the bean is in luck mode
	 * @param rand      the random number generator
	 */
	BeanImpl(int slotCount, boolean isLuck, Random rand) {
		this.isLuck = isLuck;
		rng = rand;
		if (!isLuck) {
			double skillAvg = (double) slotCount * 0.5;
			double skillStDev = Math.sqrt((double) slotCount * 0.5 * (1.0 - 0.5));
			initSkill = (int) Math.round(rand.nextGaussian() * skillStDev + skillAvg);
		} else {
			initSkill = -1;
		}
		currSkill = initSkill;
	}

	// Returns false if the choice is left, true if right.
	public boolean getChoice() {
		if (isLuck) {
			int choice = rng.nextInt(2);
			if (choice == 0) {
				return false;
			}
			return true;
		}
		if (currSkill > 0) {
			currSkill--;
			return true;
		}
		return false;
	}

	public void restart() {
		currSkill = initSkill;
	}
}