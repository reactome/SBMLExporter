package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class CVTermBuilder extends AnnotationBuilder {

    CVTermBuilder(SBase sbase) {

        super(sbase);
    }

    /**
     * Adds the resources for a model. This uses BQB_IS to link to the Reactome entry
     * and BQB_IS_DESCRIBED_BY to link to any relevant publications.
     *
     * @param path  Pathway instance from ReactomeDB
     */
    void createModelAnnotations(Pathway path) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, path.getStId());
        if (path.getLiteratureReference() != null) {
            for (Publication pub : path.getLiteratureReference()) {
                if (pub instanceof LiteratureReference) {
                    addResource("pubmed", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, ((LiteratureReference) (pub)).getPubMedIdentifier().toString());
                }
            }
        }
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML reaction. This uses BQB_IS to link to the Reactome entry;
     * BQB_IS to link to any GO biological processes
     * and BQB_IS_DESCRIBED_BY to link to any relevant publications.
     *
     * @param event   Event instance from ReactomeDB
     */
    void createReactionAnnotations(org.reactome.server.graph.domain.model.Reaction event) {
//        TODO is this type appropriate
        addResource("reactome", CVTerm.Qualifier.BQB_IS, event.getStId());
        if (event.getGoBiologicalProcess() != null) {
            addResource("go", CVTerm.Qualifier.BQB_IS, event.getGoBiologicalProcess().getAccession());
        }
        if (event.getLiteratureReference() != null) {
            for (Publication pub : event.getLiteratureReference()) {
                if (pub instanceof LiteratureReference) {
                    addResource("pubmed", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, ((LiteratureReference) (pub)).getPubMedIdentifier().toString());
                }
            }
        }
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML species. This uses BQB_IS to link to the Reactome entry
     * and then calls createPhysicalEntityAnnotations to deal with the particlar type.
     *
     * @param pe  PhysicalEntity from ReactomeDB
     */
    void createSpeciesAnnotations(PhysicalEntity pe){
        addResource("reactome", CVTerm.Qualifier.BQB_IS, pe.getStId());
        createPhysicalEntityAnnotations(pe, CVTerm.Qualifier.BQB_IS);
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML compartment. This uses BQB_IS to link to the Reactome entry.
     *
     * @param comp  Compartment from ReactomeDB
     */
    void createCompartmentAnnotations(org.reactome.server.graph.domain.model.Compartment comp){
        addResource("go", CVTerm.Qualifier.BQB_IS, comp.getAccession());
        createCVTerms();
    }

    /**
     * Adds the resources relating to different types of PhysicalEntity. In the case of a Complex
     * it will iterate through all the components.
     *
     * @param pe            PhysicalEntity from ReactomeDB
     * @param qualifier     The MIRIAM qualifier for the reference
     */
    private void createPhysicalEntityAnnotations(PhysicalEntity pe, CVTerm.Qualifier qualifier){
        // TODO make sure all physicalentity types are covered
        if (pe instanceof SimpleEntity){
            addResource("chebi", qualifier, ((SimpleEntity)(pe)).getReferenceEntity().getIdentifier());
        }
        else if (pe instanceof EntityWithAccessionedSequence){
            ReferenceEntity ref = ((EntityWithAccessionedSequence)(pe)).getReferenceEntity();
            addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier());
        }
        else if (pe instanceof Complex){
            for (PhysicalEntity component : ((Complex)(pe)).getHasComponent()){
                createPhysicalEntityAnnotations(component, CVTerm.Qualifier.BQB_HAS_PART);
            }
        }
        else {
            if (!(pe instanceof OtherEntity)) {
                addResource("TODO", qualifier, "class not dealt with");
            }
        }
    }
}
