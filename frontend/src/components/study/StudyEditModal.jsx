import React, { useState, useEffect } from 'react';
import { updateStudy } from '../../api/studyApi';
import { useAuth } from '../../context/AuthContext';

function StudyEditModal({ show, onClose, onSuccess, initialData }) {
    const { user } = useAuth();
    const [studyName, setStudyName] = useState('');
    const [studyExplain, setStudyExplain] = useState('');

    useEffect(() => {
        if (initialData) {
            setStudyName(initialData.studyName || '');
            setStudyExplain(initialData.studyExplain || '');
        }
    }, [initialData]);

    if (!show || !initialData) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!user) {
            alert('로그인이 필요합니다.');
            return;
        }
        if (!studyName.trim() || !studyExplain.trim()) {
            alert('스터디 이름과 소개를 모두 입력해주세요.');
            return;
        }

        try {
            const result = await updateStudy(initialData.studyNum, {
                userNum: user.userNum,
                studyName,
                studyExplain
            });
            
            if (result && result.responseCode && result.responseCode.code === 200) {
                alert('스터디 정보가 성공적으로 수정되었습니다!');
                onSuccess(); // 목록 새로고침
                onClose(); // 모달 닫기
            } else {
                alert('스터디 수정에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    return (
        <>
            <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                <div className="modal-dialog modal-dialog-centered">
                    <div className="modal-content border-0 shadow-lg" style={{ borderRadius: '1rem' }}>
                        <div className="modal-header border-bottom-0 pb-0">
                            <h5 className="modal-title fw-bold">✏️ 스터디 수정하기</h5>
                            <button type="button" className="btn-close" onClick={onClose}></button>
                        </div>
                        <div className="modal-body">
                            <form id="studyEditForm" onSubmit={handleSubmit}>
                                <div className="mb-3">
                                    <label className="form-label fw-semibold text-secondary small">스터디 이름</label>
                                    <input 
                                        type="text" 
                                        className="form-control" 
                                        placeholder="예) 프론트엔드 모의 면접 스터디"
                                        value={studyName}
                                        onChange={(e) => setStudyName(e.target.value)}
                                        maxLength={100}
                                    />
                                </div>
                                <div className="mb-3">
                                    <label className="form-label fw-semibold text-secondary small">스터디 소개 및 목표</label>
                                    <textarea 
                                        className="form-control" 
                                        rows="4" 
                                        placeholder="스터디 진행 방식, 목표, 우대 사항 등을 적어주세요."
                                        value={studyExplain}
                                        onChange={(e) => setStudyExplain(e.target.value)}
                                        style={{ resize: 'none' }}
                                    ></textarea>
                                </div>
                            </form>
                        </div>
                        <div className="modal-footer border-top-0 pt-0 justify-content-between">
                            <button type="button" className="btn btn-light rounded-pill px-4" onClick={onClose}>취소</button>
                            <button type="submit" form="studyEditForm" className="btn btn-primary rounded-pill px-4">수정 완료</button>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}

export default StudyEditModal;
