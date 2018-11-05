package uni.sasjonge.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRemover;

public class OntologyHierarchy {

	Map<Integer, List<OWLClass>> depthToClasses = new HashMap<>();
	Map<Integer, List<OWLObjectProperty>> depthToProperties = new HashMap<>();
	boolean somethingChanged = false;

	Map<OWLClass, OWLClass> classToParent = new HashMap<>();
	Map<OWLObjectProperty, OWLObjectProperty> propertyToParent = new HashMap<>();

	Map<String, String> classToParentString = new HashMap<>();
	Map<String, String> propertyToParentString = new HashMap<>();

	Map<String, Set<String>> parentToClassesString = new HashMap<>();
	Map<String, Set<String>> parentToPropertiesString = new HashMap<>();
	
	OWLReasonerFactory reasonerFactory;
	OWLReasoner reasoner;
	OWLDataFactory df;

	public OntologyHierarchy(OWLOntology ontology) throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology copydOnt = manager.copyOntology(ontology, OntologyCopy.DEEP);
		
		reasonerFactory = new StructuralReasonerFactory();
		reasoner = reasonerFactory.createNonBufferingReasoner(copydOnt);
		df = manager.getOWLDataFactory();

		System.out.println("STEEEEEP 1x1");

		initDepthToClass(manager, copydOnt, 0);

		System.out.println("STEEEEEP 1x2");

		initDepthToRoles(manager, copydOnt, 0);

		System.out.println("STEEEEEP 1x3");
		parentToClassesString = collectStringMapByValue(classToParentString);

		System.out.println("STEEEEEP 1x4");
		parentToPropertiesString = collectStringMapByValue(propertyToParentString);

	}

	private void initDepthToClass(OWLOntologyManager manager, OWLOntology ont, int depth) {

		boolean areThereLower = true;

		System.out.println("STEEEEEP 1x1x1");
		long startClassDepthTime = System.nanoTime();

		List<OWLClass> classes = reasoner.getSubClasses(df.getOWLThing(), true).entities().collect(Collectors.toList());

		while (areThereLower) {

			// Save this new subclasses as the classes for this depth
			depthToClasses.put(depth, classes);

			List<OWLClass> newClasses = new ArrayList<>();
			for (OWLClass cls : classes) {
				List<OWLClass> subForCls = reasoner.getSubClasses(cls, true).entities().collect(Collectors.toList());
				newClasses.addAll(subForCls);

				// save parent structure
				subForCls.stream().forEach(subCls -> {
					classToParent.put(subCls, cls);
					classToParentString.put(OntologyDescriptor.getCleanNameOWLObj(subCls),
							OntologyDescriptor.getCleanNameOWLObj(cls));
				});

			}

			classes = newClasses;

			depth++;

			if (newClasses.size() < 3) {
				System.out.println(newClasses.toString());
				areThereLower = false;
			}

		}
		long endClassDepthTime = System.nanoTime();
		System.out.println("Graph building took " + (endClassDepthTime - startClassDepthTime) / 1000000 + "ms");
	}

//	private void initDepthToClassNew(OWLOntologyManager manager, OWLOntology ont, int depth) {
//		OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
//		OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ont);
//		OWLDataFactory df = manager.getOWLDataFactory();
//		boolean ontDidChange = true;
//		
//		System.out.println("STEEEEEP 1x1x1");
//		long startClassDepthTime = System.nanoTime();
//		
//		Set<OWLClassExpression> topClassesThisLevel = new HashSet<>();
//		topClassesThisLevel.add(df.getOWLThing());
//
//		while (ontDidChange) {
//
//			ontDidChange = false;
//
//			for (OWLClassExpression expr : topClassesThisLevel)
//			//List<OWLClass> classes = reasoner.getSubClasses(topClassesThisLevel, true).entities()
//					.collect(Collectors.toList());
//
//			// save parent structure
//			for (OWLClass cls : classes) {
//				reasoner.getSubClasses(cls, true).entities().forEach(subCls -> {
//					classToParent.put(subCls, cls);
//					classToParentString.put(OntologyDescriptor.getCleanNameOWLObj(subCls),
//							OntologyDescriptor.getCleanNameOWLObj(cls));
//				});
//			}
//
//			if (!classes.isEmpty()) {
//				OWLEntityRemover remover = new OWLEntityRemover(Collections.singleton(ont));
//				for (OWLClass e : classes) {
//					if (!e.isOWLNothing() && !reasoner.getSubClasses(e).isBottomSingleton()) {
//						ontDidChange = true;
//						e.accept(remover);
//					}
//				}
//				manager.applyChanges(remover.getChanges());
//				remover.reset(); // TODO: Remove if not needed
//
//				depthToClasses.put(depth, classes);
//
//				depth++;
//			}
//		}
//		long endClassDepthTime = System.nanoTime();
//		System.out.println("Graph building took " + (endClassDepthTime - startClassDepthTime)/1000000 + "ms");
//	}

	private void initDepthToRoles(OWLOntologyManager manager, OWLOntology ont, int depth) {
		long startRoleDepthTime = System.nanoTime();

		// save parent role structure
		ont.objectPropertiesInSignature().forEach(property -> {
			if (!property.getInverseProperty().isOWLObjectProperty()) {
				reasoner.getSubObjectProperties(property, true).entities().forEach(subProp -> {
					if (!subProp.getInverseProperty().isOWLObjectProperty()) {
						OWLObjectProperty subPropAsProp = subProp.asOWLObjectProperty();
						OWLObjectProperty propAsProp = property.asOWLObjectProperty();
						propertyToParent.put(subPropAsProp, propAsProp);
						propertyToParentString.put(OntologyDescriptor.getCleanNameOWLObj(subPropAsProp),
								OntologyDescriptor.getCleanNameOWLObj(propAsProp));
					}
				});

			}
		});

		// Get top classes
		Set<OWLObjectPropertyExpression> properties = reasoner
				.getSubObjectProperties(df.getOWLTopObjectProperty(), true).entities().collect(Collectors.toSet());

		boolean isThereLowerLevel = true;
		while (isThereLowerLevel) {
			
			List<OWLObjectProperty> propertiesToSave = new ArrayList<>();
			properties.stream().forEach(e -> {
				if (!e.isOWLBottomObjectProperty()) {
					if (!e.getInverseProperty().isOWLObjectProperty()) {
						propertiesToSave.add(e.asOWLObjectProperty());
					}
				}
			});
			
			depthToProperties.put(depth,propertiesToSave);


			Set<OWLObjectPropertyExpression> nextLevelProperties = new HashSet<>();

			for (OWLObjectPropertyExpression expr : properties) {
				nextLevelProperties.addAll(reasoner
						.getSubObjectProperties(expr, true).entities().collect(Collectors.toSet()));
			}
			

			properties = nextLevelProperties;

			depth++;
			
			if (properties.isEmpty()) {
				isThereLowerLevel = false;
			}
		}

		long endRoleDepthTime = System.nanoTime();
		System.out.println("Graph building took " + (endRoleDepthTime - startRoleDepthTime) / 1000000 + "ms");

	}

	public List<OWLClass> getClassesOfDepth(int depth) {
		return depthToClasses.get(depth);
	}

	public List<OWLObjectProperty> getPropertiesOfDepth(int depth) {
		return depthToProperties.get(depth);
	}

	public int getHighestPropertyDepth() {
		return depthToProperties.size() - 1;
	}

	public int getHighestClassDepth() {
		return depthToClasses.size() - 1;
	}

	public OWLClass getOWLClassParent(OWLClass cls) {
		return classToParent.get(cls);
	}

	public OWLObjectProperty getOWLPropertyParent(OWLObjectProperty cls) {
		return propertyToParent.get(cls);
	}

	public static Map<String, Set<String>> collectStringMapByValue(Map<String, String> aToB) {
		Map<String, Set<String>> bToAList = new HashMap<>();

		for (Entry<String, String> entry : aToB.entrySet()) {
			// Add node to corresponding Group
			String b = entry.getValue();
			if (!bToAList.containsKey(b)) {
				bToAList.put(b, new HashSet<>());
			}
			bToAList.get(b).add(entry.getKey());
		}

		return bToAList;
	}

	public Map<String, String> getClassToParentString() {
		return classToParentString;
	}

	public Map<String, String> getPropertyToParentString() {
		return propertyToParentString;
	}

	public Map<String, Set<String>> getParentToClassesString() {
		return parentToClassesString;
	}

	public Map<String, Set<String>> getParentToPropertiesString() {
		return parentToPropertiesString;
	}

}
