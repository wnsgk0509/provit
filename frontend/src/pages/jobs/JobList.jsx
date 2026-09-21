import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
    fetchRecruitments, 
    fetchOccupations, 
    fetchJobsByOccupation, 
    syncRecruitments 
} from '../../api/recruitmentApi';
import './JobList.css';

// 주요 지역 필터 옵션
const LOCATIONS = [
    { value: '', label: '지역 전체' },
    { value: '서울', label: '서울' },
    { value: '경기', label: '경기' },
    { value: '인천', label: '인천' },
    { value: '판교', label: '판교·분당' },
    { value: '대전', label: '대전·충청' },
    { value: '대구', label: '대구·경북' },
    { value: '부산', label: '부산·경남' },
    { value: '전국', label: '전국' },
];

// 경력 조건 필터 옵션
const EXPERIENCES = [
    { value: '', label: '경력 전체' },
    { value: '신입', label: '신입' },
    { value: '경력', label: '경력' },
    { value: '경력무관', label: '경력무관' },
];

function JobList() {
    const navigate = useNavigate();

    // 1. 공고 및 페이징 상태
    const [recruitments, setRecruitments] = useState([]);
    const [pageInfo, setPageInfo] = useState({
        currentPage: 1,
        pageSize: 10,
        totalElements: 0,
        totalPages: 1,
        hasNext: false,
        hasPrevious: false
    });
    const [loading, setLoading] = useState(false);
    const [syncing, setSyncing] = useState(false);

    // 2. 직군 / 직무 연쇄 드롭다운 상태
    const [occupations, setOccupations] = useState([]);
    const [selectedOccupation, setSelectedOccupation] = useState('');
    const [jobs, setJobs] = useState([]);
    const [selectedJob, setSelectedJob] = useState('');

    // 3. 검색 쿼리 파라미터 상태
    const [params, setParams] = useState({
        page: 1,
        size: 9, // 한 페이지당 9개 카드 (3x3 그리드)
        keyword: '',
        location: '',
        experienceLevel: ''
    });

    // 4. 검색창 입력 버퍼
    const [keywordInput, setKeywordInput] = useState('');

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
                    hasPrevious: pageData.hasPrevious || false
                });
            }
        } catch (error) {
            console.error('공고 목록 로딩 에러:', error);
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
                console.error('직군 목록 조회 에러:', err);
            }
        };
        loadOccupations();
    }, []);

    // 대분류 직군 선택 변경 시 소분류 직무 목록 동적 로드
    useEffect(() => {
        if (!selectedOccupation) {
            setJobs([]);
            setSelectedJob('');
            return;
        }

        const loadJobs = async () => {
            try {
                const res = await fetchJobsByOccupation(selectedOccupation);
                if (res && res.data) {
                    setJobs(res.data);
                }
            } catch (err) {
                console.error('직무 목록 조회 에러:', err);
            }
        };
        loadJobs();
    }, [selectedOccupation]);

    // 파라미터 변경 시 공고 목록 자동 갱신
    useEffect(() => {
        loadRecruitments();
    }, [loadRecruitments]);

    // ==========================================
    // 🎯 이벤트 핸들러
    // ==========================================

    // 검색 제출
    const handleSearchSubmit = (e) => {
        e.preventDefault();
        setParams(prev => ({
            ...prev,
            page: 1,
            keyword: keywordInput.trim()
        }));
    };

    // 직무 드롭다운 변경 시 검색 키워드에 바로 반영
    const handleJobChange = (e) => {
        const jobName = e.target.value;
        setSelectedJob(jobName);
        setKeywordInput(jobName);
        setParams(prev => ({
            ...prev,
            page: 1,
            keyword: jobName
        }));
    };

    // 필터 초기화
    const handleResetFilters = () => {
        setSelectedOccupation('');
        setSelectedJob('');
        setJobs([]);
        setKeywordInput('');
        setParams({
            page: 1,
            size: 9,
            keyword: '',
            location: '',
            experienceLevel: ''
        });
    };

    // 수동 크롤링 동기화 실행
    const handleManualSync = async () => {
        if (syncing) return;
        if (!window.confirm('사람인에서 실시간 인기 공고 100건을 수집하여 DB를 최신화하시겠습니까? (약 5~10초 소요)')) {
            return;
        }

        setSyncing(true);
        try {
            const res = await syncRecruitments(100);
            alert(res?.data || '동기화가 완료되었습니다.');
            loadRecruitments(); // 목록 새로고침
        } catch (err) {
            alert('동기화 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
        } finally {
            setSyncing(false);
        }
    };

    // 페이지 변경
    const handlePageChange = (newPage) => {
        if (newPage < 1 || newPage > pageInfo.totalPages) return;
        setParams(prev => ({ ...prev, page: newPage }));
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    // AI 모의면접실로 이동
    const handleStartInterview = (job) => {
        navigate('/interview', {
            state: {
                selectedJobTitle: job.title,
                selectedCompanyName: job.companyName,
                selectedJobDuty: job.jobName
            }
        });
    };

    // ==========================================
    // 🏷️ D-Day 뱃지 렌더링 헬퍼
    // ==========================================
    const renderDDayBadge = (expirationDate, closeType) => {
        if (!expirationDate) {
            if (closeType === '채용시') {
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

    // ==========================================
    // 📄 페이징 블록 계산 (5페이지 단위)
    // ==========================================
    const renderPagination = () => {
        const { currentPage, totalPages, hasPrevious, hasNext } = pageInfo;
        if (totalPages <= 1) return null;

        const blockSize = 5;
        const currentBlock = Math.ceil(currentPage / blockSize);
        const startPage = (currentBlock - 1) * blockSize + 1;
        const endPage = Math.min(startPage + blockSize - 1, totalPages);

        const pageNumbers = [];
        for (let i = startPage; i <= endPage; i++) {
            pageNumbers.push(i);
        }

        return (
            <nav className="d-flex justify-content-center my-4" aria-label="Job pagination">
                <ul className="pagination shadow-sm">
                    <li className={`page-item ${!hasPrevious ? 'disabled' : ''}`}>
                        <button 
                            className="page-link" 
                            onClick={() => handlePageChange(currentPage - 1)}
                            disabled={!hasPrevious}
                        >
                            &laquo; 이전
                        </button>
                    </li>

                    {pageNumbers.map(num => (
                        <li key={num} className={`page-item ${num === currentPage ? 'active' : ''}`}>
                            <button className="page-link" onClick={() => handlePageChange(num)}>
                                {num}
                            </button>
                        </li>
                    ))}

                    <li className={`page-item ${!hasNext ? 'disabled' : ''}`}>
                        <button 
                            className="page-link" 
                            onClick={() => handlePageChange(currentPage + 1)}
                            disabled={!hasNext}
                        >
                            다음 &raquo;
                        </button>
                    </li>
                </ul>
            </nav>
        );
    };

    return (
        <div className="job-page-container">
            {/* 1. 상단 배너 헤더 */}
            <div className="job-header-card d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3">
                <div>
                    <h2 className="fw-bold mb-2">🎯 실시간 채용 공고 & 맞춤 면접</h2>
                    <p className="mb-0 text-white-50">
                        사람인의 실시간 인기 채용공고를 탐색하고, 관심 있는 기업의 JD로 1:1 개인화 AI 모의면접을 시작해 보세요!
                    </p>
                </div>
                <div>
                    <button 
                        type="button" 
                        className="btn btn-outline-light btn-sm d-flex align-items-center gap-2 text-nowrap"
                        onClick={handleManualSync}
                        disabled={syncing}
                    >
                        {syncing ? (
                            <>
                                <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                                공고 수집 중...
                            </>
                        ) : (
                            <>
                                <span>🔄</span> 실시간 공고 수집 (100건)
                            </>
                        )}
                    </button>
                </div>
            </div>

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
                                onChange={(e) => setSelectedOccupation(e.target.value)}
                            >
                                <option value="">전체 직군 선택</option>
                                {occupations.map(occ => (
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
                                <option value="">{selectedOccupation ? '세부 직무 선택' : '직군을 먼저 선택하세요'}</option>
                                {jobs.map(job => (
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
                                onChange={(e) => setParams(prev => ({ ...prev, page: 1, location: e.target.value }))}
                            >
                                {LOCATIONS.map(loc => (
                                    <option key={loc.value} value={loc.value}>{loc.label}</option>
                                ))}
                            </select>
                        </div>

                        {/* 경력 필터 */}
                        <div className="col-md-3">
                            <label className="form-label small fw-bold text-muted">경력 조건</label>
                            <select 
                                className="form-select form-select-sm"
                                value={params.experienceLevel}
                                onChange={(e) => setParams(prev => ({ ...prev, page: 1, experienceLevel: e.target.value }))}
                            >
                                {EXPERIENCES.map(exp => (
                                    <option key={exp.value} value={exp.value}>{exp.label}</option>
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
                            </div>
                        </div>
                    </div>
                </form>
            </div>

            {/* 3. 검색 결과 요약 */}
            <div className="d-flex justify-content-between align-items-center mb-3 px-1">
                <span className="small text-muted">
                    총 <strong className="text-primary">{pageInfo.totalElements.toLocaleString()}</strong>건의 채용 공고
                </span>
                {params.keyword && (
                    <span className="badge bg-light text-dark border">
                        검색어: "{params.keyword}"
                    </span>
                )}
            </div>

            {/* 4. 공고 카드 그리드 */}
            {loading ? (
                <div className="text-center py-5">
                    <div className="spinner-border text-primary mb-3" role="status"></div>
                    <p className="text-muted">실시간 채용 정보를 불러오는 중입니다...</p>
                </div>
            ) : recruitments.length === 0 ? (
                <div className="card shadow-sm border-0 text-center py-5">
                    <div className="card-body">
                        <span style={{ fontSize: '3rem' }}>📂</span>
                        <h5 className="mt-3 fw-bold">등록된 공고가 없습니다.</h5>
                        <p className="text-muted">
                            조건에 일치하는 공고가 없거나 아직 데이터가 수집되지 않았습니다.
                        </p>
                        <button 
                            type="button" 
                            className="btn btn-primary btn-sm mt-2"
                            onClick={handleManualSync}
                        >
                            🔄 사람인 실시간 공고 수집하기
                        </button>
                    </div>
                </div>
            ) : (
                <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
                    {recruitments.map(job => (
                        <div key={job.recruitmentNum} className="col">
                            <div className="card job-card h-100">
                                <div className="card-body d-flex flex-column p-4">
                                    {/* 상단: 회사명 + D-Day 뱃지 */}
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                        <span className="job-card-company text-truncate pe-2">
                                            🏢 {job.companyName}
                                        </span>
                                        {renderDDayBadge(job.expirationDate, job.closeType)}
                                    </div>

                                    {/* 제목 */}
                                    <h5 className="job-card-title mb-3" title={job.title}>
                                        {job.title}
                                    </h5>

                                    {/* 근무지 & 경력 메타 태그 */}
                                    <div className="d-flex flex-wrap gap-1 mb-3">
                                        {job.locationName && (
                                            <span className="badge bg-light text-secondary border">
                                                📍 {job.locationName}
                                            </span>
                                        )}
                                        {job.experienceLevel && (
                                            <span className="badge bg-light text-secondary border">
                                                💼 {job.experienceLevel}
                                            </span>
                                        )}
                                    </div>

                                    {/* 직무 / 기술 스택 태그 */}
                                    <div className="mt-auto pt-2">
                                        {job.jobName ? (
                                            job.jobName.split(',').slice(0, 4).map((tag, idx) => (
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
                                        className="btn btn-primary btn-sm flex-fill text-nowrap fw-bold"
                                        onClick={() => handleStartInterview(job)}
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
            {!loading && renderPagination()}
        </div>
    );
}

export default JobList;
