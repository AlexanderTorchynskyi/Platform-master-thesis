package ua.tor.platform.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ua.tor.platform.model.ParsedVacancy;
import ua.tor.platform.model.Status;
import ua.tor.platform.model.Vacancy;
import ua.tor.platform.repository.IParsedVacancyRepository;
import ua.tor.platform.repository.IVacancyRepository;

/**
 * 
 * @author alex
 *
 */
@Component
public class ParserService {

	private static final Logger LOGGER = Logger.getLogger(ParserService.class);

	@Autowired
	private IParsedVacancyRepository parsedVacancyRepository;
	@Autowired
	private IVacancyRepository vacancyRepository;

	private List<Vacancy> batchOfVacancy;
	private List<ParsedVacancy> bathOfParsedVacancy;

	/**
	 * The method will remove all words that are repeated and all symbolics;
	 * 
	 * @param crawlerId
	 */
	public void parseVacancies(ObjectId crawlerId) {
		StopWatch watcher = new StopWatch();
		watcher.start();
		LOGGER.info("Getting vacancies from DB by crawler id");
		batchOfVacancy = vacancyRepository.findByCrawlerId(crawlerId);
		changeStatusForVacancy(crawlerId, Status.IN_PROCESS);
		LOGGER.info("Start chopping.. ");
		chopChop();
		LOGGER.info("Finish chopping.. ");
		changeStatusForVacancy(crawlerId, Status.PROCESSED);
		LOGGER.info("Start saving to DB");
		saveOrUpdate(bathOfParsedVacancy);
		watcher.stop();
		LOGGER.info("Total time in seconds " + watcher.getTime(TimeUnit.SECONDS));
	}

	private void changeStatusForVacancy(ObjectId crawlerId, Status status) {
		for (Vacancy vacancy : batchOfVacancy) {
			vacancy.setStatus(status);
		}
		vacancyRepository.saveAll(batchOfVacancy);
	}

	private void chopChop() {
		String description;
		ParsedVacancy parsedVacancy;

		String[] splitedDescription;
		bathOfParsedVacancy = new ArrayList<>();
		Set<String> cleanDescription;

		for (Vacancy vacancy : batchOfVacancy) {

			parsedVacancy = new ParsedVacancy();
			cleanDescription = new HashSet<>();

			description = vacancy.getDescription();
			description = description.replaceAll("[^a-zA-Z0-9]", " ");
			splitedDescription = description.split("\\s+");

			for (String word : splitedDescription) {
				if (!word.isEmpty()) {
					cleanDescription.add(word.toLowerCase());
				}
			}

			parsedVacancy.setCrawlerId(vacancy.getCrawlerId());
			parsedVacancy.setDescription(cleanDescription);
			parsedVacancy.setStatus(Status.NEW);

			bathOfParsedVacancy.add(parsedVacancy);
		}
	}

	private void saveOrUpdate(List<ParsedVacancy> parsedVacancies) {
		parsedVacancyRepository.saveAll(parsedVacancies);
	}
}