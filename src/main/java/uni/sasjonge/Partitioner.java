package uni.sasjonge;

import java.io.IOException;
import java.util.List;

import javax.xml.transform.TransformerConfigurationException;

import org.jgrapht.io.ExportException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.xml.sax.SAXException;

import uni.sasjonge.partitioning.PartitioningCore;
import uni.sasjonge.utils.GraphExporter;
import uni.sasjonge.utils.OntologyReducer;

public class Partitioner {
	
	// The output path for the graph
	static final String GRAPH_OUTPUT_PATH = "/home/sascha/Desktop/test.graphml";
	
	// The input ontology
	// static final String INPUT_ONTOLOGY = "file:/home/sascha/workspace/java_ws/partitioner/res/knowrob_merged.owl";
	static final String INPUT_ONTOLOGY = "file:/home/sascha/workspace/java_ws/partitioner/res/pto.owl";
	// static final String INPUT_ONTOLOGY = "file:/home/sascha/workspace/java_ws/partitioner/res/partitioner_test.owl";
	// static final String INPUT_ONTOLOGY = "file:/home/sascha/pepper_dialog/ros_dep/src/knowrob/knowrob_household/owl/kitchen_items.owl";
	// SNOMED
	
	public static void main(String[] args) {
		long startTime = System.nanoTime();
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		// Initizalize the partioning algorithm
		PartitioningCore pc = new PartitioningCore();
		
		try {
			// Load the input ontology
			long loadStartTime = System.nanoTime();
			OWLOntology loadedOnt = manager.loadOntology(IRI.create(INPUT_ONTOLOGY));
			long loadEndTime = System.nanoTime();
			System.out.println("Loading the ontology took " + (loadEndTime - loadStartTime)/1000000 + "ms");

			long reduceStartTime = System.nanoTime();
			OWLOntology ontology = OntologyReducer.removeHighestLevelConc(manager,loadedOnt,2);
			//OWLOntology ontology = (new OntologyReducer(manager,loadedOnt)).removeHighestLevelConc(3);

			long reduceEndTime = System.nanoTime();
			System.out.println("Reducing the ontology took " + (reduceEndTime - reduceStartTime)/1000000 + "ms");
			
			long startPartTime = System.nanoTime();
			// Call the partitioning algorithm
			List<OWLOntology> partitionedOntologies = pc.partition(ontology);
			long endPartTime = System.nanoTime();
			System.out.println("Partitioning took " + (endPartTime - startPartTime)/1000000 + "ms");
			
			// Export the onotologys
			//partitionedOntologies.stream().forEach(t -> {
			//	try {
			//		manager.saveOntology(t);
			//	} catch (OWLOntologyStorageException e) {
			//		e.printStackTrace();
			//	}
			//});
			
			// Export the graph
			long startGraphTime = System.nanoTime();
			GraphExporter.init(ontology);
			//GraphExporter.exportCCStructureGraphSimple(pc.g, ontology, GRAPH_OUTPUT_PATH);
			GraphExporter.exportCCStructureGraphSimpleHideAxiomLestt(pc.g, ontology, pc.vertexToAxiom, GRAPH_OUTPUT_PATH);
			// GraphExporter.exportCCStructureGraphWithAllSubConceptsAndAx(pc.g, pc.vertexToAxiom, GRAPH_OUTPUT_PATH);
			//GraphExporter.exportComplexGraph(pc.g, GRAPH_OUTPUT_PATH);
			long endGraphTime = System.nanoTime();
			System.out.println("Graph building took " + (endGraphTime - startGraphTime)/1000000 + "ms");
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long endTime = System.nanoTime();
		System.out.println("Overall: " + (endTime - startTime)/1000000 + "ms");

	}
}
