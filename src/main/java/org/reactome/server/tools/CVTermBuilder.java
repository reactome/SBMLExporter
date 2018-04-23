package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.*;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;
import org.reactome.server.graph.domain.model.CandidateSet;

import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class CVTermBuilder extends AnnotationBuilder {

    private String thisPath;

    CVTermBuilder(SBase sbase, String pathref) {
        super(sbase);
        thisPath = pathref;
    }

    /**
     * Adds the resources for a model. This uses BQB_IS to link to the Reactome entry
     * and BQB_IS_DESCRIBED_BY to link to any relevant publications.
     *
     * @param path  Pathway instance from ReactomeDB
     */
    void createModelAnnotations(Pathway path) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, path.getStId());
        addGOTerm(path);
        addPublications(path.getLiteratureReference());
        addDiseaseReference(path.getDisease());
        addCrossReferences(path.getCrossReference());
        createCVTerms();
    }

    void createModelAnnotations(List<Event> listOfEvents) {
        for (Event e : listOfEvents) {
            addPublications(e.getLiteratureReference());
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
    void createReactionAnnotations(org.reactome.server.graph.domain.model.ReactionLikeEvent event) {
        addResource("reactome", CVTerm.Qualifier.BQB_IS, event.getStId());
        addGOTerm(event);
        addECNumber(event);
        addPublications(event.getLiteratureReference());
        addDiseaseReference(event.getDisease());
        createCVTerms();
    }

    /**
     * Adds the resources for a SBML species. This uses BQB_IS to link to the Reactome entry
     * and then calls createPhysicalEntityAnnotations to deal with the particular type.
     *
     * @param pe  PhysicalEntity from ReactomeDB
     */
    void createSpeciesAnnotations(PhysicalEntity pe){
        addResource("reactome", CVTerm.Qualifier.BQB_IS, pe.getStId());
        createPhysicalEntityAnnotations(pe, CVTerm.Qualifier.BQB_IS, true);
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

    private void addPublications(List<Publication> publications) {
        if (publications == null || publications.size() == 0) {
            return;
        }
        for (Publication pub : publications) {
            if (pub instanceof LiteratureReference) {
                Integer pubmed = ((LiteratureReference) pub).getPubMedIdentifier();
                if (pubmed != null) {
                    addResource("pubmed", CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, pubmed.toString());
                }
            }
        }

    }


    /**
     * Function to determine GO terms associated with the event
     *
     * @param event ReactionLikeEvent from ReactomeDB
     */
    private void addGOTerm(org.reactome.server.graph.domain.model.ReactionLikeEvent event){
        if (event.getGoBiologicalProcess() != null) {
            addResource("go", CVTerm.Qualifier.BQB_IS, event.getGoBiologicalProcess().getAccession());
        }
        else if (event.getCatalystActivity() != null && event.getCatalystActivity().size() > 0) {
            CatalystActivity cat = event.getCatalystActivity().get(0);
            GO_MolecularFunction goterm = cat.getActivity();
            if (goterm != null){
                addResource("go", CVTerm.Qualifier.BQB_IS, cat.getActivity().getAccession());
            }
        }
    }

    /**
     * Function to determine GO terms associated with the pathway
     *
     * @param path Pathway from ReactomeDB
     */
    private void addGOTerm(org.reactome.server.graph.domain.model.Pathway path){
        if (path.getGoBiologicalProcess() != null) {
            addResource("go", CVTerm.Qualifier.BQB_IS, path.getGoBiologicalProcess().getAccession());
        }
    }

    private void addECNumber(org.reactome.server.graph.domain.model.ReactionLikeEvent event) {
        if (event.getCatalystActivity() != null && event.getCatalystActivity().size() > 0) {
            for (CatalystActivity cat : event.getCatalystActivity()) {
                String ecnum = cat.getActivity().getEcNumber();
                if (ecnum != null) {
                    addResource("ec-code", CVTerm.Qualifier.BQB_IS, ecnum);
                }
            }
        }
    }

    private void addDiseaseReference(List<Disease> diseases){
        if (diseases != null) {
            for (Disease disease: diseases){
                addResource(disease.getDatabaseName(), CVTerm.Qualifier.BQB_OCCURS_IN, disease.getIdentifier());
            }
        }
    }

    private void addCrossReferences(List<DatabaseIdentifier> xrefs){
        if (xrefs != null) {
            for (DatabaseIdentifier xref: xrefs){
                addResource(xref.getDatabaseName(), CVTerm.Qualifier.BQM_HAS_INSTANCE, xref.getIdentifier());
            }
        }

    }

    private void createSimpleEntityAnnotations(SimpleEntity se, CVTerm.Qualifier qualifier){
        if (se.getReferenceEntity() != null) {
            addResource("chebi", qualifier, (se.getReferenceEntity().getIdentifier()));
        }
        String ref = getKeggReference(se.getCrossReference());
        if (ref.length() > 0){
            addResource("kegg", qualifier, ref);
        }
        ref = null;
    }
    /**
     * Adds the resources relating to different types of PhysicalEntity. In the case of a Complex
     * it will iterate through all the components.
     *
     * @param pe            PhysicalEntity from ReactomeDB
     * @param qualifier     The MIRIAM qualifier for the reference
     */
    private void createPhysicalEntityAnnotations(PhysicalEntity pe, CVTerm.Qualifier qualifier, boolean recurse){
        if (pe instanceof SimpleEntity){
            createSimpleEntityAnnotations(((SimpleEntity)(pe)), qualifier);
        }
        else if (pe instanceof EntityWithAccessionedSequence){
            ReferenceEntity ref = ((EntityWithAccessionedSequence)(pe)).getReferenceEntity();
            if (ref != null) {
                addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier());
            }
            ref = null;
            if (recurse) {
                List<PhysicalEntity> inferences = pe.getInferredTo();
                if (inferences != null) {
                    for (PhysicalEntity inf : inferences) {
                        addResource("reactome", CVTerm.Qualifier.BQB_IS_HOMOLOG_TO, inf.getStId());
                        // could add nested annotation but decided not to at present
                    }
                }
                inferences = null;
                inferences = pe.getInferredFrom();
                if (inferences != null) {
                    for (PhysicalEntity inf : inferences) {
                        addResource("reactome", CVTerm.Qualifier.BQB_IS_HOMOLOG_TO, inf.getStId());
                        // could add nested annotation but decided not to at present
                    }
                }
                inferences = null;
                List<AbstractModifiedResidue> mods = ((EntityWithAccessionedSequence) pe).getHasModifiedResidue();
                if (mods != null) {
                    for (AbstractModifiedResidue inf : mods) {
                        if ((inf instanceof TranslationalModification) && ((TranslationalModification)(inf)).getPsiMod() != null){
                            PsiMod psi = ((TranslationalModification)(inf)).getPsiMod();
                            addResource(psi.getDatabaseName(), CVTerm.Qualifier.BQB_HAS_VERSION, psi.getIdentifier());
                        }
                    }
                }
                mods = null;
            }
        }
        else if (pe instanceof Complex){
            List<PhysicalEntity> components = ((Complex)(pe)).getHasComponent();
            if (components != null) {
                for (PhysicalEntity component : components) {
                    createPhysicalEntityAnnotations(component, CVTerm.Qualifier.BQB_HAS_PART, false);
                }
            }
            components = null;
        }
        else if (pe instanceof EntitySet){
            List<PhysicalEntity> members = ((EntitySet)(pe)).getHasMember();
            if (members != null) {
                for (PhysicalEntity member : members) {
                    createPhysicalEntityAnnotations(member, CVTerm.Qualifier.BQB_HAS_PART, false);
                }
            }
            members = null;
        }
        else if (pe instanceof Polymer){
            List<PhysicalEntity> repeated = ((Polymer) pe).getRepeatedUnit();
            if (repeated != null) {
                for (PhysicalEntity component : repeated) {
                    createPhysicalEntityAnnotations(component, CVTerm.Qualifier.BQB_HAS_PART, false);
                }
            }
            repeated = null;
        }
        else if (pe instanceof ChemicalDrug){
            ReferenceEntity ref = ((ChemicalDrug)(pe)).getReferenceEntity();
            if (ref != null) {
                addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier());
            }
            ref = null;
        }
        else if (pe instanceof ProteinDrug){
            ReferenceEntity ref = ((ProteinDrug)(pe)).getReferenceEntity();
            if (ref != null) {
                addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier());
            }
            ref = null;
        }
        else if (pe instanceof RNADrug){
            ReferenceEntity ref = ((RNADrug)(pe)).getReferenceEntity();
            if (ref != null) {
                addResource(ref.getDatabaseName(), qualifier, ref.getIdentifier());
            }
            ref = null;
        }
        else if (pe instanceof GenomeEncodedEntity) {
            // no additional annotation
            System.out.println("Found GEE in path " + thisPath);
        }
        else if (pe instanceof OtherEntity) {
            // no additional annotation
            System.out.println("Found OE in path " + thisPath);
        }
        else {
            // FIX_Unknown_Physical_Entity
            // here we have encountered a physical entity type that did not exist in the graph database
            // when this code was written (April 2018)
            System.err.println("Function CVTermBuilder::createPhysicalEntityAnnotations " +
                            "Encountered unknown PhysicalEntity " + pe.getStId());

        }
    }

    /**
     * Find the KEGG compound reference
     *
     * @param references List<DatabaseIdentifiers> from ReactomeDB that might contain a kegg
     *
     * @return the KEGG reference identifier or empty string if none present
     */
    private String getKeggReference(List<DatabaseIdentifier> references){
        if (references != null) {
            for (DatabaseIdentifier ref : references) {
                if (ref.getDatabaseName().equals("COMPOUND")) {
                    return ref.getIdentifier();
                }
            }
        }
        return "";
    }
}
