package org.baderlab.autoannotate;

import static org.baderlab.autoannotate.NetworkTestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.baderlab.autoannotate.internal.model.AnnotationSet;
import org.baderlab.autoannotate.internal.model.Cluster;
import org.baderlab.autoannotate.internal.model.ModelManager;
import org.baderlab.autoannotate.internal.task.AnnotationSetTaskParamters;
import org.baderlab.autoannotate.internal.task.CreateAnnotationSetTask;
import org.baderlab.autoannotate.util.LogSilenceRule;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(JukitoRunner.class)
public class TestCreateAnnotationSetTask {

	@Rule public TestRule logSilenceRule = new LogSilenceRule();
	
	private CyNetwork network;
	private CyNetworkView networkView;
	private Map<String, CyNode> nodes;
	
	public static class TestModule extends NetworkTestUtils.TestModule { }
	
	@Before
	public void setUp() {
		networkView = createEmptyNetwork();
		network = networkView.getModel();
		
		nodes = new HashMap<>();
		nodes.put("n1", createNode(network, "n1", "cluster_1"));
		nodes.put("n2", createNode(network, "n2", "cluster_1"));
		nodes.put("n3", createNode(network, "n3", "cluster_1"));
		
		nodes.put("n4", createNode(network, "n4", "cluster_2"));
		nodes.put("n5", createNode(network, "n5", "cluster_2"));
		
		nodes.put("n6", createNode(network, "n6", "cluster_3"));
		
		nodes.put("n7", createNode(network, "n7", null));
		nodes.put("n8", createNode(network, "n8", null));
		nodes.put("n9", createNode(network, "n9", null));
	}
	
	
	@Test
	public void testCreateAnnotationSet(CreateAnnotationSetTask.Factory taskFactory, ModelManager modelManager) {
		test(false, taskFactory, modelManager);
	}
	
	@Test
	public void testCreateAnnotationSetWithSingletonClusters(CreateAnnotationSetTask.Factory taskFactory, ModelManager modelManager) { 
		test(true, taskFactory, modelManager);
	}
	
	private void test(boolean createSingletonClusters, CreateAnnotationSetTask.Factory taskFactory, ModelManager modelManager) {
		AnnotationSetTaskParamters params = createParams(networkView, createSingletonClusters, false);
		AnnotationSet as = createAnnotationSet(params, taskFactory, modelManager);

		if(createSingletonClusters)
			assertEquals(6, as.getClusterCount());
		else
			assertEquals(3, as.getClusterCount());
		
		List<Cluster> clusters = new ArrayList<>(as.getClusters());
		clusters.sort(Comparator.comparing(Cluster::getLabel));
		
		Cluster cluster1 = clusters.get(0);
		assertEquals(3, cluster1.getNodeCount());
		assertTrue(cluster1.contains(nodes.get("n1")));
		assertTrue(cluster1.contains(nodes.get("n2")));
		assertTrue(cluster1.contains(nodes.get("n3")));
		
		Cluster cluster2 = clusters.get(1);
		assertEquals(2, cluster2.getNodeCount());
		assertTrue(cluster2.contains(nodes.get("n4")));
		assertTrue(cluster2.contains(nodes.get("n5")));
		
		Cluster cluster3 = clusters.get(2);
		assertEquals(1, cluster3.getNodeCount());
		assertTrue(cluster3.contains(nodes.get("n6")));
		
		if(createSingletonClusters) {
			Cluster cluster4 = clusters.get(3);
			assertEquals(1, cluster4.getNodeCount());
			assertTrue(cluster4.contains(nodes.get("n7")));
			
			Cluster cluster5 = clusters.get(4);
			assertEquals(1, cluster5.getNodeCount());
			assertTrue(cluster5.contains(nodes.get("n8")));
			
			Cluster cluster6 = clusters.get(5);
			assertEquals(1, cluster6.getNodeCount());
			assertTrue(cluster6.contains(nodes.get("n9")));
		}
	}

}
