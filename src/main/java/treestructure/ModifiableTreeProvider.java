package treestructure;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dsutil.protopeer.FingerDescriptor;
import dsutil.protopeer.services.topology.trees.TreeApplicationInterface;
import dsutil.protopeer.services.topology.trees.TreeMiddlewareInterface;
import dsutil.protopeer.services.topology.trees.TreeProviderInterface;
import protopeer.BasePeerlet;
import protopeer.Finger;
import protopeer.Peer;

/**
 * The class is mediator between ModifiableTreeClient and the application (the Agent).
 * Main functionalities include:
 *  - extracting <code>Finger</code> from <code>FingerDescriptor</code> of parent-agent and children agents.
 *  
 * @author jovan
 *
 */
public class ModifiableTreeProvider extends BasePeerlet implements TreeProviderInterface {
	
	private Logger logger = Logger.getLogger(ModifiableTreeProvider.class.toString());
	
	public ModifiableTreeProvider() {	}
	
	/**
	 * Returns the peerlet that is implementor of TreeApplicationInterface.
	 * 
	 * @return peerlet that implements TreeApplicationInterface
	 */
	private TreeApplicationInterface getApplication(){
        return (TreeApplicationInterface) getPeer().getPeerletOfType(TreeApplicationInterface.class);
    }
	
	/**
	 * Returns the peerlet that is implementor of TreeMiddlewareInterface.
	 * 
	 * @return peerlet that implements TreeMiddlewareInterface
	 */
	private TreeMiddlewareInterface getTreeMiddleware(){
        return (TreeMiddlewareInterface) getPeer().getPeerletOfType(TreeMiddlewareInterface.class);
    }
	
	@Override
    public void init(Peer peer) {
        super.init(peer);
    }
	
	@Override
    public void start() {
        super.start();
    }

	@Override
	/**
	 * From TreeProviderInterface:
	 * Sets children's fingers to the application. If any child's FingerDescriptor is <code>null</code>,
	 * <code>null</code> is added to the children's fingers list. Otherwise, <code>Finger</code> is extracted,
	 * which itself can be <code>null</code> and as such is added to the children's fingers list.
	 * 
	 * Note that the list can have != 0 elements, but all of them can be <code>null</code>, implying
	 * that this agent effectively has no children.
	 */
	public void provideChildren(List<FingerDescriptor> children) {		
		this.getApplication().setChildren(this.extractFingersFromChildren(children));
	}
	
	private List<Finger> extractFingersFromChildren(List<FingerDescriptor> children) {
		ArrayList<Finger> childrensFingers = new ArrayList<>();
		if(children != null) {			
			children.stream().map(f -> (f == null) ? null : f.getFinger() )
			                 .forEach(f -> childrensFingers.add(f));
		}
		return childrensFingers;
	}

	@Override
	/**
	 * From TreeProviderInterface:
	 * Sets Finger of the Parent Agent to this Agent. Note that finger itself can be <code>null</code>
	 * but <code>FingerDescriptor</code> must not be <code>null</code>.
	 */
	public void provideParent(FingerDescriptor parent) {
		this.getApplication().setParent(this.extractParentsFinger(parent));
	}
	
	private Finger extractParentsFinger(FingerDescriptor parent) {
		return (parent == null) ? null : parent.getFinger();
	}

	@Override
	/**
	 * From TreeProviderInterface:
	 */
	public void provideTreeView(FingerDescriptor parent, List<FingerDescriptor> children) {
		List<Finger> childrensFingers = this.extractFingersFromChildren(children);
		Finger parentsFinger = this.extractParentsFinger(parent);
		//this.logger.log(Level.FINER, "NODE: " + this.getPeer().getIndexNumber() + "ModifiableTreeProvider::provideTreeView()");
		this.getApplication().setTreeView(parentsFinger, childrensFingers);		
	}

}
