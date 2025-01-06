package org.baderlab.autoannotate.internal.ui.view.action;

import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.baderlab.autoannotate.internal.model.AnnotationSet;
import org.baderlab.autoannotate.internal.model.Cluster;
import org.baderlab.autoannotate.internal.model.ModelManager;
import org.baderlab.autoannotate.internal.model.NetworkViewSet;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNode;

import com.google.inject.Inject;
import com.google.inject.Provider;


@SuppressWarnings("serial")
public class ClusterLabelToTableAction extends AbstractCyAction {

	private static final String TITLE = "Create Column of Cluster Labels";
	
	@Inject private Provider<JFrame> jFrameProvider;
	@Inject private ModelManager modelManager;
	
	
	public ClusterLabelToTableAction() {
		super(TITLE + "...");		
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		Optional<AnnotationSet> asOpt = modelManager.getActiveNetworkViewSet().flatMap(NetworkViewSet::getActiveAnnotationSet);
		if(asOpt.isEmpty())
			return;
		
		AnnotationSet as = asOpt.get();
		
		String colName = promptForColumnName(as);
		if(colName == null)
			return;
		
		createColumn(as, colName);
	}
	
	
	private String promptForColumnName(AnnotationSet as) {
		JFrame frame = jFrameProvider.get();
		
		var network = as.getParent().getNetwork();
		var table = network.getDefaultNodeTable();

		var existingColumns = table.getColumns().stream().map(CyColumn::getName).collect(Collectors.toSet());
		
		while(true) {
			String newColName = JOptionPane.showInputDialog(frame, 
					"Create a column in the node table that contains\nthe current cluster labels.\n\nColumn name:", 
					"AutoAnnotate", 
					JOptionPane.PLAIN_MESSAGE);
			
			if(newColName == null) // cancel
				return null;
			
			newColName = newColName.trim();
			
			if(newColName.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Column name must not be blank.", "Error", JOptionPane.ERROR_MESSAGE);
			} else if(existingColumns.contains(newColName)) {
				JOptionPane.showMessageDialog(frame, "Column " + newColName + " already exists.", "Error", JOptionPane.ERROR_MESSAGE);
			} else {
				return newColName;
			}
		}
	}
	
	
	private void createColumn(AnnotationSet as, String columnName) {
		var network = as.getParent().getNetwork();
		var table = network.getDefaultNodeTable();
		
		table.createColumn(columnName, String.class, false);
		
		var clusters = as.getClusters();
		
		for(Cluster cluster : clusters) {
			String label = cluster.getLabel();
			var nodes = cluster.getNodes();
			
			for(CyNode node : nodes) {
				network.getRow(node).set(columnName, label);
			}
		}
	}
	
	
}
