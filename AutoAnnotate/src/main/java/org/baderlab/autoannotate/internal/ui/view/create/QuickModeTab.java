package org.baderlab.autoannotate.internal.ui.view.create;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
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
import org.baderlab.autoannotate.internal.task.AnnotationSetTaskParamters.ClusterMakerParameters;
import org.baderlab.autoannotate.internal.util.GBCFactory;
import org.baderlab.autoannotate.internal.util.SwingUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyColumnComboBox;
import org.cytoscape.application.swing.CyColumnPresentationManager;
import org.cytoscape.model.CyColumn;
import org.cytoscape.view.model.CyNetworkView;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

@SuppressWarnings("serial")
public class QuickModeTab extends JPanel implements DialogTab {

	private static final ClusterAlgorithm DEFAULT_CLUSTER_ALG = ClusterAlgorithm.MCL;
	private static final String EM_SIMILARITY_COLUMN_SUFFIX = "similarity_coefficient";
	
	private final CyNetworkView networkView;
	private final DialogParent parent;
	
	@Inject private Provider<LabelMakerManager> labelManagerProvider;
	@Inject private Provider<CyColumnPresentationManager> presentationManagerProvider;
	@Inject private InstallWarningPanel.Factory installWarningPanelFactory;
	@Inject private DependencyChecker dependencyChecker;
	
	private JRadioButton clusterAllRadio;
	private JRadioButton clusterMaxRadio;
	private JCheckBox layoutCheckBox;
	private JSpinner spinner;
	private CyColumnComboBox labelCombo;
	
	private InstallWarningPanel warnPanel;
	private boolean ready;
	
	public static interface Factory {
		QuickModeTab create(DialogParent parent);
	}
	
	@Inject
	public QuickModeTab(@Assisted DialogParent parent, CyApplicationManager appManager) {
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
		
		warnPanel = installWarningPanelFactory.create(parentPanel, DependencyChecker.CLUSTERMAKER);
		warnPanel.setOnClickHandler(() -> parent.close());
		warnPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
		
		setLayout(new BorderLayout());
		add(warnPanel, BorderLayout.NORTH);
		setOpaque(false);
	}
	
	
	private JPanel createClusterPanel() {
		JPanel clusterPanel = new JPanel(new GridBagLayout());
		clusterPanel.setBorder(BorderFactory.createEmptyBorder(14,10,10,10));
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
		return clusterPanel;
	}
	
	
	private JPanel createLabelPanel() {
		JPanel labelPanel = new JPanel(new GridBagLayout());
		labelPanel.setBorder(BorderFactory.createEmptyBorder(14,10,10,10));
		labelPanel.setOpaque(false);
		
		JLabel label = new JLabel("Label Column:");
		labelCombo = CreateViewUtil.createLabelColumnCombo(presentationManagerProvider.get(), networkView.getModel());
		SwingUtil.makeSmall(label, labelCombo);
		
		labelPanel.add(label, GBCFactory.grid(0,0).get());
		labelPanel.add(labelCombo, GBCFactory.grid(1,0).weightx(1.0).get());
		
		return labelPanel;
	}
	
	@Override
	public void onShow() {
		ready = dependencyChecker.isClusterMakerInstalled();
		warnPanel.showWarning(!ready);
		
		List<CyColumn> columns = CreateViewUtil.getColumnsOfType(networkView.getModel(), String.class, true, true);
		CreateViewUtil.updateColumnCombo(labelCombo, columns);
	}
	
	
	@Override
	public void reset() {
		spinner.setValue(10);
		clusterAllRadio.setSelected(true);
		layoutCheckBox.setSelected(false);
		CreateViewUtil.setLabelColumnDefault(labelCombo);
	}
	
	@Override
	public boolean isReady() {
		return ready;
	}
	
	public CyColumn getLabelColumn() {
		return labelCombo.getSelectedItem();
	}
	
	private int getMaxClusters() {
		return ((SpinnerNumberModel)spinner.getModel()).getNumber().intValue();
	}
	
	
	private Optional<CyColumn> getDefaultClusterMakerEdgeAttribute() {
		List<CyColumn> columns = CreateViewUtil.getColumnsOfType(networkView.getModel(), Number.class, false, false);
		return columns.stream().filter(c -> c.getName().endsWith(EM_SIMILARITY_COLUMN_SUFFIX)).findAny();
	}
	
	@Override
	public AnnotationSetTaskParamters createAnnotationSetTaskParameters() {
		LabelMakerFactory<?> labelMakerFactory = labelManagerProvider.get().getDefaultFactory();
		Object labelMakerContext = labelMakerFactory.getDefaultContext();
		String edgeAttribute = getDefaultClusterMakerEdgeAttribute().map(CyColumn::getName).orElse(null);
		
		AnnotationSetTaskParamters.Builder builder = 
			new AnnotationSetTaskParamters.Builder(networkView)
			.setLabelColumn(getLabelColumn().getName())
			.setClusterParameters(new ClusterMakerParameters(DEFAULT_CLUSTER_ALG, edgeAttribute))
			.setLabelMakerFactory(labelMakerFactory)
			.setLabelMakerContext(labelMakerContext)
			.setLayoutClusters(layoutCheckBox.isSelected());
		
		if(clusterAllRadio.isSelected())
			builder.setCreateSingletonClusters(true);
		else
			builder.setMaxClusters(getMaxClusters());
		
		return builder.build();
	}

}
