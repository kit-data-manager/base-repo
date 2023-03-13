/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.kit.datamanager.repo.web.converter;

import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap;
import com.nimbusds.jose.util.JSONObjectUtils;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.Contributor;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Description;
import edu.kit.datamanager.repo.domain.RelatedIdentifier;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Scheme;
import edu.kit.datamanager.repo.domain.Subject;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 *
 * @author jejkal
 */
@Component
public class ZenodoMessageConverter implements HttpMessageConverter {

    private Logger LOGGER = LoggerFactory.getLogger(ZenodoMessageConverter.class);

    @Override
    public boolean canRead(Class arg0, MediaType arg1) {
        if (arg0 == null || arg1 == null) {
            return false;
        }
        LOGGER.trace("Checking applicability of ZenodoMessageConverter for class {} and mediatype {}.", arg0, arg1);
        return DataResource.class.equals(arg0) && arg1.toString().startsWith("application/vnd.zenodo.org+json");
    }

    @Override
    public boolean canWrite(Class arg0, MediaType arg1) {
        //writing currently not supported
        return false;
    }

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.valueOf("application/vnd.zenodo.org+json"));
    }

    @Override
    public Object read(Class arg0, HttpInputMessage arg1) throws IOException, HttpMessageNotReadableException {
        LOGGER.trace("Reading HttpInputMessage for transformation.");
        try (InputStreamReader reader = new InputStreamReader(arg1.getBody(), "UTF-8")) {
            String data = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
            try {
                return parseZenodo(data);
            } catch (Exception e) {
                throw new HttpMessageNotReadableException("Unable to parse Zenodo input.");
            }
        }
    }

    @Override
    public void write(Object arg0, MediaType arg1, HttpOutputMessage arg2) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private DataResource parseZenodo(String input) throws Exception {
        Map<String, Object> result = JSONObjectUtils.parse(input);
        result = (Map<String, Object>) result.get("metadata");
        //String doi = (String) result.get("doi");//doi=10.5281/zenodo.7651129 
        DataResource res = DataResource.factoryDataResourceWithDoi((String) result.get("doi"));

        res.getDescriptions().add(Description.factoryDescription((String) result.get("description"), Description.TYPE.ABSTRACT));

        ArrayList<Map<String, Object>> contributors = (ArrayList<Map<String, Object>>) result.get("contributors");//contributors=[{name=TIB-Technische Informationsbibliothek Universit?tsbibliothek Hannover, nameType=Organizational, affiliation=[], contributorType=HostingInstitution, nameIdentifiers=[]}, {name=Technische Informationsbibliothek (TIB), affiliation=[], contributorType=DataManager, nameIdentifiers=[]}, {name=Sack, Martin, nameType=Personal, givenName=Martin, familyName=Sack, affiliation=[], contributorType=Other, nameIdentifiers=[]}]
        if (contributors != null) {
            contributors.forEach(contributor -> {
                ArrayList<String> affiliationNames = new ArrayList<String>();
                if (contributor.containsKey("affiliation")) {
                    affiliationNames.add((String) contributor.get("affiliation"));
                }

                String name = (String) contributor.get("name");
                String[] nameSplit = name.split("[\\s,]+");
                Agent agent = Agent.factoryAgent(nameSplit[0], nameSplit[1], affiliationNames.toArray(new String[]{}));
                Contributor.TYPE type = Contributor.TYPE.OTHER;
                for (Contributor.TYPE t : Contributor.TYPE.values()) {
                    if (t.getValue().equals((String) contributor.get("type"))) {
                        type = t;
                        break;
                    }
                }
                res.getContributors().add(Contributor.factoryContributor(agent, type));
            });
        }
        res.getTitles().add(Title.factoryTitle((String) result.get("title")));
        res.setLanguage((String) result.get("language"));

        res.setVersion((String) result.get("version"));
        //todo: take only year, format is yyyy-mm-dd
        String pubDate = (String) result.get("publication_date");

        String pubYear = Integer.toString(LocalDate.now().getYear());
        if (pubDate != null) {
            if (pubDate.indexOf("-") > 0) {
                pubYear = pubDate.substring(0, pubDate.indexOf("-"));
            }

            try {
                Integer.parseInt(pubYear);
                res.setPublicationYear(pubYear);
            } catch (NumberFormatException ex) {
                //invalid year
            }
        }

        ArrayList<Map<String, Object>> creators = (ArrayList<Map<String, Object>>) result.get("creators");//creators=[{name=Karlsruhe, nameType=Organizational, affiliation=[], nameIdentifiers=[{schemeUri=https://d-nb.info/gnd/, nameIdentifier=4029713-5, nameIdentifierScheme=GND}]}]
        if (creators != null) {
            creators.forEach(creator -> {
                ArrayList<String> affiliationNames = new ArrayList<String>();
                if (creator.containsKey("affiliation")) {
                    affiliationNames.add((String) creator.get("affiliation"));
                }

                String name = (String) creator.get("name");
                String[] nameSplit = name.split("[\\s,]+");
                Agent agent = Agent.factoryAgent(nameSplit[1].trim(), nameSplit[0].trim(), affiliationNames.toArray(new String[]{}));
                res.getCreators().add(agent);
            });
        }
        Map<String, Object> resourceType = (Map<String, Object>) result.get("resource_type");//types={ris=RPRT, bibtex=article, citeproc=article-journal, schemaOrg=ScholarlyArticle, resourceType=Miscellaneous, resourceTypeGeneral=Text}
        if (resourceType != null) {
            ResourceType.TYPE_GENERAL typeGeneral = ResourceType.TYPE_GENERAL.OTHER;
            for (ResourceType.TYPE_GENERAL type : ResourceType.TYPE_GENERAL.values()) {
                if (type.getValue().equals((String) resourceType.get("type"))) {
                    typeGeneral = type;
                    break;
                }
            }
            res.setResourceType(ResourceType.createResourceType((String) resourceType.get("Title"), typeGeneral));
        } else {
            res.setResourceType(ResourceType.createResourceType("unknown", ResourceType.TYPE_GENERAL.OTHER));
        }

        ArrayList<Map<String, Object>> relatedIdentifiers = (ArrayList<Map<String, Object>>) result.get("related_identifiers");//relatedIdentifiers=[{relationType=IsSupplementTo, relatedIdentifier=https://github.com/Chilipp/de-messaging-python-presentation-20210122/tree/v1.0, relatedIdentifierType=URL}, {relationType=IsVersionOf, relatedIdentifier=10.5281/zenodo.4456786, relatedIdentifierType=DOI}]
        if (relatedIdentifiers != null) {
            relatedIdentifiers.forEach(identifier -> {
                RelatedIdentifier.RELATION_TYPES relationType = null;
                for (RelatedIdentifier.RELATION_TYPES t : RelatedIdentifier.RELATION_TYPES.values()) {
                    if (t.getValue().equals((String) identifier.get("relation"))) {
                        relationType = t;
                        break;
                    }
                }

                Identifier.IDENTIFIER_TYPE identifierType = Identifier.IDENTIFIER_TYPE.OTHER;
                for (Identifier.IDENTIFIER_TYPE t : Identifier.IDENTIFIER_TYPE.values()) {
                    if (t.getValue().equals((String) identifier.get("scheme"))) {
                        identifierType = t;
                        break;
                    }
                }
                RelatedIdentifier relId = RelatedIdentifier.factoryRelatedIdentifier(relationType, (String) identifier.get("identifier"), null, null);
                relId.setIdentifierType(identifierType);
                res.getRelatedIdentifiers().add(relId);
            });
        }

        LinkedTreeMap<String, Object> rights = (LinkedTreeMap<String, Object>) result.get("license");//[{rights=KITopen License, rightsUri=https://publikationen.bibliothek.kit.edu/kitopen-lizenz}]
        if (rights != null) {
            res.getRights().add(Scheme.factoryScheme((String) rights.get("id"), ""));
        }

        ArrayList<String> keywords = (ArrayList<String>) result.get("keywords"); //subjects=[{subject=Energieeinsparung, subjectScheme=gnd, classificationCode=4014700-9}, {subject=Klima?nderung, subjectScheme=gnd, classificationCode=4164199-1}, {subject=Globale Umweltprobleme, subjectScheme=bk, classificationCode=(id=106416790)43.47}, {subjectScheme=linsearch(mapping), classificationCode=rest}]
        if (keywords != null) {
            keywords.forEach(keyword -> {
                res.getSubjects().add(Subject.factorySubject(keyword, null, null, null));
            });
        }

        ArrayList<Map<String, Object>> communities = (ArrayList<Map<String, Object>>) result.get("communities");//[{rights=KITopen License, rightsUri=https://publikationen.bibliothek.kit.edu/kitopen-lizenz}]
        if (communities != null) {
            communities.forEach(community -> {
                res.getSubjects().add(Subject.factorySubject((String) community.get("id"), null, null, null));
            });
        }
        res.getAcls().add(new AclEntry(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL, PERMISSION.READ));

        return res;
    }

    public static void main(String[] args) throws Exception {
        String input = ("{\n"
                + "    \"conceptrecid\": \"632990\",\n"
                + "    \"created\": \"2016-04-27T06:43:22+00:00\",\n"
                + "    \"doi\": \"10.5281/zenodo.50325\",\n"
                + "    \"files\": [\n"
                + "        {\n"
                + "            \"bucket\": \"34b3cf23-3382-40a2-b056-18ebfcdfe198\",\n"
                + "            \"checksum\": \"md5:37ecda80658ee2a3e07ce83cc2b025c4\",\n"
                + "            \"key\": \"Data_extraction_review_of_self_medication_questionnaires.xlsb\",\n"
                + "            \"links\": {\n"
                + "                \"self\": \"https://zenodo.org/api/files/34b3cf23-3382-40a2-b056-18ebfcdfe198/Data_extraction_review_of_self_medication_questionnaires.xlsb\"\n"
                + "            },\n"
                + "            \"size\": 44405,\n"
                + "            \"type\": \"xlsb\"\n"
                + "        }\n"
                + "    ],\n"
                + "    \"id\": 50325,\n"
                + "    \"links\": {\n"
                + "        \"badge\": \"https://zenodo.org/badge/doi/10.5281/zenodo.50325.svg\",\n"
                + "        \"bucket\": \"https://zenodo.org/api/files/34b3cf23-3382-40a2-b056-18ebfcdfe198\",\n"
                + "        \"doi\": \"https://doi.org/10.5281/zenodo.50325\",\n"
                + "        \"html\": \"https://zenodo.org/record/50325\",\n"
                + "        \"latest\": \"https://zenodo.org/api/records/50325\",\n"
                + "        \"latest_html\": \"https://zenodo.org/record/50325\",\n"
                + "        \"self\": \"https://zenodo.org/api/records/50325\"\n"
                + "    },\n"
                + "    \"metadata\": {\n"
                + "        \"access_right\": \"open\",\n"
                + "        \"access_right_category\": \"success\",\n"
                + "        \"creators\": [\n"
                + "            {\n"
                + "                \"affiliation\": \"1.\\tDepartment of Epidemiology, Helmholtz Centre for Infection Research, Braunschweig, Germany.\",\n"
                + "                \"name\": \"Limaye Dnyanesh\"\n"
                + "            }\n"
                + "        ],\n"
                + "        \"description\": \"<p>This is pubmed and web of science data sets for a review on self medication survey questionnaires.&nbsp;</p>\",\n"
                + "        \"doi\": \"10.5281/zenodo.50325\",\n"
                + "        \"journal\": {\n"
                + "            \"title\": \"BMC Public Health\",\n"
                + "            \"year\": \"2016\"\n"
                + "        },\n"
                + "        \"keywords\": [\n"
                + "            \"self-medication, questionnaires, survey\"\n"
                + "        ],\n"
                + "        \"license\": {\n"
                + "            \"id\": \"CC0-1.0\"\n"
                + "        },\n"
                + "        \"publication_date\": \"2016-04-23\",\n"
                + "        \"relations\": {\n"
                + "            \"version\": [\n"
                + "                {\n"
                + "                    \"count\": 1,\n"
                + "                    \"index\": 0,\n"
                + "                    \"is_last\": true,\n"
                + "                    \"last_child\": {\n"
                + "                        \"pid_type\": \"recid\",\n"
                + "                        \"pid_value\": \"50325\"\n"
                + "                    },\n"
                + "                    \"parent\": {\n"
                + "                        \"pid_type\": \"recid\",\n"
                + "                        \"pid_value\": \"632990\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        \"resource_type\": {\n"
                + "            \"title\": \"Dataset\",\n"
                + "            \"type\": \"dataset\"\n"
                + "        },\n"
                + "        \"title\": \"Data set for survey questionnaires to assess self-medication practices\"\n"
                + "    },\n"
                + "    \"owners\": [\n"
                + "        21575\n"
                + "    ],\n"
                + "    \"revision\": 9,\n"
                + "    \"stats\": {\n"
                + "        \"downloads\": 151.0,\n"
                + "        \"unique_downloads\": 147.0,\n"
                + "        \"unique_views\": 55.0,\n"
                + "        \"version_downloads\": 151.0,\n"
                + "        \"version_unique_downloads\": 147.0,\n"
                + "        \"version_unique_views\": 55.0,\n"
                + "        \"version_views\": 56.0,\n"
                + "        \"version_volume\": 6705155.0,\n"
                + "        \"views\": 56.0,\n"
                + "        \"volume\": 6705155.0\n"
                + "    },\n"
                + "    \"updated\": \"2020-01-21T07:21:59.608743+00:00\"\n"
                + "}");

        //collection of funny dois
        //https://doi.org/10.25991/vrhga.2019.20.3.001 (russian)
        String text = org.apache.commons.codec.binary.StringUtils.newStringUtf8(input.getBytes());

        System.out.println(new ZenodoMessageConverter().parseZenodo(text));
    }
}
