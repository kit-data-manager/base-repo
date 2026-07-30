/*
 * Copyright 2016 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.repo.web.converter;

import edu.kit.datamanager.repo.domain.DataResource;
import com.nimbusds.jose.util.JSONObjectUtils;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.Contributor;
import edu.kit.datamanager.repo.domain.Date;
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
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
 * Message converter implementation from internal format to DataCite format.
 *
 * @author jejkal
 */
@Component
public class DataCiteMessageConverter implements HttpMessageConverter {

    private Logger LOGGER = LoggerFactory.getLogger(DataCiteMessageConverter.class);

    @Override
    public boolean canRead(Class arg0, MediaType arg1) {
        if (arg0 == null || arg1 == null) {
            return false;
        }
        LOGGER.trace("Checking applicability of DataCiteMessageConverter for class {} and mediatype {}.", arg0, arg1);
        return DataResource.class.equals(arg0) && arg1.toString().startsWith("application/vnd.datacite.org+json");
    }

    @Override
    public boolean canWrite(Class arg0, MediaType arg1) {
        //writing currently not supported
        return false;
    }

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.valueOf("application/vnd.datacite.org+json"));
    }

    @Override
    public Object read(Class arg0, HttpInputMessage arg1) throws IOException, HttpMessageNotReadableException {
        LOGGER.trace("Reading HttpInputMessage for transformation.");
        try (InputStreamReader reader = new InputStreamReader(arg1.getBody(), "UTF-8")) {
            String data = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
            try {
                return parseDatacite(data);
            } catch (Exception e) {
                throw new HttpMessageNotReadableException("Unable to parse DataCite input.");
            }
        }
    }

    @Override
    public void write(Object arg0, MediaType arg1, HttpOutputMessage arg2) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private DataResource parseDatacite(String input) throws Exception {
        Map<String, Object> result = JSONObjectUtils.parse(input);
        //String doi = (String) result.get("doi");//doi=10.5445/IR/1000081328 
        DataResource res = DataResource.factoryDataResourceWithDoi((String) result.get("doi"));
        //String id = (String) result.get("id");//id=https://doi.org/10.5445/ir/1000081328
        //res.setIdentifier(PrimaryIdentifier.factoryPrimaryIdentifier((String) result.get("id")));
        //String url = (String) result.get("url");//url=https://publikationen.bibliothek.kit.edu/1000081328
        res.getAlternateIdentifiers().add(Identifier.factoryIdentifier((String) result.get("url"), Identifier.IDENTIFIER_TYPE.URL));
        //String publisher = (String) result.get("publisher");//publisher=Karlsruhe
        res.setPublisher((String) result.get("publisher"));
        //Long publicationYear = (Long) result.get("publicationYear");//publicationYear=2008
        res.setPublicationYear(Long.toString((Long) result.get("publicationYear")));
        //String language = (String) result.get("language");//language=de
        res.setLanguage((String) result.get("language"));
        //String version = (String) result.get("version");//version=1.0
        res.setVersion((String) result.get("version"));
        ArrayList<String> sizes = (ArrayList<String>) result.get("sizes");//sizes=[Online-Ressource (163 S., 2,09 MB)]
        sizes.forEach(size -> {
            res.getSizes().add(size);
        });

        ArrayList<String> formats = (ArrayList<String>) result.get("formats");//formats=[application/pdf]
        formats.forEach(format -> {
            res.getFormats().add(format);
        });

        Map<String, Object> types = (Map<String, Object>) result.get("types");//types={ris=RPRT, bibtex=article, citeproc=article-journal, schemaOrg=ScholarlyArticle, resourceType=Miscellaneous, resourceTypeGeneral=Text}
        String resourceType = (String) types.get("resourceType");
        ResourceType.TYPE_GENERAL typeGeneral = ResourceType.TYPE_GENERAL.OTHER;
        for (ResourceType.TYPE_GENERAL type : ResourceType.TYPE_GENERAL.values()) {
            if (type.getValue().equals((String) types.get("resourceTypeGeneral"))) {
                typeGeneral = type;
                break;
            }
        }
        res.setResourceType(ResourceType.createResourceType(resourceType, typeGeneral));

        ArrayList<Map<String, Object>> creators = (ArrayList<Map<String, Object>>) result.get("creators");//creators=[{name=Karlsruhe, nameType=Organizational, affiliation=[], nameIdentifiers=[{schemeUri=https://d-nb.info/gnd/, nameIdentifier=4029713-5, nameIdentifierScheme=GND}]}]
        creators.forEach(creator -> {
            ArrayList<String> affiliationNames = new ArrayList<String>();
            ArrayList<Map<String, Object>> affiliation = (ArrayList<Map<String, Object>>) creator.get("affiliation");
            affiliation.forEach(a -> {
                affiliationNames.add((String) a.get("name"));
            });
            res.getCreators().add(Agent.factoryAgent((creator.get("givenName") == null) ? (String) creator.get("name") : (String) creator.get("givenName"), (String) creator.get("familyName"), affiliationNames.toArray(new String[]{})));
        });

        ArrayList<Map<String, Object>> titles = (ArrayList<Map<String, Object>>) result.get("titles");//titles=[{title=Entwicklung, Erprobung und Demonstration neuer Logistikkonzepte f?r Biobrennstoffe (BIOLOG), Teilvorhaben: Konditionierung gr?ner Biomasse durch elektroporationsunterst?tzte Entw?sserung : Abschlussbericht ; Berichtszeitraum: 01.08.2006 - 31.07.2008}, {title=BIOLOG, titleType=AlternativeTitle}]
        titles.forEach(title -> {
            String lang = (String) title.get("lang");
            String value = (String) title.get("title");
            Title.TYPE type = Title.TYPE.OTHER;
            for (Title.TYPE t : Title.TYPE.values()) {
                if (t.getValue().equals((String) title.get("titleType"))) {
                    type = t;
                    break;
                }
            }
            Title titleValue = Title.factoryTitle(value, type);
            titleValue.setLang(lang);
            res.getTitles().add(titleValue);
        });

        ArrayList<Map<String, Object>> identifiers = (ArrayList<Map<String, Object>>) result.get("identifiers");//identifiers=[{identifier=GBV:621034096, identifierType=firstid}, {identifier=TIBKAT:621034096, identifierType=ftx-id}, {identifier=621034096, identifierType=ppn}, {identifier=03KS0089, identifierType=contract}]
        identifiers.forEach(identifier -> {
            Identifier.IDENTIFIER_TYPE type = Identifier.IDENTIFIER_TYPE.OTHER;
            for (Identifier.IDENTIFIER_TYPE t : Identifier.IDENTIFIER_TYPE.values()) {
                if (t.getValue().equals((String) identifier.get("identifierType"))) {
                    type = t;
                    break;
                }
            }
            res.getAlternateIdentifiers().add(Identifier.factoryIdentifier((String) identifier.get("identifier"), type));
        });

        ArrayList<Map<String, Object>> relatedIdentifiers = (ArrayList<Map<String, Object>>) result.get("relatedIdentifiers");//relatedIdentifiers=[{relationType=IsSupplementTo, relatedIdentifier=https://github.com/Chilipp/de-messaging-python-presentation-20210122/tree/v1.0, relatedIdentifierType=URL}, {relationType=IsVersionOf, relatedIdentifier=10.5281/zenodo.4456786, relatedIdentifierType=DOI}]
        relatedIdentifiers.forEach(identifier -> {
            RelatedIdentifier.RELATION_TYPES relationType = null;
            for (RelatedIdentifier.RELATION_TYPES t : RelatedIdentifier.RELATION_TYPES.values()) {
                if (t.getValue().equals((String) identifier.get("relationType"))) {
                    relationType = t;
                    break;
                }
            }

            Identifier.IDENTIFIER_TYPE identifierType = Identifier.IDENTIFIER_TYPE.OTHER;
            for (Identifier.IDENTIFIER_TYPE t : Identifier.IDENTIFIER_TYPE.values()) {
                if (t.getValue().equals((String) identifier.get("identifierType"))) {
                    identifierType = t;
                    break;
                }
            }
            RelatedIdentifier relId = RelatedIdentifier.factoryRelatedIdentifier(relationType, (String) identifier.get("relatedIdentifier"), null, null);
            relId.setIdentifierType(identifierType);
            res.getRelatedIdentifiers().add(relId);
        });

        ArrayList<Map<String, Object>> descriptions = (ArrayList<Map<String, Object>>) result.get("descriptions");//descriptions=[{descriptionType=Abstract}, {description=graph. Darst., descriptionType=Other}]
        descriptions.forEach(description -> {
            Description.TYPE type = Description.TYPE.OTHER;
            for (Description.TYPE t : Description.TYPE.values()) {
                if (t.getValue().equals((String) description.get("descriptionType"))) {
                    type = t;
                    break;
                }
            }
            res.getDescriptions().add(Description.factoryDescription((String) description.get("descriptionType"), type, (String) description.get("lang")));
        });

        ArrayList<Map<String, Object>> dates = (ArrayList<Map<String, Object>>) result.get("dates");//dates=[{date=2008, dateType=Issued}]
        dates.forEach(date -> {
            Date.DATE_TYPE type = null;
            for (Date.DATE_TYPE t : Date.DATE_TYPE.values()) {
                if (t.getValue().equals((String) date.get("dateType"))) {
                    type = t;
                    break;
                }
            }

            String[] patterns = new String[]{"yyyy-MM-dd", "yyyy"};

            for (String p : patterns) {
                try {
                    TemporalAccessor a = DateTimeFormatter.ofPattern(p).parse((String) date.get("date"));
                    res.getDates().add(Date.factoryDate(Instant.from(a), type));
                    break;
                } catch (DateTimeException ex) {
                    //next attempt
                }
            }
        });

        ArrayList<Map<String, Object>> rights = (ArrayList<Map<String, Object>>) result.get("rightsList");//[{rights=KITopen License, rightsUri=https://publikationen.bibliothek.kit.edu/kitopen-lizenz}]
        rights.forEach(right -> {
            /*
            {
             "rights": "Creative Commons Attribution 4.0 International",
             "rightsUri": "https://creativecommons.org/licenses/by/4.0/legalcode",
             "schemeUri": "https://spdx.org/licenses/",
             "rightsIdentifier": "cc-by-4.0",
             "rightsIdentifierScheme": "SPDX"
            }
             */
            res.getRights().add(Scheme.factoryScheme((String) right.get("rights"), (String) right.get("rightsUri")));
        });

        ArrayList<Map<String, Object>> subjects = (ArrayList<Map<String, Object>>) result.get("subjects"); //subjects=[{subject=Energieeinsparung, subjectScheme=gnd, classificationCode=4014700-9}, {subject=Klima?nderung, subjectScheme=gnd, classificationCode=4164199-1}, {subject=Globale Umweltprobleme, subjectScheme=bk, classificationCode=(id=106416790)43.47}, {subjectScheme=linsearch(mapping), classificationCode=rest}]
        subjects.forEach(subject -> {
            Scheme subjectScheme = Scheme.factoryScheme((String) subject.get("subjectScheme"), (String) subject.get("classificatiocCode"));
            res.getSubjects().add(Subject.factorySubject((String) subject.get("subject"), null, (String) subject.get("lang"), subjectScheme));
        });

        ArrayList<Map<String, Object>> contributors = (ArrayList<Map<String, Object>>) result.get("contributors");//contributors=[{name=TIB-Technische Informationsbibliothek Universit?tsbibliothek Hannover, nameType=Organizational, affiliation=[], contributorType=HostingInstitution, nameIdentifiers=[]}, {name=Technische Informationsbibliothek (TIB), affiliation=[], contributorType=DataManager, nameIdentifiers=[]}, {name=Sack, Martin, nameType=Personal, givenName=Martin, familyName=Sack, affiliation=[], contributorType=Other, nameIdentifiers=[]}]
        contributors.forEach(contributor -> {
            ArrayList<String> affiliationNames = new ArrayList<String>();
            ArrayList<Map<String, Object>> affiliation = (ArrayList<Map<String, Object>>) contributor.get("affiliation");
            affiliation.forEach(a -> {
                affiliationNames.add((String) a.get("name"));
            });
            Agent agent = Agent.factoryAgent((contributor.get("givenName") == null) ? (String) contributor.get("name") : (String) contributor.get("givenName"), (String) contributor.get("familyName"), affiliationNames.toArray(new String[]{}));
            Contributor.TYPE type = Contributor.TYPE.OTHER;
            for (Contributor.TYPE t : Contributor.TYPE.values()) {
                if (t.getValue().equals((String) contributor.get("contributorType"))) {
                    type = t;
                    break;
                }
            }
            res.getContributors().add(Contributor.factoryContributor(agent, type));
        });
        
        res.getAcls().add(new AclEntry(AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL, PERMISSION.READ));
        
        return res;
    }

    public static void main(String[] args) throws Exception {
        String input = ("{\n"
                + "  \"id\": \"https://doi.org/10.5281/zenodo.4456786\",\n"
                + "  \"doi\": \"10.5281/ZENODO.4456786\",\n"
                + "  \"url\": \"https://zenodo.org/record/4456786\",\n"
                + "  \"types\": {\n"
                + "    \"ris\": \"RPRT\",\n"
                + "    \"bibtex\": \"article\",\n"
                + "    \"citeproc\": \"article-journal\",\n"
                + "    \"schemaOrg\": \"ScholarlyArticle\",\n"
                + "    \"resourceType\": \"Presentation\",\n"
                + "    \"resourceTypeGeneral\": \"Text\"\n"
                + "  },\n"
                + "  \"creators\": [\n"
                + "    {\n"
                + "      \"name\": \"Sommer, Philipp S.\",\n"
                + "      \"givenName\": \"Philipp S.\",\n"
                + "      \"familyName\": \"Sommer\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"Helmholtz-Zentrum Geesthacht (HZG), Institute of Coastal Research, Geesthacht, Germany\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"nameIdentifiers\": [\n"
                + "        {\n"
                + "          \"schemeUri\": \"https://orcid.org\",\n"
                + "          \"nameIdentifier\": \"https://orcid.org/0000-0001-6171-7716\",\n"
                + "          \"nameIdentifierScheme\": \"ORCID\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Wichert, Viktoria\",\n"
                + "      \"givenName\": \"Viktoria\",\n"
                + "      \"familyName\": \"Wichert\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"Helmholtz-Zentrum Geesthacht (HZG), Institute of Coastal Research, Geesthacht, Germany\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"nameIdentifiers\": [\n"
                + "        {\n"
                + "          \"schemeUri\": \"https://orcid.org\",\n"
                + "          \"nameIdentifier\": \"https://orcid.org/0000-0002-3402-6562\",\n"
                + "          \"nameIdentifierScheme\": \"ORCID\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Eggert, Daniel\",\n"
                + "      \"givenName\": \"Daniel\",\n"
                + "      \"familyName\": \"Eggert\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"German Research Center for GeoSciences GFZ, Potsdam, Germany\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"nameIdentifiers\": [\n"
                + "        {\n"
                + "          \"schemeUri\": \"https://orcid.org\",\n"
                + "          \"nameIdentifier\": \"https://orcid.org/0000-0003-0251-4390\",\n"
                + "          \"nameIdentifierScheme\": \"ORCID\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Dinter, Tilman\",\n"
                + "      \"givenName\": \"Tilman\",\n"
                + "      \"familyName\": \"Dinter\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"Alfred-Wegener-Institut Helmholtz-Zentrum für Polar- und Meeresforschung (AWI), Bremerhaven, Germany\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Getzlaff, Klaus\",\n"
                + "      \"givenName\": \"Klaus\",\n"
                + "      \"familyName\": \"Getzlaff\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"GEOMAR Helmholtz Centre for Ocean Research Kiel, Germany\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"nameIdentifiers\": [\n"
                + "        {\n"
                + "          \"schemeUri\": \"https://orcid.org\",\n"
                + "          \"nameIdentifier\": \"https://orcid.org/0000-0002-0347-7838\",\n"
                + "          \"nameIdentifierScheme\": \"ORCID\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Lehmann, Andreas\",\n"
                + "      \"givenName\": \"Andreas\",\n"
                + "      \"familyName\": \"Lehmann\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"GEOMAR Helmholtz Centre for Ocean Research Kiel, Germany\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"nameIdentifiers\": [\n"
                + "        {\n"
                + "          \"schemeUri\": \"https://orcid.org\",\n"
                + "          \"nameIdentifier\": \"https://orcid.org/0000-0001-5618-6105\",\n"
                + "          \"nameIdentifierScheme\": \"ORCID\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Werner, Christian\",\n"
                + "      \"givenName\": \"Christian\",\n"
                + "      \"familyName\": \"Werner\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"Karlsruhe Institute of Technology, Institute of Meteorology and Climate Research - Atmospheric Environmental Research (IMK-IFU), Garmisch-Partenkirchen, Germany\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Silva, Brenner\",\n"
                + "      \"givenName\": \"Brenner\",\n"
                + "      \"familyName\": \"Silva\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"Alfred-Wegener-Institut Helmholtz-Zentrum für Polar- und Meeresforschung (AWI), Bremerhaven, Germany\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Schmidt, Lennart\",\n"
                + "      \"givenName\": \"Lennart\",\n"
                + "      \"familyName\": \"Schmidt\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"Helmholtz-Zentrum für Umweltforschung GmbH - UFZ, Leipzig, Germany\"\n"
                + "        }\n"
                + "      ]\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Schäfer, Angela\",\n"
                + "      \"givenName\": \"Angela\",\n"
                + "      \"familyName\": \"Schäfer\",\n"
                + "      \"affiliation\": [\n"
                + "        {\n"
                + "          \"name\": \"Alfred-Wegener-Institut Helmholtz-Zentrum für Polar- und Meeresforschung (AWI), Bremerhaven, Germany\"\n"
                + "        }\n"
                + "      ],\n"
                + "      \"nameIdentifiers\": [\n"
                + "        {\n"
                + "          \"schemeUri\": \"https://orcid.org\",\n"
                + "          \"nameIdentifier\": \"https://orcid.org/0000-0003-1784-2979\",\n"
                + "          \"nameIdentifierScheme\": \"ORCID\"\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  ],\n"
                + "  \"titles\": [\n"
                + "    {\n"
                + "      \"title\": \"Distributed data analysis for better scientific collaborations\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"publisher\": \"Zenodo\",\n"
                + "  \"container\": {},\n"
                + "  \"subjects\": [\n"
                + "    {\n"
                + "      \"subject\": \"marehub\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"subject\": \"datahub\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"subject\": \"helmholtz\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"subject\": \"geomar\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"subject\": \"hgf\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"subject\": \"remote procedure call\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"contributors\": [],\n"
                + "  \"dates\": [\n"
                + "    {\n"
                + "      \"date\": \"2021-01-22\",\n"
                + "      \"dateType\": \"Issued\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"publicationYear\": 2021,\n"
                + "  \"identifiers\": [],\n"
                + "  \"sizes\": [],\n"
                + "  \"formats\": [],\n"
                + "  \"version\": \"v1.0\",\n"
                + "  \"rightsList\": [\n"
                + "    {\n"
                + "      \"rights\": \"Creative Commons Attribution 4.0 International\",\n"
                + "      \"rightsUri\": \"https://creativecommons.org/licenses/by/4.0/legalcode\",\n"
                + "      \"schemeUri\": \"https://spdx.org/licenses/\",\n"
                + "      \"rightsIdentifier\": \"cc-by-4.0\",\n"
                + "      \"rightsIdentifierScheme\": \"SPDX\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"rights\": \"Open Access\",\n"
                + "      \"rightsUri\": \"info:eu-repo/semantics/openAccess\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"descriptions\": [\n"
                + "    {\n"
                + "      \"description\": \"A common challenge for projects with multiple involved research institutes is a well-defined and productive collaboration. All parties measure and analyze different aspects, depend on each other, share common methods, and exchange the latest results, findings, and data. Today this exchange is often impeded by a lack of ready access to shared computing and storage resources. In our talk, we present a new and innovative remote procedure call (RPC) framework. We focus on a distributed setup, where project partners do not necessarily work at the same institute, and do not have access to each others resources. We present the prototype of an application programming interface (API) developed in Python that enables scientists to collaboratively explore and analyze sets of distributed data. It offers the functionality to request remote data through a comfortable interface, and to share analytical workflows and their results. Our methodology uses the Digital Earth software framework, especially its messaging component. The prototype enables researchers to make their methods accessible as a backend module running on their own servers. Hence researchers from other institutes may apply the available methods through a lightweight python API. This API transforms standard python calls into requests to the backend process on the remote server. In the end, the overhead for both, the backend developer and the remote user, is very low. The effort of implementing the necessary workflow and API usage equalizes the writing of code in a non-distributed setup. Besides that, data do not have to be downloaded locally, the analysis can be executed \\\"close to the data\\\" while using the institutional infrastructure where the eligible data set is stored. With our prototype, we demonstrate distributed data access and analysis workflows across institutional borders to enable effective scientific collaboration, thus deepening our understanding of the Earth system.\",\n"
                + "      \"descriptionType\": \"Abstract\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"geoLocations\": [],\n"
                + "  \"fundingReferences\": [],\n"
                + "  \"relatedIdentifiers\": [\n"
                + "    {\n"
                + "      \"relationType\": \"IsSupplementTo\",\n"
                + "      \"relatedIdentifier\": \"https://github.com/Chilipp/de-messaging-python-presentation-20210122/tree/v1.0\",\n"
                + "      \"relatedIdentifierType\": \"URL\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"relationType\": \"HasVersion\",\n"
                + "      \"relatedIdentifier\": \"10.5281/zenodo.4456787\",\n"
                + "      \"relatedIdentifierType\": \"DOI\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"schemaVersion\": \"http://datacite.org/schema/kernel-4\",\n"
                + "  \"providerId\": \"cern\",\n"
                + "  \"clientId\": \"cern.zenodo\",\n"
                + "  \"agency\": \"datacite\",\n"
                + "  \"state\": \"findable\"\n"
                + "}");

        //collection of funny dois
        //https://doi.org/10.25991/vrhga.2019.20.3.001 (russian)
        String text = org.apache.commons.codec.binary.StringUtils.newStringUtf8(input.getBytes());

        new DataCiteMessageConverter().parseDatacite(text);
    }
}
