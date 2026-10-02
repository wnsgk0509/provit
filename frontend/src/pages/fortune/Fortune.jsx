import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchTodayFortune } from '../../api/fortuneApi';
import { toggleJobScrap } from '../../api/recruitmentApi';
import { useAuth } from '../../context/AuthContext';

function Fortune() {
    const navigate = useNavigate();
    const { user } = useAuth();

    const [fortune, setFortune] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [pendingScraps, setPendingScraps] = useState(new Set());

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
        <div className="container my-4" style={{ maxWidth: '1100px' }}>
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
                        <i className="bi bi-arrow-clockwise"></i> 새로고침
                    </button>
                </div>
            </div>

            {/* 1. 메인 총운 카드 (Hero Section) */}
            <div className="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden">
                <div className="card-body p-4 p-md-5" style={{ background: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 100%)', color: '#fff' }}>
                    <div className="row align-items-center">
                        <div className="col-lg-8">
                            <div className="d-flex flex-wrap gap-2 mb-3">
                                <span className={`badge ${getElementBadgeClass(fortune.dayMasterElement)} px-3 py-2 rounded-pill`}>
                                    나의 일간: {fortune.dayMaster}
                                </span>
                                <span className="badge bg-light text-dark px-3 py-2 rounded-pill">
                                    오늘의 일진: {fortune.todayIlju} ({fortune.todayElement})
                                </span>
                                <span className="badge bg-warning text-dark px-3 py-2 rounded-pill fw-bold">
                                    십성: {fortune.tenGodsRelation}
                                </span>
                            </div>

                            <h3 className="fw-bold mb-3" style={{ lineHeight: '1.4' }}>
                                "{fortune.tenGodsMeaning}"
                            </h3>

                            <p className="fs-6 opacity-90 mb-4" style={{ lineHeight: '1.7' }}>
                                {fortune.overallSummary}
                            </p>

                            <div className="p-3 rounded-3" style={{ backgroundColor: 'rgba(255, 255, 255, 0.12)', backdropFilter: 'blur(8px)' }}>
                                <div className="d-flex align-items-start gap-2">
                                    <span className="fs-5">💡</span>
                                    <div>
                                        <strong className="text-warning">오늘의 행동 조언:</strong>
                                        <p className="mb-0 mt-1 small opacity-90">{fortune.advice}</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* 운세 총점 스코어 */}
                        <div className="col-lg-4 text-center mt-4 mt-lg-0 border-lg-start ps-lg-4">
                            <div className="d-inline-flex flex-column align-items-center justify-content-center p-4 rounded-circle bg-white text-dark shadow-lg" style={{ width: '170px', height: '170px' }}>
                                <span className="text-muted small fw-semibold">취업 활력 지수</span>
                                <span className="display-4 fw-black text-primary my-1">{fortune.overallScore}</span>
                                <span className="badge bg-success-subtle text-success fw-bold">100점 만점</span>
                            </div>
                            <div className="mt-3 small text-white-50">
                                신살: <strong className="text-white">{fortune.sinsalName}</strong> ({fortune.sinsalAdvice})
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* 2. 행운의 가이드 카드 4종 */}
            <div className="row g-3 mb-5">
                {/* 행운의 직무 */}
                <div className="col-6 col-md-3">
                    <div className="card h-100 border-0 shadow-sm rounded-3 text-center p-3">
                        <div className="fs-3 mb-2">💼</div>
                        <span className="text-muted small fw-semibold">오늘의 행운 직무</span>
                        <h6 className="fw-bold text-primary mt-2 mb-0">{fortune.luckyJobName}</h6>
                    </div>
                </div>

                {/* 행운의 키워드 */}
                <div className="col-6 col-md-3">
                    <div className="card h-100 border-0 shadow-sm rounded-3 text-center p-3">
                        <div className="fs-3 mb-2">🎯</div>
                        <span className="text-muted small fw-semibold">행운의 핵심 키워드</span>
                        <h6 className="fw-bold text-dark mt-2 mb-0">{fortune.luckyKeyword}</h6>
                    </div>
                </div>

                {/* 행운의 아이템 */}
                <div className="col-6 col-md-3">
                    <div className="card h-100 border-0 shadow-sm rounded-3 text-center p-3">
                        <div className="fs-3 mb-2">🎨</div>
                        <span className="text-muted small fw-semibold">행운의 컬러 & 방위</span>
                        <p className="mb-0 small fw-bold text-dark mt-1">{fortune.luckyColor}</p>
                        <span className="text-muted small mt-1">{fortune.luckyDirection}</span>
                    </div>
                </div>

                {/* 행운의 숫자 */}
                <div className="col-6 col-md-3">
                    <div className="card h-100 border-0 shadow-sm rounded-3 text-center p-3">
                        <div className="fs-3 mb-2">🍀</div>
                        <span className="text-muted small fw-semibold">행운의 숫자</span>
                        <h4 className="fw-bold text-success mt-2 mb-0">{fortune.luckyNumber}</h4>
                    </div>
                </div>
            </div>

            {/* 3. 운세 기반 오늘의 추천 채용 공고 (최대 6건) */}
            <div className="mb-5">
                <div className="d-flex justify-content-between align-items-end mb-3">
                    <div>
                        <h4 className="fw-bold mb-1 d-flex align-items-center gap-2">
                            <span>🚀 오늘의 행운 추천 공고</span>
                            <span className="badge bg-danger-subtle text-danger fs-6 rounded-pill px-3">
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
                                    className="card h-100 border-0 shadow-sm rounded-3 p-3 position-relative job-recommend-card"
                                    style={{
                                        cursor: 'pointer',
                                        transition: 'transform 0.2s, box-shadow 0.2s',
                                        backgroundColor: '#fff'
                                    }}
                                    onMouseEnter={(e) => {
                                        e.currentTarget.style.transform = 'translateY(-4px)';
                                        e.currentTarget.style.boxShadow = '0 10px 20px rgba(0,0,0,0.08)';
                                    }}
                                    onMouseLeave={(e) => {
                                        e.currentTarget.style.transform = 'translateY(0)';
                                        e.currentTarget.style.boxShadow = '0 .125rem .25rem rgba(0,0,0,.075)';
                                    }}
                                    onClick={() => {
                                        if (job.jobUrl) {
                                            window.open(job.jobUrl, '_blank', 'noopener,noreferrer');
                                        }
                                    }}
                                >
                                    {/* 상단 기업명 & 스크랩 버튼 */}
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                        <span className="fw-bold text-secondary small text-truncate" style={{ maxWidth: '75%' }}>
                                            {job.companyName}
                                        </span>
                                        <button
                                            type="button"
                                            className="btn btn-sm p-0 border-0 bg-transparent text-danger fs-5"
                                            title={job.isScrapped === 1 ? '스크랩 취소' : '스크랩 저장'}
                                            disabled={pendingScraps.has(job.recruitmentNum)}
                                            onClick={(e) => handleScrapToggle(e, job.recruitmentNum)}
                                        >
                                            {job.isScrapped === 1 ? '❤️' : '🤍'}
                                        </button>
                                    </div>

                                    {/* 공고 제목 */}
                                    <h6 className="fw-bold text-dark mb-2 text-truncate-2" style={{
                                        display: '-webkit-box',
                                        WebkitLineClamp: 2,
                                        WebkitBoxOrient: 'vertical',
                                        overflow: 'hidden',
                                        minHeight: '44px',
                                        lineHeight: '1.4'
                                    }}>
                                        {job.title}
                                    </h6>

                                    {/* 태그 / 정보 */}
                                    <div className="d-flex flex-wrap gap-1 mb-3 mt-auto">
                                        {job.jobName && (
                                            <span className="badge bg-light text-secondary border small">
                                                {job.jobName}
                                            </span>
                                        )}
                                        {job.locationName && (
                                            <span className="badge bg-light text-secondary border small">
                                                {job.locationName}
                                            </span>
                                        )}
                                        {job.experienceLevel && (
                                            <span className="badge bg-light text-secondary border small">
                                                {job.experienceLevel}
                                            </span>
                                        )}
                                    </div>

                                    {/* 하단 마감일 및 지원하기 버튼 */}
                                    <div className="d-flex justify-content-between align-items-center pt-2 mt-auto border-top">
                                        <div>
                                            {getDdayBadge(job.expirationDate, job.closeType)}
                                        </div>
                                        <a
                                            href={job.jobUrl}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            className="btn btn-primary btn-sm px-3 rounded-pill fw-semibold text-nowrap d-inline-flex align-items-center gap-1 shadow-sm"
                                            style={{ fontSize: "0.8rem" }}
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
            </div>

            {/* 4. 취업 성향 MBTI 테스트 안내 배너 (기존 섹션 유지 및 업그레이드) */}
            <section className="card border-0 shadow-sm rounded-4 overflow-hidden mb-4">
                <div className="card-body p-4 p-md-5 text-white" style={{ background: 'linear-gradient(135deg, #4f46e5, #7c3aed)' }}>
                    <div className="row align-items-center">
                        <div className="col-md-8">
                            <span className="badge text-bg-light text-primary mb-3 px-3 py-1 rounded-pill fw-semibold">
                                취업 준비 성향 테스트
                            </span>
                            <h3 className="fw-bold mb-2">나의 취업 MBTI는 무엇일까요?</h3>
                            <p className="mb-md-0 opacity-75">
                                12가지 실전문항으로 취업 준비와 면접 상황에서의 나의 성향을 파악하고 강점을 극대화해 보세요.
                            </p>
                        </div>
                        <div className="col-md-4 text-md-end mt-3 mt-md-0">
                            <button
                                type="button"
                                className="btn btn-light fw-bold text-primary px-4 py-2 rounded-pill shadow-sm"
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
