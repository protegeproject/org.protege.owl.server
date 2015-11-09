package org.protege.owl.server.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.protege.owl.server.api.client.RemoteOntologyDocument;
import org.protege.owl.server.api.client.VersionedOntologyDocument;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;

public interface DocumentFactory {
    
    ChangeHistory createEmptyChangeDocument(OntologyDocumentRevision revision);
	
	/**
	 * Create a change document for the list of changes and the commit comments.
	 *
	 * @param changes	changes
	 * @param metaData	metaData
	 * @param start	start
	 * @return SingletonChangeHistory
	 */
	SingletonChangeHistory createChangeDocument(List<OWLOntologyChange> changes, ChangeMetaData metaData, OntologyDocumentRevision start);
	
	ChangeHistory readChangeDocument(InputStream in, OntologyDocumentRevision start, OntologyDocumentRevision end) throws IOException;
	
	boolean hasServerMetadata(OWLOntology ontology);
	
	IRI getServerLocation(OWLOntology ontology) throws IOException;
	
	boolean hasServerMetadata(IRI ontologyDocumentLocation);
	
	IRI getServerLocation(IRI ontologyDocumentLocation) throws IOException;

	VersionedOntologyDocument getVersionedOntologyDocument(OWLOntology ontology) throws IOException;
	
	VersionedOntologyDocument createVersionedOntology(OWLOntology ontology, RemoteOntologyDocument serverDocument, OntologyDocumentRevision revision);
	
	OWLObjectRenderer getOWLRenderer();

}
