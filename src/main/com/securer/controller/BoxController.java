package com.securer.controller;

import com.box.sdk.*;
import com.securer.controller.webHook.CallBoxAPI;
import com.securer.controller.webHook.HttpClientConfig;
import com.securer.model.*;

import com.securer.services.AccessTokenRepository;
import jdk.nashorn.internal.runtime.arrays.ArrayData;
import jdk.nashorn.internal.runtime.arrays.IteratorAction;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;

import org.springframework.data.geo.Box;
import org.springframework.data.mongodb.core.MongoAction;
import org.springframework.data.mongodb.core.MongoActionOperation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by faraz on 12/25/15.
 */

@RestController
@RequestMapping("/box")
public class BoxController {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BoxController.class);

    private static final String phoneNumberPattern =
            "1?\\W*([2-9][0-8][0-9])\\W*([2-9][0-9]{2})\\W*([0-9]{4})(\\se?x?t?(\\d*))?";
    private static final String creditCardPattern =
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[12345][0-9]{14}|3[47][0-9]{13}|3(?:0[012345]" +
                    "|[68][0-9])[0-9]{11}|6(?:011|5[0-9]{2})[0-9]{12}|(?:2131|1800|35[0-9]{3})[0-9]{11})\\b";
    private static final String socialSecurityPattern = "^(?!(000|666|9))\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}$";


    private static final String combinedPattern = phoneNumberPattern +
            "|" + creditCardPattern + "|" + socialSecurityPattern;

    @Value("${box.clientId}")
    private String clientId;

    @Value("${box.clientSecret}")
    private String clientSecret;

    @Autowired
    AccessTokenRepository accessTokenRepository;

    private String boxUrl = "https://app.box.com/api/oauth2/token";

    String code;
    String error;


    private BoxUser user;
    private BoxUser.Info boxUserInfo;
    private BoxAPIConnection api;
    private AccessToken token;

    private void setUpBoxService() {
        if (api == null || token == null) {
            callBoxService();
        }
    }

    private void callBoxService() throws NoSuchElementException {

        token = getToken();
        try {

            api = new BoxAPIConnection(clientId, clientSecret,
                    token.getAccess_token(), token.getRefresh_token());
        } catch (BoxAPIException e) {
            logger.error(e.getMessage());
        }
    }

    private AccessToken getToken() {
        Sort sort = new Sort(Sort.Direction.DESC, "createdDate");

        List<AccessToken> tokens = accessTokenRepository.findAll(sort);

        Optional<AccessToken>
                OptionalToken = tokens.stream().findFirst();

        token = OptionalToken.get();
        return token;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/test")
    public String test() {
        return "OK";
    }


    @RequestMapping(method = RequestMethod.GET, path = "/code")
    public ResponseEntity getAuthorizationCode(@RequestParam(required = false) String code,
                                               @RequestParam(required = false) String error,
                                               @RequestParam(required = false) String error_description) {

        this.code = code;
        if (error != null) {
            System.out.println("error: " + error + " " + error_description);
            return new ResponseEntity(error + " " + error_description, HttpStatus.BAD_REQUEST);
        } else {
            (new Thread((new CallBoxAPI(code, accessTokenRepository)))).start();
        }

        return new ResponseEntity("Successfully connected to Box!", HttpStatus.ACCEPTED);
    }


    @RequestMapping(method = RequestMethod.GET, path = "/view")
    public ResponseEntity accessResources() {
        HttpClient httpClient = HttpClientConfig.getConnection();

        HttpGet httpGet = new HttpGet(boxUrl);
        HttpResponse response = null;
        String line = "";
        try {
            response = httpClient.execute(httpGet);

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();

            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }


        return new ResponseEntity(line, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/user")
    public ResponseEntity getUserInfo() {
        try {
            setUpBoxService();
        } catch (NoSuchElementException e) {
            return new ResponseEntity("No Authorization Token Found, please authorize through user",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            user = BoxUser.getCurrentUser(api);
            boxUserInfo = user.getInfo();
        } catch (BoxAPIException e) {
            error = e.getResponseCode() + " | " + e.getMessage() + e.getCause();
            return new ResponseEntity(error, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(boxUserInfo, HttpStatus.OK);

    }


    @RequestMapping(method = RequestMethod.GET, path = "/folder")
    public BoxItem.Info getFolders() {

        try {
            setUpBoxService();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        return rootFolder.getInfo();

    }


    //TODO:
    @RequestMapping(value = "/model/user", method = RequestMethod.GET)
    public BoxUser.Info getUserModel() {
        if (boxUserInfo == null) {
            getUserInfo();
        }
        return boxUserInfo;
    }

    private static void listFolder(BoxFolder folder, int depth) {
        for (BoxItem.Info itemInfo : folder) {
            String indent = "";
            for (int i = 0; i < depth; i++) {
                indent += "    ";
            }

            System.out.println(indent + itemInfo.getName());
            if (itemInfo instanceof BoxFolder.Info) {
                BoxFolder childFolder = (BoxFolder) itemInfo.getResource();
                if (depth < 1) {
                    listFolder(childFolder, depth + 1);
                }
            }
        }
    }

    /**
     * get the folder in the user's account, if no param is found, get the root folder info.
     *
     * @param folderId
     * @return
     */
    @RequestMapping(value = "/getFolder", method = RequestMethod.GET)
    public ResponseEntity getRootFolder(@RequestParam(required = false) String folderId) {
        if (folderId == null) {
            folderId = "0";
        }

        token = getToken();
        String accessToken = token.getAccess_token();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
//        ResponseEntity<String> responseEntity = restTemplate.exchange("https://api.box.com/2.0/folders/" + folderId,
//                HttpMethod.GET, entity, String.class);
        ResponseEntity<Folder> responseEntity = restTemplate.exchange("https://api.box.com/2.0/folders/" + folderId,
                HttpMethod.GET, entity, Folder.class);
        //System.out.println(responseEntity.getBody());
        //System.out.println(responseEntity);
        return responseEntity;
    }

    @RequestMapping(value = "/checkFile", method = RequestMethod.GET)
    public ResponseEntity<List<SuspiciousFile>> getFile() {
        //TODO: returns a list of Suspicious Files
        List<SuspiciousFile> suspiciousFiles = new ArrayList<>();

        ResponseEntity<Folder> rootFolder = getRootFolder("0");
        Folder root = rootFolder.getBody();
        Queue<Folder> queue = new ArrayDeque<>();

        queue.add(root);
        StringBuilder data = new StringBuilder();
        while (!queue.isEmpty()) {
            Folder folder = queue.remove();

            if (folder.getType().equals("file")) {
                String file = data.append(readFile(folder.getId()).getBody()).toString();
                List<String> matches = checkFile(file);

                //TODO: checkFile for patterns and return file name and match found
                if (!matches.isEmpty()) {
                    SuspiciousFile suspiciousFile = getFileInfo(folder.getId()).getBody();
                    suspiciousFile.setSuspiciousData(matches);
                    suspiciousFiles.add(suspiciousFile);
                }

            } else {
                for (Entry entry : folder.getItem_collection().getEntries()) {
                    if (entry.getType().equals("file")) {
                        //readFile
                        String file = data.append(readFile(entry.getId()).getBody()).toString();
                        List<String> matches = checkFile(file);

                        //TODO: checkFile for patterns and return file name and match found
                        if (!matches.isEmpty()) {
                            SuspiciousFile suspiciousFile = getFileInfo(entry.getId()).getBody();
                            suspiciousFile.setSuspiciousData(matches);
                            suspiciousFiles.add(suspiciousFile);
                        }
                    } else {
                        //call getFolder with id
                        ResponseEntity<Folder> entity = getRootFolder(entry.getId());
                        queue.add(entity.getBody());
                    }
                }
            }
        }
        return new ResponseEntity<>(suspiciousFiles, HttpStatus.OK);

    }

    @RequestMapping(value = "/readFile", method = RequestMethod.GET)
    public ResponseEntity<String> readFile(@RequestParam(required = true) String fileId) {

        token = getToken();
        String accessToken = token.getAccess_token();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange("https://api.box.com/2.0/files/" + fileId + "/content",
                HttpMethod.GET, entity, String.class);

        return responseEntity;
    }

    @RequestMapping(value = "/fileInfo", method = RequestMethod.GET)
    public ResponseEntity<SuspiciousFile> getFileInfo(@RequestParam(required = true) String fileId) {

        token = getToken();
        String accessToken = token.getAccess_token();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<File> responseEntity = restTemplate.exchange("https://api.box.com/2.0/files/" + fileId,
                HttpMethod.GET, entity, File.class);
        SuspiciousFile suspiciousFile = new SuspiciousFile();

        File file = responseEntity.getBody();

        suspiciousFile.setCreatedAt(file.getCreated_at());
        suspiciousFile.setFileName(file.getName());
        suspiciousFile.setOwner(file.getCreated_by());

        return new ResponseEntity<>(suspiciousFile, HttpStatus.OK);
    }

    private List<String> checkFile(String file) {
        Pattern p = Pattern.compile(combinedPattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

        Matcher m = p.matcher(file);
        List<String> matches = new ArrayList<>();
        while (m.find()) {

            matches.add(m.group());
        }
        return matches;
    }


}


