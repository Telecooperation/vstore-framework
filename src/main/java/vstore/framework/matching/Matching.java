package vstore.framework.matching;

import static vstore.framework.node.NodeType.CLOUD;
import static vstore.framework.node.NodeType.CLOUDLET;
import static vstore.framework.node.NodeType.CORENET;
import static vstore.framework.node.NodeType.GATEWAY;
import static vstore.framework.node.NodeType.OWNCLOUD;
import static vstore.framework.node.NodeType.PHONE;
import static vstore.framework.node.NodeType.UNKNOWN;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.greenrobot.eventbus.EventBus;

import vstore.framework.context.ContextDescription;
import vstore.framework.context.RuleContextDescription;
import vstore.framework.context.types.place.VPlaces;
import vstore.framework.context.types.place.VSinglePlace;
import vstore.framework.db.table_helper.RulesDBHelper;
import vstore.framework.exceptions.DatabaseException;
import vstore.framework.file.VStoreFile;
import vstore.framework.logging.log_events.LogMatchingAddRuleEvent;
import vstore.framework.node.NodeInfo;
import vstore.framework.node.NodeManager;
import vstore.framework.node.NodeType;
import vstore.framework.rule.DecisionLayer;
import vstore.framework.rule.VStoreRule;
import vstore.framework.utils.ContextUtils;

/**
 * This class handles the storage decision, e.g. matching a file to a storage node based on the
 * usage context, the file meta data and the user intention, as well as the list of
 * available storage nodes.
 */
public class Matching {
    /**
     * Different matching modes.
     *
     * RULES_ONLY = Only use available rules. If no rule matches, the file is stored only on the phone.
     * RULES_NEXT_ON_NO_MATCH = Use next more general rule if no node found for current rule.
     * FALL_BACK = Experimental algorithm
     * RULES_THEN_FALL_BACK = If no rule matches, the fallback (experimental) algorithm is used.
     * RANDOM = A node is chosen completely randomly
     */
    public enum MatchingMode {
        RULES_ONLY, RULES_NEXT_ON_NO_MATCH, FALL_BACK, RULES_THEN_FALL_BACK, RANDOM
    }
    /**
     * Will hold the information about the decided node after the matching process.
     */
    private NodeInfo mDecidedNode;

    /**
     * Will hold the index of the used decision layer of the rule.
     */
    private int mUsedDecisionLayerIndex;

    /**
     * Make a new storage decision for the given parameters. If no rule matches, a default matching
     * is done. A default matching is also done, if a rule has a specific storage node configured
     * but this one cannot be found in the Node Manager.
     * To get the node that resulted from matching, use {@link Matching#getDecidedNode()}.
     *
     * All parameters must not be null, or a RuntimeException is thrown.
     *
     * @param f The file to make the decision for. Must not be null and should contain a context description.
     * @param mode The matching mode, one of the enum type {@link Matching.MatchingMode}.
     */
    public Matching(VStoreFile f, MatchingMode mode) {
        if(f == null) 
        {
            throw new RuntimeException("File must not be null.");
        }

        mUsedDecisionLayerIndex = -1;

        switch(mode) 
        {
            case RULES_ONLY:
            case RULES_NEXT_ON_NO_MATCH:
            case RULES_THEN_FALL_BACK:
                //Get a list of all rules that could be triggered by the MIME type of the file.
				RulesDBHelper dbHelper;
				List<VStoreRule> rules;
				try 
				{
					dbHelper = new RulesDBHelper();
					rules = dbHelper.getRulesMatchingFileType(f.getFileType());
				} catch (DatabaseException | SQLException e) {
					e.printStackTrace();
					mDecidedNode = null;
					return;
				}
                ContextDescription fileCtx = f.getContext();

                if(fileCtx == null) 
                {
                    //Should happen rarely
                    NodeManager manager = NodeManager.getInstance();
                    mDecidedNode = manager.getRandomNodeOfTypes(new NodeType[] { CLOUDLET, GATEWAY });
                    break;
                }

                //Check if we have some rules that might be applicable for the MIME type.
                //If yes, start filtering them by context
                if (rules.size() > 0) 
                {
                    //Step 1: Filter out all rules that do not apply to the file
                    Iterator<VStoreRule> it = rules.iterator();
                    while (it.hasNext()) 
                    {
                        VStoreRule r = it.next();
                        //Step 1.1: Eliminate rule if minimum file size is configured but does not match
                        if(r.hasFileSizeConfigured() && f.getFileSize() < r.getMinFileSize()) 
                        {
                            it.remove();
                            continue;
                        }
                        RuleContextDescription ruleCtx = r.getRuleContext();
                        //Step 1.2: Filter by sharing domain
                        //If a file is shared as public, every rule that applies to the private domain is
                        //removed and vice versa.
                        int fileSharingDomain = (f.isPrivate() ? 1 : 0);
                        if (!ContextUtils.isIncludedInSharingDomain(fileSharingDomain, r.getSharingDomain())) 
                        {
                            it.remove();
                            continue;
                        }
                        //Step 1.3: Filter by day and timespan
                        //Ignore any rules that do not have a matching weekday and time.
                        //No days configured means day/time does not matter.
                        if (r.getWeekdays().size() > 0) {
                            int day = ContextUtils.getDayOfWeek();
                            if (!r.getWeekdays().contains(day)) {
                                //Remove rule if the current day is not contained in the day list of the rule
                                it.remove();
                                continue;
                            } else if (r.hasTimeSet() && !ContextUtils.isNowBetween(r.getStartHour(),
                                    r.getStartMinutes(), r.getEndHour(), r.getEndMinutes())) {
                                //Remove rule if the current time is not in the timespan of the rule
                                it.remove();
                                continue;
                            }
                        }
                        //Step 1.4: Filter by location
                        //If the file's location context is not contained in the radius configured for
                        //the rule, we ignore the rule.
                        if (r.hasLocationContext() && fileCtx.hasLocationContext()) 
                        {
                            double distance = ContextUtils.distanceBetween(ruleCtx.getLocationContext(),
                                    fileCtx.getLocationContext().getLatLng());
                            if (Math.ceil(distance) > ruleCtx.getRadius()) 
                            {
                                //Distance is farther away than the radius allows. Thus, this rule does
                                //not apply
                                it.remove();
                                continue;
                            }
                        }
                        //Step 1.5: Filter by place types
                        //If the place type of the most likely place of the file's context
                        //is not contained in those of the rule, we ignore the rule.
                        if (r.hasPlaceContext()) 
                        {
                            if (fileCtx.getMostLikelyPlace() == null || !ruleCtx.getPlaceTypes().contains(
                                    fileCtx.getPlaces().getMostLikelyPlace().getPlaceType())) 
                            {
                                it.remove();
                                continue;
                            }
                        }
                        //Step 1.6: Filter by network
                        //We assume that if the rule has a WiFi name set, this has next highest priority.
                        //If the rule has not WiFi set, but mobile network, than it is checked if the
                        //same type is given.
                        //So if the file context's network conditions do not meet those of the rule,
                        //we ignore the rule.
                        if (r.hasNetworkContext() && fileCtx.hasNetworkContext() &&
                                !fileCtx.getNetworkContext().matches(r.getRuleContext().getNetworkContext())) 
                        {
                            it.remove();
                            continue;
                        }
                        //Step 1.7: Filter by activity
                        //If a rule has activity context set but it does not match the file's activity,
                        //we ignore the rule.
                        if (r.hasActivityContext() && fileCtx.hasActivityContext() && !fileCtx.getActivityContext()
                                .matches(r.getRuleContext().getActivityContext())) 
                        {
                            it.remove();
                            continue;
                        }
                        //Step 1.8: Filter by noise
                        //If both have noise context set: Match the dB value of the file context vs the
                        //threshold of the rule. If the rule specifies "must be silent" but it's not, we
                        //ignore the rule. If it specifies "must be loud" but it's not, we ignore the rule.
                        if (r.hasNoiseContext() && fileCtx.hasNoiseContext()
                                && !r.getRuleContext().getNoiseContext().matches(fileCtx.getNoiseContext())) 
                        {
                            it.remove();
                        }
                    }

                    //Step 2: Now the rules that are remaining do all match the file's context.
                    //Continue if some are actually remaining.
                    if (rules.size() > 0) 
                    {
                        //Step 2.1: Calculate the detail score for each rule.
                        //Remember all in a list to choose the next more general one if no
                        //matching node is found and Mode is RULES_NEXT_ON_NO_MATCH.
                        List<VStoreRule> ruleList = new ArrayList<>();
                        for (VStoreRule r : rules) 
                        {
                            float score = r.getDetailScore();
                            int pos = 0;
                            //Add it sorted into the list (highest to lowest score)
                            while(pos < ruleList.size()) 
                            {
                                if(ruleList.get(pos).getDetailScore() < score) break;
                                ++pos;
                            }
                            ruleList.add(pos, r);
                        }

                        VStoreRule usedRule = null;
                        //First is now the rule with the highest score
                        if (mode.equals(MatchingMode.RULES_NEXT_ON_NO_MATCH)) 
                        {
                            //Try rule by rule, when one of the more detailed ones does not
                            //yield a storage node
                            for (VStoreRule r : ruleList) 
                            {
                                usedRule = r;
                                if(getNodeForRule( r, f)) break;
                            }
                            //If mDecidedNode is still null after the loop, no rule had a matching node
                            //or one rule had PHONE as a layer

                        } 
                        else 
                        {
                            //Get highest rated rule without using the next rule on no result
                            usedRule = ruleList.get(0);
                            getNodeForRule( usedRule, f);
                        }
                        //Log that we used this rule
                        LogMatchingAddRuleEvent logEvt = new LogMatchingAddRuleEvent();
                        logEvt.fileId = f.getUUID();
                        logEvt.rule = usedRule;
                        logEvt.mDecisionLayerIndex = this.mUsedDecisionLayerIndex;
                        EventBus.getDefault().post(logEvt);
                    }
                }

                if(mode.equals(MatchingMode.RULES_THEN_FALL_BACK) && mDecidedNode == null) 
                {
                    //If we reached until here: No rule matched or no rules have been configured
                    //(or rules have been disabled).
                    //Fallback to some default action (based on file context)
                    mDecidedNode = Matching.getNodeFallback( f);
                    return;
                }
                break;

            case RANDOM:
                NodeManager manager = NodeManager.getInstance();
                mDecidedNode = manager.getRandomNode();
                break;

            //Currently not used (only there for experimental purposes)!
            case FALL_BACK:
                mDecidedNode = Matching.getNodeFallback( f);
                break;

            default:
                //mDecidedNode stays null
                break;
        }
    }

    /**
     * This method determines the node for the given rule. Will go through all decision layers until one
     * layer yields a result or until the end is reached.
     * @param r The rule to find a node for.
     * @param f The file to find a node for.
     * @return True, if a match was found. False, if not.
     */
    private boolean getNodeForRule(VStoreRule r, VStoreFile f) {
        NodeManager m = NodeManager.getInstance();
        mUsedDecisionLayerIndex = 0;
        for(DecisionLayer layer : r.getDecisionLayers()) 
        {
            //Check if no node is configured (= either PHONE or UNKNOWN as type)
            if(layer.selectedType.equals(PHONE) || layer.selectedType.equals(UNKNOWN)) 
            {
                mDecidedNode = null;
                return true;
            }

            //Check if specific node is configured
            if(layer.isSpecific && layer.specificNodeId != null && !layer.specificNodeId.equals("")) 
            {
                NodeInfo n = m.getNode(layer.specificNodeId);
                if(n != null) 
                {
                    mDecidedNode = n;
                    return true;
                }
            }

            //Check if NONE and some constraints are configured
            if(layer.selectedType.equals(NodeType.NONE) 
            		&& (layer.maxRadius > 0 || layer.minBwUp > 0 || layer.minBwDown > 0)) 
            {
                if(layer.minBwUp > 0 || layer.minBwDown > 0) 
                {
                    mDecidedNode = m.getRandomNodeMatchingBandwidthAndRadius(layer.minBwUp, layer.minBwDown,
                            layer.minRadius, layer.maxRadius,
                            f.getContext().getLocationContext().getLatLng());

                    if(mDecidedNode == null) 
                    {
                        ++mUsedDecisionLayerIndex;
                        continue;
                    } 
                    else 
                    {
                        return true;
                    }
                }
            }

            //All constraints 0 --> Get random node of the configured type
            if(layer.minRadius == 0 && layer.maxRadius == 0 && layer.minBwUp == 0 && layer.minBwDown == 0) 
            {
                mDecidedNode = m.getRandomNodeOfTypes(new NodeType[] { layer.selectedType });

                if(mDecidedNode == null) 
                {
                    ++mUsedDecisionLayerIndex;
                    continue;
                } 
                else 
                {
                    return true;
                }
            }

            //Radius constraints set (will also take bandwidth into account if necessary)
            if(layer.minRadius>=0 && layer.maxRadius>0) 
            {
                mDecidedNode = m.getRandomNodeOfTypeMatchingBandwidth(
                        layer.selectedType, layer.minBwUp, layer.minBwDown, layer.minRadius,
                        layer.maxRadius, f.getContext().getLocationContext().getLatLng());

                if(mDecidedNode != null) 
                {
                    return true;
                }
            }

            //Radius constraints not set or invalid. Thus, use bandwidth constraints.
            if(layer.minBwUp>0 || layer.minBwDown>0) 
            {
                mDecidedNode = m.getRandomNodeOfTypeMatchingBandwidth(
                        layer.selectedType, layer.minBwUp, layer.minBwDown, 0, 0, null);

                if(mDecidedNode != null) 
                {
                    return true;
                }
            }

            ++mUsedDecisionLayerIndex;
        }
        mUsedDecisionLayerIndex = -1;
        return false;
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
        NodeManager manager = NodeManager.getInstance();
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
                //OWNCLOUD, CORENET, CLOUD
                return manager.getNodeFollowingHierarchy(
                        Arrays.asList(OWNCLOUD, CORENET, CLOUD),
                        NodeManager.Mode.NEAREST,
                        fileCtx.getLocationContext().getLatLng());
            }
            //No location context (should happen rarely).
            //Get a random node following this hierarchy:
            //OWNCLOUD, CORENET, CLOUD
            return manager.getNodeFollowingHierarchy(
                    Arrays.asList(OWNCLOUD, CORENET, CLOUD),
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
     * Returns the node this decision has decided for.
     *
     * @return If decision was successful, it will return a NodeInfo object. If decision was not
     * successful, null will be returned.
     */
    public NodeInfo getDecidedNode() {
        return mDecidedNode;
    }
}
