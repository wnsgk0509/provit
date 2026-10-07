import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchTodayFortune } from '../../api/fortuneApi';
import { toggleJobScrap } from '../../api/recruitmentApi';
import { useAuth } from '../../context/AuthContext';
import { Bookmark, RotateCcw, ChevronDown, ChevronUp, Sparkles } from 'lucide-react';
import './Fortune.css';

function Fortune() {
    const navigate = useNavigate();
    const { user } = useAuth();

    const [fortune, setFortune] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [pendingScraps, setPendingScraps] = useState(new Set());
    const [isExpanded, setIsExpanded] = useState(false);

    // 운세 데이터 로드
    const loadFortune = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await fetchTodayFortune();
            if (res && res.data) {
                setFortune(res.data);
            } else {
                setError('운세 데이터를 불러오지 못했습니다.');
            }
        } catch (err) {
            console.error('오늘의 운세 로드 실패:', err);
            setError('운세 정보를 가져오는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadFortune();
    }, []);

    // 공고 스크랩 토글 핸들러
    const handleScrapToggle = async (e, recruitmentNum) => {
        e.stopPropagation();

        if (!user) {
            if (window.confirm('채용 공고 스크랩은 로그인이 필요한 기능입니다. 로그인 페이지로 이동하시겠습니까?')) {
                navigate('/login');
            }
            return;
        }

        if (pendingScraps.has(recruitmentNum)) return;

        // 낙관적 UI 업데이트
        setFortune(prev => {
            if (!prev || !prev.recommendRecruitments) return prev;
            return {
                ...prev,
                recommendRecruitments: prev.recommendRecruitments.map(item =>
                    item.recruitmentNum === recruitmentNum
                        ? { ...item, isScrapped: item.isScrapped === 1 ? 0 : 1 }
                        : item
                )
            };
        });

        setPendingScraps(prev => new Set(prev).add(recruitmentNum));

        try {
            const res = await toggleJobScrap(recruitmentNum);
            if (res && res.data) {
                const finalStatus = res.data.isScrapped !== undefined ? res.data.isScrapped : res.data.scrapped;
                setFortune(prev => {
                    if (!prev || !prev.recommendRecruitments) return prev;
                    return {
                        ...prev,
                        recommendRecruitments: prev.recommendRecruitments.map(item =>
                            item.recruitmentNum === recruitmentNum
                                ? { ...item, isScrapped: finalStatus }
                                : item
                        )
                    };
                });
            }
        } catch (err) {
            console.error('스크랩 토글 실패:', err);
            // 실패 시 원복
            setFortune(prev => {
                if (!prev || !prev.recommendRecruitments) return prev;
                return {
                    ...prev,
                    recommendRecruitments: prev.recommendRecruitments.map(item =>
                        item.recruitmentNum === recruitmentNum
                            ? { ...item, isScrapped: item.isScrapped === 1 ? 0 : 1 }
                            : item
                    )
                };
            });
            alert('스크랩 처리 중 오류가 발생했습니다.');
        } finally {
            setPendingScraps(prev => {
                const next = new Set(prev);
                next.delete(recruitmentNum);
                return next;
            });
        }
    };

    // D-Day 계산 유틸리티
    const getDdayBadge = (expirationDate, closeType) => {
        if (closeType && (closeType.includes('상시') || closeType.includes('채용시'))) {
            return <span className="badge bg-secondary">상시채용</span>;
        }
        if (!expirationDate) {
            return <span className="badge bg-light text-muted border">마감일 미정</span>;
        }
        try {
            const target = new Date(expirationDate);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            target.setHours(0, 0, 0, 0);
            const diffDays = Math.ceil((target - today) / (1000 * 60 * 60 * 24));

            if (diffDays < 0) return <span className="badge bg-dark">마감됨</span>;
            if (diffDays === 0) return <span className="badge bg-danger">오늘 마감</span>;
            if (diffDays <= 3) return <span className="badge bg-warning text-dark">D-{diffDays}</span>;
            return <span className="badge bg-primary-subtle text-primary border border-primary-subtle">D-{diffDays}</span>;
        } catch {
            return <span className="badge bg-secondary">마감일 확인</span>;
        }
    };

    // 오행 배지 색상
    const getElementBadgeClass = (element) => {
        if (!element) return 'bg-secondary';
        if (element.includes('목')) return 'bg-success';
        if (element.includes('화')) return 'bg-danger';
        if (element.includes('토')) return 'bg-warning text-dark';
        if (element.includes('금')) return 'bg-info text-dark';
        if (element.includes('수')) return 'bg-primary';
        return 'bg-secondary';
    };

    if (loading) {
        return (
            <div className="container my-5 text-center py-5">
                <div className="spinner-border text-primary" role="status" style={{ width: '3rem', height: '3rem' }}>
                    <span className="visually-hidden">Loading...</span>
                </div>
                <h5 className="mt-3 text-secondary fw-semibold">오늘의 취업 운세를 분석하고 있습니다...</h5>
                <p className="text-muted small">사주 명리학 데이터와 최신 채용 공고를 결합하는 중입니다.</p>
            </div>
        );
    }

    if (error || !fortune) {
        return (
            <div className="container my-5 text-center py-5">
                <div className="alert alert-danger mx-auto" style={{ maxWidth: '600px' }}>
                    <h5 className="fw-bold mb-2">운세 조회 실패</h5>
                    <p className="mb-3">{error || '데이터를 불러오지 못했습니다.'}</p>
                    <button className="btn btn-outline-danger btn-sm" onClick={loadFortune}>
                        다시 시도하기
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="fortune-page-container my-4">
            {/* 상단 헤더 & 인사말 */}
            <div className="d-flex flex-wrap justify-content-between align-items-center mb-4 pb-2 border-bottom">
                <div>
                    <h2 className="fw-bold mb-1 d-flex align-items-center gap-2">
                        <span>🔮 오늘의 취업 운세</span>
                        <span className="badge bg-primary-subtle text-primary fs-6 fw-normal px-3 py-1 rounded-pill">
                            {fortune.fortuneDate}
                        </span>
                    </h2>
                    <p className="text-muted mb-0">
                        <strong className="text-dark">{fortune.userNickname}</strong>님의 타고난 사주와 오늘 일진의 상호작용을 분석한 맞춤 취업 가이드입니다.
                    </p>
                </div>
                <div className="mt-3 mt-md-0">
                    <button className="btn btn-outline-secondary btn-sm d-flex align-items-center gap-1 shadow-sm" onClick={loadFortune}>
                        <RotateCcw size={14} /> 새로고침
                    </button>
                </div>
            </div>

            {/* 1. 메인 총운 카드 (Hero Section - Solid Modern Navy) */}
            <div className="card fortune-hero-card">
                <div className="fortune-hero-body">
                    <div className="row align-items-center g-4">
                        <div className="col-lg-8">
                            <div className="d-flex justify-content-between align-items-center mb-2">
                                <div className="fortune-eyebrow mb-0">Today's Career Insight</div>
                                <span className="fortune-source-badge">
                                    {fortune.engineSource === 'SAZU_API' ? '⚡ SAZU API 연동' : '⚙️ Provit 자체 사주 엔진'}
                                </span>
                            </div>
                            <div className="fortune-pill-group">
                                <span className="fortune-pill">
                                    나의 일간: <strong>{fortune.dayMaster} ({fortune.dayMasterElement})</strong>
                                </span>
                                <span className="fortune-pill">
                                    오늘의 일진: <strong>{fortune.todayIlju} ({fortune.todayElement})</strong>
                                </span>
                                <span className="fortune-pill fortune-pill-highlight">
                                    십성: <strong>{fortune.tenGodsRelation}</strong>
                                </span>
                            </div>

                            <h3 className="fortune-title">
                                "{fortune.tenGodsMeaning}"
                            </h3>

                            <p className="fortune-summary">
                                {fortune.overallSummary}
                            </p>

                            <div className="fortune-advice-box">
                                <div className="d-flex align-items-start gap-2">
                                    <span className="fs-5">💡</span>
                                    <div>
                                        <div className="fortune-advice-label">오늘의 행동 조언</div>
                                        <p className="fortune-advice-text">{fortune.advice}</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* 운세 총점 스코어 위젯 */}
                        <div className="col-lg-4 text-center ps-lg-3">
                            <div className="fortune-score-widget">
                                <div className="fortune-score-label">취업 활력 지수</div>
                                <div className="fortune-score-value">{fortune.overallScore}</div>
                                <span className="fortune-score-badge">100점 만점</span>
                                <div className="fortune-score-sinsal">
                                    오늘의 신살: <strong className="text-white">{fortune.sinsalName}</strong>
                                    {fortune.sinsalAdvice && <div className="mt-1 opacity-75">{fortune.sinsalAdvice}</div>}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 메인 총운 카드 하단 펼치기/접기 버튼 */}
                <div className="fortune-hero-footer">
                    <button
                        type="button"
                        className="fortune-toggle-btn"
                        onClick={() => setIsExpanded(prev => !prev)}
                        aria-expanded={isExpanded}
                    >
                        <Sparkles size={16} className="text-warning" />
                        <span>
                            {isExpanded
                                ? '오늘의 행운 가이드 및 추천 공고 접기'
                                : '오늘의 행운 가이드 및 추천 공고 펼쳐보기'}
                        </span>
                        {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                    </button>
                </div>
            </div>

            {/* 펼치기 영역: 2. 행운의 가이드 카드 4종 & 3. 오늘의 행운 추천 채용 공고 */}
            {isExpanded && (
                <div className="fortune-expanded-content">
                    {/* 2. 행운의 가이드 카드 4종 */}
                    <div className="row g-3 mb-5">
                {/* 행운의 직무 */}
                <div className="col-6 col-md-3">
                    <div className="fortune-guide-card">
                        <div className="fortune-guide-icon">💼</div>
                        <div className="fortune-guide-label">오늘의 행운 직무</div>
                        <h6 className="fortune-guide-value text-primary">{fortune.luckyJobName}</h6>
                    </div>
                </div>

                {/* 행운의 키워드 */}
                <div className="col-6 col-md-3">
                    <div className="fortune-guide-card">
                        <div className="fortune-guide-icon">🎯</div>
                        <div className="fortune-guide-label">행운의 핵심 키워드</div>
                        <h6 className="fortune-guide-value">{fortune.luckyKeyword}</h6>
                    </div>
                </div>

                {/* 행운의 아이템 */}
                <div className="col-6 col-md-3">
                    <div className="fortune-guide-card">
                        <div className="fortune-guide-icon">🎨</div>
                        <div className="fortune-guide-label">행운 컬러 & 방위</div>
                        <h6 className="fortune-guide-value">{fortune.luckyColor} · {fortune.luckyDirection}</h6>
                    </div>
                </div>

                {/* 행운의 숫자 */}
                <div className="col-6 col-md-3">
                    <div className="fortune-guide-card">
                        <div className="fortune-guide-icon">🍀</div>
                        <div className="fortune-guide-label">행운의 숫자</div>
                        <h6 className="fortune-guide-value text-success">{fortune.luckyNumber}</h6>
                    </div>
                </div>
            </div>

            {/* 3. 운세 기반 오늘의 추천 채용 공고 (최대 6건) */}
            <div className="mb-5">
                <div className="d-flex justify-content-between align-items-end mb-3">
                    <div>
                        <h4 className="fw-bold mb-1 d-flex align-items-center gap-2">
                            <span>🚀 오늘의 행운 추천 공고</span>
                            <span className="badge bg-primary-subtle text-primary fs-6 rounded-pill px-3">
                                #{fortune.luckyJobName}
                            </span>
                        </h4>
                        <p className="text-muted small mb-0">
                            오늘 당신의 십성 기운과 가장 잘 맞는 실시간 활성 채용 공고입니다. (스크랩으로 관심 공고에 저장하세요)
                        </p>
                    </div>
                    <button
                        className="btn btn-link text-decoration-none text-primary fw-semibold btn-sm p-0"
                        onClick={() => navigate('/jobs')}
                    >
                        전체 공고 보러가기 &rarr;
                    </button>
                </div>

                {fortune.recommendRecruitments && fortune.recommendRecruitments.length > 0 ? (
                    <div className="row g-3">
                        {fortune.recommendRecruitments.map((job) => (
                            <div key={job.recruitmentNum} className="col-12 col-md-6 col-lg-4">
                                <div
                                    className="card fortune-job-card"
                                    onClick={() => {
                                        if (job.jobUrl) {
                                            window.open(job.jobUrl, '_blank', 'noopener,noreferrer');
                                        }
                                    }}
                                >
                                    {/* 상단 기업명 & 스크랩 버튼 */}
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                        <span className="fortune-job-company text-truncate pe-2">
                                            🏢 {job.companyName}
                                        </span>
                                        <button
                                            type="button"
                                            className={`job-scrap-btn ${job.isScrapped === 1 ? 'active' : ''}`}
                                            title={job.isScrapped === 1 ? '관심 공고 스크랩 취소' : '관심 공고 스크랩 등록'}
                                            disabled={pendingScraps.has(job.recruitmentNum)}
                                            onClick={(e) => handleScrapToggle(e, job.recruitmentNum)}
                                            aria-label="관심 공고 스크랩"
                                        >
                                            <Bookmark
                                                size={17}
                                                fill={job.isScrapped === 1 ? "currentColor" : "none"}
                                                strokeWidth={job.isScrapped === 1 ? 2.5 : 2}
                                            />
                                        </button>
                                    </div>

                                    {/* 공고 제목 */}
                                    <h6 className="fortune-job-title" title={job.title}>
                                        {job.title}
                                    </h6>

                                    {/* 태그 / 정보 */}
                                    <div className="d-flex flex-wrap gap-1 mb-3">
                                        {job.jobName && (
                                            <span className="fortune-tag-pill">
                                                {job.jobName}
                                            </span>
                                        )}
                                        {job.locationName && (
                                            <span className="fortune-tag-pill">
                                                📍 {job.locationName}
                                            </span>
                                        )}
                                        {job.experienceLevel && (
                                            <span className="fortune-tag-pill">
                                                💼 {job.experienceLevel}
                                            </span>
                                        )}
                                    </div>

                                    {/* 하단 마감일 및 지원하기 버튼 */}
                                    <div className="fortune-job-footer">
                                        <div>
                                            {getDdayBadge(job.expirationDate, job.closeType)}
                                        </div>
                                        <a
                                            href={job.jobUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="btn-fortune-apply"
                                            onClick={(e) => e.stopPropagation()}
                                        >
                                            <span>지원하기</span>
                                            <i className="bi bi-box-arrow-up-right small"></i>
                                        </a>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="card border-0 shadow-sm rounded-3 p-5 text-center text-muted">
                        <p className="mb-0">추천 공고를 준비 중입니다. 전체 채용 공고에서 다양한 공고를 확인해 보세요!</p>
                    </div>
                )}

                {/* 하단 접기 바로가기 버튼 */}
                <div className="text-center mt-4">
                    <button
                        type="button"
                        className="btn-fortune-collapse-bottom"
                        onClick={() => {
                            setIsExpanded(false);
                            window.scrollTo({ top: 0, behavior: 'smooth' });
                        }}
                    >
                        <ChevronUp size={15} />
                        <span>행운 가이드 및 추천 공고 접기</span>
                    </button>
                </div>
            </div>
        </div>
    )}

    {/* 4. 취업 성향 MBTI 테스트 안내 배너 (Solid Modern Slate) */}
            <section className="card fortune-mbti-card mb-4">
                <div className="fortune-mbti-body">
                    <div className="row align-items-center g-3">
                        <div className="col-md-8">
                            <span className="fortune-mbti-badge">
                                취업 준비 성향 테스트
                            </span>
                            <h3 className="fortune-mbti-title">나의 취업 MBTI는 무엇일까요?</h3>
                            <p className="fortune-mbti-desc">
                                12가지 실전문항으로 취업 준비와 면접 상황에서의 나의 성향을 파악하고 강점을 극대화해 보세요.
                            </p>
                        </div>
                        <div className="col-md-4 text-md-end">
                            <button
                                type="button"
                                className="btn btn-fortune-mbti"
                                onClick={() => navigate('/mbti')}
                            >
                                MBTI 검사 시작하기 &rarr;
                            </button>
                        </div>
                    </div>
                </div>
            </section>
        </div>
    );
}

export default Fortune;
