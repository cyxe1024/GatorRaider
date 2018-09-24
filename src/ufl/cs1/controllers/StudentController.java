package ufl.cs1.controllers;
import game.controllers.DefenderController;
import game.models.*;

import java.util.List;
import java.util.ArrayList;

public final class StudentController implements DefenderController {
	public void init(Game game) {

	}
	public void shutdown(Game game) {

	}
	/*
	 *  Entire Process of the Update Method
	 *  1) Initialized data types
	 *     a. int [] actions: keep track of the direction ghosts want to go and returns these directions
	 *     b. boolean [] isAssigned: keeps track of which ghosts have already been assigned a direction
	 *        true is used to indicate any ghost that has already been assigned
	 *     c. int [] distance: is the distance of each ghost from the Pacman
	 *        -1 means the distance is unknown, this is for when the ghost is still in its jail
	 *
	 *  2) How we determine which ghost(s) are assigned which behaviour using precedence
	 *     a. flee: any ghosts that are vulnerable will be assigned this, this means to go away from pacman
	 *     b. chase: the closest ghost will be assigned to chase, which means to go directly towards pacman
	 *     c. pillTraveler: the next closet ghost will be the pillTraveler, this means he will guard the powerPill
	 *           in hopes of getting there before pacman and blocking him
	 *     d. flank: the rest of the ghosts will be assigned this, also means to go straight to pacman, however
	 *           it will check "be aware" of other ghosts and never take the same path to pacman, in hopes of flanking
	 */
	public int[] update(Game game, long timeDue) {
		int[] actions = new int[Game.NUM_DEFENDER];
		boolean[] isAssigned = new boolean[4];
		int[] distance = new int[4];
		Maze newMaze = game.getCurMaze();
		List<Node> pills = newMaze.getPillNodes();
		List<Node> powerPills = new ArrayList(newMaze.getPowerPillNodes());
		if (!powerPills.isEmpty()) {
			for (int i = 0; i < powerPills.size(); i++) {
				if (!powerPills.get(i).isPowerPill()) {
					powerPills.remove(i);
				}
			}
/*
* Creates an array of ghosts
* Creates the Pacman
* Remember that array index starts from zero, so ghost one is at index 0
*/
			Defender[] ghosts = new Defender[4];
			for (int i = 0; i < 4; i++) {
				ghosts[i] = game.getDefender(i);
			}
			Attacker attacker = game.getAttacker();
/*
* for loop checks which ghosts are vulnerable
* if any are it changes the behaviour to -1
* then calls the flee method to determine the desired direction and assigns that to the appropriate index in actions
*/
			for (int i = 0; i < 4; i++) {
				if (ghosts[i].isVulnerable() == true) {
					isAssigned[i] = true;
					actions[i] = flee(attacker, ghosts[i]);
				}
			}
        /*
        * The following loop assigns the distance from each ghost to pacman
        * if distance is -1, that means the ghost is in jail
        */

			for (int i = 0; i < 4; i++) {
				if (isAssigned[i] == true) {
					distance[i] = -1;
				} else {
					distance[i] = distanceToAttacker(attacker, ghosts[i]);
				}
			}
// assigns closest ghost as chase
			int closest = 0;
			boolean empty = true;
			for (int i = 0; i < 4; i++) {
				if (isAssigned[i] == false) {
					if (empty && distance[i] != -1) {
						closest = i;
						empty = false;
					} else {
						if (distance[closest] > distance[i] && distance[i] != -1) {
							closest = i;
						}
					}
				}
			}
			isAssigned[closest] = true; //this ghost is now used
			actions[closest] = chase(attacker, ghosts[closest]);


// if there are no power pills left, they all rush to pacman to keep him from scoring more points
			int numPowerPills = newMaze.getNumberPowerPills();
			if (numPowerPills == 0) {
				actions[0] = chase(attacker, ghosts[0]);
				for (int i = 1; i < 4; i++) {
					actions[i] = flank(ghosts, attacker, ghosts[i]);
				}
				return actions;
			}

// checks if all ghosts are used and if they are return actions and skip the rest of the method
			if (areGhostsUsed(isAssigned) == true) {
				return actions;
			}
// assigns the pillTraveler
			int secondClosest = 0;
			empty = true;
			for (int i = 0; i < 4; i++) {
				if (isAssigned[i] == false) {
					if (empty && distance[i] != -1) {
						secondClosest = i;
						empty = false;
					} else {
						if (distance[secondClosest] > distance[i] && distance[i] != -1) {
							secondClosest = i;
						}
					}
				}
			}
			isAssigned[secondClosest] = true; //this ghost is now used
			actions[secondClosest] = pillTraveller(attacker, ghosts[secondClosest], game);
// checks if all ghosts are used and if they are return actions and skip the rest of the method
			if (areGhostsUsed(isAssigned) == true) {
				return actions;
			}
// assigns the first flank
			int thirdClosest = 0;
			empty = true;
			for (int i = 0; i < 4; i++) {
				if (isAssigned[i] == false) {
					if (empty && distance[i] != -1) {
						thirdClosest = i;
						empty = false;
					} else {
						if (distance[thirdClosest] > distance[i] && distance[i] != -1) {
							thirdClosest = i;
						}
					}
				}
			}
			Maze maze = game.getCurMaze();
			isAssigned[thirdClosest] = true; //this ghost is now used
			actions[thirdClosest] = flank(ghosts, attacker, ghosts[thirdClosest]);

// assigns the unassigned ghost as another flank if there is an unassigned ghost
			for (int i = 0; i < 4; i++) {
				if (isAssigned[i] == false) {
					actions[i] = flank(ghosts, attacker, ghosts[i]);
				}
			}
		}
		return actions;
	}

// creates chase behavior
	public int chase(Attacker attacker, Defender ghost){
		int direction = ghost.getNextDir(attacker.getLocation(), true);
		int reverse = ghost.getReverse();
		List<Integer> possible = ghost.getPossibleDirs();
		while (direction == reverse){
			direction = possible.remove(0);
		}
		return direction;
	}
// creates flee behavior
	public int flee(Attacker attacker, Defender ghost) {
		int direction = ghost.getNextDir(attacker.getLocation(), false);
		int reverse = ghost.getReverse();
		List<Integer> possible = ghost.getPossibleDirs();
		while (direction == reverse){
			direction = possible.remove(0);
		}
		return direction;
	}
// creates flank behavior
	public static int flank(Defender[] ghostArray, Attacker attacker, Defender ghost) {
		int direction = ghost.getNextDir(attacker.getLocation(), true);
		int reverse = ghost.getReverse();
		int tempDirection = direction;
		List<Integer> possible = ghost.getPossibleDirs();
		List<Node> path = ghost.getPathTo(attacker.getLocation());
		for (int ii = 0; ii < path.size(); ii++) {
			for (int jj = 0; jj < ghostArray.length; jj++) {
				if (ghostArray[jj] == path.get(ii)) {
					while (path.size() >= 1) {
						while (direction == tempDirection) {
							direction = possible.remove(0);
						}
					}
				}
			}
		}
		return direction;
	}

// creates pill traveller behavior
	public static int pillTraveller (Attacker attacker, Defender ghosts, Game game) {
		int direction = 0;
		Maze thisMaze = game.getCurMaze();
		List<Node> powerPills = new ArrayList(thisMaze.getPowerPillNodes());
		if (!powerPills.isEmpty()) {
			int[] distances = new int[powerPills.size()];
			int distance = 0;
			for (int i = 0; i < powerPills.size(); i++) {
				distance = ghosts.getLocation().getPathDistance(powerPills.get(i));
				distances[i] = distance;
			}
			distance = distances[0];
			int here = 0;
			for (int i = 1; i < powerPills.size(); i++) {
				if (distances[i] > distance) {
					distance = distances[i];
					here = i;
				}
			}
			direction = ghosts.getNextDir(powerPills.get(here), true);
		} else {
			direction = ghosts.getNextDir(attacker.getLocation(), true);
		}
		return direction;
	}

// determines distance from ghost to attacker, determined by grabbing the location of the desired ghost
	// and using the .getPathDistance(); in order to allocate its target to the position of its attacker
	public static int distanceToAttacker(Attacker attacker, Defender ghost){
		int distance = ghost.getLocation().getPathDistance(attacker.getLocation());
		return distance;
	}
// decides if a ghost has been assigned a behavior or not using a series of booleans that work to organize the ghosts
	// by numbers that correspond to their indexes, thus starting at 0
	public static boolean areGhostsUsed(boolean [] isAssigned){
		if (isAssigned[0] == true && isAssigned[1] == true && isAssigned[2] == true && isAssigned[3] == true)
			return true;
		else
			return false;
	}
}

