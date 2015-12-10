package org.baderlab.autoannotate.internal.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetSelectedNetworkViewsEvent;
import org.cytoscape.application.events.SetSelectedNetworkViewsListener;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.ViewChangeRecord;
import org.cytoscape.view.model.events.ViewChangedEvent;
import org.cytoscape.view.model.events.ViewChangedListener;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ModelManager implements SetSelectedNetworkViewsListener, NetworkViewAboutToBeDestroyedListener, ViewChangedListener {
	
	@Inject private CyApplicationManager applicationManager;
	@Inject private EventBus eventBus;
	
	private boolean silenceEvents = false;
	
	private Map<CyNetworkView, NetworkViewSet> networkViews = new HashMap<>();
	
	
	public synchronized NetworkViewSet getNetworkViewSet(CyNetworkView networkView) {
		NetworkViewSet set = networkViews.get(networkView);
		if(set == null) {
			set = new NetworkViewSet(this, networkView);
			networkViews.put(networkView, set);
		}
		return set;
	}
	
	public NetworkViewSet getActiveNetworkViewSet() {
		CyNetworkView activeView = applicationManager.getCurrentNetworkView();
		return networkViews.get(activeView);
	}
	
	public Collection<NetworkViewSet> getNetworkViewSets() {
		return Collections.unmodifiableCollection(networkViews.values());
	}
	
	synchronized void postEvent(Object event) {
		if(!silenceEvents) {
			eventBus.post(event);
		}
	}
	
	public synchronized void silenceEvents(boolean silence) {
		this.silenceEvents = silence;
	}

	@Override
	public void handleEvent(SetSelectedNetworkViewsEvent e) {
		System.out.println("ModelManager.handleEvent(SetSelectedNetworkViewsEvent)");
		NetworkViewSet nvs = getActiveNetworkViewSet();
		postEvent(new ModelEvents.NetworkViewSetSelected(nvs));
	}

	@Override
	public void handleEvent(NetworkViewAboutToBeDestroyedEvent e) {
		System.out.println("ModelManager.handleEvent(NetworkViewAboutToBeDestroyedEvent)");
		CyNetworkView networkView = e.getNetworkView();
		NetworkViewSet networkViewSet = networkViews.remove(networkView);
		if(networkViewSet != null) {
			postEvent(new ModelEvents.NetworkViewSetDeleted(networkViewSet));
		}
	}

	public boolean isNetworkViewSetSelected(NetworkViewSet networkViewSet) {
		CyNetworkView view = applicationManager.getCurrentNetworkView();
		if(view == null)
			return networkViewSet == null;
		return view.equals(networkViewSet.getNetworkView());
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void handleEvent(ViewChangedEvent<?> e) {
		CyNetworkView networkView = e.getSource();
		NetworkViewSet nvs = getActiveNetworkViewSet();
		
		if(networkView.equals(nvs.getNetworkView())) {
			Set<Cluster> affectedClusters = new HashSet<>();
			
			Collection<?> payload = e.getPayloadCollection();
			
			for(ViewChangeRecord vcr: (Collection<ViewChangeRecord>)payload) {
				if (!(vcr.getView().getModel() instanceof CyNode))
					continue;
	
				VisualProperty<?> property =  vcr.getVisualProperty();
				if (property.equals(BasicVisualLexicon.NODE_X_LOCATION) ||
				    property.equals(BasicVisualLexicon.NODE_Y_LOCATION) ||
						property.equals(BasicVisualLexicon.NODE_WIDTH) ||
						property.equals(BasicVisualLexicon.NODE_HEIGHT)) {
	
					View<CyNode> nodeView = vcr.getView();
					CyNode node = nodeView.getModel();
					
					for(Cluster cluster : nvs.getActiveAnnotationSet().getClusters()) {
						if(cluster.contains(node)) {
							affectedClusters.add(cluster);
						}
					}
				}
			}
			
			for(Cluster cluster : affectedClusters) {
				postEvent(new ModelEvents.ClusterChanged(cluster));
			}
		}
		
	}
	
}
