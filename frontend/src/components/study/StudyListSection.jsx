import React, { useState, useEffect } from 'react';
import { fetchStudyList, deleteStudy, joinStudy, leaveStudy } from '../../api/studyApi';
import { useAuth } from '../../context/AuthContext';
import StudyCreateModal from './StudyCreateModal';
import StudyEditModal from './StudyEditModal';
import StudyDetailModal from './StudyDetailModal';

function StudyListSection() {
    const { user, isLoggedIn } = useAuth();
    const [studies, setStudies] = useState([]);
    const [loading, setLoading] = useState(false);
    const [showModal, setShowModal] = useState(false);
    const [editModalData, setEditModalData] = useState(null);
    const [detailModalData, setDetailModalData] = useState(null);

    useEffect(() => {
        loadStudies();
    }, [user]);

    const loadStudies = async () => {
        setLoading(true);
        try {
            const userNum = user ? user.userNum : null;
            const result = await fetchStudyList(userNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setStudies(result.data || []);
            }
        } catch (error) {
            console.error('스터디 목록을 불러오는 중 오류 발생:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (e, studyNum) => {
        e.stopPropagation();
        if (!window.confirm('정말로 이 스터디를 삭제하시겠습니까?\n모든 참여자 정보가 함께 삭제됩니다.')) return;
        try {
            const result = await deleteStudy(studyNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                loadStudies();
            } else {
                alert('삭제에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    const handleJoin = async (e, studyNum) => {
        e.stopPropagation();
        if (!isLoggedIn) {
            alert('로그인 후 참여할 수 있습니다.');
            return;
        }
        try {
            const result = await joinStudy(studyNum, user.userNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                loadStudies();
            } else {
                alert('참여에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    const handleLeave = async (e, studyNum) => {
        e.stopPropagation();
        if (!window.confirm('정말 스터디 참여를 취소하시겠습니까?')) return;
        try {
            const result = await leaveStudy(studyNum, user.userNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                loadStudies();
                // 상세 모달이 열려있다면 닫기 (또는 갱신)
                if (detailModalData && detailModalData.studyNum === studyNum) {
                    setDetailModalData(null);
                }
            } else {
                alert('참여 취소에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    return (
        <div className="study-section">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h4 className="fw-bold mb-2">모집 중인 스터디</h4>
                    <p className="text-muted small mb-0">스터디장은 참여형 단톡방을 만들고 직접 삭제할 수 있습니다.</p>
                </div>
                {isLoggedIn ? (
                    <button 
                        className="btn btn-dark rounded-3 px-4 shadow-sm"
                        onClick={() => setShowModal(true)}
                    >
                        + 스터디 만들기
                    </button>
                ) : (
                    <button 
                        className="btn btn-outline-secondary rounded-3 px-3 btn-sm"
                        onClick={() => alert('로그인 후 스터디를 개설할 수 있습니다.')}
                    >
                        + 스터디 만들기 (로그인 필요)
                    </button>
                )}
            </div>

            {loading ? (
                <div className="text-center py-5">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                </div>
            ) : (
                <div className="row g-4">
                    {studies.length === 0 ? (
                        <div className="col-12 text-center py-5 bg-light rounded-4">
                            <div className="text-muted mb-3 fs-5">아직 개설된 스터디가 없습니다.</div>
                            <button 
                                className="btn btn-outline-primary rounded-pill px-4" 
                                onClick={() => {
                                    if (!isLoggedIn) {
                                        alert('로그인 후 스터디를 개설할 수 있습니다.');
                                    } else {
                                        setShowModal(true);
                                    }
                                }}
                            >
                                첫 스터디의 방장이 되어보세요!
                            </button>
                        </div>
                    ) : (
                        studies.map((study) => (
                            <div className="col-md-6 col-lg-4" key={study.studyNum}>
                                <div 
                                    className="card h-100 shadow-sm border-1 border-light-subtle" 
                                    style={{ borderRadius: '1rem', transition: 'transform 0.2s', cursor: 'pointer' }}
                                    onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-3px)'}
                                    onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
                                    onClick={() => setDetailModalData(study)}
                                >
                                    <div className="card-body p-4 d-flex flex-column">
                                        <div className="mb-3">
                                            <span className="badge bg-primary-subtle text-primary px-3 py-1 rounded-pill" style={{ fontSize: '0.75rem' }}>
                                                {study.isJoined ? '참여 중' : '모집 중'} · {study.memberCount}명
                                            </span>
                                        </div>
                                        
                                        <h5 className="card-title fw-bold mb-2">{study.studyName}</h5>
                                        <p className="card-text text-muted flex-grow-1" style={{ 
                                            fontSize: '0.85rem', 
                                            whiteSpace: 'pre-wrap',
                                            display: '-webkit-box',
                                            WebkitLineClamp: 2,
                                            WebkitBoxOrient: 'vertical',
                                            overflow: 'hidden',
                                            marginBottom: '1.5rem'
                                        }}>
                                            {study.studyExplain}
                                        </p>
                                        
                                        <div className="d-flex justify-content-between align-items-center mt-auto pt-3 border-top">
                                            <div className="d-flex align-items-center">
                                                <div className="rounded-circle bg-light d-flex justify-content-center align-items-center text-secondary shadow-sm" style={{ width: '32px', height: '32px', fontSize: '0.8rem', border: '1px solid #fff' }}>
                                                    {study.userNickname ? study.userNickname.charAt(0) : 'U'}
                                                </div>
                                                {study.memberCount > 1 && (
                                                    <div className="rounded-circle bg-secondary-subtle d-flex justify-content-center align-items-center text-secondary shadow-sm" style={{ width: '32px', height: '32px', fontSize: '0.75rem', border: '1px solid #fff', marginLeft: '-8px' }}>
                                                        +{study.memberCount - 1}
                                                    </div>
                                                )}
                                                <span className="ms-2 small text-muted">{study.userNickname}</span>
                                            </div>

                                            {/* 방장인 경우 수정/삭제, 일반 유저는 참여/취소 버튼 */}
                                            {(!user || user.userNum !== study.userNum) ? (
                                                study.isJoined ? (
                                                    <button className="btn btn-secondary btn-sm px-3 rounded-pill" onClick={(e) => handleLeave(e, study.studyNum)}>
                                                        참여 취소
                                                    </button>
                                                ) : (
                                                    <button className="btn btn-outline-primary btn-sm px-3 rounded-pill" onClick={(e) => handleJoin(e, study.studyNum)}>
                                                        참여하기
                                                    </button>
                                                )
                                            ) : (
                                                <div>
                                                    <button 
                                                        className="btn btn-sm text-secondary p-0 me-2"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            setEditModalData(study);
                                                        }}
                                                    >
                                                        수정
                                                    </button>
                                                    <button 
                                                        className="btn btn-sm text-danger p-0"
                                                        onClick={(e) => handleDelete(e, study.studyNum)}
                                                    >
                                                        삭제
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))
                    )}
                </div>
            )}

            {/* 개설 모달 */}
            <StudyCreateModal 
                show={showModal} 
                onClose={() => setShowModal(false)} 
                onSuccess={loadStudies}
            />

            {/* 수정 모달 */}
            <StudyEditModal
                show={editModalData !== null}
                onClose={() => setEditModalData(null)}
                onSuccess={() => {
                    setEditModalData(null);
                    loadStudies();
                }}
                initialData={editModalData}
            />

            {/* 상세 모달 */}
            <StudyDetailModal
                show={detailModalData !== null}
                onClose={() => setDetailModalData(null)}
                study={detailModalData}
            />
        </div>
    );
}

export default StudyListSection;
