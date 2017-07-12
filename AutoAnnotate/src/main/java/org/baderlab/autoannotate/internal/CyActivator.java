package org.baderlab.autoannotate.internal;

import static org.baderlab.autoannotate.internal.util.TaskTools.taskFactory;
import static org.cytoscape.work.ServiceProperties.*;

import java.util.Arrays;
import java.util.Properties;

import org.baderlab.autoannotate.internal.command.AnnotateCommandTask;
import org.baderlab.autoannotate.internal.command.CollapseCommandTask;
import org.baderlab.autoannotate.internal.command.ExpandCommandTask;
import org.baderlab.autoannotate.internal.command.LabelClusterCommandTask;
import org.baderlab.autoannotate.internal.command.LayoutCommandTask;
import org.baderlab.autoannotate.internal.command.RedrawCommandTask;
import org.baderlab.autoannotate.internal.command.SummaryNetworkCommandTask;
import org.baderlab.autoannotate.internal.labels.LabelFactoryModule;
import org.baderlab.autoannotate.internal.labels.LabelMakerFactory;
import org.baderlab.autoannotate.internal.labels.LabelMakerManager;
import org.baderlab.autoannotate.internal.model.ModelManager;
import org.baderlab.autoannotate.internal.model.io.ModelTablePersistor;
import org.baderlab.autoannotate.internal.ui.PanelManager;
import org.baderlab.autoannotate.internal.ui.view.WarnDialogModule;
import org.baderlab.autoannotate.internal.ui.view.action.CreateClusterTaskFactory;
import org.baderlab.autoannotate.internal.ui.view.action.ShowAboutDialogAction;
import org.baderlab.autoannotate.internal.ui.view.action.ShowCreateDialogAction;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.ops4j.peaberry.osgi.OSGiModule;
import org.osgi.framework.BundleContext;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;


public class CyActivator extends AbstractCyActivator {
	
	private Injector injector;
	
	
	@Override
	public void start(BundleContext bc) {
		injector = Guice.createInjector(
						new OSGiModule(bc), // Peaberry
						new AfterInjectionModule(), 
						new CytoscapeServiceModule(), 
						new ApplicationModule(), 
						new LabelFactoryModule(),
						new WarnDialogModule());
		
		// ModelManager listens to Cytoscape events
		ModelManager modelManager = injector.getInstance(ModelManager.class);
		registerAllServices(bc, modelManager, new Properties());
		
		// Register menu Actions
		PanelManager panelManager = injector.getInstance(PanelManager.class);
		registerAction(bc, injector.getInstance(ShowCreateDialogAction.class));
		registerAction(bc, panelManager.getShowHideAction());
		registerAction(bc, injector.getInstance(ShowAboutDialogAction.class));
		
		// Context menu action in network view
		CreateClusterTaskFactory createClusterTaskFactory = injector.getInstance(CreateClusterTaskFactory.class);
		Properties createClusterProps = new Properties();
		createClusterProps.setProperty(IN_MENU_BAR, "false");
		createClusterProps.setProperty(PREFERRED_MENU, APPS_MENU);
		createClusterProps.setProperty(TITLE, "AutoAnnotate - Create Cluster");
		registerAllServices(bc, createClusterTaskFactory, createClusterProps);
		
		// ModelTablePersistor listents to session save/load events
		ModelTablePersistor persistor = injector.getInstance(ModelTablePersistor.class);
		registerAllServices(bc, persistor, new Properties());
		
		// Configuration properties
		CyProperty<Properties> configProps = injector.getInstance(Key.get(new TypeLiteral<CyProperty<Properties>>(){}));
		Properties propsReaderServiceProps = new Properties();
		propsReaderServiceProps.setProperty("cyPropertyName", "autoannotate.props");
		registerAllServices(bc, configProps, propsReaderServiceProps);
		
		// Commands that depend on LabelMaker
		LabelMakerManager labelMakerManager = injector.getInstance(LabelMakerManager.class);
		for(LabelMakerFactory<?> factory : labelMakerManager.getFactories()) {
			// MKTODO make sure the factory ID doesn't contain spaces or other illegal characters
			LabelClusterCommandTask.Factory labelClusterCommandTaskFactory = injector.getInstance(LabelClusterCommandTask.Factory.class);
			TaskFactory labelTaskFactory = taskFactory(() -> labelClusterCommandTaskFactory.create(factory));
			
			AnnotateCommandTask.Factory annotateCommandTaskFactory = injector.getInstance(AnnotateCommandTask.Factory.class);
			TaskFactory annotateTaskFactory = taskFactory(() -> annotateCommandTaskFactory.create(factory));
			
			String id = factory.getID();
			String description = String.join(" ", Arrays.asList(factory.getDescription()));
			registerCommand(bc, "label-"+id, labelTaskFactory, "Run label algorithm '" + id + "'. " + description);
			registerCommand(bc, "annotate-"+id, annotateTaskFactory, "Annotate network using label algorithm '" + id + "'. " + description);
		}
		
		// Regular commands
		registerCommand(bc, "redraw", RedrawCommandTask.class, "Redraw annotations");
		registerCommand(bc, "layout", LayoutCommandTask.class, "Layout network by clusters");
		registerCommand(bc, "collapse", CollapseCommandTask.class, "Collapse all clusters");
		registerCommand(bc, "expand", ExpandCommandTask.class, "Expand all clusters");
		registerCommand(bc, "summary", SummaryNetworkCommandTask.class, "Create summary network");
		
		// If no session is loaded then this won't do anything, but if there is a session loaded 
		// then we want to load the model immediately.
		persistor.importModel();
	}
	
	
	@Override
	public void shutDown() {
		// MKTODO make this smarter like how EM does it
		try {
			ModelTablePersistor persistor = injector.getInstance(ModelTablePersistor.class);
			persistor.exportModel();
			ModelManager modelManager = injector.getInstance(ModelManager.class);
			modelManager.dispose();
		} finally {
			super.shutDown();
		}
	}
	
	
	
	private void registerAction(BundleContext bc, AbstractCyAction action) {
		action.setPreferredMenu("Apps." + BuildProperties.APP_NAME);
		registerService(bc, action, CyAction.class, new Properties());
	}
	
	private void registerCommand(BundleContext bc, String name, Class<? extends Task> type, String description) {
		TaskFactory taskFactory = taskFactory(injector.getProvider(type));
		registerCommand(bc, name, taskFactory, description);
	}
	
	private void registerCommand(BundleContext bc, String name, TaskFactory factory, String description) {
		Properties props = new Properties();
		props.put(ServiceProperties.COMMAND, name);
		props.put(ServiceProperties.COMMAND_NAMESPACE, "autoannotate");
		if(description != null)
			props.put("commandDescription", description); // added in Cytoscape 3.2
		registerService(bc, factory, TaskFactory.class, props);
	}
	
}
