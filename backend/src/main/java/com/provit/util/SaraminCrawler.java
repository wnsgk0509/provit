package com.provit.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.provit.dto.recruitment.RecruitmentDTO;

@Component
public class SaraminCrawler {

    private static final Logger log = LoggerFactory.getLogger(SaraminCrawler.class);

    private static final String BASE_URL = "https://www.saramin.co.kr";
    // 사람인 실시간 인기(조회수/지원자수) 상위 검색 URL (전 직종 대상 '채용' 통합 검색으로 실제 페이징 정상 활성화)
    private static final String SEARCH_URL = "https://www.saramin.co.kr/zf_user/search/recruit?searchType=search&searchword=%EC%B1%84%EC%9A%A9&recruitSort=count&recruitPage=";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 10000;
    private static final int DELAY_MS = 300; // 페이지 요청 간 차단 방지 안전 딜레이

    private static final Pattern DATE_PATTERN = Pattern.compile("~\\s*(\\d{1,2})/(\\d{1,2})");

    /**
     * 사람인 인기 상위 공고 크롤링
     * 
     * @param maxCount 수집할 최대 공고 개수 (예: 1000)
     * @return 수집 및 정제된 RecruitmentDTO 리스트
     */
    public List<RecruitmentDTO> crawlTopRecruitments(int maxCount) {
        List<RecruitmentDTO> recruitments = new ArrayList<>();
        // 검색 결과는 1페이지당 40건 노출됨
        int maxPage = (int) Math.ceil((double) maxCount / 40.0);
        if (maxPage < 1) maxPage = 1;

        log.info(">> [SaraminCrawler] 사람인 인기 상위 공고 크롤링 시작 (목표: 최대 {}건 / {}페이지)", maxCount, maxPage);

        for (int page = 1; page <= maxPage; page++) {
            String targetUrl = SEARCH_URL + page;
            log.info(">> [SaraminCrawler] {}/{} 페이지 수집 요청: {}", page, maxPage, targetUrl);

            try {
                Document doc = Jsoup.connect(targetUrl)
                        .userAgent(USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                        .timeout(TIMEOUT_MS)
                        .maxBodySize(0) // 무제한 수신 (사람인 대용량 2.5MB HTML 잘림 방지)
                        .get();

                Elements items = doc.select("div.item_recruit");
                if (items.isEmpty()) {
                    log.warn(">> [SaraminCrawler] {}페이지에서 공고 요소를 찾지 못해 수집을 종료합니다.", page);
                    break;
                }

                for (Element item : items) {
                    RecruitmentDTO dto = parseItem(item);
                    if (dto != null) {
                        recruitments.add(dto);
                        if (recruitments.size() >= maxCount) {
                            break;
                        }
                    }
                }

                if (recruitments.size() >= maxCount) {
                    log.info(">> [SaraminCrawler] 목표 수량({}건)에 도달하여 수집을 마칩니다.", maxCount);
                    break;
                }

                // 사람인 서버 봇 감지 방지를 위한 미세 딜레이
                Thread.sleep(DELAY_MS);

            } catch (IOException e) {
                log.error(">> [SaraminCrawler] {}페이지 수집 중 네트워크 오류 발생: {}", page, e.getMessage());
                // 네트워크 일시 오류 시 다음 페이지 또는 현재까지 수집본 유지
                break;
            } catch (InterruptedException e) {
                log.warn(">> [SaraminCrawler] 크롤링 스레드 인터럽트 발생");
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info(">> [SaraminCrawler] 크롤링 완료: 총 {}건 수집됨", recruitments.size());
        return recruitments;
    }

    /**
     * 단일 공고 카드 HTML 요소 파싱
     */
    private RecruitmentDTO parseItem(Element item) {
        try {
            // 1. 사람인 공고 고유 ID
            String saraminJobId = item.attr("value");
            if (saraminJobId == null || saraminJobId.trim().isEmpty()) {
                saraminJobId = item.select(".icon_scrap_star").attr("rec_idx");
            }
            if (saraminJobId == null || saraminJobId.trim().isEmpty()) {
                return null;
            }
            saraminJobId = saraminJobId.trim();

            // 2. 기업명
            Element corpElem = item.selectFirst("div.area_corp strong.corp_name a");
            if (corpElem == null) {
                corpElem = item.selectFirst("div.area_corp a");
            }
            String companyName = corpElem != null ? corpElem.text().trim() : "회사명 미기재";
            if (companyName.length() > 60) companyName = companyName.substring(0, 60);

            // 3. 공고 제목
            Element titleElem = item.selectFirst("h2.job_tit a");
            if (titleElem == null) {
                return null;
            }
            String title = titleElem.hasAttr("title") && !titleElem.attr("title").trim().isEmpty() 
                    ? titleElem.attr("title").trim() 
                    : titleElem.text().trim();
            if (title.length() > 120) title = title.substring(0, 120);

            // 4. 공고 원본 URL
            String jobUrl = titleElem.attr("href");
            if (!jobUrl.startsWith("http")) {
                jobUrl = BASE_URL + jobUrl;
            }

            // 5. 근무지 & 경력 요건
            String locationName = "";
            String experienceLevel = "";
            Elements conditions = item.select("div.job_condition span");
            if (conditions.size() > 0) {
                locationName = conditions.get(0).text().trim();
                if (locationName.length() > 60) locationName = locationName.substring(0, 60);
            }
            if (conditions.size() > 1) {
                experienceLevel = conditions.get(1).text().trim();
                if (experienceLevel.length() > 30) experienceLevel = experienceLevel.substring(0, 30);
            }

            // 6. 직무/키워드 태그
            List<String> sectorList = new ArrayList<>();
            Elements sectorTags = item.select("div.job_sector a");
            for (Element tag : sectorTags) {
                String txt = tag.text().trim();
                if (!txt.isEmpty()) {
                    sectorList.add(txt);
                }
            }
            String jobName = String.join(", ", sectorList);
            if (jobName.isEmpty()) {
                Element sectorElem = item.selectFirst("div.job_sector");
                if (sectorElem != null) {
                    jobName = sectorElem.text().replace("외", "").trim();
                }
            }
            if (jobName.length() > 90) {
                jobName = jobName.substring(0, 87) + "...";
            }

            // 7. 마감일자 및 마감 형태 파싱
            Element dateElem = item.selectFirst("div.job_date span.date");
            String dateText = dateElem != null ? dateElem.text().trim() : "";
            Date expirationDate = parseExpirationDate(dateText);
            String closeType = determineCloseType(dateText);

            return RecruitmentDTO.builder()
                    .saraminJobId(saraminJobId)
                    .companyName(companyName)
                    .title(title)
                    .jobUrl(jobUrl)
                    .locationName(locationName)
                    .jobName(jobName)
                    .experienceLevel(experienceLevel)
                    .expirationDate(expirationDate)
                    .closeType(closeType)
                    .isActive(1)
                    .build();

        } catch (Exception e) {
            log.warn(">> [SaraminCrawler] 공고 파싱 중 예외 발생 (스킵): {}", e.getMessage());
            return null;
        }
    }

    /**
     * 마감일 텍스트 -> Date 파싱
     */
    private Date parseExpirationDate(String dateText) {
        if (dateText == null || dateText.isEmpty()) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        if (dateText.contains("오늘마감")) {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        }

        Matcher matcher = DATE_PATTERN.matcher(dateText);
        if (matcher.find()) {
            try {
                int month = Integer.parseInt(matcher.group(1));
                int day = Integer.parseInt(matcher.group(2));

                // 현재 월이 12월인데 공고가 1~2월인 경우 다음 해로 보정
                int currentMonth = cal.get(Calendar.MONTH) + 1;
                if (currentMonth == 12 && month <= 2) {
                    currentYear += 1;
                }

                cal.set(Calendar.YEAR, currentYear);
                cal.set(Calendar.MONTH, month - 1);
                cal.set(Calendar.DAY_OF_MONTH, day);
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTime();
            } catch (Exception e) {
                log.debug("마감일 파싱 에러 (무시): {}", dateText);
            }
        }

        return null;
    }

    /**
     * 마감 형태 결정
     */
    private String determineCloseType(String dateText) {
        if (dateText == null || dateText.isEmpty()) {
            return "상시채용";
        }
        if (dateText.contains("상시")) {
            return "상시채용";
        }
        if (dateText.contains("채용시")) {
            return "채용시";
        }
        if (dateText.contains("오늘마감")) {
            return "오늘마감";
        }
        if (dateText.startsWith("~")) {
            return "접수마감일";
        }
        return dateText;
    }
}
