package vstore.framework.matching;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import vstore.framework.context.ContextDescription;
import vstore.framework.context.RuleContextDescription;
import vstore.framework.context.types.place.VPlaces;
import vstore.framework.context.types.place.VSinglePlace;
import vstore.framework.db.table_helper.RulesDBHelper;
import vstore.framework.file.VStoreFile;
import vstore.framework.logging.LogHandler;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.node.NodeType;
import vstore.framework.rule.DecisionLayer;
import vstore.framework.rule.VStoreRule;
import vstore.framework.utils.ContextUtils;

import static vstore.framework.matching.NodeSelectionResult.DONE_FALSE;
import static vstore.framework.matching.NodeSelectionResult.DONE_TRUE;
import static vstore.framework.matching.NodeSelectionResult.NEXT_LAYER;
import static vstore.framework.matching.NodeSelectionResult.NEXT_TEST;
import static vstore.framework.node.NodeType.CLOUD;
import static vstore.framework.node.NodeType.CLOUDLET;
import static vstore.framework.node.NodeType.CORENET;
import static vstore.framework.node.NodeType.DEVICE_ONLY;
import static vstore.framework.node.NodeType.GATEWAY;
import static vstore.framework.node.NodeType.PRIVATE_NODE;
import static vstore.framework.node.NodeType.UNKNOWN;

/**
 * This class handles the storage decision, e.g. matching a file to a storage node based on the
 * usage context, the file meta data and the user intention, as well as the list of
 * available storage nodes.
 */
public class Matching {
    /**
     * Different matching modes.
     *
     * RULES_NEXT_ON_NO_MATCH = Use next more general rule if no node found for current rule
     * RULES_THEN_FALLBACK = First tries to match using the rules. If no rule matched, will use the
     *                       experimental fallback algorithm (currently not usable).
     * RANDOM = A node is chosen completely randomly
     */
    public enum MatchingMode {
        RULES_NEXT_ON_NO_MATCH, RULES_THEN_FALL_BACK, RANDOM
    }
    /**
     * Will hold the information about all decided node(s) after the matching process.
     * If a decision layer resulted in no storage node, this will also be put into this list
     */
    private List<NodeInfo> mDecidedNodes;
    /**
     * Will hold the information only about the valid nodes after the matching process (no null entries).
     */
    private List<NodeInfo> mValidNodes;

    /**
     * Make a new storage decision for the given parameters. If no rule matches, a default matching
     * is done. A default matching is also done, if a rule has a specific storage node configured
     * but this one cannot be found in the Node Manager.
     * To get the node that resulted from matching, use {@link Matching#getDecidedNodes()}.
     *
     * All parameters must not be null, or a RuntimeException is thrown.
     *
     * @param f The file to make the decision for. Must not be null and should contain a context description.
     * @param mode The matching mode, one of the enum type {@link Matching.MatchingMode}.
     *
     * @throws RuntimeException in case some parameters are null
     */
    public Matching(VStoreFile f, MatchingMode mode) throws RuntimeException{
        if(f == null) 
        {
            throw new RuntimeException("File must not be null.");
        }

        mDecidedNodes = new ArrayList<>();
        mValidNodes = new ArrayList<>();

        switch(mode) 
        {
            case RULES_NEXT_ON_NO_MATCH:
                //Get a list of all rules that could be triggered by the MIME type of the file.
				List<VStoreRule> rules;
				try {
					rules = RulesDBHelper.getRulesMatchingFileType(f.getFileType());
				} catch (SQLException e) {
					e.printStackTrace();
					return;
				}

                //Do we have some rules that might be applicable for the MIME type?
                if (rules.size() > 0) {
                    //Yes. start filtering them by context

                    //Step 1: Filter out all rules that do not apply to the MIME type
                    Iterator<VStoreRule> it = rules.iterator();
                    while (it.hasNext())
                    {
                        VStoreRule r = it.next();
                        if(!keepRuleInSet(r, f)) { it.remove(); }
                    }

                    //Step 2: Now the rules that are remaining do all match the file's context.
                    //Only continue, if there are actually some rules remaining.
                    if (rules.size() > 0) 
                    {
                        //Step 2.1: Sort rules by detail score. Remember all rules in a list
                        //to choose the next more general one if no matching node is
                        //found and matching mode is RULES_NEXT_ON_NO_MATCH.
                        List<VStoreRule> ruleList = sortRulesByScore(rules);

                        VStoreRule usedRule = null;
                        //First is now the rule with the highest score
                        if (mode.equals(MatchingMode.RULES_NEXT_ON_NO_MATCH)) 
                        {
                            //Try to find storage nodes rule by rule, when one of the more
                            //detailed rules does not yield a storage node
                            for (VStoreRule r : ruleList) 
                            {
                                usedRule = r;
                                if(getNodesForRule(r, f, true)) {
                                    //Break rule evaluation if replication factor is met.
                                    //If not, find the remaining nodes with the next rules
                                    if(mValidNodes.size() >= usedRule.getReplicationFactor()) {
                                        break;
                                    }
                                }
                            }
                            //If mDecidedNodes is still empty after the loop, no rule had a matching node
                            //or the rule had DEVICE_ONLY as a decision layer target type

                        } 
                        else 
                        {
                            //Get highest rated rule without using the next rule on no result
                            usedRule = ruleList.get(0);
                            getNodesForRule(usedRule, f, false);
                        }
                        //Log that we used this rule
                        LogHandler.logMatchingAddRule(f, usedRule, mDecidedNodes);
                    }
                }
                break;

            case RANDOM:
                NodeManager manager = NodeManager.get();
                NodeInfo randomNode = manager.getRandomNode();
                mDecidedNodes.add(randomNode);
                break;

            default:
                //mDecidedNodes stays empty
                break;
        }
    }

    /**
     * @param r The rule for which to check if it should be eliminated from the matching rule set
     * @param f The file for which the matching is currently being done
     * @return True, if the rule should be deleted from the rule set used for matching.
     * False, if the rule should be kept in the set of rules used for matching.
     */
    private boolean keepRuleInSet(VStoreRule r, VStoreFile f) {
        //Conjunction of all conditions
        //  A single condition returns true: Rule should be kept
        //  A single condition returns false: Rule should be eliminated
        //  (evaluation of further conditions stops)

        //Step 1.1: Eliminate rule if minimum file size is configured but does not match
        return checkConditions_fileSize(r, f)
                //Step 1.2: Filter by sharing domain
                //If a file is shared as public, every rule that applies to the private domain is
                //removed and vice versa.
                && checkConditions_sharingDomain(r, f)
                //Step 1.3: Filter by day and timespan
                //Ignore any rules that do not have a matching weekday and time.
                //No days configured means day/time does not matter.
                && checkConditions_dayTime(r, f)
                //Step 1.4: Filter by location
                //If the file's location context is not contained in the radius configured for
                //the rule, we ignore the rule.
                && checkConditions_location(r, f)
                //Step 1.5: Filter by place types
                //If the place type of the most likely place of the file's context
                //is not contained in those of the rule, we ignore the rule.
                && checkConditions_places(r, f)
                //Step 1.6: Filter by network
                && checkConditions_network(r, f)
                //Step 1.7: Filter by activity
                //If a rule has activity context set but it does not match the file's activity,
                //we ignore the rule.
                && checkConditions_activity(r, f)
                //Step 1.8: Filter by noise
                && checkConditions_noise(r, f);

        //To add new context type conditions here:
        //Simply add a condition which returns true, if the rule should be kept in the rule
        //set, and false if it should be removed from the set.
    }

    private boolean checkConditions_fileSize(VStoreRule r, VStoreFile f) {
        return !((r.hasFileSizeConfigured()) && (f.getFileSize() < r.getMinFileSize()));
    }
    private boolean checkConditions_sharingDomain(VStoreRule r, VStoreFile f) {
        int fileSharingDomain = (f.isPrivate() ? 1 : 0);
        if (!ContextUtils.isIncludedInSharingDomain(fileSharingDomain, r.getSharingDomain()))
        {
            return false;
        }
        return true;
    }
    private boolean checkConditions_dayTime(VStoreRule r, VStoreFile f) {
        if (r.getWeekdays().size() > 0) {
            int day = ContextUtils.getDayOfWeek();
            if (!r.getWeekdays().contains(day)) {
                //Remove rule if the current day is not contained in the day list of the rule
                return false;
            } else if (r.hasTimeSet() && !ContextUtils.isNowBetween(r.getStartHour(),
                    r.getStartMinutes(), r.getEndHour(), r.getEndMinutes())) {
                //Remove rule if the current time is not in the timespan of the rule
                return false;
            }
        }
        return true;
    }
    private boolean checkConditions_location(VStoreRule r, VStoreFile f) {
        ContextDescription fileCtx = f.getContext();
        RuleContextDescription ruleCtx = r.getRuleContext();
        if (r.hasLocationContext() && f.getContext().hasLocationContext())
        {
            double distance = ContextUtils.distanceBetween(ruleCtx.getLocationContext(),
                    fileCtx.getLocationContext().getLatLng());
            if (Math.ceil(distance) > ruleCtx.getRadius())
            {
                //Distance is farther away than the radius allows. Thus, this rule does
                //not apply
                return false;
            }
        }
        return true;
    }
    private boolean checkConditions_places(VStoreRule r, VStoreFile f) {
        ContextDescription fileCtx = f.getContext();
        RuleContextDescription ruleCtx = r.getRuleContext();
        if (r.hasPlaceContext())
        {
            if (fileCtx.getMostLikelyPlace() == null || !ruleCtx.getPlaceTypes().contains(
                    fileCtx.getPlaces().getMostLikelyPlace().getPlaceType()))
            {
                //The type of the most likely place of the file context is not contained in
                //the most likely place types of the defined rule context
                return false;
            }
        }
        return true;
    }
    private boolean checkConditions_network(VStoreRule r, VStoreFile f) {
        //We assume that if the rule has a WiFi name set, this has next highest priority.
        //If the rule has not WiFi set, but mobile network, than it is checked if the
        //same type is given.
        //So if the file context's network conditions do not meet those of the rule,
        //we ignore the rule.
        if (r.hasNetworkContext() && f.getContext().hasNetworkContext() &&
                !f.getContext().getNetworkContext().matches(r.getRuleContext().getNetworkContext()))
        {
            return false;
        }
        return true;
    }
    private boolean checkConditions_activity(VStoreRule r, VStoreFile f) {
        if (r.hasActivityContext() && f.getContext().hasActivityContext()
                && !f.getContext().getActivityContext() .matches(r.getRuleContext().getActivityContext()))
        {
            return false;
        }
        return true;
    }
    private boolean checkConditions_noise(VStoreRule r, VStoreFile f) {
        //If both have noise context set: Match the dB value of the file context vs the
        //threshold of the rule. If the rule specifies "must be silent" but it's not, we
        //ignore the rule. If it specifies "must be loud" but it's not, we ignore the rule.
        if (r.hasNoiseContext() && f.getContext().hasNoiseContext()
                && !r.getRuleContext().getNoiseContext().matches(f.getContext().getNoiseContext()))
        {
            return false;
        }
        return true;
    }

    /**
     * Sorts the given list of rules based on the detail score.
     * @param rules The list of rules to be sorted.
     * @return A list which contains the given rules in order based on the detail score.
     */
    private List<VStoreRule> sortRulesByScore(List<VStoreRule> rules) {
        ArrayList<VStoreRule> sortedRules = new ArrayList<>();
        for (VStoreRule r : rules)
        {
            float score = r.getDetailScore();
            int pos = 0;
            //Add it sorted into the list (highest to lowest score)
            while(pos < sortedRules.size())
            {
                if(sortedRules.get(pos).getDetailScore() < score) break;
                ++pos;
            }
            sortedRules.add(pos, r);
        }
        return sortedRules;
    }


    /**
     * This method determines the node (or multiple storage nodes) for the given rule.
     * Will go through all decision layers and - depending on the rule configuration - will either
     * - continue until one layer yields a result or until the end is reached.
     * - or find a node for each decision layer and store the file on all of them.
     *
     * @param rule The rule to find a node for.
     * @param file The file to find a node for.
     * @param keepNodesAlreadyFound If set, the nodes already contained in the decidedNodes list are kept.
     *                        If set to false, the list is cleared before search.
     * @return True, if a match was found. False, if not.
     */
    private boolean getNodesForRule(VStoreRule rule, VStoreFile file, boolean keepNodesAlreadyFound) {
        if(!keepNodesAlreadyFound) { mDecidedNodes.clear(); }

        //If the rule has the "storeMultiple" flag, an entry for each decision layer is
        //put into the list mDecidedNodes.
        //If the rule does not have this flag set, only one entry will be contained in this list.

        NodeSelectionResult lastResult;
        for(DecisionLayer layer : rule.getDecisionLayers())
        {
            //Confirm that maximum replication factor is not reached yet.
            //If it is reached, return.
            if(mValidNodes.size() >= rule.getReplicationFactor()) {
                return true;
            }

            //Check if no node is configured (this means either DEVICE_ONLY or UNKNOWN as target type)
            lastResult = getNodes_NoNode(rule, layer);
            if(lastResult == DONE_TRUE) { return true; }
            if(lastResult == DONE_FALSE) { return false; }
            if(lastResult == NEXT_LAYER) { continue; }

            //Check if a specific node is configured for this decision layer
            lastResult = getNodes_specificNode(rule, layer);
            if(lastResult == DONE_TRUE) { return true; }
            if(lastResult == DONE_FALSE) { return false; }
            if(lastResult == NEXT_LAYER) { continue; }

            //Check if target type "NONE" and some constraints are configured for this decision layer
            lastResult = getNodes_noneAndConstraints(rule, layer, file);
            if(lastResult == DONE_TRUE) { return true; }
            if(lastResult == DONE_FALSE) { return false; }
            if(lastResult == NEXT_LAYER) { continue; }

            //All constraints 0 on which we could select a node
            //(--> Get random node of the configured type)
            lastResult = getNodes_randomOfType(rule, layer);
            if(lastResult == DONE_TRUE) { return true; }
            if(lastResult == DONE_FALSE) { return false; }
            if(lastResult == NEXT_LAYER) { continue; }

            //Radius constraints set (will also take bandwidth into account if necessary)
            lastResult = getNodes_radiusAndBandwidth(rule, layer, file);
            if(lastResult == DONE_TRUE) { return true; }
            if(lastResult == DONE_FALSE) { return false; }
            if(lastResult == NEXT_LAYER) { continue; }

            //Radius constraints not set or invalid. Thus, use bandwidth constraints.
            lastResult = getNodes_useBandwidthConstraints(rule, layer);
            if(lastResult == DONE_TRUE) { return true; }
            if(lastResult == DONE_FALSE) { return false; }
            if(lastResult == NEXT_LAYER) { continue; }

            //For further conditions, please add them here
        }

        //If we found one or more valid nodes, return true
        boolean valid = false;
        for(NodeInfo node : mDecidedNodes) {
            if(node != null) { valid = true; }
        }
        return valid;
    }

    private NodeSelectionResult getNodes_NoNode(VStoreRule rule, DecisionLayer layer) {
        //Check if no node is configured (this means either DEVICE_ONLY or UNKNOWN as target type)
        if(layer.targetType.equals(DEVICE_ONLY) || layer.targetType.equals(UNKNOWN))
        {
            if(rule.isStoreMultiple()) {
                //Store null value for this layer in the list of decided nodes, so that in the
                //logging service it is clear what the outcomes of the layers were.
                mDecidedNodes.add(null);
                return NEXT_LAYER;
            }
            //"Single node" mode: Clear list, just to be sure
            return DONE_FALSE;
        }
        return NEXT_TEST;
    }
    private NodeSelectionResult getNodes_specificNode(VStoreRule rule, DecisionLayer layer) {
        //Check if a specific node is configured for this decision layer
        if(layer.isSpecific && layer.specificNodeId != null && !layer.specificNodeId.equals(""))
        {
            //Get information of this specific node
            NodeInfo node = NodeManager.get().getNode(layer.specificNodeId);
            if(node != null) {
                //Check if rule is configured to store the file on multiple nodes,
                //or only on one node
                if(rule.isStoreMultiple()) {
                    mDecidedNodes.add(node);
                    mValidNodes.add(node);
                    return NEXT_LAYER;
                } else {
                    mDecidedNodes.add(node);
                    mValidNodes.add(node);
                    //Done in the "single node" mode since we found our target
                    return DONE_TRUE;
                }
            }
        }
        return NEXT_TEST;
    }
    private NodeSelectionResult getNodes_noneAndConstraints(VStoreRule rule, DecisionLayer layer, VStoreFile file) {
        //Check if target type "NONE" and some constraints are configured for this decision layer
        if(layer.targetType.equals(NodeType.NONE)
                && (layer.maxRadius > 0 || layer.minBwUp > 0 || layer.minBwDown > 0))
        {
            //Check if at least one value is valid (either bandwidth up and/or down)
            if(layer.minBwUp > 0 || layer.minBwDown > 0)
            {
                NodeInfo node
                        = NodeManager.get().getRandomNodeMatchingBandwidthAndRadius(
                        layer.minBwUp, layer.minBwDown,
                        layer.minRadius, layer.maxRadius,
                        file.getContext().getLocationContext().getLatLng());

                if(node != null) {
                    if(rule.isStoreMultiple()) {
                        mDecidedNodes.add(node);
                        mValidNodes.add(node);
                        return NEXT_LAYER;
                    } else {
                        mDecidedNodes.add(node);
                        mValidNodes.add(node);
                        //Done in the "single node" mode since we found our target
                        return DONE_TRUE;
                    }
                }
                else
                {
                    //Node info is not available.
                    if(rule.isStoreMultiple()) {
                        //Store null value for this layer in the list of decided nodes
                        mDecidedNodes.add(null);
                    }
                    //In this case: in both single and multiple mode:
                    return NEXT_LAYER;
                }
            }
        }
        return NEXT_TEST;
    }
    private NodeSelectionResult getNodes_randomOfType(VStoreRule rule, DecisionLayer layer) {
        //All constraints 0 on which we could select a node
        //--> Get random node of the configured type
        if(layer.minRadius == 0 && layer.maxRadius == 0 && layer.minBwUp == 0
                && layer.minBwDown == 0)
        {
            NodeInfo node
                    = NodeManager.get().getRandomNodeOfTypes(new NodeType[] { layer.targetType});

            if(node != null) {
                if(rule.isStoreMultiple()) {
                    mDecidedNodes.add(node);
                    mValidNodes.add(node);
                    return NEXT_LAYER;
                } else {
                    mDecidedNodes.add(node);
                    mValidNodes.add(node);
                    //Done in the "single node" mode since we found our target
                    return DONE_TRUE;
                }
            }
            else
            {
                if(rule.isStoreMultiple())
                {
                    mDecidedNodes.add(null);
                }
                return NEXT_LAYER;
            }
        }
        return NEXT_TEST;
    }
    private NodeSelectionResult getNodes_radiusAndBandwidth(VStoreRule rule, DecisionLayer layer, VStoreFile file) {
        //Radius constraints set (will also take bandwidth into account if necessary)
        if(layer.minRadius>=0 && layer.maxRadius>0)
        {
            NodeInfo node = NodeManager.get().getRandomNodeOfTypeMatchingBandwidth(
                    layer.targetType, layer.minBwUp, layer.minBwDown, layer.minRadius,
                    layer.maxRadius, file.getContext().getLocationContext().getLatLng());

            if(node != null)
            {
                if(rule.isStoreMultiple())
                {
                    mDecidedNodes.add(node);
                    mValidNodes.add(node);
                    return NEXT_LAYER;
                } else {
                    mDecidedNodes.clear();
                    mDecidedNodes.add(node);
                    mValidNodes.add(node);
                    //Done in the "single node" mode since we found our target
                    return DONE_TRUE;
                }
            } else {
                mDecidedNodes.add(null);
            }
        }
        return NEXT_TEST;
    }
    private NodeSelectionResult getNodes_useBandwidthConstraints(VStoreRule rule, DecisionLayer layer) {
        //Radius constraints not set or invalid. Thus, use bandwidth constraints.
        if(layer.minBwUp>0 || layer.minBwDown>0)
        {
            NodeInfo node = NodeManager.get().getRandomNodeOfTypeMatchingBandwidth(
                    layer.targetType, layer.minBwUp, layer.minBwDown, 0, 0, null);

            if(rule.isStoreMultiple())
            {
                mDecidedNodes.add(node);
                mValidNodes.add(node);
                return NEXT_LAYER;
            }
            else
            {
                mDecidedNodes.clear();
                mDecidedNodes.add(node);
                mValidNodes.add(node);
                //Done in the "single node" mode since we found our target
                return DONE_TRUE;
            }
        }
        return NEXT_TEST;
    }

    /**
     * (CURRENTLY NOT USED!!!!)
     * This method determines the node for the given file without rules.
     * This is just experimental and not finished. Currently not used.
     *
     * @param f The file to find a node for.
     * @return The {@link NodeInfo} of the decided node. Or null, if no decision was possible.
     */
    private static NodeInfo getNodeFallback( VStoreFile f) {
        if (f == null) 
        {
            //Return null if no file is given (should never happen).
            return null;
        }
        
        //Get the node manager and check if nodes are available
        NodeManager manager = NodeManager.get();
        if (manager.getNodeCount() <= 0) 
        {
            //No nodes available, thus we store the file only on the phone.
            return null;
        }
        ContextDescription fileCtx = f.getContext();
        if(f.isPrivate()) 
        {
            //File is private (since we are here in the code, no "Home" rule applies)
            //We will ignore cloudlet and gateway, because it does not make sense to save
            //personal files there.
            if(fileCtx.hasLocationContext()) 
            {
                //Get a nearest node following this hierarchy:
                //PRIVATE_NODE, CORENET, CLOUD
                return manager.getNodeFollowingHierarchy(
                        Arrays.asList(PRIVATE_NODE, CORENET, CLOUD),
                        NodeManager.Mode.NEAREST,
                        fileCtx.getLocationContext().getLatLng());
            }
            //No location context (should happen rarely).
            //Get a random node following this hierarchy:
            //PRIVATE_NODE, CORENET, CLOUD
            return manager.getNodeFollowingHierarchy(
                    Arrays.asList(PRIVATE_NODE, CORENET, CLOUD),
                    NodeManager.Mode.RANDOM,
                    null);
        } 
        else 
        {
            //Intention is sharing. Let's do some matching based on usage context.
            //If the activity is IN_VEHICLE, we do not store the file on a cloudlet nearby,
            //because the user might just drive by the places with a train/car.
            //Instead, we use nearest corenet or cloud, if location context is available and
            //random corenet/cloud if no location context is available.
            if(fileCtx.hasActivityContext()
                    )//TODO: && fileCtx.getActivityContext().getType() == DetectedActivity.IN_VEHICLE) 
            {
                if(fileCtx.hasLocationContext()) 
                {
                    return manager.getNodeFollowingHierarchy(
                            Arrays.asList(CORENET, CLOUD),
                            NodeManager.Mode.NEAREST,
                            fileCtx.getLocationContext().getLatLng());
                } 
                else 
                {
                    return manager.getNodeFollowingHierarchy(
                            Arrays.asList(CORENET, CLOUD),
                            NodeManager.Mode.RANDOM,
                            null);
                }
            }
            //Match based on nearby and most likely place the user is currently located at.
            VSinglePlace placeUse = null;
            if(fileCtx.hasPlacesContext()) {
                VPlaces places = fileCtx.getPlaces();
                //Iteratively try to get a list of the most likely places by decreasing the
                //threshold gradually
                List<VSinglePlace> filtered = new ArrayList<>();
                float threshold = 0.3f; //Start with 30% likelihood threshold
                float lowestAllowed = 0.05f; //5% minimum likelihood
                while(filtered.isEmpty() && threshold > lowestAllowed) {
                    filtered = places.filterPlaces(threshold);
                    //Reduce threshold by 2 percent for next round
                    threshold -= 0.02;
                }
                //Check if we found a place.
                if(filtered.size() > 0) {
                    placeUse = filtered.get(0);
                    if (filtered.size() != 1) {
                        if(fileCtx.hasLocationContext()) {
                            //Multiple places. We need to decide for one place.
                            //Choose the place to use by selecting the closest one (if location
                            //context is available).
                            //Calculate the distance for the first place.
                            placeUse.calculateDistanceFrom(fileCtx.getLocationContext());
                            for (int i = 1; i < filtered.size(); ++i) {
                                VSinglePlace p = filtered.get(i);
                                p.calculateDistanceFrom(fileCtx.getLocationContext());
                                if (p.getDistance() < placeUse.getDistance()) {
                                    placeUse = p;
                                }
                            }
                        } else {
                            //Use most likely one
                            for(VSinglePlace p : filtered) {
                                if(p.getLikelihood() > placeUse.getLikelihood()) {
                                    placeUse = p;
                                }
                            }
                        }
                    }
                }
            }
            if(placeUse != null) {
                //Found our place to use.
                switch(placeUse.getPlaceType())
                {
                    case EVENT:
                        //1. Sharing at an Event/Social place: Event-type nearby AND Noise is loud
                        if(fileCtx.hasNoiseContext() && !fileCtx.getNoiseContext().isSilent()) {
                            //Store file randomly on one of the 3 nearest cloudlets or gateways
                            //because we want to share it at this event
                            if(fileCtx.hasLocationContext()) {
                                //Construct a list of 2 nodes per type and randomly get one from those.
                                //This is to prevent local overload of one node at an event by using
                                //one of them randomly.
                                NodeInfo n = manager.getNearestNodeOfTypes(
                                        new NodeType[] { CLOUDLET, GATEWAY },
                                        fileCtx.getLocationContext().getLatLng(),
                                        2);
                                if(n != null) {
                                    return n;
                                }
                                //No cloudlet or gateway available. So try corenet.
                                n = manager.getNearestNodeOfType(CORENET, fileCtx.getLocationContext().getLatLng());
                                if(n != null) {
                                    return n;
                                }
                                //No corenet. Try cloud.
                                n = manager.getNearestNodeOfType(CLOUD, fileCtx.getLocationContext().getLatLng());
                                if(n == null) {
                                    //No cloud node available.
                                    //Reaching here should only happen if no nodes are configured.
                                    return null;
                                }
                            } else {
                                //We have a place and noise but no location, so we choose a cloudlet or gateway randomly.
                                return manager.getRandomNodeOfTypes(new NodeType[] { CLOUDLET, GATEWAY });
                            }
                        } else {
                            //Near an event but it is not loud. Thus we assume the user does not want to share at the event.
                            //Store file in core net, this is a good tradeoff between sharing it
                            //with friends and sharing it at this event, if this was actually the intention.
                            if(fileCtx.hasLocationContext()) {
                                //Use the following hierarchy, if we have location context:
                                //Nearest Corenet. If not available, then nearest Cloud.
                                return manager.getNodeFollowingHierarchy(
                                        Arrays.asList(CORENET, CLOUD),
                                        NodeManager.Mode.NEAREST,
                                        fileCtx.getLocationContext().getLatLng());
                            } else {
                                //No location context, so we choose a corenet or cloud randomly.
                                return manager.getRandomNodeOfTypes(new NodeType[] { CORENET, CLOUD });
                            }
                        }
                        break;

                    case POI:
                        //2. Sharing at a POI. Pick a node with fallback hierarchy of the
                        //following order
                        //First: Nearest Corenet (best tradeoff between "I want to share at the POI"
                        //and "Share with friends")
                        NodeInfo node;
                        if(fileCtx.hasLocationContext()) {
                            //Can only use nearest nodes, if we have location context.
                            node = manager.getNodeFollowingHierarchy(
                                    Arrays.asList(CORENET, CLOUDLET, GATEWAY, CLOUD),
                                    NodeManager.Mode.NEAREST,
                                    fileCtx.getLocationContext().getLatLng());
                        } else {
                            //No location context, so we change mode to random
                            node = manager.getNodeFollowingHierarchy(
                                    Arrays.asList(CORENET, CLOUDLET, GATEWAY, CLOUD),
                                    NodeManager.Mode.RANDOM,
                                    null);
                        }
                        return node;

                    /*case WORK:
                        //3. Sharing at a work type
                        NodeInfo n;
                        //Check if we share the file during working hours
                        if(!ContextUtils.isWeekend(new Date(fileCtx.getTimestamp()))
                                && ContextUtils.isDateBetween(new Date(fileCtx.getTimestamp()), 8, 0, 18, 0)) {
                            //Then use a nearby cloudlet or gateway because we might share it with colleagues
                            if(fileCtx.hasLocationContext()) {
                                n = manager.getNearestNodeOfTypes(
                                        new NodeType[] { NodeType.GATEWAY, NodeType.CLOUDLET},
                                        fileCtx.getLocationContext().getLatLng(),
                                        2);
                            } else {
                                n = manager.getRandomNodeOfTypes(new NodeType[] { NodeType.GATEWAY, NodeType.CLOUDLET});
                            }
                        } else {
                            //Sharing the file at the weekend or in the morning/evening and thus
                            //not because of work.
                            //Store file in core net, this is a good tradeoff between sharing it
                            //with friends and sharing it at this place, if this was actually the intention.
                            if(fileCtx.hasLocationContext()) {
                                //Use the following hierarchy, if we have location context:
                                //Nearest Corenet. If not available, then nearest Cloud.
                                n = manager.getNodeFollowingHierarchy(
                                        Arrays.asList(CORENET, NodeType.CLOUD),
                                        NodeManager.Mode.NEAREST,
                                        fileCtx.getLocationContext().getLatLng());
                            } else {
                                //No location context, so we choose a corenet or cloud randomly.
                                n = manager.getRandomNodeOfTypes(new NodeType[] { CORENET, NodeType.CLOUD });
                            }
                        }
                        return n;*/

                    case UNKNOWN:
                    default:
                    //Will continue below
                    break;
                }
            }
            //No likely place found or PlaceType is UNKNOWN.
            //At an unknown place, we store the file in the intermediate level (= CoreNet), if available.
            //This is the best tradeoff between sharing it locally and sharing it with friends somewhere else.
            //If no corenet is available, it will be stored in the cloud.
            NodeInfo n = null;
            if(fileCtx.hasLocationContext()) {
                //Find nearest corenet/cloud
                n = manager.getNodeFollowingHierarchy(
                        Arrays.asList(CORENET, CLOUD),
                        NodeManager.Mode.NEAREST,
                        fileCtx.getLocationContext().getLatLng());
            }
            if(n == null) {
                //Neither corenet, nor cloud available. So try a random cloudlet or gateway so that the file
                //is at least stored somewhere.
                n = manager.getRandomNodeOfTypes(new NodeType[]{CLOUDLET, GATEWAY});
            }
            return n;
        }
    }

    /**
     * Returns the node(s) this decision has decided for.
     *
     * @return If decision was successful, it will return a list of NodeInfo objects. If decision was not
     * successful, null will be returned.
     */
    public List<NodeInfo> getDecidedNodes() {
        if(mDecidedNodes.size() == 0) {
            return null;
        }
        return mDecidedNodes;
    }
}
