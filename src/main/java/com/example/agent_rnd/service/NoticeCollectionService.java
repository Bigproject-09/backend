package com.example.agent_rnd.service;

import com.example.agent_rnd.config.BizinfoProperties;
import com.example.agent_rnd.domain.notice.NoticeFile;
import com.example.agent_rnd.domain.notice.NoticeHashtag;
import com.example.agent_rnd.domain.notice.ProjectNotice;
import com.example.agent_rnd.repository.NoticeFileRepository;
import com.example.agent_rnd.repository.NoticeHashtagRepository;
import com.example.agent_rnd.repository.ProjectNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeCollectionService {

    private final ProjectNoticeRepository projectNoticeRepository;
    private final NoticeFileRepository noticeFileRepository;
    private final NoticeHashtagRepository noticeHashtagRepository;
    private final BizinfoProperties bizinfoProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 기업마당 API에서 기술 공고 수집
     */
    @Transactional
    public int collectNotices() {
        int inserted = 0;
        int pageIndex = 1;
        int pageUnit = 100;
        Set<String> seenSeq = new HashSet<>();

        log.info("🔥 공고 수집 시작");

        while (true) {
            try {
                // 1. API 호출
                String xmlResponse = fetchPage(pageIndex, pageUnit);

                // 2. XML 파싱
                List<NoticeData> noticeList = parseXml(xmlResponse);

                log.info("page={} 수집된 기술 공고={}", pageIndex, noticeList.size());

                if (noticeList.isEmpty()) {
                    break;
                }

                // 3. DB 저장
                for (NoticeData data : noticeList) {
                    if (seenSeq.contains(data.seq)) {
                        continue;
                    }
                    seenSeq.add(data.seq);

                    // 이미 존재하는 공고는 스킵
                    if (projectNoticeRepository.existsBySeq(data.seq)) {
                        continue;
                    }

                    // ProjectNotice 저장
                    ProjectNotice notice = ProjectNotice.ofSeq(
                            data.seq,
                            data.title,
                            data.link,
                            data.author,
                            data.excInsttNm,
                            data.description,
                            data.pubDate,
                            data.reqstDt,
                            data.trgetNm
                    );

                    projectNoticeRepository.save(notice);

                    // ✅ NoticeFile 저장 (본문 + 첨부 파일)
                    List<FileInfo> printFiles = parseFiles(data.printFileNm, data.printFlpthNm);
                    List<FileInfo> attachFiles = parseFiles(data.fileNm, data.flpthNm);

                    // 합치기
                    List<FileInfo> allFiles = new ArrayList<>();
                    allFiles.addAll(printFiles);
                    allFiles.addAll(attachFiles);

                    for (FileInfo file : allFiles) {
                        NoticeFile noticeFile = NoticeFile.of(
                                notice,
                                file.fileName,
                                file.filePath
                        );
                        noticeFileRepository.save(noticeFile);
                    }

                    // NoticeHashtag 저장
                    List<String> hashtags = parseHashtags(data.hashTags);
                    for (String tag : hashtags) {
                        if (!tag.isEmpty()) {
                            NoticeHashtag hashtag = NoticeHashtag.of(notice, tag);
                            noticeHashtagRepository.save(hashtag);
                        }
                    }

                    inserted++;
                }

                pageIndex++;

            } catch (Exception e) {
                log.error("공고 수집 중 오류 발생: page={}", pageIndex, e);
                break;
            }
        }

        log.info("✅ 공고 수집 완료: {}건", inserted);
        return inserted;
    }

    /**
     * 기업마당 API 호출
     */
    private String fetchPage(int pageIndex, int pageUnit) {
        String url = String.format("%s?crtfcKey=%s&dataType=rss&pageIndex=%d&pageUnit=%d&searchCnt=%d",
                bizinfoProperties.getUrl(),
                bizinfoProperties.getKey(),
                pageIndex,
                pageUnit,
                pageUnit
        );

        return restTemplate.getForObject(url, String.class);
    }

    /**
     * XML 파싱
     */
    private List<NoticeData> parseXml(String xmlResponse) throws Exception {
        List<NoticeData> result = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes("UTF-8")));

        NodeList items = doc.getElementsByTagName("item");

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            String lcategory = getTagValue("lcategory", item);

            // 기술 공고만 필터링
            if (!isTechCategory(lcategory)) {
                continue;
            }

            NoticeData data = new NoticeData();
            data.seq = getTagValue("seq", item);
            data.title = getTagValue("title", item);
            data.link = getTagValue("link", item);
            data.author = getTagValue("author", item);
            data.excInsttNm = getTagValue("excInsttNm", item);
            data.description = getTagValue("description", item);
            data.pubDate = getTagValue("pubDate", item);
            data.reqstDt = getTagValue("reqstDt", item);
            data.trgetNm = getTagValue("trgetNm", item);
            data.printFileNm = getTagValue("printFileNm", item);
            data.printFlpthNm = getTagValue("printFlpthNm", item);
            data.fileNm = getTagValue("fileNm", item);        // ✅ 추가
            data.flpthNm = getTagValue("flpthNm", item);      // ✅ 추가
            data.hashTags = getTagValue("hashtags", item);

            result.add(data);
        }

        return result;
    }

    /**
     * XML 태그 값 추출
     */
    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() == 0) {
            return "";
        }

        NodeList childNodes = nodeList.item(0).getChildNodes();
        if (childNodes.getLength() == 0) {
            return "";
        }

        String value = childNodes.item(0).getNodeValue();
        return value != null ? value.trim() : "";
    }

    /**
     * 기술 카테고리 확인
     */
    private boolean isTechCategory(String lcategory) {
        if (lcategory == null || lcategory.isEmpty()) {
            return false;
        }

        // "|" 또는 "@"로 구분된 카테고리에서 "기술" 포함 여부 확인
        String[] parts = lcategory.replace("|", "@").split("@");
        for (String part : parts) {
            if (part.trim().contains("기술")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 해시태그 파싱: "태그1,태그2,태그3" → ["태그1", "태그2", "태그3"]
     */
    private List<String> parseHashtags(String hashtagStr) {
        if (hashtagStr == null || hashtagStr.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        String[] tags = hashtagStr.split(",");

        for (String tag : tags) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    /**
     * 파일 정보 파싱: "파일1@파일2" + "경로1@경로2" → List<FileInfo>
     */
    private List<FileInfo> parseFiles(String fileNames, String filePaths) {
        if (fileNames == null || fileNames.isEmpty() ||
                filePaths == null || filePaths.isEmpty()) {
            return Collections.emptyList();
        }

        String[] names = fileNames.split("@");
        String[] paths = filePaths.split("@");

        if (names.length != paths.length) {
            log.warn("⚠️ 파일명({})과 경로({}) 개수 불일치", names.length, paths.length);
            return Collections.emptyList();
        }

        List<FileInfo> result = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String name = names[i].trim();
            String path = paths[i].trim();

            if (!name.isEmpty() && !path.isEmpty()) {
                result.add(new FileInfo(name, path));
            }
        }

        return result;
    }

    /**
     * 공고 데이터 DTO
     */
    private static class NoticeData {
        String seq;
        String title;
        String link;
        String author;
        String excInsttNm;
        String description;
        String pubDate;
        String reqstDt;
        String trgetNm;
        String printFileNm;
        String printFlpthNm;
        String fileNm;        // ✅ 추가
        String flpthNm;       // ✅ 추가
        String hashTags;
    }

    /**
     * 파일 정보 DTO
     */
    private static class FileInfo {
        String fileName;
        String filePath;

        FileInfo(String fileName, String filePath) {
            this.fileName = fileName;
            this.filePath = filePath;
        }
    }
}