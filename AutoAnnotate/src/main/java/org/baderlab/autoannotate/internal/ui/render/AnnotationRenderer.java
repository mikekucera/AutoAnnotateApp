package org.baderlab.autoannotate.internal.ui.render;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.baderlab.autoannotate.internal.model.AnnotationSet;
import org.baderlab.autoannotate.internal.model.Cluster;
import org.baderlab.autoannotate.internal.model.DisplayOptions;
import org.baderlab.autoannotate.internal.model.ModelEvents;
import org.baderlab.autoannotate.internal.model.NetworkViewSet;
import org.baderlab.autoannotate.internal.ui.render.DrawClusterTask.LabelArgs;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class AnnotationRenderer {
	
	@Inject private DialogTaskManager dialogTaskManager;
	@Inject private SynchronousTaskManager<?> syncTaskManager;
	
	@Inject private Provider<DrawClusterTask> drawTaskProvider;
	@Inject private Provider<EraseClusterTask> eraseTaskProvider;
	@Inject private Provider<RemoveAllAnnotationsTask> removeAllTaskProvider;
	@Inject private Provider<SelectClusterTask> selectTaskProvider;
	
	private Map<Cluster,TextAnnotation> textAnnotations = new HashMap<>();
	private Map<Cluster,ShapeAnnotation> shapeAnnotations = new HashMap<>();
	
	
	@Inject
	public void registerForEvents(EventBus eventBus) {
		eventBus.register(this);
	}
	
	
	@Subscribe
	public void handle(ModelEvents.AnnotationSetSelected event) {
		redrawAnnotations(event.getNetworkViewSet(), event.getAnnotationSet());
	}
	
	public void redrawAnnotations(NetworkViewSet networkViewSet, Optional<AnnotationSet> selected) {
		TaskIterator tasks = new TaskIterator();
		
		RemoveAllAnnotationsTask removeTask = removeAllTaskProvider.get();
		removeTask.setNetworkViewSet(networkViewSet);
		tasks.append(removeTask);
		
		if(selected.isPresent()) {
			for(Cluster cluster : selected.get().getClusters()) {
				tasks.append(drawTaskProvider.get().setCluster(cluster));
			}
		}
		
		dialogTaskManager.execute(tasks);
	}
	
	
	@Subscribe
	public void handle(ModelEvents.ClusterChanged event) {
		Cluster cluster = event.getCluster();
		if(cluster.getParent().isActive()) {
			TaskIterator tasks = new TaskIterator();
			tasks.append(eraseTaskProvider.get().setCluster(cluster));
			tasks.append(drawTaskProvider.get().setCluster(cluster));
			syncTaskManager.execute(tasks);
		}
	}
	
	
	@Subscribe
	public void handle(ModelEvents.ClusterRemoved event) {
		Cluster cluster = event.getCluster();
		TaskIterator tasks = new TaskIterator();
		tasks.append(eraseTaskProvider.get().setCluster(cluster));
		syncTaskManager.execute(tasks);
	}
	
	
	@Subscribe
	public void handle(ModelEvents.ClusterAdded event) {
		Cluster cluster = event.getCluster();
		TaskIterator tasks = new TaskIterator(drawTaskProvider.get().setCluster(cluster));
		syncTaskManager.execute(tasks);
	}
	
	
	@Subscribe
	public void handle(ModelEvents.DisplayOptionChanged event) {
		DisplayOptions options = event.getDisplayOptions();
		
		switch(event.getOption()) {
		case BORDER_WIDTH:
			for(Cluster cluster : options.getParent().getClusters()) {
				ShapeAnnotation shape = shapeAnnotations.get(cluster);
				if(shape != null) {
					shape.setBorderWidth(options.getBorderWidth());
					shape.update();
				}
			}
			break;
		case OPACITY:
		case SHOW_CLUSTERS:
			for(Cluster cluster : options.getParent().getClusters()) {
				ShapeAnnotation shape = shapeAnnotations.get(cluster);
				if(shape != null) {
					shape.setFillOpacity(options.isShowClusters() ? options.getOpacity() : 0);
					shape.setBorderOpacity(options.isShowClusters() ? 100 : 0);
					shape.update();
				}
			}
			break;
		case FONT_SCALE:
		case SHOW_LABELS:
		case USE_CONSTANT_FONT_SIZE:
			for(Cluster cluster : options.getParent().getClusters()) {
				TextAnnotation text = textAnnotations.get(cluster);
				if(text != null) {
					LabelArgs labelArgs = DrawClusterTask.computeLabelArgs(this,cluster);
					double fontSize = options.isShowLabels() ? labelArgs.fontSize : 0;
					text.setFontSize(fontSize);
					text.setSpecificZoom(labelArgs.zoom);
					text.moveAnnotation(new Point2D.Double(labelArgs.x, labelArgs.y));
					text.update();
				}
			}
			break;
		case SHAPE_TYPE:
			for(Cluster cluster : options.getParent().getClusters()) {
				ShapeAnnotation shape = shapeAnnotations.get(cluster);
				if(shape != null) {
					shape.setShapeType(options.getShapeType().shapeName());
					shape.update();
				}
			}
			break;
		}
		
	}
	
	@Subscribe
	public void handle(ModelEvents.ClustersSelected event) {
		AnnotationSet annotationSet = event.getAnnotationSet();
		Collection<Cluster> select = event.getClusters();
		Set<Cluster> deselect = new HashSet<>(annotationSet.getClusters());
		deselect.removeAll(select);
		
		TaskIterator tasks = new TaskIterator();
		
		for(Cluster cluster : deselect) {
			SelectClusterTask deselectTask = selectTaskProvider.get();
			deselectTask.setCluster(cluster);
			deselectTask.setSelect(false);
			tasks.append(deselectTask);
		}
		for(Cluster cluster : select) {
			SelectClusterTask selectTask = selectTaskProvider.get();
			selectTask.setCluster(cluster);
			tasks.append(selectTask);
		}
		
		syncTaskManager.execute(tasks);
	}
	

	
	ShapeAnnotation getShapeAnnotation(Cluster cluster) {
		return shapeAnnotations.get(cluster);
	}
	
	void setShapeAnnotation(Cluster cluster, ShapeAnnotation shapeAnnotation) {
		shapeAnnotations.put(cluster, shapeAnnotation);
	}
	
	ShapeAnnotation removeShapeAnnoation(Cluster cluster) {
		return shapeAnnotations.remove(cluster);
	}
	
	
	TextAnnotation getTextAnnotation(Cluster cluster) {
		return textAnnotations.get(cluster);
	}
	
	void setTextAnnotation(Cluster cluster, TextAnnotation textAnnotation) {
		textAnnotations.put(cluster, textAnnotation);
	}

	TextAnnotation removeTextAnnotation(Cluster cluster) {
		return textAnnotations.remove(cluster);
	}
	
}