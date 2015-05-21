package org.eclipse.smarthome.binding.digitalstrom.handler;

/**
 * 
 * @author Michael Ochel - Initial contribution
 * @author Mathias Siegele - Initial contribution
 *
 */
public interface SceneStatusListener {

	public final static String SCENE_DESCOVERY = "SceneDiscovey";
	
	/**
     * This method is called whenever the state of the given scene has changed.
     * 
     * @param call_undo
     * 
     */
    public void onSceneStateChanged(boolean call_undo);
    
    /**
     * This method is called whenever a scene is removed.
     * 
     */
    public void onSceneRemoved();

    /**
     * This method is called whenever a scene is added.
     * 
     * @param device
     * 
     */
    public void onSceneAdded(boolean flag );

    
}
