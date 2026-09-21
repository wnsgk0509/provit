import React, { useState, useEffect } from 'react';
import { fetchStudyList, deleteStudy, joinStudy, leaveStudy } from '../../api/studyApi';
import { useAuth } from '../../context/AuthContext';
import StudyCreateModal from '../../components/study/StudyCreateModal';

function Study() {
    const { user, isLoggedIn } = useAuth();
    const [studies, setStudies] = useState([]);
    const [showModal, setShowModal] = useState(false);

    useEffect(() => {
        loadStudies();
    }, [user]);

    const loadStudies = async () => {
        try {
            const userNum = user ? user.userNum : null;
            const result = await fetchStudyList(userNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setStudies(result.data || []);
            }
        } catch (error) {
            console.error('스터디 목록을 불러오는 중 오류 발생');
        }
    };

    const handleDelete = async (studyNum) => {
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

    const handleJoin = async (studyNum) => {
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

    const handleLeave = async (studyNum) => {
        if (!window.confirm('정말 스터디 참여를 취소하시겠습니까?')) return;
        try {
            const result = await leaveStudy(studyNum, user.userNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                loadStudies();
            } else {
                alert('참여 취소에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    return (
        <div className="container py-4">
            <div className="d-flex justify-content-between align-items-center mb-5">
                <div>
                    <h2 className="fw-bold mb-1">스터디 모집</h2>
                    <p className="text-muted mb-0">함께 성장할 동료를 찾고 스터디에 참여해 보세요.</p>
                </div>
                {isLoggedIn && (
                    <button 
                        className="btn btn-primary rounded-pill px-4 shadow-sm"
                        onClick={() => setShowModal(true)}
                    >
                        + 스터디 개설
                    </button>
                )}
            </div>

            <div className="row g-4">
                {studies.length === 0 ? (
                    <div className="col-12 text-center py-5">
                        <div className="text-muted mb-3 fs-5">아직 개설된 스터디가 없습니다.</div>
                        <button className="btn btn-outline-primary" onClick={() => setShowModal(true)}>첫 스터디의 방장이 되어보세요!</button>
                    </div>
                ) : (
                    studies.map((study) => (
                        <div className="col-md-6 col-lg-4" key={study.studyNum}>
                            <div className="card h-100 shadow-sm border-0" style={{ borderRadius: '1rem', transition: 'transform 0.2s', cursor: 'default' }}
                                 onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-5px)'}
                                 onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}>
                                
                                <div className="card-body p-4 d-flex flex-column">
                                    <div className="d-flex justify-content-between align-items-start mb-3">
                                        <span className="badge bg-primary-subtle text-primary px-3 py-2 rounded-pill">
                                            <i className="bi bi-people-fill me-1"></i> {study.memberCount}명 참여중
                                        </span>
                                        {/* 방장일 경우 삭제 버튼 표출 */}
                                        {user && user.userNum === study.userNum && (
                                            <button 
                                                className="btn btn-sm btn-link text-danger p-0 text-decoration-none"
                                                onClick={() => handleDelete(study.studyNum)}
                                            >
                                                삭제
                                            </button>
                                        )}
                                    </div>
                                    
                                    <h5 className="card-title fw-bold mb-3">{study.studyName}</h5>
                                    <p className="card-text text-muted flex-grow-1" style={{ fontSize: '0.9rem', whiteSpace: 'pre-wrap' }}>
                                        {study.studyExplain}
                                    </p>
                                    
                                    <div className="d-flex justify-content-between align-items-end mt-4 pt-3 border-top">
                                        <div className="text-muted small">
                                            <div>방장: <strong className="text-dark">{study.userNickname}</strong></div>
                                            <div>{study.studyCreateDate.split(' ')[0]} 개설</div>
                                        </div>
                                        
                                        {/* 방장인 경우 참여/취소 버튼 숨김 (자신이 방장이므로 무조건 참여) */}
                                        {(!user || user.userNum !== study.userNum) && (
                                            study.isJoined ? (
                                                <button className="btn btn-secondary btn-sm px-3 rounded-pill" onClick={() => handleLeave(study.studyNum)}>
                                                    참여 취소
                                                </button>
                                            ) : (
                                                <button className="btn btn-outline-primary btn-sm px-3 rounded-pill" onClick={() => handleJoin(study.studyNum)}>
                                                    참여하기
                                                </button>
                                            )
                                        )}
                                        {user && user.userNum === study.userNum && (
                                            <span className="badge bg-secondary rounded-pill px-3 py-2">내 스터디</span>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))
                )}
            </div>

            {/* 개설 모달 컴포넌트 */}
            <StudyCreateModal 
                show={showModal} 
                onClose={() => setShowModal(false)} 
                onSuccess={loadStudies}
            />
        </div>
    );
}

export default Study;
