package leiliang91;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.playersummaries.GetPlayerSummaries;
import com.lukaspradel.steamapi.data.json.playersummaries.Player;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.GetPlayerSummariesRequest;
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Hello world!
 *
 */
public class HallOfShameGeneratorCli
{
    private static final Logger log = LogManager.getLogger(HallOfShameGeneratorCli.class);

    private static final CommandLineParser parser = new DefaultParser();
    private static final String TRASHES_FILE = "trashesFile";
    private static final String API_KEY_FILE = "apiKeyFile";
    private static final List<Option> optionList = Arrays.asList(
            new Option(TRASHES_FILE, true, "trashes.json file"),
            new Option(API_KEY_FILE, true, "api key file")
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        Options options = new Options();
        for(Option option : optionList) {
            options.addOption(option);
        }

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            log.error("Parse options error", e);
            System.exit(1);
        }

        String apiKeyFile;
        if(cmd.hasOption(API_KEY_FILE)){
            apiKeyFile = cmd.getOptionValue(API_KEY_FILE);
            log.info("Using apiKeyFile from cmd: {}", apiKeyFile);
        } else {
            apiKeyFile = "test-only/api-key";
            log.info("Using test apiKeyFile: {}", apiKeyFile);
        }

        String apiKey = null;
        try {
            apiKey = Files.readAllLines(Paths.get(apiKeyFile)).get(0);
            log.info("API_KEY is set");
        } catch (IOException e) {
            log.error("ApiKeyFile read error", e);
            System.exit(1);
        }

        String trashesFile;
        if(cmd.hasOption(TRASHES_FILE)) {
            trashesFile = cmd.getOptionValue(TRASHES_FILE);
            log.info("Using trashesFile from cmd: {}", trashesFile);
        } else {
            trashesFile = "test-only/trashes.json";
            log.info("Using test trashesFile: {}", trashesFile);
        }

        updateTrashes(apiKey, trashesFile);

    }

    private static void updateTrashes(String apiKey, String trashesFile) {
        Trashes trashes = null;
        try {
            trashes = objectMapper.readValue(new File(trashesFile), Trashes.class);
            log.info("Trashes are reload from {}", trashesFile);
        } catch (IOException e) {
            log.error("Exception when reading trashesFile", e);
            System.exit(1);
        }

        List<String> ids = new ArrayList<>(trashes.getTrashesMap().keySet());
        log.info("Ids for query: {}", ids);

        SteamWebApiClient client = new SteamWebApiClient.SteamWebApiClientBuilder(apiKey).build();
        GetPlayerSummariesRequest request = SteamWebApiRequestFactory.createGetPlayerSummariesRequest(ids);
        GetPlayerSummaries getPlayerSummaries = null;
        try {
            getPlayerSummaries = client.processRequest(request);
        } catch (SteamApiException e) {
            log.error("Client call error", e);
            System.exit(1);
        }


        Trashes updatedTrashes = new Trashes();
        updatedTrashes.setTrashesMap(new HashMap<>());
        for (Player player : getPlayerSummaries.getResponse().getPlayers()) {
            String id = player.getSteamid();
            log.info("Updating trash {} using response: {}", id , player);
            Trash trash = new Trash();
            trash.setId(id);
            trash.setAvatar(player.getAvatarfull());
            trash.setName(player.getPersonaname());
            trash.setReasons(trashes.getTrashesMap().get(player.getSteamid()).getReasons());
            trash.setProfileUrl(player.getProfileurl());

            updatedTrashes.getTrashesMap().put(id, trash);
        }
        updatedTrashes.setLastUpdated(System.currentTimeMillis());

        log.info("Updating trashesFile with new values");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(trashesFile), updatedTrashes);
        } catch (IOException e) {
            log.error("Error when updating trashesFile {}", trashesFile);
            System.exit(1);
        }
    }
}
