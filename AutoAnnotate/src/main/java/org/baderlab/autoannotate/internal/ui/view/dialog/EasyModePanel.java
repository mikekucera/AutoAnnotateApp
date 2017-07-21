package org.baderlab.autoannotate.internal.ui.view.dialog;

import static org.baderlab.autoannotate.internal.ui.view.dialog.CreateAnnotationSetDialog.getColumnsOfType;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.baderlab.autoannotate.internal.AfterInjection;
import org.baderlab.autoannotate.internal.labels.LabelMakerFactory;
import org.baderlab.autoannotate.internal.labels.LabelMakerManager;
import org.baderlab.autoannotate.internal.model.ClusterAlgorithm;
import org.baderlab.autoannotate.internal.task.AnnotationSetTaskParamters;
import org.baderlab.autoannotate.internal.ui.view.LabelOptionsPanel;
import org.baderlab.autoannotate.internal.util.GBCFactory;
import org.baderlab.autoannotate.internal.util.SwingUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.view.model.CyNetworkView;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

@SuppressWarnings("serial")
public class EasyModePanel extends JPanel implements TabPanel {

	private final CyNetworkView networkView;
	private final CreateAnnotationSetDialog parent;
	
	@Inject private Provider<LabelMakerManager> labelManagerProvider;
	
	private JRadioButton clusterAllRadio;
	private JRadioButton clusterMaxRadio;
	private JCheckBox layoutCheckBox;
	private JSpinner spinner;
	private JComboBox<String> labelCombo;
	
	
	public static interface Factory {
		EasyModePanel create(CreateAnnotationSetDialog parent);
	}
	
	@Inject
	public EasyModePanel(@Assisted CreateAnnotationSetDialog parent, CyApplicationManager appManager) {
		this.networkView = appManager.getCurrentNetworkView();
		this.parent = parent;
	}
	
	
	@AfterInjection
	private void createContents() {
		JPanel parentPanel = new JPanel(new GridBagLayout());
		parentPanel.setOpaque(false);
		
		JPanel clusterPanel = createClusterPanel();
		clusterPanel.setOpaque(false);
		parentPanel.add(clusterPanel, GBCFactory.grid(0,0).get());
		
		JPanel labelPanel = createLabelPanel();
		labelPanel.setOpaque(false);
		parentPanel.add(labelPanel, GBCFactory.grid(0,1).weightx(1.0).get());
		
		setLayout(new BorderLayout());
		add(parentPanel, BorderLayout.NORTH);
		setOpaque(false);
	}
	
	
	private JPanel createClusterPanel() {
		JPanel clusterPanel = new JPanel(new GridBagLayout());
		clusterPanel.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));
		clusterPanel.setOpaque(false);
		
		clusterMaxRadio = new JRadioButton("Max number of annotations");
		clusterAllRadio = new JRadioButton("Annotate entire network");
		clusterMaxRadio.setToolTipText("Annotate only the top X largest clusters in the network");
		SwingUtil.groupButtons(clusterAllRadio, clusterMaxRadio);
		clusterAllRadio.setSelected(true);
		
		spinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
		layoutCheckBox = new JCheckBox("Layout network to prevent cluster overlap");
		SwingUtil.makeSmall(clusterMaxRadio, clusterAllRadio, spinner, layoutCheckBox);
		spinner.setEnabled(false);
		
		clusterAllRadio.addActionListener(e -> spinner.setEnabled(clusterMaxRadio.isSelected()));
		clusterMaxRadio.addActionListener(e -> spinner.setEnabled(clusterMaxRadio.isSelected()));
		
		clusterPanel.add(clusterAllRadio, GBCFactory.grid(0,0).get());
		clusterPanel.add(clusterMaxRadio, GBCFactory.grid(0,1).get());
		clusterPanel.add(spinner, GBCFactory.grid(1,1).get());
		clusterPanel.add(new JLabel(""), GBCFactory.grid(2,1).weightx(1.0).get());
		clusterPanel.add(new JLabel(" "), GBCFactory.grid(0,2).get());
		clusterPanel.add(layoutCheckBox, GBCFactory.grid(0,3).gridwidth(3).get());
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(LookAndFeelUtil.createTitledBorder("Cluster Options"));
		panel.add(clusterPanel, BorderLayout.CENTER);
		
		return panel;
	}
	
	
	private JPanel createLabelPanel() {
		JPanel labelPanel = new JPanel(new GridBagLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(4,0,0,0));
		labelPanel.setOpaque(false);
		
		JLabel label = new JLabel("Label Column:");
		labelCombo = LabelOptionsPanel.createLabelColumnCombo(networkView.getModel());
		SwingUtil.makeSmall(label, labelCombo);
		
		labelPanel.add(label, GBCFactory.grid(0,0).get());
		labelPanel.add(labelCombo, GBCFactory.grid(1,0).weightx(1.0).get());
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(LookAndFeelUtil.createTitledBorder("Label Options"));
		panel.add(labelPanel, BorderLayout.CENTER);
		
		return panel;
	}
	
	
	@Override
	public boolean isOkButtonEnabled() {
		return parent.isClusterMakerInstalled() && parent.isWordCloudInstalled();
	}
	
	public String getLabelColumn() {
		return labelCombo.getItemAt(labelCombo.getSelectedIndex());
	}
	
	private int getMaxClusters() {
		return ((SpinnerNumberModel)spinner.getModel()).getNumber().intValue();
	}
	
	
	private String getDefaultClusterMakerEdgeAttribute() {
		 List<String> columns = getColumnsOfType(networkView.getModel(), Number.class, false, false, false);
		 Optional<String> emColumn = columns.stream().filter(col -> col.endsWith("similarity_coefficient")).findAny();
		 return emColumn.orElse(CreateAnnotationSetDialog.NONE);
	}
	
	@Override
	public AnnotationSetTaskParamters createAnnotationSetTaskParameters() {
		LabelMakerFactory<?> labelMakerFactory = labelManagerProvider.get().getDefaultFactory();
		Object labelMakerContext = labelMakerFactory.getDefaultContext();
		
		AnnotationSetTaskParamters.Builder builder = 
			new AnnotationSetTaskParamters.Builder(networkView)
			.setLabelColumn(getLabelColumn())
			.setUseClusterMaker(true)
			.setClusterAlgorithm(ClusterAlgorithm.MCL)
			.setClusterMakerEdgeAttribute(getDefaultClusterMakerEdgeAttribute())
			.setLabelMakerFactory(labelMakerFactory)
			.setLabelMakerContext(labelMakerContext)
			.setCreateGroups(false)
			.setLayoutClusters(layoutCheckBox.isSelected());
		
		if(clusterAllRadio.isSelected()) {
			builder.setCreateSingletonClusters(true);
		} else {
			builder.setMaxClusters(getMaxClusters());
		}
		
		return builder.build();
	}

}
