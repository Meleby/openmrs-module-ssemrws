package org.openmrs.module.ssemrws.constants;

import org.apache.commons.lang3.time.DateUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.openmrs.*;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ssemrws.web.constants.GeneratePatientListObject;
import org.openmrs.module.ssemrws.web.constants.GenerateSummary;
import org.openmrs.module.ssemrws.web.constants.GenerateSummaryResponse;
import org.openmrs.module.ssemrws.web.controller.SSEMRWebServicesController;
import org.openmrs.module.ssemrws.web.dto.PatientObservations;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.openmrs.module.ssemrws.constants.GetDateObservations.*;
import static org.openmrs.module.ssemrws.constants.GetObservationValue.*;
import static org.openmrs.module.ssemrws.web.constants.AllConcepts.*;
import static org.openmrs.module.ssemrws.web.constants.GenerateSummary.generateSummary;
import static org.openmrs.module.ssemrws.web.constants.PatientDataUtils.*;
import static org.openmrs.module.ssemrws.web.constants.RegimenConcepts.*;

@Component
public class SharedConstants {
	
	public static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy");
	
	public static final double THRESHOLD = 1000.0;
	
	private static GenerateSummaryResponse generateSummaryResponse = null;
	
	private static GeneratePatientListObject generatePatientListObj;
	
	public SharedConstants(GenerateSummaryResponse generateSummaryResponse,
	    GeneratePatientListObject generatePatientListObj) {
		SharedConstants.generateSummaryResponse = generateSummaryResponse;
		SharedConstants.generatePatientListObj = generatePatientListObj;
	}
	
	public static Date[] getStartAndEndDate(String qStartDate, String qEndDate, SimpleDateFormat dateTimeFormatter)
	        throws ParseException {
		Date endDate = (qEndDate != null) ? dateTimeFormatter.parse(qEndDate) : new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(endDate);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date startDate = (qStartDate != null) ? dateTimeFormatter.parse(qStartDate) : calendar.getTime();
		
		return new Date[] { startDate, endDate };
	}
	
	// Retrieves a list of encounters filtered by encounter types.
	public static List<Encounter> getEncountersByEncounterTypes(List<String> encounterTypeUuids, Date startDate,
	        Date endDate) {
		List<EncounterType> encounterTypes = encounterTypeUuids.stream()
		        .map(uuid -> Context.getEncounterService().getEncounterTypeByUuid(uuid)).collect(Collectors.toList());
		
		EncounterSearchCriteria encounterCriteria = new EncounterSearchCriteria(null, null, startDate, endDate, null, null,
		        encounterTypes, null, null, null, false);
		return Context.getEncounterService().getEncounters(encounterCriteria);
	}
	
	/**
	 * Retrieves a list of concepts based on their UUIDs.
	 * 
	 * @param conceptUuids A list of UUIDs of concepts to retrieve.
	 * @return A list of concepts corresponding to the given UUIDs.
	 */
	public static List<Concept> getConceptsByUuids(List<String> conceptUuids) {
		return conceptUuids.stream().map(uuid -> Context.getConceptService().getConceptByUuid(uuid))
		        .collect(Collectors.toList());
	}
	
	public static String getARTRegimen(Patient patient) {
		List<Obs> artRegimenObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(ACTIVE_REGIMEN_CONCEPT_UUID)), null,
		    null, null, null, 0, null, null, null, false);
		
		for (Obs obs : artRegimenObs) {
			if (obs.getValueCoded() != null) {
				return obs.getValueCoded().getName().getName();
			}
		}
		return "";
	}
	
	// Determine Patient Enrollment Date From the Adult and Adolescent and Pediatric
	// Forms
	public static String getEnrolmentDate(Patient patient) {
		List<Obs> enrollmentDateObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(DATE_OF_ENROLLMENT_UUID)), null,
		    null, null, null, 0, null, null, null, false);
		
		if (!enrollmentDateObs.isEmpty()) {
			Obs dateObs = enrollmentDateObs.get(0);
			Date enrollmentDate = dateObs.getValueDate();
			if (enrollmentDate != null) {
				return dateTimeFormatter.format(enrollmentDate);
			}
		}
		return "";
		
	}
	
	// Retrieve the Last Refill Date from Patient Observation
	public static String getLastRefillDate(Patient patient) {
		List<Obs> lastRefillDateObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(LAST_REFILL_DATE_UUID)), null, null,
		    null, null, 0, null, null, null, false);
		
		if (!lastRefillDateObs.isEmpty()) {
			Obs lastObs = lastRefillDateObs.get(0);
			Date lastRefillDate = lastObs.getValueDate();
			if (lastRefillDate != null) {
				return dateTimeFormatter.format(lastRefillDate);
			}
		}
		return "";
	}
	
	public static boolean determineIfPatientIsPregnantOrBreastfeeding(Patient patient, Date endDate) {
		
		List<Concept> pregnantAndBreastfeedingConcepts = new ArrayList<>();
		pregnantAndBreastfeedingConcepts
		        .add(Context.getConceptService().getConceptByUuid(CURRENTLY_BREASTFEEDING_CONCEPT_UUID));
		pregnantAndBreastfeedingConcepts.add(Context.getConceptService().getConceptByUuid(CURRENTLY_PREGNANT_CONCEPT_UUID));
		
		List<Obs> obsList = Context.getObsService().getObservations(Collections.singletonList(patient), null,
		    pregnantAndBreastfeedingConcepts,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(CONCEPT_BY_UUID)), null, null, null, null,
		    null, null, endDate, false);
		
		return !obsList.isEmpty();
	}
	
	public static Map<String, Object> createResultMap(String key, int value) {
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put(key, value);
		return resultMap;
	}
	
	public static ResponseEntity<Object> buildErrorResponse(String message, HttpStatus status) {
		return new ResponseEntity<>(message, new HttpHeaders(), status);
	}
	
	public static List<Map<String, String>> getIdentifiersList(Patient patient) {
		List<Map<String, String>> identifiersList = new ArrayList<>();
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			Map<String, String> identifierObj = new HashMap<>();
			identifierObj.put("identifier", identifier.getIdentifier().trim());
			identifierObj.put("identifierType", identifier.getIdentifierType().getName().trim());
			identifiersList.add(identifierObj);
		}
		return identifiersList;
	}
	
	public static ArrayNode getPatientIdentifiersArray(Patient patient) {
		ArrayNode identifiersArray = JsonNodeFactory.instance.arrayNode();
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			ObjectNode identifierObj = JsonNodeFactory.instance.objectNode();
			identifierObj.put("identifier", identifier.getIdentifier());
			identifierObj.put("identifierType", identifier.getIdentifierType().getName());
			identifiersArray.add(identifierObj);
		}
		return identifiersArray;
	}
	
	public static String getPatientFullAddress(Patient patient) {
		String village = "";
		String landmark = "";
		for (PersonAddress address : patient.getAddresses()) {
			if (address.getAddress5() != null) {
				village = address.getAddress5();
			}
			if (address.getAddress6() != null) {
				landmark = address.getAddress6();
			}
		}
		return "Village: " + village + ", Landmark: " + landmark;
	}
	
	public static String formatBirthdate(Date birthdate) {
		return dateTimeFormatter.format(birthdate);
	}
	
	public static long calculateAge(Date birthdate) {
		Date currentDate = new Date();
		return (currentDate.getTime() - birthdate.getTime()) / (1000L * 60 * 60 * 24 * 365);
	}
	
	public static Double getPatientAge(Patient patient) {
		LocalDate birthdate = patient.getBirthdate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate currentDate = LocalDate.now();
		
		return (double) Period.between(birthdate, currentDate).getYears();
	}
	
	public static Map<String, Object> buildResponseMap(Patient patient, long age, String birthdate,
	        List<Map<String, String>> identifiersList, PatientObservations observations) {
		Map<String, Object> responseMap = new LinkedHashMap<>();
		responseMap.put("Name", patient.getPersonName() != null ? patient.getPersonName().toString() : "");
		responseMap.put("uuid", patient.getUuid());
		responseMap.put("age", age);
		responseMap.put("birthdate", birthdate);
		responseMap.put("sex", patient.getGender());
		responseMap.put("Identifiers", identifiersList);
		responseMap.put("results", Collections.singletonList(observations));
		
		return responseMap;
	}
	
	public static Double getLastCD4Count(Patient patient) {
		Concept lastCD4CountConcept = Context.getConceptService().getConceptByUuid(LAST_CD4_COUNT_UUID);
		List<Obs> lastCD4CountObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(lastCD4CountConcept), null, null, null, null, null, null, null, null, false);
		
		if (!lastCD4CountObs.isEmpty()) {
			Obs lastcd4Obs = lastCD4CountObs.get(0);
			return lastcd4Obs.getValueNumeric();
		}
		return null;
	}
	
	public static String getCD4Done(Patient patient) {
		Concept cd4Concept = Context.getConceptService().getConceptByUuid(CD4_DONE_UUID);
		List<Obs> cd4Obs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(cd4Concept), null, null, null, null, null, null, null, null, false);
		
		if (!cd4Obs.isEmpty()) {
			Obs lastcd4Obs = cd4Obs.get(0);
			return lastcd4Obs.getValueCoded().getName().getName();
		}
		return null;
	}
	
	public static String getIsPatientOnTb(Patient patient) {
		Concept tbConcept = Context.getConceptService().getConceptByUuid(ON_TB);
		List<Obs> tbObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(tbConcept), null, null, null, null, null, null, null, null, false);
		
		if (!tbObs.isEmpty()) {
			Obs lastTbObs = tbObs.get(0);
			return lastTbObs.getValueCoded().getName().getName();
		}
		return null;
	}
	
	public static String getTbStatus(Patient patient) {
		Concept tbStatusConcepts = Context.getConceptService().getConceptByUuid(TB_STATUS_CONCEPT_UUID);
		
		List<Obs> tbStatusObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(tbStatusConcepts), null, null, null, null, null, null, null, null, false);
		
		if (!tbStatusObs.isEmpty()) {
			Obs tbStatus = tbStatusObs.get(0);
			return tbStatus.getValueCoded().getName().getName();
		}
		return "";
	}
	
	public static String getARVRegimenDose(Patient patient) {
		Concept arvRegimenDoseConcept = Context.getConceptService().getConceptByUuid(ARV_REGIMEN_DOSE_UUID);
		
		List<Obs> arvRegimenDoseObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(arvRegimenDoseConcept), null, null, null, null, null, null, null, null, false);
		
		if (arvRegimenDoseObs.isEmpty()) {
			System.err.println("No observations found for the concept " + arvRegimenDoseConcept.getName().getName() + ".");
			return "";
		}
		
		Obs arvRegimenDose = arvRegimenDoseObs.get(0);
		
		if (arvRegimenDose.getValueText() != null) {
			return arvRegimenDose.getValueText();
		} else if (arvRegimenDose.getValueCoded() != null) {
			return arvRegimenDose.getValueCoded().getName().getName();
		} else {
			return "";
		}
	}
	
	public static String getWHOClinicalStage(Patient patient) {
		Concept whoClinicalConcept = Context.getConceptService().getConceptByUuid(WHO_CLINICAL_UUID);
		Concept whoClinicalStageIntakeConcept = Context.getConceptService().getConceptByUuid(WHO_CLINICAL_STAGE_INTAKE_UUID);
		
		List<Concept> whoConcepts = Arrays.asList(whoClinicalConcept, whoClinicalStageIntakeConcept);
		
		List<Obs> obsList = Context.getObsService().getObservations(Collections.singletonList(patient), null, whoConcepts,
		    null, null, null, null, null, null, null, null, false);
		
		if (obsList.isEmpty()) {
			return "";
		}
		
		Obs whoClinicalStageObs = obsList.get(0);
		
		if (whoClinicalStageObs.getValueText() != null) {
			return whoClinicalStageObs.getValueText();
		} else if (whoClinicalStageObs.getValueCoded() != null) {
			return whoClinicalStageObs.getValueCoded().getName().getName();
		} else {
			return "";
		}
	}
	
	public static String getDateVLResultsReceived(Patient patient) {
		return getLatestDateForConcepts(patient, DATE_VL_RESULTS_RECEIVED_UUID, REAPEAT_VL_COLLECTION);
	}
	
	public static String getLatestVLSampleCollectionDate(Patient patient) {
		return getLatestDateForConcepts(patient, SAMPLE_COLLECTION_DATE_UUID, REPEAT_VL_COLLECTION_DATE);
	}
	
	public static String getVLResults(Patient patient) {
		Concept viralLoadResultsConcept = Context.getConceptService().getConceptByUuid(VIRAL_LOAD_RESULTS_UUID);
		Concept bdlConcept = Context.getConceptService().getConceptByUuid(BDL_CONCEPT_UUID);
		Concept viralLoadConcept = Context.getConceptService().getConceptByUuid(VIRAL_LOAD_CONCEPT_UUID);
		
		List<Obs> getVLResultNumericObs = Context.getObsService().getObservations(
		    Collections.singletonList(patient.getPerson()), null, Collections.singletonList(viralLoadConcept), null, null,
		    null, null, 1, null, null, null, false);
		
		List<Obs> getVLResultObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(viralLoadResultsConcept), null, null, null, null, 1, null, null, null, false);
		
		List<Obs> allObservations = new ArrayList<>();
		allObservations.addAll(getVLResultNumericObs);
		allObservations.addAll(getVLResultObs);
		
		allObservations.sort((o1, o2) -> o2.getObsDatetime().compareTo(o1.getObsDatetime()));
		
		if (!allObservations.isEmpty()) {
			Obs mostRecentObs = allObservations.get(0);
			
			if (mostRecentObs.getValueNumeric() != null) {
				return mostRecentObs.getValueNumeric().toString();
			} else if (mostRecentObs.getValueText() != null) {
				return mostRecentObs.getValueText();
			} else if (mostRecentObs.getValueCoded() != null) {
				if (mostRecentObs.getValueCoded().equals(bdlConcept)) {
					return "Below Detectable (BDL)";
				} else {
					return mostRecentObs.getValueCoded().getName().getName();
				}
			} else {
				System.err.println("Observation value is neither numeric, text, nor coded.");
			}
		}
		return null;
	}
	
	/**
	 * Gets the latest VL Result from the Follow-Up Form.
	 */
	public static String getVLResultsFromFollowUpForm(Patient patient) {
		return getLatestVlValueForEncounterType(patient, FOLLOW_UP_FORM_ENCOUNTER_TYPE);
	}
	
	/**
	 * This is the new private helper method that contains all the reusable logic. It fetches the most
	 * recent Viral Load value for a patient from within encounters of a specific type.
	 * 
	 * @param patient The patient to check.
	 * @param encounterTypeUuid The UUID of the EncounterType to search within.
	 * @return The formatted String of the latest VL value, or null if none is found.
	 */
	private static String getLatestVlValueForEncounterType(Patient patient, String encounterTypeUuid) {
		EncounterType encounterType = Context.getEncounterService().getEncounterTypeByUuid(encounterTypeUuid);
		if (encounterType == null) {
			System.err.println("Could not find EncounterType with UUID: " + encounterTypeUuid);
			return null;
		}
		
		EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(patient, null, null, null, null, null,
		        Collections.singletonList(encounterType), null, null, null, false);
		List<Encounter> encounters = Context.getEncounterService().getEncounters(encounterSearchCriteria);
		
		if (encounters.isEmpty()) {
			return null;
		}
		
		Concept viralLoadResultsConcept = Context.getConceptService().getConceptByUuid(VIRAL_LOAD_RESULTS_UUID);
		Concept bdlConcept = Context.getConceptService().getConceptByUuid(BDL_CONCEPT_UUID);
		Concept viralLoadConcept = Context.getConceptService().getConceptByUuid(VIRAL_LOAD_CONCEPT_UUID);
		
		List<Obs> getVLResultNumericObs = Context.getObsService().getObservations(
		    Collections.singletonList(patient.getPerson()), encounters, Collections.singletonList(viralLoadConcept), null,
		    null, null, null, null, null, null, null, false);
		
		List<Obs> getVLResultObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    encounters, Collections.singletonList(viralLoadResultsConcept), null, null, null, null, null, null, null, null,
		    false);
		
		List<Obs> allObservations = new ArrayList<>();
		allObservations.addAll(getVLResultNumericObs);
		allObservations.addAll(getVLResultObs);
		
		if (!allObservations.isEmpty()) {
			Obs mostRecentObs = allObservations.get(0);
			
			if (mostRecentObs.getValueNumeric() != null) {
				return mostRecentObs.getValueNumeric().toString();
			} else if (mostRecentObs.getValueText() != null) {
				return mostRecentObs.getValueText();
			} else if (mostRecentObs.getValueCoded() != null) {
				if (mostRecentObs.getValueCoded().equals(bdlConcept)) {
					return "Below Detectable (BDL)";
				} else {
					return mostRecentObs.getValueCoded().getName().getName();
				}
			} else {
				System.err.println("Observation value is neither numeric, text, nor coded.");
			}
		}
		return null;
	}
	
	public static String getVLStatus(Patient patient) {
		String vlResult = getVLResults(patient);
		
		if (vlResult == null) {
			System.err.println("VL result is null for patient: " + patient);
			return "Unknown";
		}
		
		try {
			double vlValue = Double.parseDouble(vlResult);
			
			if (vlValue >= 1000) {
				return "Unsuppressed";
			} else {
				return "Suppressed";
			}
		}
		catch (NumberFormatException e) {
			if ("Below Detectable (BDL)".equalsIgnoreCase(vlResult)) {
				return "Suppressed";
			} else {
				System.err.println("Error parsing VL result or unrecognized value: " + vlResult);
				return "Unknown";
			}
		}
	}
	
	public static Double getBMI(Patient patient) {
		List<Obs> bmiObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(BMI_CONCEPT_UUID)), null, null, null,
		    null, 1, null, null, null, false);
		
		if (!bmiObs.isEmpty()) {
			Obs bmiObservation = bmiObs.get(0);
			return bmiObservation.getValueNumeric();
		}
		
		return null;
	}
	
	public static Double getMUAC(Patient patient) {
		List<Obs> muacObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(MUAC_CONCEPT_UUID)), null, null, null,
		    null, 1, null, null, null, false);
		
		if (!muacObs.isEmpty()) {
			Obs muacObservation = muacObs.get(0);
			return muacObservation.getValueNumeric();
		}
		
		return null;
	}
	
	public static String getClinicianName(Patient patient) {
		List<Obs> clinicianObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(CLINICIAN_CONCEPT_UUID)), null,
		    null, null, null, 1, null, null, null, false);
		
		if (!clinicianObs.isEmpty()) {
			Obs clinicianObservation = clinicianObs.get(0);
			return clinicianObservation.getValueText();
		}
		
		return "";
	}
	
	public static String getLastVisitDate(Patient patient) {
		VisitService visitService = Context.getVisitService();
		List<Visit> visits = visitService.getVisitsByPatient(patient);
		
		if (!visits.isEmpty()) {
			// get the latest visit date
			visits.sort((v1, v2) -> v2.getStartDatetime().compareTo(v1.getStartDatetime()));
			
			Date lastVisitDate = visits.get(0).getStartDatetime();
			
			if (lastVisitDate != null) {
				return dateTimeFormatter.format(lastVisitDate);
			}
		}
		return "";
	}
	
	public static String getTbNumber(Patient patient) {
		List<Obs> tbObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(TB_UNIT_NUMBER)), null, null, null, null,
		    1, null, null, null, false);
		
		if (!tbObs.isEmpty()) {
			Obs tbObservation = tbObs.get(0);
			return tbObservation.getValueText();
		}
		
		return "";
	}
	
	public enum Flags {
		MISSED_APPOINTMENT,
		IIT,
		DIED,
		TRANSFERRED_OUT,
		DUE_FOR_VL,
		ACTIVE,
		HIGH_VL,
		RTT,
		NEW_CLIENT,
		PMTCT
	}
	
	public static HashSet<Patient> getTransferredOutClients(Date startDate, Date endDate) {
		Concept transferredOutConcept = ConceptCache.getCachedConcept(TRANSFERRED_OUT_CONCEPT_UUID);
		Concept yesConcept = ConceptCache.getCachedConcept(YES_CONCEPT);
		
		List<Obs> transferredOutPatientsObs = Context.getObsService().getObservations(null, null,
		    Collections.singletonList(transferredOutConcept), Collections.singletonList(yesConcept), null, null, null, null,
		    null, null, endDate, false);
		
		HashSet<Patient> transferredOutPatients = new HashSet<>();
		
		for (Obs obs : transferredOutPatientsObs) {
			Person person = obs.getPerson();
			if (person != null) {
				Patient patient = Context.getPatientService().getPatient(person.getPersonId());
				if (patient != null) {
					transferredOutPatients.add(patient);
				}
			}
		}
		
		HashSet<Patient> deceasedPatients = getDeceasedPatientsByDateRange(startDate, endDate);
		transferredOutPatients.removeAll(deceasedPatients);
		
		return transferredOutPatients;
	}
	
	public static HashSet<Patient> getDeceasedPatientsByDateRange(Date startDate, Date endDate) {
		Concept deceasedConcept = ConceptCache.getCachedConcept(DECEASED_CONCEPT_UUID);
		Concept yesConcept = ConceptCache.getCachedConcept(YES_CONCEPT);
		
		List<Obs> deceasedPatientsObs = Context.getObsService().getObservations(null, null,
		    Collections.singletonList(deceasedConcept), Collections.singletonList(yesConcept), null, null, null, null, null,
		    null, endDate, false);
		
		HashSet<Patient> deadPatients = new HashSet<>();
		
		for (Obs obs : deceasedPatientsObs) {
			Person person = obs.getPerson();
			if (person != null) {
				Patient patient = Context.getPatientService().getPatient(person.getPersonId());
				if (patient != null) {
					deadPatients.add(patient);
				}
			}
		}
		
		return deadPatients;
	}
	
	public static HashSet<Patient> getTransferredInPatients(Date startDate, Date endDate) {
		PatientService patientService = Context.getPatientService();
		List<Patient> allPatients = patientService.getAllPatients();
		
		return allPatients.stream()
		        .filter(patient -> patient.getIdentifiers().stream()
		                .anyMatch(identifier -> identifier.getIdentifier().startsWith("TI-")))
		        .collect(Collectors.toCollection(HashSet::new));
	}
	
	public static HashSet<Patient> getReturnToTreatmentPatients(Date startDate, Date endDate) {
		List<String> returnedToTreatmentencounterTypeUuids = Collections
		        .singletonList(ART_TREATMENT_INTURRUPTION_ENCOUNTER_TYPE_UUID);
		
		List<Encounter> returnedToTreatmentEncounters = getEncountersByEncounterTypes(returnedToTreatmentencounterTypeUuids,
		    startDate, endDate);
		
		List<Obs> returnedToTreatmentObs = Context.getObsService().getObservations(null, returnedToTreatmentEncounters,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(RETURNING_TO_TREATMENT_UUID)),
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(CONCEPT_BY_UUID)), null, null, null, null,
		    null, startDate, endDate, false);
		
		HashSet<Patient> returnToTreatmentPatients = new HashSet<>();
		
		for (Obs obs : returnedToTreatmentObs) {
			Patient patient = (Patient) obs.getPerson();
			returnToTreatmentPatients.add(patient);
		}
		
		return returnToTreatmentPatients;
	}
	
	// Get all patients who have high Viral Load
	public static HashSet<Patient> getPatientsWithHighVL(Date startDate, Date endDate) {
		return getPatientsWithVL(startDate, endDate, FOLLOW_UP_FORM_ENCOUNTER_TYPE, VIRAL_LOAD_CONCEPT_UUID);
	}
	
	public static HashSet<Patient> getPatientsWithPersistentHighVL(Date startDate, Date endDate) {
		return getPatientsWithVL(startDate, endDate, HIGH_VL_ENCOUNTERTYPE_UUID, REPEAT_VL_RESULTS);
	}
	
	public static HashSet<Patient> getPatientsWithVL(Date startDate, Date endDate, String encounterTypeUuid,
	        String conceptUuid) {
		EncounterType encounterType = Context.getEncounterService().getEncounterTypeByUuid(encounterTypeUuid);
		EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(null, null, startDate, endDate, null,
		        null, Collections.singletonList(encounterType), null, null, null, false);
		List<Encounter> encounters = Context.getEncounterService().getEncounters(encounterSearchCriteria);
		
		HashSet<Patient> vlPatients = new HashSet<>();
		
		List<Obs> vlObs = Context.getObsService().getObservations(null, encounters,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(conceptUuid)), null, null, null, null,
		    null, null, startDate, endDate, false);
		
		for (Obs obs : vlObs) {
			if (obs.getValueNumeric() != null && obs.getValueNumeric() >= THRESHOLD) {
				vlPatients.add((Patient) obs.getPerson());
			}
		}
		
		HashSet<Patient> deceasedPatients = getDeceasedPatientsByDateRange(startDate, endDate);
		HashSet<Patient> transferredOutPatients = getTransferredOutClients(startDate, endDate);
		
		vlPatients.removeAll(deceasedPatients);
		vlPatients.removeAll(transferredOutPatients);
		
		return vlPatients;
	}
	
	public static HashSet<Patient> getPatientsWithRepeatedVL(Date startDate, Date endDate) {
		EncounterType repeatViralLoadEncounterType = Context.getEncounterService()
		        .getEncounterTypeByUuid(HIGH_VL_ENCOUNTERTYPE_UUID);
		EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(null, null, startDate, endDate, null,
		        null, Collections.singletonList(repeatViralLoadEncounterType), null, null, null, false);
		List<Encounter> encounters = Context.getEncounterService().getEncounters(encounterSearchCriteria);
		
		HashSet<Patient> repeatviralLoadPatients = new HashSet<>();
		
		List<Obs> repeatviralLoadObs = Context.getObsService().getObservations(null, encounters,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(REAPEAT_VL_COLLECTION)), null, null, null,
		    null, null, null, startDate, endDate, false);
		
		for (Obs obs : repeatviralLoadObs) {
			if (obs != null) {
				repeatviralLoadPatients.add((Patient) obs.getPerson());
			}
		}
		
		return repeatviralLoadPatients;
		
	}
	
	public static HashSet<Patient> getPatientsWithSwitchART(Date startDate, Date endDate) {
		EncounterType switchARTRegimenEncounterType = Context.getEncounterService()
		        .getEncounterTypeByUuid(FOLLOW_UP_FORM_ENCOUNTER_TYPE);
		EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(null, null, startDate, endDate, null,
		        null, Collections.singletonList(switchARTRegimenEncounterType), null, null, null, false);
		List<Encounter> encounters = Context.getEncounterService().getEncounters(encounterSearchCriteria);
		
		HashSet<Patient> switchARTRegimenPatients = new HashSet<>();
		Map<Patient, String> patientPreviousRegimen = new HashMap<>();
		
		List<Obs> switchARTRegimenObs = Context.getObsService().getObservations(null, encounters,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(ACTIVE_REGIMEN_CONCEPT_UUID)), null, null,
		    null, null, null, null, startDate, endDate, false);
		
		for (Obs obs : switchARTRegimenObs) {
			if (obs != null && obs.getPerson() instanceof Patient) {
				Patient patient = (Patient) obs.getPerson();
				String currentRegimen = obs.getValueCoded() != null ? obs.getValueCoded().getUuid() : null;
				
				if (currentRegimen != null) {
					if (patientPreviousRegimen.containsKey(patient)) {
						String previousRegimen = patientPreviousRegimen.get(patient);
						if (!currentRegimen.equals(previousRegimen)) {
							switchARTRegimenPatients.add(patient);
						}
					}
					patientPreviousRegimen.put(patient, currentRegimen);
				}
			}
		}
		
		return switchARTRegimenPatients;
	}
	
	public static HashSet<Patient> getPatientsWithSecondLineSwitchART(Date startDate, Date endDate) {
		EncounterType secondLineSwitchARTRegimenEncounterType = Context.getEncounterService()
		        .getEncounterTypeByUuid(FOLLOW_UP_FORM_ENCOUNTER_TYPE);
		EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(null, null, startDate, endDate, null,
		        null, Collections.singletonList(secondLineSwitchARTRegimenEncounterType), null, null, null, false);
		List<Encounter> encounters = Context.getEncounterService().getEncounters(encounterSearchCriteria);
		
		HashSet<Patient> secondLineSwitchARTRegimenPatients = new HashSet<>();
		
		List<Obs> secondLineSwitchARTRegimenObs = Context.getObsService().getObservations(null, encounters,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(ACTIVE_REGIMEN_CONCEPT_UUID)), null, null,
		    null, null, null, null, startDate, endDate, false);
		
		for (Obs obs : secondLineSwitchARTRegimenObs) {
			if (obs != null && obs.getPerson() instanceof Patient) {
				Patient patient = (Patient) obs.getPerson();
				String currentRegimen = obs.getValueCoded() != null ? obs.getValueCoded().getUuid() : null;
				
				if (currentRegimen != null && SECOND_LINE_REGIMENS.contains(currentRegimen)) {
					secondLineSwitchARTRegimenPatients.add(patient);
				}
			}
		}
		
		return secondLineSwitchARTRegimenPatients;
	}
	
	// Determine if Patient is High Viral Load and return true if it is equal or
	// above threshold
	public static boolean determineIfPatientIsHighVl(Patient patient) {
		List<Obs> vlObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(VIRAL_LOAD_CONCEPT_UUID)), null, null,
		    null, null, null, null, null, null, false);
		
		if (vlObs != null && !vlObs.isEmpty()) {
			return vlObs.get(0).getValueNumeric() >= THRESHOLD;
		}
		return false;
	}
	
	public static boolean determineIfPatientIsRTT(Patient patient) {
		List<Obs> rttObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(DATE_RETURNED_TO_TREATMENT)), null, null,
		    null, null, null, null, null, null, false);
		return rttObs != null && !rttObs.isEmpty();
	}
	
	public static Map<String, Map<String, Integer>> generateDashboardSummaryFromObs(Date startDate, Date endDate,
	        List<Obs> obsList, SSEMRWebServicesController.filterCategory filterCategory) {
		
		if (obsList == null) {
			throw new IllegalArgumentException("Observation list cannot be null");
		}
		
		List<Date> dates = new ArrayList<>();
		for (Obs obs : obsList) {
			if (obs == null) {
				System.out.println("Encountered null observation");
				continue;
			}
			
			Date obsDate = obs.getObsDatetime();
			if (obsDate == null) {
				System.out.println("Encountered observation with null date: " + obs);
				continue;
			}
			
			if (obsDate.after(DateUtils.addDays(startDate, -1)) && obsDate.before(DateUtils.addDays(endDate, 1))) {
				dates.add(obsDate);
			}
		}
		
		return generateSummary(dates);
	}
	
	public static List<PatientObservations.FamilyMemberObservation> getFamilyMemberObservations(Patient patient) {
		List<PatientObservations.FamilyMemberObservation> familyMemberObservations = new ArrayList<>();
		
		Concept familyInfoConcept = Context.getConceptService().getConceptByUuid(GENERAL_FAMILY_MEMBERS_UUID);
		
		List<Obs> familyObs = Context.getObsService().getObservationsByPersonAndConcept(patient.getPerson(),
		    familyInfoConcept);
		
		for (Obs obs : familyObs) {
			PatientObservations.FamilyMemberObservation memberObservation = new PatientObservations.FamilyMemberObservation();
			
			// Use getFamilyMemberField to populate fields dynamically
			memberObservation.setName((String) getFamilyMemberField(patient, obs, "name", false));
			memberObservation.setAge((Double) getFamilyMemberField(patient, obs, "age", false));
			memberObservation.setSex((String) getFamilyMemberField(patient, obs, "sex", false));
			memberObservation.setHivStatus((String) getFamilyMemberField(patient, obs, "hivStatus", false));
			memberObservation.setArtNumber((String) getFamilyMemberField(patient, obs, "artNumber", false));
			
			familyMemberObservations.add(memberObservation);
		}
		
		return familyMemberObservations;
	}
	
	public static List<PatientObservations.IndexFamilyMemberObservation> getIndexFamilyMemberObservations(Patient patient) {
		List<PatientObservations.IndexFamilyMemberObservation> indexFamilyMemberObservations = new ArrayList<>();
		
		Concept indexFamilyInfoConcept = Context.getConceptService().getConceptByUuid(INDEX_FAMILY_MEMBERS_UUID);
		
		// Retrieve index family member observations
		List<Obs> indexFamilyObs = Context.getObsService().getObservationsByPersonAndConcept(patient.getPerson(),
		    indexFamilyInfoConcept);
		
		for (Obs obs : indexFamilyObs) {
			PatientObservations.IndexFamilyMemberObservation indexMemberObservation = new PatientObservations.IndexFamilyMemberObservation();
			
			// Use getFamilyMemberField to populate fields dynamically
			indexMemberObservation.setName((String) getFamilyMemberField(patient, obs, "name", true));
			indexMemberObservation.setAge((String) getFamilyMemberField(patient, obs, "age", true));
			indexMemberObservation.setSex((String) getFamilyMemberField(patient, obs, "sex", true));
			indexMemberObservation.setRelationship((String) getFamilyMemberField(patient, obs, "relationship", true));
			indexMemberObservation.setHivStatusKnown((String) getFamilyMemberField(patient, obs, "hivStatusKnown", true));
			indexMemberObservation.setHivStatus((String) getFamilyMemberField(patient, obs, "hivStatus", true));
			indexMemberObservation.setPhone((String) getFamilyMemberField(patient, obs, "phone", true));
			indexMemberObservation.setUniqueArtNumber((String) getFamilyMemberField(patient, obs, "uniqueArtNumber", true));
			
			indexFamilyMemberObservations.add(indexMemberObservation);
		}
		
		return indexFamilyMemberObservations;
	}
	
	// General Family Members Info
	public static final Map<String, String> familyMemberFields;
	static {
		familyMemberFields = new HashMap<>();
		familyMemberFields.put("name", FAMILY_MEMBER_NAME_UUID);
		familyMemberFields.put("age", FAMILY_MEMBER_AGE_UUID);
		familyMemberFields.put("sex", FAMILY_MEMBER_SEX_UUID);
		familyMemberFields.put("hivStatus", FAMILY_MEMBER_HIV_STATUS_UUID);
		familyMemberFields.put("artNumber", FAMILY_MEMBER_UAN_UUID);
	}
	
	// Index Family Members Info
	public static final Map<String, String> indexFamilyMemberFields;
	static {
		indexFamilyMemberFields = new HashMap<>();
		indexFamilyMemberFields.put("name", INDEX_FAMILY_MEMBER_NAME_UUID);
		indexFamilyMemberFields.put("age", INDEX_FAMILY_MEMBER_AGE_UUID);
		indexFamilyMemberFields.put("sex", INDEX_FAMILY_MEMBER_SEX_UUID);
		indexFamilyMemberFields.put("relationship", INDEX_FAMILY_MEMBER_RELATIONSHIP_UUID);
		indexFamilyMemberFields.put("hivStatusKnown", INDEX_FAMILY_MEMBER_HIV_STATUS_KNOWN_UUID);
		indexFamilyMemberFields.put("hivStatus", INDEX_FAMILY_MEMBER_HIV_STATUS_UUID);
		indexFamilyMemberFields.put("phone", INDEX_FAMILY_MEMBER_PHONE_UUID);
		indexFamilyMemberFields.put("uniqueArtNumber", INDEX_FAMILY_MEMBER_ART_NUMBER_UUID);
	}
	
	public static Object getFamilyMemberField(Patient patient, Obs observation, String field, boolean isIndexFamilyMember) {
		String conceptUuid = isIndexFamilyMember ? indexFamilyMemberFields.get(field) : familyMemberFields.get(field);
		return getObsValue(patient, observation, conceptUuid);
	}
	
	public static List<PatientObservations.CommunityHealthWorkerObservation> getCommunityHealthWorkerObservations(
	        Patient patient) {
		List<PatientObservations.CommunityHealthWorkerObservation> chwObservations = new ArrayList<>();
		
		Concept cadreConcept = Context.getConceptService().getConceptByUuid(COMMUNITY_CADRE);
		List<Obs> cadreObsList = Context.getObsService().getObservationsByPersonAndConcept(patient.getPerson(),
		    cadreConcept);
		
		for (Obs cadreObs : cadreObsList) {
			String cadreValue = cadreObs.getValueCoded() != null ? cadreObs.getValueCoded().getName().getName() : null;
			
			if (cadreValue != null) {
				PatientObservations.CommunityHealthWorkerObservation chwObservation = new PatientObservations.CommunityHealthWorkerObservation();
				
				if (cadreValue.equalsIgnoreCase("Community HIV Service Officer")) {
					chwObservation.setCadre("CHSO");
					chwObservation.setName((String) getLatestObsByConcept(patient, NAME_OF_COMMUNITY_HIV_SERVICE_OFFICER));
					chwObservation.setPhone((String) getLatestObsByConcept(patient, COMMUNITY_HIV_SERVICE_OFFICER_PHONE));
					
				} else if (cadreValue.equalsIgnoreCase("Mentor Mother")) {
					chwObservation.setCadre("MM");
					chwObservation.setName((String) getLatestObsByConcept(patient, NAME_OF_MENTOR_MOTHER));
					chwObservation.setPhone((String) getLatestObsByConcept(patient, MENTOR_MOTHER_PHONE));
					chwObservation.setAddress((String) getLatestObsByConcept(patient, ADDRESS_OF_MENTOR_MOTHER));
					
				} else if (cadreValue.equalsIgnoreCase("Senior Mentor Mother")) {
					chwObservation.setCadre("sMM");
					chwObservation.setName((String) getLatestObsByConcept(patient, NAME_OF_SENIOR_MENTOR_MOTHER));
					chwObservation.setPhone((String) getLatestObsByConcept(patient, SENIOR_MENTOR_MOTHER_PHONE));
					
				} else if (cadreValue.equalsIgnoreCase("Community outreach worker")) {
					chwObservation.setCadre("COW");
					chwObservation.setName((String) getLatestObsByConcept(patient, NAME_OF_COW));
					chwObservation.setPhone((String) getLatestObsByConcept(patient, COW_PHONE));
					chwObservation.setAddress((String) getLatestObsByConcept(patient, ADDRESS_OF_COW));
				}
				chwObservations.add(chwObservation);
			}
		}
		return chwObservations;
	}
	
	public static String getInitiationDate(Patient patient) {
		return getPatientDateByConcept(patient, DATE_OF_ART_INITIATION_CONCEPT_UUID);
	}
	
	public static String getDateOfDeath(Patient patient) {
		return getPatientDateByConcept(patient, DATE_OF_DEATH_UUID);
	}
	
	public static String getDateTransferredOut(Patient patient) {
		return getPatientDateByConcept(patient, DATE_TRANSFERRED_OUT_UUID);
	}
	
	public static String getReturnToTreatmentDate(Patient patient) {
		return getPatientDateByConcept(patient, DATE_RETURNED_TO_TREATMENT);
	}
	
	public static Date getDeathDate(Patient patient) {
		return getDateByConcept(patient, DATE_OF_DEATH_UUID);
	}
	
	public static Date getTransferredOutDate(Patient patient) {
		return getDateByConcept(patient, DATE_TRANSFERRED_OUT_UUID);
	}
	
	public static Date getReturnedToTreatmentDate(Patient patient) {
		return getDateByConcept(patient, DATE_RETURNED_TO_TREATMENT);
	}
	
	public static Date getFirstEACDate(Patient patient) {
		return getDateByConcept(patient, FIRST_EAC_DATE_UUID);
	}
	
	public static Date getSecondEACDate(Patient patient) {
		return getDateByConcept(patient, SECOND_EAC_DATE_UUID);
	}
	
	public static Date getThirdEACDate(Patient patient) {
		return getDateByConcept(patient, THIRD_EAC_DATE_UUID);
	}
	
	public static Date getExtendedEACDate(Patient patient) {
		return getDateByConcept(patient, EXTENDED_EAC_DATE_UUID);
	}
	
	public static Date getRepeatVLDate(Patient patient) {
		return getDateByConcept(patient, REPEAT_VIRAL_LOAD_DATE);
	}
	
	public static Date getARTFirstLineSwitchDate(Patient patient) {
		return getDateByConcept(patient, ART_FIRST_LINE_SWITCH_DATE);
	}
	
	public static Date getARTSecondLineSwitchDate(Patient patient) {
		return getDateByConcept(patient, ART_SECOND_LINE_SWITCH_DATE);
	}
	
	public static Object paginateAndGenerateSummary(List<Patient> patientList, int page, int size, int totalCount,
	        Date startDate, Date endDate, SSEMRWebServicesController.filterCategory filterCategory) {
		return generateSummaryResponse.generateSummaryResponse(patientList, page, size, "totalPatients", totalCount,
		    startDate, endDate, filterCategory, GenerateSummary::generateSummary);
	}
	
	public static Object fetchAndPaginatePatients(List<Patient> patientList, int page, int size, int totalCount,
	        Date startDate, Date endDate, SSEMRWebServicesController.filterCategory filterCategory) {
		
		if (page < 0 || size <= 0) {
			return "Invalid page or size value. Page must be >= 0 and size must be > 0.";
		}
		
		int fromIndex = page * size;
		if (fromIndex >= patientList.size()) {
			return "Page out of bounds. Please check the page number and size.";
		}
		
		int toIndex = Math.min((page + 1) * size, patientList.size());
		
		List<Patient> paginatedPatients = patientList.subList(fromIndex, toIndex);
		
		ObjectNode allPatientsObj = JsonNodeFactory.instance.objectNode();
		allPatientsObj.put("totalPatients", totalCount);
		
		return generatePatientListObj.generatePatientListObj(new HashSet<>(paginatedPatients), startDate, endDate,
		    filterCategory, allPatientsObj);
	}
	
	public static Integer getPatientSystolicPressure(Patient patient) {
		List<Obs> systolicPressureObs = Context.getObsService().getObservations(
		    Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(SYSTOLIC_BLOOD_PRESSURE)), null, null,
		    null, null, null, null, null, null, false);
		
		if (!systolicPressureObs.isEmpty()) {
			Obs systolicPressureObservation = systolicPressureObs.get(0);
			return systolicPressureObservation.getValueNumeric().intValue();
		}
		
		return null;
	}
	
	public static Integer getPatientDiastolicPressure(Patient patient) {
		List<Obs> diastolicPressureObs = Context.getObsService().getObservations(
		    Collections.singletonList(patient.getPerson()), null,
		    Collections.singletonList(Context.getConceptService().getConceptByUuid(DIASTOLIC_BLOOD_PRESSURE)), null, null,
		    null, null, null, null, null, null, false);
		
		if (!diastolicPressureObs.isEmpty()) {
			Obs diastolicPressureObservation = diastolicPressureObs.get(0);
			return diastolicPressureObservation.getValueNumeric().intValue();
		}
		return null;
		
	}
	
	public static Double getPatientTemperature(Patient patient) {
		List<Obs> temperatureObs = Context.getObsService().getObservations(Collections.singletonList(patient.getPerson()),
		    null, Collections.singletonList(Context.getConceptService().getConceptByUuid(TEMPERATURE)), null, null, null,
		    null, null, null, null, null, false);
		
		if (!temperatureObs.isEmpty()) {
			Obs temperatureObservation = temperatureObs.get(0);
			return temperatureObservation.getValueNumeric();
		}
		
		return null;
	}
	
	public static String populateBloodPressure(Patient patient) {
		Integer systolic = getPatientSystolicPressure(patient);
		Integer diastolic = getPatientDiastolicPressure(patient);
		if (systolic != null && diastolic != null) {
			return systolic + "/" + diastolic;
		}
		return null;
	}
	
	public static String hasPendingVlResults(Patient patient) {
		EncounterType followUpEncounterType = Context.getEncounterService()
		        .getEncounterTypeByUuid(FOLLOW_UP_FORM_ENCOUNTER_TYPE);
		EncounterSearchCriteria encounterSearchCriteria = new EncounterSearchCriteria(patient, null, null, null, null, null,
		        Collections.singletonList(followUpEncounterType), null, null, null, false);
		List<Encounter> encounters = Context.getEncounterService().getEncounters(encounterSearchCriteria);
		
		if (encounters == null || encounters.isEmpty()) {
			return "No";
		}
		
		Encounter latestEncounter = Collections.max(encounters, Comparator.comparing(Encounter::getEncounterDatetime));
		
		Concept sampleCollectedConcept = Context.getConceptService().getConceptByUuid(SAMPLE_COLLECTION_DATE_UUID);
		Concept sampleReceivedConcept = Context.getConceptService().getConceptByUuid(DATE_VL_RESULTS_RECEIVED_UUID);
		
		boolean hasCollected = false;
		boolean hasReceived = false;
		
		for (Obs obs : latestEncounter.getAllObs()) {
			if (obs.getConcept().equals(sampleCollectedConcept)) {
				hasCollected = true;
			} else if (obs.getConcept().equals(sampleReceivedConcept)) {
				hasReceived = true;
			}
		}
		
		return (hasCollected && !hasReceived) ? "Yes" : "No";
	}
	
	/**
	 * Calculates the age of a patient based on their birthdate.
	 */
	public static Period calculatePatientAge(Patient patient) {
		LocalDate birthDate = patient.getBirthdate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		return Period.between(birthDate, LocalDate.now());
	}
	
	/**
	 * Formats the patient's age into a human-readable string.
	 */
	public static String formatAge(Period age) {
		int years = age.getYears();
		int months = age.getMonths();
		
		if (years >= 1) {
			return years + " year" + (years > 1 ? "s" : "");
		}
		if (months >= 1) {
			return months + " month" + (months > 1 ? "s" : "");
		}
		
		long weeks = age.getDays() / 7;
		return weeks + " week" + (weeks > 1 ? "s" : "");
	}
	
	/**
	 * Fetches the last encounter for a given patient and encounter type.
	 * 
	 * @param patient The patient
	 * @param encounterType The encounter type
	 * @return The most recent encounter of the given type for the patient, or null if none exist
	 */
	public static Encounter getLastEncounterForType(Patient patient, EncounterType encounterType) {
		List<Encounter> encounters = Context.getEncounterService().getEncounters(patient, null, null, null, null,
		    Collections.singletonList(encounterType), null, null, null, false);
		if (!encounters.isEmpty()) {
			encounters.sort((e1, e2) -> e2.getEncounterDatetime().compareTo(e1.getEncounterDatetime()));
			return encounters.get(0);
		}
		return null;
	}
	
	/**
	 * Fetches the most recent active visit for a given patient.
	 */
	public static Visit getLatestActiveVisit(Patient patient) {
		List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);
		if (activeVisits.isEmpty()) {
			return null;
		}
		activeVisits.sort(Comparator.comparing(Visit::getStartDatetime).reversed());
		return activeVisits.get(0);
	}
	
	public static String getCHWName(Patient patient) {
		return getLatestValueFromConcepts(patient, COW_NAME_UUID, CHW_NAME_UUID);
	}
	
	public static String getCHWPhone(Patient patient) {
		return getLatestValueFromConcepts(patient, COW_PHONE_UUID, CHW_PHONE_UUID);
	}
}
