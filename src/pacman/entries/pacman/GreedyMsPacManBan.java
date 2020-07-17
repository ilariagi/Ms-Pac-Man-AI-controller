package pacman.entries.pacman;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pacman.controllers.Controller;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.GameView;

/*
 */
public class GreedyMsPacManBan extends Controller<MOVE>
{
	private static final int MIN_DISTANCE=20;	//if a ghost is this close, run away
	private static final int LAIR_DISTANCE=10;	//if a ghost is this close, run away
	private MOVE myMove=MOVE.NEUTRAL;
	private static final int GUARD_DISTANCE=10; // distance before getting eaten
	
	private GreedyMsPacManStrategyBan strategy;
	private int greedySafePill, greedyPill, greedySafeIndex, greedyIndex, safePillWithJunction, safeEscapeNode, safePowerPillWithJunction;
	private int banned;
	
	public GreedyMsPacManBan() {
		this.strategy = new GreedyMsPacManStrategyBan();
	}
	
	public MOVE getMove(Game game, long timeDue) {
		// COLOR LEGEND:
		// - light gray, path to the "greedily safe" closest pill in heuristic0
		// - orange, path to the trap power pill in heuristic1
		// - cyan, path to the "greedily safe" closest pill in heuristic1 when chased
		// - green, path to the ghost to eat
		// - red, path to the "greedily safe" closest pill in heuristic1 when running away
		// - yellow, path to the closest safe junction in heuristic1 when running away
		// - magenta, path to the emergency index in heuristic1 when running away
		long startTime = java.lang.System.currentTimeMillis();
		
		
		GameView.addPoints(game, Color.gray, banned);
		if(game.wasPacManEaten()) 
			System.out.println("++++++++++++++++ SONO CREPATO ++++++++++++++++");
		int posMsPacman=game.getPacmanCurrentNodeIndex();
//		System.out.println("NODO CORRENTE "+posMsPacman);
		
		myMove = MOVE.NEUTRAL;
		
		MOVE[] moves = null;
		moves = game.getPossibleMoves(posMsPacman);
		
		// given the current position find greedily the best pill, that is a pill maximally far from the closest ghost near to it
		// and maximally near to pacman. Caution: this pill may not exist 
		greedySafePill = strategy.getGreedySafeTarget(game, posMsPacman, true, strategy.getAllTargets(game, true));
		// if a safe greedy pill was not found pick another pill that is maximally far from the closest ghost near to it
		// and maximally near to pacman without the constraint that MsPacMan reaches it before the ghosts. This MUST exist
		greedyPill = strategy.getGreedySafeTarget(game, posMsPacman, false, strategy.getAllTargets(game, true));
		
		safePillWithJunction = strategy.getSafePillWithJunction(game, posMsPacman, strategy.getPillTargets(game, true));
		safeEscapeNode = strategy.getSafePillWithJunction(game, posMsPacman, strategy.getAllTargets(game, false));
		
		int utility0[] = new int[2];
		int utility1[] = new int[2];
		
		utility1 = heuristic1(game, moves);
		// OTTIMIZZAZIONE *****************
		if(utility1[0] < 50)
			utility0 = heuristic0(game, moves);	
		else
			utility0[0] = 0;
		// ********************************
		if(utility0[0] > utility1[0]) {
			System.out.println("heur0 won " + utility0[0] + " move: " + moves[utility0[1]]);
			myMove = moves[utility0[1]];
		}
		else {
			System.out.println("heur1 won " + utility1[0] + " move: " + moves[utility1[1]]);
			myMove = moves[utility1[1]];
		}

		System.out.println("Time: " + (java.lang.System.currentTimeMillis() - startTime));
		return myMove;
	}
		
	// Heuristic 0 -> safe path
	// If all ghosts are "far enough" and there are pills to eat, this heuristic gains a high value
	// Which pill to eat must be chosen ensuring that Ms Pacman is not closed in a path from ghosts, 
	// this means that there is always a junction reachable from the pill that Ms Pacman can reach 
	// before any ghost in their shortest path. To foresee the path of the ghosts, the last move
	// made by ghost must be taken into account and an A* is used to calculate all the paths to the
	// junctions and then estimate a safe one. From the junction, Ms Pacman must be able to reach a 
	// power pill with her ShortestPath before any ghost
	private int[] heuristic0(Game game, MOVE[] moves) {
		// WELL NOTE: at the moment all computation is carried on using the current position and not the new position given the move.
		// Is it useful to consider the next position supposing that the ghosts are still? (because at any point we use their current
		// position)
		int current = game.getPacmanCurrentNodeIndex();
		int m = 0, bestMove = -1;
		int[] returnValues = new int[2];
		Map<Integer, Integer> movesScore = new HashMap<Integer, Integer>();

		// Prunare heur0 se sono abb lontani
//		GHOST closestGhost = strategy.getCloserGhost(game, current);
//		int closestGhostDistance = -1;
//		if(closestGhost != null) {
//			closestGhostDistance = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(closestGhost), current, 
//					game.getGhostLastMoveMade(closestGhost));
//		}
//		
		for(MOVE move: moves) {
			ArrayList<Integer> score = new ArrayList<Integer>(moves.length);
			
			// When MsPacman is too close to the lair and ghosts are in it just go away, they could come out suddenly and kill you
			// CAUTION: MS PACMAN OFTEN GETS STUCKED IN THE SAME PLACE FOLLOWING THIS RULE
//			if(strategy.isThereGhostInLair(game) && 
//					game.getShortestPathDistance(current, game.getGhostInitialNodeIndex()) < LAIR_DISTANCE &&
//					move == game.getNextMoveAwayFromTarget(current, game.getGhostInitialNodeIndex(), DM.PATH)) {
//				score.add(75);
//			}
			
			if(safePillWithJunction != -1 &&
					move == game.getNextMoveTowardsTarget(current, safePillWithJunction, DM.PATH)) {
				score.add(50);
//				GameView.addPoints(game,Color.white, game.getShortestPath(current, safePillWithJunction));
			}
			// Case 1: go to the best pill chosen greedily, always going further from ghosts (PILL + POWER PILL)
			else if(greedySafePill != -1 &&
					move == game.getNextMoveTowardsTarget(current, greedySafePill, DM.PATH)) {
				score.add(40);
//				GameView.addPoints(game,Color.lightGray, game.getShortestPath(current, greedySafePill));
			}
			// Escape to a node with 2 junctions available to reach
			if(safeEscapeNode != -1 && move == game.getNextMoveTowardsTarget(current, safeEscapeNode, DM.PATH)) {
				score.add(30);
			}
			else if(greedyPill != -1 && move == game.getNextMoveTowardsTarget(current, greedyPill, DM.PATH)) {
				score.add(20);
//				GameView.addPoints(game,Color.gray, game.getShortestPath(current, greedyPill));
			}
			if(score.isEmpty()) {
				score.add(0);
			}
			movesScore.put(m, score.get(score.indexOf(Collections.max(score))));
			m++;
		}
		
		int tmp = Integer.MIN_VALUE;
		for(Integer moveIndex: movesScore.keySet()) {
			if(movesScore.get(moveIndex) > tmp) {
				tmp = movesScore.get(moveIndex);
				bestMove = moveIndex;
			}
		}
		
		returnValues[0] = tmp;
		returnValues[1] = bestMove;
		return returnValues;
	}
		
	
	// Heuristics 1 -> run away from ghosts or try to eat them
	// The path must foresee also the worst case (i.e: the most dangerous move that each ghost can do) 
	// up to X steps in the future, where X can be the distance between Ms Pacman and the ghost
	// NB each ghost should be taken into account before deciding the move
	// Astar path based on the current direction of the ghost, in case of crossing, the worst case is considered 
	private int[] heuristic1(Game game, MOVE[] moves) {
		// WELL NOTE: at the moment all computation is carried on using the current position and not the new position given the move.
		// Is it useful to consider the next position supposing that the ghosts are still? (because at any point we use their current
		// position)
		int m = 0, bestMove = -1;
		int[] returnValues = new int[2];
		Map<Integer, Integer> movesScore = new HashMap<Integer, Integer>();
		
		int current = game.getPacmanCurrentNodeIndex();
		if (game.isJunction(current)) {
			banned = current;
		}
		
		// ghost targets to take care of
		GHOST closestGhost, edibleGhost;
		
		// check if MsPacMan is chased
		int chasers = strategy.isMsPacManChased(current, game);
//		
		// power pill to chase to trap the ghosts and eat them in sequence
		int trapPowerPill = -1;
		// a star safe pill search used for walk around with ghosts
//		int safePill = strategy.getSafeEscapeToPillWithJunction(game, current, strategy.getAllTargets(game, true), banned);
		int safePill = -1; int safeNodeEscape = -1;
		int farthestJunction = -1;
		int eatPill = -1;
//		int greedySafeIndex = strategy.getGreedySafeTarget(game, current, true, strategy.getAllTargets(game, false));
		
		for(MOVE move: moves) {
			trapPowerPill = strategy.trapTheGhosts(game, current, strategy.getPowePillTargets(game, true));
			safePill = strategy.getSafeEscapeToPillWithJunction(game, current, strategy.getAllTargets(game, true), banned);
			// OTTIMIZZAZIONE ********
			if(safePill == -1) {
				greedySafeIndex = strategy.getGreedySafeTarget(game, current, true, strategy.getAllTargets(game, false));
//				safeNodeEscape = strategy.getSafePillWithJunction(game, current, strategy.getAllTargets(game, false));
				farthestJunction = strategy.getSafeEscapeToClosestJunction(game, current, banned);
			}
			// ***********************
			
			ArrayList<Integer> score = new ArrayList<Integer>();
			// find the most "interesting" ghosts that are available
			closestGhost = strategy.getCloserGhost(game, current);
			edibleGhost = strategy.isThereEdibleGhost(game, current);
			
			// There is a safely edible ghost, go and catch it
			if(edibleGhost != null
					&& move == game.getNextMoveTowardsTarget(current, game.getGhostCurrentNodeIndex(edibleGhost), DM.PATH)) {
				score.add(200);
				GameView.addPoints(game, Color.green, game.getShortestPath(current, game.getGhostCurrentNodeIndex(edibleGhost)));
			}
			
//			System.out.println(farthestSafeIndex);
			// Escape from ghosts
//			System.out.println("Eat pill "+eatPill+" safePill "+safePill+" junct "+farthestJunction+" index "+optSafeIndex+" last index "+farthestSafeIndex);
			if(closestGhost != null && game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(closestGhost)) <= 2*MIN_DISTANCE) {
//				GameView.addPoints(game, Color.lightGray, game.getShortestPath(current, game.getGhostCurrentNodeIndex(closestGhost)));
				System.out.println("chasers: "+ chasers);
				if(chasers > 3) {
					// it's the Aggressive ghost team, then go for a walk and eat pills in the zone
					eatPill = strategy.eatPills(game, current, strategy.getAllTargets(game, true));
					System.out.println("eat pill: "+eatPill);
					if(eatPill != -1 
							&& move == game.getNextMoveTowardsTarget(current, eatPill, DM.PATH)
							) {
						GameView.addPoints(game,Color.magenta, eatPill);
						score.add(199);
					}
				}
				if(safePill != -1
//						&& strategy.checkSafeChase(safePill, current, game)
						&& move == game.getNextMoveTowardsTarget(current, safePill, DM.PATH)
						) {
//					GameView.addPoints(game,Color.red, safePill);
					score.add(198);
				}
				
//				if(greedySafePill != -1 
//						&& move == game.getNextMoveTowardsTarget(current, greedySafePill, DM.PATH)) {
//					score.add(197);
//					GameView.addPoints(game, Color.white, greedySafePill);
//				}
//				if(safePillWithJunction != -1 &&
//						move == game.getNextMoveTowardsTarget(current, safePillWithJunction, DM.PATH)) {
//					score.add(197);
//					GameView.addPoints(game, Color.white, safePillWithJunction);
//				}
				
				if(trapPowerPill != -1 
//						&& strategy.checkSafeChase(trapPowerPill, current, game) 
						&& move == game.getNextMoveTowardsTarget(current, trapPowerPill, DM.PATH)) {
					GameView.addPoints(game,Color.DARK_GRAY, game.getShortestPath(current, trapPowerPill));
					score.add(195);
				}
//				if(safeEscapeNode != -1
//						&& move == game.getNextMoveTowardsTarget(current, safeEscapeNode, DM.PATH)) {
//					GameView.addPoints(game,Color.orange, safeEscapeNode);
//					score.add(194);
//				}
				if(farthestJunction != -1 
//						&& strategy.checkSafeChase(farthestJunction, current, game) 
						&& move == game.getNextMoveTowardsTarget(current, farthestJunction, DM.PATH)) {
					GameView.addPoints(game, Color.orange, game.getShortestPath(game.getNeighbour(current, move), farthestJunction));
					score.add(191);
				}
//				if(safeNodeEscape != -1 
////						&& strategy.checkSafeChase(farthestSafeIndex, current, game)
//						&& move == game.getNextMoveTowardsTarget(current, safeNodeEscape, DM.PATH)) {
//					score.add(190);
//				}
				
				else if(greedySafeIndex != -1 
						&& move == game.getNextMoveTowardsTarget(current, greedySafeIndex, DM.PATH)) {
//					GameView.addPoints(game,Color.blue, game.getShortestPath(current, greedySafeIndex));
					score.add(182);
				}
			}
			else {
//				System.out.println("else grande");
				score.add(0);
			}
			
			if(score.isEmpty()) {
				score.add(0);
			}
			movesScore.put(m, score.get(score.indexOf(Collections.max(score))));
			m++;
		}
	
//		for(int k: movesScore.keySet()) {
//			System.out.print(k+"="+movesScore.get(k)+" ");
//		}
//		System.out.println();
		// compare the results of the moves
		int tmp = Integer.MIN_VALUE;
		for(Integer moveIndex: movesScore.keySet()) {
			if(movesScore.get(moveIndex) == tmp && moves[moveIndex] == game.getPacmanLastMoveMade()) {
				tmp = movesScore.get(moveIndex);
				bestMove = moveIndex;
			}
			if(movesScore.get(moveIndex) > tmp) {
				tmp = movesScore.get(moveIndex);
				bestMove = moveIndex;
			}
		}
		
		returnValues[0] = tmp;
		returnValues[1] = bestMove;
		return returnValues;
	}
	
}