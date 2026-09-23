import React, { useState, useEffect } from 'react';
import { fetchStudyMembers } from '../../api/studyApi';

function StudyDetailModal({ show, onClose, study }) {
    const [members, setMembers] = useState([]);

    useEffect(() => {
        if (show && study) {
            loadMembers();
        } else {
            setMembers([]);
        }
    }, [show, study]);

    const loadMembers = async () => {
        try {
            const result = await fetchStudyMembers(study.studyNum);
            if (result && result.responseCode && result.responseCode.code === 200) {
                setMembers(result.data || []);
            }
        } catch (error) {
            console.error('참여자 목록을 불러오는 중 오류 발생:', error);
        }
    };

    if (!show || !study) return null;

    return (
        <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog modal-dialog-centered modal-lg">
                <div className="modal-content border-0 shadow-lg" style={{ borderRadius: '1rem' }}>
                    <div className="modal-header border-bottom-0 pb-0">
                        <h5 className="modal-title fw-bold">📖 스터디 상세 내용</h5>
                        <button type="button" className="btn-close" onClick={onClose}></button>
                    </div>
                    <div className="modal-body p-4">
                        <div className="mb-4">
                            <span className={`badge ${study.memberCount >= study.maxMembers ? 'bg-danger-subtle text-danger' : 'bg-primary-subtle text-primary'} px-3 py-2 rounded-pill me-2`}>
                                👥 {study.memberCount} / {study.maxMembers}명 {study.memberCount >= study.maxMembers ? '마감됨' : '참여중'}
                            </span>
                            {study.isJoined && (
                                <span className="badge bg-success-subtle text-success px-3 py-2 rounded-pill">
                                    ✓ 참여중
                                </span>
                            )}
                        </div>
                        
                        <h4 className="fw-bold mb-3">{study.studyName}</h4>
                        
                        <div className="d-flex align-items-center text-muted small mb-4 pb-3 border-bottom">
                            <div className="me-3">방장: <strong className="text-dark">{study.userNickname}</strong></div>
                            <div>개설일: {study.studyCreateDate ? study.studyCreateDate.split(' ')[0] : ''}</div>
                        </div>

                        <div className="bg-light rounded-4 p-4 mb-4">
                            <h6 className="fw-bold mb-3 text-secondary">스터디 소개 및 목표</h6>
                            <p className="mb-0 text-dark" style={{ whiteSpace: 'pre-wrap', lineHeight: '1.6' }}>
                                {study.studyExplain}
                            </p>
                        </div>

                        <div className="mb-2">
                            <h6 className="fw-bold mb-3 text-secondary">참여 멤버 목록 ({members.length}명)</h6>
                            <div className="d-flex flex-wrap gap-2">
                                {members.map((nickname, index) => (
                                    <span key={index} className="badge bg-white text-dark border border-light-subtle px-3 py-2 rounded-pill shadow-sm">
                                        👤 {nickname}
                                        {nickname === study.userNickname && (
                                            <span className="text-primary ms-1">(방장)</span>
                                        )}
                                    </span>
                                ))}
                                {members.length === 0 && (
                                    <span className="text-muted small">참여자 정보를 불러오는 중입니다...</span>
                                )}
                            </div>
                        </div>
                    </div>
                    <div className="modal-footer border-top-0 pt-0">
                        <button type="button" className="btn btn-secondary rounded-pill px-4" onClick={onClose}>
                            닫기
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default StudyDetailModal;
