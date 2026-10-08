import React, { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
    fetchRecruitments,
    fetchOccupations,
    fetchJobsByOccupation,
    syncRecruitments,
    toggleJobScrap,
    fetchUserJobRecommendations,
} from "../../api/recruitmentApi";
import { useAuth } from "../../context/AuthContext";
import { useModal } from "../../context/ModalContext";
import { Pagination } from "../../components/common/Pagination";
import { Bookmark, RotateCcw, X } from "lucide-react";
import "./JobList.css";

// 주요 지역 필터 옵션 (대한민국 17개 표준 시·도 정규화)
const LOCATIONS = [
    { value: "", label: "지역 전체" },
    { value: "서울", label: "서울" },
    { value: "경기", label: "경기" },
    { value: "인천", label: "인천" },
    { value: "대전", label: "대전" },
    { value: "세종", label: "세종" },
    { value: "충북", label: "충북 (청주·충주 등)" },
    { value: "충남", label: "충남 (천안·아산 등)" },
    { value: "광주", label: "광주" },
    { value: "전북", label: "전북 (전주·익산 등)" },
    { value: "전남", label: "전남 (여수·순천 등)" },
    { value: "대구", label: "대구" },
    { value: "경북", label: "경북 (포항·구미 등)" },
    { value: "부산", label: "부산" },
    { value: "울산", label: "울산" },
    { value: "경남", label: "경남 (창원·김해 등)" },
    { value: "강원", label: "강원" },
    { value: "제주", label: "제주" },
];

// 경력 조건 필터 옵션
const EXPERIENCES = [
    { value: "", label: "경력 전체" },
    { value: "신입", label: "신입" },
    { value: "경력", label: "경력" },
    { value: "경력무관", label: "경력무관" },
];

/**
 * 공고 목록 내 특정 공고의 스크랩 상태를 불변성을 유지하며 갱신하는 순수 헬퍼 함수
 * @param {Array} list - 공고 리스트
 * @param {number|string} targetId - 대상 recruitmentNum
 * @param {boolean|null} forcedStatus - 명시적 확정 상태 (null이면 기존 상태 반전)
 */
const updateScrapStatusInList = (list, targetId, forcedStatus = null) => {
    if (!Array.isArray(list)) return list;
    return list.map((job) => {
        if (Number(job.recruitmentNum) !== Number(targetId)) return job;
        const nextStatus =
            forcedStatus !== null && forcedStatus !== undefined ? Boolean(forcedStatus) : !job.isScrapped;
        return {
            ...job,
            isScrapped: nextStatus,
        };
    });
};

function JobList() {
    const navigate = useNavigate();
    const { isLoggedIn, user } = useAuth();
    const { showAlert, showConfirm, showToast } = useModal();

    // 0. 회원 직무 맞춤 / 실시간 인기 추천 공고 상태 및 동시성 요청 제어 ref
    const [recommendation, setRecommendation] = useState(null);
    const [recommendLoading, setRecommendLoading] = useState(true);
    const recommendReqIdRef = useRef(0);

    // 1. 공고 및 페이징 상태
    const [recruitments, setRecruitments] = useState([]);
    const [pageInfo, setPageInfo] = useState({
        currentPage: 1,
        pageSize: 10,
        totalElements: 0,
        totalPages: 1,
        hasNext: false,
        hasPrevious: false,
    });
    const [loading, setLoading] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [pendingScraps, setPendingScraps] = useState(() => new Set());

    // 2. 직군 / 직무 연쇄 드롭다운 상태
    const [occupations, setOccupations] = useState([]);
    const [selectedOccupation, setSelectedOccupation] = useState("");
    const [jobs, setJobs] = useState([]);
    const [selectedJob, setSelectedJob] = useState("");

    // 3. 검색 쿼리 파라미터 상태
    const [params, setParams] = useState({
        page: 1,
        size: 9, // 한 페이지당 9개 카드 (3x3 그리드)
        occupationCode: "",
        keyword: "",
        location: "",
        experienceLevel: "",
        scrapOnly: false,
        sortBy: "LATEST",
    });

    // 4. 검색창 입력 버퍼
    const [keywordInput, setKeywordInput] = useState("");

    // ==========================================
    // 📡 데이터 로드 함수들
    // ==========================================

    // 공고 목록 로드
    const loadRecruitments = useCallback(async () => {
        setLoading(true);
        try {
            const response = await fetchRecruitments(params);
            if (response && response.data) {
                const pageData = response.data;
                setRecruitments(pageData.content || []);
                setPageInfo({
                    currentPage: pageData.currentPage || 1,
                    pageSize: pageData.pageSize || 9,
                    totalElements: pageData.totalElements || 0,
                    totalPages: pageData.totalPages || 1,
                    hasNext: pageData.hasNext || false,
                    hasPrevious: pageData.hasPrevious || false,
                });
            }
        } catch (error) {
            console.error("공고 목록 로딩 에러:", error);
        } finally {
            setLoading(false);
        }
    }, [params]);

    // 초기 마운트 시 대분류 직군 목록 로드
    useEffect(() => {
        const loadOccupations = async () => {
            try {
                const res = await fetchOccupations();
                if (res && res.data) {
                    setOccupations(res.data);
                }
            } catch (err) {
                console.error("직군 목록 조회 에러:", err);
            }
        };
        loadOccupations();
    }, []);

    // 대분류 직군 선택 변경 시 소분류 직무 목록 동적 로드
    useEffect(() => {
        if (!selectedOccupation) {
            setJobs([]);
            setSelectedJob("");
            return;
        }

        const loadJobs = async () => {
            try {
                const res = await fetchJobsByOccupation(selectedOccupation);
                if (res && res.data) {
                    setJobs(res.data);
                }
            } catch (err) {
                console.error("직무 목록 조회 에러:", err);
            }
        };
        loadJobs();
    }, [selectedOccupation]);

    // 파라미터 변경 시 공고 목록 자동 갱신
    useEffect(() => {
        loadRecruitments();
    }, [loadRecruitments]);

    // 회원 직무 맞춤 / 실시간 인기 추천 공고 로드
    const loadRecommendations = useCallback(async () => {
        const reqId = ++recommendReqIdRef.current;
        setRecommendLoading(true);
        // [정합성/보안] 계정 전환 또는 로그아웃 시 이전 사용자 데이터가 화면에 잔류하지 않도록 즉시 초기화
        setRecommendation(null);

        try {
            const res = await fetchUserJobRecommendations();
            // 최신 요청인 경우에만 상태 반영 (네트워크 응답 지연/역전 방어)
            if (reqId === recommendReqIdRef.current) {
                if (res && res.data) {
                    setRecommendation(res.data);
                } else {
                    setRecommendation(null);
                }
            }
        } catch (err) {
            if (reqId === recommendReqIdRef.current) {
                console.error("맞춤 추천 공고 조회 에러:", err);
                // API 실패 시에도 이전 사용자의 데이터가 잔류하지 않도록 안전하게 초기화
                setRecommendation(null);
            }
        } finally {
            if (reqId === recommendReqIdRef.current) {
                setRecommendLoading(false);
            }
        }
    }, []);

    // 초기 마운트 및 로그인 상태/계정 변경 시 추천 공고 갱신
    useEffect(() => {
        loadRecommendations();
    }, [loadRecommendations, isLoggedIn, user?.userNum]);

    // ==========================================
    // 🎯 이벤트 핸들러
    // ==========================================

    // 검색 제출
    const handleSearchSubmit = (e) => {
        e.preventDefault();
        setParams((prev) => ({
            ...prev,
            page: 1,
            keyword: keywordInput.trim(),
        }));
    };

    // 대분류 직군 변경 시: 하위 세부 직무 목록 로드 트리거 + 직군 코드(occupationCode)로 DB 관계 기반 포괄 검색
    const handleOccupationChange = (e) => {
        const occCode = e.target.value;
        setSelectedOccupation(occCode);
        setSelectedJob(""); // 직군이 변경되면 이전 세부 직무 선택 초기화
        setKeywordInput(""); // 검색창 입력값 초기화

        if (!occCode) {
            // 직군을 '전체'로 푼 경우 직군 코드 및 키워드 초기화
            setParams((prev) => ({
                ...prev,
                page: 1,
                occupationCode: "",
                keyword: "",
            }));
            return;
        }

        // 방안 1 적용: 대분류는 DB 관계 기반 포괄 검색(occupationCode)으로 전달
        setParams((prev) => ({
            ...prev,
            page: 1,
            occupationCode: occCode,
            keyword: "",
        }));
    };

    // 소분류 직무 드롭다운 변경 시 검색 키워드에 반영 (부모 occupationCode는 유지)
    const handleJobChange = (e) => {
        const jobName = e.target.value;
        setSelectedJob(jobName);

        if (!jobName) {
            // 세부 직무를 '전체'로 선택한 경우: 키워드를 비워 대분류 직군 전체 포괄 검색으로 복원
            setKeywordInput("");
            setParams((prev) => ({
                ...prev,
                page: 1,
                keyword: "",
            }));
            return;
        }

        setKeywordInput(jobName);
        setParams((prev) => ({
            ...prev,
            page: 1,
            keyword: jobName,
        }));
    };

    // 필터 초기화
    const handleResetFilters = () => {
        setSelectedOccupation("");
        setSelectedJob("");
        setJobs([]);
        setKeywordInput("");
        setParams({
            page: 1,
            size: 9,
            occupationCode: "",
            keyword: "",
            location: "",
            experienceLevel: "",
            scrapOnly: false,
            sortBy: "LATEST",
        });
    };

    // 정렬 기준 변경 핸들러 (최신순, 마감임박순, 인기/스크랩순)
    const handleSortChange = (newSort) => {
        if (params.sortBy === newSort) return;
        setParams((prev) => ({
            ...prev,
            page: 1,
            sortBy: newSort,
        }));
    };

    // 개별 필터 칩 제거 핸들러들
    const handleRemoveOccupation = () => {
        setSelectedOccupation("");
        setSelectedJob("");
        setJobs([]);
        setKeywordInput("");
        setParams((prev) => ({
            ...prev,
            page: 1,
            occupationCode: "",
            keyword: "",
        }));
    };

    const handleRemoveJob = () => {
        setSelectedJob("");
        setKeywordInput("");
        setParams((prev) => ({
            ...prev,
            page: 1,
            keyword: "",
        }));
    };

    const handleRemoveLocation = () => {
        setParams((prev) => ({
            ...prev,
            page: 1,
            location: "",
        }));
    };

    const handleRemoveExperience = () => {
        setParams((prev) => ({
            ...prev,
            page: 1,
            experienceLevel: "",
        }));
    };

    const handleRemoveKeyword = () => {
        setKeywordInput("");
        setParams((prev) => ({
            ...prev,
            page: 1,
            keyword: "",
        }));
    };

    const handleRemoveScrapOnly = () => {
        setParams((prev) => ({
            ...prev,
            page: 1,
            scrapOnly: false,
        }));
    };

    // 적용된 필터가 1개 이상 존재하는지 확인
    const hasActiveFilters = Boolean(
        selectedOccupation ||
        selectedJob ||
        params.location ||
        params.experienceLevel ||
        (params.keyword && params.keyword !== selectedJob) ||
        params.scrapOnly
    );

    // 일반 공고 목록과 맞춤 추천 공고의 스크랩 상태를 일괄 동기화하는 헬퍼 함수
    const updateScrapStatus = useCallback((targetId, forcedStatus = null) => {
        setRecruitments((prevList) => updateScrapStatusInList(prevList, targetId, forcedStatus));
        setRecommendation((prev) => {
            if (!prev || !prev.recruitments) return prev;
            return {
                ...prev,
                recruitments: updateScrapStatusInList(prev.recruitments, targetId, forcedStatus),
            };
        });
    }, []);

    // 관심 공고 스크랩(북마크) 토글 핸들러
    const handleToggleScrap = async (e, recruitmentNum) => {
        e.stopPropagation();
        e.preventDefault();

        if (!isLoggedIn) {
            const ok = await showConfirm(
                "관심 공고 스크랩은 로그인이 필요한 서비스입니다.\n로그인 페이지로 이동하시겠습니까?",
                { title: "로그인 필요", confirmText: "로그인하기" }
            );
            if (ok) {
                navigate("/login");
            }
            return;
        }

        // 1. 이미 요청 진행 중인 공고는 중복 클릭 무시 (광클/더블클릭 방지)
        if (pendingScraps.has(recruitmentNum)) {
            return;
        }

        // 2. Pending 상태 등록
        setPendingScraps((prev) => new Set(prev).add(recruitmentNum));

        // 3. 낙관적 UI 업데이트 (즉시 별 상태 토글로 체감 속도 향상)
        updateScrapStatus(recruitmentNum);

        // 4. 서버 스크랩 토글 API 호출
        try {
            const res = await toggleJobScrap(recruitmentNum);
            if (res && res.data) {
                // isScrapped 또는 scrapped 프로퍼티 호환성 방어 추출
                const serverStatus = res.data.isScrapped !== undefined ? res.data.isScrapped : res.data.scrapped;

                // 5. 서버에서 최종 확정된 isScrapped 상태로 UI 정합성 동기화
                updateScrapStatus(recruitmentNum, serverStatus);

                // 만약 스크랩만 모아보기 상태에서 스크랩을 취소했다면 목록 새로고침
                if (params.scrapOnly && !serverStatus) {
                    loadRecruitments();
                }
            }
        } catch (error) {
            console.error("스크랩 토글 에러:", error);
            // 6. 실패 시 이전 상태로 안전하게 롤백 (재토글로 원복)
            updateScrapStatus(recruitmentNum);
            showToast("스크랩 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", "danger");
        } finally {
            // 7. Pending 상태 해제
            setPendingScraps((prev) => {
                const next = new Set(prev);
                next.delete(recruitmentNum);
                return next;
            });
        }
    };

    // '내 스크랩 공고만 보기' 필터 토글
    const handleToggleScrapOnly = async () => {
        if (!isLoggedIn && !params.scrapOnly) {
            const ok = await showConfirm(
                "스크랩한 공고를 확인하려면 로그인이 필요합니다.\n로그인 페이지로 이동하시겠습니까?",
                { title: "로그인 필요", confirmText: "로그인하기" }
            );
            if (ok) {
                navigate("/login");
            }
            return;
        }
        setParams((prev) => ({
            ...prev,
            page: 1,
            scrapOnly: !prev.scrapOnly,
        }));
    };

    // 수동 크롤링 동기화 실행
    const handleManualSync = async () => {
        if (syncing) return;
        const ok = await showConfirm(
            "사람인에서 실시간 인기 공고 100건을 수집하여 DB를 최신화하시겠습니까? (약 5~10초 소요)",
            { title: "채용공고 최신화", confirmText: "동기화 시작" }
        );
        if (!ok) return;

        setSyncing(true);
        try {
            const res = await syncRecruitments(100);
            showToast(res?.data || "동기화가 완료되었습니다.", "success");
            loadRecruitments(); // 목록 새로고침
            loadRecommendations(); // 추천 공고도 최신 데이터로 새로고침
        } catch (err) {
            showAlert("동기화 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", { type: "error" });
        } finally {
            setSyncing(false);
        }
    };

    // 페이지 변경
    const handlePageChange = (newPage) => {
        if (newPage < 1 || newPage > pageInfo.totalPages) return;
        setParams((prev) => ({ ...prev, page: newPage }));
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    // 선택한 공고 정보는 면접 시작 API를 거쳐 질문 생성·후속 질문·평가에 사용합니다.
    const handleStartInterview = (job) => {
        navigate("/interview", {
            state: {
                recruitment: {
                    recruitmentNum: job.recruitmentNum,
                    companyName: job.companyName,
                    title: job.title,
                    jobName: job.jobName,
                    locationName: job.locationName,
                    experienceLevel: job.experienceLevel,
                },
            },
        });
    };

    // ==========================================
    // 🏷️ D-Day 뱃지 렌더링 헬퍼
    // ==========================================
    const renderDDayBadge = (expirationDate, closeType) => {
        if (!expirationDate) {
            if (closeType === "채용시") {
                return <span className="badge bg-info text-dark">채용시 마감</span>;
            }
            return <span className="badge badge-dday-always">상시채용</span>;
        }

        const expDate = new Date(expirationDate);
        const today = new Date();
        // 시간차 제거 후 일자만 비교
        expDate.setHours(0, 0, 0, 0);
        today.setHours(0, 0, 0, 0);

        const diffTime = expDate - today;
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays < 0) {
            return <span className="badge bg-secondary">마감</span>;
        }
        if (diffDays === 0) {
            return <span className="badge badge-dday-urgent">오늘마감</span>;
        }
        if (diffDays <= 3) {
            return <span className="badge badge-dday-urgent">D-{diffDays}</span>;
        }
        if (diffDays <= 7) {
            return <span className="badge badge-dday-warning">D-{diffDays}</span>;
        }
        return <span className="badge badge-dday-normal">D-{diffDays}</span>;
    };

    // 마감 임박 공고 (D-Day 1~3일, 오늘마감) 여부 판별 헬퍼
    const isUrgentJob = (expirationDate) => {
        if (!expirationDate) return false;
        const expDate = new Date(expirationDate);
        const today = new Date();
        expDate.setHours(0, 0, 0, 0);
        today.setHours(0, 0, 0, 0);
        const diffDays = Math.ceil((expDate - today) / (1000 * 60 * 60 * 24));
        return diffDays >= 0 && diffDays <= 3;
    };

    return (
        <div className="job-page-container">
            {/* 1. 사용자 직무 맞춤 / 실시간 인기 추천 공고 섹션 */}
            {!recommendLoading &&
                recommendation &&
                recommendation.recruitments &&
                recommendation.recruitments.length > 0 && (
                    <div className="recommend-section">
                        <div className="recommend-header">
                            <div>
                                <div className="d-flex align-items-center gap-2 mb-1">
                                    {recommendation.recommendType === "JOB_MATCH" && (
                                        <span className="recommend-badge">🎯 직무 맞춤 추천</span>
                                    )}
                                    {recommendation.recommendType === "OCCUPATION_MATCH" && (
                                        <span className="recommend-badge">📂 직군 맞춤 추천</span>
                                    )}
                                    {recommendation.recommendType === "POPULAR_FALLBACK" && (
                                        <span className="recommend-badge recommend-badge-popular">🔥 실시간 인기 공고</span>
                                    )}
                                    <h4 className="fw-bold mb-0 text-dark">
                                        {recommendation.recommendType === "JOB_MATCH" && (
                                            <span>
                                                <strong>
                                                    {recommendation.userNickname
                                                        ? `${recommendation.userNickname}님`
                                                        : "회원님"}
                                                </strong>
                                                을 위한
                                                <span className="text-primary ms-1">
                                                    [{recommendation.targetJobName}]
                                                </span>{" "}
                                                맞춤 공고
                                            </span>
                                        )}
                                        {recommendation.recommendType === "OCCUPATION_MATCH" && (
                                            <span>
                                                <strong>
                                                    {recommendation.userNickname
                                                        ? `${recommendation.userNickname}님`
                                                        : "회원님"}
                                                </strong>
                                                의 관심 분야
                                                <span className="text-primary ms-1">
                                                    [{recommendation.targetJobName}]
                                                </span>{" "}
                                                추천 공고
                                            </span>
                                        )}
                                        {recommendation.recommendType === "POPULAR_FALLBACK" && (
                                            <span>지원자들의 관심이 집중된 실시간 인기 공고</span>
                                        )}
                                    </h4>
                                </div>
                                <p className="text-muted small mb-0">
                                    {recommendation.recommendType === "JOB_MATCH" ||
                                    recommendation.recommendType === "OCCUPATION_MATCH"
                                        ? "회원님의 관심 직무에 맞춰 엄선한 공고입니다. 원하는 공고로 즉시 AI 1:1 맞춤 모의면접을 시작해 보세요!"
                                        : "현재 가장 많이 탐색되는 실시간 인기 공고입니다. 원하는 기업의 공고를 선택해 모의면접을 체험해 보세요!"}
                                </p>
                            </div>
                        </div>

                        <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3">
                            {recommendation.recruitments.map((job) => (
                                <div key={`rec-${job.recruitmentNum}`} className="col">
                                    <div className="card recommend-card p-3 d-flex flex-column h-100">
                                        <div className="d-flex justify-content-between align-items-center mb-2">
                                            <div className="d-flex align-items-center gap-1 overflow-hidden" style={{ maxWidth: "70%" }}>
                                                <span className="recommend-pick-tag">✨ Pick</span>
                                                <span
                                                    className="small fw-bold text-truncate text-secondary"
                                                    title={job.companyName}
                                                >
                                                    {job.companyName}
                                                </span>
                                            </div>
                                            <div className="d-flex align-items-center gap-1">
                                                {renderDDayBadge(job.expirationDate, job.closeType)}
                                                <button
                                                    type="button"
                                                    className={`job-scrap-btn ${job.isScrapped ? "active" : ""}`}
                                                    onClick={(e) => handleToggleScrap(e, job.recruitmentNum)}
                                                    disabled={pendingScraps.has(job.recruitmentNum)}
                                                    title={
                                                        job.isScrapped
                                                            ? "관심 공고 스크랩 취소"
                                                            : "관심 공고 스크랩 등록"
                                                    }
                                                    aria-label="관심 공고 스크랩"
                                                >
                                                    <Bookmark
                                                        size={17}
                                                        fill={job.isScrapped ? "currentColor" : "none"}
                                                        strokeWidth={job.isScrapped ? 2.5 : 2}
                                                    />
                                                </button>
                                            </div>
                                        </div>

                                        <h6
                                            className="fw-bold mb-2 text-dark text-truncate"
                                            title={job.title}
                                            style={{ lineHeight: 1.4 }}
                                        >
                                            {job.title}
                                        </h6>

                                        <div className="d-flex flex-wrap gap-1 mb-2">
                                            {job.locationName && (
                                                <span className="job-meta-pill" style={{ fontSize: "0.72rem", padding: "0.15rem 0.45rem" }}>
                                                    📍 {job.locationName}
                                                </span>
                                            )}
                                            {job.experienceLevel && (
                                                <span className="job-meta-pill" style={{ fontSize: "0.72rem", padding: "0.15rem 0.45rem" }}>
                                                    💼 {job.experienceLevel}
                                                </span>
                                            )}
                                        </div>

                                        <div className="mt-auto pt-2 d-flex gap-2">
                                            <a
                                                href={job.jobUrl}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                className="btn btn-outline-secondary btn-sm flex-fill text-nowrap"
                                                style={{ fontSize: "0.75rem" }}
                                            >
                                                사람인 ↗
                                            </a>
                                            <button
                                                type="button"
                                                className="btn btn-interview-cta btn-sm flex-fill text-nowrap fw-bold"
                                                style={{ fontSize: "0.75rem" }}
                                                onClick={() => handleStartInterview(job)}
                                                title="선택한 공고의 요구 역량(JD)으로 1:1 맞춤 AI 모의면접을 시작합니다"
                                            >
                                                🎙️ 모의면접
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

            {/* 2. 스마트 필터 & 검색 카드 */}
            <div className="filter-card">
                <form onSubmit={handleSearchSubmit}>
                    <div className="row g-3">
                        {/* 직군 연쇄 드롭다운 (Step 1 연동) */}
                        <div className="col-md-3">
                            <label className="form-label small fw-bold text-muted">대분류 직군</label>
                            <select
                                className="form-select form-select-sm"
                                value={selectedOccupation}
                                onChange={handleOccupationChange}
                            >
                                <option value="">전체 직군 선택</option>
                                {occupations.map((occ) => (
                                    <option key={occ.occupationCode} value={occ.occupationCode}>
                                        {occ.occupationName}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* 세부 직무 드롭다운 */}
                        <div className="col-md-3">
                            <label className="form-label small fw-bold text-muted">소분류 세부 직무</label>
                            <select
                                className="form-select form-select-sm"
                                value={selectedJob}
                                onChange={handleJobChange}
                                disabled={!selectedOccupation || jobs.length === 0}
                            >
                                <option value="">
                                    {selectedOccupation ? "세부 직무 전체" : "직군을 먼저 선택하세요"}
                                </option>
                                {jobs.map((job) => (
                                    <option key={job.jobCode} value={job.jobName}>
                                        {job.jobName}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* 지역 필터 */}
                        <div className="col-md-3">
                            <label className="form-label small fw-bold text-muted">근무 지역</label>
                            <select
                                className="form-select form-select-sm"
                                value={params.location}
                                onChange={(e) => setParams((prev) => ({ ...prev, page: 1, location: e.target.value }))}
                            >
                                {LOCATIONS.map((loc) => (
                                    <option key={loc.value} value={loc.value}>
                                        {loc.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* 경력 필터 */}
                        <div className="col-md-3">
                            <label className="form-label small fw-bold text-muted">경력 조건</label>
                            <select
                                className="form-select form-select-sm"
                                value={params.experienceLevel}
                                onChange={(e) =>
                                    setParams((prev) => ({ ...prev, page: 1, experienceLevel: e.target.value }))
                                }
                            >
                                {EXPERIENCES.map((exp) => (
                                    <option key={exp.value} value={exp.value}>
                                        {exp.label}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* 키워드 검색바 */}
                        <div className="col-12 mt-3">
                            <div className="input-group">
                                <span className="input-group-text bg-white">🔍</span>
                                <input
                                    type="text"
                                    className="form-control"
                                    placeholder="회사명, 채용공고 제목, 요구 기술 스택(Java, Spring, React 등)을 검색해 보세요"
                                    value={keywordInput}
                                    onChange={(e) => setKeywordInput(e.target.value)}
                                />
                                <button type="submit" className="btn btn-primary px-4 fw-bold">
                                    검색
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary px-3"
                                    onClick={handleResetFilters}
                                >
                                    초기화
                                </button>
                                <button
                                    type="button"
                                    className={`btn px-3 text-nowrap fw-bold ${params.scrapOnly ? "btn-warning text-dark" : "btn-outline-warning text-dark"}`}
                                    onClick={handleToggleScrapOnly}
                                    title={params.scrapOnly ? "전체 공고 보기" : "내가 스크랩한 관심 공고만 모아보기"}
                                >
                                    {params.scrapOnly ? "⭐ 스크랩 모아보기 중" : "☆ 내 스크랩 공고"}
                                </button>
                                {user?.userType === "ADMIN" && (
                                    <button
                                        type="button"
                                        className="btn btn-outline-primary px-3 text-nowrap d-flex align-items-center gap-1"
                                        onClick={handleManualSync}
                                        disabled={syncing}
                                        title="사람인 실시간 공고 수집"
                                    >
                                        {syncing ? (
                                            <>
                                                <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                                                수집 중...
                                            </>
                                        ) : (
                                            <>
                                                <RotateCcw size={14} /> 공고 수집
                                            </>
                                        )}
                                    </button>
                                )}
                            </div>
                        </div>
                    </div>
                </form>
            </div>

            {/* 2-1. 활성화된 필터 태그 칩 바 */}
            {hasActiveFilters && (
                <div className="active-filter-chips d-flex align-items-center flex-wrap gap-2 mb-3">
                    <span className="small text-muted fw-bold d-flex align-items-center me-1">
                        적용된 필터:
                    </span>
                    {selectedOccupation && (
                        <span className="filter-chip">
                            <span>
                                📂{" "}
                                {occupations.find((o) => String(o.occupationCode) === String(selectedOccupation))
                                    ?.occupationName || "직군"}
                            </span>
                            <button type="button" onClick={handleRemoveOccupation} title="직군 필터 해제">
                                <X size={13} />
                            </button>
                        </span>
                    )}
                    {selectedJob && (
                        <span className="filter-chip">
                            <span>💼 {selectedJob}</span>
                            <button type="button" onClick={handleRemoveJob} title="직무 필터 해제">
                                <X size={13} />
                            </button>
                        </span>
                    )}
                    {params.location && (
                        <span className="filter-chip">
                            <span>📍 {LOCATIONS.find((l) => l.value === params.location)?.label || params.location}</span>
                            <button type="button" onClick={handleRemoveLocation} title="지역 필터 해제">
                                <X size={13} />
                            </button>
                        </span>
                    )}
                    {params.experienceLevel && (
                        <span className="filter-chip">
                            <span>
                                🎯{" "}
                                {EXPERIENCES.find((e) => e.value === params.experienceLevel)?.label ||
                                    params.experienceLevel}
                            </span>
                            <button type="button" onClick={handleRemoveExperience} title="경력 필터 해제">
                                <X size={13} />
                            </button>
                        </span>
                    )}
                    {params.keyword && params.keyword !== selectedJob && (
                        <span className="filter-chip">
                            <span>🔍 "{params.keyword}"</span>
                            <button type="button" onClick={handleRemoveKeyword} title="검색어 제거">
                                <X size={13} />
                            </button>
                        </span>
                    )}
                    {params.scrapOnly && (
                        <span className="filter-chip filter-chip-scrap">
                            <span>⭐ 관심 공고만 보기</span>
                            <button type="button" onClick={handleRemoveScrapOnly} title="스크랩 모아보기 해제">
                                <X size={13} />
                            </button>
                        </span>
                    )}
                    <button
                        type="button"
                        className="btn btn-link btn-sm text-decoration-none p-0 ms-auto d-flex align-items-center gap-1 filter-reset-link"
                        onClick={handleResetFilters}
                    >
                        <RotateCcw size={12} />
                        <span className="small">전체 초기화</span>
                    </button>
                </div>
            )}

            {/* 3. 검색 결과 건수 요약 및 정렬 탭 */}
            <div className="d-flex justify-content-between align-items-center mb-3 px-1 flex-wrap gap-2">
                <span className="small text-muted">
                    총 <strong className="text-primary">{pageInfo.totalElements.toLocaleString()}</strong>건의 채용 공고
                </span>
                <div className="btn-group btn-group-sm job-sort-group shadow-sm" role="group" aria-label="정렬 기준">
                    <button
                        type="button"
                        className={`btn ${params.sortBy === "LATEST" ? "btn-primary active" : "btn-outline-secondary"}`}
                        onClick={() => handleSortChange("LATEST")}
                        title="채용공고 등록일자 최신순으로 정렬합니다"
                    >
                        최신순
                    </button>
                    <button
                        type="button"
                        className={`btn ${params.sortBy === "CLOSING_SOON" ? "btn-primary active" : "btn-outline-secondary"}`}
                        onClick={() => handleSortChange("CLOSING_SOON")}
                        title="마감기한이 가까운 공고부터 정렬합니다 (채용시마감·상시채용·종료공고 후순위)"
                    >
                        ⏰ 마감 임박순
                    </button>
                </div>
            </div>

            {/* 4. 공고 카드 그리드 & 스켈레톤 로딩 */}
            {loading ? (
                <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
                    {Array.from({ length: params.size || 9 }).map((_, idx) => (
                        <div key={`skeleton-${idx}`} className="col">
                            <div className="card job-card job-skeleton-card h-100">
                                <div className="card-body p-4 d-flex flex-column">
                                    <div className="d-flex justify-content-between align-items-center mb-3">
                                        <div className="skeleton-shimmer skeleton-company"></div>
                                        <div className="skeleton-shimmer skeleton-badge"></div>
                                    </div>
                                    <div className="skeleton-shimmer skeleton-title mb-2"></div>
                                    <div className="skeleton-shimmer skeleton-title-sub mb-3"></div>
                                    <div className="d-flex gap-2 mb-4">
                                        <div className="skeleton-shimmer skeleton-pill"></div>
                                        <div className="skeleton-shimmer skeleton-pill"></div>
                                    </div>
                                    <div className="mt-auto pt-2 d-flex gap-2">
                                        <div className="skeleton-shimmer skeleton-tag"></div>
                                        <div className="skeleton-shimmer skeleton-tag"></div>
                                        <div className="skeleton-shimmer skeleton-tag"></div>
                                    </div>
                                </div>
                                <div className="job-card-footer d-flex gap-2">
                                    <div className="skeleton-shimmer skeleton-btn"></div>
                                    <div className="skeleton-shimmer skeleton-btn"></div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            ) : recruitments.length === 0 ? (
                <div className="card shadow-sm border-0 text-center py-5">
                    <div className="card-body">
                        <span style={{ fontSize: "3rem" }}>📂</span>
                        <h5 className="mt-3 fw-bold">등록된 공고가 없습니다.</h5>
                        <p className="text-muted">조건에 일치하는 공고가 없거나 아직 데이터가 수집되지 않았습니다.</p>
                        <button type="button" className="btn btn-primary btn-sm mt-2" onClick={handleManualSync}>
                            🔄 사람인 실시간 공고 수집하기
                        </button>
                    </div>
                </div>
            ) : (
                <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
                    {recruitments.map((job) => (
                        <div key={job.recruitmentNum} className="col">
                            <div className={`card job-card h-100 ${isUrgentJob(job.expirationDate) ? "urgent-deadline" : ""}`}>
                                <div className="card-body d-flex flex-column p-4">
                                    {/* 상단: 회사명 + D-Day 뱃지 & 관심 공고 스크랩 버튼 */}
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                        <span className="job-card-company text-truncate pe-2">
                                            🏢 {job.companyName}
                                        </span>
                                        <div className="d-flex align-items-center gap-2">
                                            {renderDDayBadge(job.expirationDate, job.closeType)}
                                            <button
                                                type="button"
                                                className={`job-scrap-btn ${job.isScrapped ? "active" : ""}`}
                                                onClick={(e) => handleToggleScrap(e, job.recruitmentNum)}
                                                disabled={pendingScraps.has(job.recruitmentNum)}
                                                title={
                                                    job.isScrapped ? "관심 공고 스크랩 취소" : "관심 공고 스크랩 등록"
                                                }
                                                aria-label="관심 공고 스크랩"
                                            >
                                                <Bookmark
                                                    size={18}
                                                    fill={job.isScrapped ? "currentColor" : "none"}
                                                    strokeWidth={job.isScrapped ? 2.5 : 2}
                                                />
                                            </button>
                                        </div>
                                    </div>

                                    {/* 제목 */}
                                    <h5 className="job-card-title mb-3" title={job.title}>
                                        {job.title}
                                    </h5>

                                    {/* 근무지 & 경력 메타 태그 */}
                                    <div className="d-flex flex-wrap gap-1 mb-3">
                                        {job.locationName && (
                                            <span className="job-meta-pill">
                                                📍 {job.locationName}
                                            </span>
                                        )}
                                        {job.experienceLevel && (
                                            <span className="job-meta-pill">
                                                💼 {job.experienceLevel}
                                            </span>
                                        )}
                                    </div>

                                    {/* 직무 / 기술 스택 태그 */}
                                    <div className="mt-auto pt-2">
                                        {job.jobName ? (
                                            job.jobName
                                                .split(",")
                                                .slice(0, 4)
                                                .map((tag, idx) => (
                                                    <span key={idx} className="job-tag">
                                                        #{tag.trim()}
                                                    </span>
                                                ))
                                        ) : (
                                            <span className="job-tag text-muted">#직무공통</span>
                                        )}
                                    </div>
                                </div>

                                {/* 카드 하단 액션 버튼 */}
                                <div className="job-card-footer d-flex justify-content-between align-items-center gap-2">
                                    <a
                                        href={job.jobUrl}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="btn btn-outline-secondary btn-sm flex-fill text-nowrap"
                                    >
                                        사람인 공고 ↗
                                    </a>
                                    <button
                                        type="button"
                                        className="btn btn-interview-cta btn-sm flex-fill text-nowrap fw-bold"
                                        onClick={() => handleStartInterview(job)}
                                        title="선택한 공고의 요구 역량(JD)으로 1:1 맞춤 AI 모의면접을 시작합니다"
                                    >
                                        🎙️ 모의면접 보기
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* 5. 페이징 네비게이션 */}
            {!loading && (
                <Pagination
                    currentPage={pageInfo.currentPage}
                    totalPages={pageInfo.totalPages}
                    onPageChange={handlePageChange}
                />
            )}
        </div>
    );
}

export default JobList;
